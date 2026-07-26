package com.pythonide.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pythonide.domain.model.ThemeMode
import com.pythonide.domain.model.settings.BackupSettings
import com.pythonide.domain.model.settings.ConsoleSettings
import com.pythonide.domain.model.settings.EditorSettingsState
import com.pythonide.domain.model.settings.FontFamily
import com.pythonide.domain.model.settings.FontSettings
import com.pythonide.domain.model.settings.PackageSettings
import com.pythonide.domain.model.settings.ThemeSettings
import com.pythonide.domain.model.settings.TimeoutSettings
import com.pythonide.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val EDITOR_THEME = stringPreferencesKey("editor_theme")
        val FONT_SIZE = intPreferencesKey("font_size")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val LINE_HEIGHT = floatPreferencesKey("line_height")
        val EXECUTION_TIMEOUT = longPreferencesKey("execution_timeout")
        val DEBUG_TIMEOUT = longPreferencesKey("debug_timeout")
        val AUTO_SAVE_INTERVAL = longPreferencesKey("auto_save_interval")
        val SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
        val WORD_WRAP = booleanPreferencesKey("word_wrap")
        val HIGHLIGHT_CURRENT_LINE = booleanPreferencesKey("highlight_current_line")
        val AUTO_INDENT = booleanPreferencesKey("auto_indent")
        val SMART_INDENT = booleanPreferencesKey("smart_indent")
        val AUTO_BRACKETS = booleanPreferencesKey("auto_brackets")
        val AUTO_QUOTES = booleanPreferencesKey("auto_quotes")
        val TAB_SIZE = intPreferencesKey("tab_size")
        val INDENT_WITH_TABS = booleanPreferencesKey("indent_with_tabs")
        val SHOW_WHITESPACE = booleanPreferencesKey("show_whitespace")
        val SHOW_END_OF_FILE = booleanPreferencesKey("show_end_of_file")
        val BRACKET_PAIR_COLORIZATION = booleanPreferencesKey("bracket_pair_colorization")
        val MINIMAP = booleanPreferencesKey("minimap")
        val STICKY_SCROLL = booleanPreferencesKey("sticky_scroll")
        val EDITOR_FONT_SIZE = intPreferencesKey("editor_font_size")
        val CONSOLE_FONT_SIZE = intPreferencesKey("console_font_size")
        val CONSOLE_FONT_FAMILY = stringPreferencesKey("console_font_family")
        val SHOW_TIMESTAMPS = booleanPreferencesKey("show_timestamps")
        val ENABLE_ANSI_COLORS = booleanPreferencesKey("enable_ansi_colors")
        val MAX_CONSOLE_LINES = intPreferencesKey("max_console_lines")
        val AUTO_SCROLL = booleanPreferencesKey("auto_scroll")
        val CONSOLE_WORD_WRAP = booleanPreferencesKey("console_word_wrap")
        val AUTO_UPDATE_PACKAGES = booleanPreferencesKey("auto_update_packages")
        val SHOW_PRERELEASE = booleanPreferencesKey("show_prerelease")
        val CACHE_TIMEOUT = intPreferencesKey("cache_timeout")
        val MAX_CONCURRENT_DOWNLOADS = intPreferencesKey("max_concurrent_downloads")
        val VERIFY_SIGNATURES = booleanPreferencesKey("verify_signatures")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val AUTO_BACKUP_INTERVAL = longPreferencesKey("auto_backup_interval")
        val MAX_BACKUPS = intPreferencesKey("max_backups")
        val BACKUP_SETTINGS = booleanPreferencesKey("backup_settings")
        val BACKUP_PROJECTS = booleanPreferencesKey("backup_projects")
        val BACKUP_PACKAGES = booleanPreferencesKey("backup_packages")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
    }

    override val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        try { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name) }
        catch (e: IllegalArgumentException) { ThemeMode.SYSTEM }
    }
    override val useDynamicColors: Flow<Boolean> = context.dataStore.data.map { it[Keys.DYNAMIC_COLORS] ?: true }
    override val useAmoledBlack: Flow<Boolean> = context.dataStore.data.map { it[Keys.AMOLED_BLACK] ?: false }
    override val editorTheme: Flow<String> = context.dataStore.data.map { it[Keys.EDITOR_THEME] ?: "DEFAULT" }
    override val fontSize: Flow<Int> = context.dataStore.data.map { it[Keys.FONT_SIZE] ?: 14 }
    override val fontFamily: Flow<FontFamily> = context.dataStore.data.map { prefs ->
        try { FontFamily.valueOf(prefs[Keys.FONT_FAMILY] ?: FontFamily.MONOSPACE.name) }
        catch (e: IllegalArgumentException) { FontFamily.MONOSPACE }
    }
    override val lineHeight: Flow<Float> = context.dataStore.data.map { it[Keys.LINE_HEIGHT] ?: 1.5f }
    override val executionTimeoutMs: Flow<Long> = context.dataStore.data.map { it[Keys.EXECUTION_TIMEOUT] ?: 30_000L }
    override val debugTimeoutMs: Flow<Long> = context.dataStore.data.map { it[Keys.DEBUG_TIMEOUT] ?: 60_000L }
    override val autoSaveIntervalMs: Flow<Long> = context.dataStore.data.map { it[Keys.AUTO_SAVE_INTERVAL] ?: 30_000L }
    override val showLineNumbers: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_LINE_NUMBERS] ?: true }
    override val wordWrap: Flow<Boolean> = context.dataStore.data.map { it[Keys.WORD_WRAP] ?: true }
    override val highlightCurrentLine: Flow<Boolean> = context.dataStore.data.map { it[Keys.HIGHLIGHT_CURRENT_LINE] ?: true }
    override val autoIndent: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_INDENT] ?: true }
    override val smartIndent: Flow<Boolean> = context.dataStore.data.map { it[Keys.SMART_INDENT] ?: true }
    override val autoBrackets: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_BRACKETS] ?: true }
    override val autoQuotes: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_QUOTES] ?: true }
    override val tabSize: Flow<Int> = context.dataStore.data.map { it[Keys.TAB_SIZE] ?: 4 }
    override val indentWithTabs: Flow<Boolean> = context.dataStore.data.map { it[Keys.INDENT_WITH_TABS] ?: false }
    override val showWhitespace: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_WHITESPACE] ?: false }
    override val showEndOfFile: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_END_OF_FILE] ?: false }
    override val bracketPairColorization: Flow<Boolean> = context.dataStore.data.map { it[Keys.BRACKET_PAIR_COLORIZATION] ?: true }
    override val minimap: Flow<Boolean> = context.dataStore.data.map { it[Keys.MINIMAP] ?: false }
    override val stickyScroll: Flow<Boolean> = context.dataStore.data.map { it[Keys.STICKY_SCROLL] ?: true }
    override val editorFontSize: Flow<Int> = context.dataStore.data.map { it[Keys.EDITOR_FONT_SIZE] ?: 14 }
    override val consoleFontSize: Flow<Int> = context.dataStore.data.map { it[Keys.CONSOLE_FONT_SIZE] ?: 14 }
    override val consoleFontFamily: Flow<FontFamily> = context.dataStore.data.map { prefs ->
        try { FontFamily.valueOf(prefs[Keys.CONSOLE_FONT_FAMILY] ?: FontFamily.MONOSPACE.name) }
        catch (e: IllegalArgumentException) { FontFamily.MONOSPACE }
    }
    override val showTimestamps: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_TIMESTAMPS] ?: true }
    override val enableAnsiColors: Flow<Boolean> = context.dataStore.data.map { it[Keys.ENABLE_ANSI_COLORS] ?: true }
    override val maxConsoleLines: Flow<Int> = context.dataStore.data.map { it[Keys.MAX_CONSOLE_LINES] ?: 10000 }
    override val autoScroll: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_SCROLL] ?: true }
    override val consoleWordWrap: Flow<Boolean> = context.dataStore.data.map { it[Keys.CONSOLE_WORD_WRAP] ?: true }
    override val autoUpdatePackages: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_UPDATE_PACKAGES] ?: false }
    override val showPrerelease: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_PRERELEASE] ?: false }
    override val cacheTimeoutMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.CACHE_TIMEOUT] ?: 60 }
    override val maxConcurrentDownloads: Flow<Int> = context.dataStore.data.map { it[Keys.MAX_CONCURRENT_DOWNLOADS] ?: 3 }
    override val verifySignatures: Flow<Boolean> = context.dataStore.data.map { it[Keys.VERIFY_SIGNATURES] ?: true }
    override val autoBackupEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_BACKUP_ENABLED] ?: true }
    override val autoBackupIntervalMs: Flow<Long> = context.dataStore.data.map { it[Keys.AUTO_BACKUP_INTERVAL] ?: 86_400_000L }
    override val maxBackups: Flow<Int> = context.dataStore.data.map { it[Keys.MAX_BACKUPS] ?: 5 }
    override val backupSettings: Flow<Boolean> = context.dataStore.data.map { it[Keys.BACKUP_SETTINGS] ?: true }
    override val backupProjects: Flow<Boolean> = context.dataStore.data.map { it[Keys.BACKUP_PROJECTS] ?: true }
    override val backupPackages: Flow<Boolean> = context.dataStore.data.map { it[Keys.BACKUP_PACKAGES] ?: false }
    override val lastBackupTimestamp: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_BACKUP_TIMESTAMP] ?: 0L }

    override suspend fun setThemeMode(mode: ThemeMode) { context.dataStore.edit { it[Keys.THEME_MODE] = mode.name } }
    override suspend fun setDynamicColors(enabled: Boolean) { context.dataStore.edit { it[Keys.DYNAMIC_COLORS] = enabled } }
    override suspend fun setAmoledBlack(enabled: Boolean) { context.dataStore.edit { it[Keys.AMOLED_BLACK] = enabled } }
    override suspend fun setEditorTheme(theme: String) { context.dataStore.edit { it[Keys.EDITOR_THEME] = theme } }
    override suspend fun setFontSize(size: Int) { context.dataStore.edit { it[Keys.FONT_SIZE] = size } }
    override suspend fun setFontFamily(family: FontFamily) { context.dataStore.edit { it[Keys.FONT_FAMILY] = family.name } }
    override suspend fun setLineHeight(height: Float) { context.dataStore.edit { it[Keys.LINE_HEIGHT] = height } }
    override suspend fun setExecutionTimeout(ms: Long) { context.dataStore.edit { it[Keys.EXECUTION_TIMEOUT] = ms } }
    override suspend fun setDebugTimeout(ms: Long) { context.dataStore.edit { it[Keys.DEBUG_TIMEOUT] = ms } }
    override suspend fun setAutoSaveInterval(ms: Long) { context.dataStore.edit { it[Keys.AUTO_SAVE_INTERVAL] = ms } }
    override suspend fun setShowLineNumbers(show: Boolean) { context.dataStore.edit { it[Keys.SHOW_LINE_NUMBERS] = show } }
    override suspend fun setWordWrap(enabled: Boolean) { context.dataStore.edit { it[Keys.WORD_WRAP] = enabled } }
    override suspend fun setHighlightCurrentLine(highlight: Boolean) { context.dataStore.edit { it[Keys.HIGHLIGHT_CURRENT_LINE] = highlight } }
    override suspend fun setAutoIndent(indent: Boolean) { context.dataStore.edit { it[Keys.AUTO_INDENT] = indent } }
    override suspend fun setSmartIndent(indent: Boolean) { context.dataStore.edit { it[Keys.SMART_INDENT] = indent } }
    override suspend fun setAutoBrackets(brackets: Boolean) { context.dataStore.edit { it[Keys.AUTO_BRACKETS] = brackets } }
    override suspend fun setAutoQuotes(quotes: Boolean) { context.dataStore.edit { it[Keys.AUTO_QUOTES] = quotes } }
    override suspend fun setTabSize(size: Int) { context.dataStore.edit { it[Keys.TAB_SIZE] = size } }
    override suspend fun setIndentWithTabs(tabs: Boolean) { context.dataStore.edit { it[Keys.INDENT_WITH_TABS] = tabs } }
    override suspend fun setShowWhitespace(show: Boolean) { context.dataStore.edit { it[Keys.SHOW_WHITESPACE] = show } }
    override suspend fun setShowEndOfFile(show: Boolean) { context.dataStore.edit { it[Keys.SHOW_END_OF_FILE] = show } }
    override suspend fun setBracketPairColorization(enabled: Boolean) { context.dataStore.edit { it[Keys.BRACKET_PAIR_COLORIZATION] = enabled } }
    override suspend fun setMinimap(enabled: Boolean) { context.dataStore.edit { it[Keys.MINIMAP] = enabled } }
    override suspend fun setStickyScroll(enabled: Boolean) { context.dataStore.edit { it[Keys.STICKY_SCROLL] = enabled } }
    override suspend fun setEditorFontSize(size: Int) { context.dataStore.edit { it[Keys.EDITOR_FONT_SIZE] = size } }
    override suspend fun setConsoleFontSize(size: Int) { context.dataStore.edit { it[Keys.CONSOLE_FONT_SIZE] = size } }
    override suspend fun setConsoleFontFamily(family: FontFamily) { context.dataStore.edit { it[Keys.CONSOLE_FONT_FAMILY] = family.name } }
    override suspend fun setShowTimestamps(show: Boolean) { context.dataStore.edit { it[Keys.SHOW_TIMESTAMPS] = show } }
    override suspend fun setEnableAnsiColors(enabled: Boolean) { context.dataStore.edit { it[Keys.ENABLE_ANSI_COLORS] = enabled } }
    override suspend fun setMaxConsoleLines(lines: Int) { context.dataStore.edit { it[Keys.MAX_CONSOLE_LINES] = lines } }
    override suspend fun setAutoScroll(enabled: Boolean) { context.dataStore.edit { it[Keys.AUTO_SCROLL] = enabled } }
    override suspend fun setConsoleWordWrap(enabled: Boolean) { context.dataStore.edit { it[Keys.CONSOLE_WORD_WRAP] = enabled } }
    override suspend fun setAutoUpdatePackages(enabled: Boolean) { context.dataStore.edit { it[Keys.AUTO_UPDATE_PACKAGES] = enabled } }
    override suspend fun setShowPrerelease(show: Boolean) { context.dataStore.edit { it[Keys.SHOW_PRERELEASE] = show } }
    override suspend fun setCacheTimeout(minutes: Int) { context.dataStore.edit { it[Keys.CACHE_TIMEOUT] = minutes } }
    override suspend fun setMaxConcurrentDownloads(max: Int) { context.dataStore.edit { it[Keys.MAX_CONCURRENT_DOWNLOADS] = max } }
    override suspend fun setVerifySignatures(verify: Boolean) { context.dataStore.edit { it[Keys.VERIFY_SIGNATURES] = verify } }
    override suspend fun setAutoBackupEnabled(enabled: Boolean) { context.dataStore.edit { it[Keys.AUTO_BACKUP_ENABLED] = enabled } }
    override suspend fun setAutoBackupInterval(ms: Long) { context.dataStore.edit { it[Keys.AUTO_BACKUP_INTERVAL] = ms } }
    override suspend fun setMaxBackups(max: Int) { context.dataStore.edit { it[Keys.MAX_BACKUPS] = max } }
    override suspend fun setBackupSettings(backup: Boolean) { context.dataStore.edit { it[Keys.BACKUP_SETTINGS] = backup } }
    override suspend fun setBackupProjects(backup: Boolean) { context.dataStore.edit { it[Keys.BACKUP_PROJECTS] = backup } }
    override suspend fun setBackupPackages(backup: Boolean) { context.dataStore.edit { it[Keys.BACKUP_PACKAGES] = backup } }
    override suspend fun setLastBackupTimestamp(timestamp: Long) { context.dataStore.edit { it[Keys.LAST_BACKUP_TIMESTAMP] = timestamp } }

    override suspend fun getThemeSettings(): ThemeSettings {
        val prefs = context.dataStore.data.first()
        return ThemeSettings(
            themeMode = try { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name) } catch (e: Exception) { ThemeMode.SYSTEM },
            useDynamicColors = prefs[Keys.DYNAMIC_COLORS] ?: true,
            useAmoledBlack = prefs[Keys.AMOLED_BLACK] ?: false,
            editorTheme = prefs[Keys.EDITOR_THEME] ?: "DEFAULT"
        )
    }

    override suspend fun getFontSettings(): FontSettings {
        val prefs = context.dataStore.data.first()
        return FontSettings(
            fontSize = prefs[Keys.FONT_SIZE] ?: 14,
            fontFamily = try { FontFamily.valueOf(prefs[Keys.FONT_FAMILY] ?: FontFamily.MONOSPACE.name) } catch (e: Exception) { FontFamily.MONOSPACE },
            lineHeight = prefs[Keys.LINE_HEIGHT] ?: 1.5f
        )
    }

    override suspend fun getTimeoutSettings(): TimeoutSettings {
        val prefs = context.dataStore.data.first()
        return TimeoutSettings(
            executionTimeoutMs = prefs[Keys.EXECUTION_TIMEOUT] ?: 30_000L,
            debugTimeoutMs = prefs[Keys.DEBUG_TIMEOUT] ?: 60_000L,
            autoSaveIntervalMs = prefs[Keys.AUTO_SAVE_INTERVAL] ?: 30_000L
        )
    }

    override suspend fun getEditorSettings(): EditorSettingsState {
        val prefs = context.dataStore.data.first()
        return EditorSettingsState(
            showLineNumbers = prefs[Keys.SHOW_LINE_NUMBERS] ?: true,
            wordWrap = prefs[Keys.WORD_WRAP] ?: true,
            highlightCurrentLine = prefs[Keys.HIGHLIGHT_CURRENT_LINE] ?: true,
            autoIndent = prefs[Keys.AUTO_INDENT] ?: true,
            smartIndent = prefs[Keys.SMART_INDENT] ?: true,
            autoBrackets = prefs[Keys.AUTO_BRACKETS] ?: true,
            autoQuotes = prefs[Keys.AUTO_QUOTES] ?: true,
            tabSize = prefs[Keys.TAB_SIZE] ?: 4,
            indentWithTabs = prefs[Keys.INDENT_WITH_TABS] ?: false,
            showWhitespace = prefs[Keys.SHOW_WHITESPACE] ?: false,
            showEndOfFile = prefs[Keys.SHOW_END_OF_FILE] ?: false,
            bracketPairColorization = prefs[Keys.BRACKET_PAIR_COLORIZATION] ?: true,
            minimap = prefs[Keys.MINIMAP] ?: false,
            stickyScroll = prefs[Keys.STICKY_SCROLL] ?: true,
            fontSize = prefs[Keys.EDITOR_FONT_SIZE] ?: 14
        )
    }

    override suspend fun getConsoleSettings(): ConsoleSettings {
        val prefs = context.dataStore.data.first()
        return ConsoleSettings(
            fontSize = prefs[Keys.CONSOLE_FONT_SIZE] ?: 14,
            fontFamily = try { FontFamily.valueOf(prefs[Keys.CONSOLE_FONT_FAMILY] ?: FontFamily.MONOSPACE.name) } catch (e: Exception) { FontFamily.MONOSPACE },
            showTimestamps = prefs[Keys.SHOW_TIMESTAMPS] ?: true,
            enableAnsiColors = prefs[Keys.ENABLE_ANSI_COLORS] ?: true,
            maxLines = prefs[Keys.MAX_CONSOLE_LINES] ?: 10000,
            autoScroll = prefs[Keys.AUTO_SCROLL] ?: true,
            wordWrap = prefs[Keys.CONSOLE_WORD_WRAP] ?: true
        )
    }

    override suspend fun getPackageSettings(): PackageSettings {
        val prefs = context.dataStore.data.first()
        return PackageSettings(
            autoUpdatePackages = prefs[Keys.AUTO_UPDATE_PACKAGES] ?: false,
            showPrerelease = prefs[Keys.SHOW_PRERELEASE] ?: false,
            cacheTimeoutMinutes = prefs[Keys.CACHE_TIMEOUT] ?: 60,
            maxConcurrentDownloads = prefs[Keys.MAX_CONCURRENT_DOWNLOADS] ?: 3,
            verifySignatures = prefs[Keys.VERIFY_SIGNATURES] ?: true
        )
    }

    override suspend fun getBackupSettings(): BackupSettings {
        val prefs = context.dataStore.data.first()
        return BackupSettings(
            autoBackupEnabled = prefs[Keys.AUTO_BACKUP_ENABLED] ?: true,
            autoBackupIntervalMs = prefs[Keys.AUTO_BACKUP_INTERVAL] ?: 86_400_000L,
            maxBackups = prefs[Keys.MAX_BACKUPS] ?: 5,
            backupSettings = prefs[Keys.BACKUP_SETTINGS] ?: true,
            backupProjects = prefs[Keys.BACKUP_PROJECTS] ?: true,
            backupPackages = prefs[Keys.BACKUP_PACKAGES] ?: false,
            lastBackupTimestamp = prefs[Keys.LAST_BACKUP_TIMESTAMP] ?: 0L
        )
    }

    override suspend fun exportSettings(): String {
        val prefs = context.dataStore.data.first()
        val map = mutableMapOf<String, Any>()
        prefs.asMap().forEach { (key, value) ->
            map[key.name] = value ?: ""
        }
        return org.json.JSONObject(map).toString(2)
    }

    override suspend fun importSettings(json: String): Result<Unit> {
        return try {
            val obj = org.json.JSONObject(json)
            context.dataStore.edit { prefs ->
                obj.keys().forEach { key ->
                    val prefKey = when (key) {
                        "theme_mode" -> Keys.THEME_MODE
                        "dynamic_colors" -> Keys.DYNAMIC_COLORS
                        "amoled_black" -> Keys.AMOLED_BLACK
                        "editor_theme" -> Keys.EDITOR_THEME
                        "font_size" -> Keys.FONT_SIZE
                        "font_family" -> Keys.FONT_FAMILY
                        "line_height" -> Keys.LINE_HEIGHT
                        "execution_timeout" -> Keys.EXECUTION_TIMEOUT
                        "debug_timeout" -> Keys.DEBUG_TIMEOUT
                        "auto_save_interval" -> Keys.AUTO_SAVE_INTERVAL
                        "show_line_numbers" -> Keys.SHOW_LINE_NUMBERS
                        "word_wrap" -> Keys.WORD_WRAP
                        "highlight_current_line" -> Keys.HIGHLIGHT_CURRENT_LINE
                        "auto_indent" -> Keys.AUTO_INDENT
                        "smart_indent" -> Keys.SMART_INDENT
                        "auto_brackets" -> Keys.AUTO_BRACKETS
                        "auto_quotes" -> Keys.AUTO_QUOTES
                        "tab_size" -> Keys.TAB_SIZE
                        "indent_with_tabs" -> Keys.INDENT_WITH_TABS
                        "show_whitespace" -> Keys.SHOW_WHITESPACE
                        "show_end_of_file" -> Keys.SHOW_END_OF_FILE
                        "bracket_pair_colorization" -> Keys.BRACKET_PAIR_COLORIZATION
                        "minimap" -> Keys.MINIMAP
                        "sticky_scroll" -> Keys.STICKY_SCROLL
                        "editor_font_size" -> Keys.EDITOR_FONT_SIZE
                        "console_font_size" -> Keys.CONSOLE_FONT_SIZE
                        "console_font_family" -> Keys.CONSOLE_FONT_FAMILY
                        "show_timestamps" -> Keys.SHOW_TIMESTAMPS
                        "enable_ansi_colors" -> Keys.ENABLE_ANSI_COLORS
                        "max_console_lines" -> Keys.MAX_CONSOLE_LINES
                        "auto_scroll" -> Keys.AUTO_SCROLL
                        "console_word_wrap" -> Keys.CONSOLE_WORD_WRAP
                        "auto_update_packages" -> Keys.AUTO_UPDATE_PACKAGES
                        "show_prerelease" -> Keys.SHOW_PRERELEASE
                        "cache_timeout" -> Keys.CACHE_TIMEOUT
                        "max_concurrent_downloads" -> Keys.MAX_CONCURRENT_DOWNLOADS
                        "verify_signatures" -> Keys.VERIFY_SIGNATURES
                        "auto_backup_enabled" -> Keys.AUTO_BACKUP_ENABLED
                        "auto_backup_interval" -> Keys.AUTO_BACKUP_INTERVAL
                        "max_backups" -> Keys.MAX_BACKUPS
                        "backup_settings" -> Keys.BACKUP_SETTINGS
                        "backup_projects" -> Keys.BACKUP_PROJECTS
                        "backup_packages" -> Keys.BACKUP_PACKAGES
                        "last_backup_timestamp" -> Keys.LAST_BACKUP_TIMESTAMP
                        else -> null
                    }
                    if (prefKey != null) {
                        when (val value = obj.get(key)) {
                            is Boolean -> prefs[prefKey as Preferences.Key<Boolean>] = value
                            is Int -> prefs[prefKey as Preferences.Key<Int>] = value
                            is Long -> prefs[prefKey as Preferences.Key<Long>] = value
                            is Float -> prefs[prefKey as Preferences.Key<Float>] = value
                            is String -> prefs[prefKey as Preferences.Key<String>] = value
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetAll() { context.dataStore.edit { it.clear() } }
    override suspend fun resetTheme() { context.dataStore.edit { prefs -> listOf(Keys.THEME_MODE, Keys.DYNAMIC_COLORS, Keys.AMOLED_BLACK, Keys.EDITOR_THEME).forEach { prefs.remove(it) } } }
    override suspend fun resetFont() { context.dataStore.edit { prefs -> listOf(Keys.FONT_SIZE, Keys.FONT_FAMILY, Keys.LINE_HEIGHT).forEach { prefs.remove(it) } } }
    override suspend fun resetEditor() { context.dataStore.edit { prefs -> listOf(Keys.SHOW_LINE_NUMBERS, Keys.WORD_WRAP, Keys.HIGHLIGHT_CURRENT_LINE, Keys.AUTO_INDENT, Keys.SMART_INDENT, Keys.AUTO_BRACKETS, Keys.AUTO_QUOTES, Keys.TAB_SIZE, Keys.INDENT_WITH_TABS, Keys.SHOW_WHITESPACE, Keys.SHOW_END_OF_FILE, Keys.BRACKET_PAIR_COLORIZATION, Keys.MINIMAP, Keys.STICKY_SCROLL, Keys.EDITOR_FONT_SIZE).forEach { prefs.remove(it) } } }
    override suspend fun resetConsole() { context.dataStore.edit { prefs -> listOf(Keys.CONSOLE_FONT_SIZE, Keys.CONSOLE_FONT_FAMILY, Keys.SHOW_TIMESTAMPS, Keys.ENABLE_ANSI_COLORS, Keys.MAX_CONSOLE_LINES, Keys.AUTO_SCROLL, Keys.CONSOLE_WORD_WRAP).forEach { prefs.remove(it) } } }
    override suspend fun resetPackages() { context.dataStore.edit { prefs -> listOf(Keys.AUTO_UPDATE_PACKAGES, Keys.SHOW_PRERELEASE, Keys.CACHE_TIMEOUT, Keys.MAX_CONCURRENT_DOWNLOADS, Keys.VERIFY_SIGNATURES).forEach { prefs.remove(it) } } }
    override suspend fun resetBackup() { context.dataStore.edit { prefs -> listOf(Keys.AUTO_BACKUP_ENABLED, Keys.AUTO_BACKUP_INTERVAL, Keys.MAX_BACKUPS, Keys.BACKUP_SETTINGS, Keys.BACKUP_PROJECTS, Keys.BACKUP_PACKAGES, Keys.LAST_BACKUP_TIMESTAMP).forEach { prefs.remove(it) } } }
}
