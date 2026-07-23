package com.pythonide.domain.repository

import com.pythonide.domain.model.debugger.Breakpoint
import com.pythonide.domain.model.debugger.DebugEvent
import com.pythonide.domain.model.debugger.DebugState
import com.pythonide.domain.model.debugger.LogEntry
import com.pythonide.domain.model.debugger.RuntimeInfo
import com.pythonide.domain.model.debugger.StackFrame
import com.pythonide.domain.model.debugger.Variable
import com.pythonide.domain.model.debugger.WatchExpression
import kotlinx.coroutines.flow.Flow

interface DebuggerRepository {

    fun observeDebugState(): Flow<DebugState>
    fun observeCallStack(): Flow<List<StackFrame>>
    fun observeVariables(): Flow<List<Variable>>
    fun observeWatchExpressions(): Flow<List<WatchExpression>>
    fun observeLogs(): Flow<List<LogEntry>>
    fun observeRuntimeInfo(): Flow<RuntimeInfo>
    fun observeEvents(): Flow<DebugEvent>
    fun observeBreakpoints(): Flow<List<Breakpoint>>

    suspend fun startDebugging(
        filePath: String,
        code: String,
        breakpoints: List<Breakpoint> = emptyList()
    ): Result<Unit>

    suspend fun stopDebugging(): Result<Unit>

    suspend fun continueExecution(): Result<Unit>
    suspend fun stepInto(): Result<Unit>
    suspend fun stepOver(): Result<Unit>
    suspend fun stepOut(): Result<Unit>

    suspend fun addBreakpoint(breakpoint: Breakpoint): Result<Unit>
    suspend fun removeBreakpoint(breakpointId: String): Result<Unit>
    suspend fun toggleBreakpoint(breakpointId: String): Result<Unit>
    suspend fun updateBreakpointCondition(breakpointId: String, condition: String?): Result<Unit>

    suspend fun addWatchExpression(expression: String): Result<Unit>
    suspend fun removeWatchExpression(expressionId: String): Result<Unit>

    suspend fun evaluateExpression(expression: String): Result<Variable>
    suspend fun inspectVariable(name: String): Result<Variable>

    suspend fun clearLogs(): Result<Unit>
}
