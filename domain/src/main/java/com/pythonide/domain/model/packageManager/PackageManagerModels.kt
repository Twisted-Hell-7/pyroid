package com.pythonide.domain.model.packageManager

import java.util.UUID

data class Package(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val version: String,
    val summary: String = "",
    val description: String = "",
    val author: String = "",
    val authorEmail: String = "",
    val homePage: String = "",
    val license: String = "",
    val requires: List<String> = emptyList(),
    val extras: Map<String, List<String>> = emptyMap(),
    val projectUrl: String = "",
    val packageUrl: String = "",
    val filename: String = "",
    val size: Long = 0,
    val uploadTime: String = "",
    val pythonVersion: String = "",
    val requiresPython: String = "",
    val classifiers: List<String> = emptyList(),
    val isInstalled: Boolean = false,
    val installedVersion: String? = null,
    val installPath: String? = null,
    val isOutdated: Boolean = false,
    val latestVersion: String? = null
)

data class InstalledPackage(
    val name: String,
    val version: String,
    val summary: String = "",
    val location: String = "",
    val requires: List<String> = emptyList(),
    val requiredBy: List<String> = emptyList(),
    val installer: String = "pip",
    val isSystemPackage: Boolean = false,
    val isUpgradable: Boolean = false,
    val latestVersion: String? = null
)

data class PackageSearchResult(
    val query: String,
    val packages: List<Package>,
    val totalCount: Int,
    val page: Int = 1,
    val pageSize: Int = 25,
    val hasMore: Boolean = false
)

data class PackageVersion(
    val version: String,
    val uploadTime: String = "",
    val size: Long = 0,
    val filename: String = "",
    val pythonVersion: String = "",
    val requiresPython: String = "",
    val hasWheel: Boolean = false,
    val hasSource: Boolean = false
)

data class PackageInfo(
    val name: String,
    val summary: String = "",
    val version: String = "",
    val homePage: String = "",
    val author: String = "",
    val authorEmail: String = "",
    val license: String = "",
    val location: String = "",
    val requires: List<String> = emptyList(),
    val requiredBy: List<String> = emptyList(),
    val versions: List<PackageVersion> = emptyList(),
    val classifiers: List<String> = emptyList()
)

data class InstallTask(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val version: String? = null,
    val status: InstallTaskStatus = InstallTaskStatus.QUEUED,
    val progress: Float = 0f,
    val message: String = "",
    val error: String? = null,
    val logs: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val isBackground: Boolean = false,
    val priority: InstallPriority = InstallPriority.NORMAL
)

enum class InstallTaskStatus {
    QUEUED,
    DOWNLOADING,
    INSTALLING,
    RESOLVING_DEPS,
    COMPLETED,
    FAILED,
    CANCELLED,
    PAUSED
}

enum class InstallPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

data class InstallProgress(
    val taskId: String,
    val packageName: String,
    val status: InstallTaskStatus,
    val progress: Float,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val speed: Long = 0,
    val message: String = ""
)

data class PackageManagerState(
    val searchQuery: String = "",
    val searchResults: PackageSearchResult? = null,
    val installedPackages: List<InstalledPackage> = emptyList(),
    val availablePackages: List<Package> = emptyList(),
    val selectedPackage: PackageInfo? = null,
    val installQueue: List<InstallTask> = emptyList(),
    val activeInstalls: Map<String, InstallProgress> = emptyMap(),
    val completedInstalls: List<InstallTask> = emptyList(),
    val failedInstalls: List<InstallTask> = emptyList(),
    val installLogs: Map<String, List<String>> = emptyMap(),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val error: String? = null,
    val viewMode: PackageViewMode = PackageViewMode.SEARCH,
    val sortBy: PackageSortBy = PackageSortBy.NAME,
    val sortOrder: PackageSortOrder = PackageSortOrder.ASCENDING,
    val filterInstalled: Boolean = false,
    val filterUpgradable: Boolean = false,
    val offlinePackages: List<OfflinePackage> = emptyList(),
    val backupPackages: List<PackageBackup> = emptyList(),
    val dependencyTree: Map<String, List<String>> = emptyMap(),
    val compatiblePackages: Int = 0,
    val incompatiblePackages: Int = 0
) {
    val filteredInstalledPackages: List<InstalledPackage>
        get() = installedPackages
            .filter { pkg ->
                (searchQuery.isEmpty() || pkg.name.contains(searchQuery, ignoreCase = true) ||
                        pkg.summary.contains(searchQuery, ignoreCase = true)) &&
                (!filterUpgradable || pkg.isUpgradable)
            }
            .sortedWith(
                when (sortBy) {
                    PackageSortBy.NAME -> compareBy<InstalledPackage> { it.name.lowercase() }
                    PackageSortBy.VERSION -> compareBy<InstalledPackage> { it.version }
                    PackageSortBy.SIZE -> compareBy<InstalledPackage> { it.location.length }
                }.let { if (sortOrder == PackageSortOrder.DESCENDING) it.reversed() else it }
            )

    val queueCount: Int
        get() = installQueue.count { it.status == InstallTaskStatus.QUEUED }

    val activeCount: Int
        get() = activeInstalls.size

    val completedCount: Int
        get() = completedInstalls.size

    val failedCount: Int
        get() = failedInstalls.size

    val hasActiveInstalls: Boolean
        get() = activeInstalls.isNotEmpty() || installQueue.any { it.status == InstallTaskStatus.QUEUED }
}

enum class PackageViewMode {
    SEARCH,
    INSTALLED,
    QUEUE,
    LOGS,
    BACKUP,
    OFFLINE
}

enum class PackageSortBy {
    NAME,
    VERSION,
    SIZE
}

enum class PackageSortOrder {
    ASCENDING,
    DESCENDING
}

data class OfflinePackage(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val version: String,
    val filePath: String,
    val fileType: OfflineFileType,
    val size: Long,
    val isCompatible: Boolean,
    val addedAt: Long = System.currentTimeMillis()
)

enum class OfflineFileType {
    WHEEL,
    TAR_GZ,
    ZIP,
    DIRECTORY
}

data class PackageBackup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val packages: List<InstalledPackage>,
    val requirementsContent: String,
    val createdAt: Long = System.currentTimeMillis(),
    val filePath: String,
    val size: Long = 0
)

data class PackageCompatibility(
    val packageName: String,
    val isCompatible: Boolean,
    val reason: String = "",
    val needsNativeBinaries: Boolean = false,
    val requiredAbis: List<String> = emptyList(),
    val availableAbis: List<String> = emptyList(),
    val alternativePackages: List<String> = emptyList(),
    val detailedLog: String = ""
)

data class NativeBinaryInfo(
    val packageName: String,
    val requiredAbi: String,
    val availableAbi: String?,
    val soFiles: List<String> = emptyList(),
    val isAvailable: Boolean = false,
    val alternatives: List<String> = emptyList()
)

enum class PackageAction {
    INSTALL,
    UNINSTALL,
    UPGRADE,
    DOWNGRADE,
    REINSTALL
}

data class BatchInstallRequest(
    val packages: List<Pair<String, String?>>,
    val priority: InstallPriority = InstallPriority.NORMAL,
    val isBackground: Boolean = false
)

sealed class ProgressState {
    data object Idle : ProgressState()
    data class Installing(val packageName: String, val progress: Float) : ProgressState()
    data class Completed(val packageName: String) : ProgressState()
    data class Failed(val packageName: String, val error: String) : ProgressState()
    data class Error(val message: String) : ProgressState()
}

data class PackageDependency(
    val packageName: String = "",
    val version: String = "",
    val dependencies: List<String> = emptyList(),
    val name: String = packageName,
    val requiredBy: String = "",
    val versionSpec: String = "",
    val isReverseDependency: Boolean = false
)

data class PipCacheInfo(
    val size: Long = 0,
    val packageCount: Int = 0,
    val cacheDir: String = "",
    val httpCacheSize: Long = 0,
    val wheelCacheSize: Long = 0,
    val lastCleaned: Long = 0
)
