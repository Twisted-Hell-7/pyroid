package com.pythonide.domain.repository

import com.pythonide.domain.model.ExecutionResult
import com.pythonide.domain.model.InterpreterConfig
import com.pythonide.domain.model.PythonInterpreter
import kotlinx.coroutines.flow.Flow

interface PythonRuntimeRepository {
    fun getInterpreters(): Flow<List<PythonInterpreter>>
    fun getInterpreter(id: String): Flow<PythonInterpreter?>
    suspend fun createInterpreter(config: InterpreterConfig = InterpreterConfig()): PythonInterpreter
    suspend fun destroyInterpreter(id: String)
    suspend fun executeCode(interpreterId: String, code: String): ExecutionResult
    suspend fun executeWithTimeout(interpreterId: String, code: String, timeoutMs: Long): ExecutionResult
    suspend fun stopExecution(interpreterId: String)
    suspend fun restartInterpreter(interpreterId: String)
    suspend fun clearHistory(interpreterId: String)
    suspend fun getHistory(interpreterId: String): List<com.pythonide.domain.model.REPLHistoryEntry>
    suspend fun isAvailable(): Boolean
    suspend fun installPackages(packages: List<String>): Boolean
    suspend fun getInstalledPackages(): List<String>
}
