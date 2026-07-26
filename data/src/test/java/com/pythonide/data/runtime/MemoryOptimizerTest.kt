package com.pythonide.data.runtime

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
class MemoryOptimizerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var crashPrevention: CrashPrevention

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        crashPrevention = CrashPrevention()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        crashPrevention.stopWatchdog()
    }

    // §1: Performance - Memory Monitoring

    @Test
    fun testMemoryReport() {
        val report = crashPrevention.getMemoryReport()
        
        assertTrue(report.usedHeap > 0)
        assertTrue(report.maxHeap > 0)
        assertTrue(report.usagePercent in 0..100)
        assertTrue(report.threadCount > 0)
        assertTrue(report.timestamp > 0)
    }

    @Test
    fun testMemoryReportFormattedString() {
        val report = crashPrevention.getMemoryReport()
        val formatted = report.toFormattedString()
        
        assertTrue(formatted.contains("Heap:"))
        assertTrue(formatted.contains("Free:"))
        assertTrue(formatted.contains("Native:"))
        assertTrue(formatted.contains("Threads:"))
        assertTrue(formatted.contains("Low Memory:"))
    }

    @Test
    fun testLowMemoryDetection() {
        val report = crashPrevention.getMemoryReport()
        // On test environment, should not be low memory
        assertFalse(report.isLowMemory)
    }

    // §1: Performance - Watchdog

    @Test
    fun testWatchdogStartStop() {
        crashPrevention.startWatchdog()
        
        val watchdogState = crashPrevention.watchdogState.value
        assertTrue(watchdogState is CrashPrevention.WatchdogState.Active)
        
        crashPrevention.stopWatchdog()
        
        val stoppedState = crashPrevention.watchdogState.value
        assertTrue(stoppedState is CrashPrevention.WatchdogState.Inactive)
    }

    @Test
    fun testWatchdogConfig() {
        val config = CrashPrevention.WatchdogConfig(
            checkIntervalMs = 1000,
            maxMemoryUsagePercent = 80,
            maxCpuTimeMs = 30_000,
            maxThreadCount = 100,
            enableAutoRecovery = false
        )
        
        assertEquals(1000L, config.checkIntervalMs)
        assertEquals(80, config.maxMemoryUsagePercent)
        assertEquals(30_000L, config.maxCpuTimeMs)
        assertEquals(100, config.maxThreadCount)
        assertFalse(config.enableAutoRecovery)
    }

    // §1: Performance - Resource State

    @Test
    fun testResourceState() {
        val state = CrashPrevention.ResourceState(
            usedHeap = 1024 * 1024,
            maxHeap = 256 * 1024 * 1024,
            nativeHeap = 512 * 1024,
            threadCount = 10,
            isMemoryLow = false,
            isCpuHigh = false
        )
        
        assertEquals(1024 * 1024L, state.usedHeap)
        assertEquals(256 * 1024 * 1024L, state.maxHeap)
        assertEquals(512 * 1024L, state.nativeHeap)
        assertEquals(10, state.threadCount)
        assertFalse(state.isMemoryLow)
        assertFalse(state.isCpuHigh)
    }

    // §1: Performance - Emergency Cleanup

    @Test
    fun testEmergencyCleanup() {
        crashPrevention.performEmergencyCleanup()
        // Should not throw
    }

    // §1: Performance - Crash State Transitions

    @Test
    fun testCrashStateNormal() {
        val state = crashPrevention.crashState.value
        assertTrue(state is CrashPrevention.CrashState.Normal)
    }

    @Test
    fun testCrashStateWarning() {
        // Trigger warning state (4-10 errors)
        repeat(5) {
            crashPrevention.handleUncaughtException(
                Thread.currentThread(),
                RuntimeException("Error $it")
            )
        }
        
        val state = crashPrevention.crashState.value
        assertTrue(state is CrashPrevention.CrashState.Warning)
    }

    @Test
    fun testCrashStateCritical() {
        // Trigger critical state (>10 errors)
        repeat(15) {
            crashPrevention.handleUncaughtException(
                Thread.currentThread(),
                RuntimeException("Error $it")
            )
        }
        
        val state = crashPrevention.crashState.value
        assertTrue(state is CrashPrevention.CrashState.Critical)
    }

    @Test
    fun testShouldPreventExecution() {
        // Initially should not prevent execution
        assertFalse(crashPrevention.shouldPreventExecution())
        
        // After many crashes, should prevent execution
        repeat(15) {
            crashPrevention.handleUncaughtException(
                Thread.currentThread(),
                RuntimeException("Error $it")
            )
        }
        
        assertTrue(crashPrevention.shouldPreventExecution())
    }

    // §1: Performance - Crash History

    @Test
    fun testCrashHistorySize() {
        repeat(200) {
            crashPrevention.handleUncaughtException(
                Thread.currentThread(),
                RuntimeException("Error $it")
            )
        }
        
        val history = crashPrevention.getCrashHistory()
        assertTrue(history.size <= 100) // Max history size
    }

    @Test
    fun testGetRecentCrashes() {
        repeat(20) {
            crashPrevention.handleUncaughtException(
                Thread.currentThread(),
                RuntimeException("Error $it")
            )
        }
        
        val recent = crashPrevention.getRecentCrashes(5)
        assertEquals(5, recent.size)
    }

    @Test
    fun testClearCrashHistory() {
        repeat(10) {
            crashPrevention.handleUncaughtException(
                Thread.currentThread(),
                RuntimeException("Error $it")
            )
        }
        
        crashPrevention.clearCrashHistory()
        
        assertTrue(crashPrevention.getCrashHistory().isEmpty())
        assertEquals(0, crashPrevention.getErrorCount())
        assertTrue(crashPrevention.crashState.value is CrashPrevention.CrashState.Normal)
    }

    // §1: Performance - Error Handler

    @Test
    fun testCreateErrorHandler() {
        val handler = crashPrevention.createErrorHandler()
        assertNotNull(handler)
    }

    // §1: Performance - Safe Execute with Retries

    @Test
    fun testSafeExecuteSuccess() {
        val result = crashPrevention.safeExecute(
            block = { "success" },
            fallback = { "fallback" }
        )
        assertEquals("success", result)
    }

    @Test
    fun testSafeExecuteFallback() {
        val result = crashPrevention.safeExecute(
            block = { throw RuntimeException("fail") },
            fallback = { "fallback" }
        )
        assertEquals("fallback", result)
    }

    @Test
    fun testSafeExecuteWithRetries() {
        var attemptCount = 0
        val result = crashPrevention.safeExecute(
            block = {
                attemptCount++
                if (attemptCount < 3) throw RuntimeException("fail")
                "success"
            },
            fallback = { "fallback" },
            maxRetries = 5
        )
        assertEquals("success", result)
        assertEquals(3, attemptCount)
    }

    // §1: Performance - WeakReferenceHolder

    @Test
    fun testWeakReferenceHolder() {
        val holder = MemoryOptimizer.WeakReferenceHolder<String>()
        
        holder.add("item1")
        holder.add("item2")
        
        val items = holder.get()
        assertEquals(2, items.size)
        assertTrue(items.contains("item1"))
        assertTrue(items.contains("item2"))
    }

    @Test
    fun testWeakReferenceHolderClear() {
        val holder = MemoryOptimizer.WeakReferenceHolder<String>()
        
        holder.add("item1")
        holder.add("item2")
        
        holder.clear()
        
        val items = holder.get()
        assertTrue(items.isEmpty())
    }

    // §1: Performance - Crash Event

    @Test
    fun testCrashEventCreation() {
        val event = CrashPrevention.CrashEvent(
            type = CrashPrevention.CrashType.OUT_OF_MEMORY,
            message = "Out of memory",
            stackTrace = "at com.example.Main.main(Main.kt:10)",
            resourceSnapshot = CrashPrevention.ResourceState()
        )
        
        assertEquals(CrashPrevention.CrashType.OUT_OF_MEMORY, event.type)
        assertEquals("Out of memory", event.message)
        assertNotNull(event.stackTrace)
        assertTrue(event.timestamp > 0)
    }

    // §1: Performance - Crash Type Classification

    @Test
    fun testCrashTypeOutOfMemory() {
        val error = OutOfMemoryError("Out of memory")
        crashPrevention.handleUncaughtException(Thread.currentThread(), error)
        
        val history = crashPrevention.getCrashHistory()
        assertEquals(CrashPrevention.CrashType.OUT_OF_MEMORY, history.first().type)
    }

    @Test
    fun testCrashTypeStackOverflow() {
        val error = StackOverflowError("Stack overflow")
        crashPrevention.handleUncaughtException(Thread.currentThread(), error)
        
        val history = crashPrevention.getCrashHistory()
        assertEquals(CrashPrevention.CrashType.STACK_OVERFLOW, history.first().type)
    }

    @Test
    fun testCrashTypeThreadDeath() {
        val error = ThreadDeath()
        crashPrevention.handleUncaughtException(Thread.currentThread(), error)
        
        val history = crashPrevention.getCrashHistory()
        assertEquals(CrashPrevention.CrashType.THREAD_DEATH, history.first().type)
    }

    @Test
    fun testCrashTypeUncaughtException() {
        val exception = RuntimeException("Test exception")
        crashPrevention.handleUncaughtException(Thread.currentThread(), exception)
        
        val history = crashPrevention.getCrashHistory()
        assertEquals(CrashPrevention.CrashType.UNCAUGHT_EXCEPTION, history.first().type)
    }

    @Test
    fun testCrashTypeWatchdogTimeout() {
        val exception = RuntimeException("watchdog timeout detected")
        crashPrevention.handleUncaughtException(Thread.currentThread(), exception)
        
        val history = crashPrevention.getCrashHistory()
        assertEquals(CrashPrevention.CrashType.WATCHDOG_TIMEOUT, history.first().type)
    }
}
