package com.pythonide.app.screens.terminal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pythonide.domain.model.terminal.AnsiColor
import com.pythonide.domain.model.terminal.AnsiSegment
import com.pythonide.domain.model.terminal.TerminalConfig
import com.pythonide.domain.model.terminal.TerminalEntry
import com.pythonide.domain.model.terminal.TerminalEntryType
import com.pythonide.domain.model.terminal.TerminalState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val terminalTimeFormat = ThreadLocal.withInitial {
    SimpleDateFormat("HH:mm:ss", Locale.getDefault())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val interpreter by viewModel.currentInterpreter.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showSettings by remember { mutableStateOf(false) }
    var showTabMenu by remember { mutableStateOf(false) }

    LaunchedEffect(state.activeEntries.size) {
        if (state.activeEntries.isNotEmpty()) {
            listState.animateScrollToItem(state.activeEntries.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Terminal",
                            style = MaterialTheme.typography.titleMedium
                        )
                        interpreter?.let { interp ->
                            Text(
                                text = "Python ${interp.pythonVersion} • ${interp.state.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Settings"
                        )
                    }
                    IconButton(onClick = { viewModel.copyLogsToClipboard() }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Logs"
                        )
                    }
                    IconButton(onClick = { viewModel.saveLogsToFile() }) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Logs"
                        )
                    }
                    if (state.isExecuting) {
                        IconButton(onClick = { viewModel.stopExecution() }) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF1E1E1E))
        ) {
            // Terminal tabs
            if (state.terminals.size > 1) {
                TerminalTabBar(
                    state = state,
                    onTabClick = { viewModel.switchTerminal(it) },
                    onNewTab = { viewModel.createNewTerminal() },
                    onCloseTab = { viewModel.closeTerminal(it) }
                )
            }

            // Terminal output
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(state.activeEntries) { entry ->
                    TerminalEntryItem(
                        entry = entry,
                        showTimestamp = state.config.showTimestamps,
                        config = state.config
                    )
                }
            }

            // Input area
            TerminalInput(
                input = state.currentInput,
                onInputChange = { viewModel.updateInput(it) },
                onSubmit = { viewModel.executeCommand() },
                onHistoryUp = { viewModel.navigateHistoryUp() },
                onHistoryDown = { viewModel.navigateHistoryDown() },
                onClearLine = { viewModel.clearLine() },
                isExecuting = state.isExecuting,
                focusRequester = focusRequester,
                modifier = Modifier.padding(12.dp)
            )

            // Status bar
            TerminalStatusBar(
                entryCount = state.activeEntries.size,
                terminalCount = state.terminals.size,
                isExecuting = state.isExecuting
            )
        }
    }

    // Settings dialog
    if (showSettings) {
        TerminalSettingsDialog(
            config = state.config,
            onConfigChange = { viewModel.updateConfig(it) },
            onDismiss = { showSettings = false },
            onClear = { viewModel.clearTerminal() },
            onRestart = { viewModel.restartInterpreter() }
        )
    }
}

@Composable
private fun TerminalTabBar(
    state: TerminalState,
    onTabClick: (String) -> Unit,
    onNewTab: () -> Unit,
    onCloseTab: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF252526))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        state.terminals.forEach { terminal ->
            val isActive = terminal.id == state.activeTerminalId
            Card(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onTabClick(terminal.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) Color(0xFF1E1E1E) else Color(0xFF2D2D2D)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = terminal.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) Color.White else Color(0xFF969696)
                    )
                    if (state.terminals.size > 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onCloseTab(terminal.id) },
                            tint = Color(0xFF969696)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        SmallFloatingActionButton(
            onClick = onNewTab,
            containerColor = Color(0xFF0E639C),
            contentColor = Color.White,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Terminal",
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun TerminalEntryItem(
    entry: TerminalEntry,
    showTimestamp: Boolean,
    config: TerminalConfig
) {
    val timestamp = remember(entry.timestamp) {
        terminalTimeFormat.get()!!.format(Date(entry.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        if (showTimestamp) {
            Text(
                text = "[$timestamp]",
                fontFamily = FontFamily.Monospace,
                fontSize = (config.fontSize - 1).sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        when (entry.type) {
            TerminalEntryType.INPUT -> {
                Text(
                    text = ">>> ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = config.fontSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF569CD6)
                )
                Text(
                    text = entry.content,
                    fontFamily = FontFamily.Monospace,
                    fontSize = config.fontSize.sp,
                    color = Color(0xFFD4D4D4)
                )
            }

            TerminalEntryType.OUTPUT -> {
                if (entry.ansiFormatted.isNotEmpty() && config.enableColors) {
                    AnsiText(
                        segments = entry.ansiFormatted,
                        fontSize = config.fontSize
                    )
                } else {
                    Text(
                        text = entry.content,
                        fontFamily = FontFamily.Monospace,
                        fontSize = config.fontSize.sp,
                        color = Color(0xFFD4D4D4)
                    )
                }
            }

            TerminalEntryType.ERROR -> {
                if (entry.ansiFormatted.isNotEmpty() && config.enableColors) {
                    AnsiText(
                        segments = entry.ansiFormatted,
                        fontSize = config.fontSize
                    )
                } else {
                    Text(
                        text = entry.content,
                        fontFamily = FontFamily.Monospace,
                        fontSize = config.fontSize.sp,
                        color = Color(0xFFF44747)
                    )
                }
            }

            TerminalEntryType.SYSTEM -> {
                Text(
                    text = entry.content,
                    fontFamily = FontFamily.Monospace,
                    fontSize = (config.fontSize - 1).sp,
                    color = Color(0xFF6A9955),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            TerminalEntryType.TIMESTAMP -> {
                Text(
                    text = entry.content,
                    fontFamily = FontFamily.Monospace,
                    fontSize = (config.fontSize - 2).sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
private fun AnsiText(
    segments: List<AnsiSegment>,
    fontSize: Int
) {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        segments.forEach { segment ->
            val color = if (segment.foreground == AnsiColor.DEFAULT) {
                Color(0xFFD4D4D4)
            } else {
                Color(segment.foreground.hex)
            }

            val bgColor = if (segment.background == AnsiColor.DEFAULT) {
                Color.Transparent
            } else {
                Color(segment.background.hex)
            }

            Text(
                text = segment.text,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                color = color,
                fontWeight = if (segment.bold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (segment.italic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                textDecoration = if (segment.underline) {
                    androidx.compose.ui.text.style.TextDecoration.Underline
                } else if (segment.strikethrough) {
                    androidx.compose.ui.text.style.TextDecoration.LineThrough
                } else {
                    null
                },
                modifier = Modifier.background(bgColor)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TerminalInput(
    input: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit,
    onClearLine: () -> Unit,
    isExecuting: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF252526))
            .border(1.dp, Color(0xFF3C3C3C), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = ">>> ",
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF569CD6),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        BasicTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onKeyEvent { keyEvent ->
                    when {
                        keyEvent.key == Key.DirectionUp -> {
                            onHistoryUp()
                            true
                        }
                        keyEvent.key == Key.DirectionDown -> {
                            onHistoryDown()
                            true
                        }
                        keyEvent.key == Key.C && keyEvent.nativeKeyEvent.isCtrlPressed -> {
                            if (input.isEmpty()) {
                                onClearLine()
                            }
                            false
                        }
                        keyEvent.key == Key.L && keyEvent.nativeKeyEvent.isCtrlPressed -> {
                            onClearLine()
                            true
                        }
                        else -> false
                    }
                },
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = Color(0xFFD4D4D4)
            ),
            cursorBrush = SolidColor(Color(0xFFD4D4D4)),
            enabled = !isExecuting,
            maxLines = 5,
            decorationBox = { innerTextField ->
                Box {
                    if (input.isEmpty()) {
                        Text(
                            text = "Enter command...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        FloatingActionButton(
            onClick = onSubmit,
            containerColor = Color(0xFF0E639C),
            contentColor = Color.White,
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(6.dp)
        ) {
            if (isExecuting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Execute",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun TerminalStatusBar(
    entryCount: Int,
    terminalCount: Int,
    isExecuting: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF007ACC))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (isExecuting) "Running..." else "Ready",
            color = Color.White,
            fontSize = 11.sp
        )
        Row {
            Text(
                text = "$entryCount lines",
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = "$terminalCount terminal(s)",
                color = Color.White,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TerminalSettingsDialog(
    config: TerminalConfig,
    onConfigChange: (TerminalConfig) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onRestart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF252526),
        title = {
            Text(
                text = "Terminal Settings",
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Font size
                Text(
                    text = "Font Size: ${config.fontSize}sp",
                    color = Color(0xFFD4D4D4),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Slider(
                    value = config.fontSize.toFloat(),
                    onValueChange = { onConfigChange(config.copy(fontSize = it.toInt())) },
                    valueRange = 10f..20f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF0E639C),
                        activeTrackColor = Color(0xFF0E639C)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Checkboxes
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = config.showTimestamps,
                        onCheckedChange = { onConfigChange(config.copy(showTimestamps = it)) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF0E639C)
                        )
                    )
                    Text(
                        text = "Show Timestamps",
                        color = Color(0xFFD4D4D4)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = config.enableColors,
                        onCheckedChange = { onConfigChange(config.copy(enableColors = it)) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF0E639C)
                        )
                    )
                    Text(
                        text = "ANSI Colors",
                        color = Color(0xFFD4D4D4)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onClear) {
                        Text(
                            text = "Clear Terminal",
                            color = Color(0xFFF44747)
                        )
                    }
                    TextButton(onClick = onRestart) {
                        Text(
                            text = "Restart Interpreter",
                            color = Color(0xFF569CD6)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Done",
                    color = Color(0xFF569CD6)
                )
            }
        }
    )
}
