package com.pythonide.data.runtime.core

import android.content.Context
import com.pythonide.domain.model.ExecutionResult
import com.pythonide.domain.model.InterpreterConfig
import com.pythonide.domain.model.PythonException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PythonNativeBridge @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val isInitialized = AtomicBoolean(false)
    private val pythonPath = AtomicReference<String?>(null)
    private val currentProcess = AtomicReference<Process?>(null)
    private val isRunning = AtomicBoolean(false)

    private val stdoutBuffer = StringBuilder()
    private val stderrBuffer = StringBuilder()
    private val inputQueue = ArrayDeque<String>()

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val pythonDir = File(context.filesDir, "python")
            if (!pythonDir.exists()) {
                pythonDir.mkdirs()
                extractPythonRuntime(pythonDir)
            }

            val pythonBin = File(pythonDir, "bin/python3")
            if (pythonBin.exists()) {
                pythonBin.setExecutable(true)
                pythonPath.set(pythonBin.absolutePath)
                isInitialized.set(true)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun extractPythonRuntime(targetDir: File) {
        val assetManager = context.assets
        try {
            val pythonAssets = assetManager.list("python") ?: emptyArray()
            for (asset in pythonAssets) {
                val inputStream = assetManager.open("python/$asset")
                val outputFile = File(targetDir, asset)
                outputFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                inputStream.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun executeCode(
        code: String,
        config: InterpreterConfig,
        onStdout: ((String) -> Unit)? = null,
        onStderr: ((String) -> Unit)? = null,
        onInput: (() -> String?)? = null
    ): ExecutionResult = withContext(Dispatchers.IO) {
        if (!isInitialized.get()) {
            initialize()
        }

        val pythonBin = pythonPath.get() ?: return@withContext ExecutionResult(
            code = code,
            stderr = "Python runtime not initialized",
            exitCode = 1
        )

        val startTime = System.currentTimeMillis()
        stdoutBuffer.clear()
        stderrBuffer.clear()
        isRunning.set(true)

        try {
            val processBuilder = ProcessBuilder(
                pythonBin,
                "-c",
                wrapCodeForExecution(code, config)
            )

            processBuilder.environment()["PYTHONIOENCODING"] = "utf-8"
            processBuilder.environment()["PYTHONDONTWRITEBYTECODE"] = "1"
            processBuilder.redirectErrorStream(false)

            val process = processBuilder.start()
            currentProcess.set(process)

            val stdoutThread = Thread {
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val output = line ?: ""
                    stdoutBuffer.appendLine(output)
                    onStdout?.invoke(output)
                }
            }

            val stderrThread = Thread {
                val reader = BufferedReader(InputStreamReader(process.errorStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val error = line ?: ""
                    stderrBuffer.appendLine(error)
                    onStderr?.invoke(error)
                }
            }

            stdoutThread.start()
            stderrThread.start()

            val completed = process.waitFor(
                config.maxExecutionTimeMs,
                TimeUnit.MILLISECONDS
            )

            if (!completed) {
                process.destroyForcibly()
                return@withContext ExecutionResult(
                    code = code,
                    stdout = stdoutBuffer.toString(),
                    stderr = "Execution timed out after ${config.maxExecutionTimeMs}ms",
                    exitCode = 124,
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    exception = PythonException(
                        type = "TimeoutError",
                        message = "Execution timed out"
                    )
                )
            }

            stdoutThread.join(1000)
            stderrThread.join(1000)

            val exitCode = process.exitValue()
            val stdout = stdoutBuffer.toString().trim()
            val stderr = stderrBuffer.toString().trim()

            val exception = if (stderr.isNotEmpty()) {
                parsePythonException(stderr)
            } else null

            ExecutionResult(
                code = code,
                stdout = stdout,
                stderr = stderr,
                exitCode = exitCode,
                executionTimeMs = System.currentTimeMillis() - startTime,
                exception = exception
            )
        } catch (e: Exception) {
            ExecutionResult(
                code = code,
                stderr = e.message ?: "Unknown error",
                exitCode = 1,
                executionTimeMs = System.currentTimeMillis() - startTime,
                exception = PythonException(
                    type = "RuntimeError",
                    message = e.message ?: "Unknown error"
                )
            )
        } finally {
            isRunning.set(false)
            currentProcess.set(null)
        }
    }

    private fun wrapCodeForExecution(code: String, config: InterpreterConfig): String {
        return """
import sys
import io
import traceback

# Capture stdout/stderr
old_stdout = sys.stdout
old_stderr = sys.stderr
sys.stdout = io.StringIO()
sys.stderr = io.StringIO()

try:
    # Execute the user code
    exec(compile(${
            code.toByteArray().joinToString(",") { "0x${it.toString(16)}" }
        }.decode('utf-8'), '<string>', 'exec'))
    
    # Get captured output
    stdout_value = sys.stdout.getvalue()
    stderr_value = sys.stderr.getvalue()
    
    # Restore original streams
    sys.stdout = old_stdout
    sys.stderr = old_stderr
    
    # Print captured output
    if stdout_value:
        print(stdout_value, end='')
    if stderr_value:
        print(stderr_value, file=sys.stderr, end='')
        
except Exception as e:
    # Restore streams on error
    sys.stdout = old_stdout
    sys.stderr = old_stderr
    traceback.print_exc()
""".trimIndent()
    }

    private fun parsePythonException(stderr: String): PythonException? {
        val lines = stderr.lines()
        if (lines.isEmpty()) return null

        val lastLine = lines.lastOrNull { it.isNotBlank() } ?: return null
        val exceptionPattern = Regex("^([A-Za-z_][A-Za-z0-9_]*):\\s*(.*)$")
        val match = exceptionPattern.find(lastLine)

        return if (match != null) {
            PythonException(
                type = match.groupValues[1],
                message = match.groupValues[2],
                traceback = stderr
            )
        } else {
            PythonException(
                type = "Error",
                message = lastLine,
                traceback = stderr
            )
        }
    }

    fun stopExecution() {
        currentProcess.get()?.destroyForcibly()
        isRunning.set(false)
    }

    fun isRunning(): Boolean = isRunning.get()

    fun cleanup() {
        stopExecution()
        stdoutBuffer.clear()
        stderrBuffer.clear()
        inputQueue.clear()
    }

    data class CommandResult(
        val exitCode: Int,
        val output: String,
        val error: String
    )

    suspend fun executeCommand(command: String): CommandResult = withContext(Dispatchers.IO) {
        if (!isInitialized.get()) {
            initialize()
        }

        val pythonBin = pythonPath.get() ?: return@withContext CommandResult(
            exitCode = 1,
            output = "",
            error = "Python runtime not initialized"
        )

        try {
            val processBuilder = ProcessBuilder(pythonBin, "-c", command)
            processBuilder.environment()["PYTHONIOENCODING"] = "utf-8"
            processBuilder.redirectErrorStream(false)

            val process = processBuilder.start()
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val completed = process.waitFor(30, TimeUnit.SECONDS)
            val exitCode = if (completed) process.exitValue() else -1

            CommandResult(
                exitCode = exitCode,
                output = stdout.trim(),
                error = stderr.trim()
            )
        } catch (e: Exception) {
            CommandResult(
                exitCode = 1,
                output = "",
                error = e.message ?: "Unknown error"
            )
        }
    }

    suspend fun executeCommandWithCallback(
        command: String,
        onLine: (String) -> Unit
    ): CommandResult = withContext(Dispatchers.IO) {
        if (!isInitialized.get()) {
            initialize()
        }

        val pythonBin = pythonPath.get() ?: return@withContext CommandResult(
            exitCode = 1,
            output = "",
            error = "Python runtime not initialized"
        )

        try {
            val processBuilder = ProcessBuilder(pythonBin, "-c", command)
            processBuilder.environment()["PYTHONIOENCODING"] = "utf-8"
            processBuilder.redirectErrorStream(false)

            val process = processBuilder.start()
            val outputBuilder = StringBuilder()
            val errorBuilder = StringBuilder()

            val stdoutThread = Thread {
                val reader = process.inputStream.bufferedReader()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val output = line ?: ""
                    outputBuilder.appendLine(output)
                    onLine(output)
                }
            }

            val stderrThread = Thread {
                val reader = process.errorStream.bufferedReader()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val error = line ?: ""
                    errorBuilder.appendLine(error)
                    onLine(error)
                }
            }

            stdoutThread.start()
            stderrThread.start()

            val completed = process.waitFor(30, TimeUnit.SECONDS)
            val exitCode = if (completed) process.exitValue() else -1

            stdoutThread.join(1000)
            stderrThread.join(1000)

            CommandResult(
                exitCode = exitCode,
                output = outputBuilder.toString().trim(),
                error = errorBuilder.toString().trim()
            )
        } catch (e: Exception) {
            CommandResult(
                exitCode = 1,
                output = "",
                error = e.message ?: "Unknown error"
            )
        }
    }

    fun getPythonVersion(): String? {
        return try {
            val pythonBin = pythonPath.get() ?: return null
            val process = ProcessBuilder(pythonBin, "--version")
                .redirectErrorStream(true)
                .start()
            val version = process.inputStream.bufferedReader().readText().trim()
            process.waitFor(5, TimeUnit.SECONDS)
            version
        } catch (e: Exception) {
            null
        }
    }
}
