package com.pythonide.data.runtime

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CrashPreventionTest {

    private lateinit var crashPrevention: CrashPrevention

    @Before
    fun setup() {
        crashPrevention = CrashPrevention()
    }

    @Test
    fun `test initial state is Normal`() {
        val state = crashPrevention.crashState.value
        assertTrue(state is CrashPrevention.CrashState.Normal)
    }

    @Test
    fun `test handleUncaughtException records crash`() {
        val exception = RuntimeException("Test exception")

        crashPrevention.handleUncaughtException(Thread.currentThread(), exception)

        val history = crashPrevention.getCrashHistory()
        assertTrue(history.isNotEmpty())
        assertEquals(CrashPrevention.CrashType.UNCAUGHT_EXCEPTION, history.first().type)
    }

    @Test
    fun `test handle OutOfMemoryError classifies as OUT_OF_MEMORY`() {
        val error = OutOfMemoryError("Out of memory")

        crashPrevention.handleUncaughtException(Thread.currentThread(), error)

        val history = crashPrevention.getCrashHistory()
        assertEquals(CrashPrevention.CrashType.OUT_OF_MEMORY, history.first().type)
    }

    @Test
    fun `test handle StackOverflowError classifies as STACK_OVERFLOW`() {
        val error = StackOverflowError("Stack overflow")

        crashPrevention.handleUncaughtException(Thread.currentThread(), error)

        val history = crashPrevention.getCrashHistory()
        assertEquals(CrashPrevention.CrashType.STACK_OVERFLOW, history.first().type)
    }

    @Test
    fun `test error count increases with crashes`() {
        crashPrevention.handleUncaughtException(Thread.currentThread(), RuntimeException("1"))
        crashPrevention.handleUncaughtException(Thread.currentThread(), RuntimeException("2"))

        assertEquals(2, crashPrevention.getErrorCount())
    }

    @Test
    fun `test clear crash history resets state`() {
        crashPrevention.handleUncaughtException(Thread.currentThread(), RuntimeException("test"))
        crashPrevention.clearCrashHistory()

        assertTrue(crashPrevention.getCrashHistory().isEmpty())
        assertEquals(0, crashPrevention.getErrorCount())
        assertTrue(crashPrevention.crashState.value is CrashPrevention.CrashState.Normal)
    }

    @Test
    fun `test safeExecute returns result on success`() {
        val result = crashPrevention.safeExecute(
            block = { "success" },
            fallback = { "fallback" }
        )

        assertEquals("success", result)
    }

    @Test
    fun `test safeExecute returns fallback on failure`() {
        val result = crashPrevention.safeExecute(
            block = { throw RuntimeException("fail") },
            fallback = { "fallback" }
        )

        assertEquals("fallback", result)
    }

    @Test
    fun `test safeSuspendExecute returns result on success`() = runBlocking {
        val result = crashPrevention.safeSuspendExecute(
            block = { "success" },
            fallback = { "fallback" }
        )

        assertEquals("success", result)
    }

    @Test
    fun `test safeSuspendExecute returns fallback on failure`() = runBlocking {
        val result = crashPrevention.safeSuspendExecute(
            block = { throw RuntimeException("fail") },
            fallback = { "fallback" }
        )

        assertEquals("fallback", result)
    }

    @Test
    fun `test createErrorHandler returns handler`() {
        val handler = crashPrevention.createErrorHandler()

        assertNotNull(handler)
    }

    @Test
    fun `test performEmergencyCleanup does not throw`() {
        crashPrevention.performEmergencyCleanup()
    }

    @Test
    fun `test getMemoryReport returns valid data`() {
        val report = crashPrevention.getMemoryReport()

        assertTrue(report.usedHeap > 0)
        assertTrue(report.maxHeap > 0)
        assertTrue(report.usagePercent in 0..100)
    }

    @Test
    fun `test recent crashes returns limited count`() {
        repeat(20) {
            crashPrevention.handleUncaughtException(Thread.currentThread(), RuntimeException("$it"))
        }

        val recent = crashPrevention.getRecentCrashes(5)
        assertEquals(5, recent.size)
    }

    @Test
    fun `test shouldPreventExecution is false initially`() {
        assertFalse(crashPrevention.shouldPreventExecution())
    }
}
