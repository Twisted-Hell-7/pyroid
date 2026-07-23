package com.pythonide.data.repository

import android.content.Context
import android.os.Build
import com.pythonide.data.runtime.core.PythonNativeBridge
import com.pythonide.domain.model.packageManager.InstallTask
import com.pythonide.domain.model.packageManager.InstallTaskStatus
import com.pythonide.domain.model.packageManager.InstalledPackage
import com.pythonide.domain.model.packageManager.PackageDependency
import com.pythonide.domain.model.packageManager.PackageInfo
import com.pythonide.domain.model.packageManager.PackageSearchResult
import com.pythonide.domain.model.packageManager.PackageVersion
import com.pythonide.domain.model.packageManager.PipCacheInfo
import com.pythonide.domain.model.packageManager.ProgressState
import com.pythonide.domain.repository.PackageManagerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.PriorityQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile
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

    private var currentInstallJob: Job? = null

    init {
        scope.launch {
            refreshInstalledPackages()
        }
    }

    override suspend fun searchPackages(query: String, page: Int): Result<List<PackageSearchResult>> {
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
                    Result.success(results)
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

    override suspend fun installPackage(
        packageName: String,
        version: String?,
        upgrade: Boolean,
        forceReinstall: Boolean,
        noDeps: Boolean,
        priority: InstallTask.Priority
    ): Result<InstallTask> {
        return withContext(Dispatchers.IO) {
            try {
                val taskId = generateTaskId()
                val task = InstallTask(
                    id = taskId,
                    packageName = packageName,
                    version = version,
                    upgrade = upgrade,
                    forceReinstall = forceReinstall,
                    noDeps = noDeps,
                    priority = priority,
                    status = InstallTaskStatus.Pending,
                    createdAt = System.currentTimeMillis()
                )

                synchronized(installTaskQueue) {
                    installTaskQueue.add(task)
                }
                updateInstallQueueState()

                if (!_isProcessingQueue.value) {
                    processInstallQueue()
                }

                Result.success(task)
            } catch (e: Exception) {
                addErrorMessage("Failed to queue install: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun uninstallPackage(packageName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                _currentProgress.value = ProgressState.Uninstalling(packageName)

                val pipArgs = buildList {
                    add("pip")
                    add("uninstall")
                    add("-y")
                    add(packageName)
                }

                val result = executePipCommand(pipArgs)

                if (result.isSuccess) {
                    _currentProgress.value = ProgressState.Idle
                    refreshInstalledPackages()
                    Result.success(Unit)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Uninstall failed"
                    _currentProgress.value = ProgressState.Error(error)
                    addErrorMessage(error)
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                _currentProgress.value = ProgressState.Error(e.message ?: "Unknown error")
                addErrorMessage("Uninstall error: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun listInstalledPackages(): Result<List<InstalledPackage>> {
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

    override suspend fun showPackageInfo(packageName: String): Result<Map<String, String>> {
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
                    Result.success(info)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to show package info"))
                }
            } catch (e: Exception) {
                addErrorMessage("Error showing package info: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun installOfflinePackage(filePath: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return@withContext Result.failure(Exception("File not found: $filePath"))
                }

                if (!isPackageFileSupported(filePath)) {
                    return@withContext Result.failure(
                        Exception("Unsupported package format. Supported: .whl, .tar.gz, .zip")
                    )
                }

                val compatibilityCheck = checkNativeBinaryCompatibility(filePath)
                if (!compatibilityCheck.isSuccess) {
                    return@withContext Result.failure(
                        compatibilityCheck.exceptionOrNull()
                            ?: Exception("Compatibility check failed")
                    )
                }

                _currentProgress.value = ProgressState.Installing(file.name, 0f)

                val pipArgs = buildList {
                    add("pip")
                    add("install")
                    add(filePath)
                }

                val result = executePipCommandWithProgress(pipArgs)

                if (result.isSuccess) {
                    _currentProgress.value = ProgressState.Idle
                    refreshInstalledPackages()
                    Result.success(Unit)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Offline install failed"
                    _currentProgress.value = ProgressState.Error(error)
                    addErrorMessage(error)
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                _currentProgress.value = ProgressState.Error(e.message ?: "Unknown error")
                addErrorMessage("Error installing offline package: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun installFromUrl(url: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val tempDir = File(context.cacheDir, "downloaded_packages")
                tempDir.mkdirs()

                val fileName = url.substringAfterLast("/").ifEmpty { "package.whl" }
                val tempFile = File(tempDir, fileName)

                _currentProgress.value = ProgressState.Downloading(0f)

                downloadFile(url, tempFile) { progress ->
                    _currentProgress.value = ProgressState.Downloading(progress)
                }

                val installResult = installOfflinePackage(tempFile.absolutePath)
                tempFile.delete()
                tempDir.deleteRecursively()

                installResult
            } catch (e: Exception) {
                _currentProgress.value = ProgressState.Error(e.message ?: "Unknown error")
                addErrorMessage("Error installing from URL: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun upgradePackage(packageName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val pipArgs = buildList {
                    add("pip")
                    add("install")
                    add("--upgrade")
                    add(packageName)
                }

                _currentProgress.value = ProgressState.Updating(packageName, 0f)
                val result = executePipCommandWithProgress(pipArgs)

                if (result.isSuccess) {
                    _currentProgress.value = ProgressState.Idle
                    refreshInstalledPackages()
                    Result.success(Unit)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Upgrade failed"
                    _currentProgress.value = ProgressState.Error(error)
                    addErrorMessage(error)
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                _currentProgress.value = ProgressState.Error(e.message ?: "Unknown error")
                addErrorMessage("Error upgrading package: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun upgradeAllPackages(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val pipArgs = buildList {
                    add("pip")
                    add("install")
                    add("--upgrade")
                    add("--all")
                }

                _currentProgress.value = ProgressState.Updating("all packages", 0f)
                val result = executePipCommandWithProgress(pipArgs)

                if (result.isSuccess) {
                    _currentProgress.value = ProgressState.Idle
                    refreshInstalledPackages()
                    Result.success(Unit)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Upgrade all failed"
                    _currentProgress.value = ProgressState.Error(error)
                    addErrorMessage(error)
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                _currentProgress.value = ProgressState.Error(e.message ?: "Unknown error")
                addErrorMessage("Error upgrading all packages: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun searchInstalledPackages(query: String): Result<List<InstalledPackage>> {
        return withContext(Dispatchers.IO) {
            try {
                val allPackages = _installedPackages.value
                val filtered = if (query.isBlank()) {
                    allPackages
                } else {
                    allPackages.filter {
                        it.name.contains(query, ignoreCase = true) ||
                                it.version.contains(query, ignoreCase = true)
                    }
                }
                Result.success(filtered)
            } catch (e: Exception) {
                addErrorMessage("Error searching installed packages: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun getPackageDependencies(packageName: String): Result<List<PackageDependency>> {
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
                    val deps = parseDependencies(output)
                    Result.success(deps)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to get dependencies"))
                }
            } catch (e: Exception) {
                addErrorMessage("Error getting dependencies: ${e.message}")
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

    override suspend fun getPipCacheInfo(): Result<PipCacheInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val pipArgs = buildList {
                    add("pip")
                    add("cache")
                    add("info")
                }

                val result = executePipCommand(pipArgs)
                if (result.isSuccess) {
                    val output = result.getOrNull() ?: ""
                    val cacheInfo = parseCacheInfo(output)
                    Result.success(cacheInfo)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to get cache info"))
                }
            } catch (e: Exception) {
                addErrorMessage("Error getting cache info: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun clearPipCache(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val pipArgs = buildList {
                    add("pip")
                    add("cache")
                    add("purge")
                }

                val result = executePipCommand(pipArgs)
                if (result.isSuccess) {
                    Result.success(Unit)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to clear cache"))
                }
            } catch (e: Exception) {
                addErrorMessage("Error clearing cache: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun backupPackages(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val freezeResult = freezePackages()
                if (freezeResult.isFailure) {
                    return@withContext Result.failure(
                        freezeResult.exceptionOrNull() ?: Exception("Freeze failed")
                    )
                }

                val freezeContent = freezeResult.getOrDefault("")
                val backupDir = File(context.filesDir, BACKUP_DIR_NAME)
                backupDir.mkdirs()

                val timestamp = System.currentTimeMillis()
                val backupFile = File(backupDir, "backup_$timestamp.txt")
                backupFile.writeText(freezeContent)

                val installedJson = JSONObject()
                val packagesArray = JSONArray()

                for (pkg in _installedPackages.value) {
                    val pkgJson = JSONObject().apply {
                        put("name", pkg.name)
                        put("version", pkg.version)
                    }
                    packagesArray.put(pkgJson)
                }

                installedJson.put("packages", packagesArray)
                installedJson.put("timestamp", timestamp)
                installedJson.put("freeze_content", freezeContent)

                val metadataFile = File(backupDir, "backup_${timestamp}_metadata.json")
                metadataFile.writeText(installedJson.toString(2))

                Result.success(backupFile.absolutePath)
            } catch (e: Exception) {
                addErrorMessage("Error backing up packages: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun restorePackages(backupPath: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val backupFile = File(backupPath)
                if (!backupFile.exists()) {
                    return@withContext Result.failure(Exception("Backup file not found: $backupPath"))
                }

                val requirementsContent = backupFile.readText()
                if (requirementsContent.isBlank()) {
                    return@withContext Result.failure(Exception("Backup file is empty"))
                }

                val metadataFile = File(
                    context.filesDir,
                    "$BACKUP_DIR_NAME/${backupFile.nameWithoutExtension}_metadata.json"
                )
                if (metadataFile.exists()) {
                    val metadata = JSONObject(metadataFile.readText())
                    val timestamp = metadata.optLong("timestamp", 0L)
                    if (timestamp > 0) {
                        val timeSinceBackup = System.currentTimeMillis() - timestamp
                        if (timeSinceBackup > 7 * 24 * 60 * 60 * 1000L) {
                            addErrorMessage("Warning: This backup is over 7 days old")
                        }
                    }
                }

                installFromRequirements(requirementsContent)
            } catch (e: Exception) {
                addErrorMessage("Error restoring packages: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun cancelInstall(taskId: String): Result<Unit> {
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
                Result.success(Unit)
            } catch (e: Exception) {
                addErrorMessage("Error cancelling install: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun retryInstall(taskId: String): Result<InstallTask> {
        return withContext(Dispatchers.IO) {
            try {
                val task = synchronized(installTaskQueue) {
                    installTaskQueue.find { it.id == taskId }
                }

                if (task != null) {
                    installPackage(
                        packageName = task.packageName,
                        version = task.version,
                        upgrade = task.upgrade,
                        forceReinstall = task.forceReinstall,
                        noDeps = task.noDeps,
                        priority = task.priority
                    )
                } else {
                    Result.failure(Exception("Task not found: $taskId"))
                }
            } catch (e: Exception) {
                addErrorMessage("Error retrying install: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun clearInstallQueue(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                synchronized(installTaskQueue) {
                    installTaskQueue.clear()
                }
                updateInstallQueueState()
                _currentProgress.value = ProgressState.Idle
                Result.success(Unit)
            } catch (e: Exception) {
                addErrorMessage("Error clearing queue: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun pipCommand(args: List<String>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val pipArgs = buildList {
                    add("pip")
                    addAll(args)
                }
                val result = executePipCommand(pipArgs)
                if (result.isSuccess) {
                    Result.success(result.getOrDefault(""))
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("pip command failed"))
                }
            } catch (e: Exception) {
                addErrorMessage("pip command error: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override fun refreshInstalledPackages() {
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

    override fun clearErrors() {
        _errorMessages.value = emptyList()
    }

    override suspend fun checkNativeBinaryCompatibility(filePath: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return@withContext Result.failure(Exception("File not found: $filePath"))
                }

                if (filePath.endsWith(".whl")) {
                    val compatible = checkWheelCompatibility(filePath)
                    return@withContext if (compatible) {
                        Result.success(true)
                    } else {
                        val deviceAbis = Build.SUPPORTED_ABIS.joinToString(", ")
                        Result.failure(
                            Exception(
                                "Binary compatibility issue detected. " +
                                        "Device ABIs: $deviceAbis. " +
                                        "Package may contain native binaries for incompatible architectures."
                            )
                        )
                    }
                }

                Result.success(true)
            } catch (e: Exception) {
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

    override suspend fun exportRequirements(): Result<String> {
        return freezePackages()
    }

    override suspend fun importRequirements(content: String): Result<Unit> {
        return installFromRequirements(content)
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

                currentInstallJob = launch {
                    executeInstallTask(task)
                }
                currentInstallJob?.join()
            }

            _isProcessingQueue.value = false
        }
    }

    private suspend fun executeInstallTask(task: InstallTask) {
        updateTaskStatus(task.id, InstallTaskStatus.Running)

        try {
            val pipArgs = buildList {
                add("pip")
                add("install")
                if (task.upgrade) add("--upgrade")
                if (task.forceReinstall) add("--force-reinstall")
                if (task.noDeps) add("--no-deps")
                if (task.version != null) {
                    add("${task.packageName}==${task.version}")
                } else {
                    add(task.packageName)
                }
            }

            _currentProgress.value = ProgressState.Installing(task.packageName, 0f)

            val result = executePipCommandWithProgress(pipArgs)

            if (result.isSuccess) {
                updateTaskStatus(task.id, InstallTaskStatus.Completed)
                _currentProgress.value = ProgressState.Idle
                refreshInstalledPackages()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Install failed"
                updateTaskStatus(task.id, InstallTaskStatus.Failed(error))
                _currentProgress.value = ProgressState.Error(error)
                addErrorMessage("Failed to install ${task.packageName}: $error")
            }
        } catch (e: Exception) {
            updateTaskStatus(task.id, InstallTaskStatus.Failed(e.message ?: "Unknown error"))
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
        val results = mutableListOf<PackageSearchResult>()

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
                PackageSearchResult(
                    name = name,
                    latestVersion = version,
                    description = description,
                    isInstalled = _installedPackages.value.any { it.name.equals(name, ignoreCase = true) }
                )
            )
        }

        if (results.isEmpty() && html.contains("No results")) {
            return emptyList()
        }

        return results
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
            description = description,
            requiresPython = requiresPython,
            requiresDist = requiresDist,
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

    private fun parseDependencies(output: String): List<PackageDependency> {
        val deps = mutableListOf<PackageDependency>()
        val info = parsePipShowOutput(output)

        val requires = info["Requires"]
        if (!requires.isNullOrBlank()) {
            val depNames = requires.split(",").map { it.trim() }
            for (depName in depNames) {
                if (depName.isNotBlank()) {
                    deps.add(
                        PackageDependency(
                            name = depName,
                            requiredBy = info["Name"] ?: "",
                            versionSpec = ""
                        )
                    )
                }
            }
        }

        val requiredBy = info["Required-by"]
        if (!requiredBy.isNullOrBlank()) {
            val dependents = requiredBy.split(",").map { it.trim() }
            for (dependent in dependents) {
                if (dependent.isNotBlank()) {
                    deps.add(
                        PackageDependency(
                            name = dependent,
                            requiredBy = info["Name"] ?: "",
                            versionSpec = "",
                            isReverseDependency = true
                        )
                    )
                }
            }
        }

        return deps
    }

    private fun parseCacheInfo(output: String): PipCacheInfo {
        val lines = output.lines()
        var packageCount = 0
        var httpSize = ""
        var wheelSize = ""
        var lastCleaned = ""

        for (line in lines) {
            when {
                line.contains("Package index page cache") -> {
                    packageCount = Regex("\\d+").find(line)?.value?.toIntOrNull() ?: 0
                }
                line.contains("http") || line.contains("HTTP") -> {
                    httpSize = line.substringAfter(":").trim()
                }
                line.contains("wheel") || line.contains("WHEEL") -> {
                    wheelSize = line.substringAfter(":").trim()
                }
                line.contains("cleaned") || line.contains("last") -> {
                    lastCleaned = line.substringAfter(":").trim()
                }
            }
        }

        if (packageCount == 0) {
            for (line in lines) {
                val number = Regex("\\d+").find(line)?.value?.toIntOrNull()
                if (number != null && packageCount == 0) {
                    packageCount = number
                }
            }
        }

        return PipCacheInfo(
            packageCount = packageCount,
            httpCacheSize = httpSize,
            wheelCacheSize = wheelSize,
            lastCleaned = lastCleaned
        )
    }

    private fun isPackageFileSupported(filePath: String): Boolean {
        return filePath.endsWith(".whl") ||
                filePath.endsWith(".tar.gz") ||
                filePath.endsWith(".zip")
    }

    private suspend fun checkWheelCompatibility(filePath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.name.endsWith(".whl")) return@withContext true

                val parts = file.nameWithoutExtension.split("-")
                if (parts.size < 4) return@withContext true

                val wheelTag = parts.last()
                val tagParts = wheelTag.split("-")

                if (tagParts.size >= 3) {
                    val abiTag = tagParts[1]
                    val platformTag = tagParts[2]

                    if (abiTag == "none" || platformTag == "any") {
                        return@withContext true
                    }

                    val supportedAbis = Build.SUPPORTED_ABIS.toSet()
                    val wheelCompatibleAbis = extractAbisFromTag(abiTag)

                    return@withContext wheelCompatibleAbis.isEmpty() ||
                            wheelCompatibleAbis.any { it in supportedAbis }
                }

                true
            } catch (e: Exception) {
                true
            }
        }
    }

    private fun extractAbisFromTag(abiTag: String): Set<String> {
        val abis = mutableSetOf<String>()
        val abiMap = mapOf(
            "arm64" to "arm64-v8a",
            "armv7l" to "armeabi-v7a",
            "armv7" to "armeabi-v7a",
            "x86_64" to "x86_64",
            "x86" to "x86",
            "aarch64" to "arm64-v8a"
        )

        for ((key, value) in abiMap) {
            if (abiTag.contains(key, ignoreCase = true)) {
                abis.add(value)
            }
        }

        return abis
    }

    private suspend fun downloadFile(
        url: String,
        destination: File,
        onProgress: (Float) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("Download failed with code ${response.code}")
            }

            val body = response.body ?: throw Exception("Empty response body")
            val contentLength = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        if (contentLength > 0) {
                            val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                            onProgress(progress)
                        }
                    }
                }
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
