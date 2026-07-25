package com.pythonide.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.pythonide.domain.model.filemanager.FileManagerItem
import com.pythonide.domain.model.filemanager.FileOperation
import com.pythonide.domain.model.filemanager.FileOperationStatus
import com.pythonide.domain.model.filemanager.FilePermissions
import com.pythonide.domain.model.filemanager.FileType
import com.pythonide.domain.model.filemanager.Project
import com.pythonide.domain.model.filemanager.RecentFile
import com.pythonide.domain.model.filemanager.StorageSource
import com.pythonide.domain.repository.FileManagerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManagerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FileManagerRepository {

    private val favorites = MutableStateFlow<List<FileManagerItem>>(emptyList())
    private val recentFiles = MutableStateFlow<List<RecentFile>>(emptyList())
    private val projects = MutableStateFlow<List<Project>>(emptyList())

    private val projectsDir = File(context.filesDir, "projects")
    private val favoritesFile = File(context.filesDir, "favorites.json")
    private val recentFile = File(context.filesDir, "recent.json")

    init {
        projectsDir.mkdirs()
        loadPersistedData()
    }

    private fun loadPersistedData() {
        try {
            if (favoritesFile.exists()) {
                val json = favoritesFile.readText()
                // Parse favorites from JSON
            }
            if (recentFile.exists()) {
                val json = recentFile.readText()
                // Parse recent files from JSON
            }
            loadProjects()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadProjects() {
        val projectFiles = projectsDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
        val loadedProjects = projectFiles.map { dir ->
            Project(
                id = dir.name,
                name = dir.name,
                rootPath = dir.absolutePath,
                lastAccessedAt = dir.lastModified()
            )
        }
        projects.value = loadedProjects
    }

    override suspend fun listDirectory(path: String, source: StorageSource): Result<List<FileManagerItem>> = withContext(Dispatchers.IO) {
        try {
            val dir = when (source) {
                StorageSource.INTERNAL -> File(path)
                StorageSource.EXTERNAL -> File(Environment.getExternalStorageDirectory(), path)
                StorageSource.PROJECT -> File(projectsDir, path)
                else -> File(path)
            }

            if (!dir.exists()) {
                return@withContext Result.failure(Exception("Directory does not exist: $path"))
            }

            if (!dir.isDirectory) {
                return@withContext Result.failure(Exception("Not a directory: $path"))
            }

            val items = dir.listFiles()?.map { file ->
                FileManagerItem(
                    id = file.absolutePath.hashCode().toString(),
                    name = file.name,
                    path = file.absolutePath,
                    parentPath = file.parent,
                    type = when {
                        file.isDirectory -> FileType.DIRECTORY
                        file.isFile -> FileType.FILE
                        else -> FileType.UNKNOWN
                    },
                    size = if (file.isFile) file.length() else 0,
                    lastModified = file.lastModified(),
                    isHidden = file.isHidden,
                    isReadOnly = !file.canWrite(),
                    extension = if (file.isFile) file.extension.takeIf { it.isNotEmpty() } else null,
                    permissions = FilePermissions(
                        read = file.canRead(),
                        write = file.canWrite(),
                        execute = file.canExecute()
                    ),
                    source = source
                )
            } ?: emptyList()

            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getItem(path: String, source: StorageSource): Result<FileManagerItem?> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.success(null)
            }

            val item = FileManagerItem(
                id = file.absolutePath.hashCode().toString(),
                name = file.name,
                path = file.absolutePath,
                parentPath = file.parent,
                type = when {
                    file.isDirectory -> FileType.DIRECTORY
                    file.isFile -> FileType.FILE
                    else -> FileType.UNKNOWN
                },
                size = if (file.isFile) file.length() else 0,
                lastModified = file.lastModified(),
                isHidden = file.isHidden,
                isReadOnly = !file.canWrite(),
                extension = if (file.isFile) file.extension.takeIf { it.isNotEmpty() } else null,
                permissions = FilePermissions(
                    read = file.canRead(),
                    write = file.canWrite(),
                    execute = file.canExecute()
                ),
                source = source
            )

            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createFile(path: String, content: ByteArray): Result<FileManagerItem> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.exists()) {
                return@withContext Result.failure(Exception("File already exists: $path"))
            }

            file.parentFile?.mkdirs()
            file.writeBytes(content)

            getItem(path).map { it ?: throw Exception("Failed to create file") }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createDirectory(path: String): Result<FileManagerItem> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.exists()) {
                return@withContext Result.failure(Exception("Directory already exists: $path"))
            }

            if (file.mkdirs()) {
                getItem(path).map { it ?: throw Exception("Failed to create directory") }
            } else {
                Result.failure(Exception("Failed to create directory: $path"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(path: String, recursive: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File does not exist: $path"))
            }

            val success = if (file.isDirectory) {
                if (recursive) {
                    file.deleteRecursively()
                } else {
                    file.listFiles()?.isEmpty() == true && file.delete()
                }
            } else {
                file.delete()
            }

            if (success) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to delete: $path"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rename(path: String, newName: String): Result<FileManagerItem> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File does not exist: $path"))
            }

            val newFile = File(file.parent, newName)
            if (newFile.exists()) {
                return@withContext Result.failure(Exception("A file with that name already exists"))
            }

            if (file.renameTo(newFile)) {
                getItem(newFile.absolutePath).map { it ?: throw Exception("Failed to rename file") }
            } else {
                Result.failure(Exception("Failed to rename file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun copy(sourcePath: String, destinationPath: String): Result<FileManagerItem> = withContext(Dispatchers.IO) {
        try {
            val source = File(sourcePath)
            if (!source.exists()) {
                return@withContext Result.failure(Exception("Source file does not exist: $sourcePath"))
            }

            val dest = File(destinationPath)
            if (source.isDirectory) {
                source.copyRecursively(dest)
            } else {
                dest.parentFile?.mkdirs()
                source.copyTo(dest, overwrite = true)
            }

            getItem(destinationPath).map { it ?: throw Exception("Failed to copy file") }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun move(sourcePath: String, destinationPath: String): Result<FileManagerItem> = withContext(Dispatchers.IO) {
        try {
            val source = File(sourcePath)
            if (!source.exists()) {
                return@withContext Result.failure(Exception("Source file does not exist: $sourcePath"))
            }

            val dest = File(destinationPath)
            dest.parentFile?.mkdirs()

            if (source.renameTo(dest)) {
                getItem(destinationPath).map { it ?: throw Exception("Failed to move file") }
            } else {
                // Fallback to copy + delete
                if (source.isDirectory) {
                    source.copyRecursively(dest)
                    source.deleteRecursively()
                } else {
                    source.copyTo(dest, overwrite = true)
                    source.delete()
                }
                getItem(destinationPath).map { it ?: throw Exception("Failed to move file") }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun readContent(path: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File does not exist: $path"))
            }

            if (!file.canRead()) {
                return@withContext Result.failure(Exception("Cannot read file: $path"))
            }

            Result.success(file.readBytes())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun writeContent(path: String, content: ByteArray): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeBytes(content)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMimeType(path: String): String? {
        return android.webkit.MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(File(path).extension.lowercase())
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).exists()
    }

    override suspend fun getSize(path: String): Long = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.isFile) file.length() else 0
    }

    override suspend fun search(query: String, path: String?, source: StorageSource): Result<List<FileManagerItem>> = withContext(Dispatchers.IO) {
        try {
            val searchPath = path ?: context.filesDir.absolutePath
            val dir = File(searchPath)
            val results = mutableListOf<FileManagerItem>()

            dir.walkTopDown()
                .maxDepth(10)
                .filter { it.name.contains(query, ignoreCase = true) }
                .forEach { file ->
                    results.add(
                        FileManagerItem(
                            id = file.absolutePath.hashCode().toString(),
                            name = file.name,
                            path = file.absolutePath,
                            parentPath = file.parent,
                            type = when {
                                file.isDirectory -> FileType.DIRECTORY
                                file.isFile -> FileType.FILE
                                else -> FileType.UNKNOWN
                            },
                            size = if (file.isFile) file.length() else 0,
                            lastModified = file.lastModified(),
                            isHidden = file.isHidden,
                            extension = if (file.isFile) file.extension.takeIf { it.isNotEmpty() } else null,
                            source = source
                        )
                    )
                }

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFavorites(): Flow<List<FileManagerItem>> = favorites

    override suspend fun addFavorite(item: FileManagerItem): Result<Boolean> {
        val current = favorites.value.toMutableList()
        if (current.none { it.path == item.path }) {
            current.add(item)
            favorites.value = current
            persistFavorites()
        }
        return Result.success(true)
    }

    override suspend fun removeFavorite(path: String): Result<Boolean> {
        val current = favorites.value.toMutableList()
        current.removeAll { it.path == path }
        favorites.value = current
        persistFavorites()
        return Result.success(true)
    }

    override suspend fun isFavorite(path: String): Boolean {
        return favorites.value.any { it.path == path }
    }

    private fun persistFavorites() {
        // Persist to JSON file
    }

    override suspend fun getRecentFiles(): Flow<List<RecentFile>> = recentFiles

    override suspend fun addRecentFile(item: FileManagerItem): Result<Boolean> {
        val current = recentFiles.value.toMutableList()
        val existing = current.find { it.item.path == item.path }
        if (existing != null) {
            current.remove(existing)
            current.add(0, existing.copy(accessedAt = System.currentTimeMillis(), accessCount = existing.accessCount + 1))
        } else {
            current.add(0, RecentFile(item = item))
        }
        if (current.size > 50) {
            current.subList(50, current.size).clear()
        }
        recentFiles.value = current
        persistRecentFiles()
        return Result.success(true)
    }

    override suspend fun clearRecentFiles(): Result<Boolean> {
        recentFiles.value = emptyList()
        persistRecentFiles()
        return Result.success(true)
    }

    private fun persistRecentFiles() {
        // Persist to JSON file
    }

    override suspend fun getProjects(): Flow<List<Project>> = projects

    override suspend fun createProject(name: String, rootPath: String, description: String): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(rootPath)
            if (!projectDir.exists()) {
                projectDir.mkdirs()
            }

            val project = Project(
                id = UUID.randomUUID().toString(),
                name = name,
                rootPath = rootPath,
                description = description
            )

            val current = projects.value.toMutableList()
            current.add(project)
            projects.value = current

            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProject(projectId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val current = projects.value.toMutableList()
            val project = current.find { it.id == projectId }
            if (project != null) {
                val dir = File(project.rootPath)
                if (dir.exists()) {
                    dir.deleteRecursively()
                }
                current.removeAll { it.id == projectId }
                projects.value = current
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProject(project: Project): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val current = projects.value.toMutableList()
            val index = current.indexOfFirst { it.id == project.id }
            if (index != -1) {
                current[index] = project
                projects.value = current
            }
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun openProject(projectId: String): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val project = projects.value.find { it.id == projectId }
            if (project != null) {
                val updated = project.copy(lastAccessedAt = System.currentTimeMillis())
                updateProject(updated)
                Result.success(updated)
            } else {
                Result.failure(Exception("Project not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun executeOperation(operation: FileOperation): Flow<FileOperationStatus> = flow {
        emit(FileOperationStatus.IN_PROGRESS)
        try {
            when (operation.type) {
                com.pythonide.domain.model.filemanager.FileOperationType.CREATE_FILE -> {
                    operation.sourcePaths.forEach { path ->
                        createFile(path)
                    }
                }
                com.pythonide.domain.model.filemanager.FileOperationType.CREATE_DIRECTORY -> {
                    operation.sourcePaths.forEach { path ->
                        createDirectory(path)
                    }
                }
                com.pythonide.domain.model.filemanager.FileOperationType.DELETE -> {
                    operation.sourcePaths.forEach { path ->
                        delete(path, recursive = true)
                    }
                }
                com.pythonide.domain.model.filemanager.FileOperationType.RENAME -> {
                    val source = operation.sourcePaths.firstOrNull()
                    val newName = operation.newName
                    if (source != null && newName != null) {
                        rename(source, newName)
                    }
                }
                com.pythonide.domain.model.filemanager.FileOperationType.COPY -> {
                    val dest = operation.destinationPath ?: return@flow
                    operation.sourcePaths.forEach { source ->
                        val newFile = File(dest, File(source).name)
                        copy(source, newFile.absolutePath)
                    }
                }
                com.pythonide.domain.model.filemanager.FileOperationType.MOVE -> {
                    val dest = operation.destinationPath ?: return@flow
                    operation.sourcePaths.forEach { source ->
                        val newFile = File(dest, File(source).name)
                        move(source, newFile.absolutePath)
                    }
                }
                else -> {}
            }
            emit(FileOperationStatus.COMPLETED)
        } catch (e: Exception) {
            emit(FileOperationStatus.FAILED)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getStorageRoots(): Result<List<FileManagerItem>> = withContext(Dispatchers.IO) {
        try {
            val roots = mutableListOf<FileManagerItem>()

            // Internal storage
            roots.add(
                FileManagerItem(
                    id = "internal",
                    name = "Internal Storage",
                    path = context.filesDir.absolutePath,
                    type = FileType.DIRECTORY,
                    source = StorageSource.INTERNAL
                )
            )

            // External storage
            val externalDir = Environment.getExternalStorageDirectory()
            if (externalDir.exists()) {
                roots.add(
                    FileManagerItem(
                        id = "external",
                        name = "External Storage",
                        path = externalDir.absolutePath,
                        type = FileType.DIRECTORY,
                        source = StorageSource.EXTERNAL
                    )
                )
            }

            // App-specific external storage
            val appExternalDir = context.getExternalFilesDir(null)
            if (appExternalDir != null) {
                roots.add(
                    FileManagerItem(
                        id = "app_external",
                        name = "App Storage",
                        path = appExternalDir.absolutePath,
                        type = FileType.DIRECTORY,
                        source = StorageSource.EXTERNAL
                    )
                )
            }

            Result.success(roots)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getExternalStoragePath(): String? = withContext(Dispatchers.IO) {
        Environment.getExternalStorageDirectory().absolutePath
    }

    override suspend fun requestSafPermission(callback: (String) -> Unit) {
        // SAF permission request would be handled by the UI layer
    }

    override suspend fun importFile(safUri: String, destinationPath: String): Result<FileManagerItem> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(safUri)
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open URI"))

            val destFile = File(destinationPath)
            destFile.parentFile?.mkdirs()

            FileOutputStream(destFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()

            getItem(destinationPath).map { it ?: throw Exception("Failed to import file") }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportFile(sourcePath: String, safUri: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(sourcePath)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File does not exist"))
            }

            val uri = Uri.parse(safUri)
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open URI"))

            file.inputStream().use { input ->
                input.copyTo(outputStream)
            }
            outputStream.close()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun shareFile(path: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File does not exist"))
            }

            // Share intent would be handled by the UI layer
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFileProperties(path: String): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File does not exist"))
            }

            val properties = mutableMapOf<String, Any>(
                "name" to file.name,
                "path" to file.absolutePath,
                "type" to if (file.isDirectory) "Directory" else "File",
                "size" to formatFileSize(file.length()),
                "sizeBytes" to file.length(),
                "lastModified" to java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(file.lastModified())),
                "canRead" to file.canRead(),
                "canWrite" to file.canWrite(),
                "canExecute" to file.canExecute(),
                "isHidden" to file.isHidden
            )

            if (file.isFile) {
                properties["extension"] = file.extension
                properties["mimeType"] = getMimeType(path) ?: "unknown"
            }

            Result.success(properties)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }
}
