package com.pythonide.data.runtime.manager

import com.pythonide.data.runtime.core.PythonNativeBridge
import com.pythonide.domain.model.ExecutionResult
import com.pythonide.domain.model.InterpreterConfig
import com.pythonide.domain.model.InterpreterState
import com.pythonide.domain.model.PythonInterpreter
import com.pythonide.domain.model.REPLHistoryEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InterpreterManager @Inject constructor(
    private val nativeBridge: PythonNativeBridge
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()

    private val _interpreters = MutableStateFlow<Map<String, InterpreterSession>>(emptyMap())
    val interpreters: StateFlow<Map<String, InterpreterSession>> = _interpreters.asStateFlow()

    private val activeJobs = ConcurrentHashMap<String, Job>()

    data class InterpreterSession(
        val interpreter: PythonInterpreter,
        val config: InterpreterConfig,
        val history: MutableList<REPLHistoryEntry> = mutableListOf(),
        val variables: MutableMap<String, Any?> = mutableMapOf()
    )

    suspend fun initialize(): Boolean {
        return nativeBridge.initialize()
    }

    suspend fun createInterpreter(
        config: InterpreterConfig = InterpreterConfig()
    ): PythonInterpreter = mutex.withLock {
        val interpreter = PythonInterpreter(
            name = "Python ${_interpreters.value.size + 1}",
            config = config
        )

        val session = InterpreterSession(
            interpreter = interpreter,
            config = config
        )

        _interpreters.update { current ->
            current + (interpreter.id to session)
        }

        interpreter
    }

    suspend fun destroyInterpreter(id: String) = mutex.withLock {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)

        _interpreters.update { current ->
            current - id
        }

        if (_interpreters.value.isEmpty()) {
            nativeBridge.cleanup()
        }
    }

    suspend fun executeCode(
        interpreterId: String,
        code: String
    ): ExecutionResult {
        val session = _interpreters.value[interpreterId]
            ?: return ExecutionResult(
                code = code,
                stderr = "Interpreter not found",
                exitCode = 1
            )

        updateInterpreterState(interpreterId, InterpreterState.RUNNING)

        return try {
            val result = nativeBridge.executeCode(
                code = code,
                config = session.config,
                onStdout = { output ->
                    // Real-time output handling can be added here
                },
                onStderr = { error ->
                    // Real-time error handling can be added here
                }
            )

            val historyEntry = REPLHistoryEntry(
                input = code,
                output = result.stdout,
                isError = !result.isSuccessful
            )

            _interpreters.update { current ->
                current[interpreterId]?.let { sess ->
                    val updatedHistory = sess.history.toMutableList()
                    updatedHistory.add(historyEntry)
                    current + (interpreterId to sess.copy(
                        history = updatedHistory,
                        interpreter = sess.interpreter.copy(
                            lastActiveAt = System.currentTimeMillis()
                        )
                    ))
                } ?: current
            }

            updateInterpreterState(
                interpreterId,
                if (result.isSuccessful) InterpreterState.IDLE else InterpreterState.ERROR
            )

            result
        } catch (e: Exception) {
            updateInterpreterState(interpreterId, InterpreterState.ERROR)
            ExecutionResult(
                code = code,
                stderr = e.message ?: "Unknown error",
                exitCode = 1
            )
        }
    }

    suspend fun executeWithTimeout(
        interpreterId: String,
        code: String,
        timeoutMs: Long
    ): ExecutionResult {
        val session = _interpreters.value[interpreterId]
            ?: return ExecutionResult(
                code = code,
                stderr = "Interpreter not found",
                exitCode = 1
            )

        val config = session.config.copy(maxExecutionTimeMs = timeoutMs)
        val tempSession = session.copy(config = config)

        _interpreters.update { current ->
            current + (interpreterId to tempSession)
        }

        return executeCode(interpreterId, code)
    }

    suspend fun executeBackground(
        interpreterId: String,
        code: String,
        onComplete: ((ExecutionResult) -> Unit)? = null
    ) {
        val job = scope.launch {
            val result = executeCode(interpreterId, code)
            onComplete?.invoke(result)
        }
        activeJobs[interpreterId] = job
    }

    fun stopExecution(interpreterId: String) {
        activeJobs[interpreterId]?.cancel()
        activeJobs.remove(interpreterId)
        nativeBridge.stopExecution()
        scope.launch {
            updateInterpreterState(interpreterId, InterpreterState.STOPPED)
        }
    }

    suspend fun restartInterpreter(interpreterId: String) = mutex.withLock {
        stopExecution(interpreterId)

        val session = _interpreters.value[interpreterId] ?: return@withLock

        updateInterpreterState(interpreterId, InterpreterState.RESTARTING)

        val newSession = session.copy(
            history = mutableListOf(),
            variables = mutableMapOf(),
            interpreter = session.interpreter.copy(
                state = InterpreterState.IDLE,
                created_at = System.currentTimeMillis(),
                lastActiveAt = System.currentTimeMillis()
            )
        )

        _interpreters.update { current ->
            current + (interpreterId to newSession)
        }
    }

    suspend fun clearHistory(interpreterId: String) = mutex.withLock {
        _interpreters.update { current ->
            current[interpreterId]?.let { session ->
                current + (interpreterId to session.copy(history = mutableListOf()))
            } ?: current
        }
    }

    fun getHistory(interpreterId: String): List<REPLHistoryEntry> {
        return _interpreters.value[interpreterId]?.history ?: emptyList()
    }

    fun getInterpreter(id: String): PythonInterpreter? {
        return _interpreters.value[id]?.interpreter
    }

    fun getAllInterpreters(): List<PythonInterpreter> {
        return _interpreters.value.values.map { it.interpreter }
    }

    private suspend fun updateInterpreterState(id: String, state: InterpreterState) {
        _interpreters.update { current ->
            current[id]?.let { session ->
                current + (id to session.copy(
                    interpreter = session.interpreter.copy(state = state)
                ))
            } ?: current
        }
    }

    fun cleanup() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        _interpreters.value.keys.forEach { id ->
            nativeBridge.stopExecution()
        }
        nativeBridge.cleanup()
    }
}
