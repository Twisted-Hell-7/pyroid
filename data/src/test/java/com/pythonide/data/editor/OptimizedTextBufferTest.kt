package com.pythonide.data.editor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class OptimizedTextBufferTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var buffer: OptimizedTextBuffer
    private lateinit var tempDir: File

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        buffer = OptimizedTextBuffer(maxLinesInMemory = 100, chunkSize = 10)
        tempDir = File(System.getProperty("java.io.tmpdir"), "buffer_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        tempDir.deleteRecursively()
    }

    // --- Load file (§2: chunked loading) ---

    @Test
    fun testLoadSmallFile() = runTest {
        val file = File(tempDir, "small.py")
        file.writeText("line1\nline2\nline3")
        buffer.loadFile(file)
        assertEquals(3, buffer.getLineCount())
        assertEquals("line1", buffer.getLine(0))
        assertEquals("line2", buffer.getLine(1))
        assertEquals("line3", buffer.getLine(2))
    }

    @Test
    fun testLoadEmptyFile() = runTest {
        val file = File(tempDir, "empty.py")
        file.writeText("")
        buffer.loadFile(file)
        assertEquals(0, buffer.getLineCount())
    }

    @Test
    fun testLoadNonExistentFile() = runTest {
        val file = File(tempDir, "nonexistent.py")
        buffer.loadFile(file)
        assertEquals(0, buffer.getLineCount())
    }

    @Test
    fun testLoadLargeFileChunked() = runTest {
        // §2: test chunked loading with file > chunk size
        val file = File(tempDir, "large.py")
        val lines = (1..50).map { "line_$it" }
        file.writeText(lines.joinToString("\n"))
        buffer.loadFile(file)
        assertEquals(50, buffer.getLineCount())
    }

    @Test
    fun testLoadFileMultipleChunks() = runTest {
        val file = File(tempDir, "multi.py")
        val lines = (1..25).map { "line_$it" }
        file.writeText(lines.joinToString("\n"))
        buffer.loadFile(file)
        assertEquals(25, buffer.getLineCount())
        // Verify all lines accessible
        for (i in 0 until 25) {
            assertEquals("line_${i + 1}", buffer.getLine(i))
        }
    }

    // --- getLine ---

    @Test
    fun testGetLineValid() = runTest {
        val file = File(tempDir, "test.py")
        file.writeText("hello\nworld")
        buffer.loadFile(file)
        assertEquals("hello", buffer.getLine(0))
        assertEquals("world", buffer.getLine(1))
    }

    @Test
    fun testGetLineOutOfBounds() = runTest {
        val file = File(tempDir, "test.py")
        file.writeText("hello")
        buffer.loadFile(file)
        assertNull(buffer.getLine(5))
        assertNull(buffer.getLine(-1))
    }

    // --- getLines ---

    @Test
    fun testGetLinesRange() = runTest {
        val file = File(tempDir, "test.py")
        val lines = (0..9).map { "line_$it" }
        file.writeText(lines.joinToString("\n"))
        buffer.loadFile(file)
        val result = buffer.getLines(2, 5)
        assertEquals(3, result.size)
        assertEquals("line_2", result[0])
        assertEquals("line_3", result[1])
        assertEquals("line_4", result[2])
    }

    @Test
    fun testGetLinesClampedEnd() = runTest {
        val file = File(tempDir, "test.py")
        file.writeText("a\nb\nc")
        buffer.loadFile(file)
        val result = buffer.getLines(0, 100)
        assertEquals(3, result.size)
    }

    // --- editLine ---

    @Test
    fun testEditLine() = runTest {
        val file = File(tempDir, "test.py")
        file.writeText("line1\nline2\nline3")
        buffer.loadFile(file)
        buffer.editLine(1, "modified_line2")
        assertEquals("modified_line2", buffer.getLine(1))
        assertEquals("line1", buffer.getLine(0))
        assertEquals("line3", buffer.getLine(2))
    }

    @Test
    fun testEditLineOutOfBounds() = runTest {
        val file = File(tempDir, "test.py")
        file.writeText("line1")
        buffer.loadFile(file)
        buffer.editLine(5, "new") // should not crash
        assertEquals("line1", buffer.getLine(0))
    }

    // --- insertLine ---

    @Test
    fun testInsertLine() = runTest {
        val file = File(tempDir, "test.py")
        file.writeText("line1\nline3")
        buffer.loadFile(file)
        buffer.insertLine(0, "line2")
        assertEquals(3, buffer.getLineCount())
        assertEquals("line1", buffer.getLine(0))
        assertEquals("line2", buffer.getLine(1))
        assertEquals("line3", buffer.getLine(2))
    }

    // --- deleteLine ---

    @Test
    fun testDeleteLine() = runTest {
        val file = File(tempDir, "test.py")
        file.writeText("line1\nline2\nline3")
        buffer.loadFile(file)
        buffer.deleteLine(1)
        assertEquals(2, buffer.getLineCount())
        assertEquals("line1", buffer.getLine(0))
        assertEquals("line3", buffer.getLine(1))
    }

    @Test
    fun testDeleteLineOutOfBounds() = runTest {
        val file = File(tempDir, "test.py")
        file.writeText("line1")
        buffer.loadFile(file)
        buffer.deleteLine(5) // should not crash
        assertEquals(1, buffer.getLineCount())
    }

    // --- clear ---

    @Test
    fun testClear() = runTest {
        val file = File(tempDir, "test.py")
        file.writeText("line1\nline2")
        buffer.loadFile(file)
        buffer.clear()
        assertEquals(0, buffer.getLineCount())
    }

    // --- search ---

    @Test
    fun testSearch() = runTest {
        val file = File(tempDir, "test.py")
        file.writeText("hello world\nfoo bar\nhello again")
        buffer.loadFile(file)
        val results = buffer.search("hello")
        assertEquals(2, results.size)
        assertTrue(results.contains(0))
        assertTrue(results.contains(2))
    }

    @Test
    fun testSearchCaseSensitive() = runTest {
        val file = File(tempDir, "test.py")
        file.writeText("Hello HELLO hello")
        buffer.loadFile(file)
        val results = buffer.search("hello", caseSensitive = true)
        assertEquals(1, results.size)
    }

    @Test
    fun testSearchNoMatch() = runTest {
        val file = File(tempDir, "test.py")
        file.writeText("hello world")
        buffer.loadFile(file)
        val results = buffer.search("xyz")
        assertTrue(results.isEmpty())
    }

    // --- replaceAll ---

    @Test
    fun testReplaceAll() = runTest {
        val file = File(tempDir, "test.py")
        file.writeText("foo bar foo baz foo")
        buffer.loadFile(file)
        val count = buffer.replaceAll("foo", "qux")
        assertEquals(1, count)
        assertEquals("qux bar qux baz qux", buffer.getLine(0))
    }

    @Test
    fun testReplaceAllCaseInsensitive() = runTest {
        val file = File(tempDir, "test.py")
        file.writeText("Foo FOO foo")
        buffer.loadFile(file)
        val count = buffer.replaceAll("foo", "bar", caseSensitive = false)
        assertEquals(1, count)
    }

    // --- getMemoryUsage ---

    @Test
    fun testGetMemoryUsage() = runTest {
        val file = File(tempDir, "test.py")
        file.writeText("hello\nworld")
        buffer.loadFile(file)
        val usage = buffer.getMemoryUsage()
        assertTrue(usage > 0)
    }

    // --- exportToFile ---

    @Test
    fun testExportToFile() = runTest {
        val inputFile = File(tempDir, "input.py")
        inputFile.writeText("line1\nline2\nline3")
        buffer.loadFile(inputFile)

        val outputFile = File(tempDir, "output.py")
        buffer.exportToFile(outputFile)
        assertTrue(outputFile.exists())
        assertEquals("line1\nline2\nline3\n", outputFile.readText())
    }

    // --- Line count state flow ---

    @Test
    fun testLineCountFlow() = runTest {
        val file = File(tempDir, "test.py")
        file.writeText("a\nb\nc")
        buffer.loadFile(file)
        val count = buffer.lineCountState.first()
        assertEquals(3, count)
    }

    // --- Unicode/emoji content (§2) ---

    @Test
    fun testLoadFileWithUnicode() = runTest {
        val file = File(tempDir, "unicode.py")
        file.writeText("hello \u00e9\u00e8\u00ea\n\u0628\u0633\u0645 \u0627\u0644\u0644\u0647")
        buffer.loadFile(file)
        assertEquals(2, buffer.getLineCount())
        assertTrue(buffer.getLine(0)!!.contains("\u00e9"))
    }

    @Test
    fun testLoadFileWithEmoji() = runTest {
        val file = File(tempDir, "emoji.py")
        file.writeText("\uD83D\uDE00 hello\n\uD83D\uDE01 world")
        buffer.loadFile(file)
        assertEquals(2, buffer.getLineCount())
    }

    // --- Extremely long line (§2) ---

    @Test
    fun testLoadFileWithLongLine() = runTest {
        val file = File(tempDir, "long.py")
        val longLine = "x".repeat(100_000)
        file.writeText(longLine)
        buffer.loadFile(file)
        assertEquals(1, buffer.getLineCount())
        assertEquals(100_000, buffer.getLine(0)!!.length)
    }
}
