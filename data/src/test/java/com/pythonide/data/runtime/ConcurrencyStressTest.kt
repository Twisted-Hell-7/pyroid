package com.pythonide.data.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class ConcurrencyStressTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // §10: Concurrency & Thread Safety

    @Test
    fun testConcurrentCrashPreventionAccess() = runBlocking {
        val crashPrevention = CrashPrevention()
        val iterations = 100
        val errors = AtomicInteger(0)

        val jobs = (1..iterations).map { i ->
            async(Dispatchers.Default) {
                try {
                    crashPrevention.handleUncaughtException(
                        Thread.currentThread(),
                        RuntimeException("Error $i")
                    )
                } catch (e: Exception) {
                    errors.incrementAndGet()
                }
            }
        }

        jobs.awaitAll()
        assertEquals(0, errors.get())
        assertEquals(iterations, crashPrevention.getErrorCount())
    }

    @Test
    fun testConcurrentSafeExecute() = runBlocking {
        val crashPrevention = CrashPrevention()
        val successCount = AtomicInteger(0)
        val fallbackCount = AtomicInteger(0)

        val jobs = (1..50).map { i ->
            async(Dispatchers.Default) {
                val result = crashPrevention.safeExecute(
                    block = {
                        if (i % 3 == 0) throw RuntimeException("Fail $i")
                        "success"
                    },
                    fallback = { fallbackCount.incrementAndGet(); "fallback" }
                )
                if (result == "success") successCount.incrementAndGet()
            }
        }

        jobs.awaitAll()
        assertTrue(successCount.get() > 0)
        assertTrue(fallbackCount.get() > 0)
    }

    @Test
    fun testConcurrentSafeSuspendExecute() = runBlocking {
        val crashPrevention = CrashPrevention()
        val results = ConcurrentHashMap<Int, String>()

        val jobs = (1..50).map { i ->
            async(Dispatchers.Default) {
                val result = crashPrevention.safeSuspendExecute(
                    block = {
                        if (i % 5 == 0) throw RuntimeException("Fail $i")
                        "success_$i"
                    },
                    fallback = { "fallback_$i" }
                )
                results[i] = result
            }
        }

        jobs.awaitAll()
        assertEquals(50, results.size)
        results.values.forEach { result ->
            assertTrue(result.startsWith("success_") || result.startsWith("fallback_"))
        }
    }

    @Test
    fun testConcurrentErrorCounting() = runBlocking {
        val crashPrevention = CrashPrevention()
        val threadCount = 20
        val errorsPerThread = 10

        val jobs = (1..threadCount).map { threadId ->
            launch(Dispatchers.Default) {
                repeat(errorsPerThread) { errorId ->
                    crashPrevention.handleUncaughtException(
                        Thread.currentThread(),
                        RuntimeException("Thread $threadId Error $errorId")
                    )
                }
            }
        }

        jobs.forEach { it.join() }
        assertEquals(threadCount * errorsPerThread, crashPrevention.getErrorCount())
    }

    @Test
    fun testConcurrentCrashHistoryAccess() = runBlocking {
        val crashPrevention = CrashPrevention()
        val readCount = AtomicInteger(0)
        val writeCount = AtomicInteger(0)

        val writerJobs = (1..10).map { i ->
            launch(Dispatchers.Default) {
                repeat(20) {
                    crashPrevention.handleUncaughtException(
                        Thread.currentThread(),
                        RuntimeException("Error ${i}_$it")
                    )
                    writeCount.incrementAndGet()
                }
            }
        }

        val readerJobs = (1..10).map { i ->
            launch(Dispatchers.Default) {
                repeat(20) {
                    crashPrevention.getCrashHistory()
                    crashPrevention.getRecentCrashes(5)
                    crashPrevention.getErrorCount()
                    readCount.incrementAndGet()
                }
            }
        }

        (writerJobs + readerJobs).forEach { it.join() }
        assertTrue(writeCount.get() > 0)
        assertTrue(readCount.get() > 0)
    }

    @Test
    fun testConcurrentClearAndRecord() = runBlocking {
        val crashPrevention = CrashPrevention()
        val iterations = 50

        val jobs = (1..iterations).map { i ->
            launch(Dispatchers.Default) {
                if (i % 2 == 0) {
                    crashPrevention.clearCrashHistory()
                } else {
                    crashPrevention.handleUncaughtException(
                        Thread.currentThread(),
                        RuntimeException("Error $i")
                    )
                }
            }
        }

        jobs.forEach { it.join() }
        // No crash = pass - concurrent clear/record should be thread-safe
        val history = crashPrevention.getCrashHistory()
        assertTrue(history.size <= 100) // Max history size
    }

    @Test
    fun testConcurrentExceptionRecovery() = runBlocking {
        val exceptionRecovery = ExceptionRecovery()
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        val jobs = (1..30).map { i ->
            async(Dispatchers.Default) {
                try {
                    exceptionRecovery.executeWithRecovery("operation_$i") {
                        if (i % 4 == 0) throw RuntimeException("Fail $i")
                        "success"
                    }
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                }
            }
        }

        jobs.awaitAll()
        assertTrue(successCount.get() > 0)
        // Some may fail after retries
    }

    @Test
    fun testConcurrentCircuitBreaker() = runBlocking {
        val exceptionRecovery = ExceptionRecovery()
        val circuitBreaker = exceptionRecovery.createCircuitBreaker(
            failureThreshold = 5,
            recoveryTimeoutMs = 1000
        )
        val openCount = AtomicInteger(0)

        val jobs = (1..20).map { i ->
            async(Dispatchers.Default) {
                try {
                    circuitBreaker.execute {
                        if (i <= 10) throw RuntimeException("Fail $i")
                        "success"
                    }
                } catch (e: Exception) {
                    openCount.incrementAndGet()
                }
            }
        }

        jobs.awaitAll()
        // Circuit breaker should open after 5 failures
        assertTrue(openCount.get() > 0)
    }

    @Test
    fun testConcurrentCrashPreventionState() = runBlocking {
        val crashPrevention = CrashPrevention()
        val stateChanges = AtomicInteger(0)

        val jobs = (1..20).map { i ->
            launch(Dispatchers.Default) {
                repeat(10) {
                    crashPrevention.handleUncaughtException(
                        Thread.currentThread(),
                        RuntimeException("Error ${i}_$it")
                    )
                    stateChanges.incrementAndGet()
                }
            }
        }

        jobs.forEach { it.join() }
        assertTrue(stateChanges.get() > 0)
        // State should be consistent
        val state = crashPrevention.crashState.value
        assertTrue(state is CrashPrevention.CrashState.Normal || 
                   state is CrashPrevention.CrashState.Warning || 
                   state is CrashPrevention.CrashState.Critical)
    }

    @Test
    fun testConcurrentMemoryReport() = runBlocking {
        val crashPrevention = CrashPrevention()
        val reports = ConcurrentHashMap<Int, CrashPrevention.MemoryReport>()

        val jobs = (1..20).map { i ->
            async(Dispatchers.Default) {
                reports[i] = crashPrevention.getMemoryReport()
            }
        }

        jobs.awaitAll()
        assertEquals(20, reports.size)
        reports.values.forEach { report ->
            assertTrue(report.usedHeap > 0)
            assertTrue(report.maxHeap > 0)
            assertTrue(report.usagePercent in 0..100)
        }
    }

    @Test
    fun testConcurrentWatchdogStartStop() = runBlocking {
        val crashPrevention = CrashPrevention()
        val iterations = 10

        val jobs = (1..iterations).map { i ->
            launch(Dispatchers.Default) {
                if (i % 2 == 0) {
                    crashPrevention.startWatchdog()
                } else {
                    crashPrevention.stopWatchdog()
                }
            }
        }

        jobs.forEach { it.join() }
        crashPrevention.stopWatchdog() // Cleanup
    }

    @Test
    fun testConcurrentSafeExecuteWithRetries() = runBlocking {
        val crashPrevention = CrashPrevention()
        val attemptCounts = ConcurrentHashMap<Int, AtomicInteger>()
        val completionCount = AtomicInteger(0)

        val jobs = (1..20).map { i ->
            attemptCounts[i] = AtomicInteger(0)
            async(Dispatchers.Default) {
                crashPrevention.safeExecute(
                    block = {
                        attemptCounts[i]!!.incrementAndGet()
                        if (attemptCounts[i]!!.get() < 3) throw RuntimeException("Retry $i")
                        "success"
                    },
                    fallback = { "fallback" },
                    maxRetries = 5
                )
                completionCount.incrementAndGet()
            }
        }

        jobs.awaitAll()
        assertEquals(20, completionCount.get())
    }

    @Test
    fun testConcurrentErrorHandlerCreation() = runBlocking {
        val crashPrevention = CrashPrevention()
        val handlers = ConcurrentHashMap<Int, kotlinx.coroutines.CoroutineExceptionHandler>()

        val jobs = (1..20).map { i ->
            async(Dispatchers.Default) {
                handlers[i] = crashPrevention.createErrorHandler()
            }
        }

        jobs.awaitAll()
        assertEquals(20, handlers.size)
        handlers.values.forEach { handler ->
            assertNotNull(handler)
        }
    }
}
