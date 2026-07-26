package com.pythonide.data.editor.core

import com.pythonide.domain.model.editor.EditorState

class BracketHandler {
    
    private val bracketPairs = mapOf(
        '(' to ')',
        '[' to ']',
        '{' to '}'
    )
    
    private val quotePairs = mapOf(
        '\'' to '\'',
        '"' to '"'
    )
    
    private val closingBrackets = setOf(')', ']', '}')
    private val openingBrackets = setOf('(', '[', '{')
    
    fun handleCharacter(state: EditorState, char: Char): BracketResult? {
        if (!state.autoBrackets && !state.autoQuotes) return null
        
        val position = state.cursorPosition
        val line = state.lines.getOrNull(position.line) ?: return null
        
        return when {
            char in openingBrackets && state.autoBrackets -> {
                handleOpeningBracket(line, position.column, char)
            }
            char in closingBrackets && state.autoBrackets -> {
                handleClosingBracket(line, position.column, char)
            }
            char == '\'' || char == '"' -> {
                handleQuote(state, char)
            }
            else -> null
        }
    }
    
    private fun handleOpeningBracket(line: String, column: Int, char: Char): BracketResult {
        val closing = bracketPairs[char] ?: return BracketResult(char.toString(), 0)
        
        val before = line.substring(0, column)
        val after = line.substring(column)
        
        val shouldClose = when {
            after.isEmpty() -> true
            after[0].isWhitespace() -> true
            after[0] in closingBrackets -> true
            after[0] == ',' -> true
            else -> false
        }
        
        return if (shouldClose) {
            BracketResult("$char$closing", -1)
        } else {
            BracketResult(char.toString(), 0)
        }
    }
    
    private fun handleClosingBracket(line: String, column: Int, char: Char): BracketResult {
        if (column < line.length && line[column] == char) {
            val openBracket = bracketPairs.entries.find { it.value == char }?.key
            if (openBracket != null) {
                val before = line.substring(0, column)
                val depth = before.count { it == openBracket } - before.count { it == char }
                if (depth > 0) {
                    return BracketResult(char.toString(), 1)
                }
            }
        }
        return BracketResult(char.toString(), 0)
    }
    
    private fun handleQuote(state: EditorState, char: Char): BracketResult {
        val position = state.cursorPosition
        val line = state.lines.getOrNull(position.line) ?: return BracketResult(char.toString(), 0)
        
        val before = line.substring(0, position.column)
        val after = line.substring(position.column)
        
        val charCount = before.count { it == char }
        
        return when {
            charCount % 2 == 1 -> {
                if (after.isNotEmpty() && after[0] == char) {
                    BracketResult(char.toString(), 1)
                } else {
                    BracketResult(char.toString(), 0)
                }
            }
            state.autoQuotes -> {
                val tripleQuote = char.toString().repeat(3)
                if (after.startsWith(tripleQuote)) {
                    BracketResult(tripleQuote, 3)
                } else {
                    BracketResult("$char$char", -1)
                }
            }
            else -> BracketResult(char.toString(), 0)
        }
    }
    
    fun shouldAutoCloseAfter(state: EditorState, char: Char, nextChar: Char?): Boolean {
        if (nextChar == null) return false
        
        return when (char) {
            '(', '[', '{' -> nextChar.isWhitespace() || nextChar in closingBrackets || nextChar == ','
            '\'', '"' -> nextChar.isWhitespace() || nextChar in closingBrackets || nextChar == ',' || nextChar == ':'
            else -> false
        }
    }
    
    fun getMatchingBracket(state: EditorState, position: Int): Int? {
        val content = state.content
        if (position < 0 || position >= content.length) return null
        
        val char = content[position]
        val isOpening = char in openingBrackets
        val isClosing = char in closingBrackets
        
        if (!isOpening && !isClosing) return null
        
        val target = if (isOpening) bracketPairs[char] else bracketPairs.entries.find { it.value == char }?.key
            ?: return null
        
        var depth = 0
        if (isOpening) {
            for (i in position until content.length) {
                val c = content[i]
                when {
                    c == char -> depth++
                    c == target -> {
                        depth--
                        if (depth == 0) return i
                    }
                }
            }
        } else {
            for (i in position downTo 0) {
                val c = content[i]
                when {
                    c == char -> depth++
                    c == target -> {
                        depth--
                        if (depth == 0) return i
                    }
                }
            }
        }
        
        return null
    }
    
    fun isBalanced(content: String): Boolean {
        val stack = ArrayDeque<Char>()
        
        for (char in content) {
            when {
                char in openingBrackets -> stack.addLast(char)
                char in closingBrackets -> {
                    val expected = bracketPairs[stack.lastOrNull()] ?: return false
                    if (char != expected) return false
                    stack.removeLast()
                }
            }
        }
        
        return stack.isEmpty()
    }
}

data class BracketResult(
    val text: String,
    val cursorOffset: Int
)
