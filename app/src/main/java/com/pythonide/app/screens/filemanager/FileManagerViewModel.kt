package com.pythonide.app.screens.filemanager

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pythonide.domain.model.filemanager.FileManagerConfig
import com.pythonide.domain.model.filemanager.FileManagerItem
import com.pythonide.domain.model.filemanager.FileOperation
import com.pythonide.domain.model.filemanager.FileOperationStatus
import com.pythonide.domain.model.filemanager.FileOperationType
import com.pythonide.domain.model.filemanager.FileType
import com.pythonide.domain.model.filemanager.FileManagerState
import com.pythonide.domain.model.filemanager.Project
import com.pythonide.domain.model.filemanager.RecentFile
import com.pythonide.domain.model.filemanager.SortBy
import com.pythonide.domain.model.filemanager.SortOrder
import com.pythonide.domain.model.filemanager.StorageSource
import com.pythonide.domain.model.filemanager.ViewMode
import com.pythonide.domain.repository.FileManagerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FileManagerViewModel @Inject constructor(
    application: Application,
    private val repository: FileManagerRepository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(FileManagerState())
    val state: StateFlow<FileManagerState> = _state.asStateFlow()

    private val _config = MutableStateFlow(FileManagerConfig())
    val config: StateFlow<FileManagerConfig> = _config.asStateFlow()

    private val _selectedItem = MutableStateFlow<FileManagerItem?>(null)
    val selectedItem: StateFlow<FileManagerItem?> = _selectedItem.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _showRenameDialog = MutableStateFlow(false)
    val showRenameDialog: StateFlow<Boolean> = _showRenameDialog.asStateFlow()

    private val _showPropertiesDialog = MutableStateFlow(false)
    val showPropertiesDialog: StateFlow<Boolean> = _showPropertiesDialog.asStateFlow()

    private val _showProjectDialog = MutableStateFlow(false)
    val showProjectDialog: StateFlow<Boolean> = _showProjectDialog.asStateFlow()

    private val _itemProperties = MutableStateFlow<Map<String, Any>>(emptyMap())
    val itemProperties: StateFlow<Map<String, Any>> = _itemProperties.asStateFlow()

    init {
        loadInitialData()
        observeFavorites()
        observeRecentFiles()
        observeProjects()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val roots = repository.getStorageRoots().getOrNull() ?: emptyList()
                _state.update { it.copy(isLoading = false) }
                navigateTo("/storage/emulated/0")
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            repository.getFavorites().collect { favorites ->
                _state.update { it.copy(favorites = favorites) }
            }
        }
    }

    private fun observeRecentFiles() {
        viewModelScope.launch {
            repository.getRecentFiles().collect { recent ->
                _state.update { it.copy(recentFiles = recent) }
            }
        }
    }

    private fun observeProjects() {
        viewModelScope.launch {
            repository.getProjects().collect { projects ->
                _state.update { it.copy(projects = projects) }
            }
        }
    }

    fun navigateTo(path: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val result = repository.listDirectory(path, _state.value.storageSource)
                result.fold(
                    onSuccess = { items ->
                        _state.update {
                            it.copy(
                                currentPath = path,
                                items = items,
                                selectedItems = emptySet(),
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { e ->
                        _state.update { it.copy(error = e.message, isLoading = false) }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun navigateUp() {
        val currentPath = _state.value.currentPath
        val parentPath = File(currentPath).parent ?: return
        navigateTo(parentPath)
    }

    fun navigateToItem(item: FileManagerItem) {
        if (item.type == FileType.DIRECTORY) {
            navigateTo(item.path)
        } else {
            openFile(item)
        }
    }

    fun openFile(item: FileManagerItem) {
        viewModelScope.launch {
            repository.addRecentFile(item)
        }
    }

    fun selectItem(itemId: String, multiSelect: Boolean = false) {
        _state.update { state ->
            val newSelected = if (multiSelect) {
                if (state.selectedItems.contains(itemId)) {
                    state.selectedItems - itemId
                } else {
                    state.selectedItems + itemId
                }
            } else {
                setOf(itemId)
            }
            state.copy(selectedItems = newSelected)
        }
    }

    fun selectAll() {
        _state.update { state ->
            state.copy(selectedItems = state.filteredItems.map { it.id }.toSet())
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedItems = emptySet()) }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun setSortBy(sortBy: SortBy) {
        _state.update { it.copy(sortBy = sortBy) }
    }

    fun toggleSortOrder() {
        _state.update { state ->
            state.copy(
                sortOrder = if (state.sortOrder == SortOrder.ASCENDING) {
                    SortOrder.DESCENDING
                } else {
                    SortOrder.ASCENDING
                }
            )
        }
    }

    fun setViewMode(viewMode: ViewMode) {
        _state.update { it.copy(viewMode = viewMode) }
        _config.update { it.copy(defaultViewMode = viewMode) }
    }

    fun toggleHiddenFiles() {
        _state.update { it.copy(showHidden = !it.showHidden) }
        _config.update { it.copy(showHiddenFiles = !it.showHiddenFiles) }
    }

    fun setStorageSource(source: StorageSource) {
        _state.update { it.copy(storageSource = source) }
        navigateTo(_state.value.currentPath)
    }

    fun showCreateDialog() {
        _showCreateDialog.value = true
    }

    fun hideCreateDialog() {
        _showCreateDialog.value = false
    }

    fun createFile(name: String, isDirectory: Boolean = false) {
        viewModelScope.launch {
            val path = File(_state.value.currentPath, name).absolutePath
            try {
                val result = if (isDirectory) {
                    repository.createDirectory(path)
                } else {
                    repository.createFile(path)
                }
                result.fold(
                    onSuccess = {
                        navigateTo(_state.value.currentPath)
                        hideCreateDialog()
                    },
                    onFailure = { e ->
                        _state.update { it.copy(error = e.message) }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun showRenameDialog(item: FileManagerItem) {
        _selectedItem.value = item
        _showRenameDialog.value = true
    }

    fun hideRenameDialog() {
        _showRenameDialog.value = false
        _selectedItem.value = null
    }

    fun renameItem(newName: String) {
        val item = _selectedItem.value ?: return
        viewModelScope.launch {
            try {
                val result = repository.rename(item.path, newName)
                result.fold(
                    onSuccess = {
                        navigateTo(_state.value.currentPath)
                        hideRenameDialog()
                    },
                    onFailure = { e ->
                        _state.update { it.copy(error = e.message) }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteSelected() {
        val selectedIds = _state.value.selectedItems
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            val items = _state.value.items.filter { it.id in selectedIds }
            var successCount = 0
            var failCount = 0

            items.forEach { item ->
                try {
                    val result = repository.delete(item.path, recursive = true)
                    if (result.isSuccess) successCount++ else failCount++
                } catch (e: Exception) {
                    failCount++
                }
            }

            clearSelection()
            navigateTo(_state.value.currentPath)

            if (failCount > 0) {
                _state.update { it.copy(error = "Failed to delete $failCount items") }
            }
        }
    }

    fun copySelected() {
        val selectedIds = _state.value.selectedItems
        if (selectedIds.isEmpty()) return

        val items = _state.value.items.filter { it.id in selectedIds }
        _state.update {
            it.copy(
                clipboardItems = items.map { item -> item.path },
                clipboardAction = com.pythonide.domain.model.filemanager.FileAction.COPY
            )
        }
    }

    fun cutSelected() {
        val selectedIds = _state.value.selectedItems
        if (selectedIds.isEmpty()) return

        val items = _state.value.items.filter { it.id in selectedIds }
        _state.update {
            it.copy(
                clipboardItems = items.map { item -> item.path },
                clipboardAction = com.pythonide.domain.model.filemanager.FileAction.MOVE
            )
        }
    }

    fun pasteItems() {
        val clipboardItems = _state.value.clipboardItems
        val action = _state.value.clipboardAction ?: return

        if (clipboardItems.isEmpty()) return

        viewModelScope.launch {
            val destPath = _state.value.currentPath
            var successCount = 0
            var failCount = 0

            clipboardItems.forEach { sourcePath ->
                try {
                    val destFile = File(destPath, File(sourcePath).name)
                    val result = when (action) {
                        com.pythonide.domain.model.filemanager.FileAction.COPY -> {
                            repository.copy(sourcePath, destFile.absolutePath)
                        }
                        com.pythonide.domain.model.filemanager.FileAction.MOVE -> {
                            repository.move(sourcePath, destFile.absolutePath)
                        }
                        else -> null
                    }
                    if (result != null && result.isSuccess) successCount++ else failCount++
                } catch (e: Exception) {
                    failCount++
                }
            }

            _state.update { it.copy(clipboardItems = emptyList(), clipboardAction = null) }
            navigateTo(_state.value.currentPath)

            if (failCount > 0) {
                _state.update { it.copy(error = "Failed to $action $failCount items") }
            }
        }
    }

    fun hasClipboardItems(): Boolean = _state.value.clipboardItems.isNotEmpty()

    fun shareSelected() {
        val selectedIds = _state.value.selectedItems
        if (selectedIds.isEmpty()) return

        val items = _state.value.items.filter { it.id in selectedIds }
        if (items.size == 1) {
            shareFile(items.first())
        }
    }

    fun shareFile(item: FileManagerItem) {
        viewModelScope.launch {
            try {
                val file = File(item.path)
                val uri = FileProvider.getUriForFile(
                    getApplication(),
                    "${getApplication<Application>().packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = repository.getMimeType(item.path) ?: "*/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooserIntent = Intent.createChooser(shareIntent, "Share ${item.name}")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                getApplication<Application>().startActivity(chooserIntent)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Failed to share: ${e.message}") }
            }
        }
    }

    fun showProperties(item: FileManagerItem) {
        _selectedItem.value = item
        viewModelScope.launch {
            try {
                val result = repository.getFileProperties(item.path)
                result.fold(
                    onSuccess = { props ->
                        _itemProperties.value = props
                        _showPropertiesDialog.value = true
                    },
                    onFailure = { e ->
                        _state.update { it.copy(error = e.message) }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun hidePropertiesDialog() {
        _showPropertiesDialog.value = false
        _selectedItem.value = null
        _itemProperties.value = emptyMap()
    }

    fun addToFavorites(item: FileManagerItem) {
        viewModelScope.launch {
            repository.addFavorite(item)
        }
    }

    fun removeFromFavorites(path: String) {
        viewModelScope.launch {
            repository.removeFavorite(path)
        }
    }

    fun showProjectDialog() {
        _showProjectDialog.value = true
    }

    fun hideProjectDialog() {
        _showProjectDialog.value = false
    }

    fun createProject(name: String, description: String) {
        viewModelScope.launch {
            val rootPath = File(context.filesDir, "projects/$name").absolutePath
            try {
                val result = repository.createProject(name, rootPath, description)
                result.fold(
                    onSuccess = {
                        hideProjectDialog()
                        navigateTo(rootPath)
                    },
                    onFailure = { e ->
                        _state.update { it.copy(error = e.message) }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun openProject(project: Project) {
        viewModelScope.launch {
            try {
                repository.openProject(project.id)
                navigateTo(project.rootPath)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            try {
                repository.deleteProject(project.id)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun switchToRecent() {
        _state.update { it.copy(storageSource = StorageSource.RECENT) }
    }

    fun switchToFavorites() {
        _state.update { it.copy(storageSource = StorageSource.FAVORITES) }
    }

    fun switchToProjects() {
        _state.update { it.copy(storageSource = StorageSource.PROJECT) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private val context: android.content.Context
        get() = getApplication<Application>()
}
