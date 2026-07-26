package com.pythonide.data.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExceptionRecoveryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var exceptionRecovery: ExceptionRecovery

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        exceptionRecovery = ExceptionRecovery()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // §10: Concurrency - Exception Recovery

    @Test
    fun testExecuteWithRecoverySuccess() = runTest {
        val result = exceptionRecovery.executeWithRecovery("test_operation") {
            "success"
        }
        assertEquals("success", result)
    }

    @Test
    fun testExecuteWithRecoveryRetry() = runTest {
        var attemptCount = 0
        val result = exceptionRecovery.executeWithRecovery("test_operation") {
            attemptCount++
            if (attemptCount < 3) throw RuntimeException("fail")
            "success"
        }
        assertEquals("success", result)
        assertEquals(3, attemptCount)
    }

    @Test
    fun testExecuteWithRecoveryMaxRetriesExceeded() = runTest {
        try {
            exceptionRecovery.executeWithRecovery(
                "test_operation",
                config = ExceptionRecovery.RetryConfig(maxRetries = 3)
            ) {
                throw RuntimeException("always fail")
            }
            fail("Should have thrown exception")
        } catch (e: RuntimeException) {
            assertEquals("always fail", e.message)
        }
    }

    @Test
    fun testExecuteWithRecoveryNonRetryableException() = runTest {
        class NonRetryableException : Exception("non-retryable")
        
        try {
            exceptionRecovery.executeWithRecovery(
                "test_operation",
                config = ExceptionRecovery.RetryConfig(
                    maxRetries = 3,
                    retryableExceptions = listOf(RuntimeException::class.java)
                )
            ) {
                throw NonRetryableException()
            }
            fail("Should have thrown exception")
        } catch (e: NonRetryableException) {
            // Expected
        }
    }

    @Test
    fun testRecoveryState() = runTest {
        var attempts = 0
        exceptionRecovery.executeWithRecovery("test_operation") {
            attempts++
            if (attempts < 2) throw RuntimeException("fail")
            "success"
        }
        
        val state = exceptionRecovery.recoveryState.value
        assertTrue(state is ExceptionRecovery.RecoveryState.Idle || 
                   state is ExceptionRecovery.RecoveryState.Recovered)
    }

    @Test
    fun testRetryStats() = runTest {
        var attempts = 0
        exceptionRecovery.executeWithRecovery("test_operation") {
            attempts++
            if (attempts < 2) throw RuntimeException("fail")
            "success"
        }
        
        val stats = exceptionRecovery.retryStats.value
        assertTrue(stats.totalAttempts > 0)
        assertTrue(stats.successfulRecoveries > 0)
    }

    @Test
    fun testRecoveryHistory() = runTest {
        var attempts = 0
        exceptionRecovery.executeWithRecovery("test_operation") {
            attempts++
            if (attempts < 2) throw RuntimeException("fail")
            "success"
        }
        
        val history = exceptionRecovery.getRecoveryHistory()
        assertTrue(history.isNotEmpty())
        assertEquals("test_operation", history.first().operation)
        assertTrue(history.first().success)
    }

    @Test
    fun testRecoveryHistoryForOperation() = runTest {
        var attemptsA = 0
        exceptionRecovery.executeWithRecovery("operation_a") {
            attemptsA++
            if (attemptsA < 2) throw RuntimeException("fail")
            "success"
        }
        var attemptsB = 0
        exceptionRecovery.executeWithRecovery("operation_b") {
            attemptsB++
            if (attemptsB < 2) throw RuntimeException("fail")
            "success"
        }
        
        val historyA = exceptionRecovery.getRecoveryHistoryForOperation("operation_a")
        assertEquals(1, historyA.size)
        assertEquals("operation_a", historyA.first().operation)
    }

    @Test
    fun testClearHistory() = runTest {
        exceptionRecovery.executeWithRecovery("test_operation") {
            "success"
        }
        
        exceptionRecovery.clearHistory()
        
        assertTrue(exceptionRecovery.getRecoveryHistory().isEmpty())
        assertEquals(0, exceptionRecovery.retryStats.value.totalAttempts)
    }

    @Test
    fun testSuccessRate() = runTest {
        exceptionRecovery.executeWithRecovery("operation_1") {
            "success"
        }
        exceptionRecovery.executeWithRecovery("operation_2") {
            "success"
        }
        
        val successRate = exceptionRecovery.getSuccessRate()
        assertEquals(1.0f, successRate, 0.01f)
    }

    @Test
    fun testMostFailingOperations() = runTest {
        repeat(5) {
            try {
                exceptionRecovery.executeWithRecovery("failing_operation") {
                    throw RuntimeException("fail")
                }
            } catch (e: Exception) {
                // Expected
            }
        }
        
        val failingOps = exceptionRecovery.getMostFailingOperations()
        assertTrue(failingOps.isNotEmpty())
        assertEquals("failing_operation", failingOps.first().first)
    }

    // §10: Concurrency - Circuit Breaker

    @Test
    fun testCircuitBreakerClosed() = runTest {
        val circuitBreaker = exceptionRecovery.createCircuitBreaker(
            failureThreshold = 3,
            recoveryTimeoutMs = 1000
        )
        
        assertEquals(
            ExceptionRecovery.CircuitBreaker.CircuitState.Closed,
            circuitBreaker.circuitState.value
        )
    }

    @Test
    fun testCircuitBreakerOpens() = runTest {
        val circuitBreaker = exceptionRecovery.createCircuitBreaker(
            failureThreshold = 3,
            recoveryTimeoutMs = 1000
        )
        
        repeat(3) {
            try {
                circuitBreaker.execute {
                    throw RuntimeException("fail")
                }
            } catch (e: Exception) {
                // Expected
            }
        }
        
        assertEquals(
            ExceptionRecovery.CircuitBreaker.CircuitState.Open,
            circuitBreaker.circuitState.value
        )
    }

    @Test
    fun testCircuitBreakerRejectsWhenOpen() = runTest {
        val circuitBreaker = exceptionRecovery.createCircuitBreaker(
            failureThreshold = 3,
            recoveryTimeoutMs = 60000
        )
        
        repeat(3) {
            try {
                circuitBreaker.execute {
                    throw RuntimeException("fail")
                }
            } catch (e: Exception) {
                // Expected
            }
        }
        
        try {
            circuitBreaker.execute {
                "should not execute"
            }
            fail("Should have thrown CircuitBreakerOpenException")
        } catch (e: ExceptionRecovery.CircuitBreakerOpenException) {
            // Expected
        }
    }

    @Test
    fun testCircuitBreakerReset() = runTest {
        val circuitBreaker = exceptionRecovery.createCircuitBreaker(
            failureThreshold = 3,
            recoveryTimeoutMs = 1000
        )
        
        repeat(3) {
            try {
                circuitBreaker.execute {
                    throw RuntimeException("fail")
                }
            } catch (e: Exception) {
                // Expected
            }
        }
        
        circuitBreaker.reset()
        
        assertEquals(
            ExceptionRecovery.CircuitBreaker.CircuitState.Closed,
            circuitBreaker.circuitState.value
        )
    }

    @Test
    fun testCircuitBreakerSuccessResetsCount() = runTest {
        val circuitBreaker = exceptionRecovery.createCircuitBreaker(
            failureThreshold = 3,
            recoveryTimeoutMs = 1000
        )
        
        // Fail twice
        repeat(2) {
            try {
                circuitBreaker.execute {
                    throw RuntimeException("fail")
                }
            } catch (e: Exception) {
                // Expected
            }
        }
        
        // Succeed once - should reset failure count
        circuitBreaker.execute {
            "success"
        }
        
        // Should still be closed
        assertEquals(
            ExceptionRecovery.CircuitBreaker.CircuitState.Closed,
            circuitBreaker.circuitState.value
        )
    }

    // §10: Concurrency - Retry Config

    @Test
    fun testRetryConfigDefaults() {
        val config = ExceptionRecovery.RetryConfig()
        assertEquals(3, config.maxRetries)
        assertEquals(1000L, config.initialDelayMs)
        assertEquals(30000L, config.maxDelayMs)
        assertEquals(2f, config.backoffMultiplier)
        assertTrue(config.retryableExceptions.isNotEmpty())
    }

    @Test
    fun testRetryConfigCustom() {
        val config = ExceptionRecovery.RetryConfig(
            maxRetries = 5,
            initialDelayMs = 500,
            maxDelayMs = 10000,
            backoffMultiplier = 1.5f,
            retryableExceptions = listOf(RuntimeException::class.java)
        )
        assertEquals(5, config.maxRetries)
        assertEquals(500L, config.initialDelayMs)
        assertEquals(10000L, config.maxDelayMs)
        assertEquals(1.5f, config.backoffMultiplier)
        assertEquals(1, config.retryableExceptions.size)
    }

    // §10: Concurrency - Recovery Entry

    @Test
    fun testRecoveryEntryCreation() {
        val entry = ExceptionRecovery.RecoveryEntry(
            operation = "test_operation",
            attempts = 3,
            success = true,
            totalTimeMs = 1500,
            error = null
        )
        assertEquals("test_operation", entry.operation)
        assertEquals(3, entry.attempts)
        assertTrue(entry.success)
        assertEquals(1500L, entry.totalTimeMs)
        assertNull(entry.error)
        assertTrue(entry.timestamp > 0)
    }

    @Test
    fun testRecoveryEntryWithError() {
        val entry = ExceptionRecovery.RecoveryEntry(
            operation = "test_operation",
            attempts = 3,
            success = false,
            totalTimeMs = 1500,
            error = "Connection refused"
        )
        assertFalse(entry.success)
        assertEquals("Connection refused", entry.error)
    }

    // §10: Concurrency - Create Retryable Block

    @Test
    fun testCreateRetryableBlock() = runTest {
        var attemptCount = 0
        val block = exceptionRecovery.createRetryableBlock("test_operation") {
            attemptCount++
            if (attemptCount < 3) throw RuntimeException("fail")
        }
        
        block()
        assertEquals(3, attemptCount)
    }

    // §10: Concurrency - Create Fallback Block

    @Test
    fun testCreateFallbackBlockPrimary() = runTest {
        val block = exceptionRecovery.createFallbackBlock(
            primary = { "primary" },
            fallback = { "fallback" }
        )
        
        val result = block()
        assertEquals("primary", result)
    }

    @Test
    fun testCreateFallbackBlockFallback() = runTest {
        val block = exceptionRecovery.createFallbackBlock(
            primary = { throw RuntimeException("fail") },
            fallback = { "fallback" }
        )
        
        val result = block()
        assertEquals("fallback", result)
    }

    @Test
    fun testCreateFallbackBlockConditional() = runTest {
        val block = exceptionRecovery.createFallbackBlock(
            primary = { throw RuntimeException("fail") },
            fallback = { "fallback" },
            shouldUseFallback = { it is RuntimeException }
        )
        
        val result = block()
        assertEquals("fallback", result)
    }

    @Test
    fun testCreateFallbackBlockNoFallback() = runTest {
        class CustomException : Exception("custom")
        
        val block = exceptionRecovery.createFallbackBlock(
            primary = { throw CustomException() },
            fallback = { "fallback" },
            shouldUseFallback = { it is RuntimeException }
        )
        
        try {
            block()
            fail("Should have thrown exception")
        } catch (e: CustomException) {
            // Expected
        }
    }
}
