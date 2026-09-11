package com.pythonide.app.screens.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pythonide.data.terminal.AnsiParser
import com.pythonide.domain.model.ExecutionResult
import com.pythonide.domain.model.InterpreterState
import com.pythonide.domain.model.PythonInterpreter
import com.pythonide.domain.model.terminal.Terminal
import com.pythonide.domain.model.terminal.TerminalConfig
import com.pythonide.domain.model.terminal.TerminalEntry
import com.pythonide.domain.model.terminal.TerminalEntryType
import com.pythonide.domain.model.terminal.TerminalState
import com.pythonide.domain.repository.PythonRuntimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private val terminalLogTimeFormat = ThreadLocal.withInitial {
    SimpleDateFormat("HH:mm:ss", Locale.getDefault())
}

@HiltViewModel
class TerminalViewModel @Inject constructor(
    application: Application,
    private val runtimeRepository: PythonRuntimeRepository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(TerminalState())
    val state: StateFlow<TerminalState> = _state.asStateFlow()

    private val _currentInterpreter = MutableStateFlow<PythonInterpreter?>(null)
    val currentInterpreter: StateFlow<PythonInterpreter?> = _currentInterpreter.asStateFlow()

    private val commandHistoryList = mutableListOf<String>()
    private var historyNavigationIndex = -1

    init {
        initializeInterpreter()
    }

    private fun initializeInterpreter() {
        viewModelScope.launch {
            try {
                val interpreter = runtimeRepository.createInterpreter()
                _currentInterpreter.value = interpreter
                addSystemEntry(content = "Python ${interpreter.pythonVersion} • ${interpreter.state.name}")
                addSystemEntry(content = "Type 'help()' for help, 'exit()' to exit")
                addSystemEntry(content = "---")
            } catch (e: Exception) {
                addErrorEntry(content = "Failed to initialize interpreter: ${e.message}")
            }
        }
    }

    fun updateInput(input: String) {
        _state.update { it.copy(currentInput = input) }
    }

    fun executeCommand() {
        val input = _state.value.currentInput.trim()
        if (input.isEmpty()) return

        val terminalId = _state.value.activeTerminalId
        val interpreterId = _currentInterpreter.value?.id ?: return

        addInputEntry(terminalId, input)

        if (input.isNotBlank()) {
            commandHistoryList.add(input)
            historyNavigationIndex = commandHistoryList.size
        }

        _state.update { it.copy(currentInput = "", historyIndex = -1) }

        viewModelScope.launch {
            _state.update { it.copy(isExecuting = true) }

            try {
                val result = withContext(Dispatchers.IO) {
                    runtimeRepository.executeCode(interpreterId, input)
                }

                if (result.stdout.isNotBlank()) {
                    addOutputEntry(terminalId, result.stdout)
                }

                if (result.stderr.isNotBlank()) {
                    addErrorEntry(terminalId, result.stderr)
                }

                if (!result.isSuccessful && result.stdout.isBlank() && result.stderr.isBlank()) {
                    addErrorEntry(terminalId, result.exception?.message ?: "Execution failed")
                }
            } catch (e: Exception) {
                addErrorEntry(terminalId, "Error: ${e.message}")
            } finally {
                _state.update { it.copy(isExecuting = false) }
            }
        }
    }

    fun navigateHistoryUp() {
        if (commandHistoryList.isEmpty()) return

        val newIndex = if (historyNavigationIndex > 0) {
            historyNavigationIndex - 1
        } else {
            0
        }

        historyNavigationIndex = newIndex
        val command = commandHistoryList[newIndex]
        _state.update { it.copy(currentInput = command, historyIndex = newIndex) }
    }

    fun navigateHistoryDown() {
        if (commandHistoryList.isEmpty()) return

        val newIndex = if (historyNavigationIndex < commandHistoryList.size - 1) {
            historyNavigationIndex + 1
        } else {
            historyNavigationIndex = commandHistoryList.size
            _state.update { it.copy(currentInput = "", historyIndex = -1) }
            return
        }

        historyNavigationIndex = newIndex
        val command = commandHistoryList[newIndex]
        _state.update { it.copy(currentInput = command, historyIndex = newIndex) }
    }

    fun clearLine() {
        _state.update { it.copy(currentInput = "", historyIndex = -1) }
        historyNavigationIndex = commandHistoryList.size
    }

    fun createNewTerminal() {
        val newTerminal = Terminal()
        _state.update { state ->
            state.copy(
                terminals = state.terminals + newTerminal,
                activeTerminalId = newTerminal.id,
                entries = state.entries + (newTerminal.id to emptyList())
            )
        }
        addSystemEntry(content = "New terminal created")
    }

    fun switchTerminal(terminalId: String) {
        _state.update { it.copy(activeTerminalId = terminalId) }
    }

    fun closeTerminal(terminalId: String) {
        val currentState = _state.value
        if (currentState.terminals.size <= 1) return

        val newTerminals = currentState.terminals.filter { it.id != terminalId }
        val newActiveId = if (currentState.activeTerminalId == terminalId) {
            newTerminals.first().id
        } else {
            currentState.activeTerminalId
        }

        _state.update { state ->
            state.copy(
                terminals = newTerminals,
                activeTerminalId = newActiveId,
                entries = state.entries - terminalId
            )
        }
    }

    fun clearTerminal() {
        val terminalId = _state.value.activeTerminalId
        _state.update { state ->
            state.copy(entries = state.entries + (terminalId to emptyList()))
        }
    }

    fun stopExecution() {
        val interpreterId = _currentInterpreter.value?.id ?: return
        viewModelScope.launch {
            runtimeRepository.stopExecution(interpreterId)
        }
        _state.update { it.copy(isExecuting = false) }
        addSystemEntry(content = "Execution stopped")
    }

    fun restartInterpreter() {
        viewModelScope.launch {
            val interpreterId = _currentInterpreter.value?.id ?: return@launch

            try {
                runtimeRepository.restartInterpreter(interpreterId)
                clearTerminal()
                addSystemEntry(content = "Interpreter restarted")
                initializeInterpreter()
            } catch (e: Exception) {
                addErrorEntry(content = "Failed to restart interpreter: ${e.message}")
            }
        }
    }

    fun copyLogsToClipboard() {
        val terminalId = _state.value.activeTerminalId
        val entries = _state.value.entries[terminalId] ?: return

        val text = entries.joinToString("\n") { entry ->
            val timestamp = formatTimestamp(entry.timestamp)
            when (entry.type) {
                TerminalEntryType.INPUT -> "[$timestamp] >>> ${entry.content}"
                TerminalEntryType.OUTPUT -> "[$timestamp] ${entry.content}"
                TerminalEntryType.ERROR -> "[$timestamp] ERROR: ${entry.content}"
                TerminalEntryType.SYSTEM -> "[$timestamp] [SYSTEM] ${entry.content}"
                TerminalEntryType.TIMESTAMP -> entry.content
            }
        }

        val clipboard = getApplication<Application>().getSystemService(Application.CLIPBOARD_SERVICE)
            as? android.content.ClipboardManager ?: run {
            addSystemEntry(content = "Clipboard not available")
            return
        }
        val clip = android.content.ClipData.newPlainText("Terminal Logs", text)
        clipboard.setPrimaryClip(clip)

        addSystemEntry(content = "Logs copied to clipboard")
    }

    fun saveLogsToFile() {
        viewModelScope.launch {
            val terminalId = _state.value.activeTerminalId
            val entries = _state.value.entries[terminalId] ?: return@launch

            val text = entries.joinToString("\n") { entry ->
                val timestamp = formatTimestamp(entry.timestamp)
                when (entry.type) {
                    TerminalEntryType.INPUT -> "[$timestamp] >>> ${entry.content}"
                    TerminalEntryType.OUTPUT -> "[$timestamp] ${entry.content}"
                    TerminalEntryType.ERROR -> "[$timestamp] ERROR: ${entry.content}"
                    TerminalEntryType.SYSTEM -> "[$timestamp] [SYSTEM] ${entry.content}"
                    TerminalEntryType.TIMESTAMP -> entry.content
                }
            }

            try {
                val dir = File(getApplication<Application>().filesDir, "terminal_logs")
                dir.mkdirs()
                val fileName = "terminal_${System.currentTimeMillis()}.log"
                val file = File(dir, fileName)
                file.writeText(text)
                addSystemEntry(content = "Logs saved to: ${file.absolutePath}")
            } catch (e: Exception) {
                addErrorEntry(content = "Failed to save logs: ${e.message}")
            }
        }
    }

    fun updateConfig(config: TerminalConfig) {
        _state.update { it.copy(config = config) }
    }

    private fun addInputEntry(terminalId: String, content: String) {
        val entry = TerminalEntry(
            terminalId = terminalId,
            type = TerminalEntryType.INPUT,
            content = content
        )
        addEntry(terminalId, entry)
    }

    private fun addOutputEntry(terminalId: String, content: String) {
        val segments = if (_state.value.config.enableColors) {
            AnsiParser.parse(content)
        } else {
            listOf(com.pythonide.domain.model.terminal.AnsiSegment(text = AnsiParser.stripAnsi(content)))
        }

        val entry = TerminalEntry(
            terminalId = terminalId,
            type = TerminalEntryType.OUTPUT,
            content = AnsiParser.stripAnsi(content),
            ansiFormatted = segments
        )
        addEntry(terminalId, entry)
    }

    private fun addErrorEntry(terminalId: String = _state.value.activeTerminalId, content: String) {
        val segments = if (_state.value.config.enableColors) {
            AnsiParser.parse(content)
        } else {
            listOf(com.pythonide.domain.model.terminal.AnsiSegment(text = AnsiParser.stripAnsi(content)))
        }

        val entry = TerminalEntry(
            terminalId = terminalId,
            type = TerminalEntryType.ERROR,
            content = AnsiParser.stripAnsi(content),
            ansiFormatted = segments
        )
        addEntry(terminalId, entry)
    }

    private fun addSystemEntry(terminalId: String = _state.value.activeTerminalId, content: String) {
        val entry = TerminalEntry(
            terminalId = terminalId,
            type = TerminalEntryType.SYSTEM,
            content = content
        )
        addEntry(terminalId, entry)
    }

    private fun addEntry(terminalId: String, entry: TerminalEntry) {
        _state.update { state ->
            val currentEntries = state.entries[terminalId] ?: emptyList()
            val newEntries = currentEntries + entry
            val trimmedEntries = if (newEntries.size > state.config.maxLines) {
                newEntries.takeLast(state.config.maxLines)
            } else {
                newEntries
            }
            state.copy(entries = state.entries + (terminalId to trimmedEntries))
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return terminalLogTimeFormat.get()!!.format(Date(timestamp))
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            _currentInterpreter.value?.id?.let { id ->
                runtimeRepository.destroyInterpreter(id)
            }
        }
    }
}
