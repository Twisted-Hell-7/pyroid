package com.pythonide.data.debugger

import android.content.Context
import com.pythonide.domain.model.debugger.Breakpoint
import com.pythonide.domain.model.debugger.DebugEvent
import com.pythonide.domain.model.debugger.DebugState
import com.pythonide.domain.model.debugger.LogEntry
import com.pythonide.domain.model.debugger.LogLevel
import com.pythonide.domain.model.debugger.RuntimeInfo
import com.pythonide.domain.model.debugger.StackFrame
import com.pythonide.domain.model.debugger.Variable
import com.pythonide.domain.model.debugger.WatchExpression
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import android.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebuggerEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var debugProcess: Process? = null
    private var stdoutReader: Job? = null
    private var stderrReader: Job? = null
    private var stdinStream: OutputStream? = null

    private val _debugState = MutableStateFlow(DebugState.IDLE)
    val debugState: StateFlow<DebugState> = _debugState.asStateFlow()

    private val _callStack = MutableStateFlow<List<StackFrame>>(emptyList())
    val callStack: StateFlow<List<StackFrame>> = _callStack.asStateFlow()

    private val _variables = MutableStateFlow<List<Variable>>(emptyList())
    val variables: StateFlow<List<Variable>> = _variables.asStateFlow()

    private val _watchExpressions = MutableStateFlow<List<WatchExpression>>(emptyList())
    val watchExpressions: StateFlow<List<WatchExpression>> = _watchExpressions.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _runtimeInfo = MutableStateFlow(RuntimeInfo())
    val runtimeInfo: StateFlow<RuntimeInfo> = _runtimeInfo.asStateFlow()

    private val _events = MutableSharedFlow<DebugEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<DebugEvent> = _events.asSharedFlow()

    private val _breakpoints = MutableStateFlow<List<Breakpoint>>(emptyList())
    val breakpoints: StateFlow<List<Breakpoint>> = _breakpoints.asStateFlow()

    private val _currentFrame = MutableStateFlow<StackFrame?>(null)
    val currentFrame: StateFlow<StackFrame?> = _currentFrame.asStateFlow()

    private var currentFilePath: String = ""
    private var currentCode: String = ""
    private val commandResponses = ConcurrentLinkedQueue<String>()

    suspend fun startDebugging(
        filePath: String,
        code: String,
        breakpoints: List<Breakpoint>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            stopDebugging()
            currentFilePath = filePath
            currentCode = code
            _breakpoints.value = breakpoints
            _debugState.value = DebugState.STARTING
            addLog(LogLevel.INFO, "Starting debug session for ${File(filePath).name}")

            val workDir = File(filePath).parentFile ?: context.filesDir
            val debugScript = createDebugScript(code, filePath, breakpoints)

            val scriptFile = File(workDir, ".debug_session.py")
            scriptFile.writeText(debugScript)
            scriptFile.deleteOnExit()

            val pythonPath = findPythonPath()
            if (pythonPath == null) {
                // No system python on Android — caller should route via Chaquopy/PythonNativeBridge.
                _debugState.value = DebugState.ERROR
                addLog(LogLevel.ERROR, "No Python executable found; debugging requires Chaquopy runtime on Android")
                return@withContext Result.failure(IllegalStateException("No Python executable available on this device"))
            }
            val processBuilder = ProcessBuilder(
                pythonPath, "-u", scriptFile.absolutePath
            )
            processBuilder.directory(workDir)
            processBuilder.environment()["PYTHONIOENCODING"] = "utf-8"
            processBuilder.environment()["PYTHONDONTWRITEBYTECODE"] = "1"
            processBuilder.redirectErrorStream(false)

            val process = processBuilder.start()
            debugProcess = process
            stdinStream = process.outputStream

            stdoutReader = scope.launch {
                readStdout(BufferedReader(InputStreamReader(process.inputStream, "UTF-8")))
            }
            stderrReader = scope.launch {
                readStderr(BufferedReader(InputStreamReader(process.errorStream, "UTF-8")))
            }

            _debugState.value = DebugState.RUNNING
            addLog(LogLevel.INFO, "Debug session started")

            scope.launch {
                val exitCode = try { process.waitFor() } catch (_: Exception) { -1 }
                if (_debugState.value != DebugState.STOPPED) {
                    _debugState.value = DebugState.STOPPED
                    _events.emit(DebugEvent.ExecutionFinished(exitCode))
                    addLog(LogLevel.INFO, "Debug session ended (exit code: $exitCode)")
                }
                cleanup()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            _debugState.value = DebugState.ERROR
            addLog(LogLevel.ERROR, "Failed to start debugging: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun stopDebugging(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sendCommand("quit")
            debugProcess?.destroyForcibly()
            cleanup()
            _debugState.value = DebugState.STOPPED
            addLog(LogLevel.INFO, "Debug session stopped")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun continueExecution(): Result<Unit> = sendPdbCommand("continue")
    suspend fun stepInto(): Result<Unit> = sendPdbCommand("step")
    suspend fun stepOver(): Result<Unit> = sendPdbCommand("next")
    suspend fun stepOut(): Result<Unit> = sendPdbCommand("return")

    suspend fun addBreakpoint(breakpoint: Breakpoint): Result<Unit> {
        _breakpoints.update { current -> current + breakpoint }
        if (_debugState.value == DebugState.PAUSED || _debugState.value == DebugState.RUNNING) {
            sendCommand("break ${breakpoint.filePath}:${breakpoint.lineNumber}")
        }
        return Result.success(Unit)
    }

    suspend fun removeBreakpoint(breakpointId: String): Result<Unit> {
        val bp = _breakpoints.value.find { it.id == breakpointId } ?: return Result.success(Unit)
        _breakpoints.update { current -> current.filter { it.id != breakpointId } }
        if (_debugState.value == DebugState.PAUSED || _debugState.value == DebugState.RUNNING) {
            sendCommand("clear ${bp.filePath}:${bp.lineNumber}")
        }
        return Result.success(Unit)
    }

    suspend fun toggleBreakpoint(breakpointId: String): Result<Unit> {
        _breakpoints.update { current ->
            current.map { bp ->
                if (bp.id == breakpointId) bp.copy(enabled = !bp.enabled) else bp
            }
        }
        return Result.success(Unit)
    }

    suspend fun updateBreakpointCondition(breakpointId: String, condition: String?): Result<Unit> {
        _breakpoints.update { current ->
            current.map { bp ->
                if (bp.id == breakpointId) bp.copy(condition = condition) else bp
            }
        }
        return Result.success(Unit)
    }

    suspend fun addWatchExpression(expression: String): Result<Unit> {
        val watch = WatchExpression(
            id = UUID.randomUUID().toString(),
            expression = expression
        )
        _watchExpressions.update { current -> current + watch }
        if (_debugState.value == DebugState.PAUSED) {
            evaluateWatchExpression(watch)
        }
        return Result.success(Unit)
    }

    suspend fun removeWatchExpression(expressionId: String): Result<Unit> {
        _watchExpressions.update { current -> current.filter { it.id != expressionId } }
        return Result.success(Unit)
    }

    suspend fun evaluateExpression(expression: String): Result<Variable> {
        return try {
            val response = sendCommandAndGetResponse("p $expression")
            val variable = parseVariable(expression, response)
            Result.success(variable)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun inspectVariable(name: String): Result<Variable> {
        return try {
            val response = sendCommandAndGetResponse("pp $name")
            val children = if (isExpandableType(response)) {
                val dirResponse = sendCommandAndGetResponse("dir($name)")
                parseDirResponse(dirResponse).map { childName ->
                    val childResponse = sendCommandAndGetResponse("p $name.$childName")
                    Variable(
                        name = childName,
                        type = inferType(childResponse),
                        value = childResponse.trim()
                    )
                }
            } else null

            val variable = Variable(
                name = name,
                type = inferType(response),
                value = response.trim(),
                repr = response.trim(),
                children = children
            )
            Result.success(variable)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearLogs(): Result<Unit> {
        _logs.value = emptyList()
        return Result.success(Unit)
    }

    private suspend fun sendPdbCommand(command: String): Result<Unit> {
        return try {
            if (_debugState.value == DebugState.PAUSED) {
                _debugState.value = DebugState.STEPPING
            }
            sendCommand(command)
            Result.success(Unit)
        } catch (e: Exception) {
            _debugState.value = DebugState.ERROR
            addLog(LogLevel.ERROR, "Command failed: ${e.message}")
            Result.failure(e)
        }
    }

    private fun sendCommand(command: String) {
        val stdin = stdinStream ?: return
        try {
            stdin.write("$command\n".toByteArray())
            stdin.flush()
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, "Failed to send command: ${e.message}")
        }
    }

    private suspend fun sendCommandAndGetResponse(command: String): String {
        commandResponses.clear()
        sendCommand(command)

        // Collect lines fed by readStdout via commandResponses, with timeout.
        val startTime = System.currentTimeMillis()
        val timeoutMs = 5000L
        val collected = StringBuilder()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val line = commandResponses.poll()
            if (line != null) {
                // (Pdb) prompt marks end of response.
                if (line.startsWith("(Pdb)") || line == "Pdb>") break
                if (collected.isNotEmpty()) collected.append('\n')
                collected.append(line)
                // Heuristic: single-expression responses are one line; keep draining briefly.
                if (collected.length > 4000) break
            } else {
                kotlinx.coroutines.delay(50)
            }
        }

        return collected.toString().trim()
    }

    private suspend fun readStdout(reader: BufferedReader) {
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: continue
                parsePdbOutput(l)
            }
        } catch (e: Exception) {
            if (_debugState.value != DebugState.STOPPED) {
                addLog(LogLevel.ERROR, " stdout read error: ${e.message}")
            }
        }
    }

    private suspend fun readStderr(reader: BufferedReader) {
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: continue
                parseStderrLine(l)
            }
        } catch (e: Exception) {
            if (_debugState.value != DebugState.STOPPED) {
                addLog(LogLevel.ERROR, " stderr read error: ${e.message}")
            }
        }
    }

    private suspend fun parsePdbOutput(line: String) {
        when {
            line.startsWith("__DEBUG_EVENT__:") -> parseDebugEvent(line.removePrefix("__DEBUG_EVENT__:"))
            line.startsWith("(Pdb) ") || line == "Pdb>" -> {
                commandResponses.offer(line)
                if (_debugState.value == DebugState.STEPPING) {
                    _debugState.value = DebugState.PAUSED
                }
            }
            line.startsWith("> ") -> parseSourceLocation(line)
            else -> {
                commandResponses.offer(line)
                addLog(LogLevel.DEBUG, line)
            }
        }
    }

    private suspend fun parseDebugEvent(jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            val type = json.getString("type")

            when (type) {
                "breakpoint_hit" -> {
                    val frame = parseStackFrame(json.getJSONObject("frame"))
                    val frames = parseCallStack(json.optJSONArray("call_stack"))
                    _currentFrame.value = frame
                    _callStack.value = frames
                    _debugState.value = DebugState.PAUSED

                    val localsJson = json.optJSONObject("locals")
                    if (localsJson != null) {
                        _variables.value = parseVariablesFromJson(localsJson)
                    }

                    _events.emit(DebugEvent.BreakpointHit(null, frame, frames))
                    addLog(LogLevel.INFO, "Breakpoint hit at ${frame.fileName}:${frame.lineNumber} in ${frame.functionName}()")

                    refreshWatchExpressions()
                    collectRuntimeInfo()
                }
                "step_complete" -> {
                    val frame = parseStackFrame(json.getJSONObject("frame"))
                    val frames = parseCallStack(json.optJSONArray("call_stack"))
                    _currentFrame.value = frame
                    _callStack.value = frames
                    _debugState.value = DebugState.PAUSED

                    val localsJson = json.optJSONObject("locals")
                    if (localsJson != null) {
                        _variables.value = parseVariablesFromJson(localsJson)
                    }

                    _events.emit(DebugEvent.StepComplete(frame, frames))
                    addLog(LogLevel.DEBUG, "Paused at ${frame.fileName}:${frame.lineNumber}")

                    refreshWatchExpressions()
                    collectRuntimeInfo()
                }
                "exception" -> {
                    val exType = json.getString("exception_type")
                    val message = json.getString("message")
                    val traceback = parseCallStack(json.optJSONArray("traceback"))
                    _debugState.value = DebugState.PAUSED

                    _events.emit(DebugEvent.ExceptionRaised(exType, message, traceback))
                    addLog(LogLevel.ERROR, "$exType: $message")
                }
                "output" -> {
                    val text = json.getString("text")
                    val stream = json.optString("stream", "stdout")
                    _events.emit(DebugEvent.OutputLogged(text, stream))
                    addLog(LogLevel.INFO, text)
                }
                "execution_finished" -> {
                    val exitCode = json.optInt("exit_code", 0)
                    _debugState.value = DebugState.STOPPED
                    _events.emit(DebugEvent.ExecutionFinished(exitCode))
                    addLog(LogLevel.INFO, "Execution finished (exit code: $exitCode)")
                }
            }
        } catch (e: Exception) {
            addLog(LogLevel.WARNING, "Failed to parse event: $jsonStr")
        }
    }

    private fun parseSourceLocation(line: String) {
        val regex = Regex(Regex.escape("> ") + "(.+):(\\d+)(?: in (.+))?")
        regex.find(line)?.let { match ->
            val fileName = match.groupValues[1]
            val lineNumber = match.groupValues[2].toIntOrNull() ?: 0
            val functionName = match.groupValues.getOrElse(3) { "" }
            val frame = StackFrame(
                id = _callStack.value.size,
                fileName = fileName,
                functionName = functionName,
                lineNumber = lineNumber
            )
            _currentFrame.value = frame
        }
    }

    private suspend fun parseStderrLine(line: String) {
        val warningPattern = Regex("^(\\w+Warning:|WARNING:|DeprecationWarning|FutureWarning|UserWarning)")
        val errorPattern = Regex("^(Traceback|\\w+Error:|Exception)")

        when {
            warningPattern.containsMatchIn(line) -> {
                _events.emit(DebugEvent.WarningLogged(line))
                addLog(LogLevel.WARNING, line)
            }
            errorPattern.containsMatchIn(line) -> {
                _events.emit(DebugEvent.ErrorLogged(line))
                addLog(LogLevel.ERROR, line)
            }
            line.isNotBlank() -> {
                addLog(LogLevel.INFO, line)
            }
        }
    }

    private fun parseStackFrame(json: JSONObject): StackFrame {
        return StackFrame(
            id = json.optInt("id", 0),
            fileName = json.optString("filename", ""),
            functionName = json.optString("function", "<module>"),
            lineNumber = json.optInt("lineno", 0),
            column = json.optInt("col_offset", 0),
            sourceLine = json.optString("source_line", "")
        )
    }

    private fun parseCallStack(jsonArray: JSONArray?): List<StackFrame> {
        if (jsonArray == null) return emptyList()
        return (0 until jsonArray.length()).map { i ->
            parseStackFrame(jsonArray.getJSONObject(i))
        }
    }

    private fun parseVariablesFromJson(json: JSONObject): List<Variable> {
        return json.keys().asSequence().map { name ->
            val value = json.opt(name)
            Variable(
                name = name,
                type = value?.javaClass?.simpleName ?: "None",
                value = value?.toString() ?: "None",
                children = if (value is JSONObject) {
                    parseVariablesFromJson(value)
                } else null
            )
        }.toList()
    }

    private fun parseVariable(name: String, value: String): Variable {
        return Variable(
            name = name,
            type = inferType(value),
            value = value.trim(),
            children = if (isExpandableType(value)) {
                emptyList()
            } else null
        )
    }

    private fun inferType(value: String): String {
        val trimmed = value.trim()
        return when {
            trimmed == "None" || trimmed == "null" -> "NoneType"
            trimmed.startsWith("'") || trimmed.startsWith("\"") -> "str"
            trimmed.matches(Regex("-?\\d+")) -> "int"
            trimmed.matches(Regex("-?\\d+\\.\\d+")) -> "float"
            trimmed == "True" || trimmed == "False" -> "bool"
            trimmed.startsWith("[") && trimmed.endsWith("]") -> "list"
            trimmed.startsWith("(") && trimmed.endsWith(")") -> "tuple"
            trimmed.startsWith("{") && trimmed.endsWith("}") -> "dict"
            trimmed.startsWith("<") && trimmed.endsWith(">") -> {
                trimmed.removeSurrounding("<", ">").split(" ").firstOrNull() ?: "object"
            }
            else -> "unknown"
        }
    }

    private fun isExpandableType(value: String): Boolean {
        val type = inferType(value)
        return type in listOf("list", "dict", "tuple", "object")
    }

    private fun parseDirResponse(response: String): List<String> {
        return response.removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().removeSurrounding("'", "\"") }
            .filter { it.isNotEmpty() && !it.startsWith("_") }
    }

    private suspend fun refreshWatchExpressions() {
        _watchExpressions.value.forEach { watch ->
            evaluateWatchExpression(watch)
        }
    }

    private suspend fun evaluateWatchExpression(watch: WatchExpression) {
        try {
            val result = evaluateExpression(watch.expression)
            result.onSuccess { variable ->
                _watchExpressions.update { current ->
                    current.map { w ->
                        if (w.id == watch.id) {
                            w.copy(value = variable.value, type = variable.type, error = null)
                        } else w
                    }
                }
            }
            result.onFailure { e ->
                _watchExpressions.update { current ->
                    current.map { w ->
                        if (w.id == watch.id) {
                            w.copy(value = null, error = e.message)
                        } else w
                    }
                }
            }
        } catch (e: Exception) {
            _watchExpressions.update { current ->
                current.map { w ->
                    if (w.id == watch.id) {
                        w.copy(value = null, error = e.message)
                    } else w
                }
            }
        }
    }

    private suspend fun collectRuntimeInfo() {
        try {
            val memResponse = sendCommandAndGetResponse("import resource; print(resource.getrusage(resource.RUSAGE_SELF).ru_maxrss)")
            val memMb = (memResponse.trim().toDoubleOrNull() ?: 0.0) / 1024.0

            val threadResponse = sendCommandAndGetResponse("import threading; print(len(threading.enumerate()))")
            val threadCount = threadResponse.trim().toIntOrNull() ?: 1

            val moduleResponse = sendCommandAndGetResponse("import sys; print(list(sys.modules.keys())[:20])")
            val modules = moduleResponse.removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("'", "\"") }
                .filter { it.isNotEmpty() }

            _runtimeInfo.value = RuntimeInfo(
                memoryUsageMb = memMb,
                threadCount = threadCount,
                activeModules = modules
            )
            _events.emit(DebugEvent.RuntimeInfoUpdated(_runtimeInfo.value))
        } catch (e: Exception) {
            // Runtime info collection is best-effort
        }
    }

    private fun createDebugScript(
        code: String,
        filePath: String,
        breakpoints: List<Breakpoint>
    ): String {
        val breakpointLines = breakpoints.filter { it.enabled }.map { it.lineNumber }
        val breakpointSet = breakpointLines.joinToString(",") { it.toString() }
        val safeFilePath = filePath.replace("\\", "\\\\").replace("\"", "\\\"")
        val codeBase64 = Base64.encodeToString(code.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

        return """
import sys
import json
import bdb
import traceback
import resource
import threading

class JsonDebugger(bdb.Bdb):
    def __init__(self):
        super().__init__()
        self.breakpoints = {$breakpointSet}
        self._breakpoint_id_map = {}
        for line in self.breakpoints:
            self._breakpoint_id_map[line] = True

    def user_line(self, frame):
        filename = frame.f_code.co_filename
        lineno = frame.f_lineno
        self._send_event("step_complete", {
            "frame": self._frame_info(frame),
            "call_stack": self._call_stack(),
            "locals": self._locals_info(frame)
        })
        self._wait_for_command()

    def user_breakpoint(self, frame, breakpoint):
        filename = frame.f_code.co_filename
        lineno = frame.f_lineno
        self._send_event("breakpoint_hit", {
            "frame": self._frame_info(frame),
            "call_stack": self._call_stack(),
            "locals": self._locals_info(frame)
        })
        self._wait_for_command()

    def user_exception(self, frame, exc_info):
        exc_type, exc_value, exc_tb = exc_info
        self._send_event("exception", {
            "exception_type": exc_type.__name__,
            "message": str(exc_value),
            "traceback": self._traceback_frames(exc_tb)
        })
        self._wait_for_command()

    def _frame_info(self, frame):
        try:
            source_line = linecache.getline(frame.f_code.co_filename, frame.f_lineno).rstrip()
        except:
            source_line = ""
        return {
            "id": id(frame),
            "filename": frame.f_code.co_filename,
            "function": frame.f_code.co_name,
            "lineno": frame.f_lineno,
            "col_offset": getattr(frame, "f_col_offset", 0),
            "source_line": source_line
        }

    def _call_stack(self):
        frames = []
        frame = self.botframe
        while frame is not None:
            frames.append(self._frame_info(frame))
            frame = frame.f_back
        return list(reversed(frames))

    def _locals_info(self, frame):
        result = {}
        for name, value in frame.f_locals.items():
            try:
                result[name] = repr(value)[:200]
            except:
                result[name] = "<unrepresentable>"
        return result

    def _traceback_frames(self, tb):
        frames = []
        while tb is not None:
            frame = tb.tb_frame
            frames.append(self._frame_info(frame))
            tb = tb.tb_next
        return frames

    def _send_event(self, event_type, data):
        event = {"type": event_type}
        event.update(data)
        print(f"__DEBUG_EVENT__:{json.dumps(event)}", flush=True)

    def _wait_for_command(self):
        print("(Pdb) ", end="", flush=True)
        while True:
            try:
                line = input()
                if not line.strip():
                    print("(Pdb) ", end="", flush=True)
                    continue
                self._process_command(line.strip())
                break
            except EOFError:
                self._send_event("execution_finished", {"exit_code": 0})
                return

    def _process_command(self, command):
        parts = command.split(None, 1)
        cmd = parts[0] if parts else ""
        args = parts[1] if len(parts) > 1 else ""

        if cmd in ("c", "continue"):
            self.set_continue()
        elif cmd in ("n", "next"):
            self.set_next(self.botframe)
        elif cmd in ("s", "step"):
            self.set_step()
        elif cmd in ("r", "return"):
            self.set_return(self.botframe)
        elif cmd == "q":
            self._send_event("execution_finished", {"exit_code": 0})
            raise SystemExit(0)
        elif cmd == "p":
            try:
                frame = self.botframe
                result = eval(args, frame.f_globals, frame.f_locals)
                print(repr(result), flush=True)
            except Exception as e:
                print(f"Error: {e}", flush=True)
        elif cmd == "pp":
            try:
                frame = self.botframe
                result = eval(args, frame.f_globals, frame.f_locals)
                print(json.dumps(result, indent=2, default=str)[:500], flush=True)
            except Exception as e:
                print(f"Error: {e}", flush=True)
        elif cmd == "break" or cmd == "b":
            if ":" in args:
                parts_args = args.split(":")
                filename = parts_args[0]
                lineno = int(parts_args[1])
            else:
                filename = self.botframe.f_code.co_filename
                lineno = int(args)
            self.set_break(filename, lineno)
            print(f"Breakpoint set at {filename}:{lineno}", flush=True)
        elif cmd == "clear":
            if ":" in args:
                parts_args = args.split(":")
                filename = parts_args[0]
                lineno = int(parts_args[1])
                self.clear_break(filename, lineno)
                print(f"Breakpoint cleared at {filename}:{lineno}", flush=True)
        else:
            try:
                frame = self.botframe
                exec(command, frame.f_globals, frame.f_locals)
            except Exception as e:
                print(f"Error: {e}", flush=True)

import linecache
import json
import bdb
import sys
import base64
try:
    import resource
except ImportError:
    resource = None
import threading

code = base64.b64decode("$codeBase64").decode("utf-8")

debugger = JsonDebugger()
for line_num in {$breakpointSet}:
    debugger.set_break("$safeFilePath", line_num)

print("__DEBUG_EVENT__:__STARTED__", flush=True)

try:
    compiled = compile(code, "$safeFilePath", "exec")
    debugger.run(compiled, {'__name__': '__main__', '__file__': "$safeFilePath"})
except SystemExit:
    pass
except Exception as e:
    traceback.print_exc()
    print(json.dumps({
        "type": "exception",
        "exception_type": type(e).__name__,
        "message": str(e),
        "traceback": []
    }), flush=True)
finally:
    print(json.dumps({"type": "execution_finished", "exit_code": 0}), flush=True)
""".trimIndent()
    }

    private fun findPythonPath(): String? {
        val paths = listOf(
            "${context.filesDir}/python/python3",
            "${context.filesDir}/python/bin/python3",
            "/system/bin/python3",
            "/usr/bin/python3"
        )
        return paths.firstOrNull { File(it).exists() && File(it).canExecute() }
    }

    private fun addLog(level: LogLevel, message: String) {
        val entry = LogEntry(
            id = UUID.randomUUID().toString(),
            level = level,
            message = message,
            timestamp = System.currentTimeMillis()
        )
        _logs.update { current -> current + entry }
        when (level) {
            LogLevel.ERROR -> scope.launch { _events.emit(DebugEvent.ErrorLogged(message)) }
            LogLevel.WARNING -> scope.launch { _events.emit(DebugEvent.WarningLogged(message)) }
            else -> {}
        }
    }

    private fun cleanup() {
        stdoutReader?.cancel()
        stderrReader?.cancel()
        stdinStream = null
        debugProcess = null
    }

    fun destroy() {
        scope.launch { stopDebugging() }
    }
}
