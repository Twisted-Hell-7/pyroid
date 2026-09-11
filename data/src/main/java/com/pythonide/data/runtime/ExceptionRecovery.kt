package com.pythonide.data.runtime

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExceptionRecovery @Inject constructor() {
    private val _recoveryState = MutableStateFlow<RecoveryState>(RecoveryState.Idle)
    val recoveryState: StateFlow<RecoveryState> = _recoveryState.asStateFlow()

    private val _retryStats = MutableStateFlow<RetryStats>(RetryStats())
    val retryStats: StateFlow<RetryStats> = _retryStats.asStateFlow()

    private val recoveryHistory = ConcurrentHashMap<String, RecoveryEntry>()
    private val totalRecoveries = AtomicInteger(0)

    sealed class RecoveryState {
        object Idle : RecoveryState()
        data class Retrying(val operation: String, val attempt: Int, val maxAttempts: Int) : RecoveryState()
        data class Recovered(val operation: String, val attempts: Int) : RecoveryState()
        data class Failed(val operation: String, val error: String) : RecoveryState()
    }

    data class RetryConfig(
        val maxRetries: Int = 3,
        val initialDelayMs: Long = 1000,
        val maxDelayMs: Long = 30000,
        val backoffMultiplier: Float = 2f,
        val retryableExceptions: List<Class<out Throwable>> = listOf(
            Exception::class.java
        )
    )

    data class RecoveryEntry(
        val operation: String,
        val attempts: Int,
        val success: Boolean,
        val totalTimeMs: Long,
        val error: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class RetryStats(
        val totalAttempts: Int = 0,
        val successfulRecoveries: Int = 0,
        val failedRecoveries: Int = 0,
        val averageAttempts: Float = 0f
    )

    private val defaultConfig = RetryConfig()

    suspend fun <T> executeWithRecovery(
        operation: String,
        config: RetryConfig = defaultConfig,
        block: suspend () -> T
    ): T {
        var lastException: Throwable? = null
        val startTime = System.currentTimeMillis()

        _recoveryState.value = RecoveryState.Retrying(operation, 0, config.maxRetries)

        repeat(config.maxRetries) { attempt ->
            try {
                val result = block()

                if (attempt > 0) {
                    val entry = RecoveryEntry(
                        operation = operation,
                        attempts = attempt + 1,
                        success = true,
                        totalTimeMs = System.currentTimeMillis() - startTime
                    )
                    recoveryHistory[operation] = entry
                    totalRecoveries.incrementAndGet()

                    _recoveryState.value = RecoveryState.Recovered(operation, attempt + 1)
                    updateStats()
                }

                return result
            } catch (e: Throwable) {
                lastException = e

                if (!isRetryableException(e, config)) {
                    throw e
                }

                _recoveryState.value = RecoveryState.Retrying(operation, attempt + 1, config.maxRetries)

                if (attempt < config.maxRetries - 1) {
                    val delay = calculateDelay(attempt, config)
                    delay(delay)
                }
            }
        }

        val entry = RecoveryEntry(
            operation = operation,
            attempts = config.maxRetries,
            success = false,
            totalTimeMs = System.currentTimeMillis() - startTime,
            error = lastException?.message
        )
        recoveryHistory[operation] = entry

        _recoveryState.value = RecoveryState.Failed(
            operation,
            lastException?.message ?: "Unknown error"
        )

        throw lastException ?: RuntimeException("Recovery failed")
    }

    private fun isRetryableException(throwable: Throwable, config: RetryConfig): Boolean {
        return config.retryableExceptions.any { it.isInstance(throwable) }
    }

    private fun calculateDelay(attempt: Int, config: RetryConfig): Long {
        val delay = config.initialDelayMs * Math.pow(config.backoffMultiplier.toDouble(), attempt.toDouble()).toLong()
        return minOf(delay, config.maxDelayMs)
    }

    private fun updateStats() {
        val entries = recoveryHistory.values.toList()
        val totalAttempts = entries.sumOf { it.attempts }
        val successful = entries.count { it.success }
        val failed = entries.count { !it.success }
        val avgAttempts = if (entries.isNotEmpty()) totalAttempts.toFloat() / entries.size else 0f

        _retryStats.value = RetryStats(
            totalAttempts = totalAttempts,
            successfulRecoveries = successful,
            failedRecoveries = failed,
            averageAttempts = avgAttempts
        )
    }

    fun getRecoveryHistory(): List<RecoveryEntry> {
        return recoveryHistory.values.sortedByDescending { it.timestamp }
    }

    fun getRecoveryHistoryForOperation(operation: String): List<RecoveryEntry> {
        return recoveryHistory.values
            .filter { it.operation == operation }
            .sortedByDescending { it.timestamp }
    }

    fun clearHistory() {
        recoveryHistory.clear()
        totalRecoveries.set(0)
        _retryStats.value = RetryStats()
        _recoveryState.value = RecoveryState.Idle
    }

    fun getSuccessRate(): Float {
        val entries = recoveryHistory.values.toList()
        if (entries.isEmpty()) return 1f
        return entries.count { it.success }.toFloat() / entries.size
    }

    fun getMostFailingOperations(): List<Pair<String, Int>> {
        return recoveryHistory.values
            .filter { !it.success }
            .groupBy { it.operation }
            .map { (operation, entries) -> operation to entries.size }
            .sortedByDescending { it.second }
            .take(5)
    }

    fun createRetryableBlock(
        operation: String,
        config: RetryConfig = defaultConfig,
        block: suspend () -> Unit
    ): suspend () -> Unit {
        return {
            executeWithRecovery(operation, config) {
                block()
            }
        }
    }

    fun <T> createFallbackBlock(
        primary: suspend () -> T,
        fallback: suspend () -> T,
        shouldUseFallback: (Throwable) -> Boolean = { true }
    ): suspend () -> T {
        return {
            try {
                primary()
            } catch (e: Throwable) {
                if (shouldUseFallback(e)) {
                    fallback()
                } else {
                    throw e
                }
            }
        }
    }

    fun createCircuitBreaker(
        failureThreshold: Int = 5,
        recoveryTimeoutMs: Long = 60000
    ): CircuitBreaker {
        return CircuitBreaker(failureThreshold, recoveryTimeoutMs)
    }

    class CircuitBreaker(
        private val failureThreshold: Int,
        private val recoveryTimeoutMs: Long
    ) {
        private val failureCount = AtomicInteger(0)
        private val state = MutableStateFlow<CircuitState>(CircuitState.Closed)
        private var lastFailureTime = 0L

        val circuitState: StateFlow<CircuitState> = state.asStateFlow()

        sealed class CircuitState {
            object Closed : CircuitState()
            object Open : CircuitState()
            object HalfOpen : CircuitState()
        }

        suspend fun <T> execute(block: suspend () -> T): T {
            if (state.value is CircuitState.Open) {
                if (System.currentTimeMillis() - lastFailureTime > recoveryTimeoutMs) {
                    state.value = CircuitState.HalfOpen
                } else {
                    throw CircuitBreakerOpenException("Circuit breaker is open")
                }
            }

            return try {
                val result = block()
                if (state.value is CircuitState.HalfOpen) {
                    state.value = CircuitState.Closed
                    failureCount.set(0)
                }
                result
            } catch (e: Throwable) {
                failureCount.incrementAndGet()
                lastFailureTime = System.currentTimeMillis()

                if (failureCount.get() >= failureThreshold) {
                    state.value = CircuitState.Open
                }

                throw e
            }
        }

        fun reset() {
            failureCount.set(0)
            state.value = CircuitState.Closed
        }
    }

    class CircuitBreakerOpenException(message: String) : RuntimeException(message)
}
