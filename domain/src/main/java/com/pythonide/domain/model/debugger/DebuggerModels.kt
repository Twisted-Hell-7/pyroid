package com.pythonide.domain.model.debugger

import java.util.UUID

/**
 * Domain models for the Python debugger.
 *
 * These are pure data classes living in the domain layer. The data layer
 * (DebuggerEngine) is responsible for producing them from raw JSON events
 * emitted by the Python debug agent, and the app layer (DebuggerViewModel /
 * DebuggerScreen) consumes them via StateFlows.
 */

// ---------------------------------------------------------------------------
// Breakpoints
// ---------------------------------------------------------------------------

/**
 * A breakpoint set on a source file line.
 *
 * @property id        Stable unique identifier.
 * @property filePath  Absolute path (or virtual path) of the source file.
 * @property lineNumber 1-based line number where execution should pause.
 * @property condition Optional Python boolean expression; when present the
 *                     breakpoint only fires when the expression evaluates truthy.
 * @property enabled   Whether the breakpoint is active.
 * @property hitCount  Number of times this breakpoint has been hit.
 */
data class Breakpoint(
    val id: String = UUID.randomUUID().toString(),
    val filePath: String,
    val lineNumber: Int,
    val condition: String? = null,
    val enabled: Boolean = true,
    val hitCount: Int = 0
)

// ---------------------------------------------------------------------------
// Call stack
// ---------------------------------------------------------------------------

/**
 * A single frame in the call stack at a pause point.
 */
data class StackFrame(
    val id: Int,
    val fileName: String,
    val functionName: String,
    val lineNumber: Int,
    val column: Int = 0,
    val sourceLine: String = "",
    val locals: List<Variable> = emptyList(),
    val isCurrent: Boolean = false
)

// ---------------------------------------------------------------------------
// Variables
// ---------------------------------------------------------------------------

/**
 * A runtime variable or value inspected by the debugger.
 *
 * @property children For compound values (dict, list, object) this holds the
 *                   expanded members; null when the value is not expandable.
 */
data class Variable(
    val name: String,
    val type: String,
    val value: String,
    val repr: String = value,
    val children: List<Variable>? = null
) {
    val isExpandable: Boolean get() = !children.isNullOrEmpty()
}

// ---------------------------------------------------------------------------
// Watch expressions
// ---------------------------------------------------------------------------

data class WatchExpression(
    val id: String = UUID.randomUUID().toString(),
    val expression: String,
    val value: String? = null,
    val type: String? = null,
    val error: String? = null
)

// ---------------------------------------------------------------------------
// Logs
// ---------------------------------------------------------------------------

enum class LogLevel { INFO, WARNING, ERROR, DEBUG }

data class LogEntry(
    val id: String = UUID.randomUUID().toString(),
    val level: LogLevel,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String? = null
)

// ---------------------------------------------------------------------------
// Runtime inspector
// ---------------------------------------------------------------------------

data class RuntimeInfo(
    val memoryUsageMb: Double = 0.0,
    val threadCount: Int = 1,
    val activeModules: List<String> = emptyList(),
    val cpuTimeMs: Long = 0
)

// ---------------------------------------------------------------------------
// Sessions & state
// ---------------------------------------------------------------------------

enum class DebugState {
    IDLE,
    STARTING,
    RUNNING,
    PAUSED,
    STEPPING,
    STOPPED,
    ERROR
}

/**
 * Represents an active or recent debug session.
 */
data class DebugSession(
    val id: String = UUID.randomUUID().toString(),
    val interpreterId: String,
    val filePath: String,
    val breakpoints: List<Breakpoint> = emptyList(),
    val state: DebugState = DebugState.IDLE,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null
)

// ---------------------------------------------------------------------------
// Events (emitted by DebuggerEngine, observed internally)
// ---------------------------------------------------------------------------

/**
 * Sealed hierarchy of debugger events. The engine maps raw JSON events from
 * the Python agent into these types before updating its StateFlows.
 */
sealed class DebugEvent {
    data class BreakpointHit(
        val breakpoint: Breakpoint?,
        val frame: StackFrame,
        val callStack: List<StackFrame>
    ) : DebugEvent()

    data class StepComplete(
        val frame: StackFrame,
        val callStack: List<StackFrame>
    ) : DebugEvent()

    data class ExceptionRaised(
        val type: String,
        val message: String,
        val traceback: List<StackFrame>
    ) : DebugEvent()

    data class ExecutionFinished(val exitCode: Int) : DebugEvent()

    data class OutputLogged(val text: String, val stream: String) : DebugEvent()

    data class ErrorLogged(val message: String, val source: String? = null) : DebugEvent()

    data class WarningLogged(val message: String, val source: String? = null) : DebugEvent()

    data class RuntimeInfoUpdated(val info: RuntimeInfo) : DebugEvent()
}

/**
 * Commands that can be sent to the running debug agent.
 */
sealed interface DebugCommand {
    val wire: String
    data object Continue : DebugCommand { override val wire = "continue" }
    data object StepInto : DebugCommand { override val wire = "step_into" }
    data object StepOver : DebugCommand { override val wire = "step_over" }
    data object StepOut : DebugCommand { override val wire = "step_out" }
    data object Stop : DebugCommand { override val wire = "stop" }
    data object Inspect : DebugCommand { override val wire = "inspect" }
    /** Evaluate [expression] in the current frame. */
    data class Evaluate(val expression: String) : DebugCommand {
        override val wire: String get() = "eval $expression"
    }
}
