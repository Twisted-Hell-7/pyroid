package com.pythonide.data.runtime

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Handler
import android.os.Looper
import com.pythonide.data.local.CacheManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryOptimizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _memoryState = MutableStateFlow<MemoryState>(MemoryState.Normal)
    val memoryState: StateFlow<MemoryState> = _memoryState.asStateFlow()

    private val _memoryUsage = MutableStateFlow(MemoryUsage())
    val memoryUsage: StateFlow<MemoryUsage> = _memoryUsage.asStateFlow()

    private val isMonitoring = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitoringJob: Job? = null

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    sealed class MemoryState {
        object Normal : MemoryState()
        object Warning : MemoryState()
        object Critical : MemoryState()
        data class OomRisk(val level: Int) : MemoryState()
    }

    data class MemoryUsage(
        val usedHeap: Long = 0,
        val maxHeap: Long = 0,
        val nativeHeap: Long = 0,
        val cacheMemory: Long = 0,
        val usagePercent: Int = 0,
        val isLowMemory: Boolean = false
    )

    fun startMonitoring(intervalMs: Long = 5000) {
        if (isMonitoring.compareAndSet(false, true)) {
            monitoringJob = scope.launch {
                while (isActive && isMonitoring.get()) {
                    updateMemoryState()
                    delay(intervalMs)
                }
            }
        }
    }

    fun stopMonitoring() {
        isMonitoring.set(false)
        monitoringJob?.cancel()
        monitoringJob = null
        scope.cancel()
    }

    private fun updateMemoryState() {
        val runtime = Runtime.getRuntime()
        val usedHeap = runtime.totalMemory() - runtime.freeMemory()
        val maxHeap = runtime.maxMemory()
        val nativeHeap = Debug.getNativeHeapAllocatedSize()
        val cacheMemory = CacheManager.getTotalMemoryUsage()
        val usagePercent = (usedHeap.toFloat() / maxHeap * 100).toInt()
        val isLowMemory = activityManager.isLowRamDevice

        val usage = MemoryUsage(
            usedHeap = usedHeap,
            maxHeap = maxHeap,
            nativeHeap = nativeHeap,
            cacheMemory = cacheMemory,
            usagePercent = usagePercent,
            isLowMemory = isLowMemory
        )

        _memoryUsage.value = usage

        val newState = when {
            usagePercent > 90 -> MemoryState.Critical
            usagePercent > 75 -> MemoryState.Warning
            usagePercent > 60 -> MemoryState.OomRisk(2)
            else -> MemoryState.Normal
        }

        _memoryState.value = newState

        if (newState is MemoryState.Critical) {
            triggerCleanup()
        }
    }

    private fun triggerCleanup() {
        CacheManager.clearAll()
        System.gc()
        System.runFinalization()
    }

    fun getMemoryInfo(): MemoryInfo {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        return MemoryInfo(
            availMem = memInfo.availMem,
            totalMem = memInfo.totalMem,
            threshold = memInfo.threshold,
            lowMemory = memInfo.lowMemory,
            runtime = Runtime.getRuntime().let {
                RuntimeInfo(
                    maxMemory = it.maxMemory(),
                    totalMemory = it.totalMemory(),
                    freeMemory = it.freeMemory(),
                    processors = it.availableProcessors()
                )
            },
            nativeHeap = Debug.getNativeHeapAllocatedSize(),
            dalvikPss = Debug.MemoryInfo().also { Debug.getMemoryInfo(it) }.totalPss.toLong()
        )
    }

    fun optimizeMemory() {
        val cache = CacheManager.getFileCache()
        if (cache.getStats().size > 100) {
            cache.clear()
        }

        System.gc()

        mainHandler.postDelayed({
            System.runFinalization()
        }, 100)
    }

    data class MemoryInfo(
        val availMem: Long,
        val totalMem: Long,
        val threshold: Long,
        val lowMemory: Boolean,
        val runtime: RuntimeInfo,
        val nativeHeap: Long,
        val dalvikPss: Long
    ) {
        fun toFormattedString(): String {
            return """
                Memory Info:
                Available: ${availMem / 1024 / 1024}MB
                Total: ${totalMem / 1024 / 1024}MB
                Threshold: ${threshold / 1024 / 1024}MB
                Low Memory: $lowMemory
                Native Heap: ${nativeHeap / 1024 / 1024}MB
                Dalvik PSS: ${dalvikPss / 1024}KB
                Runtime:
                  Max: ${runtime.maxMemory / 1024 / 1024}MB
                  Total: ${runtime.totalMemory / 1024 / 1024}MB
                  Free: ${runtime.freeMemory / 1024 / 1024}MB
                  Processors: ${runtime.processors}
            """.trimIndent()
        }
    }

    data class RuntimeInfo(
        val maxMemory: Long,
        val totalMemory: Long,
        val freeMemory: Long,
        val processors: Int
    )

    class WeakReferenceHolder<T : Any> {
        private val references = mutableListOf<WeakReference<T>>()

        fun add(reference: T) {
            references.add(WeakReference(reference))
            cleanup()
        }

        fun get(): List<T> {
            cleanup()
            return references.mapNotNull { it.get() }
        }

        fun clear() {
            references.clear()
        }

        private fun cleanup() {
            references.removeAll { it.get() == null }
        }
    }
}
