package com.pythonide.app.screens.packages

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pythonide.domain.model.packageManager.*
import com.pythonide.domain.repository.PackageManagerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class PackageManagerViewModel @Inject constructor(
    application: Application,
    private val packageManagerRepository: PackageManagerRepository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(PackageManagerState())
    val state: StateFlow<PackageManagerState> = _state.asStateFlow()

    private val _selectedPackage = MutableStateFlow<PackageInfo?>(null)
    val selectedPackage: StateFlow<PackageInfo?> = _selectedPackage.asStateFlow()

    private val _showInstallDialog = MutableStateFlow(false)
    val showInstallDialog: StateFlow<Boolean> = _showInstallDialog.asStateFlow()

    private val _showBatchInstallDialog = MutableStateFlow(false)
    val showBatchInstallDialog: StateFlow<Boolean> = _showBatchInstallDialog.asStateFlow()

    private val _showBackupDialog = MutableStateFlow(false)
    val showBackupDialog: StateFlow<Boolean> = _showBackupDialog.asStateFlow()

    private val _showOfflineInstallDialog = MutableStateFlow(false)
    val showOfflineInstallDialog: StateFlow<Boolean> = _showOfflineInstallDialog.asStateFlow()

    private val _showPackageInfoDialog = MutableStateFlow(false)
    val showPackageInfoDialog: StateFlow<Boolean> = _showPackageInfoDialog.asStateFlow()

    private val _showCompatibilityDialog = MutableStateFlow(false)
    val showCompatibilityDialog: StateFlow<Boolean> = _showCompatibilityDialog.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadInstalledPackages()
        observeInstallQueue()
        observeActiveInstalls()
        checkUpgradablePackages()
        observeOfflinePackages()
        observeBackups()
    }

    private fun loadInstalledPackages() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                packageManagerRepository.getInstalledPackages().collect { packages ->
                    _state.update { state ->
                        state.copy(
                            installedPackages = packages,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun observeInstallQueue() {
        viewModelScope.launch {
            try {
                packageManagerRepository.getInstallQueue().collect { queue ->
                    _state.update { it.copy(installQueue = queue) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun observeActiveInstalls() {
        viewModelScope.launch {
            try {
                packageManagerRepository.getActiveInstalls().collect { active ->
                    _state.update { it.copy(activeInstalls = active) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun observeOfflinePackages() {
        viewModelScope.launch {
            try {
                packageManagerRepository.getOfflinePackages().collect { offline ->
                    _state.update { it.copy(offlinePackages = offline) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun observeBackups() {
        viewModelScope.launch {
            try {
                packageManagerRepository.getBackups().collect { backups ->
                    _state.update { it.copy(backupPackages = backups) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun checkUpgradablePackages() {
        viewModelScope.launch {
            try {
                val installed = _state.value.installedPackages
                val upgradable = installed.filter { it.isUpgradable }
                _state.update { state ->
                    state.copy(
                        installedPackages = state.installedPackages.map { pkg ->
                            if (upgradable.any { it.name == pkg.name }) pkg.copy(isUpgradable = true) else pkg
                        }
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        if (query.isNotEmpty()) {
            searchPackages()
        } else {
            _state.update { it.copy(searchResults = null) }
        }
    }

    fun searchPackages() {
        searchJob?.cancel()
        val query = _state.value.searchQuery
        if (query.isBlank()) {
            _state.update { it.copy(searchResults = null, isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            try {
                val result = packageManagerRepository.searchPackages(query)
                result.fold(
                    onSuccess = { searchResult ->
                        _state.update { state ->
                            state.copy(
                                searchResults = searchResult,
                                availablePackages = searchResult.packages,
                                isSearching = false
                            )
                        }
                    },
                    onFailure = { e ->
                        _state.update { it.copy(error = e.message, isSearching = false) }
                    }
                )
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isSearching = false) }
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _state.update { it.copy(searchQuery = "", searchResults = null, isSearching = false) }
    }

    fun installPackage(packageName: String, version: String? = null) {
        viewModelScope.launch {
            val task = InstallTask(
                packageName = packageName,
                version = version,
                status = InstallTaskStatus.QUEUED
            )
            _state.update { state ->
                state.copy(installQueue = state.installQueue + task)
            }
            try {
                val progressFlow = packageManagerRepository.installPackage(packageName, version)
                trackInstallProgress(task.id, packageName, progressFlow)
            } catch (e: Exception) {
                handleInstallFailure(task.id, packageName, e.message ?: "Unknown error")
            }
        }
    }

    fun uninstallPackage(packageName: String) {
        viewModelScope.launch {
            val task = InstallTask(
                packageName = packageName,
                status = InstallTaskStatus.QUEUED
            )
            _state.update { state ->
                state.copy(installQueue = state.installQueue + task)
            }
            try {
                val progressFlow = packageManagerRepository.uninstallPackage(packageName)
                trackInstallProgress(task.id, packageName, progressFlow)
            } catch (e: Exception) {
                handleInstallFailure(task.id, packageName, e.message ?: "Unknown error")
            }
        }
    }

    fun upgradePackage(packageName: String) {
        viewModelScope.launch {
            val task = InstallTask(
                packageName = packageName,
                status = InstallTaskStatus.QUEUED
            )
            _state.update { state ->
                state.copy(installQueue = state.installQueue + task)
            }
            try {
                val progressFlow = packageManagerRepository.upgradePackage(packageName)
                trackInstallProgress(task.id, packageName, progressFlow)
            } catch (e: Exception) {
                handleInstallFailure(task.id, packageName, e.message ?: "Unknown error")
            }
        }
    }

    fun downgradePackage(packageName: String, version: String) {
        viewModelScope.launch {
            val task = InstallTask(
                packageName = packageName,
                version = version,
                status = InstallTaskStatus.QUEUED
            )
            _state.update { state ->
                state.copy(installQueue = state.installQueue + task)
            }
            try {
                val progressFlow = packageManagerRepository.downgradePackage(packageName, version)
                trackInstallProgress(task.id, packageName, progressFlow)
            } catch (e: Exception) {
                handleInstallFailure(task.id, packageName, e.message ?: "Unknown error")
            }
        }
    }

    fun reinstallPackage(packageName: String) {
        viewModelScope.launch {
            val task = InstallTask(
                packageName = packageName,
                status = InstallTaskStatus.QUEUED
            )
            _state.update { state ->
                state.copy(installQueue = state.installQueue + task)
            }
            try {
                val progressFlow = packageManagerRepository.reinstallPackage(packageName)
                trackInstallProgress(task.id, packageName, progressFlow)
            } catch (e: Exception) {
                handleInstallFailure(task.id, packageName, e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun trackInstallProgress(
        taskId: String,
        packageName: String,
        progressFlow: Flow<InstallProgress>
    ) {
        _state.update { state ->
            state.copy(activeInstalls = state.activeInstalls + (taskId to InstallProgress(
                taskId = taskId,
                packageName = packageName,
                status = InstallTaskStatus.DOWNLOADING,
                progress = 0f
            )))
        }
        _state.update { state ->
            state.copy(installQueue = state.installQueue.filter { it.id != taskId })
        }
        try {
            progressFlow.collect { progress ->
                _state.update { state ->
                    val updatedActive = if (progress.status == InstallTaskStatus.COMPLETED ||
                        progress.status == InstallTaskStatus.FAILED ||
                        progress.status == InstallTaskStatus.CANCELLED
                    ) {
                        state.activeInstalls - taskId
                    } else {
                        state.activeInstalls + (taskId to progress)
                    }
                    state.copy(activeInstalls = updatedActive)
                }
                val task = InstallTask(
                    id = taskId,
                    packageName = packageName,
                    status = progress.status,
                    progress = progress.progress,
                    message = progress.message,
                    startedAt = System.currentTimeMillis(),
                    completedAt = if (progress.status == InstallTaskStatus.COMPLETED ||
                        progress.status == InstallTaskStatus.FAILED
                    ) System.currentTimeMillis() else null
                )
                when (progress.status) {
                    InstallTaskStatus.COMPLETED -> {
                        _state.update { state ->
                            state.copy(completedInstalls = state.completedInstalls + task)
                        }
                        loadInstalledPackages()
                    }
                    InstallTaskStatus.FAILED -> {
                        _state.update { state ->
                            state.copy(failedInstalls = state.failedInstalls + task)
                        }
                    }
                    else -> { /* still in progress */ }
                }
                updateInstallLogs(taskId, progress.message)
            }
        } catch (e: Exception) {
            handleInstallFailure(taskId, packageName, e.message ?: "Unknown error")
        }
    }

    private fun handleInstallFailure(taskId: String, packageName: String, errorMessage: String) {
        val task = InstallTask(
            id = taskId,
            packageName = packageName,
            status = InstallTaskStatus.FAILED,
            error = errorMessage,
            startedAt = System.currentTimeMillis(),
            completedAt = System.currentTimeMillis()
        )
        _state.update { state ->
            state.copy(
                activeInstalls = state.activeInstalls - taskId,
                failedInstalls = state.failedInstalls + task,
                installQueue = state.installQueue.filter { it.id != taskId }
            )
        }
        updateInstallLogs(taskId, "ERROR: $errorMessage")
    }

    private fun updateInstallLogs(taskId: String, message: String) {
        if (message.isBlank()) return
        _state.update { state ->
            val currentLogs = state.installLogs[taskId] ?: emptyList()
            state.copy(
                installLogs = state.installLogs + (taskId to (currentLogs + message))
            )
        }
    }

    fun batchInstall(packages: List<Pair<String, String?>>) {
        viewModelScope.launch {
            val request = BatchInstallRequest(
                packages = packages,
                priority = InstallPriority.NORMAL,
                isBackground = false
            )
            packages.forEach { (packageName, version) ->
                val task = InstallTask(
                    packageName = packageName,
                    version = version,
                    status = InstallTaskStatus.QUEUED
                )
                _state.update { state ->
                    state.copy(installQueue = state.installQueue + task)
                }
            }
            try {
                val progressFlow = packageManagerRepository.batchInstall(request)
                val taskIds = packages.mapNotNull { (packageName, _) ->
                    val task = _state.value.installQueue.find { q -> q.packageName == packageName }
                    task?.let { packageName to it.id }
                }.toMap()
                progressFlow.collect { progress ->
                    val taskId = taskIds[progress.packageName] ?: return@collect
                    trackInstallProgress(taskId, progress.packageName, flowOf(progress))
                }
            } catch (e: Exception) {
                packages.forEach { (packageName, _) ->
                    _state.update { state ->
                        state.copy(error = e.message)
                    }
                }
            }
        }
    }

    fun addToQueue(packageName: String, version: String? = null) {
        val task = InstallTask(
            packageName = packageName,
            version = version,
            status = InstallTaskStatus.QUEUED
        )
        _state.update { state ->
            state.copy(installQueue = state.installQueue + task)
        }
    }

    fun removeFromQueue(taskId: String) {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(installQueue = state.installQueue.filter { it.id != taskId })
            }
            packageManagerRepository.cancelInstall(taskId)
        }
    }

    fun clearQueue() {
        viewModelScope.launch {
            val queuedTasks = _state.value.installQueue.filter { it.status == InstallTaskStatus.QUEUED }
            queuedTasks.forEach { task ->
                packageManagerRepository.cancelInstall(task.id)
            }
            _state.update { state ->
                state.copy(installQueue = state.installQueue.filter { it.status != InstallTaskStatus.QUEUED })
            }
        }
    }

    fun cancelInstall(taskId: String) {
        viewModelScope.launch {
            try {
                packageManagerRepository.cancelInstall(taskId)
                _state.update { state ->
                    state.copy(
                        activeInstalls = state.activeInstalls - taskId,
                        installQueue = state.installQueue.filter { it.id != taskId }
                    )
                }
                val task = _state.value.completedInstalls.find { it.id == taskId }
                    ?: _state.value.failedInstalls.find { it.id == taskId }
                    ?: InstallTask(
                        id = taskId,
                        packageName = "",
                        status = InstallTaskStatus.CANCELLED,
                        completedAt = System.currentTimeMillis()
                    )
                _state.update { state ->
                    state.copy(completedInstalls = state.completedInstalls + task.copy(status = InstallTaskStatus.CANCELLED))
                }
                updateInstallLogs(taskId, "Install cancelled by user")
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun pauseInstall(taskId: String) {
        viewModelScope.launch {
            try {
                packageManagerRepository.pauseInstall(taskId)
                _state.update { state ->
                    val progress = state.activeInstalls[taskId]
                    if (progress != null) {
                        state.copy(
                            activeInstalls = state.activeInstalls + (taskId to progress.copy(
                                status = InstallTaskStatus.PAUSED
                            ))
                        )
                    } else state
                }
                updateInstallLogs(taskId, "Install paused")
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun resumeInstall(taskId: String) {
        viewModelScope.launch {
            try {
                packageManagerRepository.resumeInstall(taskId)
                _state.update { state ->
                    val progress = state.activeInstalls[taskId]
                    if (progress != null) {
                        state.copy(
                            activeInstalls = state.activeInstalls + (taskId to progress.copy(
                                status = InstallTaskStatus.INSTALLING
                            ))
                        )
                    } else state
                }
                updateInstallLogs(taskId, "Install resumed")
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun retryInstall(taskId: String) {
        viewModelScope.launch {
            val failedTask = _state.value.failedInstalls.find { it.id == taskId }
            if (failedTask != null) {
                _state.update { state ->
                    state.copy(failedInstalls = state.failedInstalls.filter { it.id != taskId })
                }
                try {
                    val progressFlow = packageManagerRepository.retryInstall(taskId)
                    trackInstallProgress(taskId, failedTask.packageName, progressFlow)
                } catch (e: Exception) {
                    handleInstallFailure(taskId, failedTask.packageName, e.message ?: "Unknown error")
                }
            }
        }
    }

    fun setSearchViewMode() {
        _state.update { it.copy(viewMode = PackageViewMode.SEARCH) }
    }

    fun setInstalledViewMode() {
        _state.update { it.copy(viewMode = PackageViewMode.INSTALLED) }
    }

    fun setQueueViewMode() {
        _state.update { it.copy(viewMode = PackageViewMode.QUEUE) }
    }

    fun setLogsViewMode() {
        _state.update { it.copy(viewMode = PackageViewMode.LOGS) }
    }

    fun setSortBy(sortBy: PackageSortBy) {
        _state.update { it.copy(sortBy = sortBy) }
    }

    fun toggleSortOrder() {
        _state.update { state ->
            state.copy(
                sortOrder = if (state.sortOrder == PackageSortOrder.ASCENDING)
                    PackageSortOrder.DESCENDING else PackageSortOrder.ASCENDING
            )
        }
    }

    fun toggleFilterInstalled() {
        _state.update { it.copy(filterInstalled = !it.filterInstalled) }
    }

    fun toggleFilterUpgradable() {
        _state.update { it.copy(filterUpgradable = !it.filterUpgradable) }
    }

    fun selectPackage(packageName: String) {
        viewModelScope.launch {
            try {
                val result = packageManagerRepository.getPackageInfo(packageName)
                result.fold(
                    onSuccess = { info ->
                        _selectedPackage.value = info
                        _showPackageInfoDialog.value = true
                        _state.update { it.copy(selectedPackage = info) }
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

    fun clearSelectedPackage() {
        _selectedPackage.value = null
        _state.update { it.copy(selectedPackage = null) }
        _showPackageInfoDialog.value = false
    }

    fun showPackageDetails(packageName: String) {
        selectPackage(packageName)
    }

    fun addOfflinePackage(filePath: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                val result = packageManagerRepository.addOfflinePackage(filePath)
                result.fold(
                    onSuccess = { offlinePackage ->
                        _state.update { state ->
                            state.copy(
                                offlinePackages = state.offlinePackages + offlinePackage,
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

    fun removeOfflinePackage(packageId: String) {
        viewModelScope.launch {
            try {
                val result = packageManagerRepository.removeOfflinePackage(packageId)
                result.fold(
                    onSuccess = {
                        _state.update { state ->
                            state.copy(
                                offlinePackages = state.offlinePackages.filter { it.id != packageId }
                            )
                        }
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

    fun createBackup(name: String, description: String = "") {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                val result = packageManagerRepository.createBackup(name, description)
                result.fold(
                    onSuccess = { backup ->
                        _state.update { state ->
                            state.copy(
                                backupPackages = state.backupPackages + backup,
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

    fun restoreBackup(backupId: String) {
        viewModelScope.launch {
            try {
                val backup = _state.value.backupPackages.find { it.id == backupId }
                if (backup != null) {
                    _showBackupDialog.value = false
                    val progressFlow = packageManagerRepository.restoreBackup(backupId)
                    progressFlow.collect { progress ->
                        if (progress.status == InstallTaskStatus.COMPLETED) {
                            loadInstalledPackages()
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteBackup(backupId: String) {
        viewModelScope.launch {
            try {
                val result = packageManagerRepository.deleteBackup(backupId)
                result.fold(
                    onSuccess = {
                        _state.update { state ->
                            state.copy(
                                backupPackages = state.backupPackages.filter { it.id != backupId }
                            )
                        }
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

    fun exportBackup(backupId: String, exportPath: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                val result = packageManagerRepository.exportBackup(backupId, exportPath)
                result.fold(
                    onSuccess = { path ->
                        _state.update { it.copy(isLoading = false) }
                        updateInstallLogs(backupId, "Backup exported to: $path")
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

    fun importBackup(backupPath: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                val result = packageManagerRepository.importBackup(backupPath)
                result.fold(
                    onSuccess = { backup ->
                        _state.update { state ->
                            state.copy(
                                backupPackages = state.backupPackages + backup,
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

    fun pipList() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                val result = packageManagerRepository.pipList()
                result.fold(
                    onSuccess = { packages ->
                        _state.update { state ->
                            state.copy(
                                installedPackages = packages,
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

    fun pipFreeze() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                val result = packageManagerRepository.pipFreeze()
                result.fold(
                    onSuccess = { requirements ->
                        _state.update { it.copy(isLoading = false) }
                        updateInstallLogs("pip-freeze", requirements)
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

    fun pipShow(packageName: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                val result = packageManagerRepository.pipShow(packageName)
                result.fold(
                    onSuccess = { details ->
                        _state.update { it.copy(isLoading = false) }
                        updateInstallLogs("pip-show-$packageName", details)
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

    fun pipCachePurge() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                val result = packageManagerRepository.pipCachePurge()
                result.fold(
                    onSuccess = { success ->
                        _state.update { it.copy(isLoading = false) }
                        updateInstallLogs("pip-cache", "Cache purged successfully")
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

    fun resolveDependencies(packageName: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                val result = packageManagerRepository.resolveDependencies(packageName)
                result.fold(
                    onSuccess = { deps ->
                        _state.update { state ->
                            state.copy(
                                dependencyTree = state.dependencyTree + (packageName to deps.keys.toList()),
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

    fun getReverseDependencies(packageName: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                val result = packageManagerRepository.getReverseDependencies(packageName)
                result.fold(
                    onSuccess = { reverseDeps ->
                        _state.update { state ->
                            state.copy(
                                dependencyTree = state.dependencyTree + (packageName to reverseDeps),
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

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun clearCompletedInstalls() {
        viewModelScope.launch {
            try {
                packageManagerRepository.clearCompletedInstalls()
                _state.update { it.copy(completedInstalls = emptyList()) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearFailedInstalls() {
        viewModelScope.launch {
            try {
                packageManagerRepository.clearFailedInstalls()
                _state.update { it.copy(failedInstalls = emptyList()) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun showInstallDialog() {
        _showInstallDialog.value = true
    }

    fun hideInstallDialog() {
        _showInstallDialog.value = false
    }

    fun showBatchInstallDialog() {
        _showBatchInstallDialog.value = true
    }

    fun hideBatchInstallDialog() {
        _showBatchInstallDialog.value = false
    }

    fun showBackupDialog() {
        _showBackupDialog.value = true
    }

    fun hideBackupDialog() {
        _showBackupDialog.value = false
    }

    fun showOfflineInstallDialog() {
        _showOfflineInstallDialog.value = true
    }

    fun hideOfflineInstallDialog() {
        _showOfflineInstallDialog.value = false
    }

    fun showCompatibilityDialog() {
        _showCompatibilityDialog.value = true
    }

    fun hideCompatibilityDialog() {
        _showCompatibilityDialog.value = false
    }

    fun refreshInstalledPackages() {
        loadInstalledPackages()
    }

    fun checkPackageCompatibility(packageName: String) {
        viewModelScope.launch {
            try {
                val result = packageManagerRepository.getPackageCompatibility(packageName)
                result.fold(
                    onSuccess = { compatibility ->
                        _state.update { state ->
                            state.copy(
                                compatiblePackages = if (compatibility.isCompatible) state.compatiblePackages + 1 else state.compatiblePackages,
                                incompatiblePackages = if (!compatibility.isCompatible) state.incompatiblePackages + 1 else state.incompatiblePackages
                            )
                        }
                        if (!compatibility.isCompatible) {
                            _state.update { it.copy(error = "Package $packageName is incompatible: ${compatibility.reason}") }
                        }
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
}
