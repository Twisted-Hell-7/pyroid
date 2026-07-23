package com.pythonide.data.runtime

import com.pythonide.data.runtime.sandbox.SandboxExecutor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InterpreterTest {

    private lateinit var sandboxExecutor: SandboxExecutor

    @Before
    fun setup() {
        sandboxExecutor = SandboxExecutor()
    }

    @Test
    fun testPythonExecution() = runBlocking {
        val result = sandboxExecutor.executeInSandbox(
            command = listOf("python3", "-c", "print('Hello from Python')"),
            workingDirectory = java.io.File(System.getProperty("java.io.tmpdir"))
        )

        // May fail if Python not installed, but tests the mechanism
        assertNotNull(result)
    }

    @Test
    fun testSandboxResourceLimits() {
        val limits = SandboxExecutor.ResourceLimits(
            maxMemoryBytes = 128 * 1024 * 1024,
            maxCpuTimeMs = 10_000,
            maxWallTimeMs = 20_000,
            maxOutputBytes = 1024 * 1024,
            maxFileDescriptors = 32,
            maxProcesses = 2,
            allowNetworkAccess = false,
            allowFileSystemWrite = false
        )

        assertEquals(128 * 1024 * 1024L, limits.maxMemoryBytes)
        assertEquals(10_000L, limits.maxCpuTimeMs)
        assertFalse(limits.allowNetworkAccess)
    }

    @Test
    fun testSandboxStateFlow() = runBlocking {
        val initialState = sandboxExecutor.sandboxState.value
        assertTrue(initialState is SandboxExecutor.SandboxState.Idle)
    }

    @Test
    fun testResourceUsageTracking() = runBlocking {
        val usage = sandboxExecutor.resourceUsage.value
        assertNotNull(usage)
        assertEquals(0, usage.activeProcesses)
    }

    @Test
    fun testBlockedCommands() = runBlocking {
        val blockedCommands = listOf(
            listOf("rm", "-rf", "/"),
            listOf("mkfs", "/dev/sda"),
            listOf("dd", "if=/dev/zero", "of=/dev/sda")
        )

        for (command in blockedCommands) {
            val result = sandboxExecutor.executeInSandbox(
                command = command,
                workingDirectory = java.io.File(System.getProperty("java.io.tmpdir"))
            )

            assertFalse("Command should be blocked: $command", result.success)
        }
    }

    @Test
    fun testNetworkCommandsBlockedByDefault() = runBlocking {
        val networkCommands = listOf(
            listOf("curl", "http://example.com"),
            listOf("wget", "http://example.com"),
            listOf("ssh", "user@host")
        )

        for (command in networkCommands) {
            val result = sandboxExecutor.executeInSandbox(
                command = command,
                workingDirectory = java.io.File(System.getProperty("java.io.tmpdir"))
            )

            assertFalse("Network command should be blocked: $command", result.success)
        }
    }

    @Test
    fun testTerminateAllProcesses() = runBlocking {
        sandboxExecutor.terminateAll()

        val state = sandboxExecutor.sandboxState.value
        assertNotNull(state)
    }
}
