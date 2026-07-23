package com.pythonide.app.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pythonide.domain.model.ThemeMode
import com.pythonide.domain.model.settings.FontFamily
import com.pythonide.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)
    val useDynamicColors: StateFlow<Boolean> = settingsRepository.useDynamicColors.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val useAmoledBlack: StateFlow<Boolean> = settingsRepository.useAmoledBlack.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val editorTheme: StateFlow<String> = settingsRepository.editorTheme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DEFAULT")
    val fontSize: StateFlow<Int> = settingsRepository.fontSize.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 14)
    val fontFamily: StateFlow<FontFamily> = settingsRepository.fontFamily.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FontFamily.MONOSPACE)
    val lineHeight: StateFlow<Float> = settingsRepository.lineHeight.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.5f)
    val executionTimeoutMs: StateFlow<Long> = settingsRepository.executionTimeoutMs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30_000L)
    val debugTimeoutMs: StateFlow<Long> = settingsRepository.debugTimeoutMs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 60_000L)
    val autoSaveIntervalMs: StateFlow<Long> = settingsRepository.autoSaveIntervalMs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30_000L)
    val showLineNumbers: StateFlow<Boolean> = settingsRepository.showLineNumbers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val wordWrap: StateFlow<Boolean> = settingsRepository.wordWrap.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val highlightCurrentLine: StateFlow<Boolean> = settingsRepository.highlightCurrentLine.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val autoIndent: StateFlow<Boolean> = settingsRepository.autoIndent.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val smartIndent: StateFlow<Boolean> = settingsRepository.smartIndent.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val autoBrackets: StateFlow<Boolean> = settingsRepository.autoBrackets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val autoQuotes: StateFlow<Boolean> = settingsRepository.autoQuotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val tabSize: StateFlow<Int> = settingsRepository.tabSize.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)
    val indentWithTabs: StateFlow<Boolean> = settingsRepository.indentWithTabs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val showWhitespace: StateFlow<Boolean> = settingsRepository.showWhitespace.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val showEndOfFile: StateFlow<Boolean> = settingsRepository.showEndOfFile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val bracketPairColorization: StateFlow<Boolean> = settingsRepository.bracketPairColorization.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val minimap: StateFlow<Boolean> = settingsRepository.minimap.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val stickyScroll: StateFlow<Boolean> = settingsRepository.stickyScroll.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val editorFontSize: StateFlow<Int> = settingsRepository.editorFontSize.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 14)
    val consoleFontSize: StateFlow<Int> = settingsRepository.consoleFontSize.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 14)
    val consoleFontFamily: StateFlow<FontFamily> = settingsRepository.consoleFontFamily.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FontFamily.MONOSPACE)
    val showTimestamps: StateFlow<Boolean> = settingsRepository.showTimestamps.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val enableAnsiColors: StateFlow<Boolean> = settingsRepository.enableAnsiColors.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val maxConsoleLines: StateFlow<Int> = settingsRepository.maxConsoleLines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10000)
    val autoScroll: StateFlow<Boolean> = settingsRepository.autoScroll.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val consoleWordWrap: StateFlow<Boolean> = settingsRepository.consoleWordWrap.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val autoUpdatePackages: StateFlow<Boolean> = settingsRepository.autoUpdatePackages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val showPrerelease: StateFlow<Boolean> = settingsRepository.showPrerelease.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val cacheTimeoutMinutes: StateFlow<Int> = settingsRepository.cacheTimeoutMinutes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 60)
    val maxConcurrentDownloads: StateFlow<Int> = settingsRepository.maxConcurrentDownloads.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)
    val verifySignatures: StateFlow<Boolean> = settingsRepository.verifySignatures.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val autoBackupEnabled: StateFlow<Boolean> = settingsRepository.autoBackupEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val autoBackupIntervalMs: StateFlow<Long> = settingsRepository.autoBackupIntervalMs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 86_400_000L)
    val maxBackups: StateFlow<Int> = settingsRepository.maxBackups.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)
    val backupSettings: StateFlow<Boolean> = settingsRepository.backupSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val backupProjects: StateFlow<Boolean> = settingsRepository.backupProjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val backupPackages: StateFlow<Boolean> = settingsRepository.backupPackages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val lastBackupTimestamp: StateFlow<Long> = settingsRepository.lastBackupTimestamp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun setThemeMode(mode: ThemeMode) { viewModelScope.launch { settingsRepository.setThemeMode(mode) } }
    fun setDynamicColors(enabled: Boolean) { viewModelScope.launch { settingsRepository.setDynamicColors(enabled) } }
    fun setAmoledBlack(enabled: Boolean) { viewModelScope.launch { settingsRepository.setAmoledBlack(enabled) } }
    fun setEditorTheme(theme: String) { viewModelScope.launch { settingsRepository.setEditorTheme(theme) } }
    fun setFontSize(size: Int) { viewModelScope.launch { settingsRepository.setFontSize(size) } }
    fun setFontFamily(family: FontFamily) { viewModelScope.launch { settingsRepository.setFontFamily(family) } }
    fun setLineHeight(height: Float) { viewModelScope.launch { settingsRepository.setLineHeight(height) } }
    fun setExecutionTimeout(ms: Long) { viewModelScope.launch { settingsRepository.setExecutionTimeout(ms) } }
    fun setDebugTimeout(ms: Long) { viewModelScope.launch { settingsRepository.setDebugTimeout(ms) } }
    fun setAutoSaveInterval(ms: Long) { viewModelScope.launch { settingsRepository.setAutoSaveInterval(ms) } }
    fun setShowLineNumbers(show: Boolean) { viewModelScope.launch { settingsRepository.setShowLineNumbers(show) } }
    fun setWordWrap(enabled: Boolean) { viewModelScope.launch { settingsRepository.setWordWrap(enabled) } }
    fun setHighlightCurrentLine(highlight: Boolean) { viewModelScope.launch { settingsRepository.setHighlightCurrentLine(highlight) } }
    fun setAutoIndent(indent: Boolean) { viewModelScope.launch { settingsRepository.setAutoIndent(indent) } }
    fun setSmartIndent(indent: Boolean) { viewModelScope.launch { settingsRepository.setSmartIndent(indent) } }
    fun setAutoBrackets(brackets: Boolean) { viewModelScope.launch { settingsRepository.setAutoBrackets(brackets) } }
    fun setAutoQuotes(quotes: Boolean) { viewModelScope.launch { settingsRepository.setAutoQuotes(quotes) } }
    fun setTabSize(size: Int) { viewModelScope.launch { settingsRepository.setTabSize(size) } }
    fun setIndentWithTabs(tabs: Boolean) { viewModelScope.launch { settingsRepository.setIndentWithTabs(tabs) } }
    fun setShowWhitespace(show: Boolean) { viewModelScope.launch { settingsRepository.setShowWhitespace(show) } }
    fun setShowEndOfFile(show: Boolean) { viewModelScope.launch { settingsRepository.setShowEndOfFile(show) } }
    fun setBracketPairColorization(enabled: Boolean) { viewModelScope.launch { settingsRepository.setBracketPairColorization(enabled) } }
    fun setMinimap(enabled: Boolean) { viewModelScope.launch { settingsRepository.setMinimap(enabled) } }
    fun setStickyScroll(enabled: Boolean) { viewModelScope.launch { settingsRepository.setStickyScroll(enabled) } }
    fun setEditorFontSize(size: Int) { viewModelScope.launch { settingsRepository.setEditorFontSize(size) } }
    fun setConsoleFontSize(size: Int) { viewModelScope.launch { settingsRepository.setConsoleFontSize(size) } }
    fun setConsoleFontFamily(family: FontFamily) { viewModelScope.launch { settingsRepository.setConsoleFontFamily(family) } }
    fun setShowTimestamps(show: Boolean) { viewModelScope.launch { settingsRepository.setShowTimestamps(show) } }
    fun setEnableAnsiColors(enabled: Boolean) { viewModelScope.launch { settingsRepository.setEnableAnsiColors(enabled) } }
    fun setMaxConsoleLines(lines: Int) { viewModelScope.launch { settingsRepository.setMaxConsoleLines(lines) } }
    fun setAutoScroll(enabled: Boolean) { viewModelScope.launch { settingsRepository.setAutoScroll(enabled) } }
    fun setConsoleWordWrap(enabled: Boolean) { viewModelScope.launch { settingsRepository.setConsoleWordWrap(enabled) } }
    fun setAutoUpdatePackages(enabled: Boolean) { viewModelScope.launch { settingsRepository.setAutoUpdatePackages(enabled) } }
    fun setShowPrerelease(show: Boolean) { viewModelScope.launch { settingsRepository.setShowPrerelease(show) } }
    fun setCacheTimeout(minutes: Int) { viewModelScope.launch { settingsRepository.setCacheTimeout(minutes) } }
    fun setMaxConcurrentDownloads(max: Int) { viewModelScope.launch { settingsRepository.setMaxConcurrentDownloads(max) } }
    fun setVerifySignatures(verify: Boolean) { viewModelScope.launch { settingsRepository.setVerifySignatures(verify) } }
    fun setAutoBackupEnabled(enabled: Boolean) { viewModelScope.launch { settingsRepository.setAutoBackupEnabled(enabled) } }
    fun setAutoBackupInterval(ms: Long) { viewModelScope.launch { settingsRepository.setAutoBackupInterval(ms) } }
    fun setMaxBackups(max: Int) { viewModelScope.launch { settingsRepository.setMaxBackups(max) } }
    fun setBackupSettings(backup: Boolean) { viewModelScope.launch { settingsRepository.setBackupSettings(backup) } }
    fun setBackupProjects(backup: Boolean) { viewModelScope.launch { settingsRepository.setBackupProjects(backup) } }
    fun setBackupPackages(backup: Boolean) { viewModelScope.launch { settingsRepository.setBackupPackages(backup) } }
    fun setLastBackupTimestamp(timestamp: Long) { viewModelScope.launch { settingsRepository.setLastBackupTimestamp(timestamp) } }

    fun exportSettings(callback: (String) -> Unit) { viewModelScope.launch { callback(settingsRepository.exportSettings()) } }
    fun importSettings(json: String, callback: (Result<Unit>) -> Unit) { viewModelScope.launch { callback(settingsRepository.importSettings(json)) } }
    fun resetAll() { viewModelScope.launch { settingsRepository.resetAll() } }
    fun resetTheme() { viewModelScope.launch { settingsRepository.resetTheme() } }
    fun resetFont() { viewModelScope.launch { settingsRepository.resetFont() } }
    fun resetEditor() { viewModelScope.launch { settingsRepository.resetEditor() } }
    fun resetConsole() { viewModelScope.launch { settingsRepository.resetConsole() } }
    fun resetPackages() { viewModelScope.launch { settingsRepository.resetPackages() } }
    fun resetBackup() { viewModelScope.launch { settingsRepository.resetBackup() } }
}
