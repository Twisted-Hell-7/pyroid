package com.pythonide.app.screens.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GoToLine
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SplitScreen
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pythonide.app.screens.debugger.BreakpointGutter
import com.pythonide.app.screens.debugger.DebugPanel
import com.pythonide.app.screens.debugger.DebugToolbar
import com.pythonide.app.screens.debugger.DebuggerViewModel
import com.pythonide.app.screens.editor.intellisense.CompletionPopup
import com.pythonide.domain.model.debugger.DebugState
import com.pythonide.domain.model.editor.EditorTheme
import com.pythonide.domain.model.editor.SplitMode
import com.pythonide.domain.model.editor.Tab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    fileId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: CodeEditorViewModel = hiltViewModel(),
    debuggerViewModel: DebuggerViewModel = hiltViewModel()
) {
    val editorState by viewModel.editorState.collectAsState()
    val tabState by viewModel.tabState.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val cursorInfo by viewModel.cursorInfo.collectAsState()
    val highlightedLines by viewModel.highlightedLines.collectAsState()
    val config by viewModel.config.collectAsState()
    val intelliSenseState by viewModel.intelliSenseState.collectAsState()
    
    val debugState by debuggerViewModel.debugState.collectAsState()
    val breakpoints by debuggerViewModel.breakpoints.collectAsState()
    val currentFrame by debuggerViewModel.currentFrame.collectAsState()
    
    var showSearch by remember { mutableStateOf(false) }
    var showGoToLine by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    
    val currentFilePath = tabState.tabs.find { it.id == tabState.activeTabId }?.title ?: ""
    
    LaunchedEffect(fileId) {
        if (fileId != null) {
            viewModel.loadFile(fileId)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = tabState.tabs.find { it.id == tabState.activeTabId }?.title ?: "Code Editor",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Line ${cursorInfo.line}, Col ${cursorInfo.column}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.undo() }) {
                        Icon(Icons.Default.Undo, "Undo")
                    }
                    IconButton(onClick = { viewModel.redo() }) {
                        Icon(Icons.Default.Redo, "Redo")
                    }
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, "Search")
                    }
                    IconButton(onClick = { showGoToLine = true }) {
                        Icon(Icons.Default.GoToLine, "Go to Line")
                    }
                    IconButton(onClick = { viewModel.saveCurrentFile() }) {
                        Icon(Icons.Default.Save, "Save")
                    }
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(Icons.Default.FindReplace, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = config.theme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(config.theme.background)
        ) {
            // Debug toolbar
            DebugToolbar(
                debugState = debugState,
                onStartDebugging = {
                    val activeTab = tabState.tabs.find { it.id == tabState.activeTabId }
                    if (activeTab != null) {
                        debuggerViewModel.startDebugging(
                            filePath = activeTab.title,
                            code = activeTab.content
                        )
                    }
                },
                onStopDebugging = { debuggerViewModel.stopDebugging() },
                onContinue = { debuggerViewModel.continueExecution() },
                onStepInto = { debuggerViewModel.stepInto() },
                onStepOver = { debuggerViewModel.stepOver() },
                onStepOut = { debuggerViewModel.stepOut() }
            )
            
            // Tabs
            TabBar(
                tabs = tabState.tabs,
                activeTabId = tabState.activeTabId,
                onTabClick = { viewModel.setActiveTab(it) },
                onTabClose = { viewModel.closeTab(it) },
                onNewTab = { viewModel.createNewTab() },
                theme = config.theme
            )
            
            // Search bar
            AnimatedVisibility(
                visible = showSearch,
                enter = slideInHorizontally() + fadeIn(),
                exit = slideOutHorizontally() + fadeOut()
            ) {
                SearchBar(
                    searchState = searchState,
                    onQueryChange = { query ->
                        viewModel.search(
                            query,
                            searchState.isCaseSensitive,
                            searchState.isRegex,
                            searchState.isWholeWord
                        )
                    },
                    onNext = { viewModel.findNext() },
                    onPrevious = { viewModel.findPrevious() },
                    onReplace = { viewModel.replace(it) },
                    onReplaceAll = { viewModel.replaceAll(it) },
                    onCaseSensitiveChange = { sensitive ->
                        viewModel.search(
                            searchState.query,
                            sensitive,
                            searchState.isRegex,
                            searchState.isWholeWord
                        )
                    },
                    onRegexChange = { regex ->
                        viewModel.search(
                            searchState.query,
                            searchState.isCaseSensitive,
                            regex,
                            searchState.isWholeWord
                        )
                    },
                    onWholeWordChange = { wholeWord ->
                        viewModel.search(
                            searchState.query,
                            searchState.isCaseSensitive,
                            searchState.isRegex,
                            wholeWord
                        )
                    },
                    onClose = { showSearch = false; viewModel.closeSearch() },
                    theme = config.theme
                )
            }
            
            // Editor content
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (tabState.splitMode) {
                    SplitMode.NONE -> {
                        EditorContent(
                            editorState = editorState,
                            highlightedLines = highlightedLines,
                            config = config,
                            onTextChange = { viewModel.insertText(it) },
                            onDelete = { viewModel.deleteCharacter(it) },
                            onCursorMove = { line, col -> viewModel.moveCursor(line, col) },
                            onSelect = { start, end -> viewModel.select(start, end) },
                            modifier = Modifier.fillMaxSize(),
                            breakpoints = breakpoints,
                            currentFilePath = currentFilePath,
                            onToggleBreakpoint = { line -> debuggerViewModel.toggleBreakpointAtLine(currentFilePath, line) }
                        )
                    }
                    SplitMode.HORIZONTAL -> {
                        Row(modifier = Modifier.fillMaxSize()) {
                            EditorContent(
                                editorState = editorState,
                                highlightedLines = highlightedLines,
                                config = config,
                                onTextChange = { viewModel.insertText(it) },
                                onDelete = { viewModel.deleteCharacter(it) },
                                onCursorMove = { line, col -> viewModel.moveCursor(line, col) },
                                onSelect = { start, end -> viewModel.select(start, end) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                breakpoints = breakpoints,
                                currentFilePath = currentFilePath,
                                onToggleBreakpoint = { line -> debuggerViewModel.toggleBreakpointAtLine(currentFilePath, line) }
                            )
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(config.theme.lineNumberColor)
                            )
                            EditorContent(
                                editorState = editorState,
                                highlightedLines = highlightedLines,
                                config = config,
                                onTextChange = { viewModel.insertText(it) },
                                onDelete = { viewModel.deleteCharacter(it) },
                                onCursorMove = { line, col -> viewModel.moveCursor(line, col) },
                                onSelect = { start, end -> viewModel.select(start, end) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                breakpoints = breakpoints,
                                currentFilePath = currentFilePath,
                                onToggleBreakpoint = { line -> debuggerViewModel.toggleBreakpointAtLine(currentFilePath, line) }
                            )
                        }
                    }
                    SplitMode.VERTICAL -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            EditorContent(
                                editorState = editorState,
                                highlightedLines = highlightedLines,
                                config = config,
                                onTextChange = { viewModel.insertText(it) },
                                onDelete = { viewModel.deleteCharacter(it) },
                                onCursorMove = { line, col -> viewModel.moveCursor(line, col) },
                                onSelect = { start, end -> viewModel.select(start, end) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                breakpoints = breakpoints,
                                currentFilePath = currentFilePath,
                                onToggleBreakpoint = { line -> debuggerViewModel.toggleBreakpointAtLine(currentFilePath, line) }
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(config.theme.lineNumberColor)
                            )
                            EditorContent(
                                editorState = editorState,
                                highlightedLines = highlightedLines,
                                config = config,
                                onTextChange = { viewModel.insertText(it) },
                                onDelete = { viewModel.deleteCharacter(it) },
                                onCursorMove = { line, col -> viewModel.moveCursor(line, col) },
                                onSelect = { start, end -> viewModel.select(start, end) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                breakpoints = breakpoints,
                                currentFilePath = currentFilePath,
                                onToggleBreakpoint = { line -> debuggerViewModel.toggleBreakpointAtLine(currentFilePath, line) }
                            )
                        }
                    }
                }
                
                // Completion popup
                CompletionPopup(
                    completions = intelliSenseState.completions,
                    selectedIndex = intelliSenseState.selectedIndex,
                    isVisible = intelliSenseState.isCompletionsVisible,
                    onItemSelected = { index ->
                        // Update selection in engine
                        repeat(index - intelliSenseState.selectedIndex) { viewModel.selectNextCompletion() }
                        repeat(intelliSenseState.selectedIndex - index) { viewModel.selectPreviousCompletion() }
                    },
                    onItemClicked = {
                        viewModel.acceptCompletion()
                    },
                    modifier = Modifier.padding(start = 48.dp)
                )
            }
            
            // Debug panel
            DebugPanel(
                viewModel = debuggerViewModel
            )
            
            // Status bar
            StatusBar(
                cursorInfo = cursorInfo,
                tabCount = tabState.tabs.size,
                splitMode = tabState.splitMode,
                onSplitModeChange = { viewModel.setSplitMode(it) },
                theme = config.theme,
                debugState = debugState,
                onToggleDebugPanel = { debuggerViewModel.togglePanel() }
            )
        }
    }
    
    // Go to line dialog
    if (showGoToLine) {
        GoToLineDialog(
            maxLine = editorState.lines.size,
            onGoToLine = { viewModel.goToLine(it); showGoToLine = false },
            onDismiss = { showGoToLine = false }
        )
    }
    
    // Settings dialog
    if (showSettings) {
        EditorSettingsDialog(
            config = config,
            onConfigChange = { newConfig ->
                viewModel.setWordWrap(newConfig.wordWrap)
                viewModel.setFontSize(newConfig.fontSize)
                viewModel.setShowLineNumbers(newConfig.showLineNumbers)
                viewModel.setHighlightCurrentLine(newConfig.highlightCurrentLine)
                viewModel.setAutoIndent(newConfig.autoIndent)
                viewModel.setSmartIndent(newConfig.smartIndent)
                viewModel.setAutoBrackets(newConfig.autoBrackets)
                viewModel.setAutoQuotes(newConfig.autoQuotes)
                viewModel.setTabSize(newConfig.tabSize)
                viewModel.setTheme(newConfig.theme)
            },
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun TabBar(
    tabs: List<Tab>,
    activeTabId: String?,
    onTabClick: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onNewTab: () -> Unit,
    theme: EditorTheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(theme.lineNumberBackground)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = rememberLazyListState(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(tabs) { _, tab ->
                TabItem(
                    tab = tab,
                    isActive = tab.id == activeTabId,
                    onClick = { onTabClick(tab.id) },
                    onClose = { onTabClose(tab.id) },
                    theme = theme
                )
            }
        }
        
        IconButton(
            onClick = onNewTab,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "New Tab",
                tint = theme.foreground,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TabItem(
    tab: Tab,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    theme: EditorTheme
) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .background(if (isActive) theme.background else theme.lineNumberBackground.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tab.title,
            color = if (isActive) theme.foreground else theme.lineNumberColor,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        if (tab.isModified) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(6.dp)
                    .background(theme.functionColor, RoundedCornerShape(3.dp))
            )
        }
        
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = theme.lineNumberColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun EditorContent(
    editorState: com.pythonide.domain.model.editor.EditorState,
    highlightedLines: List<List<Pair<String, androidx.compose.ui.graphics.Color>>>,
    config: com.pythonide.domain.model.editor.EditorConfig,
    onTextChange: (String) -> Unit,
    onDelete: (Boolean) -> Unit,
    onCursorMove: (Int, Int) -> Unit,
    onSelect: (com.pythonide.domain.model.editor.CursorPosition, com.pythonide.domain.model.editor.CursorPosition) -> Unit,
    modifier: Modifier = Modifier,
    breakpoints: List<com.pythonide.domain.model.debugger.Breakpoint> = emptyList(),
    currentFilePath: String = "",
    onToggleBreakpoint: (Int) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(editorState.cursorPosition.line) {
        listState.animateScrollToItem(editorState.cursorPosition.line)
    }
    
    Row(
        modifier = modifier
            .background(config.theme.background)
            .focusRequester(focusRequester)
            .onKeyEvent { keyEvent ->
                when (keyEvent.key) {
                    Key.Backspace -> { onDelete(true); true }
                    Key.Delete -> { onDelete(false); true }
                    Key.DirectionLeft -> { 
                        if (editorState.cursorPosition.column > 0) {
                            onCursorMove(editorState.cursorPosition.line, editorState.cursorPosition.column - 1)
                        }
                        true
                    }
                    Key.DirectionRight -> {
                        val maxCol = editorState.lines.getOrElse(editorState.cursorPosition.line) { "" }.length
                        if (editorState.cursorPosition.column < maxCol) {
                            onCursorMove(editorState.cursorPosition.line, editorState.cursorPosition.column + 1)
                        }
                        true
                    }
                    Key.DirectionUp -> {
                        if (editorState.cursorPosition.line > 0) {
                            onCursorMove(editorState.cursorPosition.line - 1, editorState.cursorPosition.column)
                        }
                        true
                    }
                    Key.DirectionDown -> {
                        if (editorState.cursorPosition.line < editorState.lines.size - 1) {
                            onCursorMove(editorState.cursorPosition.line + 1, editorState.cursorPosition.column)
                        }
                        true
                    }
                    else -> false
                }
            }
    ) {
        // Line numbers
        if (config.showLineNumbers) {
            LineNumbers(
                lineCount = editorState.lines.size,
                currentLine = editorState.cursorPosition.line,
                theme = config.theme,
                breakpoints = breakpoints,
                currentFilePath = currentFilePath,
                onToggleBreakpoint = onToggleBreakpoint,
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
            )
        }
        
        // Code content
        CodeContent(
            lines = editorState.lines,
            highlightedLines = highlightedLines,
            cursorPosition = editorState.cursorPosition,
            selection = editorState.selection,
            config = config,
            onTextChange = onTextChange,
            onCursorMove = onCursorMove,
            onSelect = onSelect,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun LineNumbers(
    lineCount: Int,
    currentLine: Int,
    theme: EditorTheme,
    breakpoints: List<com.pythonide.domain.model.debugger.Breakpoint> = emptyList(),
    currentFilePath: String = "",
    onToggleBreakpoint: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(theme.lineNumberBackground)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.End
    ) {
        LazyColumn {
            items(lineCount) { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                        .then(
                            if (index == currentLine) {
                                Modifier.background(theme.currentLineColor)
                            } else {
                                Modifier
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Breakpoint gutter
                    BreakpointGutter(
                        lineNumber = index + 1,
                        hasBreakpoint = breakpoints.any {
                            it.filePath == currentFilePath && it.lineNumber == index + 1
                        },
                        isCurrentLine = index == currentLine,
                        onToggleBreakpoint = { onToggleBreakpoint(index + 1) },
                        modifier = Modifier.size(20.dp)
                    )
                    
                    // Line number
                    Text(
                        text = "${index + 1}",
                        color = if (index == currentLine) theme.foreground else theme.lineNumberColor,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeContent(
    lines: List<String>,
    highlightedLines: List<List<Pair<String, androidx.compose.ui.graphics.Color>>>,
    cursorPosition: com.pythonide.domain.model.editor.CursorPosition,
    selection: com.pythonide.domain.model.editor.Selection?,
    config: com.pythonide.domain.model.editor.EditorConfig,
    onTextChange: (String) -> Unit,
    onCursorMove: (Int, Int) -> Unit,
    onSelect: (com.pythonide.domain.model.editor.CursorPosition, com.pythonide.domain.model.editor.CursorPosition) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            itemsIndexed(lines) { lineIndex, line ->
                val highlighted = highlightedLines.getOrElse(lineIndex) { emptyList() }
                val isCurrentLine = lineIndex == cursorPosition.line
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((config.fontSize * 1.5).sp.value.dp)
                        .then(
                            if (isCurrentLine && config.highlightCurrentLine) {
                                Modifier.background(config.theme.currentLineColor)
                            } else {
                                Modifier
                            }
                        )
                        .clickable {
                            onCursorMove(lineIndex, line.length)
                        }
                ) {
                    // Render highlighted text
                    var columnOffset = 0
                    highlighted.forEach { (text, color) ->
                        Text(
                            text = text,
                            color = color,
                            fontSize = config.fontSize.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.clickable {
                                onCursorMove(lineIndex, columnOffset + text.length / 2)
                            }
                        )
                        columnOffset += text.length
                    }
                    
                    // Cursor
                    if (isCurrentLine) {
                        val cursorOffset = cursorPosition.column
                        val textBeforeCursor = line.substring(0, cursorOffset.coerceAtMost(line.length))
                        
                        // This is a simplified cursor rendering
                        // In a real implementation, you'd need to calculate exact pixel positions
                    }
                }
            }
        }
        
        // Selection highlight
        selection?.let { sel ->
            if (sel.isValid) {
                val normalized = sel.normalize()
                // Selection rendering would go here
            }
        }
    }
}

@Composable
private fun StatusBar(
    cursorInfo: com.pythonide.domain.model.editor.CursorInfo,
    tabCount: Int,
    splitMode: SplitMode,
    onSplitModeChange: (SplitMode) -> Unit,
    theme: EditorTheme,
    debugState: DebugState = DebugState.IDLE,
    onToggleDebugPanel: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(theme.lineNumberBackground)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Ln ${cursorInfo.line}, Col ${cursorInfo.column}",
            color = theme.foreground,
            fontSize = 11.sp
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (cursorInfo.selection != null) {
                Text(
                    text = "Selected: ${cursorInfo.selectedLength} chars",
                    color = theme.foreground,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
            
            Text(
                text = "$tabCount tab(s)",
                color = theme.foreground,
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            
            // Debug indicator
            if (debugState != DebugState.IDLE) {
                IconButton(
                    onClick = onToggleDebugPanel,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = "Toggle Debug Panel",
                        tint = when (debugState) {
                            DebugState.PAUSED -> Color(0xFFFFC107)
                            DebugState.RUNNING -> Color(0xFF4CAF50)
                            DebugState.ERROR -> Color(0xFFF44336)
                            else -> theme.foreground
                        },
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            IconButton(
                onClick = {
                    val newMode = when (splitMode) {
                        SplitMode.NONE -> SplitMode.HORIZONTAL
                        SplitMode.HORIZONTAL -> SplitMode.VERTICAL
                        SplitMode.VERTICAL -> SplitMode.NONE
                    }
                    onSplitModeChange(newMode)
                },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.SplitScreen,
                    contentDescription = "Split",
                    tint = theme.foreground,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchState: com.pythonide.domain.model.editor.SearchState,
    onQueryChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReplace: (String) -> Unit,
    onReplaceAll: (String) -> Unit,
    onCaseSensitiveChange: (Boolean) -> Unit,
    onRegexChange: (Boolean) -> Unit,
    onWholeWordChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    theme: EditorTheme
) {
    var showReplace by remember { mutableStateOf(false) }
    var replacementText by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.lineNumberBackground)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchState.query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search...", color = theme.lineNumberColor) },
                textStyle = TextStyle(color = theme.foreground),
                singleLine = true
            )
            
            Text(
                text = "${searchState.matches.size} results",
                color = theme.lineNumberColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.ZoomOut, "Previous", tint = theme.foreground)
            }
            
            IconButton(onClick = onNext) {
                Icon(Icons.Default.ZoomIn, "Next", tint = theme.foreground)
            }
            
            IconButton(onClick = { showReplace = !showReplace }) {
                Icon(Icons.Default.FindReplace, "Toggle Replace", tint = theme.foreground)
            }
            
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Close", tint = theme.foreground)
            }
        }
        
        // Options row
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = searchState.isCaseSensitive,
                onCheckedChange = onCaseSensitiveChange
            )
            Text("Case Sensitive", color = theme.foreground, fontSize = 12.sp)
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Checkbox(
                checked = searchState.isRegex,
                onCheckedChange = onRegexChange
            )
            Text("Regex", color = theme.foreground, fontSize = 12.sp)
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Checkbox(
                checked = searchState.isWholeWord,
                onCheckedChange = onWholeWordChange
            )
            Text("Whole Word", color = theme.foreground, fontSize = 12.sp)
        }
        
        // Replace row
        AnimatedVisibility(visible = showReplace) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = replacementText,
                    onValueChange = { replacementText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Replace...", color = theme.lineNumberColor) },
                    textStyle = TextStyle(color = theme.foreground),
                    singleLine = true
                )
                
                TextButton(onClick = { onReplace(replacementText) }) {
                    Text("Replace", color = theme.foreground)
                }
                
                TextButton(onClick = { onReplaceAll(replacementText) }) {
                    Text("All", color = theme.foreground)
                }
            }
        }
    }
}

@Composable
private fun GoToLineDialog(
    maxLine: Int,
    onGoToLine: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var lineNumber by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Go to Line") },
        text = {
            Column {
                OutlinedTextField(
                    value = lineNumber,
                    onValueChange = { lineNumber = it.filter { c -> c.isDigit() } },
                    label = { Text("Line number (1-$maxLine)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val line = lineNumber.toIntOrNull() ?: return@TextButton
                    onGoToLine(line)
                }
            ) {
                Text("Go")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditorSettingsDialog(
    config: com.pythonide.domain.model.editor.EditorConfig,
    onConfigChange: (com.pythonide.domain.model.editor.EditorConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var fontSize by remember { mutableIntStateOf(config.fontSize) }
    var tabSize by remember { mutableIntStateOf(config.tabSize) }
    var wordWrap by remember { mutableStateOf(config.wordWrap) }
    var showLineNumbers by remember { mutableStateOf(config.showLineNumbers) }
    var highlightCurrentLine by remember { mutableStateOf(config.highlightCurrentLine) }
    var autoIndent by remember { mutableStateOf(config.autoIndent) }
    var smartIndent by remember { mutableStateOf(config.smartIndent) }
    var autoBrackets by remember { mutableStateOf(config.autoBrackets) }
    var autoQuotes by remember { mutableStateOf(config.autoQuotes) }
    var selectedTheme by remember { mutableStateOf(config.theme) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editor Settings") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Font size
                Text("Font Size: $fontSize")
                Slider(
                    value = fontSize.toFloat(),
                    onValueChange = { fontSize = it.toInt() },
                    valueRange = 8f..72f
                )
                
                // Tab size
                Text("Tab Size: $tabSize")
                Slider(
                    value = tabSize.toFloat(),
                    onValueChange = { tabSize = it.toInt() },
                    valueRange = 2f..8f
                )
                
                // Options
                CheckboxRow("Word Wrap", wordWrap) { wordWrap = it }
                CheckboxRow("Show Line Numbers", showLineNumbers) { showLineNumbers = it }
                CheckboxRow("Highlight Current Line", highlightCurrentLine) { highlightCurrentLine = it }
                CheckboxRow("Auto Indent", autoIndent) { autoIndent = it }
                CheckboxRow("Smart Indent", smartIndent) { smartIndent = it }
                CheckboxRow("Auto Brackets", autoBrackets) { autoBrackets = it }
                CheckboxRow("Auto Quotes", autoQuotes) { autoQuotes = it }
                
                // Theme
                Spacer(modifier = Modifier.height(16.dp))
                Text("Theme:")
                EditorTheme.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTheme = theme }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(theme.background)
                                .border(2.dp, if (selectedTheme == theme) theme.foreground else theme.lineNumberColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(theme.name, color = theme.foreground)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfigChange(
                        config.copy(
                            fontSize = fontSize,
                            tabSize = tabSize,
                            wordWrap = wordWrap,
                            showLineNumbers = showLineNumbers,
                            highlightCurrentLine = highlightCurrentLine,
                            autoIndent = autoIndent,
                            smartIndent = smartIndent,
                            autoBrackets = autoBrackets,
                            autoQuotes = autoQuotes,
                            theme = selectedTheme
                        )
                    )
                    onDismiss()
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(label)
    }
}
