package com.pythonide.data.runtime.sandbox

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class SandboxExecutorTest {

    private lateinit var sandboxExecutor: SandboxExecutor

    @Before
    fun setup() {
        sandboxExecutor = SandboxExecutor()
    }

    @Test
    fun `test empty command throws SecurityException`() = runBlocking {
        val result = sandboxExecutor.executeInSandbox(
            command = emptyList(),
            workingDirectory = File(System.getProperty("java.io.tmpdir"))
        )

        assertFalse(result.success)
        assertTrue(result.error.isNotEmpty())
    }

    @Test
    fun `test blocked command pattern is rejected`() = runBlocking {
        val result = sandboxExecutor.executeInSandbox(
            command = listOf("rm", "-rf", "/"),
            workingDirectory = File(System.getProperty("java.io.tmpdir"))
        )

        assertFalse(result.success)
        assertTrue(result.error.contains("Blocked command pattern"))
    }

    @Test
    fun `test network command blocked when network access disabled`() = runBlocking {
        val limits = SandboxExecutor.ResourceLimits(allowNetworkAccess = false)

        val result = sandboxExecutor.executeInSandbox(
            command = listOf("curl", "http://example.com"),
            workingDirectory = File(System.getProperty("java.io.tmpdir")),
            limits = limits
        )

        assertFalse(result.success)
        assertTrue(result.error.contains("Network access not allowed"))
    }

    @Test
    fun `test sandbox state transitions to Running`() = runBlocking {
        val job = kotlinx.coroutines.launch {
            sandboxExecutor.executeInSandbox(
                command = listOf("echo", "test"),
                workingDirectory = File(System.getProperty("java.io.tmpdir"))
            )
        }

        kotlinx.coroutines.delay(100)
        val state = sandboxExecutor.sandboxState.value
        assertTrue(state is SandboxExecutor.SandboxState.Running || 
                   state is SandboxExecutor.SandboxState.Terminated)
        job.cancel()
    }

    @Test
    fun `test terminateAll kills all processes`() = runBlocking {
        val job1 = kotlinx.coroutines.launch {
            sandboxExecutor.executeInSandbox(
                command = listOf("sleep", "10"),
                workingDirectory = File(System.getProperty("java.io.tmpdir"))
            )
        }

        val job2 = kotlinx.coroutines.launch {
            sandboxExecutor.executeInSandbox(
                command = listOf("sleep", "10"),
                workingDirectory = File(System.getProperty("java.io.tmpdir"))
            )
        }

        kotlinx.coroutines.delay(100)
        sandboxExecutor.terminateAll()

        job1.cancel()
        job2.cancel()
    }

    @Test
    fun `test ResourceLimits defaults are set correctly`() {
        val limits = SandboxExecutor.ResourceLimits()

        assertEquals(256 * 1024 * 1024L, limits.maxMemoryBytes)
        assertEquals(30_000L, limits.maxCpuTimeMs)
        assertEquals(60_000L, limits.maxWallTimeMs)
        assertEquals(10 * 1024 * 1024L, limits.maxOutputBytes)
        assertFalse(limits.allowNetworkAccess)
        assertFalse(limits.allowFileSystemWrite)
    }

    @Test
    fun `test SandboxResult Timeout has correct termination reason`() {
        val result = SandboxExecutor.SandboxResult.Timeout("test", 5000)

        assertFalse(result.success)
        assertEquals(SandboxExecutor.TerminationReason.TIMEOUT, result.terminationReason)
    }

    @Test
    fun `test SandboxResult SecurityViolation has correct termination reason`() {
        val result = SandboxExecutor.SandboxResult.SecurityViolation("test", "test violation")

        assertFalse(result.success)
        assertEquals(SandboxExecutor.TerminationReason.SECURITY_VIOLATION, result.terminationReason)
    }

    @Test
    fun `test setEnforcement toggles enforcement`() {
        sandboxExecutor.setEnforcement(false)
        sandboxExecutor.setEnforcement(true)
    }
}
