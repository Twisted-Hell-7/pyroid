package com.pythonide.domain.model.terminal

import java.util.UUID

data class Terminal(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Terminal ${id.take(4)}",
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

data class TerminalEntry(
    val id: String = UUID.randomUUID().toString(),
    val terminalId: String,
    val type: TerminalEntryType,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val ansiFormatted: List<AnsiSegment> = emptyList()
)

enum class TerminalEntryType {
    INPUT,
    OUTPUT,
    ERROR,
    SYSTEM,
    TIMESTAMP
}

data class AnsiSegment(
    val text: String,
    val foreground: AnsiColor = AnsiColor.DEFAULT,
    val background: AnsiColor = AnsiColor.DEFAULT,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val dim: Boolean = false,
    val strikethrough: Boolean = false
)

enum class AnsiColor(val code: Int, val hex: Long) {
    DEFAULT(-1, 0xFFCCCCCC),
    BLACK(0, 0xFF000000),
    RED(1, 0xFFCD3131),
    GREEN(2, 0xFF0DBC79),
    YELLOW(3, 0xFFF4D034),
    BLUE(4, 0xFF2472C8),
    MAGENTA(5, 0xFFBC3FBC),
    CYAN(6, 0xFF11A8CD),
    WHITE(7, 0xFFE5E5E5),
    BRIGHT_BLACK(8, 0xFF666666),
    BRIGHT_RED(9, 0xFFF14C4C),
    BRIGHT_GREEN(10, 0xFF23D18B),
    BRIGHT_YELLOW(11, 0xFFFFF437),
    BRIGHT_BLUE(12, 0xFF3B8EEA),
    BRIGHT_MAGENTA(13, 0xFFD670D6),
    BRIGHT_CYAN(14, 0xFF29B8DB),
    BRIGHT_WHITE(15, 0xFFFFFFFF);

    companion object {
        fun fromCode(code: Int): AnsiColor {
            return entries.find { it.code == code } ?: DEFAULT
        }

        fun from256(code: Int): AnsiColor {
            return when {
                code < 0 -> DEFAULT
                code < 8 -> entries[code + 1]
                code < 16 -> entries[code - 7]
                else -> {
                    val r = ((code - 16) / 36) * 51
                    val g = (((code - 16) % 36) / 6) * 51
                    val b = ((code - 16) % 6) * 51
                    DEFAULT
                }
            }
        }
    }
}

data class TerminalConfig(
    val showTimestamps: Boolean = true,
    val fontSize: Int = 13,
    val maxLines: Int = 10000,
    val enableColors: Boolean = true,
    val enableBell: Boolean = false,
    val scrollbackLines: Int = 5000
)

data class TerminalState(
    val terminals: List<Terminal> = listOf(Terminal()),
    val activeTerminalId: String = terminals.firstOrNull()?.id ?: "",
    val entries: Map<String, List<TerminalEntry>> = emptyMap(),
    val commandHistory: List<String> = emptyList(),
    val historyIndex: Int = -1,
    val currentInput: String = "",
    val isExecuting: Boolean = false,
    val config: TerminalConfig = TerminalConfig()
) {
    val activeTerminal: Terminal?
        get() = terminals.find { it.id == activeTerminalId }

    val activeEntries: List<TerminalEntry>
        get() = entries[activeTerminalId] ?: emptyList()
}
