package com.pythonide.data.editor

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SafeFileAccessTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var safeFileAccess: SafeFileAccess

    @Before
    fun setup() {
        safeFileAccess = SafeFileAccess()
        safeFileAccess.updatePolicy(
            SafeFileAccess.AccessPolicy(
                allowedReadPaths = listOf(tempFolder.root.absolutePath),
                allowedWritePaths = listOf(tempFolder.root.absolutePath),
                blockedPaths = listOf("/etc", "/var"),
                maxFileSizeBytes = 1024 * 1024,
                allowSymlinks = false,
                allowHiddenFiles = false
            )
        )
    }

    @Test
    fun `test read existing file returns Success`() = runBlocking {
        val file = tempFolder.newFile("test.txt")
        file.writeText("Hello World")

        val result = safeFileAccess.readFile(file.absolutePath)

        assertTrue(result is SafeFileAccess.FileResult.Success)
        assertEquals("Hello World", (result as SafeFileAccess.FileResult.Success).data)
    }

    @Test
    fun `test read non-existent file returns NotFound`() = runBlocking {
        val result = safeFileAccess.readFile("/nonexistent/file.txt")

        assertTrue(result is SafeFileAccess.FileResult.NotFound)
    }

    @Test
    fun `test read blocked path returns Denied`() = runBlocking {
        val result = safeFileAccess.readFile("/etc/passwd")

        assertTrue(result is SafeFileAccess.FileResult.Denied)
    }

    @Test
    fun `test write file returns Success`() = runBlocking {
        val file = File(tempFolder.root, "output.txt")

        val result = safeFileAccess.writeFile(file.absolutePath, "Test content")

        assertTrue(result is SafeFileAccess.FileResult.Success)
        assertTrue(file.exists())
        assertEquals("Test content", file.readText())
    }

    @Test
    fun `test write to blocked path returns Denied`() = runBlocking {
        val result = safeFileAccess.writeFile("/etc/test.txt", "content")

        assertTrue(result is SafeFileAccess.FileResult.Denied)
    }

    @Test
    fun `test delete file returns Success`() = runBlocking {
        val file = tempFolder.newFile("to_delete.txt")

        val result = safeFileAccess.deleteFile(file.absolutePath)

        assertTrue(result is SafeFileAccess.FileResult.Success)
        assertFalse(file.exists())
    }

    @Test
    fun `test delete non-existent file returns NotFound`() = runBlocking {
        val result = safeFileAccess.deleteFile("/nonexistent/file.txt")

        assertTrue(result is SafeFileAccess.FileResult.NotFound)
    }

    @Test
    fun `test list directory returns file list`() = runBlocking {
        tempFolder.newFile("file1.txt")
        tempFolder.newFile("file2.txt")
        tempFolder.newFolder("subdir")

        val result = safeFileAccess.listDirectory(tempFolder.root.absolutePath)

        assertTrue(result is SafeFileAccess.FileResult.Success)
        val files = (result as SafeFileAccess.FileResult.Success).data
        assertEquals(3, files.size)
    }

    @Test
    fun `test list non-existent directory returns NotFound`() = runBlocking {
        val result = safeFileAccess.listDirectory("/nonexistent/dir")

        assertTrue(result is SafeFileAccess.FileResult.NotFound)
    }

    @Test
    fun `test calculate checksum returns valid hash`() = runBlocking {
        val file = tempFolder.newFile("checksum.txt")
        file.writeText("test content")

        val result = safeFileAccess.calculateChecksum(file.absolutePath)

        assertTrue(result is SafeFileAccess.FileResult.Success)
        val checksum = (result as SafeFileAccess.FileResult.Success).data
        assertEquals(64, checksum.length) // SHA-256 produces 64 hex chars
    }

    @Test
    fun `test hidden files are blocked by default`() = runBlocking {
        val hiddenFile = File(tempFolder.root, ".hidden")
        hiddenFile.writeText("hidden content")

        val result = safeFileAccess.readFile(hiddenFile.absolutePath)

        assertTrue(result is SafeFileAccess.FileResult.Denied)
    }

    @Test
    fun `test access log records operations`() = runBlocking {
        val file = tempFolder.newFile("logged.txt")
        file.writeText("content")

        safeFileAccess.readFile(file.absolutePath)
        safeFileAccess.writeFile(file.absolutePath, "new content")

        val log = safeFileAccess.getAccessLog()
        assertTrue(log.isNotEmpty())
    }

    @Test
    fun `test clear access log empties it`() = runBlocking {
        val file = tempFolder.newFile("log_clear.txt")
        safeFileAccess.readFile(file.absolutePath)

        safeFileAccess.clearAccessLog()

        assertTrue(safeFileAccess.getAccessLog().isEmpty())
    }

    @Test
    fun `test policy update changes behavior`() = runBlocking {
        safeFileAccess.updatePolicy(
            SafeFileAccess.AccessPolicy(
                allowedReadPaths = listOf("/tmp"),
                allowedWritePaths = listOf("/tmp"),
                blockedPaths = emptyList()
            )
        )

        val result = safeFileAccess.readFile("/tmp/test.txt")

        // Should not be denied for path reasons (but file won't exist)
        assertFalse(result is SafeFileAccess.FileResult.Denied)
    }
}
