package com.pythonide.data.runtime.repository

import com.pythonide.data.runtime.engine.ExecutionEngine
import com.pythonide.data.runtime.manager.InterpreterManager
import com.pythonide.domain.model.ExecutionResult
import com.pythonide.domain.model.InterpreterConfig
import com.pythonide.domain.model.PythonInterpreter
import com.pythonide.domain.model.REPLHistoryEntry
import com.pythonide.domain.repository.PythonRuntimeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PythonRuntimeRepositoryImpl @Inject constructor(
    private val interpreterManager: InterpreterManager,
    private val executionEngine: ExecutionEngine
) : PythonRuntimeRepository {

    override fun getInterpreters(): Flow<List<PythonInterpreter>> {
        return interpreterManager.interpreters.map { sessions ->
            sessions.values.map { it.interpreter }
        }
    }

    override fun getInterpreter(id: String): Flow<PythonInterpreter?> {
        return interpreterManager.interpreters.map { sessions ->
            sessions[id]?.interpreter
        }
    }

    override suspend fun createInterpreter(
        config: InterpreterConfig
    ): PythonInterpreter {
        return interpreterManager.createInterpreter(config)
    }

    override suspend fun destroyInterpreter(id: String) {
        interpreterManager.destroyInterpreter(id)
    }

    override suspend fun executeCode(
        interpreterId: String,
        code: String
    ): ExecutionResult {
        return executionEngine.execute(interpreterId, code)
    }

    override suspend fun executeWithTimeout(
        interpreterId: String,
        code: String,
        timeoutMs: Long
    ): ExecutionResult {
        return executionEngine.execute(interpreterId, code, timeoutMs)
    }

    override suspend fun stopExecution(interpreterId: String) {
        executionEngine.stopExecution(interpreterId)
    }

    override suspend fun restartInterpreter(interpreterId: String) {
        interpreterManager.restartInterpreter(interpreterId)
    }

    override suspend fun clearHistory(interpreterId: String) {
        interpreterManager.clearHistory(interpreterId)
    }

    override suspend fun getHistory(interpreterId: String): List<REPLHistoryEntry> {
        return interpreterManager.getHistory(interpreterId)
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            interpreterManager.initialize()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun installPackages(packages: List<String>): Result<Boolean> {
        // Validate to prevent Python string injection.
        val pkgPattern = Regex("^[A-Za-z0-9_.-]+$")
        if (packages.any { !pkgPattern.matches(it) }) return Result.success(false)
        return try {
            val interpreterId = interpreterManager.createInterpreter().id
            val code = buildString {
                appendLine("import subprocess")
                appendLine("import sys")
                appendLine()
                appendLine("try:")
                for (pkg in packages) {
                    appendLine("    subprocess.check_call([sys.executable, '-m', 'pip', 'install', '$pkg'])")
                }
                appendLine("except Exception as e:")
                appendLine("    print(f'Error installing packages: {e}', file=sys.stderr)")
                appendLine("    sys.exit(1)")
            }

            val result = interpreterManager.executeCode(interpreterId, code)
            interpreterManager.destroyInterpreter(interpreterId)
            Result.success(result.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getInstalledPackages(): Result<List<String>> {
        return try {
            val interpreterId = interpreterManager.createInterpreter().id
            val code = """
import subprocess
import sys

try:
    result = subprocess.run([sys.executable, '-m', 'pip', 'list', '--format=json'], 
                          capture_output=True, text=True)
    if result.returncode == 0:
        import json
        packages = json.loads(result.stdout)
        for pkg in packages:
            print(f"{pkg['name']}=={pkg['version']}")
except Exception as e:
    print(f"Error: {e}", file=sys.stderr)
""".trimIndent()

            val result = interpreterManager.executeCode(interpreterId, code)
            interpreterManager.destroyInterpreter(interpreterId)

            Result.success(
                result.stdout.lines()
                    .filter { it.isNotBlank() }
                    .map { it.trim() }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun cleanup() {
        interpreterManager.cleanup()
        executionEngine.cleanup()
    }
}
