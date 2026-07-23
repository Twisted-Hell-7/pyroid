package com.pythonide.data.editor

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class OptimizedTextBufferTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var buffer: OptimizedTextBuffer

    @Before
    fun setup() {
        buffer = OptimizedTextBuffer(maxLinesInMemory = 100, chunkSize = 10)
    }

    @Test
    fun `test load file populates buffer`() = runBlocking {
        val file = tempFolder.newFile("test.txt")
        file.writeText("line1\nline2\nline3")

        buffer.loadFile(file)

        assertEquals(3, buffer.getLineCount())
    }

    @Test
    fun `test get line returns correct content`() = runBlocking {
        val file = tempFolder.newFile("test.txt")
        file.writeText("line1\nline2\nline3")

        buffer.loadFile(file)

        assertEquals("line1", buffer.getLine(0))
        assertEquals("line2", buffer.getLine(1))
        assertEquals("line3", buffer.getLine(2))
    }

    @Test
    fun `test get lines returns range`() = runBlocking {
        val file = tempFolder.newFile("test.txt")
        file.writeText("line1\nline2\nline3\nline4\nline5")

        buffer.loadFile(file)

        val lines = buffer.getLines(1, 3)
        assertEquals(2, lines.size)
        assertEquals("line2", lines[0])
        assertEquals("line3", lines[1])
    }

    @Test
    fun `test edit line updates content`() = runBlocking {
        val file = tempFolder.newFile("test.txt")
        file.writeText("line1\nline2\nline3")

        buffer.loadFile(file)
        buffer.editLine(1, "modified")

        assertEquals("modified", buffer.getLine(1))
    }

    @Test
    fun `test insert line adds content`() = runBlocking {
        val file = tempFolder.newFile("test.txt")
        file.writeText("line1\nline3")

        buffer.loadFile(file)
        buffer.insertLine(0, "line2")

        assertEquals(3, buffer.getLineCount())
        assertEquals("line2", buffer.getLine(1))
    }

    @Test
    fun `test delete line removes content`() = runBlocking {
        val file = tempFolder.newFile("test.txt")
        file.writeText("line1\nline2\nline3")

        buffer.loadFile(file)
        buffer.deleteLine(1)

        assertEquals(2, buffer.getLineCount())
        assertEquals("line3", buffer.getLine(1))
    }

    @Test
    fun `test search finds matching lines`() = runBlocking {
        val file = tempFolder.newFile("test.txt")
        file.writeText("hello world\nfoo bar\nhello kotlin")

        buffer.loadFile(file)

        val results = buffer.search("hello")
        assertEquals(2, results.size)
        assertTrue(results.contains(0))
        assertTrue(results.contains(2))
    }

    @Test
    fun `test search with case sensitivity`() = runBlocking {
        val file = tempFolder.newFile("test.txt")
        file.writeText("Hello\nhello\nHELLO")

        buffer.loadFile(file)

        val caseSensitive = buffer.search("hello", caseSensitive = true)
        assertEquals(1, caseSensitive.size)

        val caseInsensitive = buffer.search("hello", caseSensitive = false)
        assertEquals(2, caseInsensitive.size)
    }

    @Test
    fun `test replace all updates content`() = runBlocking {
        val file = tempFolder.newFile("test.txt")
        file.writeText("foo bar foo baz foo")

        buffer.loadFile(file)

        val count = buffer.replaceAll("foo", "replaced")
        assertEquals(3, count)
    }

    @Test
    fun `test clear resets buffer`() = runBlocking {
        val file = tempFolder.newFile("test.txt")
        file.writeText("line1\nline2")

        buffer.loadFile(file)
        buffer.clear()

        assertEquals(0, buffer.getLineCount())
    }

    @Test
    fun `test get memory usage returns positive value`() = runBlocking {
        val file = tempFolder.newFile("test.txt")
        file.writeText("test content")

        buffer.loadFile(file)

        assertTrue(buffer.getMemoryUsage() > 0)
    }

    @Test
    fun `test export to file`() = runBlocking {
        val inputFile = tempFolder.newFile("input.txt")
        inputFile.writeText("line1\nline2\nline3")
        val outputFile = tempFolder.newFile("output.txt")

        buffer.loadFile(inputFile)
        buffer.exportToFile(outputFile)

        assertEquals(inputFile.readText(), outputFile.readText())
    }

    @Test
    fun `test empty file loads correctly`() = runBlocking {
        val file = tempFolder.newFile("empty.txt")

        buffer.loadFile(file)

        assertEquals(0, buffer.getLineCount())
    }

    @Test
    fun `test large file loads in chunks`() = runBlocking {
        val file = tempFolder.newFile("large.txt")
        val lines = (1..50).map { "Line $it" }
        file.writeText(lines.joinToString("\n"))

        buffer.loadFile(file)

        assertEquals(50, buffer.getLineCount())
        assertEquals("Line 1", buffer.getLine(0))
        assertEquals("Line 50", buffer.getLine(49))
    }
}
