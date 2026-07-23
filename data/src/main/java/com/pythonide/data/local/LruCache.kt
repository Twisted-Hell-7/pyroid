package com.pythonide.data.local

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class LruCache<K, V>(
    private val maxSize: Int = 1000,
    private val ttlMillis: Long = 30_000
) {
    private val cache = ConcurrentHashMap<K, CacheEntry<V>>()
    private val accessOrder = ConcurrentHashMap<K, Long>()
    private val hits = AtomicLong(0)
    private val misses = AtomicLong(0)

    private data class CacheEntry<V>(
        val value: V,
        val createdAt: Long = System.currentTimeMillis(),
        var lastAccessedAt: Long = System.currentTimeMillis()
    )

    fun get(key: K): V? {
        val entry = cache[key] ?: return null
        
        if (System.currentTimeMillis() - entry.createdAt > ttlMillis) {
            cache.remove(key)
            accessOrder.remove(key)
            misses.incrementAndGet()
            return null
        }
        
        entry.lastAccessedAt = System.currentTimeMillis()
        accessOrder[key] = System.currentTimeMillis()
        hits.incrementAndGet()
        
        evictIfNeeded()
        
        return entry.value
    }

    fun put(key: K, value: V) {
        cache[key] = CacheEntry(value)
        accessOrder[key] = System.currentTimeMillis()
        
        evictIfNeeded()
    }

    fun remove(key: K) {
        cache.remove(key)
        accessOrder.remove(key)
    }

    fun clear() {
        cache.clear()
        accessOrder.clear()
    }

    private fun evictIfNeeded() {
        while (cache.size > maxSize) {
            val oldestKey = accessOrder.entries
                .minByOrNull { it.value }
                ?.key
            
            if (oldestKey != null) {
                cache.remove(oldestKey)
                accessOrder.remove(oldestKey)
            } else {
                break
            }
        }
    }

    fun getStats(): CacheStats {
        val total = hits.get() + misses.get()
        return CacheStats(
            size = cache.size,
            maxSize = maxSize,
            hits = hits.get(),
            misses = misses.get(),
            hitRate = if (total > 0) hits.get().toFloat() / total else 0f,
            memoryEstimate = estimateMemoryUsage()
        )
    }

    private fun estimateMemoryUsage(): Long {
        var size = 0L
        for (entry in cache.values) {
            val value = entry.value
            size += when (value) {
                is String -> value.length * 2L
                is ByteArray -> value.size.toLong()
                is IntArray -> value.size * 4L
                is LongArray -> value.size * 8L
                is FloatArray -> value.size * 4L
                is DoubleArray -> value.size * 8L
                else -> 64L
            }
        }
        return size
    }

    data class CacheStats(
        val size: Int,
        val maxSize: Int,
        val hits: Long,
        val misses: Long,
        val hitRate: Float,
        val memoryEstimate: Long
    ) {
        fun toFormattedString(): String {
            return "Cache[size=$size/$maxSize, hitRate=${(hitRate * 100).toInt()}%, mem=${memoryEstimate / 1024}KB]"
        }
    }
}

object CacheManager {
    private val fileCache = LruCache<String, String>(maxSize = 50, ttlMillis = 60_000)
    private val syntaxCache = LruCache<String, List<SyntaxToken>>(maxSize = 200, ttlMillis = 300_000)
    private val pathCache = LruCache<String, String>(maxSize = 100, ttlMillis = 120_000)
    private val projectCache = LruCache<String, ProjectInfo>(maxSize = 20, ttlMillis = 300_000)

    fun getFileCache(): LruCache<String, String> = fileCache
    fun getSyntaxCache(): LruCache<String, List<SyntaxToken>> = syntaxCache
    fun getPathCache(): LruCache<String, String> = pathCache
    fun getProjectCache(): LruCache<String, ProjectInfo> = projectCache

    fun getAllStats(): Map<String, LruCache.CacheStats> {
        return mapOf(
            "files" to fileCache.getStats(),
            "syntax" to syntaxCache.getStats(),
            "paths" to pathCache.getStats(),
            "projects" to projectCache.getStats()
        )
    }

    fun clearAll() {
        fileCache.clear()
        syntaxCache.clear()
        pathCache.clear()
        projectCache.clear()
    }

    fun getTotalMemoryUsage(): Long {
        return fileCache.getStats().memoryEstimate +
                syntaxCache.getStats().memoryEstimate +
                pathCache.getStats().memoryEstimate +
                projectCache.getStats().memoryEstimate
    }
}

data class SyntaxToken(
    val start: Int,
    val end: Int,
    val type: SyntaxTokenType,
    val color: Long
)

enum class SyntaxTokenType {
    KEYWORD, STRING, NUMBER, COMMENT, FUNCTION, CLASS, OPERATOR, BUILTIN, DECORATOR
}

data class ProjectInfo(
    val name: String,
    val path: String,
    val lastModified: Long,
    val fileCount: Int
)
