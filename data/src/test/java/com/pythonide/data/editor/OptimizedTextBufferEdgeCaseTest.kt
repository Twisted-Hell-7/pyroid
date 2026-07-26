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

@OptIn(ExperimentalCoroutinesApi::class)
class OptimizedTextBufferEdgeCaseTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var tempDir: File

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        tempDir = File(System.getProperty("java.io.tmpdir"), "text_buffer_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        tempDir.deleteRecursively()
    }

    // §2: Code Editor - Chunked Loading

    @Test
    fun testChunkedLoading() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 100)
        val testFile = File(tempDir, "test.txt")
        
        // Create file with 500 lines
        val lines = (1..500).map { "Line $it" }
        testFile.writeText(lines.joinToString("\n"))
        
        buffer.loadFile(testFile)
        
        assertEquals(500, buffer.getLineCount())
        assertEquals(500, buffer.lineCountState.value)
    }

    @Test
    fun testChunkedLoadingLargeFile() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 1000)
        val testFile = File(tempDir, "large.txt")
        
        // Create file with 10000 lines
        val lines = (1..10000).map { "Line $it" }
        testFile.writeText(lines.joinToString("\n"))
        
        buffer.loadFile(testFile)
        
        assertEquals(10000, buffer.getLineCount())
    }

    @Test
    fun testGetLine() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        
        val lines = (1..50).map { "Line $it" }
        testFile.writeText(lines.joinToString("\n"))
        
        buffer.loadFile(testFile)
        
        assertEquals("Line 1", buffer.getLine(0))
        assertEquals("Line 25", buffer.getLine(24))
        assertEquals("Line 50", buffer.getLine(49))
    }

    @Test
    fun testGetLineOutOfBounds() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Line 1\nLine 2")
        
        buffer.loadFile(testFile)
        
        assertNull(buffer.getLine(-1))
        assertNull(buffer.getLine(100))
    }

    @Test
    fun testGetLines() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        
        val lines = (1..50).map { "Line $it" }
        testFile.writeText(lines.joinToString("\n"))
        
        buffer.loadFile(testFile)
        
        val result = buffer.getLines(0, 5)
        assertEquals(5, result.size)
        assertEquals("Line 1", result[0])
        assertEquals("Line 5", result[4])
    }

    @Test
    fun testGetLinesBeyondEnd() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Line 1\nLine 2\nLine 3")
        
        buffer.loadFile(testFile)
        
        val result = buffer.getLines(0, 100)
        assertEquals(3, result.size)
    }

    @Test
    fun testEditLine() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Line 1\nLine 2\nLine 3")
        
        buffer.loadFile(testFile)
        buffer.editLine(1, "Modified Line 2")
        
        assertEquals("Modified Line 2", buffer.getLine(1))
    }

    @Test
    fun testInsertLine() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Line 1\nLine 2")
        
        buffer.loadFile(testFile)
        buffer.insertLine(0, "Inserted Line")
        
        assertEquals(3, buffer.getLineCount())
        assertEquals("Inserted Line", buffer.getLine(1))
    }

    @Test
    fun testDeleteLine() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Line 1\nLine 2\nLine 3")
        
        buffer.loadFile(testFile)
        buffer.deleteLine(1)
        
        assertEquals(2, buffer.getLineCount())
        assertEquals("Line 3", buffer.getLine(1))
    }

    @Test
    fun testSearch() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Line 1: hello\nLine 2: world\nLine 3: hello world")
        
        buffer.loadFile(testFile)
        
        val results = buffer.search("hello")
        assertEquals(2, results.size)
        assertTrue(results.contains(0))
        assertTrue(results.contains(2))
    }

    @Test
    fun testSearchCaseSensitive() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Hello\nhello\nHELLO")
        
        buffer.loadFile(testFile)
        
        val results = buffer.search("hello", caseSensitive = true)
        assertEquals(1, results.size)
        assertTrue(results.contains(1))
    }

    @Test
    fun testSearchCaseInsensitive() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Hello\nhello\nHELLO")
        
        buffer.loadFile(testFile)
        
        val results = buffer.search("hello", caseSensitive = false)
        assertEquals(3, results.size)
    }

    @Test
    fun testSearchWithRegex() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Line 1: 123\nLine 2: 456\nLine 3: abc")
        
        buffer.loadFile(testFile)
        
        val results = buffer.search("\\d+")
        assertEquals(3, results.size)
    }

    @Test
    fun testReplaceAll() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("hello world\nhello there")
        
        buffer.loadFile(testFile)
        
        val count = buffer.replaceAll("hello", "hi")
        assertEquals(2, count)
        
        assertEquals("hi world", buffer.getLine(0))
        assertEquals("hi there", buffer.getLine(1))
    }

    @Test
    fun testReplaceAllCaseSensitive() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Hello\nhello\nHELLO")
        
        buffer.loadFile(testFile)
        
        val count = buffer.replaceAll("hello", "hi", caseSensitive = true)
        assertEquals(1, count)
        
        assertEquals("Hello", buffer.getLine(0))
        assertEquals("hi", buffer.getLine(1))
        assertEquals("HELLO", buffer.getLine(2))
    }

    @Test
    fun testReplaceAllWithRegex() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Line 1: 123\nLine 2: 456")
        
        buffer.loadFile(testFile)
        
        val count = buffer.replaceAll("\\d+", "NUM")
        assertEquals(2, count)
    }

    @Test
    fun testMemoryUsage() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Line 1\nLine 2\nLine 3")
        
        buffer.loadFile(testFile)
        
        val memoryUsage = buffer.getMemoryUsage()
        assertTrue(memoryUsage > 0)
    }

    @Test
    fun testClear() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Line 1\nLine 2\nLine 3")
        
        buffer.loadFile(testFile)
        assertEquals(3, buffer.getLineCount())
        
        buffer.clear()
        assertEquals(0, buffer.getLineCount())
    }

    @Test
    fun testExportToFile() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Line 1\nLine 2\nLine 3")
        
        buffer.loadFile(testFile)
        
        val exportFile = File(tempDir, "export.txt")
        buffer.exportToFile(exportFile)
        
        assertTrue(exportFile.exists())
        assertEquals("Line 1\nLine 2\nLine 3\n", exportFile.readText())
    }

    @Test
    fun testLoadNonExistentFile() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "nonexistent.txt")
        
        buffer.loadFile(testFile)
        
        assertEquals(0, buffer.getLineCount())
    }

    @Test
    fun testLoadEmptyFile() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "empty.txt")
        testFile.writeText("")
        
        buffer.loadFile(testFile)
        
        assertEquals(0, buffer.getLineCount())
    }

    // §2: Code Editor - Virtual Scrolling

    @Test
    fun testUpdateVisibleLines() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10, maxLinesInMemory = 50)
        val testFile = File(tempDir, "test.txt")
        
        val lines = (1..200).map { "Line $it" }
        testFile.writeText(lines.joinToString("\n"))
        
        buffer.loadFile(testFile)
        buffer.updateVisibleLines(0)
        
        val visibleLines = buffer.visibleLinesState.value
        assertTrue(visibleLines.size <= 50)
    }

    @Test
    fun testScrollOffset() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10, maxLinesInMemory = 50)
        val testFile = File(tempDir, "test.txt")
        
        val lines = (1..200).map { "Line $it" }
        testFile.writeText(lines.joinToString("\n"))
        
        buffer.loadFile(testFile)
        buffer.updateVisibleLines(100)
        
        assertEquals(100, buffer.scrollOffsetState.value)
    }

    // §2: Code Editor - Boundary Values

    @Test
    fun testBoundaryAt999Lines() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 1000)
        val testFile = File(tempDir, "test.txt")
        
        val lines = (1..999).map { "Line $it" }
        testFile.writeText(lines.joinToString("\n"))
        
        buffer.loadFile(testFile)
        
        assertEquals(999, buffer.getLineCount())
    }

    @Test
    fun testBoundaryAt1000Lines() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 1000)
        val testFile = File(tempDir, "test.txt")
        
        val lines = (1..1000).map { "Line $it" }
        testFile.writeText(lines.joinToString("\n"))
        
        buffer.loadFile(testFile)
        
        assertEquals(1000, buffer.getLineCount())
    }

    @Test
    fun testBoundaryAt1001Lines() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 1000)
        val testFile = File(tempDir, "test.txt")
        
        val lines = (1..1001).map { "Line $it" }
        testFile.writeText(lines.joinToString("\n"))
        
        buffer.loadFile(testFile)
        
        assertEquals(1001, buffer.getLineCount())
    }

    // §2: Code Editor - Unicode Content

    @Test
    fun testUnicodeContent() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("日本語テスト\n한국어 테스트\n中文测试")
        
        buffer.loadFile(testFile)
        
        assertEquals(3, buffer.getLineCount())
        assertEquals("日本語テスト", buffer.getLine(0))
    }

    @Test
    fun testEmojiContent() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Hello 🌍\nHello 🎉\nHello 🚀")
        
        buffer.loadFile(testFile)
        
        assertEquals(3, buffer.getLineCount())
        assertEquals("Hello 🌍", buffer.getLine(0))
    }

    // §2: Code Editor - Mixed Line Endings

    @Test
    fun testMixedLineEndings() = runTest {
        val buffer = OptimizedTextBuffer(chunkSize = 10)
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("line1\r\nline2\nline3\rline4")
        
        buffer.loadFile(testFile)
        
        // Should handle mixed line endings
        assertTrue(buffer.getLineCount() >= 3)
    }
}
