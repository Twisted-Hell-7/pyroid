package com.pythonide.domain.repository

import com.pythonide.domain.model.filemanager.FileManagerItem
import com.pythonide.domain.model.filemanager.FileOperation
import com.pythonide.domain.model.filemanager.FileOperationStatus
import com.pythonide.domain.model.project.Project
import com.pythonide.domain.model.filemanager.RecentFile
import com.pythonide.domain.model.filemanager.StorageSource
import kotlinx.coroutines.flow.Flow

interface FileManagerRepository {
    suspend fun listDirectory(path: String, source: StorageSource = StorageSource.INTERNAL): Result<List<FileManagerItem>>
    suspend fun getItem(path: String, source: StorageSource = StorageSource.INTERNAL): Result<FileManagerItem?>
    suspend fun createFile(path: String, content: ByteArray = ByteArray(0)): Result<FileManagerItem>
    suspend fun createDirectory(path: String): Result<FileManagerItem>
    suspend fun delete(path: String, recursive: Boolean = false): Result<Boolean>
    suspend fun rename(path: String, newName: String): Result<FileManagerItem>
    suspend fun copy(sourcePath: String, destinationPath: String): Result<FileManagerItem>
    suspend fun move(sourcePath: String, destinationPath: String): Result<FileManagerItem>
    suspend fun readContent(path: String): Result<ByteArray>
    suspend fun writeContent(path: String, content: ByteArray): Result<Boolean>
    suspend fun getMimeType(path: String): String?
    suspend fun exists(path: String): Boolean
    suspend fun getSize(path: String): Long
    suspend fun search(query: String, path: String? = null, source: StorageSource = StorageSource.INTERNAL): Result<List<FileManagerItem>>
    suspend fun getFavorites(): Flow<List<FileManagerItem>>
    suspend fun addFavorite(item: FileManagerItem): Result<Boolean>
    suspend fun removeFavorite(path: String): Result<Boolean>
    suspend fun isFavorite(path: String): Boolean
    suspend fun getRecentFiles(): Flow<List<RecentFile>>
    suspend fun addRecentFile(item: FileManagerItem): Result<Boolean>
    suspend fun clearRecentFiles(): Result<Boolean>
    suspend fun getProjects(): Flow<List<Project>>
    suspend fun createProject(name: String, rootPath: String, description: String = ""): Result<Project>
    suspend fun deleteProject(projectId: String): Result<Boolean>
    suspend fun updateProject(project: Project): Result<Project>
    suspend fun openProject(projectId: String): Result<Project>
    suspend fun executeOperation(operation: FileOperation): Flow<FileOperationStatus>
    suspend fun getStorageRoots(): Result<List<FileManagerItem>>
    suspend fun getExternalStoragePath(): String?
    suspend fun requestSafPermission(callback: (String) -> Unit)
    suspend fun importFile(safUri: String, destinationPath: String): Result<FileManagerItem>
    suspend fun exportFile(sourcePath: String, safUri: String): Result<Boolean>
    suspend fun shareFile(path: String): Result<Boolean>
    suspend fun getFileProperties(path: String): Result<Map<String, Any>>
}
