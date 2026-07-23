package com.pythonide.data.runtime.engine

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import com.pythonide.data.runtime.manager.InterpreterManager
import com.pythonide.domain.model.ExecutionResult
import com.pythonide.domain.model.InterpreterConfig
import com.pythonide.domain.model.InterpreterState
import com.pythonide.domain.model.PythonInterpreter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExecutionEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val interpreterManager: InterpreterManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mutex = Mutex()

    private val _isRunning = AtomicBoolean(false)
    private val _activeExecution = MutableStateFlow<String?>(null)
    val activeExecution: StateFlow<String?> = _activeExecution.asStateFlow()

    private val _executionQueue = ConcurrentLinkedQueue<ExecutionRequest>()

    private val _executionResults = MutableSharedFlow<ExecutionResult>(
        replay = 1,
        extraBufferCapacity = 64
    )
    val executionResults: SharedFlow<ExecutionResult> = _executionResults.asSharedFlow()

    private val _memoryUsage = MutableStateFlow(0L)
    val memoryUsage: StateFlow<Long> = _memoryUsage.asStateFlow()

    private val executionCounter = AtomicLong(0)
    private var monitoringJob: Job? = null

    data class ExecutionRequest(
        val id: String,
        val interpreterId: String,
        val code: String,
        val timeout: Long = 30_000L,
        val priority: Priority = Priority.NORMAL,
        val callback: ((ExecutionResult) -> Unit)? = null
    )

    enum class Priority {
        LOW, NORMAL, HIGH, IMMEDIATE
    }

    init {
        startMemoryMonitoring()
    }

    private fun startMemoryMonitoring() {
        monitoringJob = scope.launch {
            while (true) {
                updateMemoryUsage()
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun updateMemoryUsage() {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val usedMemory = Runtime.getRuntime().let { runtime ->
            runtime.totalMemory() - runtime.freeMemory()
        }

        _memoryUsage.value = usedMemory
    }

    suspend fun execute(
        interpreterId: String,
        code: String,
        timeout: Long = 30_000L,
        priority: Priority = Priority.NORMAL
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val requestId = "exec_${executionCounter.incrementAndGet()}"

        if (_isRunning.get() && priority != Priority.IMMEDIATE) {
            val request = ExecutionRequest(
                id = requestId,
                interpreterId = interpreterId,
                code = code,
                timeout = timeout,
                priority = priority
            )
            _executionQueue.add(request)
            return@withContext ExecutionResult(
                code = code,
                stderr = "Execution queued",
                exitCode = 0
            )
        }

        executeImmediate(interpreterId, code, timeout)
    }

    private suspend fun executeImmediate(
        interpreterId: String,
        code: String,
        timeout: Long
    ): ExecutionResult = mutex.withLock {
        _isRunning.set(true)
        _activeExecution.value = interpreterId

        try {
            checkMemoryPressure()

            val result = withTimeout(timeout) {
                interpreterManager.executeWithTimeout(
                    interpreterId = interpreterId,
                    code = code,
                    timeoutMs = timeout
                )
            }

            _executionResults.emit(result)
            processQueue()

            result
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            interpreterManager.stopExecution(interpreterId)
            val result = ExecutionResult(
                code = code,
                stderr = "Execution timed out",
                exitCode = 124
            )
            _executionResults.emit(result)
            result
        } catch (e: Exception) {
            val result = ExecutionResult(
                code = code,
                stderr = e.message ?: "Unknown error",
                exitCode = 1
            )
            _executionResults.emit(result)
            result
        } finally {
            _isRunning.set(false)
            _activeExecution.value = null
        }
    }

    private fun checkMemoryPressure() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()

        if (usedMemory > maxMemory * 0.8) {
            System.gc()
            Thread.sleep(100)
        }
    }

    private suspend fun processQueue() {
        val nextRequest = _executionQueue.poll() ?: return
        executeImmediate(
            interpreterId = nextRequest.interpreterId,
            code = nextRequest.code,
            timeout = nextRequest.timeout
        )
    }

    suspend fun executeBackground(
        interpreterId: String,
        code: String,
        onComplete: ((ExecutionResult) -> Unit)? = null
    ) {
        scope.launch {
            val result = execute(interpreterId, code)
            onComplete?.invoke(result)
        }
    }

    fun stopAll() {
        interpreterManager.getAllInterpreters().forEach { interpreter ->
            interpreterManager.stopExecution(interpreter.id)
        }
        _executionQueue.clear()
        _isRunning.set(false)
        _activeExecution.value = null
    }

    fun stopExecution(interpreterId: String) {
        interpreterManager.stopExecution(interpreterId)
        if (_activeExecution.value == interpreterId) {
            _isRunning.set(false)
            _activeExecution.value = null
        }
    }

    fun getQueueSize(): Int = _executionQueue.size

    fun hasPendingExecutions(): Boolean = _executionQueue.isNotEmpty()

    fun cleanup() {
        monitoringJob?.cancel()
        stopAll()
        scope.cancel()
    }

    suspend fun executeWithLifecycle(
        interpreterId: String,
        code: String,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner
    ): ExecutionResult {
        return withContext(Dispatchers.IO) {
            val result = execute(interpreterId, code)

            kotlinx.coroutines.withContext(Dispatchers.Main) {
                // Lifecycle-aware callback can be added here
            }

            result
        }
    }
}
