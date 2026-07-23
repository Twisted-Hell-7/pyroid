package com.pythonide.domain.model.project

import java.util.UUID

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val rootPath: String,
    val description: String = "",
    val templateId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val lastSavedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val fileCount: Int = 0,
    val totalSize: Long = 0,
    val metadata: ProjectMetadata = ProjectMetadata(),
    val tags: List<String> = emptyList()
)

data class ProjectMetadata(
    val pythonVersion: String = "3.11",
    val interpreterPath: String = "python3",
    val author: String = "",
    val version: String = "1.0.0",
    val license: String = "",
    val dependencies: List<String> = emptyList(),
    val gitRepository: String? = null,
    val customProperties: Map<String, String> = emptyMap()
)

data class ProjectTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val category: TemplateCategory,
    val icon: TemplateIcon = TemplateIcon.DEFAULT,
    val files: List<TemplateFile> = emptyList(),
    val directories: List<String> = emptyList(),
    val gitignoreContent: String? = null,
    val requirementsContent: String? = null,
    val isBuiltIn: Boolean = true
)

enum class TemplateCategory {
    BLANK,
    CONSOLE_APP,
    WEB_APP,
    DATA_SCIENCE,
    GAME,
    API,
    AUTOMATION,
    TESTING,
    CUSTOM
}

enum class TemplateIcon {
    DEFAULT,
    CONSOLE,
    WEB,
    DATA,
    GAME,
    API,
    GEAR,
    TEST
}

data class TemplateFile(
    val path: String,
    val content: String,
    val isExecutable: Boolean = false
)

data class ProjectSession(
    val projectId: String,
    val openFiles: List<SessionFile> = emptyList(),
    val activeFileId: String? = null,
    val cursorPositions: Map<String, CursorPosition> = emptyMap(),
    val scrollPositions: Map<String, ScrollPosition> = emptyMap(),
    val splitLayout: SplitLayout = SplitLayout.NONE,
    val terminalHistory: List<String> = emptyList(),
    val lastSavedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

data class SessionFile(
    val fileId: String,
    val path: String,
    val name: String,
    val isModified: Boolean = false,
    val openedAt: Long = System.currentTimeMillis()
)

data class CursorPosition(
    val line: Int,
    val column: Int
)

data class ScrollPosition(
    val x: Float,
    val y: Float
)

enum class SplitLayout {
    NONE,
    HORIZONTAL,
    VERTICAL
}

data class ProjectBackup(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val projectName: String,
    val backupPath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val size: Long = 0,
    val fileCount: Int = 0,
    val description: String = ""
)

data class AutoSaveConfig(
    val enabled: Boolean = true,
    val intervalMs: Long = 30_000L,
    val saveOnClose: Boolean = true,
    val saveOnSwitch: Boolean = true,
    val maxAutoSaves: Int = 10
)

data class ProjectState(
    val projects: List<Project> = emptyList(),
    val currentProject: Project? = null,
    val templates: List<ProjectTemplate> = emptyList(),
    val recentProjects: List<Project> = emptyList(),
    val backups: List<ProjectBackup> = emptyList(),
    val currentSession: ProjectSession? = null,
    val autoSaveConfig: AutoSaveConfig = AutoSaveConfig(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val sortBy: ProjectSortBy = ProjectSortBy.LAST_ACCESSED,
    val sortOrder: ProjectSortOrder = ProjectSortOrder.DESCENDING,
    val showFavoritesOnly: Boolean = false
) {
    val filteredProjects: List<Project>
        get() = projects
            .filter { project ->
                (searchQuery.isEmpty() || project.name.contains(searchQuery, ignoreCase = true) ||
                        project.description.contains(searchQuery, ignoreCase = true)) &&
                (!showFavoritesOnly || project.isFavorite)
            }
            .sortedWith(
                when (sortBy) {
                    ProjectSortBy.NAME -> compareBy<Project> { it.name.lowercase() }
                    ProjectSortBy.CREATED -> compareBy<Project> { it.createdAt }
                    ProjectSortBy.LAST_ACCESSED -> compareBy<Project> { it.lastAccessedAt }
                    ProjectSortBy.LAST_SAVED -> compareBy<Project> { it.lastSavedAt }
                    ProjectSortBy.SIZE -> compareBy<Project> { it.totalSize }
                }.let { if (sortOrder == ProjectSortOrder.DESCENDING) it.reversed() else it }
            )

    val favoriteProjects: List<Project>
        get() = projects.filter { it.isFavorite }
}

enum class ProjectSortBy {
    NAME,
    CREATED,
    LAST_ACCESSED,
    LAST_SAVED,
    SIZE
}

enum class ProjectSortOrder {
    ASCENDING,
    DESCENDING
}

enum class SaveResult {
    SUCCESS,
    FAILURE,
    NO_PROJECT,
    READ_ONLY
}
