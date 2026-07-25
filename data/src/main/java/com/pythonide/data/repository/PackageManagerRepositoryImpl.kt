package com.pythonide.data.repository

import android.content.Context
import android.os.Build
import com.pythonide.data.runtime.core.PythonNativeBridge
import com.pythonide.domain.model.packageManager.BatchInstallRequest
import com.pythonide.domain.model.packageManager.InstallProgress
import com.pythonide.domain.model.packageManager.InstallTask
import com.pythonide.domain.model.packageManager.InstallTaskStatus
import com.pythonide.domain.model.packageManager.InstalledPackage
import com.pythonide.domain.model.packageManager.NativeBinaryInfo
import com.pythonide.domain.model.packageManager.OfflinePackage
import com.pythonide.domain.model.packageManager.Package
import com.pythonide.domain.model.packageManager.PackageBackup
import com.pythonide.domain.model.packageManager.PackageCompatibility
import com.pythonide.domain.model.packageManager.PackageInfo
import com.pythonide.domain.model.packageManager.PackageSearchResult
import com.pythonide.domain.model.packageManager.PackageVersion
import com.pythonide.domain.model.packageManager.ProgressState
import com.pythonide.domain.repository.PackageManagerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.PriorityQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageManagerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pythonNativeBridge: PythonNativeBridge
) : PackageManagerRepository {

    companion object {
        const val PYPI_BASE_URL = "https://pypi.org"
        const val PYPI_SEARCH_URL = "https://pypi.org/search/"
        val ANDROID_ABIS = listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")

        private const val REQUEST_TIMEOUT_SECONDS = 30L
        private const val READ_TIMEOUT_SECONDS = 60L
        private const val CONNECT_TIMEOUT_SECONDS = 15L
        private const val PIP_PROGRESS_REGEX_PATTERN = """(\d+)%"""
        private const val CACHE_DIR_NAME = "pip_cache"
        private const val BACKUP_DIR_NAME = "package_backups"
        private const val PACKAGES_CACHE_TTL_MS = 5 * 60 * 1000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _installedPackages = MutableStateFlow<List<InstalledPackage>>(emptyList())
    val installedPackages: StateFlow<List<InstalledPackage>> = _installedPackages.asStateFlow()

    private val _availablePackages = MutableStateFlow<List<PackageSearchResult>>(emptyList())
    val availablePackages: StateFlow<List<PackageSearchResult>> = _availablePackages.asStateFlow()

    private val _currentProgress = MutableStateFlow<ProgressState>(ProgressState.Idle)
    val currentProgress: StateFlow<ProgressState> = _currentProgress.asStateFlow()

    private val _installQueue = MutableStateFlow<List<InstallTask>>(emptyList())
    val installQueue: StateFlow<List<InstallTask>> = _installQueue.asStateFlow()

    private val _isProcessingQueue = MutableStateFlow(false)
    val isProcessingQueue: StateFlow<Boolean> = _isProcessingQueue.asStateFlow()

    private val _searchResults = MutableStateFlow<List<PackageSearchResult>>(emptyList())
    val searchResults: StateFlow<List<PackageSearchResult>> = _searchResults.asStateFlow()

    private val _errorMessages = MutableStateFlow<List<String>>(emptyList())
    val errorMessages: StateFlow<List<String>> = _errorMessages.asStateFlow()

    private val _freezeOutput = MutableStateFlow("")
    val freezeOutput: StateFlow<String> = _freezeOutput.asStateFlow()

    private val packageCache = ConcurrentHashMap<String, PackageInfo>()
    private var lastCacheRefresh = 0L

    private val taskComparator = Comparator<InstallTask> { a, b ->
        val priorityDiff = a.priority.ordinal - b.priority.ordinal
        if (priorityDiff != 0) return@Comparator priorityDiff
        a.createdAt.compareTo(b.createdAt)
    }
    private val installTaskQueue = PriorityQueue(taskComparator)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private var currentInstallJob: kotlinx.coroutines.Job? = null

    init {
        scope.launch {
            refreshInstalledPackages()
        }
    }

    override suspend fun searchPackages(query: String, page: Int): Result<PackageSearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                val searchUrl = "$PYPI_SEARCH_URL?q=${query}&page=$page"
                val request = Request.Builder()
                    .url(searchUrl)
                    .header("Accept", "application/json")
                    .header("User-Agent", "PythonIDE-Android/1.0")
                    .build()

                val response = executeHttpRequest(request)
                if (response.isSuccessful) {
                    val htmlBody = response.body?.string() ?: ""
                    val results = parseSearchResults(htmlBody, query)
                    _searchResults.value = results
                    if (results.isNotEmpty()) {
                        Result.success(results.first())
                    } else {
                        Result.success(PackageSearchResult(query = query, packages = emptyList(), totalCount = 0))
                    }
                } else {
                    val errorMsg = "Search failed with code ${response.code}"
                    addErrorMessage(errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                addErrorMessage("Search error: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun getPackageInfo(packageName: String): Result<PackageInfo> {
        return withContext(Dispatchers.IO) {
            try {
                packageCache[packageName]?.let {
                    if (System.currentTimeMillis() - lastCacheRefresh < PACKAGES_CACHE_TTL_MS) {
                        return@withContext Result.success(it)
                    }
                }

                val apiUrl = "$PYPI_BASE_URL/pypi/${packageName}/json"
                val request = Request.Builder()
                    .url(apiUrl)
                    .header("Accept", "application/json")
                    .header("User-Agent", "PythonIDE-Android/1.0")
                    .build()

                val response = executeHttpRequest(request)
                if (response.isSuccessful) {
                    val jsonBody = response.body?.string() ?: "{}"
                    val packageInfo = parsePackageInfo(jsonBody, packageName)
                    packageCache[packageName] = packageInfo
                    lastCacheRefresh = System.currentTimeMillis()
                    Result.success(packageInfo)
                } else if (response.code == 404) {
                    Result.failure(Exception("Package '$packageName' not found on PyPI"))
                } else {
                    Result.failure(Exception("Failed to fetch package info: ${response.code}"))
                }
            } catch (e: Exception) {
                addErrorMessage("Error fetching package info: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun getPackageVersions(packageName: String): Result<List<PackageVersion>> {
        return withContext(Dispatchers.IO) {
            try {
                val apiUrl = "$PYPI_BASE_URL/pypi/${packageName}/json"
                val request = Request.Builder()
                    .url(apiUrl)
                    .header("Accept", "application/json")
                    .header("User-Agent", "PythonIDE-Android/1.0")
                    .build()

                val response = executeHttpRequest(request)
                if (response.isSuccessful) {
                    val jsonBody = response.body?.string() ?: "{}"
                    val versions = parsePackageVersions(jsonBody)
                    Result.success(versions)
                } else {
                    Result.failure(Exception("Failed to fetch versions: ${response.code}"))
                }
            } catch (e: Exception) {
                addErrorMessage("Error fetching versions: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun getPackageCompatibility(packageName: String): Result<PackageCompatibility> {
        TODO("Not yet implemented")
    }

    override suspend fun checkNativeBinaries(packageName: String): Result<NativeBinaryInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun getInstalledPackages(): Flow<List<InstalledPackage>> = flow {
        emit(_installedPackages.value)
    }

    override suspend fun getInstalledPackage(packageName: String): Result<InstalledPackage> {
        val pkg = _installedPackages.value.find { it.name.equals(packageName, ignoreCase = true) }
        return if (pkg != null) {
            Result.success(pkg)
        } else {
            Result.failure(Exception("Package '$packageName' is not installed"))
        }
    }

    override suspend fun isPackageInstalled(packageName: String): Boolean {
        return _installedPackages.value.any { it.name.equals(packageName, ignoreCase = true) }
    }

    override suspend fun getInstalledVersion(packageName: String): String? {
        return _installedPackages.value.find { it.name.equals(packageName, ignoreCase = true) }?.version
    }

    override suspend fun installPackage(packageName: String, version: String?): Flow<InstallProgress> = flow {
        try {
            val taskId = generateTaskId()
            val pipArgs = buildList {
                add("pip")
                add("install")
                if (version != null) {
                    add("${packageName}==${version}")
                } else {
                    add(packageName)
                }
            }

            _currentProgress.value = ProgressState.Installing(packageName, 0f)
            emit(InstallProgress(taskId = taskId, packageName = packageName, status = InstallTaskStatus.INSTALLING, progress = 0f))

            val result = executePipCommandWithProgress(pipArgs)

            if (result.isSuccess) {
                _currentProgress.value = ProgressState.Idle
                emit(InstallProgress(taskId = taskId, packageName = packageName, status = InstallTaskStatus.COMPLETED, progress = 1f))
                refreshInstalledPackages()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Install failed"
                _currentProgress.value = ProgressState.Error(error)
                emit(InstallProgress(taskId = taskId, packageName = packageName, status = InstallTaskStatus.FAILED, progress = 0f, message = error))
                addErrorMessage("Failed to install $packageName: $error")
            }
        } catch (e: Exception) {
            _currentProgress.value = ProgressState.Error(e.message ?: "Unknown error")
            emit(InstallProgress(taskId = generateTaskId(), packageName = packageName, status = InstallTaskStatus.FAILED, progress = 0f, message = e.message ?: "Unknown error"))
            addErrorMessage("Error installing $packageName: ${e.message}")
        }
    }

    override suspend fun installPackages(packages: List<Pair<String, String?>>): Flow<InstallProgress> = flow {
        for ((packageName, version) in packages) {
            installPackage(packageName, version).collect { emit(it) }
        }
    }

    override suspend fun uninstallPackage(packageName: String): Flow<InstallProgress> = flow {
        try {
            val taskId = generateTaskId()
            _currentProgress.value = ProgressState.Installing(packageName, 0f)
            emit(InstallProgress(taskId = taskId, packageName = packageName, status = InstallTaskStatus.INSTALLING, progress = 0f, message = "Uninstalling..."))

            val pipArgs = buildList {
                add("pip")
                add("uninstall")
                add("-y")
                add(packageName)
            }

            val result = executePipCommand(pipArgs)

            if (result.isSuccess) {
                _currentProgress.value = ProgressState.Idle
                emit(InstallProgress(taskId = taskId, packageName = packageName, status = InstallTaskStatus.COMPLETED, progress = 1f))
                refreshInstalledPackages()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Uninstall failed"
                _currentProgress.value = ProgressState.Error(error)
                emit(InstallProgress(taskId = taskId, packageName = packageName, status = InstallTaskStatus.FAILED, progress = 0f, message = error))
                addErrorMessage(error)
            }
        } catch (e: Exception) {
            _currentProgress.value = ProgressState.Error(e.message ?: "Unknown error")
            emit(InstallProgress(taskId = generateTaskId(), packageName = packageName, status = InstallTaskStatus.FAILED, progress = 0f, message = e.message ?: "Unknown error"))
            addErrorMessage("Uninstall error: ${e.message}")
        }
    }

    override suspend fun upgradePackage(packageName: String): Flow<InstallProgress> = flow {
        try {
            val taskId = generateTaskId()
            val pipArgs = buildList {
                add("pip")
                add("install")
                add("--upgrade")
                add(packageName)
            }

            _currentProgress.value = ProgressState.Installing(packageName, 0f)
            emit(InstallProgress(taskId = taskId, packageName = packageName, status = InstallTaskStatus.INSTALLING, progress = 0f))

            val result = executePipCommandWithProgress(pipArgs)

            if (result.isSuccess) {
                _currentProgress.value = ProgressState.Idle
                emit(InstallProgress(taskId = taskId, packageName = packageName, status = InstallTaskStatus.COMPLETED, progress = 1f))
                refreshInstalledPackages()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Upgrade failed"
                _currentProgress.value = ProgressState.Error(error)
                emit(InstallProgress(taskId = taskId, packageName = packageName, status = InstallTaskStatus.FAILED, progress = 0f, message = error))
                addErrorMessage(error)
            }
        } catch (e: Exception) {
            _currentProgress.value = ProgressState.Error(e.message ?: "Unknown error")
            emit(InstallProgress(taskId = generateTaskId(), packageName = packageName, status = InstallTaskStatus.FAILED, progress = 0f, message = e.message ?: "Unknown error"))
            addErrorMessage("Error upgrading package: ${e.message}")
        }
    }

    override suspend fun downgradePackage(packageName: String, version: String): Flow<InstallProgress> = flow {
        try {
            val taskId = generateTaskId()
            val pipArgs = buildList {
                add("pip")
                add("install")
                add("${packageName}==${version}")
            }

            _currentProgress.value = ProgressState.Installing(packageName, 0f)
            emit(InstallProgress(taskId = taskId, packageName = packageName, status = InstallTaskStatus.INSTALLING, progress = 0f))

            val result = executePipCommandWithProgress(pipArgs)

            if (result.isSuccess) {
                _currentProgress.value = ProgressState.Idle
                emit(InstallProgress(taskId = taskId, packageName = packageName, status = InstallTaskStatus.COMPLETED, progress = 1f))
                refreshInstalledPackages()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Downgrade failed"
                _currentProgress.value = ProgressState.Error(error)
                emit(InstallProgress(taskId = taskId, packageName = packageName, status = InstallTaskStatus.FAILED, progress = 0f, message = error))
                addErrorMessage(error)
            }
        } catch (e: Exception) {
            _currentProgress.value = ProgressState.Error(e.message ?: "Unknown error")
            emit(InstallProgress(taskId = generateTaskId(), packageName = packageName, status = InstallTaskStatus.FAILED, progress = 0f, message = e.message ?: "Unknown error"))
            addErrorMessage("Error downgrading package: ${e.message}")
        }
    }

    override suspend fun reinstallPackage(packageName: String): Flow<InstallProgress> = flow {
        try {
            val taskId = generateTaskId()
            val pipArgs = buildList {
                add("pip")
                add("install")
                add("--force-reinstall")
                add(packageName)
            }

            _currentProgress.value = ProgressState.Installing(packageName, 0f)
            emit(InstallProgress(taskId = taskId, packageName = packageName, status = InstallTaskStatus.INSTALLING, progress = 0f))

            val result = executePipCommandWithProgress(pipArgs)

            if (result.isSuccess) {
                _currentProgress.value = ProgressState.Idle
                emit(InstallProgress(taskId = taskId, packageName = packageName, status = InstallTaskStatus.COMPLETED, progress = 1f))
                refreshInstalledPackages()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Reinstall failed"
                _currentProgress.value = ProgressState.Error(error)
                emit(InstallProgress(taskId = taskId, packageName = packageName, status = InstallTaskStatus.FAILED, progress = 0f, message = error))
                addErrorMessage(error)
            }
        } catch (e: Exception) {
            _currentProgress.value = ProgressState.Error(e.message ?: "Unknown error")
            emit(InstallProgress(taskId = generateTaskId(), packageName = packageName, status = InstallTaskStatus.FAILED, progress = 0f, message = e.message ?: "Unknown error"))
            addErrorMessage("Error reinstalling package: ${e.message}")
        }
    }

    override suspend fun batchInstall(request: BatchInstallRequest): Flow<InstallProgress> = flow {
        for ((packageName, version) in request.packages) {
            installPackage(packageName, version).collect { emit(it) }
        }
    }

    override suspend fun installFromWheel(wheelPath: String): Flow<InstallProgress> = flow {
        try {
            val file = File(wheelPath)
            if (!file.exists()) {
                emit(InstallProgress(taskId = generateTaskId(), packageName = file.nameWithoutExtension, status = InstallTaskStatus.FAILED, progress = 0f, message = "File not found: $wheelPath"))
                return@flow
            }

            val taskId = generateTaskId()
            _currentProgress.value = ProgressState.Installing(file.nameWithoutExtension, 0f)
            emit(InstallProgress(taskId = taskId, packageName = file.nameWithoutExtension, status = InstallTaskStatus.INSTALLING, progress = 0f))

            val pipArgs = buildList {
                add("pip")
                add("install")
                add(wheelPath)
            }

            val result = executePipCommandWithProgress(pipArgs)

            if (result.isSuccess) {
                _currentProgress.value = ProgressState.Idle
                emit(InstallProgress(taskId = taskId, packageName = file.nameWithoutExtension, status = InstallTaskStatus.COMPLETED, progress = 1f))
                refreshInstalledPackages()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Install failed"
                _currentProgress.value = ProgressState.Error(error)
                emit(InstallProgress(taskId = taskId, packageName = file.nameWithoutExtension, status = InstallTaskStatus.FAILED, progress = 0f, message = error))
                addErrorMessage(error)
            }
        } catch (e: Exception) {
            _currentProgress.value = ProgressState.Error(e.message ?: "Unknown error")
            emit(InstallProgress(taskId = generateTaskId(), packageName = "", status = InstallTaskStatus.FAILED, progress = 0f, message = e.message ?: "Unknown error"))
            addErrorMessage("Error installing wheel: ${e.message}")
        }
    }

    override suspend fun installFromArchive(archivePath: String): Flow<InstallProgress> = flow {
        try {
            val file = File(archivePath)
            if (!file.exists()) {
                emit(InstallProgress(taskId = generateTaskId(), packageName = file.nameWithoutExtension, status = InstallTaskStatus.FAILED, progress = 0f, message = "File not found: $archivePath"))
                return@flow
            }

            val taskId = generateTaskId()
            _currentProgress.value = ProgressState.Installing(file.nameWithoutExtension, 0f)
            emit(InstallProgress(taskId = taskId, packageName = file.nameWithoutExtension, status = InstallTaskStatus.INSTALLING, progress = 0f))

            val pipArgs = buildList {
                add("pip")
                add("install")
                add(archivePath)
            }

            val result = executePipCommandWithProgress(pipArgs)

            if (result.isSuccess) {
                _currentProgress.value = ProgressState.Idle
                emit(InstallProgress(taskId = taskId, packageName = file.nameWithoutExtension, status = InstallTaskStatus.COMPLETED, progress = 1f))
                refreshInstalledPackages()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Install failed"
                _currentProgress.value = ProgressState.Error(error)
                emit(InstallProgress(taskId = taskId, packageName = file.nameWithoutExtension, status = InstallTaskStatus.FAILED, progress = 0f, message = error))
                addErrorMessage(error)
            }
        } catch (e: Exception) {
            _currentProgress.value = ProgressState.Error(e.message ?: "Unknown error")
            emit(InstallProgress(taskId = generateTaskId(), packageName = "", status = InstallTaskStatus.FAILED, progress = 0f, message = e.message ?: "Unknown error"))
            addErrorMessage("Error installing archive: ${e.message}")
        }
    }

    override suspend fun installFromDirectory(dirPath: String): Flow<InstallProgress> = flow {
        try {
            val dir = File(dirPath)
            if (!dir.exists() || !dir.isDirectory) {
                emit(InstallProgress(taskId = generateTaskId(), packageName = dir.name, status = InstallTaskStatus.FAILED, progress = 0f, message = "Directory not found: $dirPath"))
                return@flow
            }

            val taskId = generateTaskId()
            _currentProgress.value = ProgressState.Installing(dir.name, 0f)
            emit(InstallProgress(taskId = taskId, packageName = dir.name, status = InstallTaskStatus.INSTALLING, progress = 0f))

            val pipArgs = buildList {
                add("pip")
                add("install")
                add(dirPath)
            }

            val result = executePipCommandWithProgress(pipArgs)

            if (result.isSuccess) {
                _currentProgress.value = ProgressState.Idle
                emit(InstallProgress(taskId = taskId, packageName = dir.name, status = InstallTaskStatus.COMPLETED, progress = 1f))
                refreshInstalledPackages()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Install failed"
                _currentProgress.value = ProgressState.Error(error)
                emit(InstallProgress(taskId = taskId, packageName = dir.name, status = InstallTaskStatus.FAILED, progress = 0f, message = error))
                addErrorMessage(error)
            }
        } catch (e: Exception) {
            _currentProgress.value = ProgressState.Error(e.message ?: "Unknown error")
            emit(InstallProgress(taskId = generateTaskId(), packageName = "", status = InstallTaskStatus.FAILED, progress = 0f, message = e.message ?: "Unknown error"))
            addErrorMessage("Error installing from directory: ${e.message}")
        }
    }

    override suspend fun cancelInstall(taskId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                synchronized(installTaskQueue) {
                    val task = installTaskQueue.find { it.id == taskId }
                    if (task != null) {
                        installTaskQueue.remove(task)
                        updateInstallQueueState()
                    }
                }

                if (currentInstallJob?.isActive == true) {
                    currentInstallJob?.cancel()
                    currentInstallJob = null
                }

                _currentProgress.value = ProgressState.Idle
                Result.success(true)
            } catch (e: Exception) {
                addErrorMessage("Error cancelling install: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun pauseInstall(taskId: String): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun resumeInstall(taskId: String): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun retryInstall(taskId: String): Flow<InstallProgress> = flow {
        val task = synchronized(installTaskQueue) {
            installTaskQueue.find { it.id == taskId }
        }
        if (task != null) {
            installPackage(task.packageName, task.version).collect { emit(it) }
        } else {
            emit(InstallProgress(taskId = taskId, packageName = "", status = InstallTaskStatus.FAILED, progress = 0f, message = "Task not found: $taskId"))
        }
    }

    override suspend fun getInstallQueue(): Flow<List<InstallTask>> = flow {
        emit(installTaskQueue.toList())
    }

    override suspend fun getActiveInstalls(): Flow<Map<String, InstallProgress>> = flow {
        TODO("Not yet implemented")
    }

    override suspend fun getCompletedInstalls(): Flow<List<InstallTask>> = flow {
        TODO("Not yet implemented")
    }

    override suspend fun getFailedInstalls(): Flow<List<InstallTask>> = flow {
        TODO("Not yet implemented")
    }

    override suspend fun getInstallLogs(taskId: String): Flow<List<String>> = flow {
        TODO("Not yet implemented")
    }

    override suspend fun clearCompletedInstalls(): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun clearFailedInstalls(): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun pipList(): Result<List<InstalledPackage>> {
        return withContext(Dispatchers.IO) {
            try {
                val pipArgs = buildList {
                    add("pip")
                    add("list")
                    add("--format=json")
                }

                val result = executePipCommand(pipArgs)
                if (result.isSuccess) {
                    val output = result.getOrNull() ?: ""
                    val packages = parseInstalledPackagesJson(output)
                    _installedPackages.value = packages
                    Result.success(packages)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to list packages"))
                }
            } catch (e: Exception) {
                addErrorMessage("Error listing packages: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun pipFreeze(): Result<String> {
        return freezePackages()
    }

    override suspend fun pipShow(packageName: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val pipArgs = buildList {
                    add("pip")
                    add("show")
                    add(packageName)
                }

                val result = executePipCommand(pipArgs)
                if (result.isSuccess) {
                    Result.success(result.getOrNull() ?: "")
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to show package info"))
                }
            } catch (e: Exception) {
                addErrorMessage("Error showing package info: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun pipCacheList(): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val pipArgs = buildList {
                    add("pip")
                    add("cache")
                    add("list")
                }

                val result = executePipCommand(pipArgs)
                if (result.isSuccess) {
                    val output = result.getOrNull() ?: ""
                    val files = output.lines().filter { it.isNotBlank() }
                    Result.success(files)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to list cache"))
                }
            } catch (e: Exception) {
                addErrorMessage("Error listing cache: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun pipCachePurge(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val pipArgs = buildList {
                    add("pip")
                    add("cache")
                    add("purge")
                }

                val result = executePipCommand(pipArgs)
                if (result.isSuccess) {
                    Result.success(true)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to purge cache"))
                }
            } catch (e: Exception) {
                addErrorMessage("Error purging cache: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun pipCacheDir(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val pipArgs = buildList {
                    add("pip")
                    add("cache")
                    add("dir")
                }

                val result = executePipCommand(pipArgs)
                if (result.isSuccess) {
                    Result.success(result.getOrNull() ?: "")
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to get cache dir"))
                }
            } catch (e: Exception) {
                addErrorMessage("Error getting cache dir: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun pipCheck(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val pipArgs = buildList {
                    add("pip")
                    add("check")
                }

                val result = executePipCommand(pipArgs)
                if (result.isSuccess) {
                    Result.success(result.getOrNull() ?: "")
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("pip check failed"))
                }
            } catch (e: Exception) {
                addErrorMessage("Error running pip check: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun getOfflinePackages(): Flow<List<OfflinePackage>> = flow {
        TODO("Not yet implemented")
    }

    override suspend fun addOfflinePackage(filePath: String): Result<OfflinePackage> {
        TODO("Not yet implemented")
    }

    override suspend fun removeOfflinePackage(packageId: String): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun createBackup(name: String, description: String): Result<PackageBackup> {
        TODO("Not yet implemented")
    }

    override suspend fun getBackups(): Flow<List<PackageBackup>> = flow {
        TODO("Not yet implemented")
    }

    override suspend fun restoreBackup(backupId: String): Flow<InstallProgress> = flow {
        TODO("Not yet implemented")
    }

    override suspend fun deleteBackup(backupId: String): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun exportBackup(backupId: String, exportPath: String): Result<String> {
        TODO("Not yet implemented")
    }

    override suspend fun importBackup(backupPath: String): Result<PackageBackup> {
        TODO("Not yet implemented")
    }

    override suspend fun resolveDependencies(packageName: String): Result<Map<String, List<String>>> {
        TODO("Not yet implemented")
    }

    override suspend fun getReverseDependencies(packageName: String): Result<List<String>> {
        TODO("Not yet implemented")
    }

    override suspend fun checkDependencyConflicts(packageName: String): Result<List<String>> {
        TODO("Not yet implemented")
    }

    override suspend fun getAvailableAbis(): List<String> {
        return Build.SUPPORTED_ABIS.toList()
    }

    override suspend fun getCompatiblePackages(): Flow<List<Package>> = flow {
        TODO("Not yet implemented")
    }

    override suspend fun getIncompatiblePackages(): Flow<List<Package>> = flow {
        TODO("Not yet implemented")
    }

    override suspend fun exportRequirements(): Result<String> {
        return freezePackages()
    }

    override suspend fun importRequirements(content: String): Result<Unit> {
        return installFromRequirements(content)
    }

    override suspend fun installFromRequirements(requirementsContent: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "requirements.txt")
                tempFile.writeText(requirementsContent)

                _currentProgress.value = ProgressState.Installing("requirements.txt", 0f)

                val pipArgs = buildList {
                    add("pip")
                    add("install")
                    add("-r")
                    add(tempFile.absolutePath)
                }

                val result = executePipCommandWithProgress(pipArgs)
                tempFile.delete()

                if (result.isSuccess) {
                    _currentProgress.value = ProgressState.Idle
                    refreshInstalledPackages()
                    Result.success(Unit)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Requirements install failed"
                    _currentProgress.value = ProgressState.Error(error)
                    addErrorMessage(error)
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                _currentProgress.value = ProgressState.Error(e.message ?: "Unknown error")
                addErrorMessage("Error installing requirements: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun freezePackages(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val pipArgs = buildList {
                    add("pip")
                    add("freeze")
                }

                val result = executePipCommand(pipArgs)
                if (result.isSuccess) {
                    val output = result.getOrNull() ?: ""
                    _freezeOutput.value = output
                    Result.success(output)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Freeze failed"))
                }
            } catch (e: Exception) {
                addErrorMessage("Error freezing packages: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun getInstalledPackageVersions(packageName: String): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val pipArgs = buildList {
                    add("pip")
                    add("show")
                    add(packageName)
                }

                val result = executePipCommand(pipArgs)
                if (result.isSuccess) {
                    val output = result.getOrNull() ?: ""
                    val info = parsePipShowOutput(output)
                    val version = info["Version"] ?: ""
                    Result.success(listOf(version))
                } else {
                    Result.failure(Exception("Package not found or error occurred"))
                }
            } catch (e: Exception) {
                addErrorMessage("Error getting installed versions: ${e.message}")
                Result.failure(e)
            }
        }
    }

    private fun processInstallQueue() {
        scope.launch {
            _isProcessingQueue.value = true

            while (true) {
                val task = synchronized(installTaskQueue) {
                    installTaskQueue.poll()
                }

                if (task == null) {
                    break
                }

                currentInstallJob = kotlinx.coroutines.coroutineScope {
                    launch {
                        executeInstallTask(task)
                    }
                }
            }

            _isProcessingQueue.value = false
        }
    }

    private suspend fun executeInstallTask(task: InstallTask) {
        updateTaskStatus(task.id, InstallTaskStatus.INSTALLING)

        try {
            val pipArgs = buildList {
                add("pip")
                add("install")
                if (task.version != null) {
                    add("${task.packageName}==${task.version}")
                } else {
                    add(task.packageName)
                }
            }

            _currentProgress.value = ProgressState.Installing(task.packageName, 0f)

            val result = executePipCommandWithProgress(pipArgs)

            if (result.isSuccess) {
                updateTaskStatus(task.id, InstallTaskStatus.COMPLETED)
                _currentProgress.value = ProgressState.Idle
                refreshInstalledPackages()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Install failed"
                updateTaskStatus(task.id, InstallTaskStatus.FAILED)
                _currentProgress.value = ProgressState.Error(error)
                addErrorMessage("Failed to install ${task.packageName}: $error")
            }
        } catch (e: Exception) {
            updateTaskStatus(task.id, InstallTaskStatus.FAILED)
            _currentProgress.value = ProgressState.Error(e.message ?: "Unknown error")
            addErrorMessage("Error installing ${task.packageName}: ${e.message}")
        }
    }

    private fun updateTaskStatus(taskId: String, status: InstallTaskStatus) {
        synchronized(installTaskQueue) {
            val tasks = installTaskQueue.toList()
            installTaskQueue.clear()
            tasks.forEach { task ->
                if (task.id == taskId) {
                    installTaskQueue.add(task.copy(status = status))
                } else {
                    installTaskQueue.add(task)
                }
            }
        }
        updateInstallQueueState()
    }

    private fun updateInstallQueueState() {
        val tasks = synchronized(installTaskQueue) {
            installTaskQueue.toList()
        }
        _installQueue.value = tasks
    }

    private suspend fun executePipCommand(args: List<String>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val command = args.joinToString(" ")
                val result = pythonNativeBridge.executeCommand(command)

                if (result.exitCode == 0) {
                    Result.success(result.output)
                } else {
                    Result.failure(Exception(result.error.ifEmpty { "Command failed with exit code ${result.exitCode}" }))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun executePipCommandWithProgress(args: List<String>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val command = args.joinToString(" ")
                val progressRegex = Regex(PIP_PROGRESS_REGEX_PATTERN)

                val result = pythonNativeBridge.executeCommandWithCallback(command) { line ->
                    val match = progressRegex.find(line)
                    if (match != null) {
                        val progress = match.groupValues[1].toFloatOrNull() ?: 0f
                        val currentPkg = extractPackageNameFromLine(line)
                        if (currentPkg != null) {
                            _currentProgress.value = ProgressState.Installing(currentPkg, progress / 100f)
                        }
                    }
                }

                if (result.exitCode == 0) {
                    Result.success(result.output)
                } else {
                    Result.failure(Exception(result.error.ifEmpty { "Command failed with exit code ${result.exitCode}" }))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun extractPackageNameFromLine(line: String): String? {
        val patterns = listOf(
            Regex("""Installing\s+(\S+)"""),
            Regex("""Downloading\s+(\S+)"""),
            Regex("""Collecting\s+(\S+)"""),
            Regex("""Processing\s+(\S+)""")
        )

        for (pattern in patterns) {
            val match = pattern.find(line)
            if (match != null) {
                return match.groupValues[1].trimEnd('/')
            }
        }
        return null
    }

    private suspend fun executeHttpRequest(request: Request): Response {
        return withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute()
        }
    }

    private fun parseSearchResults(html: String, query: String): List<PackageSearchResult> {
        val results = mutableListOf<Package>()

        val packagePattern = Regex("""<a\s+class="package-snippet__name"[^>]*>([^<]+)</a>""")
        val versionPattern = Regex("""<span\s+class="package-snippet__version[^"]*">([^<]+)</span>""")
        val descPattern = Regex("""<p\s+class="package-snippet__description[^"]*">([^<]+)</p>""")

        val packages = packagePattern.findAll(html).toList()
        val versions = versionPattern.findAll(html).toList()
        val descriptions = descPattern.findAll(html).toList()

        for (i in packages.indices) {
            val name = packages[i].groupValues[1].trim()
            val version = if (i < versions.size) versions[i].groupValues[1].trim() else ""
            val description = if (i < descriptions.size) descriptions[i].groupValues[1].trim() else ""

            results.add(
                Package(
                    name = name,
                    version = version,
                    summary = description,
                    isInstalled = _installedPackages.value.any { it.name.equals(name, ignoreCase = true) }
                )
            )
        }

        if (results.isEmpty() && html.contains("No results")) {
            return emptyList()
        }

        return listOf(PackageSearchResult(
            query = query,
            packages = results,
            totalCount = results.size
        ))
    }

    private fun parsePackageInfo(json: String, packageName: String): PackageInfo {
        val jsonObject = JSONObject(json)
        val info = jsonObject.getJSONObject("info")

        val name = info.optString("name", packageName)
        val version = info.optString("version", "")
        val summary = info.optString("summary", "")
        val author = info.optString("author", "")
        val homePage = info.optString("home_page", "")
        val licenseText = info.optString("license", "")
        val description = info.optString("description", "")
        val requiresPython = info.optString("requires_python", "")

        val requiresDist = mutableListOf<String>()
        if (info.has("requires_dist")) {
            val distArray = info.getJSONArray("requires_dist")
            for (i in 0 until distArray.length()) {
                distArray.optString(i)?.let { requiresDist.add(it) }
            }
        }

        val classifiers = mutableListOf<String>()
        if (info.has("classifiers")) {
            val classifiersArray = info.getJSONArray("classifiers")
            for (i in 0 until classifiersArray.length()) {
                classifiersArray.optString(i)?.let { classifiers.add(it) }
            }
        }

        val versions = mutableListOf<PackageVersion>()
        if (jsonObject.has("releases")) {
            val releases = jsonObject.getJSONObject("releases")
            for (versionKey in releases.keys()) {
                val files = releases.getJSONArray(versionKey)
                val uploadTime = if (files.length() > 0) {
                    files.getJSONObject(0).optString("upload_time", "")
                } else ""

                versions.add(
                    PackageVersion(
                        version = versionKey,
                        uploadTime = uploadTime
                    )
                )
            }
        }

        return PackageInfo(
            name = name,
            version = version,
            summary = summary,
            author = author,
            homePage = homePage,
            license = licenseText,
            classifiers = classifiers,
            versions = versions
        )
    }

    private fun parsePackageVersions(json: String): List<PackageVersion> {
        val jsonObject = JSONObject(json)
        val versions = mutableListOf<PackageVersion>()

        if (jsonObject.has("releases")) {
            val releases = jsonObject.getJSONObject("releases")
            for (versionKey in releases.keys()) {
                val files = releases.getJSONArray(versionKey)
                val uploadTime = if (files.length() > 0) {
                    files.getJSONObject(0).optString("upload_time", "")
                } else ""

                versions.add(
                    PackageVersion(
                        version = versionKey,
                        uploadTime = uploadTime
                    )
                )
            }
        }

        return versions.sortedByDescending { it.version }
    }

    private fun parseInstalledPackagesJson(json: String): List<InstalledPackage> {
        val packages = mutableListOf<InstalledPackage>()

        try {
            val jsonArray = JSONArray(json)

            for (i in 0 until jsonArray.length()) {
                val pkgObj = jsonArray.getJSONObject(i)
                val name = pkgObj.optString("name", "")
                val version = pkgObj.optString("version", "")

                if (name.isNotBlank()) {
                    packages.add(
                        InstalledPackage(
                            name = name,
                            version = version
                        )
                    )
                }
            }
        } catch (e: Exception) {
            val lines = json.lines()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isBlank()) continue

                val parts = trimmed.split(Regex("\\s+"))
                if (parts.size >= 2) {
                    packages.add(
                        InstalledPackage(
                            name = parts[0],
                            version = parts[1]
                        )
                    )
                }
            }
        }

        return packages.sortedBy { it.name.lowercase() }
    }

    private fun parsePipShowOutput(output: String): Map<String, String> {
        val info = mutableMapOf<String, String>()
        val lines = output.lines()

        for (line in lines) {
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val key = line.substring(0, colonIndex).trim()
                val value = line.substring(colonIndex + 1).trim()
                info[key] = value
            }
        }

        return info
    }

    private fun refreshInstalledPackages() {
        scope.launch {
            try {
                val pipArgs = buildList {
                    add("pip")
                    add("list")
                    add("--format=json")
                }

                val result = executePipCommand(pipArgs)
                if (result.isSuccess) {
                    val output = result.getOrNull() ?: ""
                    val packages = parseInstalledPackagesJson(output)
                    _installedPackages.value = packages
                }
            } catch (e: Exception) {
                addErrorMessage("Error refreshing packages: ${e.message}")
            }
        }
    }

    private fun addErrorMessage(message: String) {
        _errorMessages.update { current ->
            val newMessages = current.toMutableList()
            newMessages.add(message)
            if (newMessages.size > 50) {
                newMessages.removeAt(0)
            }
            newMessages
        }
    }

    private fun generateTaskId(): String {
        return "task_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
    }
}
