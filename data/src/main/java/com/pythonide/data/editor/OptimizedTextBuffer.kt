package com.pythonide.data.editor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

class OptimizedTextBuffer(
    private val maxLinesInMemory: Int = 10_000,
    private val chunkSize: Int = 1_000
) {
    private val chunks = ConcurrentHashMap<Int, List<String>>()
    private val lineCount = MutableStateFlow(0)
    private val visibleLines = MutableStateFlow<List<String>>(emptyList())
    private val scrollOffset = MutableStateFlow(0)
    @Volatile
    private var totalLines = 0

    val lineCountState: StateFlow<Int> = lineCount.asStateFlow()
    val visibleLinesState: StateFlow<List<String>> = visibleLines.asStateFlow()
    val scrollOffsetState: StateFlow<Int> = scrollOffset.asStateFlow()

    suspend fun loadFile(file: File) = withContext(Dispatchers.IO) {
        clear()
        
        if (!file.exists() || !file.canRead()) return@withContext

        val reader = BufferedReader(InputStreamReader(FileInputStream(file)), 8192)
        var lineNumber = 0
        
        try {
            while (true) {
                val chunk = mutableListOf<String>()
                var linesRead = 0
                
                while (linesRead < chunkSize) {
                    val line = reader.readLine() ?: break
                    chunk.add(line)
                    linesRead++
                }
                
                if (chunk.isEmpty()) break
                
                chunks[lineNumber / chunkSize] = chunk
                lineNumber += chunk.size
            }
            
            totalLines = lineNumber
            lineCount.value = totalLines
            
            updateVisibleLines(0)
        } finally {
            reader.close()
        }
    }

    suspend fun getLine(lineNumber: Int): String? = withContext(Dispatchers.Default) {
        val chunkIndex = lineNumber / chunkSize
        val indexInChunk = lineNumber % chunkSize
        
        chunks[chunkIndex]?.getOrNull(indexInChunk)
    }

    suspend fun getLines(start: Int, end: Int): List<String> = withContext(Dispatchers.Default) {
        val result = mutableListOf<String>()
        val clampedStart = start.coerceIn(0, totalLines)
        val clampedEnd = minOf(end, totalLines).coerceAtLeast(clampedStart)

        // Bulk chunk read without per-line context hops.
        var i = clampedStart
        while (i < clampedEnd) {
            val chunkIndex = i / chunkSize
            val indexInChunk = i % chunkSize
            val chunk = chunks[chunkIndex] ?: break
            val take = minOf(chunk.size - indexInChunk, clampedEnd - i)
            if (take <= 0) break
            result.addAll(chunk.subList(indexInChunk, indexInChunk + take))
            i += take
        }

        result
    }

    suspend fun updateVisibleLines(scrollOffset: Int) = withContext(Dispatchers.Default) {
        this@OptimizedTextBuffer.scrollOffset.value = scrollOffset
        
        val startChunk = scrollOffset / chunkSize
        val endChunk = minOf((scrollOffset + maxLinesInMemory) / chunkSize + 1, chunks.size)
        
        val visible = mutableListOf<String>()
        
        for (chunkIdx in startChunk until endChunk) {
            chunks[chunkIdx]?.let { chunk ->
                visible.addAll(chunk)
            }
        }
        
        visibleLines.value = visible.take(maxLinesInMemory)
    }

    suspend fun editLine(lineNumber: Int, newContent: String) = withContext(Dispatchers.Default) {
        val chunkIndex = lineNumber / chunkSize
        val indexInChunk = lineNumber % chunkSize
        
        val chunk = chunks[chunkIndex]?.toMutableList() ?: return@withContext
        
        if (indexInChunk < chunk.size) {
            chunk[indexInChunk] = newContent
            chunks[chunkIndex] = chunk
        }
    }

    suspend fun insertLine(afterLineNumber: Int, content: String) = withContext(Dispatchers.Default) {
        // Flatten + re-chunk to keep line numbers consistent across chunks.
        val all = flatten()
        val idx = (afterLineNumber + 1).coerceIn(0, all.size)
        all.add(idx, content)
        rechunk(all)

        totalLines = all.size
        lineCount.value = totalLines
    }

    suspend fun deleteLine(lineNumber: Int) = withContext(Dispatchers.Default) {
        val all = flatten()
        if (lineNumber < 0 || lineNumber >= all.size) return@withContext
        all.removeAt(lineNumber)
        rechunk(all)

        totalLines = all.size
        lineCount.value = totalLines
    }

    private fun flatten(): MutableList<String> {
        val all = mutableListOf<String>()
        for (chunkIdx in chunks.keys.sorted()) {
            chunks[chunkIdx]?.let { all.addAll(it) }
        }
        return all
    }

    private fun rechunk(all: List<String>) {
        chunks.clear()
        all.chunked(chunkSize).forEachIndexed { idx, chunk ->
            chunks[idx] = chunk
        }
    }

    fun clear() {
        chunks.clear()
        totalLines = 0
        lineCount.value = 0
        visibleLines.value = emptyList()
        scrollOffset.value = 0
    }

    suspend fun search(query: String, caseSensitive: Boolean = false): List<Int> = withContext(Dispatchers.Default) {
        val results = mutableListOf<Int>()
        val flags = if (caseSensitive) emptySet<RegexOption>() else setOf(RegexOption.IGNORE_CASE)
        val regex = try {
            Regex(query, flags)
        } catch (e: Exception) {
            // Fall back to literal search on invalid regex.
            Regex(Regex.escape(query), flags)
        }
        
        for ((chunkIdx, chunk) in chunks) {
            for ((lineIdx, line) in chunk.withIndex()) {
                if (regex.containsMatchIn(line)) {
                    results.add(chunkIdx * chunkSize + lineIdx)
                }
            }
        }
        
        results
    }

    suspend fun replaceAll(pattern: String, replacement: String, caseSensitive: Boolean = false): Int = withContext(Dispatchers.Default) {
        val flags = if (caseSensitive) emptySet<RegexOption>() else setOf(RegexOption.IGNORE_CASE)
        val regex = try {
            Regex(pattern, flags)
        } catch (e: Exception) {
            Regex(Regex.escape(pattern), flags)
        }
        var count = 0
        
        for ((chunkIdx, chunk) in chunks) {
            val newChunk = chunk.map { line ->
                val newLine = regex.replace(line, replacement)
                if (newLine != line) count++
                newLine
            }
            chunks[chunkIdx] = newChunk
        }
        
        count
    }

    fun getMemoryUsage(): Long {
        var size = 0L
        for (chunk in chunks.values) {
            for (line in chunk) {
                size += line.length * 2
            }
        }
        return size
    }

    fun getLineCount(): Int = totalLines

    suspend fun exportToFile(file: File) = withContext(Dispatchers.IO) {
        file.bufferedWriter().use { writer ->
            for (chunkIdx in chunks.keys.sorted()) {
                chunks[chunkIdx]?.forEach { line ->
                    writer.write(line)
                    writer.newLine()
                }
            }
        }
    }
}
