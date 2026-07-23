package com.pythonide.app.screens.debugger

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pythonide.domain.model.debugger.Breakpoint
import com.pythonide.domain.model.debugger.DebugEvent
import com.pythonide.domain.model.debugger.DebugState
import com.pythonide.domain.model.debugger.LogEntry
import com.pythonide.domain.model.debugger.RuntimeInfo
import com.pythonide.domain.model.debugger.StackFrame
import com.pythonide.domain.model.debugger.Variable
import com.pythonide.domain.model.debugger.WatchExpression
import com.pythonide.domain.repository.DebuggerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DebuggerViewModel @Inject constructor(
    application: Application,
    private val debuggerRepository: DebuggerRepository
) : AndroidViewModel(application) {

    private val _debugState = MutableStateFlow(DebugState.IDLE)
    val debugState: StateFlow<DebugState> = _debugState.asStateFlow()

    private val _callStack = MutableStateFlow<List<StackFrame>>(emptyList())
    val callStack: StateFlow<List<StackFrame>> = _callStack.asStateFlow()

    private val _variables = MutableStateFlow<List<Variable>>(emptyList())
    val variables: StateFlow<List<Variable>> = _variables.asStateFlow()

    private val _watchExpressions = MutableStateFlow<List<WatchExpression>>(emptyList())
    val watchExpressions: StateFlow<List<WatchExpression>> = _watchExpressions.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _runtimeInfo = MutableStateFlow(RuntimeInfo())
    val runtimeInfo: StateFlow<RuntimeInfo> = _runtimeInfo.asStateFlow()

    private val _breakpoints = MutableStateFlow<List<Breakpoint>>(emptyList())
    val breakpoints: StateFlow<List<Breakpoint>> = _breakpoints.asStateFlow()

    private val _currentFrame = MutableStateFlow<StackFrame?>(null)
    val currentFrame: StateFlow<StackFrame?> = _currentFrame.asStateFlow()

    private val _expandedVariables = MutableStateFlow<Set<String>>(emptySet())
    val expandedVariables: StateFlow<Set<String>> = _expandedVariables.asStateFlow()

    private val _errorMessages = MutableStateFlow<List<String>>(emptyList())
    val errorMessages: StateFlow<List<String>> = _errorMessages.asStateFlow()

    private val _warningMessages = MutableStateFlow<List<String>>(emptyList())
    val warningMessages: StateFlow<List<String>> = _warningMessages.asStateFlow()

    private val _activePanel = MutableStateFlow(DebugPanel.VARIABLES)
    val activePanel: StateFlow<DebugPanel> = _activePanel.asStateFlow()

    private val _isPanelVisible = MutableStateFlow(false)
    val isPanelVisible: StateFlow<Boolean> = _isPanelVisible.asStateFlow()

    init {
        viewModelScope.launch {
            debuggerRepository.observeDebugState().collect { state ->
                _debugState.value = state
            }
        }
        viewModelScope.launch {
            debuggerRepository.observeCallStack().collect { stack ->
                _callStack.value = stack
            }
        }
        viewModelScope.launch {
            debuggerRepository.observeVariables().collect { vars ->
                _variables.value = vars
            }
        }
        viewModelScope.launch {
            debuggerRepository.observeWatchExpressions().collect { watches ->
                _watchExpressions.value = watches
            }
        }
        viewModelScope.launch {
            debuggerRepository.observeLogs().collect { logs ->
                _logs.value = logs
                updateFilteredMessages(logs)
            }
        }
        viewModelScope.launch {
            debuggerRepository.observeRuntimeInfo().collect { info ->
                _runtimeInfo.value = info
            }
        }
        viewModelScope.launch {
            debuggerRepository.observeBreakpoints().collect { bps ->
                _breakpoints.value = bps
            }
        }
        viewModelScope.launch {
            debuggerRepository.observeEvents().collect { event ->
                handleEvent(event)
            }
        }
    }

    fun startDebugging(filePath: String, code: String) {
        viewModelScope.launch {
            _isPanelVisible.value = true
            debuggerRepository.startDebugging(
                filePath = filePath,
                code = code,
                breakpoints = _breakpoints.value
            )
        }
    }

    fun stopDebugging() {
        viewModelScope.launch {
            debuggerRepository.stopDebugging()
            _currentFrame.value = null
            _callStack.value = emptyList()
            _variables.value = emptyList()
        }
    }

    fun continueExecution() {
        viewModelScope.launch {
            debuggerRepository.continueExecution()
        }
    }

    fun stepInto() {
        viewModelScope.launch {
            debuggerRepository.stepInto()
        }
    }

    fun stepOver() {
        viewModelScope.launch {
            debuggerRepository.stepOver()
        }
    }

    fun stepOut() {
        viewModelScope.launch {
            debuggerRepository.stepOut()
        }
    }

    fun addBreakpoint(filePath: String, lineNumber: Int, condition: String? = null) {
        val breakpoint = Breakpoint(
            id = UUID.randomUUID().toString(),
            filePath = filePath,
            lineNumber = lineNumber,
            condition = condition
        )
        viewModelScope.launch {
            debuggerRepository.addBreakpoint(breakpoint)
        }
    }

    fun removeBreakpoint(breakpointId: String) {
        viewModelScope.launch {
            debuggerRepository.removeBreakpoint(breakpointId)
        }
    }

    fun toggleBreakpoint(breakpointId: String) {
        viewModelScope.launch {
            debuggerRepository.toggleBreakpoint(breakpointId)
        }
    }

    fun updateBreakpointCondition(breakpointId: String, condition: String?) {
        viewModelScope.launch {
            debuggerRepository.updateBreakpointCondition(breakpointId, condition)
        }
    }

    fun addWatchExpression(expression: String) {
        viewModelScope.launch {
            debuggerRepository.addWatchExpression(expression)
        }
    }

    fun removeWatchExpression(expressionId: String) {
        viewModelScope.launch {
            debuggerRepository.removeWatchExpression(expressionId)
        }
    }

    fun evaluateExpression(expression: String) {
        viewModelScope.launch {
            debuggerRepository.evaluateExpression(expression)
        }
    }

    fun inspectVariable(name: String) {
        viewModelScope.launch {
            debuggerRepository.inspectVariable(name)
        }
    }

    fun toggleVariableExpansion(name: String) {
        _expandedVariables.update { current ->
            if (name in current) current - name else current + name
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            debuggerRepository.clearLogs()
            _errorMessages.value = emptyList()
            _warningMessages.value = emptyList()
        }
    }

    fun setActivePanel(panel: DebugPanel) {
        _activePanel.value = panel
    }

    fun togglePanel() {
        _isPanelVisible.update { !it }
    }

    fun showPanel() {
        _isPanelVisible.value = true
    }

    fun hidePanel() {
        _isPanelVisible.value = false
    }

    fun getBreakpointAtLine(filePath: String, lineNumber: Int): Breakpoint? {
        return _breakpoints.value.find {
            it.filePath == filePath && it.lineNumber == lineNumber
        }
    }

    fun toggleBreakpointAtLine(filePath: String, lineNumber: Int) {
        val existing = getBreakpointAtLine(filePath, lineNumber)
        if (existing != null) {
            removeBreakpoint(existing.id)
        } else {
            addBreakpoint(filePath, lineNumber)
        }
    }

    private fun handleEvent(event: DebugEvent) {
        when (event) {
            is DebugEvent.ExceptionRaised -> {
                _errorMessages.update { current ->
                    current + "${event.type}: ${event.message}"
                }
                if (_activePanel.value != DebugPanel.ERRORS) {
                    _activePanel.value = DebugPanel.ERRORS
                }
            }
            is DebugEvent.WarningLogged -> {
                _warningMessages.update { current ->
                    current + event.message
                }
            }
            is DebugEvent.ErrorLogged -> {
                _errorMessages.update { current ->
                    current + event.message
                }
            }
            is DebugEvent.OutputLogged -> {
                // Output is captured in logs via repository
            }
            is DebugEvent.BreakpointHit -> {
                _currentFrame.value = event.frame
                _callStack.value = event.callStack
                if (_activePanel.value == DebugPanel.ERRORS) {
                    _activePanel.value = DebugPanel.VARIABLES
                }
            }
            is DebugEvent.StepComplete -> {
                _currentFrame.value = event.frame
                _callStack.value = event.callStack
            }
            is DebugEvent.ExecutionFinished -> {
                _currentFrame.value = null
            }
            is DebugEvent.RuntimeInfoUpdated -> {
                _runtimeInfo.value = event.info
            }
        }
    }

    private fun updateFilteredMessages(logs: List<LogEntry>) {
        _errorMessages.value = logs
            .filter { it.level == com.pythonide.domain.model.debugger.LogLevel.ERROR }
            .map { it.message }
        _warningMessages.value = logs
            .filter { it.level == com.pythonide.domain.model.debugger.LogLevel.WARNING }
            .map { it.message }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            debuggerRepository.stopDebugging()
        }
    }
}

enum class DebugPanel {
    VARIABLES,
    WATCH,
    CALL_STACK,
    LOGS,
    RUNTIME,
    ERRORS,
    WARNINGS
}
