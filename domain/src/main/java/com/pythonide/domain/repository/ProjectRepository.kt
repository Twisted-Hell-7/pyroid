package com.pythonide.domain.repository

import com.pythonide.domain.model.project.AutoSaveConfig
import com.pythonide.domain.model.project.Project
import com.pythonide.domain.model.project.ProjectBackup
import com.pythonide.domain.model.project.ProjectMetadata
import com.pythonide.domain.model.project.ProjectSession
import com.pythonide.domain.model.project.ProjectStats
import com.pythonide.domain.model.project.ProjectTemplate
import com.pythonide.domain.model.project.SaveResult
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getProjects(): Flow<List<Project>>
    fun getProjectById(id: String): Flow<Project?>
    fun getRecentProjects(): Flow<List<Project>>
    fun getFavoriteProjects(): Flow<List<Project>>

    suspend fun createProject(
        name: String,
        description: String = "",
        templateId: String? = null,
        rootPath: String? = null,
        metadata: ProjectMetadata = ProjectMetadata()
    ): Result<Project>

    suspend fun openProject(projectId: String): Result<Project>
    suspend fun closeProject(projectId: String): Result<Boolean>
    suspend fun deleteProject(projectId: String, deleteFiles: Boolean = true): Result<Boolean>
    suspend fun renameProject(projectId: String, newName: String): Result<Project>
    suspend fun updateProject(project: Project): Result<Project>
    suspend fun updateProjectMetadata(projectId: String, metadata: ProjectMetadata): Result<Project>
    suspend fun toggleFavorite(projectId: String): Result<Project>
    suspend fun addTag(projectId: String, tag: String): Result<Project>
    suspend fun removeTag(projectId: String, tag: String): Result<Project>

    suspend fun saveProject(projectId: String): Result<SaveResult>
    suspend fun saveProjectAs(projectId: String, newPath: String, newName: String): Result<Project>
    suspend fun saveAllProjects(): Result<Int>

    suspend fun getTemplates(): Flow<List<ProjectTemplate>>
    suspend fun getTemplateById(templateId: String): ProjectTemplate?
    suspend fun createProjectFromTemplate(
        templateId: String,
        projectName: String,
        destinationPath: String
    ): Result<Project>
    suspend fun addCustomTemplate(template: ProjectTemplate): Result<ProjectTemplate>
    suspend fun deleteTemplate(templateId: String): Result<Boolean>

    suspend fun getSession(projectId: String): Flow<ProjectSession?>
    suspend fun saveSession(session: ProjectSession): Result<Boolean>
    suspend fun updateSessionFile(projectId: String, fileId: String, isModified: Boolean): Result<Boolean>
    suspend fun addOpenFile(projectId: String, path: String, name: String): Result<Boolean>
    suspend fun removeOpenFile(projectId: String, fileId: String): Result<Boolean>
    suspend fun clearSession(projectId: String): Result<Boolean>
    suspend fun restoreSession(): Result<ProjectSession?>

    suspend fun getAutoSaveConfig(): Flow<AutoSaveConfig>
    suspend fun updateAutoSaveConfig(config: AutoSaveConfig): Result<Boolean>
    suspend fun performAutoSave(projectId: String): Result<SaveResult>

    suspend fun createBackup(projectId: String, description: String = ""): Result<ProjectBackup>
    suspend fun getBackups(projectId: String): Flow<List<ProjectBackup>>
    suspend fun restoreBackup(backupId: String): Result<Project>
    suspend fun deleteBackup(backupId: String): Result<Boolean>
    suspend fun exportBackup(backupId: String, exportPath: String): Result<String>
    suspend fun importBackup(backupPath: String): Result<ProjectBackup>

    suspend fun getProjectStats(projectId: String): Result<ProjectStats>
}
