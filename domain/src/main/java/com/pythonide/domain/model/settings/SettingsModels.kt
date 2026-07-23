package com.pythonide.domain.model.settings

import com.pythonide.domain.model.ThemeMode

data class AppSettings(
    val themeSettings: ThemeSettings = ThemeSettings(),
    val fontSettings: FontSettings = FontSettings(),
    val timeoutSettings: TimeoutSettings = TimeoutSettings(),
    val editorSettings: EditorSettingsState = EditorSettingsState(),
    val consoleSettings: ConsoleSettings = ConsoleSettings(),
    val packageSettings: PackageSettings = PackageSettings(),
    val backupSettings: BackupSettings = BackupSettings()
)

data class ThemeSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColors: Boolean = true,
    val useAmoledBlack: Boolean = false,
    val editorTheme: String = "DEFAULT"
)

data class FontSettings(
    val fontSize: Int = 14,
    val fontFamily: FontFamily = FontFamily.MONOSPACE,
    val lineHeight: Float = 1.5f
)

enum class FontFamily(val displayName: String, val family: String) {
    MONOSPACE("Monospace", "monospace"),
    SANS_SERIF("Sans Serif", "sans-serif"),
    SERIF("Serif", "serif"),
    CASUAL("Casual", "casual"),
    CURSIVE("Cursive", "cursive")
}

data class TimeoutSettings(
    val executionTimeoutMs: Long = 30_000L,
    val debugTimeoutMs: Long = 60_000L,
    val autoSaveIntervalMs: Long = 30_000L
)

data class EditorSettingsState(
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = true,
    val highlightCurrentLine: Boolean = true,
    val autoIndent: Boolean = true,
    val smartIndent: Boolean = true,
    val autoBrackets: Boolean = true,
    val autoQuotes: Boolean = true,
    val tabSize: Int = 4,
    val indentWithTabs: Boolean = false,
    val showWhitespace: Boolean = false,
    val showEndOfFile: Boolean = false,
    val bracketPairColorization: Boolean = true,
    val minimap: Boolean = false,
    val stickyScroll: Boolean = true,
    val fontSize: Int = 14
)

data class ConsoleSettings(
    val fontSize: Int = 14,
    val fontFamily: FontFamily = FontFamily.MONOSPACE,
    val showTimestamps: Boolean = true,
    val enableAnsiColors: Boolean = true,
    val maxLines: Int = 10000,
    val autoScroll: Boolean = true,
    val wordWrap: Boolean = true
)

data class PackageSettings(
    val autoUpdatePackages: Boolean = false,
    val showPrerelease: Boolean = false,
    val cacheTimeoutMinutes: Int = 60,
    val maxConcurrentDownloads: Int = 3,
    val verifySignatures: Boolean = true
)

data class BackupSettings(
    val autoBackupEnabled: Boolean = true,
    val autoBackupIntervalMs: Long = 86_400_000L,
    val maxBackups: Int = 5,
    val backupSettings: Boolean = true,
    val backupProjects: Boolean = true,
    val backupPackages: Boolean = false,
    val lastBackupTimestamp: Long = 0L
)
