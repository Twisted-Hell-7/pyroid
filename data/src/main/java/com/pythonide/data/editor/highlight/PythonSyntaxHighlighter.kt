package com.pythonide.data.editor.highlight

import androidx.compose.ui.graphics.Color
import com.pythonide.domain.model.editor.EditorTheme

data class SyntaxToken(
    val text: String,
    val start: Int,
    val end: Int,
    val type: TokenType
)

enum class TokenType {
    KEYWORD,
    STRING,
    NUMBER,
    COMMENT,
    FUNCTION,
    CLASS_NAME,
    OPERATOR,
    PUNCTUATION,
    BUILTIN,
    DECORATOR,
    WHITESPACE,
    TEXT
}

class PythonSyntaxHighlighter(private val theme: EditorTheme = EditorTheme.DEFAULT) {

    private val keywords = setOf(
        "False", "None", "True", "and", "as", "assert", "async", "await",
        "break", "class", "continue", "def", "del", "elif", "else", "except",
        "finally", "for", "from", "global", "if", "import", "in", "is",
        "lambda", "nonlocal", "not", "or", "pass", "raise", "return",
        "try", "while", "with", "yield"
    )

    private val builtins = setOf(
        "abs", "all", "any", "bin", "bool", "bytearray", "bytes", "callable",
        "chr", "classmethod", "compile", "complex", "delattr", "dict", "dir",
        "divmod", "enumerate", "eval", "exec", "filter", "float", "format",
        "frozenset", "getattr", "globals", "hasattr", "hash", "help", "hex",
        "id", "input", "int", "isinstance", "issubclass", "iter", "len",
        "list", "locals", "map", "max", "memoryview", "min", "next",
        "object", "oct", "open", "ord", "pow", "print", "property",
        "range", "repr", "reversed", "round", "set", "setattr", "slice",
        "sorted", "staticmethod", "str", "sum", "super", "tuple", "type",
        "vars", "zip", "__import__"
    )

    private val operators = setOf(
        "+", "-", "*", "/", "//", "%", "**", "=", "==", "!=", "<", ">",
        "<=", ">=", "+=", "-=", "*=", "/=", "//=", "%=", "**=",
        "&", "|", "^", "~", "<<", ">>", "&&", "||",
        "->", ":=", "@"
    )

    private val punctuation = setOf(
        "(", ")", "[", "]", "{", "}", ",", ":", ".", ";", "@"
    )

    fun highlight(line: String): List<SyntaxToken> {
        val tokens = mutableListOf<SyntaxToken>()
        var i = 0

        while (i < line.length) {
            val remaining = line.substring(i)

            when {
                remaining.startsWith("#") -> {
                    val end = line.length
                    tokens.add(SyntaxToken(line.substring(i, end), i, end, TokenType.COMMENT))
                    i = end
                }
                remaining.startsWith("\"\"\"") || remaining.startsWith("'''") -> {
                    val quote = remaining.substring(0, 3)
                    val end = findTripleQuoteEnd(line, i + 3, quote[0])
                    tokens.add(SyntaxToken(line.substring(i, end), i, end, TokenType.STRING))
                    i = end
                }
                remaining.startsWith("\"") || remaining.startsWith("'") -> {
                    val end = findStringEnd(line, i + 1, line[i])
                    tokens.add(SyntaxToken(line.substring(i, end), i, end, TokenType.STRING))
                    i = end
                }
                remaining.startsWith("@") -> {
                    val end = findWordEnd(line, i + 1)
                    tokens.add(SyntaxToken(line.substring(i, end), i, end, TokenType.DECORATOR))
                    i = end
                }
                line[i].isDigit() -> {
                    val end = findNumberEnd(line, i)
                    tokens.add(SyntaxToken(line.substring(i, end), i, end, TokenType.NUMBER))
                    i = end
                }
                line[i].isLetter() || line[i] == '_' -> {
                    val end = findWordEnd(line, i)
                    val word = line.substring(i, end)
                    val type = when {
                        word in keywords -> TokenType.KEYWORD
                        word in builtins -> TokenType.BUILTIN
                        end < line.length && line[end] == '(' -> TokenType.FUNCTION
                        i > 0 && line[i - 1] == '.' -> TokenType.FUNCTION
                        else -> TokenType.TEXT
                    }
                    tokens.add(SyntaxToken(word, i, end, type))
                    i = end
                }
                line[i].isWhitespace() -> {
                    val end = findWhitespaceEnd(line, i)
                    tokens.add(SyntaxToken(line.substring(i, end), i, end, TokenType.WHITESPACE))
                    i = end
                }
                line[i].toString() in operators -> {
                    val end = findOperatorEnd(line, i)
                    tokens.add(SyntaxToken(line.substring(i, end), i, end, TokenType.OPERATOR))
                    i = end
                }
                line[i].toString() in punctuation -> {
                    tokens.add(SyntaxToken(line[i].toString(), i, i + 1, TokenType.PUNCTUATION))
                    i++
                }
                else -> {
                    tokens.add(SyntaxToken(line[i].toString(), i, i + 1, TokenType.TEXT))
                    i++
                }
            }
        }

        return tokens
    }

    private fun findTripleQuoteEnd(line: String, start: Int, quote: Char): Int {
        var i = start
        while (i < line.length - 2) {
            if (line[i] == quote && line[i + 1] == quote && line[i + 2] == quote) {
                return i + 3
            }
            i++
        }
        return line.length
    }

    private fun findStringEnd(line: String, start: Int, quote: Char): Int {
        var i = start
        while (i < line.length) {
            when (line[i]) {
                '\\' -> i += 2
                quote -> return i + 1
                else -> i++
            }
        }
        return line.length
    }

    private fun findWordEnd(line: String, start: Int): Int {
        var i = start
        while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_')) {
            i++
        }
        return i
    }

    private fun findNumberEnd(line: String, start: Int): Int {
        var i = start
        while (i < line.length && (line[i].isDigit() || line[i] == '.' || line[i] in "xXeEoObBlL")) {
            i++
        }
        return i
    }

    private fun findWhitespaceEnd(line: String, start: Int): Int {
        var i = start
        while (i < line.length && line[i].isWhitespace()) {
            i++
        }
        return i
    }

    private fun findOperatorEnd(line: String, start: Int): Int {
        var i = start
        while (i < line.length && line[i].toString() in operators) {
            i++
        }
        return i
    }

    fun getColorForToken(type: TokenType): Color {
        return when (type) {
            TokenType.KEYWORD -> theme.keywordColor
            TokenType.STRING -> theme.stringColor
            TokenType.NUMBER -> theme.numberColor
            TokenType.COMMENT -> theme.commentColor
            TokenType.FUNCTION -> theme.functionColor
            TokenType.CLASS_NAME -> theme.classNameColor
            TokenType.OPERATOR -> theme.operatorColor
            TokenType.PUNCTUATION -> theme.punctuationColor
            TokenType.BUILTIN -> theme.builtinColor
            TokenType.DECORATOR -> theme.decoratorColor
            TokenType.WHITESPACE -> theme.foreground
            TokenType.TEXT -> theme.foreground
        }
    }

    fun highlightLine(line: String): List<Pair<String, Color>> {
        return highlight(line).map { token ->
            token.text to getColorForToken(token.type)
        }
    }
}
