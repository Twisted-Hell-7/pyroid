package com.pythonide.app.screens.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pythonide.data.editor.core.BracketHandler
import com.pythonide.data.editor.core.ClipboardHandler
import com.pythonide.data.editor.core.EditorStateManager
import com.pythonide.data.editor.core.IndentationHandler
import com.pythonide.data.editor.core.SearchHandler
import com.pythonide.data.editor.core.TabManager
import com.pythonide.data.editor.highlight.PythonSyntaxHighlighter
import com.pythonide.data.intellisense.core.IntelliSenseEngine
import com.pythonide.data.intellisense.providers.BuiltinProvider
import com.pythonide.data.intellisense.providers.FunctionProvider
import com.pythonide.data.intellisense.providers.KeywordProvider
import com.pythonide.data.intellisense.providers.Linter
import com.pythonide.data.intellisense.providers.ModuleProvider
import com.pythonide.data.intellisense.providers.SnippetProvider
import com.pythonide.data.intellisense.providers.SyntaxValidator
import com.pythonide.domain.model.editor.CursorInfo
import com.pythonide.domain.model.editor.CursorPosition
import com.pythonide.domain.model.editor.EditorConfig
import com.pythonide.domain.model.editor.EditorState
import com.pythonide.domain.model.editor.EditorTabState
import com.pythonide.domain.model.editor.EditorTheme
import com.pythonide.domain.model.editor.SearchMatch
import com.pythonide.domain.model.editor.SearchState
import com.pythonide.domain.model.editor.SplitMode
import com.pythonide.domain.model.editor.Tab
import com.pythonide.domain.model.intellisense.CompletionItem
import com.pythonide.domain.model.intellisense.Diagnostic
import com.pythonide.domain.model.intellisense.IntelliSenseState
import com.pythonide.domain.model.project.AutoSaveConfig
import com.pythonide.domain.repository.FileRepository
import com.pythonide.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CodeEditorViewModel @Inject constructor(
    application: Application,
    private val fileRepository: FileRepository,
    private val projectRepository: ProjectRepository
) : AndroidViewModel(application) {
    
    private val editorStateManager = EditorStateManager()
    private val indentationHandler = IndentationHandler()
    private val bracketHandler = BracketHandler()
    private val searchHandler = SearchHandler()
    private val clipboardHandler = ClipboardHandler(application)
    private val tabManager = TabManager()
    private val intelliSenseEngine = IntelliSenseEngine()
    
    private var syntaxHighlighter = PythonSyntaxHighlighter()
    
    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()
    
    private val _tabState = MutableStateFlow(EditorTabState())
    val tabState: StateFlow<EditorTabState> = _tabState.asStateFlow()
    
    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()
    
    private val _cursorInfo = MutableStateFlow(CursorInfo(1, 1))
    val cursorInfo: StateFlow<CursorInfo> = _cursorInfo.asStateFlow()
    
    private val _highlightedLines = MutableStateFlow<List<List<Pair<String, androidx.compose.ui.graphics.Color>>>>(emptyList())
    val highlightedLines: StateFlow<List<List<Pair<String, androidx.compose.ui.graphics.Color>>>> = _highlightedLines.asStateFlow()
    
    private val _config = MutableStateFlow(EditorConfig())
    val config: StateFlow<EditorConfig> = _config.asStateFlow()
    
    private val _intelliSenseState = MutableStateFlow(IntelliSenseState())
    val intelliSenseState: StateFlow<IntelliSenseState> = _intelliSenseState.asStateFlow()
    
    private val _isModified = MutableStateFlow(false)
    val isModified: StateFlow<Boolean> = _isModified.asStateFlow()
    
    private val _currentProjectId = MutableStateFlow<String?>(null)
    val currentProjectId: StateFlow<String?> = _currentProjectId.asStateFlow()
    
    private var autoSaveJob: Job? = null
    private var autoSaveConfig: AutoSaveConfig = AutoSaveConfig()
    
    init {
        registerIntelliSenseProviders()
        
        viewModelScope.launch {
            editorStateManager.state.collect { state ->
                _editorState.value = state
                updateHighlightedLines(state)
                updateCursorInfo(state)
            }
        }
        
        viewModelScope.launch {
            tabManager.state.collect { state ->
                _tabState.value = state
            }
        }
        
        viewModelScope.launch {
            intelliSenseEngine.state.collect { state ->
                _intelliSenseState.value = state
            }
        }
        
        viewModelScope.launch {
            projectRepository.getAutoSaveConfig().collect { config ->
                autoSaveConfig = config
                if (config.enabled) {
                    startAutoSave(config.intervalMs)
                } else {
                    stopAutoSave()
                }
            }
        }
        
        viewModelScope.launch {
            val tab = tabManager.createTab("Untitled", "")
            loadTabContent(tab.id)
        }
    }
    
    private fun registerIntelliSenseProviders() {
        intelliSenseEngine.registerCompletionProvider(KeywordProvider())
        intelliSenseEngine.registerCompletionProvider(BuiltinProvider())
        intelliSenseEngine.registerCompletionProvider(ModuleProvider())
        intelliSenseEngine.registerCompletionProvider(FunctionProvider())
        intelliSenseEngine.registerCompletionProvider(SnippetProvider())
        
        intelliSenseEngine.registerDiagnosticProvider(SyntaxValidator())
        intelliSenseEngine.registerDiagnosticProvider(Linter())
    }
    
    private fun updateHighlightedLines(state: EditorState) {
        val lines = state.lines.map { line ->
            syntaxHighlighter.highlightLine(line).map { (text, color) ->
                text to androidx.compose.ui.graphics.Color(color)
            }
        }
        _highlightedLines.value = lines
    }
    
    private fun updateCursorInfo(state: EditorState) {
        val selection = state.selection
        val selectedText = if (selection?.isValid == true) {
            val normalized = selection.normalize()
            val start = normalized.start.toOffset(state.content)
            val end = normalized.end.toOffset(state.content)
            state.content.substring(start, end)
        } else null
        
        _cursorInfo.value = CursorInfo(
            line = state.cursorPosition.line + 1,
            column = state.cursorPosition.column + 1,
            selection = selectedText,
            selectedLength = selectedText?.length ?: 0
        )
    }
    
    fun insertText(text: String) {
        viewModelScope.launch {
            val state = _editorState.value
            
            if (state.selection?.isValid == true) {
                editorStateManager.deleteSelection()
            }
            
            val firstChar = text.firstOrNull() ?: return@launch
            val bracketResult = bracketHandler.handleCharacter(state, firstChar)
            if (bracketResult != null) {
                editorStateManager.insertText(bracketResult.text)
                if (bracketResult.cursorOffset != 0) {
                    val currentPos = editorStateManager.getCurrentState().cursorPosition
                    val currentOffset = currentPos.toOffset(editorStateManager.getCurrentState().content)
                    val newOffset = (currentOffset + bracketResult.cursorOffset).coerceAtLeast(0)
                    editorStateManager.moveCursorToOffset(newOffset)
                }
            } else {
                val indentResult = indentationHandler.autoIndentOnCharacter(state, firstChar)
                if (indentResult != null) {
                    editorStateManager.insertText(indentResult)
                } else {
                    editorStateManager.insertText(text)
                }
            }
            
            updateActiveTabContent()
            triggerIntelliSense()
            markModified()
        }
    }
    
    fun deleteCharacter(backward: Boolean = true) {
        viewModelScope.launch {
            val state = _editorState.value
            if (state.selection?.isValid == true) {
                editorStateManager.deleteSelection()
            } else {
                editorStateManager.deleteCharacter(backward)
            }
            updateActiveTabContent()
            markModified()
        }
    }
    
    fun insertTab() {
        viewModelScope.launch {
            val state = _editorState.value
            val tabSpaces = indentationHandler.handleTab(state)
            editorStateManager.insertText(tabSpaces)
            updateActiveTabContent()
            markModified()
        }
    }
    
    fun insertNewLine() {
        viewModelScope.launch {
            val state = _editorState.value
            val newLine = indentationHandler.handleEnter(state)
            editorStateManager.insertText(newLine)
            updateActiveTabContent()
            markModified()
        }
    }
    
    fun moveCursor(line: Int, column: Int) {
        viewModelScope.launch {
            editorStateManager.moveCursor(line, column)
        }
    }
    
    fun moveCursorToOffset(offset: Int) {
        viewModelScope.launch {
            editorStateManager.moveCursorToOffset(offset)
        }
    }
    
    fun select(start: CursorPosition, end: CursorPosition) {
        viewModelScope.launch {
            editorStateManager.select(start, end)
        }
    }
    
    fun selectAll() {
        viewModelScope.launch {
            editorStateManager.selectAll()
        }
    }
    
    fun undo() {
        viewModelScope.launch {
            editorStateManager.undo()
            updateActiveTabContent()
            markModified()
        }
    }
    
    fun redo() {
        viewModelScope.launch {
            editorStateManager.redo()
            updateActiveTabContent()
            markModified()
        }
    }
    
    fun copy(): Boolean {
        return clipboardHandler.copy(_editorState.value)
    }
    
    fun cut() {
        viewModelScope.launch {
            val (newState, success) = clipboardHandler.cut(_editorState.value)
            if (success) {
                editorStateManager.initialize(newState.content)
                editorStateManager.moveCursor(newState.cursorPosition.line, newState.cursorPosition.column)
                updateActiveTabContent()
                markModified()
            }
        }
    }
    
    fun paste() {
        viewModelScope.launch {
            val newState = clipboardHandler.paste(_editorState.value)
            editorStateManager.initialize(newState.content)
            editorStateManager.moveCursor(newState.cursorPosition.line, newState.cursorPosition.column)
            updateActiveTabContent()
            markModified()
        }
    }
    
    fun search(
        query: String,
        isCaseSensitive: Boolean = false,
        isRegex: Boolean = false,
        isWholeWord: Boolean = false
    ) {
        val matches = searchHandler.search(
            _editorState.value,
            query,
            isCaseSensitive,
            isRegex,
            isWholeWord
        )
        
        _searchState.update {
            it.copy(
                query = query,
                matches = matches,
                currentMatchIndex = if (matches.isNotEmpty()) 0 else -1,
                isOpen = true
            )
        }
        
        if (matches.isNotEmpty()) {
            val firstMatch = matches.first()
            viewModelScope.launch {
                editorStateManager.moveCursor(firstMatch.start.line, firstMatch.start.column)
                editorStateManager.select(firstMatch.start, firstMatch.end)
            }
        }
    }
    
    fun findNext() {
        val state = _searchState.value
        val match = searchHandler.findNext(_editorState.value, state) ?: return
        
        val matchIndex = state.matches.indexOf(match)
        _searchState.update { it.copy(currentMatchIndex = matchIndex) }
        
        viewModelScope.launch {
            editorStateManager.moveCursor(match.start.line, match.start.column)
            editorStateManager.select(match.start, match.end)
        }
    }
    
    fun findPrevious() {
        val state = _searchState.value
        val match = searchHandler.findPrevious(_editorState.value, state) ?: return
        
        val matchIndex = state.matches.indexOf(match)
        _searchState.update { it.copy(currentMatchIndex = matchIndex) }
        
        viewModelScope.launch {
            editorStateManager.moveCursor(match.start.line, match.start.column)
            editorStateManager.select(match.start, match.end)
        }
    }
    
    fun replace(replacement: String) {
        val state = _searchState.value
        val currentMatch = state.matches.getOrNull(state.currentMatchIndex) ?: return
        
        val newState = searchHandler.replace(_editorState.value, currentMatch, replacement)
        viewModelScope.launch {
            editorStateManager.initialize(newState.content)
            editorStateManager.moveCursor(newState.cursorPosition.line, newState.cursorPosition.column)
        }
        
        search(_searchState.value.query, state.isCaseSensitive, state.isRegex, state.isWholeWord)
        updateActiveTabContent()
        markModified()
    }
    
    fun replaceAll(replacement: String) {
        val state = _searchState.value
        val newState = searchHandler.replaceAll(
            _editorState.value,
            state.query,
            replacement,
            state.isCaseSensitive,
            state.isRegex,
            state.isWholeWord
        )
        
        viewModelScope.launch {
            editorStateManager.initialize(newState.content)
            editorStateManager.moveCursor(0, 0)
        }
        
        search(_searchState.value.query, state.isCaseSensitive, state.isRegex, state.isWholeWord)
        updateActiveTabContent()
        markModified()
    }
    
    fun closeSearch() {
        _searchState.update { it.copy(isOpen = false, matches = emptyList(), query = "") }
        viewModelScope.launch {
            editorStateManager.select(
                editorStateManager.getCurrentState().cursorPosition,
                editorStateManager.getCurrentState().cursorPosition
            )
        }
    }
    
    fun goToLine(lineNumber: Int) {
        val line = (lineNumber - 1).coerceIn(0, _editorState.value.lines.size - 1)
        viewModelScope.launch {
            editorStateManager.moveCursor(line, 0)
        }
    }
    
    fun setWordWrap(enabled: Boolean) {
        viewModelScope.launch {
            editorStateManager.setWordWrap(enabled)
            _config.update { it.copy(wordWrap = enabled) }
        }
    }
    
    fun setFontSize(size: Int) {
        viewModelScope.launch {
            editorStateManager.setFontSize(size)
            _config.update { it.copy(fontSize = size) }
            syntaxHighlighter = PythonSyntaxHighlighter(_config.value.theme)
        }
    }
    
    fun zoomIn() {
        val currentSize = _config.value.fontSize
        if (currentSize < 72) {
            setFontSize(currentSize + 2)
        }
    }
    
    fun zoomOut() {
        val currentSize = _config.value.fontSize
        if (currentSize > 8) {
            setFontSize(currentSize - 2)
        }
    }
    
    fun resetZoom() {
        setFontSize(14)
    }
    
    fun setTheme(theme: EditorTheme) {
        _config.update { it.copy(theme = theme) }
        syntaxHighlighter = PythonSyntaxHighlighter(theme)
        updateHighlightedLines(_editorState.value)
    }
    
    fun setShowLineNumbers(show: Boolean) {
        viewModelScope.launch {
            editorStateManager.setShowLineNumbers(show)
            _config.update { it.copy(showLineNumbers = show) }
        }
    }
    
    fun setHighlightCurrentLine(highlight: Boolean) {
        viewModelScope.launch {
            editorStateManager.setHighlightCurrentLine(highlight)
            _config.update { it.copy(highlightCurrentLine = highlight) }
        }
    }
    
    fun setAutoIndent(enabled: Boolean) {
        viewModelScope.launch {
            editorStateManager.setAutoIndent(enabled)
            _config.update { it.copy(autoIndent = enabled) }
        }
    }
    
    fun setSmartIndent(enabled: Boolean) {
        viewModelScope.launch {
            editorStateManager.setSmartIndent(enabled)
            _config.update { it.copy(smartIndent = enabled) }
        }
    }
    
    fun setAutoBrackets(enabled: Boolean) {
        viewModelScope.launch {
            editorStateManager.setAutoBrackets(enabled)
            _config.update { it.copy(autoBrackets = enabled) }
        }
    }
    
    fun setAutoQuotes(enabled: Boolean) {
        viewModelScope.launch {
            editorStateManager.setAutoQuotes(enabled)
            _config.update { it.copy(autoQuotes = enabled) }
        }
    }
    
    fun setTabSize(size: Int) {
        _config.update { it.copy(tabSize = size) }
    }
    
    fun createNewTab(title: String = "Untitled", content: String = "") {
        viewModelScope.launch {
            val tab = tabManager.createTab(title, content)
            loadTabContent(tab.id)
        }
    }
    
    fun closeTab(tabId: String) {
        viewModelScope.launch {
            val projectId = _currentProjectId.value
            if (projectId != null && autoSaveConfig.saveOnClose) {
                performAutoSave(projectId)
            }
            tabManager.closeTab(tabId)
            val activeTab = tabManager.getActiveTab()
            if (activeTab != null) {
                loadTabContent(activeTab.id)
            }
        }
    }
    
    fun setActiveTab(tabId: String) {
        viewModelScope.launch {
            val projectId = _currentProjectId.value
            if (projectId != null && autoSaveConfig.saveOnSwitch) {
                performAutoSave(projectId)
            }
            tabManager.setActiveTab(tabId)
            loadTabContent(tabId)
        }
    }
    
    fun setSplitMode(mode: SplitMode) {
        viewModelScope.launch {
            tabManager.setSplitMode(mode)
        }
    }
    
    private fun loadTabContent(tabId: String) {
        viewModelScope.launch {
            val tab = tabManager.getTab(tabId) ?: return@launch
            editorStateManager.initialize(tab.content)
            editorStateManager.moveCursor(tab.cursorPosition.line, tab.cursorPosition.column)
        }
    }
    
    private fun updateActiveTabContent() {
        viewModelScope.launch {
            val activeTab = tabManager.getActiveTab() ?: return@launch
            tabManager.updateTabContent(activeTab.id, _editorState.value.content)
            tabManager.updateTabCursorPosition(activeTab.id, _editorState.value.cursorPosition)
        }
    }
    
    private fun markModified() {
        _isModified.value = true
        viewModelScope.launch {
            val activeTab = tabManager.getActiveTab()
            if (activeTab != null) {
                val projectId = _currentProjectId.value
                if (projectId != null) {
                    projectRepository.updateSessionFile(projectId, activeTab.id, true)
                }
            }
        }
    }
    
    fun loadFile(fileId: String) {
        viewModelScope.launch {
            val file = fileRepository.getFileById(fileId).first()
            if (file != null) {
                tabManager.createTab(file.name, file.content)
            }
        }
    }
    
    fun loadFileFromPath(path: String, name: String) {
        viewModelScope.launch {
            val projectId = _currentProjectId.value
            if (projectId != null) {
                projectRepository.addOpenFile(projectId, path, name)
            }
        }
    }
    
    fun saveCurrentFile() {
        viewModelScope.launch {
            val activeTab = tabManager.getActiveTab() ?: return@launch
            val state = _editorState.value
            fileRepository.saveFileContent(activeTab.id, state.content)
            tabManager.markTabSaved(activeTab.id)
            _isModified.value = false
            
            val projectId = _currentProjectId.value
            if (projectId != null) {
                projectRepository.saveProject(projectId)
                projectRepository.updateSessionFile(projectId, activeTab.id, false)
            }
        }
    }
    
    fun saveAllFiles() {
        viewModelScope.launch {
            val tabs = _tabState.value.tabs
            tabs.forEach { tab ->
                fileRepository.saveFileContent(tab.id, tab.content)
                tabManager.markTabSaved(tab.id)
            }
            _isModified.value = false
            
            val projectId = _currentProjectId.value
            if (projectId != null) {
                projectRepository.saveProject(projectId)
            }
        }
    }
    
    fun setCurrentProject(projectId: String) {
        _currentProjectId.value = projectId
        restoreSessionFiles(projectId)
    }
    
    private fun restoreSessionFiles(projectId: String) {
        viewModelScope.launch {
            projectRepository.getSession(projectId).collect { session ->
                if (session != null) {
                    session.openFiles.forEach { sessionFile ->
                        val existingTab = tabManager.getAllTabs().find { it.id == sessionFile.fileId }
                        if (existingTab == null) {
                            try {
                                val content = java.io.File(sessionFile.path).readText()
                                tabManager.createTab(sessionFile.name, content)
                            } catch (_: Exception) {}
                        }
                    }
                    session.activeFileId?.let { activeId ->
                        tabManager.setActiveTab(activeId)
                        loadTabContent(activeId)
                    }
                }
            }
        }
    }
    
    fun restoreCursorPosition(fileId: String) {
        val projectId = _currentProjectId.value ?: return
        viewModelScope.launch {
            val session = projectRepository.getSession(projectId).first()
            if (session != null) {
                val cursorPos = session.cursorPositions[fileId]
                if (cursorPos != null) {
                    editorStateManager.moveCursor(cursorPos.line, cursorPos.column)
                }
            }
        }
    }
    
    private fun startAutoSave(intervalMs: Long) {
        stopAutoSave()
        autoSaveJob = viewModelScope.launch {
            while (true) {
                delay(intervalMs)
                val projectId = _currentProjectId.value
                if (projectId != null && _isModified.value) {
                    performAutoSave(projectId)
                }
            }
        }
    }
    
    private fun stopAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = null
    }
    
    private suspend fun performAutoSave(projectId: String) {
        if (!_isModified.value) return
        
        val activeTab = tabManager.getActiveTab() ?: return
        val state = _editorState.value
        
        fileRepository.saveFileContent(activeTab.id, state.content)
        tabManager.markTabSaved(activeTab.id)
        projectRepository.performAutoSave(projectId)
        _isModified.value = false
    }
    
    fun performManualSave() {
        viewModelScope.launch {
            val projectId = _currentProjectId.value
            if (projectId != null) {
                performAutoSave(projectId)
                projectRepository.saveProject(projectId)
            } else {
                saveCurrentFile()
            }
        }
    }
    
    private fun triggerIntelliSense() {
        viewModelScope.launch {
            val state = _editorState.value
            val cursorOffset = state.cursorPosition.toOffset(state.content)
            intelliSenseEngine.triggerCompletion(state.content, cursorOffset)
            intelliSenseEngine.updateDiagnostics(state.content)
        }
    }
    
    fun triggerCompletion() {
        viewModelScope.launch {
            val state = _editorState.value
            val cursorOffset = state.cursorPosition.toOffset(state.content)
            intelliSenseEngine.triggerCompletion(state.content, cursorOffset)
        }
    }
    
    fun acceptCompletion(): CompletionItem? {
        val item = intelliSenseEngine.acceptCompletion() ?: return null
        viewModelScope.launch {
            val state = _editorState.value
            val triggerOffset = _intelliSenseState.value.triggerOffset
            val currentOffset = state.cursorPosition.toOffset(state.content)
            if (triggerOffset < 0 || triggerOffset > currentOffset || currentOffset > state.content.length) return@launch
            val prefix = state.content.substring(triggerOffset, currentOffset)
            
            val newText = item.insertText.substring(prefix.length)
            editorStateManager.insertText(newText)
            updateActiveTabContent()
            markModified()
        }
        return item
    }
    
    fun selectNextCompletion() {
        intelliSenseEngine.selectNextCompletion()
    }
    
    fun selectPreviousCompletion() {
        intelliSenseEngine.selectPreviousCompletion()
    }
    
    fun hideCompletions() {
        intelliSenseEngine.hideCompletions()
    }
    
    fun getDiagnostics(): List<Diagnostic> {
        return _intelliSenseState.value.diagnostics
    }
    
    override fun onCleared() {
        super.onCleared()
        stopAutoSave()
        val projectId = _currentProjectId.value
        if (projectId != null && autoSaveConfig.saveOnClose) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                performAutoSave(projectId)
                projectRepository.saveProject(projectId)
                projectRepository.closeProject(projectId)
            }
        }
    }
}
