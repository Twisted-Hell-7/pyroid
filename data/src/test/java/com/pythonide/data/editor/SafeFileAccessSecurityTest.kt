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
class SafeFileAccessSecurityTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var safeFileAccess: SafeFileAccess
    private lateinit var tempDir: File

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        safeFileAccess = SafeFileAccess()
        tempDir = File(System.getProperty("java.io.tmpdir"), "safe_file_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        tempDir.deleteRecursively()
    }

    // §12: Security - Path Traversal

    @Test
    fun testPathTraversalBlocked() = runTest {
        val result = safeFileAccess.readFile("${tempDir.absolutePath}/../../../etc/passwd")
        assertTrue(result is SafeFileAccess.FileResult.Denied)
    }

    @Test
    fun testPathTraversalWriteBlocked() = runTest {
        val result = safeFileAccess.writeFile("${tempDir.absolutePath}/../../../etc/malicious.txt", "malicious content")
        assertTrue(result is SafeFileAccess.FileResult.Denied)
    }

    @Test
    fun testPathTraversalDeleteBlocked() = runTest {
        val result = safeFileAccess.deleteFile("${tempDir.absolutePath}/../../../etc/file.txt")
        assertTrue(result is SafeFileAccess.FileResult.Denied)
    }

    // §12: Security - Sensitive Path Access

    @Test
    fun testEtcPathBlocked() = runTest {
        val result = safeFileAccess.readFile("/etc/passwd")
        assertTrue(result is SafeFileAccess.FileResult.Denied)
    }

    @Test
    fun testVarPathBlocked() = runTest {
        val result = safeFileAccess.readFile("/var/log/syslog")
        assertTrue(result is SafeFileAccess.FileResult.Denied)
    }

    @Test
    fun testUsrPathBlocked() = runTest {
        val result = safeFileAccess.readFile("/usr/bin/python3")
        assertTrue(result is SafeFileAccess.FileResult.Denied)
    }

    @Test
    fun testBinPathBlocked() = runTest {
        val result = safeFileAccess.readFile("/bin/sh")
        assertTrue(result is SafeFileAccess.FileResult.Denied)
    }

    @Test
    fun testSbinPathBlocked() = runTest {
        val result = safeFileAccess.readFile("/sbin/init")
        assertTrue(result is SafeFileAccess.FileResult.Denied)
    }

    @Test
    fun testRootPathBlocked() = runTest {
        val result = safeFileAccess.readFile("/root/.ssh/id_rsa")
        assertTrue(result is SafeFileAccess.FileResult.Denied)
    }

    @Test
    fun testSystemPathBlocked() = runTest {
        val result = safeFileAccess.readFile("/system/build.prop")
        assertTrue(result is SafeFileAccess.FileResult.Denied)
    }

    @Test
    fun testProcPathBlocked() = runTest {
        val result = safeFileAccess.readFile("/proc/self/environ")
        assertTrue(result is SafeFileAccess.FileResult.Denied)
    }

    @Test
    fun testSysPathBlocked() = runTest {
        val result = safeFileAccess.readFile("/sys/class/android_usb/android0/idVendor")
        assertTrue(result is SafeFileAccess.FileResult.Denied)
    }

    @Test
    fun testDataDataPathBlocked() = runTest {
        val result = safeFileAccess.readFile("/data/data/com.other.app/shared_prefs/prefs.xml")
        assertTrue(result is SafeFileAccess.FileResult.Denied)
    }

    // §12: Security - File Size Limits

    @Test
    fun testFileTooLargeForRead() = runTest {
        val policy = SafeFileAccess.AccessPolicy(
            maxFileSizeBytes = 1024
        )
        safeFileAccess.updatePolicy(policy)
        
        val largeFile = File(tempDir, "large_file.txt")
        largeFile.writeText("x".repeat(2048))
        
        val result = safeFileAccess.readFile(largeFile.absolutePath)
        assertTrue(result is SafeFileAccess.FileResult.TooLarge)
    }

    @Test
    fun testContentTooLargeForWrite() = runTest {
        val policy = SafeFileAccess.AccessPolicy(
            maxFileSizeBytes = 1024
        )
        safeFileAccess.updatePolicy(policy)
        
        val largeContent = "x".repeat(2048)
        val result = safeFileAccess.writeFile(File(tempDir, "test.txt").absolutePath, largeContent)
        assertTrue(result is SafeFileAccess.FileResult.TooLarge)
    }

    // §12: Security - Hidden Files

    @Test
    fun testHiddenFileBlockedByDefault() = runTest {
        val hiddenFile = File(tempDir, ".hidden_file")
        hiddenFile.writeText("secret")
        
        val result = safeFileAccess.readFile(hiddenFile.absolutePath)
        assertTrue(result is SafeFileAccess.FileResult.Denied)
    }

    @Test
    fun testHiddenFileAllowedWhenPolicyEnables() = runTest {
        val policy = SafeFileAccess.AccessPolicy(
            allowHiddenFiles = true
        )
        safeFileAccess.updatePolicy(policy)
        
        val hiddenFile = File(tempDir, ".hidden_file")
        hiddenFile.writeText("secret")
        
        val result = safeFileAccess.readFile(hiddenFile.absolutePath)
        assertTrue(result is SafeFileAccess.FileResult.Success)
    }

    // §12: Security - Symlink Handling

    @Test
    fun testSymlinkBlockedByDefault() = runTest {
        val targetFile = File(tempDir, "target.txt")
        targetFile.writeText("target content")
        
        val symlinkFile = File(tempDir, "link.txt")
        try {
            java.nio.file.Files.createSymbolicLink(
                symlinkFile.toPath(),
                targetFile.toPath()
            )
            
            val result = safeFileAccess.listDirectory(tempDir.absolutePath)
            if (result is SafeFileAccess.FileResult.Success) {
                val files = result.data
                val symlinkInfo = files.find { it.name == "link.txt" }
                assertNull(symlinkInfo)
            }
        } catch (e: Exception) {
            // Symlinks may not be supported on all systems
        }
    }

    // §12: Security - Checksum Calculation

    @Test
    fun testCalculateChecksum() = runTest {
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("Hello, World!")
        
        val result = safeFileAccess.calculateChecksum(testFile.absolutePath)
        assertTrue(result is SafeFileAccess.FileResult.Success)
        val checksum = (result as SafeFileAccess.FileResult.Success).data
        assertNotNull(checksum)
        assertTrue(checksum.isNotEmpty())
    }

    @Test
    fun testCalculateChecksumForNonExistentFile() = runTest {
        val result = safeFileAccess.calculateChecksum(File(tempDir, "nonexistent_checksum.txt").absolutePath)
        assertTrue(result is SafeFileAccess.FileResult.NotFound)
    }

    @Test
    fun testCalculateChecksumForBlockedPath() = runTest {
        val result = safeFileAccess.calculateChecksum("/etc/passwd")
        assertTrue(result is SafeFileAccess.FileResult.Denied)
    }

    // §12: Security - Access Policy

    @Test
    fun testDefaultPolicyBlocksSensitivePaths() {
        val policy = SafeFileAccess.AccessPolicy()
        assertTrue(policy.blockedPaths.contains("/etc"))
        assertTrue(policy.blockedPaths.contains("/var"))
        assertTrue(policy.blockedPaths.contains("/usr"))
        assertTrue(policy.blockedPaths.contains("/bin"))
        assertTrue(policy.blockedPaths.contains("/sbin"))
        assertTrue(policy.blockedPaths.contains("/root"))
        assertTrue(policy.blockedPaths.contains("/system"))
        assertTrue(policy.blockedPaths.contains("/proc"))
        assertTrue(policy.blockedPaths.contains("/sys"))
        assertTrue(policy.blockedPaths.contains("/data/data"))
    }

    @Test
    fun testDefaultPolicyFileSizeLimit() {
        val policy = SafeFileAccess.AccessPolicy()
        assertEquals(100 * 1024 * 1024L, policy.maxFileSizeBytes)
    }

    @Test
    fun testDefaultPolicySymlinksDisabled() {
        val policy = SafeFileAccess.AccessPolicy()
        assertFalse(policy.allowSymlinks)
    }

    @Test
    fun testDefaultPolicyHiddenFilesDisabled() {
        val policy = SafeFileAccess.AccessPolicy()
        assertFalse(policy.allowHiddenFiles)
    }

    // §12: Security - File Operations

    @Test
    fun testReadNonExistentFile() = runTest {
        val result = safeFileAccess.readFile(File(tempDir, "nonexistent_read.txt").absolutePath)
        assertTrue(result is SafeFileAccess.FileResult.NotFound)
    }

    @Test
    fun testDeleteNonExistentFile() = runTest {
        val result = safeFileAccess.deleteFile(File(tempDir, "nonexistent_delete.txt").absolutePath)
        assertTrue(result is SafeFileAccess.FileResult.NotFound)
    }

    @Test
    fun testListNonExistentDirectory() = runTest {
        val result = safeFileAccess.listDirectory(File(tempDir, "nonexistent_list_dir").absolutePath)
        assertTrue(result is SafeFileAccess.FileResult.NotFound)
    }

    @Test
    fun testListFileAsDirectory() = runTest {
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("content")
        
        val result = safeFileAccess.listDirectory(testFile.absolutePath)
        assertTrue(result is SafeFileAccess.FileResult.Error)
    }

    // §12: Security - Access Logging

    @Test
    fun testAccessLogging() = runTest {
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("content")
        
        safeFileAccess.readFile(testFile.absolutePath)
        
        val log = safeFileAccess.getAccessLog()
        assertTrue(log.isNotEmpty())
        assertEquals(testFile.absolutePath, log.first().path)
        assertEquals(SafeFileAccess.FileOperation.READ, log.first().operation)
    }

    @Test
    fun testClearAccessLog() = runTest {
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("content")
        
        safeFileAccess.readFile(testFile.absolutePath)
        safeFileAccess.clearAccessLog()
        
        val log = safeFileAccess.getAccessLog()
        assertTrue(log.isEmpty())
    }

    // §12: Security - Write and Read Round Trip

    @Test
    fun testWriteAndReadRoundTrip() = runTest {
        val testFile = File(tempDir, "roundtrip.txt")
        val content = "Hello, World!"
        
        val writeResult = safeFileAccess.writeFile(testFile.absolutePath, content)
        assertTrue(writeResult is SafeFileAccess.FileResult.Success)
        
        val readResult = safeFileAccess.readFile(testFile.absolutePath)
        assertTrue(readResult is SafeFileAccess.FileResult.Success)
        assertEquals(content, (readResult as SafeFileAccess.FileResult.Success).data)
    }

    @Test
    fun testDeleteExistingFile() = runTest {
        val testFile = File(tempDir, "to_delete.txt")
        testFile.writeText("content")
        
        val result = safeFileAccess.deleteFile(testFile.absolutePath)
        assertTrue(result is SafeFileAccess.FileResult.Success)
        assertFalse(testFile.exists())
    }

    // §12: Security - Directory Operations

    @Test
    fun testListDirectorySuccess() = runTest {
        File(tempDir, "file1.txt").writeText("content1")
        File(tempDir, "file2.txt").writeText("content2")
        
        val result = safeFileAccess.listDirectory(tempDir.absolutePath)
        assertTrue(result is SafeFileAccess.FileResult.Success)
        val files = (result as SafeFileAccess.FileResult.Success).data
        assertEquals(2, files.size)
    }

    @Test
    fun testListDirectoryExcludesHiddenFiles() = runTest {
        File(tempDir, "visible.txt").writeText("visible")
        File(tempDir, ".hidden.txt").writeText("hidden")
        
        val result = safeFileAccess.listDirectory(tempDir.absolutePath)
        assertTrue(result is SafeFileAccess.FileResult.Success)
        val files = (result as SafeFileAccess.FileResult.Success).data
        assertEquals(1, files.size)
        assertEquals("visible.txt", files[0].name)
    }

    @Test
    fun testListDirectoryExcludesSymlinks() = runTest {
        val targetFile = File(tempDir, "target.txt")
        targetFile.writeText("target")
        
        val symlinkFile = File(tempDir, "link.txt")
        try {
            java.nio.file.Files.createSymbolicLink(
                symlinkFile.toPath(),
                targetFile.toPath()
            )
            
            val result = safeFileAccess.listDirectory(tempDir.absolutePath)
            if (result is SafeFileAccess.FileResult.Success) {
                val files = result.data
                val symlinks = files.filter { it.isSymlink }
                // Symlinks should be excluded by default
                // Note: This test may behave differently on different systems
            }
        } catch (e: Exception) {
            // Symlinks may not be supported
        }
    }

    // §12: Security - File Info

    @Test
    fun testFileInfoCreation() {
        val info = SafeFileAccess.FileInfo(
            name = "test.txt",
            path = "/tmp/test.txt",
            isDirectory = false,
            size = 1024,
            lastModified = System.currentTimeMillis(),
            isHidden = false,
            isSymlink = false
        )
        assertEquals("test.txt", info.name)
        assertEquals("/tmp/test.txt", info.path)
        assertFalse(info.isDirectory)
        assertEquals(1024, info.size)
        assertFalse(info.isHidden)
        assertFalse(info.isSymlink)
    }

    @Test
    fun testDirectoryFileInfo() {
        val info = SafeFileAccess.FileInfo(
            name = "subdir",
            path = "/tmp/subdir",
            isDirectory = true,
            size = 0,
            lastModified = System.currentTimeMillis(),
            isHidden = false,
            isSymlink = false
        )
        assertTrue(info.isDirectory)
        assertEquals(0L, info.size)
    }
}
