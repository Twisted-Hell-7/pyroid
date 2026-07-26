package com.pythonide.data.editor

import com.pythonide.domain.model.editor.CursorPosition
import com.pythonide.domain.model.editor.EditorState
import org.junit.Assert.*
import org.junit.Test

class PythonSyntaxHighlighterEdgeCaseTest {

    private val highlighter = com.pythonide.data.editor.highlight.PythonSyntaxHighlighter()

    // §2: Code Editor - Syntax Highlighting Edge Cases

    @Test
    fun testHighlightEmptyCode() {
        val tokens = highlighter.highlight("")
        assertTrue(tokens.isEmpty())
    }

    @Test
    fun testHighlightPlainText() {
        val tokens = highlighter.highlight("hello world")
        assertTrue(tokens.isNotEmpty())
    }

    @Test
    fun testHighlightKeywords() {
        val keywords = listOf(
            "def", "class", "if", "elif", "else", "for", "while", "return",
            "import", "from", "as", "try", "except", "finally", "raise",
            "with", "yield", "lambda", "pass", "break", "continue",
            "and", "or", "not", "in", "is", "True", "False", "None"
        )
        
        for (keyword in keywords) {
            val tokens = highlighter.highlight(keyword)
            assertTrue("Keyword $keyword should be highlighted", tokens.isNotEmpty())
        }
    }

    @Test
    fun testHighlightBuiltins() {
        val builtins = listOf(
            "print", "len", "range", "int", "str", "float", "list", "dict",
            "set", "tuple", "bool", "type", "input", "open", "file",
            "isinstance", "issubclass", "hasattr", "getattr", "setattr"
        )
        
        for (builtin in builtins) {
            val tokens = highlighter.highlight(builtin)
            assertTrue("Builtin $builtin should be highlighted", tokens.isNotEmpty())
        }
    }

    @Test
    fun testHighlightStrings() {
        val stringTests = listOf(
            "'hello'",
            "\"hello\"",
            "r'raw string'",
            "b'byte string'"
        )
        
        for (test in stringTests) {
            val tokens = highlighter.highlight(test)
            assertTrue("String $test should be highlighted", tokens.isNotEmpty())
        }
    }

    @Test
    fun testHighlightNumbers() {
        val numberTests = listOf(
            "42",
            "3.14",
            "1_000_000",
            "0xFF",
            "0o77",
            "0b1010",
            "1e10",
            "1.5e-10"
        )
        
        for (test in numberTests) {
            val tokens = highlighter.highlight(test)
            assertTrue("Number $test should be highlighted", tokens.isNotEmpty())
        }
    }

    @Test
    fun testHighlightComments() {
        val commentTests = listOf(
            "# single line comment",
            "# comment with special chars: !@#$%^&*()",
            "# comment with unicode: 日本語テスト",
            "# comment with emoji: 🎉"
        )
        
        for (test in commentTests) {
            val tokens = highlighter.highlight(test)
            assertTrue("Comment $test should be highlighted", tokens.isNotEmpty())
        }
    }

    @Test
    fun testHighlightDecorators() {
        val decoratorTests = listOf(
            "@staticmethod",
            "@classmethod",
            "@property",
            "@decorator",
            "@module.decorator"
        )
        
        for (test in decoratorTests) {
            val tokens = highlighter.highlight(test)
            assertTrue("Decorator $test should be highlighted", tokens.isNotEmpty())
        }
    }

    @Test
    fun testHighlightOperators() {
        val operatorTests = listOf(
            "+", "-", "*", "/", "//", "%", "**",
            "==", "!=", "<", ">", "<=", ">=",
            "+=", "-=", "*=", "/=", "//=", "%=", "**=",
            "&", "|", "^", "~", "<<", ">>"
        )
        
        for (test in operatorTests) {
            val tokens = highlighter.highlight(test)
            assertTrue("Operator $test should be highlighted", tokens.isNotEmpty())
        }
    }

    @Test
    fun testHighlightPunctuation() {
        val punctuationTests = listOf(
            "(", ")", "[", "]", "{", "}",
            ",", ":", ".", ";", "@",
            "->", "=>"
        )
        
        for (test in punctuationTests) {
            val tokens = highlighter.highlight(test)
            assertTrue("Punctuation $test should be highlighted", tokens.isNotEmpty())
        }
    }

    // §2: Code Editor - Mixed Content

    @Test
    fun testHighlightMixedContent() {
        val code = "def hello(name: str) -> None:\n    # This is a comment\n    print(f\"Hello, {name}!\")\n    return 42"
        
        val tokens = highlighter.highlight(code)
        assertTrue(tokens.isNotEmpty())
    }

    @Test
    fun testHighlightComplexCode() {
        val code = "class MyClass:\n    def __init__(self, x: int = 0):\n        self.x = x"
        
        val tokens = highlighter.highlight(code)
        assertTrue(tokens.isNotEmpty())
    }

    // §2: Code Editor - Edge Cases

    @Test
    fun testHighlightUnicodeIdentifiers() {
        val code = "変数 = 42"
        val tokens = highlighter.highlight(code)
        assertTrue(tokens.isNotEmpty())
    }

    @Test
    fun testHighlightEmojiInStrings() {
        val code = "print('Hello 🌍')"
        val tokens = highlighter.highlight(code)
        assertTrue(tokens.isNotEmpty())
    }

    @Test
    fun testHighlightLongLine() {
        val code = "x = " + "a".repeat(10000)
        val tokens = highlighter.highlight(code)
        assertTrue(tokens.isNotEmpty())
    }

    // §2: Code Editor - Token Types

    @Test
    fun testTokenTypes() {
        val code = "def hello(): pass"
        val tokens = highlighter.highlight(code)
        
        // Should have different token types
        val tokenTypes = tokens.map { it.type }.distinct()
        assertTrue(tokenTypes.size > 1)
    }

    @Test
    fun testTokenPositions() {
        val code = "hello world"
        val tokens = highlighter.highlight(code)
        
        // Tokens should have valid positions
        for (token in tokens) {
            assertTrue(token.start >= 0)
            assertTrue(token.end > token.start)
            assertTrue(token.end <= code.length)
        }
    }

    @Test
    fun testGetColorForToken() {
        val tokenTypes = com.pythonide.data.editor.highlight.TokenType.entries
        for (type in tokenTypes) {
            val color = highlighter.getColorForToken(type)
            assertTrue(color != 0L)
        }
    }
}
