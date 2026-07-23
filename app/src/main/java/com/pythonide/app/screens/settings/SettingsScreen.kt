package com.pythonide.app.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Package
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pythonide.domain.model.ThemeMode
import com.pythonide.domain.model.editor.EditorTheme
import com.pythonide.domain.model.settings.FontFamily
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val useDynamicColors by viewModel.useDynamicColors.collectAsState()
    val useAmoledBlack by viewModel.useAmoledBlack.collectAsState()
    val editorTheme by viewModel.editorTheme.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val fontFamily by viewModel.fontFamily.collectAsState()
    val lineHeight by viewModel.lineHeight.collectAsState()
    val executionTimeoutMs by viewModel.executionTimeoutMs.collectAsState()
    val debugTimeoutMs by viewModel.debugTimeoutMs.collectAsState()
    val autoSaveIntervalMs by viewModel.autoSaveIntervalMs.collectAsState()
    val showLineNumbers by viewModel.showLineNumbers.collectAsState()
    val wordWrap by viewModel.wordWrap.collectAsState()
    val highlightCurrentLine by viewModel.highlightCurrentLine.collectAsState()
    val autoIndent by viewModel.autoIndent.collectAsState()
    val smartIndent by viewModel.smartIndent.collectAsState()
    val autoBrackets by viewModel.autoBrackets.collectAsState()
    val autoQuotes by viewModel.autoQuotes.collectAsState()
    val tabSize by viewModel.tabSize.collectAsState()
    val indentWithTabs by viewModel.indentWithTabs.collectAsState()
    val showWhitespace by viewModel.showWhitespace.collectAsState()
    val showEndOfFile by viewModel.showEndOfFile.collectAsState()
    val bracketPairColorization by viewModel.bracketPairColorization.collectAsState()
    val minimap by viewModel.minimap.collectAsState()
    val stickyScroll by viewModel.stickyScroll.collectAsState()
    val editorFontSize by viewModel.editorFontSize.collectAsState()
    val consoleFontSize by viewModel.consoleFontSize.collectAsState()
    val consoleFontFamily by viewModel.consoleFontFamily.collectAsState()
    val showTimestamps by viewModel.showTimestamps.collectAsState()
    val enableAnsiColors by viewModel.enableAnsiColors.collectAsState()
    val maxConsoleLines by viewModel.maxConsoleLines.collectAsState()
    val autoScroll by viewModel.autoScroll.collectAsState()
    val consoleWordWrap by viewModel.consoleWordWrap.collectAsState()
    val autoUpdatePackages by viewModel.autoUpdatePackages.collectAsState()
    val showPrerelease by viewModel.showPrerelease.collectAsState()
    val cacheTimeoutMinutes by viewModel.cacheTimeoutMinutes.collectAsState()
    val maxConcurrentDownloads by viewModel.maxConcurrentDownloads.collectAsState()
    val verifySignatures by viewModel.verifySignatures.collectAsState()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()
    val autoBackupIntervalMs by viewModel.autoBackupIntervalMs.collectAsState()
    val maxBackups by viewModel.maxBackups.collectAsState()
    val backupSettings by viewModel.backupSettings.collectAsState()
    val backupProjects by viewModel.backupProjects.collectAsState()
    val backupPackages by viewModel.backupPackages.collectAsState()
    val lastBackupTimestamp by viewModel.lastBackupTimestamp.collectAsState()

    var expandedSection by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJson by remember { mutableStateOf("") }

    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Reset All")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // THEMES
            item {
                SettingsSection(
                    title = "Themes",
                    icon = Icons.Default.ColorLens,
                    isExpanded = expandedSection == "themes",
                    onToggle = { expandedSection = if (expandedSection == "themes") null else "themes" }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("App Theme", style = MaterialTheme.typography.labelMedium)
                        ThemeMode.entries.forEach { mode ->
                            SettingsRadioButton(
                                label = when (mode) {
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                    ThemeMode.SYSTEM -> "System"
                                },
                                selected = themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("Dynamic Colors", style = MaterialTheme.typography.labelMedium)
                        SettingsSwitch(
                            title = "Use Dynamic Colors",
                            subtitle = "Material You colors on Android 12+",
                            checked = useDynamicColors,
                            onCheckedChange = { viewModel.setDynamicColors(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("AMOLED Black", style = MaterialTheme.typography.labelMedium)
                        SettingsSwitch(
                            title = "Pure Black Background",
                            subtitle = "True black for AMOLED screens",
                            checked = useAmoledBlack,
                            onCheckedChange = { viewModel.setAmoledBlack(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("Editor Theme", style = MaterialTheme.typography.labelMedium)
                        EditorTheme.entries.forEach { theme ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setEditorTheme(theme.name) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(theme.background)
                                        .border(
                                            2.dp,
                                            if (editorTheme == theme.name) MaterialTheme.colorScheme.primary else theme.lineNumberColor,
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(theme.name.replace("_", " "))
                            }
                        }
                    }
                }
            }

            // FONT SIZE
            item {
                SettingsSection(
                    title = "Font",
                    icon = Icons.Default.TextFields,
                    isExpanded = expandedSection == "font",
                    onToggle = { expandedSection = if (expandedSection == "font") null else "font" }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Font Size: $fontSize", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = fontSize.toFloat(),
                            onValueChange = { viewModel.setFontSize(it.toInt()) },
                            valueRange = 8f..72f,
                            steps = 63
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("Font Family", style = MaterialTheme.typography.labelMedium)
                        FontFamily.entries.forEach { family ->
                            SettingsRadioButton(
                                label = family.displayName,
                                selected = fontFamily == family,
                                onClick = { viewModel.setFontFamily(family) }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("Line Height: ${String.format("%.1f", lineHeight)}", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = lineHeight,
                            onValueChange = { viewModel.setLineHeight(it) },
                            valueRange = 1.0f..3.0f,
                            steps = 19
                        )
                    }
                }
            }

            // TIMEOUT
            item {
                SettingsSection(
                    title = "Timeout",
                    icon = Icons.Default.Timer,
                    isExpanded = expandedSection == "timeout",
                    onToggle = { expandedSection = if (expandedSection == "timeout") null else "timeout" }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeoutSlider(
                            label = "Execution Timeout",
                            valueMs = executionTimeoutMs,
                            onValueChange = { viewModel.setExecutionTimeout(it) },
                            rangeMs = 5_000L..120_000L
                        )
                        TimeoutSlider(
                            label = "Debug Timeout",
                            valueMs = debugTimeoutMs,
                            onValueChange = { viewModel.setDebugTimeout(it) },
                            rangeMs = 10_000L..300_000L
                        )
                        TimeoutSlider(
                            label = "Auto-Save Interval",
                            valueMs = autoSaveIntervalMs,
                            onValueChange = { viewModel.setAutoSaveInterval(it) },
                            rangeMs = 5_000L..300_000L
                        )
                    }
                }
            }

            // EDITOR SETTINGS
            item {
                SettingsSection(
                    title = "Editor",
                    icon = Icons.Default.Code,
                    isExpanded = expandedSection == "editor",
                    onToggle = { expandedSection = if (expandedSection == "editor") null else "editor" }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsSwitch("Show Line Numbers", "Display line numbers in gutter", showLineNumbers) { viewModel.setShowLineNumbers(it) }
                        SettingsSwitch("Word Wrap", "Wrap long lines", wordWrap) { viewModel.setWordWrap(it) }
                        SettingsSwitch("Highlight Current Line", "Highlight the active line", highlightCurrentLine) { viewModel.setHighlightCurrentLine(it) }
                        SettingsSwitch("Auto Indent", "Auto-indent new lines", autoIndent) { viewModel.setAutoIndent(it) }
                        SettingsSwitch("Smart Indent", "Intelligent indentation", smartIndent) { viewModel.setSmartIndent(it) }
                        SettingsSwitch("Auto Brackets", "Insert matching brackets", autoBrackets) { viewModel.setAutoBrackets(it) }
                        SettingsSwitch("Auto Quotes", "Insert matching quotes", autoQuotes) { viewModel.setAutoQuotes(it) }
                        SettingsSwitch("Indent With Tabs", "Use tabs instead of spaces", indentWithTabs) { viewModel.setIndentWithTabs(it) }
                        SettingsSwitch("Show Whitespace", "Display whitespace characters", showWhitespace) { viewModel.setShowWhitespace(it) }
                        SettingsSwitch("Show End of File", "Display end-of-file marker", showEndOfFile) { viewModel.setShowEndOfFile(it) }
                        SettingsSwitch("Bracket Pair Colorization", "Color matching brackets", bracketPairColorization) { viewModel.setBracketPairColorization(it) }
                        SettingsSwitch("Minimap", "Show code minimap", minimap) { viewModel.setMinimap(it) }
                        SettingsSwitch("Sticky Scroll", "Keep scope visible", stickyScroll) { viewModel.setStickyScroll(it) }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("Tab Size: $tabSize", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = tabSize.toFloat(),
                            onValueChange = { viewModel.setTabSize(it.toInt()) },
                            valueRange = 2f..8f,
                            steps = 5
                        )

                        Text("Editor Font Size: $editorFontSize", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = editorFontSize.toFloat(),
                            onValueChange = { viewModel.setEditorFontSize(it.toInt()) },
                            valueRange = 8f..72f,
                            steps = 63
                        )
                    }
                }
            }

            // CONSOLE SETTINGS
            item {
                SettingsSection(
                    title = "Console",
                    icon = Icons.Default.Terminal,
                    isExpanded = expandedSection == "console",
                    onToggle = { expandedSection = if (expandedSection == "console") null else "console" }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsSwitch("Show Timestamps", "Display timestamps in console", showTimestamps) { viewModel.setShowTimestamps(it) }
                        SettingsSwitch("ANSI Colors", "Render ANSI color codes", enableAnsiColors) { viewModel.setEnableAnsiColors(it) }
                        SettingsSwitch("Auto Scroll", "Auto-scroll to bottom", autoScroll) { viewModel.setAutoScroll(it) }
                        SettingsSwitch("Word Wrap", "Wrap long lines", consoleWordWrap) { viewModel.setConsoleWordWrap(it) }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("Console Font Size: $consoleFontSize", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = consoleFontSize.toFloat(),
                            onValueChange = { viewModel.setConsoleFontSize(it.toInt()) },
                            valueRange = 8f..72f,
                            steps = 63
                        )

                        Text("Console Font Family", style = MaterialTheme.typography.labelMedium)
                        FontFamily.entries.forEach { family ->
                            SettingsRadioButton(
                                label = family.displayName,
                                selected = consoleFontFamily == family,
                                onClick = { viewModel.setConsoleFontFamily(family) }
                            )
                        }

                        Text("Max Lines: $maxConsoleLines", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = maxConsoleLines.toFloat(),
                            onValueChange = { viewModel.setMaxConsoleLines(it.toInt()) },
                            valueRange = 1000f..50000f,
                            steps = 48
                        )
                    }
                }
            }

            // PACKAGE SETTINGS
            item {
                SettingsSection(
                    title = "Packages",
                    icon = Icons.Default.Package,
                    isExpanded = expandedSection == "packages",
                    onToggle = { expandedSection = if (expandedSection == "packages") null else "packages" }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsSwitch("Auto-Update Packages", "Automatically update packages", autoUpdatePackages) { viewModel.setAutoUpdatePackages(it) }
                        SettingsSwitch("Show Prerelease", "Include pre-release versions", showPrerelease) { viewModel.setShowPrerelease(it) }
                        SettingsSwitch("Verify Signatures", "Verify package signatures", verifySignatures) { viewModel.setVerifySignatures(it) }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("Cache Timeout: $cacheTimeoutMinutes min", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = cacheTimeoutMinutes.toFloat(),
                            onValueChange = { viewModel.setCacheTimeout(it.toInt()) },
                            valueRange = 5f..1440f,
                            steps = 286
                        )

                        Text("Max Concurrent Downloads: $maxConcurrentDownloads", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = maxConcurrentDownloads.toFloat(),
                            onValueChange = { viewModel.setMaxConcurrentDownloads(it.toInt()) },
                            valueRange = 1f..10f,
                            steps = 8
                        )
                    }
                }
            }

            // BACKUP SETTINGS
            item {
                SettingsSection(
                    title = "Backup",
                    icon = Icons.Default.Backup,
                    isExpanded = expandedSection == "backup",
                    onToggle = { expandedSection = if (expandedSection == "backup") null else "backup" }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsSwitch("Auto-Backup", "Enable automatic backups", autoBackupEnabled) { viewModel.setAutoBackupEnabled(it) }
                        SettingsSwitch("Backup Settings", "Include settings in backup", backupSettings) { viewModel.setBackupSettings(it) }
                        SettingsSwitch("Backup Projects", "Include projects in backup", backupProjects) { viewModel.setBackupProjects(it) }
                        SettingsSwitch("Backup Packages", "Include packages in backup", backupPackages) { viewModel.setBackupPackages(it) }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("Max Backups: $maxBackups", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = maxBackups.toFloat(),
                            onValueChange = { viewModel.setMaxBackups(it.toInt()) },
                            valueRange = 1f..20f,
                            steps = 18
                        )

                        Text("Backup Interval: ${formatDuration(autoBackupIntervalMs)}", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = autoBackupIntervalMs.toFloat(),
                            onValueChange = { viewModel.setAutoBackupInterval(it.toLong()) },
                            valueRange = 3_600_000f..604_800_000f,
                            steps = 6
                        )

                        if (lastBackupTimestamp > 0) {
                            Text(
                                "Last backup: ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(lastBackupTimestamp))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // RESTORE SETTINGS
            item {
                SettingsSection(
                    title = "Restore",
                    icon = Icons.Default.Restore,
                    isExpanded = expandedSection == "restore",
                    onToggle = { expandedSection = if (expandedSection == "restore") null else "restore" }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(
                            onClick = {
                                viewModel.exportSettings { json ->
                                    val clip = ClipData.newPlainText("settings", json)
                                    clipboardManager.setPrimaryClip(clip)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy Settings to Clipboard")
                        }

                        TextButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import Settings from Clipboard")
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("Reset Individual Sections", style = MaterialTheme.typography.labelMedium)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ResetButton("Theme") { viewModel.resetTheme() }
                            ResetButton("Font") { viewModel.resetFont() }
                            ResetButton("Editor") { viewModel.resetEditor() }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ResetButton("Console") { viewModel.resetConsole() }
                            ResetButton("Packages") { viewModel.resetPackages() }
                            ResetButton("Backup") { viewModel.resetBackup() }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Settings") },
            text = { Text("This will reset all settings to their default values. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetAll(); showResetDialog = false }) {
                    Text("Reset All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showImportDialog) {
        val clip = clipboardManager.primaryClip
        val clipText = clip?.getItemAt(0)?.text?.toString() ?: ""
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Settings") },
            text = {
                Column {
                    Text("Paste settings JSON below:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = if (importJson.isEmpty()) clipText else importJson,
                        onValueChange = { importJson = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                        placeholder = { Text("Settings JSON") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val json = if (importJson.isEmpty()) clipText else importJson
                    if (json.isNotBlank()) {
                        viewModel.importSettings(json) { result ->
                            showImportDialog = false
                            importJson = ""
                        }
                    }
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false; importJson = "" }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsRadioButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TimeoutSlider(
    label: String,
    valueMs: Long,
    onValueChange: (Long) -> Unit,
    rangeMs: LongRange
) {
    Column {
        Text("$label: ${formatDuration(valueMs)}", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = valueMs.toFloat(),
            onValueChange = { onValueChange(it.toLong()) },
            valueRange = rangeMs.first.toFloat()..rangeMs.last.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun ResetButton(
    label: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.weight(1f)
    ) {
        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun formatDuration(ms: Long): String {
    return when {
        ms < 60_000 -> "${ms / 1000}s"
        ms < 3_600_000 -> "${ms / 60_000}m"
        ms < 86_400_000 -> "${ms / 3_600_000}h"
        else -> "${ms / 86_400_000}d"
    }
}
