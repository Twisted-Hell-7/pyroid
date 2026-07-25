package com.pythonide.domain.repository

import com.pythonide.domain.model.packageManager.BatchInstallRequest
import com.pythonide.domain.model.packageManager.InstallProgress
import com.pythonide.domain.model.packageManager.InstallTask
import com.pythonide.domain.model.packageManager.InstalledPackage
import com.pythonide.domain.model.packageManager.NativeBinaryInfo
import com.pythonide.domain.model.packageManager.OfflinePackage
import com.pythonide.domain.model.packageManager.Package
import com.pythonide.domain.model.packageManager.PackageBackup
import com.pythonide.domain.model.packageManager.PackageCompatibility
import com.pythonide.domain.model.packageManager.PackageInfo
import com.pythonide.domain.model.packageManager.PackageSearchResult
import com.pythonide.domain.model.packageManager.PackageVersion
import kotlinx.coroutines.flow.Flow

interface PackageManagerRepository {
    suspend fun searchPackages(query: String, page: Int = 1): Result<PackageSearchResult>
    suspend fun getPackageInfo(packageName: String): Result<PackageInfo>
    suspend fun getPackageVersions(packageName: String): Result<List<PackageVersion>>
    suspend fun getPackageCompatibility(packageName: String): Result<PackageCompatibility>
    suspend fun checkNativeBinaries(packageName: String): Result<NativeBinaryInfo>

    suspend fun getInstalledPackages(): Flow<List<InstalledPackage>>
    suspend fun getInstalledPackage(packageName: String): Result<InstalledPackage>
    suspend fun isPackageInstalled(packageName: String): Boolean
    suspend fun getInstalledVersion(packageName: String): String?

    suspend fun installPackage(packageName: String, version: String? = null): Flow<InstallProgress>
    suspend fun installPackages(packages: List<Pair<String, String?>>): Flow<InstallProgress>
    suspend fun uninstallPackage(packageName: String): Flow<InstallProgress>
    suspend fun upgradePackage(packageName: String): Flow<InstallProgress>
    suspend fun downgradePackage(packageName: String, version: String): Flow<InstallProgress>
    suspend fun reinstallPackage(packageName: String): Flow<InstallProgress>
    suspend fun batchInstall(request: BatchInstallRequest): Flow<InstallProgress>

    suspend fun installFromWheel(wheelPath: String): Flow<InstallProgress>
    suspend fun installFromArchive(archivePath: String): Flow<InstallProgress>
    suspend fun installFromDirectory(dirPath: String): Flow<InstallProgress>

    suspend fun cancelInstall(taskId: String): Result<Boolean>
    suspend fun pauseInstall(taskId: String): Result<Boolean>
    suspend fun resumeInstall(taskId: String): Result<Boolean>
    suspend fun retryInstall(taskId: String): Flow<InstallProgress>

    suspend fun getInstallQueue(): Flow<List<InstallTask>>
    suspend fun getActiveInstalls(): Flow<Map<String, InstallProgress>>
    suspend fun getCompletedInstalls(): Flow<List<InstallTask>>
    suspend fun getFailedInstalls(): Flow<List<InstallTask>>
    suspend fun getInstallLogs(taskId: String): Flow<List<String>>
    suspend fun clearCompletedInstalls(): Result<Boolean>
    suspend fun clearFailedInstalls(): Result<Boolean>

    suspend fun pipList(): Result<List<InstalledPackage>>
    suspend fun pipFreeze(): Result<String>
    suspend fun pipShow(packageName: String): Result<String>
    suspend fun pipCacheList(): Result<List<String>>
    suspend fun pipCachePurge(): Result<Boolean>
    suspend fun pipCacheDir(): Result<String>
    suspend fun pipCheck(): Result<String>

    suspend fun getOfflinePackages(): Flow<List<OfflinePackage>>
    suspend fun addOfflinePackage(filePath: String): Result<OfflinePackage>
    suspend fun removeOfflinePackage(packageId: String): Result<Boolean>

    suspend fun createBackup(name: String, description: String = ""): Result<PackageBackup>
    suspend fun getBackups(): Flow<List<PackageBackup>>
    suspend fun restoreBackup(backupId: String): Flow<InstallProgress>
    suspend fun deleteBackup(backupId: String): Result<Boolean>
    suspend fun exportBackup(backupId: String, exportPath: String): Result<String>
    suspend fun importBackup(backupPath: String): Result<PackageBackup>

    suspend fun resolveDependencies(packageName: String): Result<Map<String, List<String>>>
    suspend fun getReverseDependencies(packageName: String): Result<List<String>>
    suspend fun checkDependencyConflicts(packageName: String): Result<List<String>>

    suspend fun getAvailableAbis(): List<String>
    suspend fun getCompatiblePackages(): Flow<List<Package>>
    suspend fun getIncompatiblePackages(): Flow<List<Package>>

    suspend fun exportRequirements(): Result<String>
    suspend fun importRequirements(content: String): Result<Unit>
    suspend fun installFromRequirements(requirementsContent: String): Result<Unit>
    suspend fun freezePackages(): Result<String>
    suspend fun getInstalledPackageVersions(packageName: String): Result<List<String>>
}
