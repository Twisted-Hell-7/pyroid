package com.pythonide.data.editor

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap

class FileWatcher(
    private val watchIntervalMs: Long = 500
) {
    private val watchService: WatchService? = try {
        FileSystems.getDefault().newWatchService()
    } catch (e: Exception) {
        null
    }
    
    private val watchedPaths = ConcurrentHashMap<Path, WatchKey>()
    private val fileChanges = MutableStateFlow<List<FileChange>>(emptyList())
    val fileChangesState: StateFlow<List<FileChange>> = fileChanges.asStateFlow()
    
    private var watchJob: Job? = null
    private val changes = mutableListOf<FileChange>()
    
    data class FileChange(
        val path: Path,
        val type: ChangeType,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    enum class ChangeType {
        CREATED, MODIFIED, DELETED
    }
    
    fun startWatching(directories: List<String>) {
        if (watchService == null) return
        
        watchJob = CoroutineScope(Dispatchers.IO).launch {
            directories.forEach { dir ->
                val path = Paths.get(dir)
                if (Files.exists(path) && Files.isDirectory(path)) {
                    watchDirectory(path)
                }
            }
            
            while (isActive) {
                val key = watchService?.poll(watchIntervalMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                
                if (key != null) {
                    for (event in key.pollEvents()) {
                        val kind = event.kind()
                        if (kind == StandardWatchEventKinds.OVERFLOW) continue
                        
                        @Suppress("UNCHECKED_CAST")
                        val eventPath = (event.context() as Path)
                        val parentPath = key.watchable() as? Path ?: continue
                        val fullPath = parentPath.resolve(eventPath)
                        
                        val changeType = when (kind) {
                            StandardWatchEventKinds.ENTRY_CREATE -> ChangeType.CREATED
                            StandardWatchEventKinds.ENTRY_MODIFY -> ChangeType.MODIFIED
                            StandardWatchEventKinds.ENTRY_DELETE -> ChangeType.DELETED
                            else -> continue
                        }
                        
                        synchronized(changes) {
                            changes.add(FileChange(fullPath, changeType))
                            fileChanges.value = changes.toList()
                        }
                    }
                    
                    key.reset()
                }
            }
        }
    }
    
    private fun watchDirectory(path: Path) {
        val key = path.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE
        )
        watchedPaths[path] = key
        
        Files.list(path).use { stream ->
            stream.filter { Files.isDirectory(it) }.forEach { subDir ->
                watchDirectory(subDir)
            }
        }
    }
    
    fun stopWatching() {
        watchJob?.cancel()
        watchedPaths.keys.forEach { path ->
            watchedPaths[path]?.cancel()
        }
        watchedPaths.clear()
        watchService?.close()
    }
    
    fun getRecentChanges(count: Int = 10): List<FileChange> {
        return synchronized(changes) {
            changes.takeLast(count)
        }
    }
    
    fun clearChanges() {
        synchronized(changes) {
            changes.clear()
            fileChanges.value = emptyList()
        }
    }
}

class LazyFileLoader(
    private val chunkSize: Int = 100
) {
    private val fileChunks = ConcurrentHashMap<String, List<String>>()
    private val loadedChunks = ConcurrentHashMap<String, MutableSet<Int>>()
    private val totalLines = ConcurrentHashMap<String, Int>()
    
    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()
    
    sealed class LoadingState {
        object Idle : LoadingState()
        data class Loading(val file: String, val progress: Float) : LoadingState()
        data class Loaded(val file: String, val totalChunks: Int) : LoadingState()
        data class Error(val message: String) : LoadingState()
    }
    
    suspend fun loadFile(file: File): List<String> = withContext(Dispatchers.IO) {
        val key = file.absolutePath
        
        if (fileChunks.containsKey(key)) {
            return@withContext getLoadedLines(key)
        }
        
        _loadingState.value = LoadingState.Loading(key, 0f)
        
        try {
            val lines = file.readLines()
            totalLines[key] = lines.size
            
            val chunks = lines.chunked(chunkSize)
            fileChunks[key] = lines
            loadedChunks[key] = (0 until chunks.size).toMutableSet()
            
            _loadingState.value = LoadingState.Loaded(key, chunks.size)
            
            lines
        } catch (e: Exception) {
            _loadingState.value = LoadingState.Error(e.message ?: "Failed to load file")
            emptyList()
        }
    }
    
    suspend fun loadChunk(file: String, chunkIndex: Int): List<String> = withContext(Dispatchers.IO) {
        val lines = fileChunks[file] ?: return@withContext emptyList()
        
        val start = chunkIndex * chunkSize
        val end = minOf(start + chunkSize, lines.size)
        
        if (start >= lines.size) return@withContext emptyList()
        
        lines.subList(start, end)
    }
    
    private fun getLoadedLines(file: String): List<String> {
        return fileChunks[file] ?: emptyList()
    }
    
    fun getTotalChunks(file: String): Int {
        val lines = fileChunks[file] ?: return 0
        return (lines.size + chunkSize - 1) / chunkSize
    }
    
    fun getLoadedChunks(file: String): Set<Int> {
        return loadedChunks[file]?.toSet() ?: emptySet()
    }
    
    fun unloadFile(file: String) {
        fileChunks.remove(file)
        loadedChunks.remove(file)
        totalLines.remove(file)
    }
    
    fun clearAll() {
        fileChunks.clear()
        loadedChunks.clear()
        totalLines.clear()
    }
    
    fun getMemoryUsage(): Long {
        var size = 0L
        for (lines in fileChunks.values) {
            for (line in lines) {
                size += line.length * 2L
            }
        }
        return size
    }
}
