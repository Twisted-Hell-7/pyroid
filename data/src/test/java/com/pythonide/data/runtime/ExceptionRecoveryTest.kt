package com.pythonide.data.runtime

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ExceptionRecoveryTest {

    private lateinit var exceptionRecovery: ExceptionRecovery

    @Before
    fun setup() {
        exceptionRecovery = ExceptionRecovery()
    }

    @Test
    fun `test executeWithRecovery succeeds on first attempt`() = runBlocking {
        val result = exceptionRecovery.executeWithRecovery(
            operation = "test_op",
            config = ExceptionRecovery.RetryConfig(maxRetries = 3)
        ) {
            "success"
        }

        assertEquals("success", result)
    }

    @Test
    fun `test executeWithRecovery retries on failure`() = runBlocking {
        var attempts = 0

        val result = exceptionRecovery.executeWithRecovery(
            operation = "retry_op",
            config = ExceptionRecovery.RetryConfig(maxRetries = 3, initialDelayMs = 10)
        ) {
            attempts++
            if (attempts < 3) {
                throw RuntimeException("fail")
            }
            "success after retries"
        }

        assertEquals("success after retries", result)
        assertEquals(3, attempts)
    }

    @Test(expected = RuntimeException::class)
    fun `test executeWithRecovery throws after max retries`() = runBlocking {
        exceptionRecovery.executeWithRecovery(
            operation = "fail_op",
            config = ExceptionRecovery.RetryConfig(maxRetries = 2, initialDelayMs = 10)
        ) {
            throw RuntimeException("always fail")
        }
    }

    @Test
    fun `test recovery history is recorded`() = runBlocking {
        exceptionRecovery.executeWithRecovery(
            operation = "recorded_op",
            config = ExceptionRecovery.RetryConfig(maxRetries = 1)
        ) {
            "done"
        }

        val history = exceptionRecovery.getRecoveryHistory()
        assertTrue(history.isNotEmpty())
        assertTrue(history.first().success)
    }

    @Test
    fun `test clear history resets state`() = runBlocking {
        exceptionRecovery.executeWithRecovery(
            operation = "clear_op",
            config = ExceptionRecovery.RetryConfig(maxRetries = 1)
        ) {
            "done"
        }

        exceptionRecovery.clearHistory()

        assertTrue(exceptionRecovery.getRecoveryHistory().isEmpty())
        assertEquals(0, exceptionRecovery.retryStats.value.totalAttempts)
    }

    @Test
    fun `test get success rate returns correct value`() = runBlocking {
        exceptionRecovery.executeWithRecovery(
            operation = "success_op",
            config = ExceptionRecovery.RetryConfig(maxRetries = 1)
        ) {
            "done"
        }

        val rate = exceptionRecovery.getSuccessRate()
        assertTrue(rate > 0f)
    }

    @Test
    fun `test createFallbackBlock uses primary on success`() = runBlocking {
        val block = exceptionRecovery.createFallbackBlock(
            primary = { "primary" },
            fallback = { "fallback" }
        )

        assertEquals("primary", block())
    }

    @Test
    fun `test createFallbackBlock uses fallback on failure`() = runBlocking {
        val block = exceptionRecovery.createFallbackBlock(
            primary = { throw RuntimeException("fail") },
            fallback = { "fallback" }
        )

        assertEquals("fallback", block())
    }

    @Test
    fun `test circuit breaker opens after failures`() = runBlocking {
        val breaker = exceptionRecovery.createCircuitBreaker(
            failureThreshold = 2,
            recoveryTimeoutMs = 60000
        )

        repeat(2) {
            try {
                breaker.execute {
                    throw RuntimeException("fail")
                }
            } catch (e: Exception) {
                // Expected
            }
        }

        assertEquals(
            ExceptionRecovery.CircuitBreaker.CircuitState.Open,
            breaker.circuitState.value
        )
    }

    @Test(expected = ExceptionRecovery.CircuitBreakerOpenException::class)
    fun `test circuit breaker throws when open`() = runBlocking {
        val breaker = exceptionRecovery.createCircuitBreaker(
            failureThreshold = 1,
            recoveryTimeoutMs = 60000
        )

        try {
            breaker.execute {
                throw RuntimeException("fail")
            }
        } catch (e: Exception) {
            // Expected
        }

        breaker.execute {
            "should not reach"
        }
    }

    @Test
    fun `test circuit breaker resets`() = runBlocking {
        val breaker = exceptionRecovery.createCircuitBreaker(
            failureThreshold = 1,
            recoveryTimeoutMs = 60000
        )

        try {
            breaker.execute {
                throw RuntimeException("fail")
            }
        } catch (e: Exception) {
            // Expected
        }

        breaker.reset()

        assertEquals(
            ExceptionRecovery.CircuitBreaker.CircuitState.Closed,
            breaker.circuitState.value
        )
    }

    @Test
    fun `test retry stats track attempts`() = runBlocking {
        exceptionRecovery.executeWithRecovery(
            operation = "stats_op",
            config = ExceptionRecovery.RetryConfig(maxRetries = 1)
        ) {
            "done"
        }

        val stats = exceptionRecovery.retryStats.value
        assertTrue(stats.totalAttempts > 0)
    }
}
