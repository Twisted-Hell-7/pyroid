package com.pythonide.data.editor.core

import com.pythonide.domain.model.editor.CursorPosition
import com.pythonide.domain.model.editor.EditorState

class IndentationHandler {
    
    fun handleTab(state: EditorState): String {
        val position = state.cursorPosition
        val line = state.lines.getOrNull(position.line) ?: return ""
        
        val indent = getIndentation(line)
        val spaces = " ".repeat(state.tabSize - (indent.length % state.tabSize))
        
        return spaces
    }

    fun handleEnter(state: EditorState): String {
        val position = state.cursorPosition
        val line = state.lines.getOrNull(position.line) ?: return "\n"
        
        val currentIndent = getIndentation(line)
        val trimmedLine = line.trimEnd()
        
        val extraIndent = when {
            trimmedLine.endsWith(":") && (trimmedLine.startsWith("def ") || 
                    trimmedLine.startsWith("class ") || 
                    trimmedLine.startsWith("if ") || 
                    trimmedLine.startsWith("elif ") || 
                    trimmedLine.startsWith("else") || 
                    trimmedLine.startsWith("for ") || 
                    trimmedLine.startsWith("while ") || 
                    trimmedLine.startsWith("with ") || 
                    trimmedLine.startsWith("try") || 
                    trimmedLine.startsWith("except") || 
                    trimmedLine.startsWith("finally")) -> " ".repeat(state.tabSize)
            trimmedLine.endsWith(":") -> " ".repeat(state.tabSize)
            else -> ""
        }
        
        return "\n$currentIndent$extraIndent"
    }

    fun handleBackspace(state: EditorState): String? {
        val position = state.cursorPosition
        if (position.column == 0) return null
        
        val line = state.lines.getOrNull(position.line) ?: return null
        val beforeCursor = line.substring(0, position.column)
        
        if (beforeCursor.all { it == ' ' }) {
            val indentLength = beforeCursor.length
            val removeCount = when {
                indentLength % state.tabSize == 0 -> state.tabSize
                else -> indentLength % state.tabSize
            }
            return "\b".repeat(removeCount)
        }
        
        return "\b"
    }

    fun getIndentation(line: String): String {
        return line.takeWhile { it == ' ' }
    }

    fun calculateSmartIndent(state: EditorState): String {
        val position = state.cursorPosition
        if (position.line == 0) return ""
        
        val previousLine = state.lines.getOrNull(position.line - 1) ?: return ""
        val currentLine = state.lines.getOrNull(position.line) ?: ""
        
        val previousIndent = getIndentation(previousLine)
        val trimmedPrevious = previousLine.trimEnd()
        
        return when {
            trimmedPrevious.endsWith(":") -> {
                previousIndent + " ".repeat(state.tabSize)
            }
            trimmedPrevious.startsWith("return ") || 
            trimmedPrevious.startsWith("break") || 
            trimmedPrevious.startsWith("continue") -> {
                val parentIndent = getParentIndent(state, position.line - 1)
                parentIndent ?: previousIndent
            }
            currentLine.trimStart().startsWith(")") || 
            currentLine.trimStart().startsWith("]") || 
            currentLine.trimStart().startsWith("}") -> {
                val decreased = previousIndent.length - state.tabSize
                if (decreased > 0) " ".repeat(decreased) else ""
            }
            else -> previousIndent
        }
    }

    private fun getParentIndent(state: EditorState, fromLine: Int): String? {
        for (i in fromLine downTo 0) {
            val line = state.lines.getOrNull(i) ?: continue
            val trimmed = line.trimEnd()
            if (trimmed.endsWith(":")) {
                return getIndentation(line)
            }
        }
        return null
    }

    fun autoIndentOnCharacter(state: EditorState, char: Char): String? {
        if (!state.autoIndent) return null
        
        val position = state.cursorPosition
        val line = state.lines.getOrNull(position.line) ?: return null
        
        return when (char) {
            ':' -> {
                val trimmed = line.trimEnd()
                if (trimmed.startsWith("def ") || 
                    trimmed.startsWith("class ") || 
                    trimmed.startsWith("if ") || 
                    trimmed.startsWith("elif ") || 
                    trimmed.startsWith("else") || 
                    trimmed.startsWith("for ") || 
                    trimmed.startsWith("while ") || 
                    trimmed.startsWith("with ") || 
                    trimmed.startsWith("try") || 
                    trimmed.startsWith("except") || 
                    trimmed.startsWith("finally")) {
                    "\n" + calculateSmartIndent(state)
                } else {
                    null
                }
            }
            '\n' -> {
                "\n" + calculateSmartIndent(state)
            }
            else -> null
        }
    }
}
