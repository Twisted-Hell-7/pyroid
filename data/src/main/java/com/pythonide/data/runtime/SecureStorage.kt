package com.pythonide.data.runtime

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _storageState = MutableStateFlow<StorageState>(StorageState.Idle)
    val storageState: StateFlow<StorageState> = _storageState.asStateFlow()

    private val _storageStats = MutableStateFlow(StorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val secureDir: File
    private val tempDir: File
    private val cacheDir: File
    private val trackedFiles = ConcurrentHashMap<String, SecureFileInfo>()
    private val totalBytesWritten = AtomicLong(0)
    private var cleanupJob: Job? = null

    sealed class StorageState {
        object Idle : StorageState()
        object Initializing : StorageState()
        data class Active(val fileCount: Int) : StorageState()
        data class Cleanup(val filesDeleted: Int) : StorageState()
        data class Error(val message: String) : StorageState()
    }

    data class StorageConfig(
        val maxStorageBytes: Long = 500 * 1024 * 1024,
        val maxFileAgeMs: Long = 3600_000,
        val cleanupIntervalMs: Long = 300_000,
        val enableAutoCleanup: Boolean = true,
        val enableEncryption: Boolean = false
    )

    data class SecureFileInfo(
        val path: String,
        val size: Long,
        val createdAt: Long,
        val lastAccessedAt: Long,
        val checksum: String,
        val isEncrypted: Boolean = false,
        val metadata: Map<String, String> = emptyMap()
    )

    data class StorageStats(
        val totalFiles: Int = 0,
        val totalSizeBytes: Long = 0,
        val secureDirSize: Long = 0,
        val tempDirSize: Long = 0,
        val cacheDirSize: Long = 0,
        val oldestFileAge: Long = 0,
        val newestFileAge: Long = 0,
        val totalBytesWritten: Long = 0
    )

    private var config = StorageConfig()

    init {
        secureDir = File(context.filesDir, "secure_storage")
        tempDir = File(context.cacheDir, "secure_temp")
        cacheDir = File(context.cacheDir, "secure_cache")

        initializeDirectories()
    }

    private fun initializeDirectories() {
        _storageState.value = StorageState.Initializing

        try {
            secureDir.mkdirs()
            tempDir.mkdirs()
            cacheDir.mkdirs()

            setDirectoryPermissions(secureDir)
            setDirectoryPermissions(tempDir)
            setDirectoryPermissions(cacheDir)

            _storageState.value = StorageState.Active(0)
        } catch (e: Exception) {
            _storageState.value = StorageState.Error(e.message ?: "Failed to initialize storage")
        }
    }

    private fun setDirectoryPermissions(dir: File) {
        try {
            dir.setExecutable(true, false)
            dir.setReadable(true, false)
            dir.setWritable(true, false)
        } catch (e: Exception) {
            // Best effort
        }
    }

    suspend fun createSecureTempFile(
        prefix: String = "secure_",
        suffix: String = ".tmp",
        content: ByteArray? = null
    ): SecureTempFile = withContext(Dispatchers.IO) {
        try {
            val fileName = generateSecureFileName(prefix, suffix)
            val file = File(tempDir, fileName)

            if (content != null) {
                file.writeBytes(content)
                totalBytesWritten.addAndGet(content.size.toLong())
            }

            val info = SecureFileInfo(
                path = file.absolutePath,
                size = file.length(),
                createdAt = System.currentTimeMillis(),
                lastAccessedAt = System.currentTimeMillis(),
                checksum = calculateChecksum(file)
            )

            trackedFiles[file.absolutePath] = info
            updateStats()

            SecureTempFile(file, info)
        } catch (e: Exception) {
            throw IOException("Failed to create secure temp file: ${e.message}")
        }
    }

    suspend fun createSecureTempDirectory(
        prefix: String = "secure_dir_"
    ): File = withContext(Dispatchers.IO) {
        try {
            val dirName = generateSecureFileName(prefix, "")
            val dir = File(tempDir, dirName)
            dir.mkdirs()
            dir

            setDirectoryPermissions(dir)
            dir
        } catch (e: Exception) {
            throw IOException("Failed to create secure temp directory: ${e.message}")
        }
    }

    suspend fun writeSecureFile(
        path: String,
        content: ByteArray,
        metadata: Map<String, String> = emptyMap()
    ): SecureFileInfo = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            ensureParentDirectory(file)

            file.writeBytes(content)
            totalBytesWritten.addAndGet(content.size.toLong())

            val info = SecureFileInfo(
                path = file.absolutePath,
                size = file.length(),
                createdAt = System.currentTimeMillis(),
                lastAccessedAt = System.currentTimeMillis(),
                checksum = calculateChecksum(file),
                metadata = metadata
            )

            trackedFiles[file.absolutePath] = info
            updateStats()

            info
        } catch (e: Exception) {
            throw IOException("Failed to write secure file: ${e.message}")
        }
    }

    suspend fun readSecureFile(path: String): ByteArray = withContext(Dispatchers.IO) {
        try {
            val file = File(path)

            if (!file.exists()) {
                throw IOException("File not found: $path")
            }

            val info = trackedFiles[path]
            if (info != null) {
                val currentChecksum = calculateChecksum(file)
                if (currentChecksum != info.checksum) {
                    throw SecurityException("File integrity check failed")
                }
            }

            val content = file.readBytes()

            trackedFiles[path]?.let { tracked ->
                trackedFiles[path] = tracked.copy(lastAccessedAt = System.currentTimeMillis())
            }

            content
        } catch (e: Exception) {
            throw IOException("Failed to read secure file: ${e.message}")
        }
    }

    suspend fun deleteSecureFile(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(path)

            if (file.exists()) {
                secureDelete(file)
                trackedFiles.remove(path)
                updateStats()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun secureDelete(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { secureDelete(it) }
        }

        val length = file.length()
        if (length > 0) {
            val randomBytes = ByteArray(length.toInt())
            SecureRandom().nextBytes(randomBytes)
            file.writeBytes(randomBytes)
        }

        file.delete()
    }

    suspend fun cleanupExpiredFiles(): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        val currentTime = System.currentTimeMillis()

        val filesToDelete = trackedFiles.values.filter { info ->
            currentTime - info.createdAt > config.maxFileAgeMs
        }

        for (info in filesToDelete) {
            if (deleteSecureFile(info.path)) {
                deletedCount++
            }
        }

        _storageState.value = StorageState.Cleanup(deletedCount)
        deletedCount
    }

    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val secureSize = calculateDirectorySize(secureDir)
        val tempSize = calculateDirectorySize(tempDir)
        val cacheSize = calculateDirectorySize(cacheDir)

        val ages = trackedFiles.values.map { System.currentTimeMillis() - it.createdAt }

        StorageStats(
            totalFiles = trackedFiles.size,
            totalSizeBytes = secureSize + tempSize + cacheSize,
            secureDirSize = secureSize,
            tempDirSize = tempSize,
            cacheDirSize = cacheSize,
            oldestFileAge = ages.maxOrNull() ?: 0,
            newestFileAge = ages.minOrNull() ?: 0,
            totalBytesWritten = totalBytesWritten.get()
        )
    }

    private fun calculateDirectorySize(dir: File): Long {
        var size = 0L
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    size += file.length()
                } else if (file.isDirectory) {
                    size += calculateDirectorySize(file)
                }
            }
        }
        return size
    }

    private fun generateSecureFileName(prefix: String, suffix: String): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        val hash = bytes.joinToString("") { "%02x".format(it) }
        return "${prefix}${hash}${suffix}"
    }

    private fun calculateChecksum(file: File): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    private fun ensureParentDirectory(file: File) {
        file.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
                setDirectoryPermissions(parent)
            }
        }
    }

    private fun updateStats() {
        CoroutineScope(Dispatchers.Default).launch {
            _storageStats.value = getStorageStats()
        }
    }

    fun startAutoCleanup() {
        if (config.enableAutoCleanup) {
            cleanupJob = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    delay(config.cleanupIntervalMs)
                    cleanupExpiredFiles()
                }
            }
        }
    }

    fun stopAutoCleanup() {
        cleanupJob?.cancel()
        cleanupJob = null
    }

    fun updateConfig(newConfig: StorageConfig) {
        config = newConfig
    }

    fun getConfig(): StorageConfig = config

    fun clearAll() {
        trackedFiles.clear()
        secureDir.deleteRecursively()
        tempDir.deleteRecursively()
        cacheDir.deleteRecursively()

        initializeDirectories()
    }

    data class SecureTempFile(
        val file: File,
        val info: SecureFileInfo
    ) : AutoCloseable {
        override fun close() {
            try {
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                // Best effort cleanup
            }
        }
    }
}
