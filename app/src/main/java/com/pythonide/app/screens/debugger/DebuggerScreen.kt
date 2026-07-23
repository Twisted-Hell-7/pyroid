package com.pythonide.app.screens.debugger

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.pythonide.domain.model.debugger.Breakpoint
import com.pythonide.domain.model.debugger.DebugState
import com.pythonide.domain.model.debugger.LogEntry
import com.pythonide.domain.model.debugger.LogLevel
import com.pythonide.domain.model.debugger.RuntimeInfo
import com.pythonide.domain.model.debugger.StackFrame
import com.pythonide.domain.model.debugger.Variable
import com.pythonide.domain.model.debugger.WatchExpression

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugToolbar(
    debugState: DebugState,
    onStartDebugging: () -> Unit,
    onStopDebugging: () -> Unit,
    onContinue: () -> Unit,
    onStepInto: () -> Unit,
    onStepOver: () -> Unit,
    onStepOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val isRunning = debugState != DebugState.IDLE && debugState != DebugState.STOPPED
        val isPaused = debugState == DebugState.PAUSED

        if (!isRunning) {
            IconButton(
                onClick = onStartDebugging,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Start Debugging",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            IconButton(
                onClick = onStopDebugging,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.Stop,
                    contentDescription = "Stop Debugging",
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onContinue,
                enabled = isPaused,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Continue",
                    tint = if (isPaused) Color(0xFF4CAF50) else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onStepInto,
                enabled = isPaused,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Step Into",
                    tint = if (isPaused) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onStepOver,
                enabled = isPaused,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = "Step Over",
                    tint = if (isPaused) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onStepOut,
                enabled = isPaused,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.ArrowDropUp,
                    contentDescription = "Step Out",
                    tint = if (isPaused) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = when (debugState) {
                DebugState.IDLE -> "Ready"
                DebugState.STARTING -> "Starting..."
                DebugState.RUNNING -> "Running"
                DebugState.PAUSED -> "Paused"
                DebugState.STEPPING -> "Stepping..."
                DebugState.STOPPED -> "Stopped"
                DebugState.ERROR -> "Error"
            },
            style = MaterialTheme.typography.bodySmall,
            color = when (debugState) {
                DebugState.PAUSED -> Color(0xFFFFC107)
                DebugState.RUNNING -> Color(0xFF4CAF50)
                DebugState.ERROR -> Color(0xFFF44336)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
fun DebugPanel(
    viewModel: DebuggerViewModel,
    modifier: Modifier = Modifier
) {
    val debugState by viewModel.debugState.collectAsState()
    val activePanel by viewModel.activePanel.collectAsState()
    val isPanelVisible by viewModel.isPanelVisible.collectAsState()
    val callStack by viewModel.callStack.collectAsState()
    val variables by viewModel.variables.collectAsState()
    val watchExpressions by viewModel.watchExpressions.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val runtimeInfo by viewModel.runtimeInfo.collectAsState()
    val errorMessages by viewModel.errorMessages.collectAsState()
    val warningMessages by viewModel.warningMessages.collectAsState()

    AnimatedVisibility(
        visible = isPanelVisible,
        enter = expandVertically(expandFrom = Alignment.Bottom),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
        ) {
            DebugPanelTabs(
                activePanel = activePanel,
                errorCount = errorMessages.size,
                warningCount = warningMessages.size,
                onPanelSelected = { viewModel.setActivePanel(it) },
                onClose = { viewModel.hidePanel() }
            )

            when (activePanel) {
                DebugPanel.VARIABLES -> VariablesPanel(
                    variables = variables,
                    expandedVariables = viewModel.expandedVariables.collectAsState().value,
                    onToggleExpansion = { viewModel.toggleVariableExpansion(it) },
                    onInspect = { viewModel.inspectVariable(it) }
                )
                DebugPanel.WATCH -> WatchPanel(
                    watchExpressions = watchExpressions,
                    onAdd = { viewModel.addWatchExpression(it) },
                    onRemove = { viewModel.removeWatchExpression(it) }
                )
                DebugPanel.CALL_STACK -> CallStackPanel(
                    callStack = callStack,
                    onFrameClick = { /* Navigate to frame */ }
                )
                DebugPanel.LOGS -> LogsPanel(
                    logs = logs,
                    onClear = { viewModel.clearLogs() }
                )
                DebugPanel.RUNTIME -> RuntimePanel(
                    runtimeInfo = runtimeInfo
                )
                DebugPanel.ERRORS -> ErrorsPanel(
                    errors = errorMessages
                )
                DebugPanel.WARNINGS -> WarningsPanel(
                    warnings = warningMessages
                )
            }
        }
    }
}

@Composable
private fun DebugPanelTabs(
    activePanel: DebugPanel,
    errorCount: Int,
    warningCount: Int,
    onPanelSelected: (DebugPanel) -> Unit,
    onClose: () -> Unit
) {
    val tabs = listOf(
        DebugPanel.VARIABLES to "Variables",
        DebugPanel.WATCH to "Watch",
        DebugPanel.CALL_STACK to "Call Stack",
        DebugPanel.LOGS to "Logs",
        DebugPanel.RUNTIME to "Runtime",
        DebugPanel.ERRORS to if (errorCount > 0) "Errors ($errorCount)" else "Errors",
        DebugPanel.WARNINGS to if (warningCount > 0) "Warnings ($warningCount)" else "Warnings"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        TabRow(
            selectedTabIndex = tabs.indexOfFirst { it.first == activePanel },
            modifier = Modifier.weight(1f),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, (panel, title) ->
                Tab(
                    selected = activePanel == panel,
                    onClick = { onPanelSelected(panel) },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.height(36.dp)
                )
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Close Panel",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun VariablesPanel(
    variables: List<Variable>,
    expandedVariables: Set<String>,
    onToggleExpansion: (String) -> Unit,
    onInspect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    if (variables.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No variables in scope",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize()
        ) {
            items(variables) { variable ->
                VariableItem(
                    variable = variable,
                    isExpanded = variable.name in expandedVariables,
                    onToggle = { onToggleExpansion(variable.name) },
                    onInspect = { onInspect(variable.name) },
                    depth = 0
                )
            }
        }
    }
}

@Composable
private fun VariableItem(
    variable: Variable,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onInspect: () -> Unit,
    depth: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (variable.isExpandable) {
                        onToggle()
                    } else {
                        onInspect()
                    }
                }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (variable.isExpandable) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Spacer(modifier = Modifier.width(20.dp))
            }

            Text(
                text = variable.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = ": ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Text(
                text = variable.value.take(100),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(2f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "(${variable.type})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }

        if (isExpanded && variable.children != null) {
            variable.children.forEach { child ->
                VariableItem(
                    variable = child,
                    isExpanded = false,
                    onToggle = {},
                    onInspect = {},
                    depth = depth + 1
                )
            }
        }
    }
}

@Composable
fun WatchPanel(
    watchExpressions: List<WatchExpression>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var newExpression by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newExpression,
                onValueChange = { newExpression = it },
                modifier = Modifier
                    .weight(1f)
                    .onKeyEvent { event ->
                        if (event.key == Key.Enter && newExpression.isNotBlank()) {
                            onAdd(newExpression.trim())
                            newExpression = ""
                            true
                        } else false
                    },
                placeholder = { Text("Add watch expression...", style = MaterialTheme.typography.bodySmall) },
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (newExpression.isNotBlank()) {
                        onAdd(newExpression.trim())
                        newExpression = ""
                    }
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Watch")
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(watchExpressions) { watch ->
                WatchExpressionItem(
                    watch = watch,
                    onRemove = { onRemove(watch.id) }
                )
            }
        }
    }
}

@Composable
private fun WatchExpressionItem(
    watch: WatchExpression,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = watch.expression,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = " = ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Text(
            text = watch.value ?: watch.error ?: "...",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = if (watch.error != null) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(2f)
        )

        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Remove Watch",
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun CallStackPanel(
    callStack: List<StackFrame>,
    onFrameClick: (StackFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    if (callStack.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No call stack available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize()
        ) {
            itemsIndexed(callStack) { index, frame ->
                CallStackFrameItem(
                    frame = frame,
                    isCurrent = index == 0,
                    onClick = { onFrameClick(frame) }
                )
            }
        }
    }
}

@Composable
private fun CallStackFrameItem(
    frame: StackFrame,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#${frame.id}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.width(32.dp)
        )

        Text(
            text = frame.functionName,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "${frame.fileName}:${frame.lineNumber}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LogsPanel(
    logs: List<LogEntry>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var filterLevel by remember { mutableStateOf<LogLevel?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filter:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            LogLevel.entries.forEach { level ->
                FilterChip(
                    label = level.name,
                    isSelected = filterLevel == level,
                    color = when (level) {
                        LogLevel.ERROR -> Color(0xFFF44336)
                        LogLevel.WARNING -> Color(0xFFFFC107)
                        LogLevel.INFO -> Color(0xFF4CAF50)
                        LogLevel.DEBUG -> Color(0xFF2196F3)
                    },
                    onClick = {
                        filterLevel = if (filterLevel == level) null else level
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onClear,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Clear Logs",
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        val filteredLogs = if (filterLevel != null) {
            logs.filter { it.level == filterLevel }
        } else {
            logs
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredLogs) { log ->
                LogEntryItem(log)
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isSelected) color.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = 1.dp,
                color = if (isSelected) color else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun LogEntryItem(
    log: LogEntry,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = log.level.name.take(1),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = when (log.level) {
                LogLevel.ERROR -> Color(0xFFF44336)
                LogLevel.WARNING -> Color(0xFFFFC107)
                LogLevel.INFO -> Color(0xFF4CAF50)
                LogLevel.DEBUG -> Color(0xFF2196F3)
            },
            modifier = Modifier.width(16.dp)
        )

        Text(
            text = log.message,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RuntimePanel(
    runtimeInfo: RuntimeInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RuntimeInfoItem(
            label = "Memory Usage",
            value = String.format("%.1f MB", runtimeInfo.memoryUsageMb)
        )
        RuntimeInfoItem(
            label = "Thread Count",
            value = runtimeInfo.threadCount.toString()
        )
        RuntimeInfoItem(
            label = "CPU Time",
            value = "${runtimeInfo.cpuTimeMs}ms"
        )

        if (runtimeInfo.activeModules.isNotEmpty()) {
            Text(
                text = "Active Modules",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            runtimeInfo.activeModules.forEach { module ->
                Text(
                    text = "  - $module",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun RuntimeInfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ErrorsPanel(
    errors: List<String>,
    modifier: Modifier = Modifier
) {
    if (errors.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No errors",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize()
        ) {
            items(errors) { error ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF44336).copy(alpha = 0.1f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "!",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFF44336),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFFFFCDD2)
                    )
                }
            }
        }
    }
}

@Composable
fun WarningsPanel(
    warnings: List<String>,
    modifier: Modifier = Modifier
) {
    if (warnings.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No warnings",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize()
        ) {
            items(warnings) { warning ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFC107).copy(alpha = 0.1f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "!",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFFFFC107),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFFFFECB3)
                    )
                }
            }
        }
    }
}

@Composable
fun BreakpointGutter(
    lineNumber: Int,
    hasBreakpoint: Boolean,
    isCurrentLine: Boolean,
    onToggleBreakpoint: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clickable(onClick = onToggleBreakpoint),
        contentAlignment = Alignment.Center
    ) {
        if (hasBreakpoint) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF44336))
            )
        } else if (isCurrentLine) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFC107))
            )
        }
    }
}
