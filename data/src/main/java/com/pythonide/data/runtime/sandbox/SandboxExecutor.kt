package com.pythonide.data.runtime.sandbox

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SandboxExecutor @Inject constructor() {
    private val _sandboxState = MutableStateFlow<SandboxState>(SandboxState.Idle)
    val sandboxState: StateFlow<SandboxState> = _sandboxState.asStateFlow()

    private val _resourceUsage = MutableStateFlow(ResourceUsage())
    val resourceUsage: StateFlow<ResourceUsage> = _resourceUsage.asStateFlow()

    private val activeProcesses = ConcurrentHashMap<String, SandboxProcess>()
    private val isEnforcing = AtomicBoolean(true)

    sealed class SandboxState {
        object Idle : SandboxState()
        data class Running(val processId: String) : SandboxState()
        data class Terminated(val processId: String, val reason: TerminationReason) : SandboxState()
        data class Error(val message: String) : SandboxState()
    }

    enum class TerminationReason {
        TIMEOUT, MEMORY_LIMIT, CPU_LIMIT, IO_VIOLATION, SECURITY_VIOLATION, USER_REQUEST
    }

    data class ResourceLimits(
        val maxMemoryBytes: Long = 256 * 1024 * 1024,
        val maxCpuTimeMs: Long = 30_000,
        val maxWallTimeMs: Long = 60_000,
        val maxOutputBytes: Long = 10 * 1024 * 1024,
        val maxFileDescriptors: Int = 64,
        val maxProcesses: Int = 4,
        val allowNetworkAccess: Boolean = false,
        val allowFileSystemWrite: Boolean = false,
        val allowedDirectories: List<String> = emptyList(),
        val blockedModules: List<String> = listOf(
            "subprocess", "os.system", "shutil.rmtree", "pathlib.Path.rmdir"
        )
    )

    data class ResourceUsage(
        val currentMemoryBytes: Long = 0,
        val cpuTimeMs: Long = 0,
        val wallTimeMs: Long = 0,
        val outputBytes: Long = 0,
        val openFileDescriptors: Int = 0,
        val activeProcesses: Int = 0
    )

    data class SandboxProcess(
        val id: String,
        val process: Process,
        val startTime: Long = System.currentTimeMillis(),
        val limits: ResourceLimits,
        val memoryUsage: AtomicLong = AtomicLong(0),
        val outputSize: AtomicLong = AtomicLong(0),
        val isKilled: AtomicBoolean = AtomicBoolean(false)
    )

    suspend fun executeInSandbox(
        command: List<String>,
        workingDirectory: File,
        limits: ResourceLimits = ResourceLimits(),
        onOutput: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ): SandboxResult = withContext(Dispatchers.IO) {
        val processId = "sandbox_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"

        try {
            validateCommand(command, limits)
            validateWorkingDirectory(workingDirectory, limits)

            val processBuilder = ProcessBuilder(command)
                .directory(workingDirectory)
                .redirectErrorStream(false)
                .apply {
                    environment().clear()
                    environment().putAll(createSandboxEnvironment())
                }

            val process = processBuilder.start()
            val sandboxProcess = SandboxProcess(
                id = processId,
                process = process,
                limits = limits
            )

            activeProcesses[processId] = sandboxProcess
            _sandboxState.value = SandboxState.Running(processId)

            val monitorJob = launch {
                monitorProcess(sandboxProcess)
            }

            val outputBuffer = StringBuilder()
            val errorBuffer = StringBuilder()
            val collectingOutput: (String) -> Unit = { line ->
                synchronized(outputBuffer) { outputBuffer.appendLine(line) }
                onOutput(line)
            }
            val collectingError: (String) -> Unit = { line ->
                synchronized(errorBuffer) { errorBuffer.appendLine(line) }
                onError(line)
            }

            val outputJob = launch {
                readProcessOutput(process, sandboxProcess, collectingOutput, collectingError)
            }

            val exitCode = withTimeoutOrNull(limits.maxWallTimeMs) {
                process.waitFor()
            }

            try {
                withTimeoutOrNull(2000) { outputJob.join() }
            } catch (e: Exception) {
                // best effort
            }
            monitorJob.cancel()
            if (outputJob.isActive) outputJob.cancel()

            if (exitCode == null) {
                terminateProcess(sandboxProcess, TerminationReason.TIMEOUT)
                SandboxResult.Timeout(processId, limits.maxWallTimeMs)
            } else {
                val output = synchronized(outputBuffer) { outputBuffer.toString().trim() } +
                    readRemainingOutput(process).let { if (it.isNotBlank()) "\n$it" else "" }
                SandboxResult.Completed(
                    processId = processId,
                    exitCode = exitCode,
                    output = output.trim(),
                    resourceUsage = getCurrentUsage(sandboxProcess)
                )
            }
        } catch (e: CancellationException) {
            terminateProcess(activeProcesses[processId], TerminationReason.USER_REQUEST)
            SandboxResult.Cancelled(processId)
        } catch (e: SecurityException) {
            SandboxResult.SecurityViolation(processId, e.message ?: "Security violation")
        } catch (e: Exception) {
            SandboxResult.Error(processId, e.message ?: "Unknown error")
        } finally {
            activeProcesses.remove(processId)
        }
    }

    private fun validateCommand(command: List<String>, limits: ResourceLimits) {
        if (command.isEmpty()) {
            throw SecurityException("Empty command")
        }

        if (isEnforcing.get()) {
            val fullCommand = command.joinToString(" ")
            for (blocked in limits.blockedModules) {
                if (fullCommand.contains(blocked)) {
                    throw SecurityException("Blocked module pattern: $blocked")
                }
            }
        }

        val blockedCommands = listOf("rm -rf", "mkfs", "dd if=", "> /dev/")
        val fullCommand = command.joinToString(" ")

        for (blocked in blockedCommands) {
            if (fullCommand.contains(blocked)) {
                throw SecurityException("Blocked command pattern: $blocked")
            }
        }

        if (!limits.allowNetworkAccess) {
            val networkCommands = listOf("curl", "wget", "ssh", "nc", "netcat")
            for (networkCmd in networkCommands) {
                if (command.any { it.contains(networkCmd) }) {
                    throw SecurityException("Network access not allowed: $networkCmd")
                }
            }
        }
    }

    private fun validateWorkingDirectory(directory: File, limits: ResourceLimits) {
        if (!directory.exists()) {
            throw SecurityException("Working directory does not exist: ${directory.absolutePath}")
        }

        if (!directory.isDirectory) {
            throw SecurityException("Path is not a directory: ${directory.absolutePath}")
        }

        val canonicalPath = directory.canonicalPath
        val sensitivePaths = listOf(
            "/etc", "/var", "/usr", "/bin", "/sbin", "/root",
            "/system", "/data/data", "/proc", "/sys"
        )

        for (sensitive in sensitivePaths) {
            if (canonicalPath == sensitive || canonicalPath.startsWith(sensitive + File.separator)) {
                throw SecurityException("Access to sensitive directory blocked: $sensitive")
            }
        }
    }

    private fun createSandboxEnvironment(): Map<String, String> {
        return mapOf(
            "PATH" to "/usr/local/bin:/usr/bin:/bin",
            "HOME" to "/tmp/sandbox_home",
            "TMPDIR" to "/tmp/sandbox_tmp",
            "LANG" to "en_US.UTF-8",
            "PYTHONDONTWRITEBYTECODE" to "1",
            "PYTHONUNBUFFERED" to "1"
        )
    }

    private suspend fun monitorProcess(process: SandboxProcess) {
        while (process.process.isAlive && !process.isKilled.get()) {
            val elapsed = System.currentTimeMillis() - process.startTime

            if (elapsed > process.limits.maxWallTimeMs) {
                terminateProcess(process, TerminationReason.TIMEOUT)
                return
            }

            if (process.outputSize.get() > process.limits.maxOutputBytes) {
                terminateProcess(process, TerminationReason.IO_VIOLATION)
                return
            }

            delay(100)
        }
    }

    private suspend fun readProcessOutput(
        process: Process,
        sandboxProcess: SandboxProcess,
        onOutput: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        coroutineScope {
            launch {
                process.inputStream.bufferedReader().useLines { lines ->
                    for (line in lines) {
                        if (sandboxProcess.outputSize.get() < sandboxProcess.limits.maxOutputBytes) {
                            sandboxProcess.outputSize.addAndGet(line.length.toLong())
                            onOutput(line)
                        } else {
                            terminateProcess(sandboxProcess, TerminationReason.IO_VIOLATION)
                            return@useLines
                        }
                    }
                }
            }

            launch {
                process.errorStream.bufferedReader().useLines { lines ->
                    for (line in lines) {
                        if (sandboxProcess.outputSize.get() < sandboxProcess.limits.maxOutputBytes) {
                            sandboxProcess.outputSize.addAndGet(line.length.toLong())
                            onError(line)
                        } else {
                            terminateProcess(sandboxProcess, TerminationReason.IO_VIOLATION)
                            return@useLines
                        }
                    }
                }
            }
        }
    }

    private fun terminateProcess(process: SandboxProcess?, reason: TerminationReason) {
        process?.let {
            it.isKilled.set(true)
            try {
                it.process.destroyForcibly()
            } catch (e: Exception) {
                // Process already terminated
            }
            _sandboxState.value = SandboxState.Terminated(it.id, reason)
        }
    }

    private fun readRemainingOutput(process: Process): String {
        return try {
            process.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            ""
        }
    }

    private fun getCurrentUsage(process: SandboxProcess): ResourceUsage {
        return ResourceUsage(
            currentMemoryBytes = process.memoryUsage.get(),
            cpuTimeMs = System.currentTimeMillis() - process.startTime,
            wallTimeMs = System.currentTimeMillis() - process.startTime,
            outputBytes = process.outputSize.get(),
            openFileDescriptors = 0,
            activeProcesses = activeProcesses.size
        )
    }

    fun terminateAll() {
        activeProcesses.values.forEach { process ->
            terminateProcess(process, TerminationReason.USER_REQUEST)
        }
        activeProcesses.clear()
    }

    fun setEnforcement(enabled: Boolean) {
        isEnforcing.set(enabled)
    }

    data class SandboxResult(
        val processId: String,
        val success: Boolean,
        val exitCode: Int? = null,
        val output: String = "",
        val error: String = "",
        val resourceUsage: ResourceUsage = ResourceUsage(),
        val terminationReason: TerminationReason? = null
    ) {
        companion object {
            fun Completed(
                processId: String,
                exitCode: Int,
                output: String,
                resourceUsage: ResourceUsage
            ) = SandboxResult(
                processId = processId,
                success = exitCode == 0,
                exitCode = exitCode,
                output = output,
                resourceUsage = resourceUsage
            )

            fun Timeout(processId: String, timeoutMs: Long) = SandboxResult(
                processId = processId,
                success = false,
                error = "Execution timed out after ${timeoutMs}ms",
                terminationReason = TerminationReason.TIMEOUT
            )

            fun Cancelled(processId: String) = SandboxResult(
                processId = processId,
                success = false,
                error = "Execution cancelled",
                terminationReason = TerminationReason.USER_REQUEST
            )

            fun SecurityViolation(processId: String, message: String) = SandboxResult(
                processId = processId,
                success = false,
                error = message,
                terminationReason = TerminationReason.SECURITY_VIOLATION
            )

            fun Error(processId: String, message: String) = SandboxResult(
                processId = processId,
                success = false,
                error = message
            )
        }
    }
}
