package com.pythonide.domain.model.filemanager

import java.util.UUID

data class FileManagerItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val path: String,
    val parentPath: String? = null,
    val type: FileType,
    val size: Long = 0,
    val lastModified: Long = System.currentTimeMillis(),
    val isHidden: Boolean = false,
    val isReadOnly: Boolean = false,
    val mimeType: String? = null,
    val extension: String? = null,
    val permissions: FilePermissions = FilePermissions(),
    val source: StorageSource = StorageSource.INTERNAL
)

enum class FileType {
    FILE,
    DIRECTORY,
    SYMLINK,
    UNKNOWN
}

enum class StorageSource {
    INTERNAL,
    EXTERNAL,
    SAF,
    PROJECT,
    RECENT,
    FAVORITES
}

data class FilePermissions(
    val read: Boolean = true,
    val write: Boolean = true,
    val execute: Boolean = false
)

enum class SortBy {
    NAME,
    SIZE,
    DATE,
    TYPE,
    EXTENSION
}

enum class SortOrder {
    ASCENDING,
    DESCENDING
}

enum class ViewMode {
    LIST,
    GRID,
    COMPACT
}

enum class FileAction {
    CREATE,
    DELETE,
    RENAME,
    COPY,
    MOVE,
    SHARE,
    IMPORT,
    EXPORT,
    OPEN,
    PROPERTIES
}

data class FileOperation(
    val id: String = UUID.randomUUID().toString(),
    val type: FileOperationType,
    val sourcePaths: List<String>,
    val destinationPath: String? = null,
    val newName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: FileOperationStatus = FileOperationStatus.PENDING
)

enum class FileOperationType {
    CREATE_FILE,
    CREATE_DIRECTORY,
    DELETE,
    RENAME,
    COPY,
    MOVE,
    IMPORT,
    EXPORT
}

enum class FileOperationStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val rootPath: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val fileCount: Int = 0,
    val totalSize: Long = 0
)

data class RecentFile(
    val item: FileManagerItem,
    val accessedAt: Long = System.currentTimeMillis(),
    val accessCount: Int = 1
)

data class FileManagerState(
    val currentPath: String = "/",
    val items: List<FileManagerItem> = emptyList(),
    val selectedItems: Set<String> = emptySet(),
    val viewMode: ViewMode = ViewMode.LIST,
    val sortBy: SortBy = SortBy.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val searchQuery: String = "",
    val storageSource: StorageSource = StorageSource.INTERNAL,
    val projects: List<Project> = emptyList(),
    val recentFiles: List<RecentFile> = emptyList(),
    val favorites: List<FileManagerItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentOperation: FileOperation? = null,
    val clipboardItems: List<String> = emptyList(),
    val clipboardAction: FileAction? = null,
    val showHidden: Boolean = false,
    val showSystem: Boolean = false
) {
    val filteredItems: List<FileManagerItem>
        get() = items
            .filter { item ->
                (showHidden || !item.isHidden) &&
                (searchQuery.isEmpty() || item.name.contains(searchQuery, ignoreCase = true))
            }
            .sortedWith(
                when (sortBy) {
                    SortBy.NAME -> compareBy<FileManagerItem> { it.name.lowercase() }
                    SortBy.SIZE -> compareBy<FileManagerItem> { it.size }
                    SortBy.DATE -> compareBy<FileManagerItem> { it.lastModified }
                    SortBy.TYPE -> compareBy<FileManagerItem> { it.type.name }
                    SortBy.EXTENSION -> compareBy<FileManagerItem> { it.extension ?: "" }
                }.let { if (sortOrder == SortOrder.DESCENDING) it.reversed() else it }
            )

    val breadcrumb: List<String>
        get() = currentPath.split("/").filter { it.isNotEmpty() }

    val totalSelectedSize: Long
        get() = items.filter { it.id in selectedItems }.sumOf { it.size }
}

data class FileManagerConfig(
    val defaultSortBy: SortBy = SortBy.NAME,
    val defaultSortOrder: SortOrder = SortOrder.ASCENDING,
    val defaultViewMode: ViewMode = ViewMode.LIST,
    val showHiddenFiles: Boolean = false,
    val showSystemFiles: Boolean = false,
    val enableThumbnails: Boolean = true,
    val confirmDelete: Boolean = true,
    val maxRecentFiles: Int = 50,
    val maxRecentProjects: Int = 20
)
