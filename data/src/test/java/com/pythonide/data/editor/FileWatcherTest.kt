package com.pythonide.data.editor

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
import java.io.File
import java.nio.file.Path

@OptIn(ExperimentalCoroutinesApi::class)
class FileWatcherTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var tempDir: File

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        tempDir = File(System.getProperty("java.io.tmpdir"), "file_watcher_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        tempDir.deleteRecursively()
    }

    // §2: Code Editor - File Watcher

    @Test
    fun testFileWatcherCreation() {
        val watcher = FileWatcher()
        assertNotNull(watcher)
    }

    @Test
    fun testFileChangeDataClass() {
        val change = FileWatcher.FileChange(
            path = Path.of("/tmp/test.txt"),
            type = FileWatcher.ChangeType.MODIFIED
        )
        assertEquals(Path.of("/tmp/test.txt"), change.path)
        assertEquals(FileWatcher.ChangeType.MODIFIED, change.type)
        assertTrue(change.timestamp > 0)
    }

    @Test
    fun testChangeTypeValues() {
        val values = FileWatcher.ChangeType.entries
        assertEquals(3, values.size)
        assertTrue(values.contains(FileWatcher.ChangeType.CREATED))
        assertTrue(values.contains(FileWatcher.ChangeType.MODIFIED))
        assertTrue(values.contains(FileWatcher.ChangeType.DELETED))
    }

    @Test
    fun testFileWatcherStartStop() {
        val watcher = FileWatcher()
        watcher.startWatching(listOf(tempDir.absolutePath))
        
        // Should not crash
        watcher.stopWatching()
    }

    @Test
    fun testFileWatcherGetRecentChanges() {
        val watcher = FileWatcher()
        val changes = watcher.getRecentChanges(10)
        assertTrue(changes.isEmpty())
    }

    @Test
    fun testFileWatcherClearChanges() {
        val watcher = FileWatcher()
        watcher.clearChanges()
        
        val changes = watcher.fileChangesState.value
        assertTrue(changes.isEmpty())
    }

    @Test
    fun testFileWatcherNonExistentDirectory() {
        val watcher = FileWatcher()
        // Should not crash when watching non-existent directory
        watcher.startWatching(listOf("/nonexistent/dir"))
        watcher.stopWatching()
    }

    // §2: Code Editor - Lazy File Loader

    @Test
    fun testLazyFileLoaderCreation() {
        val loader = LazyFileLoader()
        assertNotNull(loader)
    }

    @Test
    fun testLazyFileLoaderLoadFile() = runTest {
        val loader = LazyFileLoader()
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Line 1\nLine 2\nLine 3")
        
        val lines = loader.loadFile(testFile)
        
        assertEquals(3, lines.size)
        assertEquals("Line 1", lines[0])
        assertEquals("Line 2", lines[1])
        assertEquals("Line 3", lines[2])
    }

    @Test
    fun testLazyFileLoaderLoadChunk() = runTest {
        val loader = LazyFileLoader(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        
        val lines = (1..50).map { "Line $it" }
        testFile.writeText(lines.joinToString("\n"))
        
        loader.loadFile(testFile)
        
        val chunk = loader.loadChunk(testFile.absolutePath, 0)
        assertEquals(10, chunk.size)
        assertEquals("Line 1", chunk[0])
    }

    @Test
    fun testLazyFileLoaderGetTotalChunks() = runTest {
        val loader = LazyFileLoader(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        
        val lines = (1..50).map { "Line $it" }
        testFile.writeText(lines.joinToString("\n"))
        
        loader.loadFile(testFile)
        
        assertEquals(5, loader.getTotalChunks(testFile.absolutePath))
    }

    @Test
    fun testLazyFileLoaderGetLoadedChunks() = runTest {
        val loader = LazyFileLoader(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        
        val lines = (1..50).map { "Line $it" }
        testFile.writeText(lines.joinToString("\n"))
        
        loader.loadFile(testFile)
        
        val loadedChunks = loader.getLoadedChunks(testFile.absolutePath)
        assertEquals(5, loadedChunks.size)
    }

    @Test
    fun testLazyFileLoaderUnloadFile() = runTest {
        val loader = LazyFileLoader()
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("content")
        
        loader.loadFile(testFile)
        loader.unloadFile(testFile.absolutePath)
        
        assertEquals(0, loader.getTotalChunks(testFile.absolutePath))
    }

    @Test
    fun testLazyFileLoaderClearAll() = runTest {
        val loader = LazyFileLoader()
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("content")
        
        loader.loadFile(testFile)
        loader.clearAll()
        
        assertEquals(0, loader.getTotalChunks(testFile.absolutePath))
    }

    @Test
    fun testLazyFileLoaderMemoryUsage() = runTest {
        val loader = LazyFileLoader()
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Line 1\nLine 2\nLine 3")
        
        loader.loadFile(testFile)
        
        val memoryUsage = loader.getMemoryUsage()
        assertTrue(memoryUsage > 0)
    }

    @Test
    fun testLazyFileLoaderLoadingState() = runTest {
        val loader = LazyFileLoader()
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("content")
        
        loader.loadFile(testFile)
        
        val state = loader.loadingState.value
        assertTrue(state is LazyFileLoader.LoadingState.Loaded)
    }

    @Test
    fun testLazyFileLoaderNonExistentFile() = runTest {
        val loader = LazyFileLoader()
        val testFile = File(tempDir, "nonexistent.txt")
        
        val lines = loader.loadFile(testFile)
        
        assertTrue(lines.isEmpty())
        val state = loader.loadingState.value
        assertTrue(state is LazyFileLoader.LoadingState.Error)
    }

    @Test
    fun testLazyFileLoaderLoadChunkNonExistentFile() = runTest {
        val loader = LazyFileLoader()
        
        val chunk = loader.loadChunk("/nonexistent/file.txt", 0)
        
        assertTrue(chunk.isEmpty())
    }

    @Test
    fun testLazyFileLoaderLoadChunkOutOfBounds() = runTest {
        val loader = LazyFileLoader(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Line 1\nLine 2\nLine 3")
        
        loader.loadFile(testFile)
        
        val chunk = loader.loadChunk(testFile.absolutePath, 100)
        assertTrue(chunk.isEmpty())
    }

    // §2: Code Editor - File Watcher State

    @Test
    fun testFileWatcherState() {
        val watcher = FileWatcher()
        val state = watcher.fileChangesState.value
        assertTrue(state.isEmpty())
    }

    @Test
    fun testFileWatcherGetRecentChangesWithCount() {
        val watcher = FileWatcher()
        val changes = watcher.getRecentChanges(5)
        assertTrue(changes.isEmpty())
    }
}
