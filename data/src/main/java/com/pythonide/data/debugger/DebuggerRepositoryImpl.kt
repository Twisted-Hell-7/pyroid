package com.pythonide.data.debugger

import com.pythonide.domain.model.debugger.Breakpoint
import com.pythonide.domain.model.debugger.DebugEvent
import com.pythonide.domain.model.debugger.DebugState
import com.pythonide.domain.model.debugger.LogEntry
import com.pythonide.domain.model.debugger.RuntimeInfo
import com.pythonide.domain.model.debugger.StackFrame
import com.pythonide.domain.model.debugger.Variable
import com.pythonide.domain.model.debugger.WatchExpression
import com.pythonide.domain.repository.DebuggerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebuggerRepositoryImpl @Inject constructor(
    private val engine: DebuggerEngine
) : DebuggerRepository {

    override fun observeDebugState(): Flow<DebugState> = engine.debugState
    override fun observeCallStack(): Flow<List<StackFrame>> = engine.callStack
    override fun observeVariables(): Flow<List<Variable>> = engine.variables
    override fun observeWatchExpressions(): Flow<List<WatchExpression>> = engine.watchExpressions
    override fun observeLogs(): Flow<List<LogEntry>> = engine.logs
    override fun observeRuntimeInfo(): Flow<RuntimeInfo> = engine.runtimeInfo
    override fun observeEvents(): Flow<DebugEvent> = engine.events
    override fun observeBreakpoints(): Flow<List<Breakpoint>> = engine.breakpoints

    override suspend fun startDebugging(
        filePath: String,
        code: String,
        breakpoints: List<Breakpoint>
    ): Result<Unit> = engine.startDebugging(filePath, code, breakpoints)

    override suspend fun stopDebugging(): Result<Unit> = engine.stopDebugging()

    override suspend fun continueExecution(): Result<Unit> = engine.continueExecution()
    override suspend fun stepInto(): Result<Unit> = engine.stepInto()
    override suspend fun stepOver(): Result<Unit> = engine.stepOver()
    override suspend fun stepOut(): Result<Unit> = engine.stepOut()

    override suspend fun addBreakpoint(breakpoint: Breakpoint): Result<Unit> =
        engine.addBreakpoint(breakpoint)

    override suspend fun removeBreakpoint(breakpointId: String): Result<Unit> =
        engine.removeBreakpoint(breakpointId)

    override suspend fun toggleBreakpoint(breakpointId: String): Result<Unit> =
        engine.toggleBreakpoint(breakpointId)

    override suspend fun updateBreakpointCondition(breakpointId: String, condition: String?): Result<Unit> =
        engine.updateBreakpointCondition(breakpointId, condition)

    override suspend fun addWatchExpression(expression: String): Result<Unit> =
        engine.addWatchExpression(expression)

    override suspend fun removeWatchExpression(expressionId: String): Result<Unit> =
        engine.removeWatchExpression(expressionId)

    override suspend fun evaluateExpression(expression: String): Result<Variable> =
        engine.evaluateExpression(expression)

    override suspend fun inspectVariable(name: String): Result<Variable> =
        engine.inspectVariable(name)

    override suspend fun clearLogs(): Result<Unit> = engine.clearLogs()
}
