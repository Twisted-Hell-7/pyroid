package com.pythonide.domain.model.editor

data class EditorState(
    val content: String = "",
    val cursorPosition: CursorPosition = CursorPosition(0, 0),
    val selection: Selection? = null,
    val lines: List<String> = emptyList(),
    val undoStack: List<EditorAction> = emptyList(),
    val redoStack: List<EditorAction> = emptyList(),
    val isModified: Boolean = false,
    val wordWrap: Boolean = true,
    val fontSize: Int = 14,
    val tabSize: Int = 4,
    val showLineNumbers: Boolean = true,
    val highlightCurrentLine: Boolean = true,
    val autoIndent: Boolean = true,
    val smartIndent: Boolean = true,
    val autoBrackets: Boolean = true,
    val autoQuotes: Boolean = true,
    val isReadOnly: Boolean = false
)

data class CursorPosition(
    val line: Int,
    val column: Int
) : Comparable<CursorPosition> {
    fun toOffset(content: String): Int {
        if (content.isEmpty()) return 0
        // Normalize CRLF so offsets match lines() indexing.
        val normalized = content.replace("\r\n", "\n").replace('\r', '\n')
        val lines = normalized.lines()
        val safeLine = line.coerceIn(0, lines.size - 1)
        var offset = 0
        for (i in 0 until safeLine) {
            offset += lines[i].length + 1
        }
        val safeColumn = column.coerceIn(0, lines.getOrNull(safeLine)?.length ?: 0)
        return (offset + safeColumn).coerceIn(0, normalized.length)
    }

    override fun compareTo(other: CursorPosition): Int {
        return when {
            line != other.line -> line - other.line
            else -> column - other.column
        }
    }

    companion object {
        fun fromOffset(content: String, offset: Int): CursorPosition {
            if (content.isEmpty()) return CursorPosition(0, 0)
            val normalized = content.replace("\r\n", "\n").replace('\r', '\n')
            val safeOffset = offset.coerceIn(0, normalized.length)
            val lines = normalized.lines()
            var currentOffset = 0
            for ((index, line) in lines.withIndex()) {
                if (currentOffset + line.length >= safeOffset) {
                    return CursorPosition(index, (safeOffset - currentOffset).coerceIn(0, line.length))
                }
                currentOffset += line.length + 1
            }
            return if (lines.isEmpty()) {
                CursorPosition(0, 0)
            } else {
                CursorPosition(lines.size - 1, lines.last().length)
            }
        }
    }
}

data class Selection(
    val start: CursorPosition,
    val end: CursorPosition
) {
    val isValid: Boolean get() = start != end
    
    fun normalize(): Selection {
        return if (start > end) Selection(end, start) else this
    }
    
    fun contains(position: CursorPosition): Boolean {
        val normalized = normalize()
        return position >= normalized.start && position <= normalized.end
    }
}

sealed class EditorAction {
    data class Insert(val position: CursorPosition, val text: String) : EditorAction()
    data class Delete(val position: CursorPosition, val text: String, val length: Int) : EditorAction()
    data class Replace(val position: CursorPosition, val oldText: String, val newText: String) : EditorAction()
}

data class EditorConfig(
    val fontSize: Int = 14,
    val tabSize: Int = 4,
    val wordWrap: Boolean = true,
    val showLineNumbers: Boolean = true,
    val highlightCurrentLine: Boolean = true,
    val autoIndent: Boolean = true,
    val smartIndent: Boolean = true,
    val autoBrackets: Boolean = true,
    val autoQuotes: Boolean = true,
    val theme: EditorTheme = EditorTheme.DEFAULT
)

enum class EditorTheme(
    val backgroundArgb: Long,
    val foregroundArgb: Long,
    val lineNumberArgb: Long,
    val lineNumberBackgroundArgb: Long,
    val currentLineArgb: Long,
    val selectionArgb: Long,
    val keywordArgb: Long,
    val stringArgb: Long,
    val numberArgb: Long,
    val commentArgb: Long,
    val functionArgb: Long,
    val classNameArgb: Long,
    val operatorArgb: Long,
    val punctuationArgb: Long,
    val builtinArgb: Long,
    val decoratorArgb: Long
) {
    DEFAULT(
        backgroundArgb = 0xFF1E1E1E,
        foregroundArgb = 0xFFD4D4D4,
        lineNumberArgb = 0xFF858585,
        lineNumberBackgroundArgb = 0xFF252526,
        currentLineArgb = 0xFF2A2D2E,
        selectionArgb = 0xFF264F78,
        keywordArgb = 0xFF569CD6,
        stringArgb = 0xFFCE9178,
        numberArgb = 0xFFB5CEA8,
        commentArgb = 0xFF6A9955,
        functionArgb = 0xFFDCDCAA,
        classNameArgb = 0xFF4EC9B0,
        operatorArgb = 0xFFD4D4D4,
        punctuationArgb = 0xFFD4D4D4,
        builtinArgb = 0xFF4FC1FF,
        decoratorArgb = 0xFFDCDCAA
    ),
    MONOKAI(
        backgroundArgb = 0xFF272822,
        foregroundArgb = 0xFFF8F8F2,
        lineNumberArgb = 0xFF90908A,
        lineNumberBackgroundArgb = 0xFF272822,
        currentLineArgb = 0xFF3E3D32,
        selectionArgb = 0xFF49483E,
        keywordArgb = 0xFFF92672,
        stringArgb = 0xFFE6DB74,
        numberArgb = 0xFFAE81FF,
        commentArgb = 0xFF75715E,
        functionArgb = 0xFFA6E22E,
        classNameArgb = 0xFFA6E22E,
        operatorArgb = 0xFFF92672,
        punctuationArgb = 0xFFF8F8F2,
        builtinArgb = 0xFF66D9EF,
        decoratorArgb = 0xFFA6E22E
    ),
    SOLARIZED_DARK(
        backgroundArgb = 0xFF002B36,
        foregroundArgb = 0xFF839496,
        lineNumberArgb = 0xFF586E75,
        lineNumberBackgroundArgb = 0xFF002B36,
        currentLineArgb = 0xFF073642,
        selectionArgb = 0xFF073642,
        keywordArgb = 0xFF859900,
        stringArgb = 0xFF2AA198,
        numberArgb = 0xFFD33682,
        commentArgb = 0xFF586E75,
        functionArgb = 0xFF268BD2,
        classNameArgb = 0xFFB58900,
        operatorArgb = 0xFF859900,
        punctuationArgb = 0xFF839496,
        builtinArgb = 0xFF268BD2,
        decoratorArgb = 0xFFB58900
    ),
    DRACULA(
        backgroundArgb = 0xFF282A36,
        foregroundArgb = 0xFFF8F8F2,
        lineNumberArgb = 0xFF6272A4,
        lineNumberBackgroundArgb = 0xFF282A36,
        currentLineArgb = 0xFF44475A,
        selectionArgb = 0xFF44475A,
        keywordArgb = 0xFFFF79C6,
        stringArgb = 0xFFF1FA8C,
        numberArgb = 0xFFBD93F9,
        commentArgb = 0xFF6272A4,
        functionArgb = 0xFF50FA7B,
        classNameArgb = 0xFF8BE9FD,
        operatorArgb = 0xFFFF79C6,
        punctuationArgb = 0xFFF8F8F2,
        builtinArgb = 0xFF8BE9FD,
        decoratorArgb = 0xFF50FA7B
    ),
    GITHUB_DARK(
        backgroundArgb = 0xFF0D1117,
        foregroundArgb = 0xFFC9D1D9,
        lineNumberArgb = 0xFF484F58,
        lineNumberBackgroundArgb = 0xFF0D1117,
        currentLineArgb = 0xFF161B22,
        selectionArgb = 0xFF264F78,
        keywordArgb = 0xFFFF7B72,
        stringArgb = 0xFFA5D6FF,
        numberArgb = 0xFF79C0FF,
        commentArgb = 0xFF8B949E,
        functionArgb = 0xFFD2A8FF,
        classNameArgb = 0xFF7EE787,
        operatorArgb = 0xFFFF7B72,
        punctuationArgb = 0xFFC9D1D9,
        builtinArgb = 0xFF79C0FF,
        decoratorArgb = 0xFFD2A8FF
    ),
    AMOLED(
        backgroundArgb = 0xFF000000,
        foregroundArgb = 0xFFE0E0E0,
        lineNumberArgb = 0xFF555555,
        lineNumberBackgroundArgb = 0xFF000000,
        currentLineArgb = 0xFF1A1A1A,
        selectionArgb = 0xFF1A3A5C,
        keywordArgb = 0xFF569CD6,
        stringArgb = 0xFFCE9178,
        numberArgb = 0xFFB5CEA8,
        commentArgb = 0xFF6A9955,
        functionArgb = 0xFFDCDCAA,
        classNameArgb = 0xFF4EC9B0,
        operatorArgb = 0xFFE0E0E0,
        punctuationArgb = 0xFFE0E0E0,
        builtinArgb = 0xFF4FC1FF,
        decoratorArgb = 0xFFDCDCAA
    );
}

data class SearchState(
    val query: String = "",
    val replacement: String = "",
    val isCaseSensitive: Boolean = false,
    val isRegex: Boolean = false,
    val isWholeWord: Boolean = false,
    val matches: List<SearchMatch> = emptyList(),
    val currentMatchIndex: Int = -1,
    val isOpen: Boolean = false
)

data class SearchMatch(
    val start: CursorPosition,
    val end: CursorPosition,
    val text: String
)

data class Tab(
    val id: String,
    val title: String,
    val content: String,
    val isModified: Boolean = false,
    val cursorPosition: CursorPosition = CursorPosition(0, 0),
    val scrollOffset: Int = 0
)

data class EditorTabState(
    val tabs: List<Tab> = emptyList(),
    val activeTabId: String? = null,
    val splitMode: SplitMode = SplitMode.NONE
)

enum class SplitMode {
    NONE,
    HORIZONTAL,
    VERTICAL
}

data class CursorInfo(
    val line: Int,
    val column: Int,
    val selection: String? = null,
    val selectedLength: Int = 0
)
