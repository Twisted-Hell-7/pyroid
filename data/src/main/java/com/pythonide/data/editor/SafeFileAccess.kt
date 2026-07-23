package com.pythonide.data.editor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafeFileAccess @Inject constructor() {
    private val accessLog = ConcurrentHashMap<String, FileAccessEntry>()
    private val blockedPaths = mutableSetOf<String>()

    data class FileAccessEntry(
        val path: String,
        val operation: FileOperation,
        val timestamp: Long = System.currentTimeMillis(),
        val success: Boolean,
        val error: String? = null
    )

    enum class FileOperation {
        READ, WRITE, DELETE, CREATE, EXECUTE, LIST
    }

    data class AccessPolicy(
        val allowedReadPaths: List<String> = listOf(
            System.getProperty("user.home"),
            "/tmp",
            "/sdcard/Documents",
            "/sdcard/Download"
        ),
        val allowedWritePaths: List<String> = listOf(
            System.getProperty("user.home"),
            "/tmp",
            "/sdcard/Documents",
            "/sdcard/Download"
        ),
        val blockedPaths: List<String> = listOf(
            "/etc", "/var", "/usr", "/bin", "/sbin", "/root",
            "/system", "/proc", "/sys", "/data/data"
        ),
        val maxFileSizeBytes: Long = 100 * 1024 * 1024,
        val allowSymlinks: Boolean = false,
        val allowHiddenFiles: Boolean = false
    )

    private var policy = AccessPolicy()

    suspend fun readFile(path: String): FileResult<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)

            if (!validatePath(file, FileOperation.READ)) {
                return@withContext FileResult.Denied("Access denied to path: $path")
            }

            if (!file.exists()) {
                return@withContext FileResult.NotFound("File not found: $path")
            }

            if (file.length() > policy.maxFileSizeBytes) {
                return@withContext FileResult.TooLarge("File too large: ${file.length()} bytes")
            }

            if (!policy.allowHiddenFiles && file.isHidden) {
                return@withContext FileResult.Denied("Hidden files not allowed")
            }

            val content = file.readText()
            logAccess(path, FileOperation.READ, true)
            FileResult.Success(content)
        } catch (e: SecurityException) {
            logAccess(path, FileOperation.READ, false, e.message)
            FileResult.Denied(e.message ?: "Security violation")
        } catch (e: IOException) {
            logAccess(path, FileOperation.READ, false, e.message)
            FileResult.Error(e.message ?: "IO error")
        } catch (e: Exception) {
            logAccess(path, FileOperation.READ, false, e.message)
            FileResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun writeFile(path: String, content: String): FileResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)

            if (!validatePath(file, FileOperation.WRITE)) {
                return@withContext FileResult.Denied("Access denied to path: $path")
            }

            if (content.length > policy.maxFileSizeBytes) {
                return@withContext FileResult.TooLarge("Content too large: ${content.length} bytes")
            }

            val parentDir = file.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }

            file.writeText(content)
            logAccess(path, FileOperation.WRITE, true)
            FileResult.Success(Unit)
        } catch (e: SecurityException) {
            logAccess(path, FileOperation.WRITE, false, e.message)
            FileResult.Denied(e.message ?: "Security violation")
        } catch (e: IOException) {
            logAccess(path, FileOperation.WRITE, false, e.message)
            FileResult.Error(e.message ?: "IO error")
        } catch (e: Exception) {
            logAccess(path, FileOperation.WRITE, false, e.message)
            FileResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun deleteFile(path: String): FileResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)

            if (!validatePath(file, FileOperation.DELETE)) {
                return@withContext FileResult.Denied("Access denied to path: $path")
            }

            if (!file.exists()) {
                return@withContext FileResult.NotFound("File not found: $path")
            }

            if (file.isDirectory) {
                deleteDirectoryRecursive(file)
            } else {
                file.delete()
            }

            logAccess(path, FileOperation.DELETE, true)
            FileResult.Success(Unit)
        } catch (e: SecurityException) {
            logAccess(path, FileOperation.DELETE, false, e.message)
            FileResult.Denied(e.message ?: "Security violation")
        } catch (e: Exception) {
            logAccess(path, FileOperation.DELETE, false, e.message)
            FileResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun listDirectory(path: String): FileResult<List<FileInfo>> = withContext(Dispatchers.IO) {
        try {
            val dir = File(path)

            if (!validatePath(dir, FileOperation.LIST)) {
                return@withContext FileResult.Denied("Access denied to path: $path")
            }

            if (!dir.exists()) {
                return@withContext FileResult.NotFound("Directory not found: $path")
            }

            if (!dir.isDirectory) {
                return@withContext FileResult.Error("Not a directory: $path")
            }

            val files = dir.listFiles()?.map { file ->
                FileInfo(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    isHidden = file.isHidden,
                    isSymlink = isSymlink(file)
                )
            } ?: emptyList()

            val filteredFiles = files.filter { fileInfo ->
                (policy.allowHiddenFiles || !fileInfo.isHidden) &&
                        (policy.allowSymlinks || !fileInfo.isSymlink)
            }

            logAccess(path, FileOperation.LIST, true)
            FileResult.Success(filteredFiles)
        } catch (e: SecurityException) {
            logAccess(path, FileOperation.LIST, false, e.message)
            FileResult.Denied(e.message ?: "Security violation")
        } catch (e: Exception) {
            logAccess(path, FileOperation.LIST, false, e.message)
            FileResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun calculateChecksum(path: String, algorithm: String = "SHA-256"): FileResult<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)

            if (!validatePath(file, FileOperation.READ)) {
                return@withContext FileResult.Denied("Access denied to path: $path")
            }

            if (!file.exists()) {
                return@withContext FileResult.NotFound("File not found: $path")
            }

            if (file.length() > policy.maxFileSizeBytes) {
                return@withContext FileResult.TooLarge("File too large for checksum")
            }

            val digest = MessageDigest.getInstance(algorithm)
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }

            val checksum = digest.digest().joinToString("") { "%02x".format(it) }
            logAccess(path, FileOperation.READ, true)
            FileResult.Success(checksum)
        } catch (e: Exception) {
            logAccess(path, FileOperation.READ, false, e.message)
            FileResult.Error(e.message ?: "Checksum calculation failed")
        }
    }

    private fun validatePath(file: File, operation: FileOperation): Boolean {
        val canonicalPath = try {
            file.canonicalPath
        } catch (e: Exception) {
            return false
        }

        if (blockedPaths.any { canonicalPath.startsWith(it) }) {
            return false
        }

        val allowedPaths = when (operation) {
            FileOperation.READ -> policy.allowedReadPaths
            FileOperation.WRITE, FileOperation.DELETE, FileOperation.CREATE -> policy.allowedWritePaths
            FileOperation.EXECUTE, FileOperation.LIST -> policy.allowedReadPaths
        }

        return allowedPaths.any { canonicalPath.startsWith(it) }
    }

    private fun deleteDirectoryRecursive(file: File) {
        if (!file.exists()) return

        file.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                deleteDirectoryRecursive(child)
            } else {
                child.delete()
            }
        }

        file.delete()
    }

    private fun isSymlink(file: File): Boolean {
        return try {
            val path = file.toPath()
            // Check if the canonical path differs from the absolute path
            // This indicates a symlink
            val canonicalPath = path.toRealPath()
            val absolutePath = path.toAbsolutePath()
            canonicalPath != absolutePath
        } catch (e: Exception) {
            false
        }
    }

    private fun logAccess(path: String, operation: FileOperation, success: Boolean, error: String? = null) {
        val entry = FileAccessEntry(
            path = path,
            operation = operation,
            success = success,
            error = error
        )
        accessLog[path] = entry
    }

    fun getAccessLog(): List<FileAccessEntry> {
        return accessLog.values.sortedByDescending { it.timestamp }
    }

    fun clearAccessLog() {
        accessLog.clear()
    }

    fun updatePolicy(newPolicy: AccessPolicy) {
        policy = newPolicy
        blockedPaths.clear()
        blockedPaths.addAll(newPolicy.blockedPaths)
    }

    fun getPolicy(): AccessPolicy = policy

    sealed class FileResult<out T> {
        data class Success<T>(val data: T) : FileResult<T>()
        data class NotFound(val message: String) : FileResult<Nothing>()
        data class Denied(val message: String) : FileResult<Nothing>()
        data class TooLarge(val message: String) : FileResult<Nothing>()
        data class Error(val message: String) : FileResult<Nothing>()
    }

    data class FileInfo(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long,
        val isHidden: Boolean,
        val isSymlink: Boolean
    )
}
