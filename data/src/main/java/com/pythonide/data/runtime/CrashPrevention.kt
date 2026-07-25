package com.pythonide.data.runtime

import android.os.Debug
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashPrevention @Inject constructor() {
    private val _crashState = MutableStateFlow<CrashState>(CrashState.Normal)
    val crashState: StateFlow<CrashState> = _crashState.asStateFlow()

    private val _watchdogState = MutableStateFlow<WatchdogState>(WatchdogState.Inactive)
    val watchdogState: StateFlow<WatchdogState> = _watchdogState.asStateFlow()

    private val _resourceState = MutableStateFlow(ResourceState())
    val resourceState: StateFlow<ResourceState> = _resourceState.asStateFlow()

    private val crashHistory = ConcurrentLinkedQueue<CrashEvent>()
    private val errorCount = AtomicInteger(0)
    private val isWatchdogActive = AtomicBoolean(false)
    private var watchdogJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    sealed class CrashState {
        object Normal : CrashState()
        data class Warning(val message: String, val count: Int) : CrashState()
        data class Critical(val message: String, val lastCrash: CrashEvent) : CrashState()
        object Recovering : CrashState()
    }

    sealed class WatchdogState {
        object Inactive : WatchdogState()
        object Active : WatchdogState()
        data class Triggered(val reason: String) : WatchdogState()
    }

    data class ResourceState(
        val usedHeap: Long = 0,
        val maxHeap: Long = Runtime.getRuntime().maxMemory(),
        val nativeHeap: Long = Debug.getNativeHeapAllocatedSize(),
        val threadCount: Int = Thread.activeCount(),
        val isMemoryLow: Boolean = false,
        val isCpuHigh: Boolean = false
    )

    data class CrashEvent(
        val timestamp: Long = System.currentTimeMillis(),
        val type: CrashType,
        val message: String,
        val stackTrace: String?,
        val resourceSnapshot: ResourceState
    )

    enum class CrashType {
        OUT_OF_MEMORY,
        STACK_OVERFLOW,
        THREAD_DEATH,
        UNCAUGHT_EXCEPTION,
        WATCHDOG_TIMEOUT,
        RESOURCE_EXHAUSTION,
        USER_ERROR
    }

    data class WatchdogConfig(
        val checkIntervalMs: Long = 5000,
        val maxMemoryUsagePercent: Int = 85,
        val maxCpuTimeMs: Long = 30_000,
        val maxThreadCount: Int = 50,
        val enableAutoRecovery: Boolean = true
    )

    private var watchdogConfig = WatchdogConfig()

    fun startWatchdog(config: WatchdogConfig = WatchdogConfig()) {
        watchdogConfig = config

        if (isWatchdogActive.compareAndSet(false, true)) {
            _watchdogState.value = WatchdogState.Active

            watchdogJob = CoroutineScope(Dispatchers.Default).launch {
                while (isActive && isWatchdogActive.get()) {
                    checkResources()
                    delay(watchdogConfig.checkIntervalMs)
                }
            }
        }
    }

    fun stopWatchdog() {
        isWatchdogActive.set(false)
        watchdogJob?.cancel()
        watchdogJob = null
        _watchdogState.value = WatchdogState.Inactive
    }

    private fun checkResources() {
        val runtime = Runtime.getRuntime()
        val usedHeap = runtime.totalMemory() - runtime.freeMemory()
        val maxHeap = runtime.maxMemory()
        val memoryPercent = (usedHeap.toFloat() / maxHeap * 100).toInt()
        val nativeHeap = Debug.getNativeHeapAllocatedSize()
        val threadCount = Thread.activeCount()

        val resourceState = ResourceState(
            usedHeap = usedHeap,
            maxHeap = maxHeap,
            nativeHeap = nativeHeap,
            threadCount = threadCount,
            isMemoryLow = memoryPercent > watchdogConfig.maxMemoryUsagePercent,
            isCpuHigh = false
        )

        _resourceState.value = resourceState

        if (resourceState.isMemoryLow) {
            _watchdogState.value = WatchdogState.Triggered("Memory usage at $memoryPercent%")
            handleResourceExhaustion(resourceState)
        }

        if (threadCount > watchdogConfig.maxThreadCount) {
            _watchdogState.value = WatchdogState.Triggered("Thread count: $threadCount")
        }
    }

    private fun handleResourceExhaustion(state: ResourceState) {
        if (watchdogConfig.enableAutoRecovery) {
            performEmergencyCleanup()
        }

        val crashEvent = CrashEvent(
            type = CrashType.RESOURCE_EXHAUSTION,
            message = "Resource exhaustion detected",
            stackTrace = null,
            resourceSnapshot = state
        )

        recordCrash(crashEvent)
    }

    fun performEmergencyCleanup() {
        System.gc()
        System.runFinalization()

        mainHandler.postDelayed({
            System.gc()
        }, 100)
    }

    fun handleUncaughtException(thread: Thread, throwable: Throwable) {
        val crashType = classifyCrash(throwable)
        val stringWriter = StringWriter()
        throwable.printStackTrace(PrintWriter(stringWriter))

        val crashEvent = CrashEvent(
            type = crashType,
            message = throwable.message ?: "Unknown error",
            stackTrace = stringWriter.toString(),
            resourceSnapshot = _resourceState.value
        )

        recordCrash(crashEvent)
        updateCrashState(crashEvent)
    }

    private fun classifyCrash(throwable: Throwable): CrashType {
        return when {
            throwable is OutOfMemoryError -> CrashType.OUT_OF_MEMORY
            throwable is StackOverflowError -> CrashType.STACK_OVERFLOW
            throwable is ThreadDeath -> CrashType.THREAD_DEATH
            throwable.message?.contains("watchdog", ignoreCase = true) == true -> CrashType.WATCHDOG_TIMEOUT
            else -> CrashType.UNCAUGHT_EXCEPTION
        }
    }

    private fun recordCrash(event: CrashEvent) {
        crashHistory.add(event)
        errorCount.incrementAndGet()

        if (crashHistory.size > 100) {
            crashHistory.poll()
        }
    }

    private fun updateCrashState(event: CrashEvent) {
        val count = errorCount.get()

        _crashState.value = when {
            count > 10 -> CrashState.Critical(
                message = "Multiple crashes detected",
                lastCrash = event
            )
            count > 3 -> CrashState.Warning(
                message = "Frequent errors detected",
                count = count
            )
            else -> CrashState.Normal
        }
    }

    fun shouldPreventExecution(): Boolean {
        val state = _crashState.value
        return state is CrashState.Critical
    }

    fun getCrashHistory(): List<CrashEvent> {
        return crashHistory.toList().sortedByDescending { it.timestamp }
    }

    fun getRecentCrashes(count: Int = 10): List<CrashEvent> {
        return getCrashHistory().take(count)
    }

    fun clearCrashHistory() {
        crashHistory.clear()
        errorCount.set(0)
        _crashState.value = CrashState.Normal
    }

    fun getErrorCount(): Int = errorCount.get()

    fun createErrorHandler(): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            handleUncaughtException(Thread.currentThread(), throwable)
        }
    }

    fun <T> safeExecute(
        block: () -> T,
        fallback: (Throwable) -> T,
        maxRetries: Int = 3
    ): T {
        var lastException: Throwable? = null

        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Throwable) {
                lastException = e
                handleUncaughtException(Thread.currentThread(), e)

                if (e is OutOfMemoryError) {
                    performEmergencyCleanup()
                }

                if (attempt < maxRetries - 1) {
                    Thread.sleep(100L * (attempt + 1))
                }
            }
        }

        return fallback(lastException ?: RuntimeException("Unknown error"))
    }

    suspend fun <T> safeSuspendExecute(
        block: suspend () -> T,
        fallback: suspend (Throwable) -> T,
        maxRetries: Int = 3
    ): T {
        var lastException: Throwable? = null

        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Throwable) {
                lastException = e
                handleUncaughtException(Thread.currentThread(), e)

                if (e is OutOfMemoryError) {
                    performEmergencyCleanup()
                }

                if (attempt < maxRetries - 1) {
                    delay(100L * (attempt + 1))
                }
            }
        }

        return fallback(lastException ?: RuntimeException("Unknown error"))
    }

    fun getMemoryReport(): MemoryReport {
        val runtime = Runtime.getRuntime()
        val usedHeap = runtime.totalMemory() - runtime.freeMemory()
        val maxHeap = runtime.maxMemory()

        return MemoryReport(
            usedHeap = usedHeap,
            maxHeap = maxHeap,
            freeHeap = runtime.freeMemory(),
            nativeHeap = Debug.getNativeHeapAllocatedSize(),
            maxNativeHeap = Debug.getNativeHeapSize(),
            usagePercent = (usedHeap.toFloat() / maxHeap * 100).toInt(),
            isLowMemory = usedHeap.toFloat() / maxHeap > 0.85f,
            threadCount = Thread.activeCount(),
            timestamp = System.currentTimeMillis()
        )
    }

    data class MemoryReport(
        val usedHeap: Long,
        val maxHeap: Long,
        val freeHeap: Long,
        val nativeHeap: Long,
        val maxNativeHeap: Long,
        val usagePercent: Int,
        val isLowMemory: Boolean,
        val threadCount: Int,
        val timestamp: Long
    ) {
        fun toFormattedString(): String {
            return """
                Memory Report:
                Heap: ${usedHeap / 1024 / 1024}MB / ${maxHeap / 1024 / 1024}MB ($usagePercent%)
                Free: ${freeHeap / 1024 / 1024}MB
                Native: ${nativeHeap / 1024 / 1024}MB
                Threads: $threadCount
                Low Memory: $isLowMemory
            """.trimIndent()
        }
    }
}
