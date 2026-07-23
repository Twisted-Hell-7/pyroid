package com.pythonide.data.runtime

import android.content.Context
import com.pythonide.data.local.CacheManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartupOptimizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _startupProgress = MutableStateFlow<StartupProgress>(StartupProgress.Idle)
    val startupProgress: StateFlow<StartupProgress> = _startupProgress.asStateFlow()

    private val _startupMetrics = MutableStateFlow(StartupMetrics())
    val startupMetrics: StateFlow<StartupMetrics> = _startupMetrics.asStateFlow()

    private val startTime = System.currentTimeMillis()

    sealed class StartupProgress {
        object Idle : StartupProgress()
        data class Loading(val stage: String, val progress: Float) : StartupProgress()
        object Complete : StartupProgress()
        data class Error(val message: String) : StartupProgress()
    }

    data class StartupMetrics(
        val totalTime: Long = 0,
        val cacheLoadTime: Long = 0,
        val preferencesLoadTime: Long = 0,
        val fileSystemScanTime: Long = 0,
        val memoryUsage: Long = 0,
        val cacheStats: Map<String, CacheManager.CacheStats> = emptyMap()
    )

    suspend fun optimizeStartup(): StartupMetrics = withContext(Dispatchers.IO) {
        _startupProgress.value = StartupProgress.Loading("Initializing", 0f)
        val metrics = StartupMetrics()
        
        try {
            _startupProgress.value = StartupProgress.Loading("Loading cache", 0.2f)
            val cacheStart = System.currentTimeMillis()
            preloadCache()
            val cacheTime = System.currentTimeMillis() - cacheStart

            _startupProgress.value = StartupProgress.Loading("Loading preferences", 0.4f)
            val prefStart = System.currentTimeMillis()
            preloadPreferences()
            val prefTime = System.currentTimeMillis() - prefStart

            _startupProgress.value = StartupProgress.Loading("Scanning files", 0.6f)
            val scanStart = System.currentTimeMillis()
            preloadFileSystem()
            val scanTime = System.currentTimeMillis() - scanStart

            _startupProgress.value = StartupProgress.Loading("Warming up", 0.8f)
            warmUp()

            _startupProgress.value = StartupProgress.Loading("Complete", 1.0f)
            
            val totalTime = System.currentTimeMillis() - startTime
            val memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            
            val finalMetrics = metrics.copy(
                totalTime = totalTime,
                cacheLoadTime = cacheTime,
                preferencesLoadTime = prefTime,
                fileSystemScanTime = scanTime,
                memoryUsage = memoryUsage,
                cacheStats = CacheManager.getAllStats()
            )
            
            _startupMetrics.value = finalMetrics
            _startupProgress.value = StartupProgress.Complete
            
            finalMetrics
        } catch (e: Exception) {
            _startupProgress.value = StartupProgress.Error(e.message ?: "Unknown error")
            metrics
        }
    }

    private suspend fun preloadCache() {
        try {
            CacheManager.getFileCache().getStats()
        } catch (e: Exception) {
            // Cache not available yet
        }
    }

    private suspend fun preloadPreferences() {
        delay(50) // Simulate async preferences load
    }

    private suspend fun preloadFileSystem() {
        try {
            val cacheDir = context.cacheDir
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.take(10)?.forEach { file ->
                    file.length()
                }
            }
        } catch (e: Exception) {
            // File system not ready
        }
    }

    private suspend fun warmUp() {
        delay(20) // Simulate warm-up
        System.gc()
    }

    fun getMemoryReport(): MemoryReport {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()

        return MemoryReport(
            totalMemory = totalMemory,
            usedMemory = usedMemory,
            freeMemory = freeMemory,
            maxMemory = maxMemory,
            usagePercent = (usedMemory.toFloat() / maxMemory * 100).toInt(),
            cacheMemory = CacheManager.getTotalMemoryUsage(),
            gcCount = getGarbageCollectionCount()
        )
    }

    private fun getGarbageCollectionCount(): Int {
        return try {
            val gc = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()
            gc.sumOf { it.collectionCount }.toInt()
        } catch (e: Exception) {
            0
        }
    }

    data class MemoryReport(
        val totalMemory: Long,
        val usedMemory: Long,
        val freeMemory: Long,
        val maxMemory: Long,
        val usagePercent: Int,
        val cacheMemory: Long,
        val gcCount: Int
    ) {
        fun toFormattedString(): String {
            return """
                Memory Report:
                Total: ${totalMemory / 1024 / 1024}MB
                Used: ${usedMemory / 1024 / 1024}MB (${usagePercent}%)
                Free: ${freeMemory / 1024 / 1024}MB
                Max: ${maxMemory / 1024 / 1024}MB
                Cache: ${cacheMemory / 1024}KB
                GC Count: $gcCount
            """.trimIndent()
        }
    }
}
