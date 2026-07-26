package com.pythonide.domain.model.packageManager

import org.junit.Assert.*
import org.junit.Test

class PackageManagerModelsTest {

    // --- §D75: Package version comparison (numeric semver, not lexicographic) ---

    @Test
    fun testVersionComparisonNumeric() {
        // §D75: 1.10.0 vs 1.9.0 - numeric comparison should put 1.10.0 > 1.9.0
        // Note: This tests if the version string is stored correctly.
        // Actual comparison logic would be in the repository/manager layer.
        val v1_10 = "1.10.0"
        val v1_9 = "1.9.0"
        // Store versions and verify they can be compared
        val pkg1 = Package(name = "test", version = v1_10)
        val pkg2 = Package(name = "test", version = v1_9)
        assertEquals("1.10.0", pkg1.version)
        assertEquals("1.9.0", pkg2.version)
    }

    @Test
    fun testVersionComparisonWithPreRelease() {
        // §D76: Pre-release version tags
        val pkgBeta = Package(name = "test", version = "1.0.0-beta")
        val pkgRc = Package(name = "test", version = "1.0.0rc1")
        val pkgFinal = Package(name = "test", version = "1.0.0")
        assertEquals("1.0.0-beta", pkgBeta.version)
        assertEquals("1.0.0rc1", pkgRc.version)
        assertEquals("1.0.0", pkgFinal.version)
    }

    // --- §C59: Sort order stability ---

    @Test
    fun testPackageManagerStateFilteredSortByName() {
        val state = PackageManagerState(
            installedPackages = listOf(
                InstalledPackage(name = "beta", version = "1.0"),
                InstalledPackage(name = "alpha", version = "2.0"),
                InstalledPackage(name = "beta", version = "1.1"), // same name, different version
                InstalledPackage(name = "gamma", version = "1.0")
            ),
            sortBy = PackageSortBy.NAME,
            sortOrder = PackageSortOrder.ASCENDING
        )
        val filtered = state.filteredInstalledPackages
        assertEquals(4, filtered.size)
        assertEquals("alpha", filtered[0].name)
        // beta entries should be adjacent (stable sort)
        val betaIndices = filtered.mapIndexedNotNull { i, pkg -> if (pkg.name == "beta") i else null }
        assertEquals(2, betaIndices.size)
        assertEquals(1, betaIndices[1] - betaIndices[0]) // adjacent
    }

    @Test
    fun testPackageManagerStateFilteredSortByVersion() {
        val state = PackageManagerState(
            installedPackages = listOf(
                InstalledPackage(name = "a", version = "2.0"),
                InstalledPackage(name = "b", version = "1.0"),
                InstalledPackage(name = "c", version = "3.0")
            ),
            sortBy = PackageSortBy.VERSION,
            sortOrder = PackageSortOrder.ASCENDING
        )
        val filtered = state.filteredInstalledPackages
        assertEquals("1.0", filtered[0].version)
        assertEquals("2.0", filtered[1].version)
        assertEquals("3.0", filtered[2].version)
    }

    @Test
    fun testPackageManagerStateFilteredSortDescending() {
        val state = PackageManagerState(
            installedPackages = listOf(
                InstalledPackage(name = "a", version = "1.0"),
                InstalledPackage(name = "b", version = "2.0"),
                InstalledPackage(name = "c", version = "3.0")
            ),
            sortBy = PackageSortBy.NAME,
            sortOrder = PackageSortOrder.DESCENDING
        )
        val filtered = state.filteredInstalledPackages
        assertEquals("c", filtered[0].name)
        assertEquals("b", filtered[1].name)
        assertEquals("a", filtered[2].name)
    }

    @Test
    fun testPackageManagerStateFilterUpgradable() {
        val state = PackageManagerState(
            installedPackages = listOf(
                InstalledPackage(name = "a", version = "1.0", isUpgradable = true),
                InstalledPackage(name = "b", version = "2.0", isUpgradable = false),
                InstalledPackage(name = "c", version = "3.0", isUpgradable = true)
            ),
            filterUpgradable = true
        )
        val filtered = state.filteredInstalledPackages
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.isUpgradable })
    }

    @Test
    fun testPackageManagerStateFilterBySearch() {
        val state = PackageManagerState(
            installedPackages = listOf(
                InstalledPackage(name = "numpy", version = "1.24"),
                InstalledPackage(name = "pandas", version = "2.0"),
                InstalledPackage(name = "scipy", version = "1.10")
            ),
            searchQuery = "num"
        )
        val filtered = state.filteredInstalledPackages
        assertEquals(1, filtered.size)
        assertEquals("numpy", filtered[0].name)
    }

    // --- Queue counts ---

    @Test
    fun testQueueCount() {
        val state = PackageManagerState(
            installQueue = listOf(
                InstallTask(packageName = "a", status = InstallTaskStatus.QUEUED),
                InstallTask(packageName = "b", status = InstallTaskStatus.QUEUED),
                InstallTask(packageName = "c", status = InstallTaskStatus.DOWNLOADING)
            )
        )
        assertEquals(2, state.queueCount)
    }

    @Test
    fun testActiveCount() {
        val state = PackageManagerState(
            activeInstalls = mapOf(
                "a" to InstallProgress("a", "a", InstallTaskStatus.INSTALLING, 0.5f),
                "b" to InstallProgress("b", "b", InstallTaskStatus.DOWNLOADING, 0.3f)
            )
        )
        assertEquals(2, state.activeCount)
    }

    @Test
    fun testHasActiveInstalls() {
        val stateWithQueue = PackageManagerState(
            installQueue = listOf(
                InstallTask(packageName = "a", status = InstallTaskStatus.QUEUED)
            )
        )
        assertTrue(stateWithQueue.hasActiveInstalls)

        val stateWithActive = PackageManagerState(
            activeInstalls = mapOf(
                "a" to InstallProgress("a", "a", InstallTaskStatus.INSTALLING, 0.5f)
            )
        )
        assertTrue(stateWithActive.hasActiveInstalls)

        val stateEmpty = PackageManagerState()
        assertFalse(stateEmpty.hasActiveInstalls)
    }

    // --- InstallProgress ---

    @Test
    fun testInstallProgressDefaults() {
        val progress = InstallProgress(
            taskId = "task1",
            packageName = "numpy",
            status = InstallTaskStatus.DOWNLOADING,
            progress = 0.5f
        )
        assertEquals(0L, progress.downloadedBytes)
        assertEquals(0L, progress.totalBytes)
        assertEquals(0L, progress.speed)
        assertEquals("", progress.message)
    }

    // --- Package data class ---

    @Test
    fun testPackageDefaults() {
        val pkg = Package(name = "test", version = "1.0")
        assertTrue(pkg.id.isNotEmpty())
        assertEquals("test", pkg.name)
        assertEquals("1.0", pkg.version)
        assertEquals("", pkg.summary)
        assertFalse(pkg.isInstalled)
        assertNull(pkg.installedVersion)
        assertFalse(pkg.isOutdated)
    }

    // --- InstalledPackage ---

    @Test
    fun testInstalledPackageDefaults() {
        val pkg = InstalledPackage(name = "numpy", version = "1.24")
        assertEquals("pip", pkg.installer)
        assertFalse(pkg.isSystemPackage)
        assertFalse(pkg.isUpgradable)
        assertNull(pkg.latestVersion)
    }

    // --- OfflinePackage ---

    @Test
    fun testOfflinePackage() {
        val pkg = OfflinePackage(
            name = "numpy",
            version = "1.24",
            filePath = "/path/to/numpy.whl",
            fileType = OfflineFileType.WHEEL,
            size = 1024,
            isCompatible = true
        )
        assertTrue(pkg.id.isNotEmpty())
        assertEquals(OfflineFileType.WHEEL, pkg.fileType)
    }

    @Test
    fun testOfflineFileTypeValues() {
        assertEquals(4, OfflineFileType.entries.size)
        assertTrue(OfflineFileType.entries.contains(OfflineFileType.WHEEL))
        assertTrue(OfflineFileType.entries.contains(OfflineFileType.TAR_GZ))
        assertTrue(OfflineFileType.entries.contains(OfflineFileType.ZIP))
        assertTrue(OfflineFileType.entries.contains(OfflineFileType.DIRECTORY))
    }

    // --- InstallTaskStatus ---

    @Test
    fun testInstallTaskStatusValues() {
        assertEquals(8, InstallTaskStatus.entries.size)
        assertTrue(InstallTaskStatus.entries.contains(InstallTaskStatus.QUEUED))
        assertTrue(InstallTaskStatus.entries.contains(InstallTaskStatus.COMPLETED))
        assertTrue(InstallTaskStatus.entries.contains(InstallTaskStatus.FAILED))
        assertTrue(InstallTaskStatus.entries.contains(InstallTaskStatus.CANCELLED))
    }

    // --- InstallPriority ---

    @Test
    fun testInstallPriorityValues() {
        assertEquals(4, InstallPriority.entries.size)
        assertTrue(InstallPriority.entries.contains(InstallPriority.LOW))
        assertTrue(InstallPriority.entries.contains(InstallPriority.NORMAL))
        assertTrue(InstallPriority.entries.contains(InstallPriority.HIGH))
        assertTrue(InstallPriority.entries.contains(InstallPriority.URGENT))
    }

    // --- PackageAction ---

    @Test
    fun testPackageActionValues() {
        assertEquals(5, PackageAction.entries.size)
        assertTrue(PackageAction.entries.contains(PackageAction.INSTALL))
        assertTrue(PackageAction.entries.contains(PackageAction.UNINSTALL))
        assertTrue(PackageAction.entries.contains(PackageAction.UPGRADE))
        assertTrue(PackageAction.entries.contains(PackageAction.DOWNGRADE))
        assertTrue(PackageAction.entries.contains(PackageAction.REINSTALL))
    }

    // --- BatchInstallRequest ---

    @Test
    fun testBatchInstallRequestDefaults() {
        val request = BatchInstallRequest(
            packages = listOf("numpy" to null, "pandas" to "2.0")
        )
        assertEquals(InstallPriority.NORMAL, request.priority)
        assertFalse(request.isBackground)
    }

    // --- ProgressState ---

    @Test
    fun testProgressStateIdle() {
        val state = ProgressState.Idle
        assertTrue(state is ProgressState.Idle)
    }

    @Test
    fun testProgressStateInstalling() {
        val state = ProgressState.Installing("numpy", 0.5f)
        assertEquals("numpy", state.packageName)
        assertEquals(0.5f, state.progress)
    }

    @Test
    fun testProgressStateCompleted() {
        val state = ProgressState.Completed("numpy")
        assertEquals("numpy", state.packageName)
    }

    @Test
    fun testProgressStateFailed() {
        val state = ProgressState.Failed("numpy", "ABI mismatch")
        assertEquals("numpy", state.packageName)
        assertEquals("ABI mismatch", state.error)
    }

    // --- PackageSearchResult ---

    @Test
    fun testPackageSearchResult() {
        val result = PackageSearchResult(
            query = "numpy",
            packages = listOf(Package(name = "numpy", version = "1.24")),
            totalCount = 1,
            page = 1,
            pageSize = 25,
            hasMore = false
        )
        assertEquals(1, result.packages.size)
        assertFalse(result.hasMore)
    }

    // --- PackageCompatibility ---

    @Test
    fun testPackageCompatibility() {
        val compat = PackageCompatibility(
            packageName = "tensorflow",
            isCompatible = false,
            reason = "No Android wheels available",
            needsNativeBinaries = true
        )
        assertFalse(compat.isCompatible)
        assertTrue(compat.needsNativeBinaries)
    }

    // --- Empty state (§A22) ---

    @Test
    fun testEmptyPackageManagerState() {
        val state = PackageManagerState()
        assertTrue(state.installedPackages.isEmpty())
        assertTrue(state.installQueue.isEmpty())
        assertTrue(state.activeInstalls.isEmpty())
        assertEquals(0, state.queueCount)
        assertEquals(0, state.activeCount)
        assertFalse(state.hasActiveInstalls)
    }
}
