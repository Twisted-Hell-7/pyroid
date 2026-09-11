package com.pythonide.domain.model.debugger

import org.junit.Assert.*
import org.junit.Test

class DebuggerModelsTest {

    // --- Breakpoint ---

    @Test
    fun testBreakpointDefaults() {
        val bp = Breakpoint(filePath = "test.py", lineNumber = 10)
        assertTrue(bp.id.isNotEmpty())
        assertEquals("test.py", bp.filePath)
        assertEquals(10, bp.lineNumber)
        assertNull(bp.condition)
        assertTrue(bp.enabled)
        assertEquals(0, bp.hitCount)
    }

    @Test
    fun testBreakpointWithCondition() {
        val bp = Breakpoint(
            filePath = "test.py",
            lineNumber = 5,
            condition = "x > 10"
        )
        assertEquals("x > 10", bp.condition)
    }

    @Test
    fun testBreakpointDisabled() {
        val bp = Breakpoint(filePath = "test.py", lineNumber = 1, enabled = false)
        assertFalse(bp.enabled)
    }

    // --- StackFrame ---

    @Test
    fun testStackFrameDefaults() {
        val frame = StackFrame(
            id = 1,
            fileName = "test.py",
            functionName = "foo",
            lineNumber = 10
        )
        assertEquals(0, frame.column)
        assertEquals("", frame.sourceLine)
        assertTrue(frame.locals.isEmpty())
        assertFalse(frame.isCurrent)
    }

    // --- Variable ---

    @Test
    fun testVariableSimple() {
        val v = Variable(name = "x", type = "int", value = "42")
        assertEquals("x", v.name)
        assertEquals("int", v.type)
        assertEquals("42", v.value)
        assertNull(v.children)
        assertFalse(v.isExpandable)
    }

    @Test
    fun testVariableExpandable() {
        val child = Variable(name = "item", type = "int", value = "1")
        val parent = Variable(
            name = "list",
            type = "list",
            value = "[1]",
            children = listOf(child)
        )
        assertTrue(parent.isExpandable)
        assertNotNull(parent.children)
        assertEquals(1, parent.children!!.size)
    }

    @Test
    fun testVariableEmptyChildren() {
        val v = Variable(name = "x", type = "list", value = "[]", children = emptyList())
        assertFalse(v.isExpandable)
    }

    // --- WatchExpression ---

    @Test
    fun testWatchExpressionDefaults() {
        val watch = WatchExpression(expression = "x + 1")
        assertTrue(watch.id.isNotEmpty())
        assertEquals("x + 1", watch.expression)
        assertNull(watch.value)
        assertNull(watch.type)
        assertNull(watch.error)
    }

    @Test
    fun testWatchExpressionWithValue() {
        val watch = WatchExpression(
            expression = "x",
            value = "42",
            type = "int"
        )
        assertEquals("42", watch.value)
        assertEquals("int", watch.type)
    }

    @Test
    fun testWatchExpressionWithError() {
        val watch = WatchExpression(
            expression = "undefined_var",
            error = "NameError: name 'undefined_var' is not defined"
        )
        assertNotNull(watch.error)
        assertNull(watch.value)
    }

    // --- LogLevel ---

    @Test
    fun testLogLevelValues() {
        assertEquals(4, LogLevel.entries.size)
        assertTrue(LogLevel.entries.contains(LogLevel.INFO))
        assertTrue(LogLevel.entries.contains(LogLevel.WARNING))
        assertTrue(LogLevel.entries.contains(LogLevel.ERROR))
        assertTrue(LogLevel.entries.contains(LogLevel.DEBUG))
    }

    // --- LogEntry ---

    @Test
    fun testLogEntryDefaults() {
        val entry = LogEntry(level = LogLevel.INFO, message = "test")
        assertTrue(entry.id.isNotEmpty())
        assertEquals(LogLevel.INFO, entry.level)
        assertEquals("test", entry.message)
        assertTrue(entry.timestamp > 0)
        assertNull(entry.source)
    }

    // --- RuntimeInfo ---

    @Test
    fun testRuntimeInfoDefaults() {
        val info = RuntimeInfo()
        assertEquals(0.0, info.memoryUsageMb, 0.001)
        assertEquals(1, info.threadCount)
        assertTrue(info.activeModules.isEmpty())
        assertEquals(0L, info.cpuTimeMs)
    }

    // --- DebugState ---

    @Test
    fun testDebugStateValues() {
        assertEquals(7, DebugState.entries.size)
        assertTrue(DebugState.entries.contains(DebugState.IDLE))
        assertTrue(DebugState.entries.contains(DebugState.STARTING))
        assertTrue(DebugState.entries.contains(DebugState.RUNNING))
        assertTrue(DebugState.entries.contains(DebugState.PAUSED))
        assertTrue(DebugState.entries.contains(DebugState.STEPPING))
        assertTrue(DebugState.entries.contains(DebugState.STOPPED))
        assertTrue(DebugState.entries.contains(DebugState.ERROR))
    }

    // --- DebugSession ---

    @Test
    fun testDebugSessionDefaults() {
        val session = DebugSession(
            interpreterId = "py3.14",
            filePath = "test.py"
        )
        assertTrue(session.id.isNotEmpty())
        assertEquals("py3.14", session.interpreterId)
        assertEquals("test.py", session.filePath)
        assertTrue(session.breakpoints.isEmpty())
        assertEquals(DebugState.IDLE, session.state)
        assertTrue(session.startedAt > 0)
        assertNull(session.endedAt)
    }

    // --- DebugEvent ---

    @Test
    fun testDebugEventBreakpointHit() {
        val frame = StackFrame(id = 1, fileName = "test.py", functionName = "foo", lineNumber = 10)
        val event = DebugEvent.BreakpointHit(
            breakpoint = Breakpoint(filePath = "test.py", lineNumber = 10),
            frame = frame,
            callStack = listOf(frame)
        )
        assertNotNull(event.breakpoint)
        assertEquals(frame, event.frame)
    }

    @Test
    fun testDebugEventStepComplete() {
        val frame = StackFrame(id = 1, fileName = "test.py", functionName = "foo", lineNumber = 10)
        val event = DebugEvent.StepComplete(frame = frame, callStack = listOf(frame))
        assertEquals(frame, event.frame)
    }

    @Test
    fun testDebugEventExceptionRaised() {
        val event = DebugEvent.ExceptionRaised(
            type = "ValueError",
            message = "invalid literal",
            traceback = emptyList()
        )
        assertEquals("ValueError", event.type)
        assertEquals("invalid literal", event.message)
    }

    @Test
    fun testDebugEventExecutionFinished() {
        val event = DebugEvent.ExecutionFinished(exitCode = 0)
        assertEquals(0, event.exitCode)
    }

    @Test
    fun testDebugEventOutputLogged() {
        val event = DebugEvent.OutputLogged(text = "hello", stream = "stdout")
        assertEquals("hello", event.text)
        assertEquals("stdout", event.stream)
    }

    @Test
    fun testDebugEventErrorLogged() {
        val event = DebugEvent.ErrorLogged(message = "error occurred")
        assertEquals("error occurred", event.message)
    }

    @Test
    fun testDebugEventWarningLogged() {
        val event = DebugEvent.WarningLogged(message = "warning")
        assertEquals("warning", event.message)
    }

    @Test
    fun testDebugEventRuntimeInfoUpdated() {
        val info = RuntimeInfo(memoryUsageMb = 64.0, threadCount = 4)
        val event = DebugEvent.RuntimeInfoUpdated(info = info)
        assertEquals(64.0, event.info.memoryUsageMb, 0.001)
    }

    // --- DebugCommand ---

    @Test
    fun testDebugCommandWire() {
        assertEquals("continue", DebugCommand.Continue.wire)
        assertEquals("step_into", DebugCommand.StepInto.wire)
        assertEquals("step_over", DebugCommand.StepOver.wire)
        assertEquals("step_out", DebugCommand.StepOut.wire)
        assertEquals("stop", DebugCommand.Stop.wire)
        assertEquals("inspect", DebugCommand.Inspect.wire)
    }

    @Test
    fun testDebugCommandEvaluate() {
        val eval = DebugCommand.Evaluate("x + 1")
        assertEquals("eval x + 1", eval.wire)
    }
}
