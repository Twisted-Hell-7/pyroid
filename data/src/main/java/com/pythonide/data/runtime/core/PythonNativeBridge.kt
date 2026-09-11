package com.pythonide.data.runtime.core

import android.content.Context
import com.pythonide.domain.model.ExecutionResult
import com.pythonide.domain.model.InterpreterConfig
import com.pythonide.domain.model.PythonException
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PythonNativeBridge @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val isInitialized = AtomicBoolean(false)
    private val isRunning = AtomicBoolean(false)

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }
            isInitialized.set(true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
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

        val startTime = System.currentTimeMillis()
        isRunning.set(true)

        try {
            val py = Python.getInstance()
            val builtins = py.getBuiltins()

            val wrappedCode = wrapCodeForExecution(code)

            val codeObj = builtins.callAttr("compile", wrappedCode, "<string>", "exec")

            val globals = builtins.callAttr("dict")
            globals.callAttr("__setitem__", "__builtins__", py.getBuiltins())
            builtins.callAttr("exec", codeObj, globals)

            val stdout = globals.callAttr("get", "_stdout_val")?.toString().orEmpty().trim()
            val stderr = globals.callAttr("get", "_stderr_val")?.toString().orEmpty().trim()

            val exception = if (stderr.isNotEmpty()) {
                parsePythonException(stderr)
            } else null

            onStdout?.invoke(stdout)
            if (stderr.isNotEmpty()) onStderr?.invoke(stderr)

            ExecutionResult(
                code = code,
                stdout = stdout,
                stderr = stderr,
                exitCode = if (exception != null) 1 else 0,
                executionTimeMs = System.currentTimeMillis() - startTime,
                exception = exception
            )
        } catch (e: Exception) {
            val stderr = e.message ?: "Unknown error"
            onStderr?.invoke(stderr)
            ExecutionResult(
                code = code,
                stderr = stderr,
                exitCode = 1,
                executionTimeMs = System.currentTimeMillis() - startTime,
                exception = PythonException(
                    type = e.javaClass.simpleName,
                    message = e.message ?: "Unknown error"
                )
            )
        } finally {
            isRunning.set(false)
        }
    }

    private fun wrapCodeForExecution(code: String): String {
        val escapedCode = code
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return """
import sys
import io

class StdoutCapture:
    def __init__(self):
        self.buffer = io.StringIO()
    def write(self, text):
        sys.__stdout__.write(text)
        self.buffer.write(text)
    def flush(self):
        sys.__stdout__.flush()

class StderrCapture:
    def __init__(self):
        self.buffer = io.StringIO()
    def write(self, text):
        sys.__stderr__.write(text)
        self.buffer.write(text)
    def flush(self):
        sys.__stderr__.flush()

_stdout_capture = StdoutCapture()
_stderr_capture = StderrCapture()
sys.stdout = _stdout_capture
sys.stderr = _stderr_capture

try:
    exec(compile("$escapedCode", "<string>", "exec"))
except Exception:
    import traceback
    traceback.print_exc()
finally:
    sys.stdout = sys.__stdout__
    sys.stderr = sys.__stderr__
    _stdout_val = _stdout_capture.buffer.getvalue()
    _stderr_val = _stderr_capture.buffer.getvalue()
    if _stdout_val:
        sys.__stdout__.write(_stdout_val)
    if _stderr_val:
        sys.__stderr__.write(_stderr_val)
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
        isRunning.set(false)
    }

    fun isRunning(): Boolean = isRunning.get()

    fun cleanup() {
        stopExecution()
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

        try {
            val py = Python.getInstance()
            val builtins = py.getBuiltins()

            val wrappedCode = wrapCodeForExecution(command)
            val codeObj = builtins.callAttr("compile", wrappedCode, "<string>", "exec")
            val globals = builtins.callAttr("dict")
            globals.callAttr("__setitem__", "__builtins__", py.getBuiltins())
            builtins.callAttr("exec", codeObj, globals)

            CommandResult(
                exitCode = 0,
                output = globals.callAttr("get", "_stdout_val")?.toString().orEmpty().trim(),
                error = globals.callAttr("get", "_stderr_val")?.toString().orEmpty().trim()
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

        try {
            val py = Python.getInstance()
            val builtins = py.getBuiltins()

            val wrappedCode = wrapCodeForExecution(command)
            val codeObj = builtins.callAttr("compile", wrappedCode, "<string>", "exec")
            val globals = builtins.callAttr("dict")
            globals.callAttr("__setitem__", "__builtins__", py.getBuiltins())
            builtins.callAttr("exec", codeObj, globals)

            val output = globals.callAttr("get", "_stdout_val")?.toString().orEmpty().trim()
            val error = globals.callAttr("get", "_stderr_val")?.toString().orEmpty().trim()
            if (output.isNotEmpty()) {
                output.lines().forEach { onLine(it) }
            }
            CommandResult(exitCode = 0, output = output, error = error)
        } catch (e: Exception) {
            onLine(e.message ?: "Unknown error")
            CommandResult(exitCode = 1, output = "", error = e.message ?: "Unknown error")
        }
    }

    fun getPythonVersion(): String? {
        return try {
            val py = Python.getInstance()
            val sys = py.getModule("sys")
            sys.get("version")?.toString()
        } catch (e: Exception) {
            null
        }
    }
}
