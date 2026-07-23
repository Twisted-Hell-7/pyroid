package com.pythonide.domain.repository

import com.pythonide.domain.model.ThemeMode
import com.pythonide.domain.model.settings.BackupSettings
import com.pythonide.domain.model.settings.ConsoleSettings
import com.pythonide.domain.model.settings.EditorSettingsState
import com.pythonide.domain.model.settings.FontFamily
import com.pythonide.domain.model.settings.FontSettings
import com.pythonide.domain.model.settings.PackageSettings
import com.pythonide.domain.model.settings.ThemeSettings
import com.pythonide.domain.model.settings.TimeoutSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val themeMode: Flow<ThemeMode>
    val useDynamicColors: Flow<Boolean>
    val useAmoledBlack: Flow<Boolean>
    val editorTheme: Flow<String>
    val fontSize: Flow<Int>
    val fontFamily: Flow<FontFamily>
    val lineHeight: Flow<Float>
    val executionTimeoutMs: Flow<Long>
    val debugTimeoutMs: Flow<Long>
    val autoSaveIntervalMs: Flow<Long>
    val showLineNumbers: Flow<Boolean>
    val wordWrap: Flow<Boolean>
    val highlightCurrentLine: Flow<Boolean>
    val autoIndent: Flow<Boolean>
    val smartIndent: Flow<Boolean>
    val autoBrackets: Flow<Boolean>
    val autoQuotes: Flow<Boolean>
    val tabSize: Flow<Int>
    val indentWithTabs: Flow<Boolean>
    val showWhitespace: Flow<Boolean>
    val showEndOfFile: Flow<Boolean>
    val bracketPairColorization: Flow<Boolean>
    val minimap: Flow<Boolean>
    val stickyScroll: Flow<Boolean>
    val editorFontSize: Flow<Int>
    val consoleFontSize: Flow<Int>
    val consoleFontFamily: Flow<FontFamily>
    val showTimestamps: Flow<Boolean>
    val enableAnsiColors: Flow<Boolean>
    val maxConsoleLines: Flow<Int>
    val autoScroll: Flow<Boolean>
    val consoleWordWrap: Flow<Boolean>
    val autoUpdatePackages: Flow<Boolean>
    val showPrerelease: Flow<Boolean>
    val cacheTimeoutMinutes: Flow<Int>
    val maxConcurrentDownloads: Flow<Int>
    val verifySignatures: Flow<Boolean>
    val autoBackupEnabled: Flow<Boolean>
    val autoBackupIntervalMs: Flow<Long>
    val maxBackups: Flow<Int>
    val backupSettings: Flow<Boolean>
    val backupProjects: Flow<Boolean>
    val backupPackages: Flow<Boolean>
    val lastBackupTimestamp: Flow<Long>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColors(enabled: Boolean)
    suspend fun setAmoledBlack(enabled: Boolean)
    suspend fun setEditorTheme(theme: String)
    suspend fun setFontSize(size: Int)
    suspend fun setFontFamily(family: FontFamily)
    suspend fun setLineHeight(height: Float)
    suspend fun setExecutionTimeout(ms: Long)
    suspend fun setDebugTimeout(ms: Long)
    suspend fun setAutoSaveInterval(ms: Long)
    suspend fun setShowLineNumbers(show: Boolean)
    suspend fun setWordWrap(enabled: Boolean)
    suspend fun setHighlightCurrentLine(highlight: Boolean)
    suspend fun setAutoIndent(indent: Boolean)
    suspend fun setSmartIndent(indent: Boolean)
    suspend fun setAutoBrackets(brackets: Boolean)
    suspend fun setAutoQuotes(quotes: Boolean)
    suspend fun setTabSize(size: Int)
    suspend fun setIndentWithTabs(tabs: Boolean)
    suspend fun setShowWhitespace(show: Boolean)
    suspend fun setShowEndOfFile(show: Boolean)
    suspend fun setBracketPairColorization(enabled: Boolean)
    suspend fun setMinimap(enabled: Boolean)
    suspend fun setStickyScroll(enabled: Boolean)
    suspend fun setEditorFontSize(size: Int)
    suspend fun setConsoleFontSize(size: Int)
    suspend fun setConsoleFontFamily(family: FontFamily)
    suspend fun setShowTimestamps(show: Boolean)
    suspend fun setEnableAnsiColors(enabled: Boolean)
    suspend fun setMaxConsoleLines(lines: Int)
    suspend fun setAutoScroll(enabled: Boolean)
    suspend fun setConsoleWordWrap(enabled: Boolean)
    suspend fun setAutoUpdatePackages(enabled: Boolean)
    suspend fun setShowPrerelease(show: Boolean)
    suspend fun setCacheTimeout(minutes: Int)
    suspend fun setMaxConcurrentDownloads(max: Int)
    suspend fun setVerifySignatures(verify: Boolean)
    suspend fun setAutoBackupEnabled(enabled: Boolean)
    suspend fun setAutoBackupInterval(ms: Long)
    suspend fun setMaxBackups(max: Int)
    suspend fun setBackupSettings(backup: Boolean)
    suspend fun setBackupProjects(backup: Boolean)
    suspend fun setBackupPackages(backup: Boolean)
    suspend fun setLastBackupTimestamp(timestamp: Long)

    suspend fun getThemeSettings(): ThemeSettings
    suspend fun getFontSettings(): FontSettings
    suspend fun getTimeoutSettings(): TimeoutSettings
    suspend fun getEditorSettings(): EditorSettingsState
    suspend fun getConsoleSettings(): ConsoleSettings
    suspend fun getPackageSettings(): PackageSettings
    suspend fun getBackupSettings(): BackupSettings

    suspend fun exportSettings(): String
    suspend fun importSettings(json: String): Result<Unit>
    suspend fun resetAll()
    suspend fun resetTheme()
    suspend fun resetFont()
    suspend fun resetEditor()
    suspend fun resetConsole()
    suspend fun resetPackages()
    suspend fun resetBackup()
}
