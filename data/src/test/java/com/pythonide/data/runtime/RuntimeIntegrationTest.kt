package com.pythonide.data.runtime

import com.pythonide.data.runtime.sandbox.SandboxExecutor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RuntimeIntegrationTest {

    private lateinit var sandboxExecutor: SandboxExecutor
    private lateinit var crashPrevention: CrashPrevention
    private lateinit var exceptionRecovery: ExceptionRecovery

    @Before
    fun setup() {
        sandboxExecutor = SandboxExecutor()
        crashPrevention = CrashPrevention()
        exceptionRecovery = ExceptionRecovery()
    }

    @Test
    fun testSandboxWithCrashPrevention() = runBlocking {
        crashPrevention.startWatchdog()

        val result = sandboxExecutor.executeInSandbox(
            command = listOf("echo", "integration test"),
            workingDirectory = java.io.File(System.getProperty("java.io.tmpdir"))
        )

        assertTrue(result.success)
        crashPrevention.stopWatchdog()
    }

    @Test
    fun testExceptionRecoveryWithSandbox() = runBlocking {
        var attempts = 0

        val result = exceptionRecovery.executeWithRecovery(
            operation = "sandbox_integration",
            config = ExceptionRecovery.RetryConfig(maxRetries = 3, initialDelayMs = 10)
        ) {
            attempts++
            if (attempts < 2) {
                sandboxExecutor.executeInSandbox(
                    command = listOf("false"),
                    workingDirectory = java.io.File(System.getProperty("java.io.tmpdir"))
                )
            }
            "success"
        }

        assertEquals("success", result)
    }

    @Test
    fun testCrashPreventionMonitorsSandbox() = runBlocking {
        crashPrevention.startWatchdog()

        repeat(5) {
            sandboxExecutor.executeInSandbox(
                command = listOf("echo", "test $it"),
                workingDirectory = java.io.File(System.getProperty("java.io.tmpdir"))
            )
        }

        val stats = crashPrevention.retryStats.value
        assertNotNull(stats)

        crashPrevention.stopWatchdog()
    }

    @Test
    fun testMemoryOptimizerWithExecution() = runBlocking {
        val memoryOptimizer = com.pythonide.data.runtime.MemoryOptimizer(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        )

        memoryOptimizer.startMonitoring(1000)

        val report = memoryOptimizer.getMemoryInfo()
        assertTrue(report.totalMem > 0)

        memoryOptimizer.stopMonitoring()
    }
}
