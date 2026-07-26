package com.pythonide.data.debugger

import com.pythonide.domain.model.debugger.Breakpoint
import com.pythonide.domain.model.debugger.DebugEvent
import com.pythonide.domain.model.debugger.DebugState
import com.pythonide.domain.model.debugger.LogLevel
import com.pythonide.domain.model.debugger.StackFrame
import com.pythonide.domain.model.debugger.Variable
import com.pythonide.domain.model.debugger.WatchExpression
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebuggerProtocolTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // §4: Debugger - Breakpoint Management

    @Test
    fun testBreakpointCreation() {
        val breakpoint = Breakpoint(
            filePath = "/test/file.py",
            lineNumber = 10
        )
        assertEquals("/test/file.py", breakpoint.filePath)
        assertEquals(10, breakpoint.lineNumber)
        assertTrue(breakpoint.enabled)
        assertNull(breakpoint.condition)
        assertEquals(0, breakpoint.hitCount)
    }

    @Test
    fun testBreakpointWithCondition() {
        val breakpoint = Breakpoint(
            filePath = "/test/file.py",
            lineNumber = 10,
            condition = "x > 5"
        )
        assertEquals("x > 5", breakpoint.condition)
    }

    @Test
    fun testBreakpointDisabled() {
        val breakpoint = Breakpoint(
            filePath = "/test/file.py",
            lineNumber = 10,
            enabled = false
        )
        assertFalse(breakpoint.enabled)
    }

    @Test
    fun testBreakpointHitCount() {
        val breakpoint = Breakpoint(
            filePath = "/test/file.py",
            lineNumber = 10,
            hitCount = 5
        )
        assertEquals(5, breakpoint.hitCount)
    }

    // §4: Debugger - Stack Frame

    @Test
    fun testStackFrameCreation() {
        val frame = StackFrame(
            id = 1,
            fileName = "/test/file.py",
            functionName = "main",
            lineNumber = 10,
            column = 0,
            sourceLine = "print('hello')"
        )
        assertEquals(1, frame.id)
        assertEquals("/test/file.py", frame.fileName)
        assertEquals("main", frame.functionName)
        assertEquals(10, frame.lineNumber)
        assertEquals(0, frame.column)
        assertEquals("print('hello')", frame.sourceLine)
    }

    @Test
    fun testStackFrameWithLocals() {
        val locals = listOf(
            Variable(name = "x", type = "int", value = "42"),
            Variable(name = "y", type = "str", value = "hello")
        )
        val frame = StackFrame(
            id = 1,
            fileName = "/test/file.py",
            functionName = "main",
            lineNumber = 10,
            locals = locals
        )
        assertEquals(2, frame.locals.size)
        assertEquals("x", frame.locals[0].name)
        assertEquals("42", frame.locals[0].value)
    }

    // §4: Debugger - Variable Inspection

    @Test
    fun testVariableCreation() {
        val variable = Variable(
            name = "myVar",
            type = "int",
            value = "42"
        )
        assertEquals("myVar", variable.name)
        assertEquals("int", variable.type)
        assertEquals("42", variable.value)
        assertFalse(variable.isExpandable)
    }

    @Test
    fun testVariableWithChildren() {
        val children = listOf(
            Variable(name = "key1", type = "str", value = "value1"),
            Variable(name = "key2", type = "int", value = "100")
        )
        val variable = Variable(
            name = "myDict",
            type = "dict",
            value = "{'key1': 'value1', 'key2': 100}",
            children = children
        )
        assertTrue(variable.isExpandable)
        assertEquals(2, variable.children?.size)
    }

    @Test
    fun testVariableEmptyChildren() {
        val variable = Variable(
            name = "myList",
            type = "list",
            value = "[]",
            children = emptyList()
        )
        assertFalse(variable.isExpandable)
    }

    // §4: Debugger - Watch Expressions

    @Test
    fun testWatchExpressionCreation() {
        val watch = WatchExpression(
            expression = "x + y"
        )
        assertNotNull(watch.id)
        assertEquals("x + y", watch.expression)
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
        assertNull(watch.error)
    }

    @Test
    fun testWatchExpressionWithError() {
        val watch = WatchExpression(
            expression = "undefined_var",
            error = "NameError: name 'undefined_var' is not defined"
        )
        assertNull(watch.value)
        assertNotNull(watch.error)
    }

    // §4: Debugger - Debug State

    @Test
    fun testDebugStateEnumValues() {
        val values = DebugState.entries
        assertEquals(7, values.size)
        assertTrue(values.contains(DebugState.IDLE))
        assertTrue(values.contains(DebugState.STARTING))
        assertTrue(values.contains(DebugState.RUNNING))
        assertTrue(values.contains(DebugState.PAUSED))
        assertTrue(values.contains(DebugState.STEPPING))
        assertTrue(values.contains(DebugState.STOPPED))
        assertTrue(values.contains(DebugState.ERROR))
    }

    // §4: Debugger - Debug Events

    @Test
    fun testDebugEventBreakpointHit() {
        val frame = StackFrame(1, "/test.py", "main", 10)
        val callStack = listOf(frame)
        val event = DebugEvent.BreakpointHit(
            breakpoint = Breakpoint(filePath = "/test.py", lineNumber = 10),
            frame = frame,
            callStack = callStack
        )
        assertNotNull(event.breakpoint)
        assertEquals(frame, event.frame)
        assertEquals(callStack, event.callStack)
    }

    @Test
    fun testDebugEventStepComplete() {
        val frame = StackFrame(1, "/test.py", "main", 10)
        val callStack = listOf(frame)
        val event = DebugEvent.StepComplete(frame, callStack)
        assertEquals(frame, event.frame)
        assertEquals(callStack, event.callStack)
    }

    @Test
    fun testDebugEventExceptionRaised() {
        val traceback = listOf(StackFrame(1, "/test.py", "main", 10))
        val event = DebugEvent.ExceptionRaised(
            type = "ValueError",
            message = "invalid value",
            traceback = traceback
        )
        assertEquals("ValueError", event.type)
        assertEquals("invalid value", event.message)
        assertEquals(traceback, event.traceback)
    }

    @Test
    fun testDebugEventExecutionFinished() {
        val event = DebugEvent.ExecutionFinished(exitCode = 0)
        assertEquals(0, event.exitCode)
    }

    @Test
    fun testDebugEventExecutionFinishedWithError() {
        val event = DebugEvent.ExecutionFinished(exitCode = 1)
        assertEquals(1, event.exitCode)
    }

    @Test
    fun testDebugEventOutputLogged() {
        val event = DebugEvent.OutputLogged(text = "Hello, World!", stream = "stdout")
        assertEquals("Hello, World!", event.text)
        assertEquals("stdout", event.stream)
    }

    @Test
    fun testDebugEventErrorLogged() {
        val event = DebugEvent.ErrorLogged(message = "Error occurred")
        assertEquals("Error occurred", event.message)
    }

    @Test
    fun testDebugEventWarningLogged() {
        val event = DebugEvent.WarningLogged(message = "Warning message")
        assertEquals("Warning message", event.message)
    }

    @Test
    fun testDebugEventRuntimeInfoUpdated() {
        val info = com.pythonide.domain.model.debugger.RuntimeInfo(
            memoryUsageMb = 10.5,
            threadCount = 4,
            activeModules = listOf("sys", "os")
        )
        val event = DebugEvent.RuntimeInfoUpdated(info)
        assertEquals(10.5, event.info.memoryUsageMb, 0.01)
        assertEquals(4, event.info.threadCount)
        assertEquals(2, event.info.activeModules.size)
    }

    // §4: Debugger - Log Levels

    @Test
    fun testLogLevelEnumValues() {
        val values = LogLevel.entries
        assertEquals(4, values.size)
        assertTrue(values.contains(LogLevel.INFO))
        assertTrue(values.contains(LogLevel.WARNING))
        assertTrue(values.contains(LogLevel.ERROR))
        assertTrue(values.contains(LogLevel.DEBUG))
    }

    @Test
    fun testLogEntryCreation() {
        val entry = com.pythonide.domain.model.debugger.LogEntry(
            level = LogLevel.INFO,
            message = "Test message"
        )
        assertNotNull(entry.id)
        assertEquals(LogLevel.INFO, entry.level)
        assertEquals("Test message", entry.message)
        assertTrue(entry.timestamp > 0)
    }

    // §4: Debugger - Runtime Info

    @Test
    fun testRuntimeInfoCreation() {
        val info = com.pythonide.domain.model.debugger.RuntimeInfo()
        assertEquals(0.0, info.memoryUsageMb, 0.01)
        assertEquals(1, info.threadCount)
        assertTrue(info.activeModules.isEmpty())
        assertEquals(0L, info.cpuTimeMs)
    }

    @Test
    fun testRuntimeInfoWithValues() {
        val info = com.pythonide.domain.model.debugger.RuntimeInfo(
            memoryUsageMb = 25.5,
            threadCount = 8,
            activeModules = listOf("sys", "os", "json"),
            cpuTimeMs = 15000
        )
        assertEquals(25.5, info.memoryUsageMb, 0.01)
        assertEquals(8, info.threadCount)
        assertEquals(3, info.activeModules.size)
        assertEquals(15000L, info.cpuTimeMs)
    }

    // §4: Debugger - Debug Session

    @Test
    fun testDebugSessionCreation() {
        val session = com.pythonide.domain.model.debugger.DebugSession(
            interpreterId = "interp_1",
            filePath = "/test/file.py"
        )
        assertNotNull(session.id)
        assertEquals("interp_1", session.interpreterId)
        assertEquals("/test/file.py", session.filePath)
        assertTrue(session.breakpoints.isEmpty())
        assertEquals(DebugState.IDLE, session.state)
        assertTrue(session.startedAt > 0)
        assertNull(session.endedAt)
    }

    @Test
    fun testDebugSessionWithBreakpoints() {
        val breakpoints = listOf(
            Breakpoint(filePath = "/test/file.py", lineNumber = 10),
            Breakpoint(filePath = "/test/file.py", lineNumber = 20)
        )
        val session = com.pythonide.domain.model.debugger.DebugSession(
            interpreterId = "interp_1",
            filePath = "/test/file.py",
            breakpoints = breakpoints,
            state = DebugState.RUNNING
        )
        assertEquals(2, session.breakpoints.size)
        assertEquals(DebugState.RUNNING, session.state)
    }

    // §4: Debugger - Debug Commands

    @Test
    fun testDebugCommandWireValues() {
        assertEquals("continue", com.pythonide.domain.model.debugger.DebugCommand.CONTINUE.wire)
        assertEquals("step_into", com.pythonide.domain.model.debugger.DebugCommand.STEP_INTO.wire)
        assertEquals("step_over", com.pythonide.domain.model.debugger.DebugCommand.STEP_OVER.wire)
        assertEquals("step_out", com.pythonide.domain.model.debugger.DebugCommand.STEP_OUT.wire)
        assertEquals("stop", com.pythonide.domain.model.debugger.DebugCommand.STOP.wire)
        assertEquals("inspect", com.pythonide.domain.model.debugger.DebugCommand.INSPECT.wire)
    }

    @Test
    fun testDebugCommandEvaluate() {
        val evaluate = com.pythonide.domain.model.debugger.DebugCommand.Evaluate("x + 1")
        assertEquals("eval x + 1", evaluate.wire)
    }

    // §4: Debugger - Deep Nesting Inspection

    @Test
    fun testDeeplyNestedVariable() {
        // Create deeply nested variable structure
        var current: Variable? = Variable(
            name = "level10",
            type = "dict",
            value = "{...}",
            children = emptyList()
        )
        
        for (i in 9 downTo 1) {
            current = Variable(
                name = "level$i",
                type = "dict",
                value = "{...}",
                children = listOf(current!!)
            )
        }
        
        assertNotNull(current)
        assertTrue(current!!.isExpandable)
        assertEquals("level1", current.name)
    }

    // §4: Debugger - Large Variable Display

    @Test
    fun testLargeListVariable() {
        val children = (1..100_000).map { i ->
            Variable(name = "item_$i", type = "int", value = "$i")
        }
        val variable = Variable(
            name = "bigList",
            type = "list",
            value = "[1, 2, 3, ...]",
            children = children
        )
        assertEquals(100_000, variable.children?.size)
        assertTrue(variable.isExpandable)
    }

    // §4: Debugger - Conditional Breakpoint

    @Test
    fun testConditionalBreakpoint() {
        val breakpoint = Breakpoint(
            filePath = "/test/file.py",
            lineNumber = 10,
            condition = "x > 5 and y < 10"
        )
        assertEquals("x > 5 and y < 10", breakpoint.condition)
    }

    // §4: Debugger - Multiple Breakpoints

    @Test
    fun testMultipleBreakpoints() {
        val breakpoints = (1..500).map { i ->
            Breakpoint(
                filePath = "/test/file_$i.py",
                lineNumber = i
            )
        }
        assertEquals(500, breakpoints.size)
        breakpoints.forEachIndexed { index, breakpoint ->
            assertEquals(index + 1, breakpoint.lineNumber)
        }
    }
}
