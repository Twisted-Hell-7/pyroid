package com.pythonide.app.screens.project

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pythonide.domain.model.project.AutoSaveConfig
import com.pythonide.domain.model.project.Project
import com.pythonide.domain.model.project.ProjectBackup
import com.pythonide.domain.model.project.ProjectMetadata
import com.pythonide.domain.model.project.ProjectSession
import com.pythonide.domain.model.project.ProjectSortBy
import com.pythonide.domain.model.project.ProjectSortOrder
import com.pythonide.domain.model.project.ProjectState
import com.pythonide.domain.repository.ProjectStats
import com.pythonide.domain.model.project.ProjectTemplate
import com.pythonide.domain.model.project.SaveResult
import com.pythonide.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProjectViewModel @Inject constructor(
    application: Application,
    private val projectRepository: ProjectRepository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ProjectState())
    val state: StateFlow<ProjectState> = _state.asStateFlow()

    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject: StateFlow<Project?> = _selectedProject.asStateFlow()

    private val _showNewProjectDialog = MutableStateFlow(false)
    val showNewProjectDialog: StateFlow<Boolean> = _showNewProjectDialog.asStateFlow()

    private val _showOpenProjectDialog = MutableStateFlow(false)
    val showOpenProjectDialog: StateFlow<Boolean> = _showOpenProjectDialog.asStateFlow()

    private val _showSaveAsDialog = MutableStateFlow(false)
    val showSaveAsDialog: StateFlow<Boolean> = _showSaveAsDialog.asStateFlow()

    private val _showTemplateDialog = MutableStateFlow(false)
    val showTemplateDialog: StateFlow<Boolean> = _showTemplateDialog.asStateFlow()

    private val _showBackupDialog = MutableStateFlow(false)
    val showBackupDialog: StateFlow<Boolean> = _showBackupDialog.asStateFlow()

    private val _showMetadataDialog = MutableStateFlow(false)
    val showMetadataDialog: StateFlow<Boolean> = _showMetadataDialog.asStateFlow()

    private val _projectStats = MutableStateFlow<ProjectStats?>(null)
    val projectStats: StateFlow<ProjectStats?> = _projectStats.asStateFlow()

    private val _selectedTemplate = MutableStateFlow<ProjectTemplate?>(null)
    val selectedTemplate: StateFlow<ProjectTemplate?> = _selectedTemplate.asStateFlow()

    private val _selectedBackup = MutableStateFlow<ProjectBackup?>(null)
    val selectedBackup: StateFlow<ProjectBackup?> = _selectedBackup.asStateFlow()

    private var autoSaveJob: Job? = null

    init {
        loadProjects()
        loadTemplates()
        loadAutoSaveConfig()
        restoreLastSession()
    }

    private fun loadProjects() {
        viewModelScope.launch {
            projectRepository.getProjects().collect { projects ->
                _state.update { it.copy(projects = projects) }
            }
        }
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            projectRepository.getTemplates().collect { templates ->
                _state.update { it.copy(templates = templates) }
            }
        }
    }

    private fun loadAutoSaveConfig() {
        viewModelScope.launch {
            projectRepository.getAutoSaveConfig().collect { config ->
                _state.update { it.copy(autoSaveConfig = config) }
                if (config.enabled) {
                    startAutoSave(config.intervalMs)
                }
            }
        }
    }

    private fun restoreLastSession() {
        viewModelScope.launch {
            val session = projectRepository.restoreSession().getOrNull()
            if (session != null) {
                _state.update { it.copy(currentSession = session) }
            }
        }
    }

    fun showNewProjectDialog() {
        _showNewProjectDialog.value = true
    }

    fun hideNewProjectDialog() {
        _showNewProjectDialog.value = false
    }

    fun showOpenProjectDialog() {
        _showOpenProjectDialog.value = true
    }

    fun hideOpenProjectDialog() {
        _showOpenProjectDialog.value = false
    }

    fun showSaveAsDialog(project: Project) {
        _selectedProject.value = project
        _showSaveAsDialog.value = true
    }

    fun hideSaveAsDialog() {
        _showSaveAsDialog.value = false
        _selectedProject.value = null
    }

    fun showTemplateDialog() {
        _showTemplateDialog.value = true
    }

    fun hideTemplateDialog() {
        _showTemplateDialog.value = false
        _selectedTemplate.value = null
    }

    fun showBackupDialog(project: Project) {
        _selectedProject.value = project
        _showBackupDialog.value = true
    }

    fun hideBackupDialog() {
        _showBackupDialog.value = false
        _selectedProject.value = null
    }

    fun showMetadataDialog(project: Project) {
        _selectedProject.value = project
        _showMetadataDialog.value = true
    }

    fun hideMetadataDialog() {
        _showMetadataDialog.value = false
        _selectedProject.value = null
    }

    fun createProject(
        name: String,
        description: String = "",
        templateId: String? = null,
        metadata: ProjectMetadata = ProjectMetadata()
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = projectRepository.createProject(
                name = name,
                description = description,
                templateId = templateId,
                metadata = metadata
            )
            result.fold(
                onSuccess = { project ->
                    _state.update { it.copy(isLoading = false) }
                    hideNewProjectDialog()
                    openProject(project)
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message, isLoading = false) }
                }
            )
        }
    }

    fun createProjectFromTemplate(templateId: String, projectName: String, destinationPath: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = projectRepository.createProjectFromTemplate(templateId, projectName, destinationPath)
            result.fold(
                onSuccess = { project ->
                    _state.update { it.copy(isLoading = false) }
                    hideTemplateDialog()
                    openProject(project)
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message, isLoading = false) }
                }
            )
        }
    }

    fun openProject(project: Project) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = projectRepository.openProject(project.id)
            result.fold(
                onSuccess = { openedProject ->
                    _state.update {
                        it.copy(
                            currentProject = openedProject,
                            isLoading = false
                        )
                    }
                    _selectedProject.value = openedProject
                    loadProjectSession(openedProject.id)
                    loadProjectStats(openedProject.id)
                    hideOpenProjectDialog()
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message, isLoading = false) }
                }
            )
        }
    }

    private fun loadProjectSession(projectId: String) {
        viewModelScope.launch {
            projectRepository.getSession(projectId).collect { session ->
                _state.update { it.copy(currentSession = session) }
            }
        }
    }

    private fun loadProjectStats(projectId: String) {
        viewModelScope.launch {
            val stats = projectRepository.getProjectStats(projectId).getOrNull()
            _projectStats.value = stats
        }
    }

    fun closeProject(projectId: String) {
        viewModelScope.launch {
            projectRepository.closeProject(projectId)
            if (_state.value.currentProject?.id == projectId) {
                _state.update { it.copy(currentProject = null, currentSession = null) }
                _selectedProject.value = null
                _projectStats.value = null
            }
        }
    }

    fun deleteProject(project: Project, deleteFiles: Boolean = true) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = projectRepository.deleteProject(project.id, deleteFiles)
            result.fold(
                onSuccess = {
                    if (_state.value.currentProject?.id == project.id) {
                        _state.update { it.copy(currentProject = null, currentSession = null) }
                        _selectedProject.value = null
                        _projectStats.value = null
                    }
                    _state.update { it.copy(isLoading = false) }
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message, isLoading = false) }
                }
            )
        }
    }

    fun renameProject(projectId: String, newName: String) {
        viewModelScope.launch {
            val result = projectRepository.renameProject(projectId, newName)
            result.fold(
                onSuccess = { updatedProject ->
                    if (_state.value.currentProject?.id == projectId) {
                        _state.update { it.copy(currentProject = updatedProject) }
                        _selectedProject.value = updatedProject
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message) }
                }
            )
        }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch {
            val result = projectRepository.updateProject(project)
            result.fold(
                onSuccess = { updatedProject ->
                    if (_state.value.currentProject?.id == project.id) {
                        _state.update { it.copy(currentProject = updatedProject) }
                        _selectedProject.value = updatedProject
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message) }
                }
            )
        }
    }

    fun updateProjectMetadata(projectId: String, metadata: ProjectMetadata) {
        viewModelScope.launch {
            val result = projectRepository.updateProjectMetadata(projectId, metadata)
            result.fold(
                onSuccess = { updatedProject ->
                    if (_state.value.currentProject?.id == projectId) {
                        _state.update { it.copy(currentProject = updatedProject) }
                        _selectedProject.value = updatedProject
                    }
                    hideMetadataDialog()
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message) }
                }
            )
        }
    }

    fun toggleFavorite(project: Project) {
        viewModelScope.launch {
            val result = projectRepository.toggleFavorite(project.id)
            result.fold(
                onSuccess = { updatedProject ->
                    if (_state.value.currentProject?.id == project.id) {
                        _state.update { it.copy(currentProject = updatedProject) }
                        _selectedProject.value = updatedProject
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message) }
                }
            )
        }
    }

    fun addTag(projectId: String, tag: String) {
        viewModelScope.launch {
            val result = projectRepository.addTag(projectId, tag)
            result.fold(
                onSuccess = { updatedProject ->
                    if (_state.value.currentProject?.id == projectId) {
                        _state.update { it.copy(currentProject = updatedProject) }
                        _selectedProject.value = updatedProject
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message) }
                }
            )
        }
    }

    fun removeTag(projectId: String, tag: String) {
        viewModelScope.launch {
            val result = projectRepository.removeTag(projectId, tag)
            result.fold(
                onSuccess = { updatedProject ->
                    if (_state.value.currentProject?.id == projectId) {
                        _state.update { it.copy(currentProject = updatedProject) }
                        _selectedProject.value = updatedProject
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message) }
                }
            )
        }
    }

    fun saveProject(projectId: String) {
        viewModelScope.launch {
            val result = projectRepository.saveProject(projectId)
            result.fold(
                onSuccess = { saveResult ->
                    when (saveResult) {
                        SaveResult.SUCCESS -> {
                            _state.update { it.copy(error = null) }
                        }
                        SaveResult.FAILURE -> {
                            _state.update { it.copy(error = "Failed to save project") }
                        }
                        SaveResult.NO_PROJECT -> {
                            _state.update { it.copy(error = "No project open") }
                        }
                        SaveResult.READ_ONLY -> {
                            _state.update { it.copy(error = "Project is read-only") }
                        }
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message) }
                }
            )
        }
    }

    fun saveProjectAs(projectId: String, newPath: String, newName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = projectRepository.saveProjectAs(projectId, newPath, newName)
            result.fold(
                onSuccess = { newProject ->
                    _state.update { it.copy(isLoading = false) }
                    hideSaveAsDialog()
                    openProject(newProject)
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message, isLoading = false) }
                }
            )
        }
    }

    fun saveAllProjects() {
        viewModelScope.launch {
            val result = projectRepository.saveAllProjects()
            result.fold(
                onSuccess = { count ->
                    _state.update { it.copy(error = null) }
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message) }
                }
            )
        }
    }

    fun selectTemplate(template: ProjectTemplate) {
        _selectedTemplate.value = template
    }

    fun createBackup(description: String = "") {
        val project = _selectedProject.value ?: return
        viewModelScope.launch {
            val result = projectRepository.createBackup(project.id, description)
            result.fold(
                onSuccess = { backup ->
                    hideBackupDialog()
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message) }
                }
            )
        }
    }

    fun restoreBackup(backup: ProjectBackup) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = projectRepository.restoreBackup(backup.id)
            result.fold(
                onSuccess = { restoredProject ->
                    _state.update { it.copy(isLoading = false) }
                    openProject(restoredProject)
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message, isLoading = false) }
                }
            )
        }
    }

    fun deleteBackup(backup: ProjectBackup) {
        viewModelScope.launch {
            val result = projectRepository.deleteBackup(backup.id)
            result.fold(
                onSuccess = {},
                onFailure = { e ->
                    _state.update { it.copy(error = e.message) }
                }
            )
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun setSortBy(sortBy: ProjectSortBy) {
        _state.update { it.copy(sortBy = sortBy) }
    }

    fun toggleSortOrder() {
        _state.update { state ->
            state.copy(
                sortOrder = if (state.sortOrder == ProjectSortOrder.ASCENDING) {
                    ProjectSortOrder.DESCENDING
                } else {
                    ProjectSortOrder.ASCENDING
                }
            )
        }
    }

    fun toggleShowFavoritesOnly() {
        _state.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
    }

    fun updateAutoSaveConfig(config: AutoSaveConfig) {
        viewModelScope.launch {
            val result = projectRepository.updateAutoSaveConfig(config)
            result.fold(
                onSuccess = {
                    if (config.enabled) {
                        startAutoSave(config.intervalMs)
                    } else {
                        stopAutoSave()
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message) }
                }
            )
        }
    }

    private fun startAutoSave(intervalMs: Long) {
        stopAutoSave()
        autoSaveJob = viewModelScope.launch {
            while (true) {
                delay(intervalMs)
                val currentProject = _state.value.currentProject
                if (currentProject != null) {
                    projectRepository.performAutoSave(currentProject.id)
                }
            }
        }
    }

    private fun stopAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = null
    }

    fun addOpenFile(path: String, name: String) {
        val project = _state.value.currentProject ?: return
        viewModelScope.launch {
            projectRepository.addOpenFile(project.id, path, name)
        }
    }

    fun removeOpenFile(fileId: String) {
        val project = _state.value.currentProject ?: return
        viewModelScope.launch {
            projectRepository.removeOpenFile(project.id, fileId)
        }
    }

    fun updateSessionFile(fileId: String, isModified: Boolean) {
        val project = _state.value.currentProject ?: return
        viewModelScope.launch {
            projectRepository.updateSessionFile(project.id, fileId, isModified)
        }
    }

    fun clearSession() {
        val project = _state.value.currentProject ?: return
        viewModelScope.launch {
            projectRepository.clearSession(project.id)
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoSave()
        val currentProject = _state.value.currentProject
        if (currentProject != null) {
            viewModelScope.launch {
                projectRepository.saveProject(currentProject.id)
                projectRepository.closeProject(currentProject.id)
            }
        }
    }
}
