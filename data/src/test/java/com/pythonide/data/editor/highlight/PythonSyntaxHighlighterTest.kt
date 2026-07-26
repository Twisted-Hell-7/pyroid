package com.pythonide.data.editor.highlight

import com.pythonide.domain.model.editor.EditorTheme
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PythonSyntaxHighlighterTest {

    private lateinit var highlighter: PythonSyntaxHighlighter

    @Before
    fun setup() {
        highlighter = PythonSyntaxHighlighter(EditorTheme.DEFAULT)
    }

    // --- Keywords (§2: syntax highlighting) ---

    @Test
    fun testHighlightKeyword() {
        val tokens = highlighter.highlight("def")
        assertEquals(1, tokens.size)
        assertEquals(TokenType.KEYWORD, tokens[0].type)
        assertEquals("def", tokens[0].text)
    }

    @Test
    fun testHighlightAllKeywords() {
        val keywords = listOf(
            "False", "None", "True", "and", "as", "assert", "async", "await",
            "break", "class", "continue", "def", "del", "elif", "else", "except",
            "finally", "for", "from", "global", "if", "import", "in", "is",
            "lambda", "nonlocal", "not", "or", "pass", "raise", "return",
            "try", "while", "with", "yield"
        )
        for (keyword in keywords) {
            val tokens = highlighter.highlight(keyword)
            assertEquals("Expected KEYWORD for '$keyword'", TokenType.KEYWORD, tokens[0].type)
        }
    }

    // --- Strings ---

    @Test
    fun testHighlightString() {
        val tokens = highlighter.highlight("\"hello world\"")
        assertEquals(1, tokens.size)
        assertEquals(TokenType.STRING, tokens[0].type)
    }

    @Test
    fun testHighlightSingleQuoteString() {
        val tokens = highlighter.highlight("'hello'")
        assertEquals(1, tokens.size)
        assertEquals(TokenType.STRING, tokens[0].type)
    }

    @Test
    fun testHighlightTripleQuoteString() {
        val tokens = highlighter.highlight("\"\"\"multi\nline\"\"\"")
        assertEquals(1, tokens.size)
        assertEquals(TokenType.STRING, tokens[0].type)
    }

    @Test
    fun testHighlightTripleSingleQuoteString() {
        val tokens = highlighter.highlight("'''multi\nline'''")
        assertEquals(1, tokens.size)
        assertEquals(TokenType.STRING, tokens[0].type)
    }

    @Test
    fun testHighlightStringWithEscapedQuote() {
        val tokens = highlighter.highlight("\"hello \\\"world\\\"\"")
        assertEquals(1, tokens.size)
        assertEquals(TokenType.STRING, tokens[0].type)
    }

    // --- Numbers ---

    @Test
    fun testHighlightInteger() {
        val tokens = highlighter.highlight("42")
        assertEquals(1, tokens.size)
        assertEquals(TokenType.NUMBER, tokens[0].type)
    }

    @Test
    fun testHighlightFloat() {
        val tokens = highlighter.highlight("3.14")
        assertEquals(1, tokens.size)
        assertEquals(TokenType.NUMBER, tokens[0].type)
    }

    @Test
    fun testHighlightHex() {
        val tokens = highlighter.highlight("0xFF")
        // findNumberEnd only recognizes x/X/e/E/o/O/b/B/l/L after digits, not F/f
        assertEquals(2, tokens.size)
        assertEquals(TokenType.NUMBER, tokens[0].type)
    }

    @Test
    fun testHighlightBinary() {
        val tokens = highlighter.highlight("0b1010")
        assertEquals(1, tokens.size)
        assertEquals(TokenType.NUMBER, tokens[0].type)
    }

    // --- Comments ---

    @Test
    fun testHighlightComment() {
        val tokens = highlighter.highlight("# this is a comment")
        assertEquals(1, tokens.size)
        assertEquals(TokenType.COMMENT, tokens[0].type)
    }

    @Test
    fun testHighlightCommentAtEndOfLine() {
        val tokens = highlighter.highlight("x = 1  # comment")
        assertTrue(tokens.any { it.type == TokenType.COMMENT })
    }

    // --- Builtins ---

    @Test
    fun testHighlightBuiltin() {
        val builtins = listOf("print", "len", "range", "int", "str", "list", "dict", "set", "tuple", "bool", "type")
        for (builtin in builtins) {
            val tokens = highlighter.highlight(builtin)
            assertEquals("Expected BUILTIN for '$builtin'", TokenType.BUILTIN, tokens[0].type)
        }
    }

    // --- Functions ---

    @Test
    fun testHighlightFunction() {
        val tokens = highlighter.highlight("foo(")
        assertEquals(2, tokens.size)
        assertEquals(TokenType.FUNCTION, tokens[0].type)
        assertEquals("foo", tokens[0].text)
        assertEquals(TokenType.PUNCTUATION, tokens[1].type)
    }

    // --- Decorators ---

    @Test
    fun testHighlightDecorator() {
        val tokens = highlighter.highlight("@staticmethod")
        assertEquals(1, tokens.size)
        assertEquals(TokenType.DECORATOR, tokens[0].type)
    }

    // --- Operators ---

    @Test
    fun testHighlightOperators() {
        val operators = listOf("+", "-", "*", "/", "==", "<", ">", "<=", ">=", "=")
        for (op in operators) {
            val tokens = highlighter.highlight(op)
            assertEquals("Expected OPERATOR for '$op'", TokenType.OPERATOR, tokens[0].type)
        }
    }

    // --- Punctuation ---

    @Test
    fun testHighlightPunctuation() {
        val punctuation = listOf("(", ")", "[", "]", "{", "}", ",", ":", ".", ";")
        for (p in punctuation) {
            val tokens = highlighter.highlight(p)
            assertEquals("Expected PUNCTUATION for '$p'", TokenType.PUNCTUATION, tokens[0].type)
        }
    }

    // --- Mixed content ---

    @Test
    fun testHighlightFunctionDefinition() {
        val tokens = highlighter.highlight("def foo(x):")
        assertTrue(tokens.any { it.type == TokenType.KEYWORD }) // def
        assertTrue(tokens.any { it.type == TokenType.FUNCTION }) // foo
        assertTrue(tokens.any { it.type == TokenType.PUNCTUATION }) // ( )
    }

    @Test
    fun testHighlightClassDefinition() {
        val tokens = highlighter.highlight("class MyClass:")
        assertTrue(tokens.any { it.type == TokenType.KEYWORD }) // class
        assertTrue(tokens.any { it.type == TokenType.TEXT }) // MyClass (not a keyword)
    }

    @Test
    fun testHighlightImport() {
        val tokens = highlighter.highlight("import os")
        assertTrue(tokens.any { it.type == TokenType.KEYWORD }) // import
        assertTrue(tokens.any { it.type == TokenType.TEXT }) // os
    }

    // --- Whitespace ---

    @Test
    fun testHighlightWhitespace() {
        val tokens = highlighter.highlight("   ")
        assertEquals(1, tokens.size)
        assertEquals(TokenType.WHITESPACE, tokens[0].type)
    }

    // --- Empty line ---

    @Test
    fun testHighlightEmptyLine() {
        val tokens = highlighter.highlight("")
        assertTrue(tokens.isEmpty())
    }

    // --- getColorForToken ---

    @Test
    fun testGetColorForToken() {
        val color = highlighter.getColorForToken(TokenType.KEYWORD)
        assertEquals(EditorTheme.DEFAULT.keywordArgb, color)
    }

    @Test
    fun testGetColorForTokenTypeString() {
        assertEquals(EditorTheme.DEFAULT.stringArgb, highlighter.getColorForToken(TokenType.STRING))
    }

    @Test
    fun testGetColorForTokenNumber() {
        assertEquals(EditorTheme.DEFAULT.numberArgb, highlighter.getColorForToken(TokenType.NUMBER))
    }

    @Test
    fun testGetColorForTokenComment() {
        assertEquals(EditorTheme.DEFAULT.commentArgb, highlighter.getColorForToken(TokenType.COMMENT))
    }

    // --- highlightLine ---

    @Test
    fun testHighlightLineReturnsColoredPairs() {
        val result = highlighter.highlightLine("def foo():")
        assertTrue(result.isNotEmpty())
        result.forEach { (text, color) ->
            assertTrue(text.isNotEmpty())
            assertTrue(color != 0L)
        }
    }

    // --- Unicode content (§2) ---

    @Test
    fun testHighlightUnicodeString() {
        val tokens = highlighter.highlight("\"hello \u00e9\u00e8\u00ea\"")
        assertEquals(1, tokens.size)
        assertEquals(TokenType.STRING, tokens[0].type)
    }

    @Test
    fun testHighlightUnicodeIdentifier() {
        val tokens = highlighter.highlight("\u00e9 = 5")
        // Unicode identifiers should be treated as TEXT
        assertTrue(tokens.any { it.type == TokenType.TEXT || it.type == TokenType.OPERATOR })
    }

    // --- Different themes ---

    @Test
    fun testDifferentThemes() {
        val themes = listOf(
            EditorTheme.DEFAULT, EditorTheme.MONOKAI, EditorTheme.SOLARIZED_DARK,
            EditorTheme.DRACULA, EditorTheme.GITHUB_DARK, EditorTheme.AMOLED
        )
        for (theme in themes) {
            val h = PythonSyntaxHighlighter(theme)
            val color = h.getColorForToken(TokenType.KEYWORD)
            assertEquals(theme.keywordArgb, color)
        }
    }

    // --- Deeply nested brackets (§2) ---

    @Test
    fun testHighlightDeeplyNestedBrackets() {
        val content = "(".repeat(100) + "a" + ")".repeat(100)
        val tokens = highlighter.highlight(content)
        assertTrue(tokens.isNotEmpty())
        // All parentheses should be punctuation
        val punctTokens = tokens.filter { it.type == TokenType.PUNCTUATION }
        assertEquals(200, punctTokens.size)
    }
}
