package com.pythonide.data.runtime.repository

import com.pythonide.data.editor.SafeFileAccess
import com.pythonide.data.local.LruCache
import com.pythonide.data.local.CacheManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RepositoryIntegrationTest {

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
                blockedPaths = emptyList(),
                maxFileSizeBytes = 1024 * 1024,
                allowSymlinks = false,
                allowHiddenFiles = true
            )
        )
        CacheManager.clearAll()
    }

    @Test
    fun testFileAccessWithCache() = runBlocking {
        val file = tempFolder.newFile("cached.txt")
        file.writeText("cached content")

        val result1 = safeFileAccess.readFile(file.absolutePath)
        assertTrue(result1 is SafeFileAccess.FileResult.Success)

        val result2 = safeFileAccess.readFile(file.absolutePath)
        assertTrue(result2 is SafeFileAccess.FileResult.Success)
        assertEquals(
            (result1 as SafeFileAccess.FileResult.Success).data,
            (result2 as SafeFileAccess.FileResult.Success).data
        )
    }

    @Test
    fun testFileWriteAndRead() = runBlocking {
        val filePath = tempFolder.root.absolutePath + "/write_test.txt"

        val writeResult = safeFileAccess.writeFile(filePath, "test content")
        assertTrue(writeResult is SafeFileAccess.FileResult.Success)

        val readResult = safeFileAccess.readFile(filePath)
        assertTrue(readResult is SafeFileAccess.FileResult.Success)
        assertEquals("test content", (readResult as SafeFileAccess.FileResult.Success).data)
    }

    @Test
    fun testFileDelete() = runBlocking {
        val file = tempFolder.newFile("delete_test.txt")

        val deleteResult = safeFileAccess.deleteFile(file.absolutePath)
        assertTrue(deleteResult is SafeFileAccess.FileResult.Success)
        assertFalse(file.exists())
    }

    @Test
    fun testDirectoryListing() = runBlocking {
        tempFolder.newFile("list1.txt")
        tempFolder.newFile("list2.txt")

        val result = safeFileAccess.listDirectory(tempFolder.root.absolutePath)
        assertTrue(result is SafeFileAccess.FileResult.Success)

        val files = (result as SafeFileAccess.FileResult.Success).data
        assertTrue(files.size >= 2)
    }

    @Test
    fun testChecksumVerification() = runBlocking {
        val file = tempFolder.newFile("checksum.txt")
        file.writeText("checksum content")

        val result1 = safeFileAccess.calculateChecksum(file.absolutePath)
        val result2 = safeFileAccess.calculateChecksum(file.absolutePath)

        assertTrue(result1 is SafeFileAccess.FileResult.Success)
        assertTrue(result2 is SafeFileAccess.FileResult.Success)
        assertEquals(
            (result1 as SafeFileAccess.FileResult.Success).data,
            (result2 as SafeFileAccess.FileResult.Success).data
        )
    }

    @Test
    fun testCacheManagerIntegration() {
        val fileCache = CacheManager.getFileCache()
        val syntaxCache = CacheManager.getSyntaxCache()

        fileCache.put("file1", "content1")
        syntaxCache.put("syntax1", listOf(
            com.pythonide.data.local.SyntaxToken(0, 5, com.pythonide.data.local.SyntaxTokenType.KEYWORD, 0xFF0000)
        ))

        assertEquals("content1", fileCache.get("file1"))
        assertNotNull(syntaxCache.get("syntax1"))

        val stats = CacheManager.getAllStats()
        assertTrue(stats.containsKey("files"))
        assertTrue(stats.containsKey("syntax"))
    }

    @Test
    fun testAccessLog() = runBlocking {
        val file1 = tempFolder.newFile("log_test1.txt")
        file1.writeText("content1")
        val file2 = tempFolder.newFile("log_test2.txt")
        file2.writeText("content2")

        safeFileAccess.readFile(file1.absolutePath)
        safeFileAccess.writeFile(file2.absolutePath, "new content")

        val log = safeFileAccess.getAccessLog()
        assertTrue(log.size >= 2)
    }
}
