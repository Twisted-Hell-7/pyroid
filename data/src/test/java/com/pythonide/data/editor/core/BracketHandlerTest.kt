package com.pythonide.data.editor.core

import com.pythonide.domain.model.editor.CursorPosition
import com.pythonide.domain.model.editor.EditorState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BracketHandlerTest {

    private lateinit var handler: BracketHandler

    @Before
    fun setup() {
        handler = BracketHandler()
    }

    // --- Opening bracket auto-close (§2: auto-pairing edge cases) ---

    @Test
    fun testOpeningParenAutoClose() {
        val state = EditorState(
            content = "print(",
            lines = listOf("print("),
            cursorPosition = CursorPosition(0, 6),
            autoBrackets = true
        )
        val result = handler.handleCharacter(state, '(')
        assertNotNull(result)
        assertEquals("()", result!!.text)
        assertEquals(-1, result.cursorOffset)
    }

    @Test
    fun testOpeningBracketAtEndOfLine() {
        val state = EditorState(
            content = "x = ",
            lines = listOf("x = "),
            cursorPosition = CursorPosition(0, 4),
            autoBrackets = true
        )
        val result = handler.handleCharacter(state, '[')
        assertNotNull(result)
        assertEquals("[]", result!!.text)
        assertEquals(-1, result.cursorOffset)
    }

    @Test
    fun testOpeningBracketBeforeWhitespace() {
        val state = EditorState(
            content = "foo( )",
            lines = listOf("foo( )"),
            cursorPosition = CursorPosition(0, 4),
            autoBrackets = true
        )
        val result = handler.handleCharacter(state, '(')
        assertNotNull(result)
        assertEquals("()", result!!.text)
    }

    @Test
    fun testOpeningBracketBeforeClosingBracket() {
        val state = EditorState(
            content = "foo())",
            lines = listOf("foo())"),
            cursorPosition = CursorPosition(0, 4),
            autoBrackets = true
        )
        val result = handler.handleCharacter(state, '(')
        assertNotNull(result)
        assertEquals("()", result!!.text)
    }

    @Test
    fun testOpeningBracketBeforeComma() {
        val state = EditorState(
            content = "foo(1,2)",
            lines = listOf("foo(1,2)"),
            cursorPosition = CursorPosition(0, 5),
            autoBrackets = true
        )
        val result = handler.handleCharacter(state, '[')
        assertNotNull(result)
        assertEquals("[]", result!!.text)
    }

    @Test
    fun testOpeningBraceAutoClose() {
        val state = EditorState(
            content = "d = ",
            lines = listOf("d = "),
            cursorPosition = CursorPosition(0, 4),
            autoBrackets = true
        )
        val result = handler.handleCharacter(state, '{')
        assertNotNull(result)
        assertEquals("{}", result!!.text)
        assertEquals(-1, result.cursorOffset)
    }

    @Test
    fun testAutoBracketsDisabled() {
        val state = EditorState(
            content = "print(",
            lines = listOf("print("),
            cursorPosition = CursorPosition(0, 6),
            autoBrackets = false
        )
        val result = handler.handleCharacter(state, '(')
        assertNull(result)
    }

    // --- Closing bracket skip-over (§2) ---

    @Test
    fun testClosingBracketSkipOver() {
        val state = EditorState(
            content = "print()",
            lines = listOf("print()"),
            cursorPosition = CursorPosition(0, 6),
            autoBrackets = true
        )
        val result = handler.handleCharacter(state, ')')
        assertNotNull(result)
        assertEquals(")", result!!.text)
        assertEquals(1, result.cursorOffset) // skip over
    }

    @Test
    fun testClosingBracketNoMatch() {
        val state = EditorState(
            content = "print(",
            lines = listOf("print("),
            cursorPosition = CursorPosition(0, 6),
            autoBrackets = true
        )
        val result = handler.handleCharacter(state, ')')
        assertNotNull(result)
        assertEquals(")", result!!.text)
        assertEquals(0, result.cursorOffset) // no skip, just insert
    }

    @Test
    fun testClosingSquareBracketSkipOver() {
        val state = EditorState(
            content = "x[0]",
            lines = listOf("x[0]"),
            cursorPosition = CursorPosition(0, 3),
            autoBrackets = true
        )
        val result = handler.handleCharacter(state, ']')
        assertNotNull(result)
        assertEquals(1, result!!.cursorOffset)
    }

    @Test
    fun testClosingBraceSkipOver() {
        val state = EditorState(
            content = "d{}",
            lines = listOf("d{}"),
            cursorPosition = CursorPosition(0, 2),
            autoBrackets = true
        )
        val result = handler.handleCharacter(state, '}')
        assertNotNull(result)
        assertEquals(1, result!!.cursorOffset)
    }

    // --- Quote handling (§2: auto-pairing edge cases) ---

    @Test
    fun testSingleQuoteAutoClose() {
        val state = EditorState(
            content = "x = ",
            lines = listOf("x = "),
            cursorPosition = CursorPosition(0, 4),
            autoQuotes = true
        )
        val result = handler.handleCharacter(state, '\'')
        assertNotNull(result)
        assertEquals("''", result!!.text)
        assertEquals(-1, result.cursorOffset)
    }

    @Test
    fun testDoubleQuoteAutoClose() {
        val state = EditorState(
            content = "x = ",
            lines = listOf("x = "),
            cursorPosition = CursorPosition(0, 4),
            autoQuotes = true
        )
        val result = handler.handleCharacter(state, '"')
        assertNotNull(result)
        assertEquals("\"\"", result!!.text)
        assertEquals(-1, result.cursorOffset)
    }

    @Test
    fun testQuoteSkipOverExisting() {
        val state = EditorState(
            content = "x = \"\"",
            lines = listOf("x = \"\""),
            cursorPosition = CursorPosition(0, 5),
            autoQuotes = true
        )
        val result = handler.handleCharacter(state, '"')
        assertNotNull(result)
        assertEquals(1, result!!.cursorOffset) // skip over
    }

    @Test
    fun testQuoteInsideStringCloses() {
        // When inside a string with odd count of the same quote, it should close
        val state = EditorState(
            content = "x = 'hello'",
            lines = listOf("x = 'hello'"),
            cursorPosition = CursorPosition(0, 9),
            autoQuotes = true
        )
        val result = handler.handleCharacter(state, '\'')
        assertNotNull(result)
        // Odd count of single quotes before cursor => should close
    }

    @Test
    fun testQuoteInsideComment() {
        val state = EditorState(
            content = "# comment '",
            lines = listOf("# comment '"),
            cursorPosition = CursorPosition(0, 11),
            autoQuotes = true
        )
        val result = handler.handleCharacter(state, '\'')
        assertNotNull(result)
        // Should still handle the quote character
    }

    // --- getMatchingBracket (§2: bracket matcher edge cases) ---

    @Test
    fun testGetMatchingParen() {
        val state = EditorState(content = "(hello)")
        val match = handler.getMatchingBracket(state, 0)
        assertEquals(6, match)
    }

    @Test
    fun testGetMatchingParenReverse() {
        val state = EditorState(content = "(hello)")
        val match = handler.getMatchingBracket(state, 6)
        assertEquals(0, match)
    }

    @Test
    fun testGetMatchingBracketNested() {
        val state = EditorState(content = "([{}])")
        assertEquals(5, handler.getMatchingBracket(state, 0))
        assertEquals(4, handler.getMatchingBracket(state, 1))
        assertEquals(3, handler.getMatchingBracket(state, 2))
        assertEquals(2, handler.getMatchingBracket(state, 3))
        assertEquals(1, handler.getMatchingBracket(state, 4))
        assertEquals(0, handler.getMatchingBracket(state, 5))
    }

    @Test
    fun testGetMatchingBracketDeeplyNested() {
        // 500+ levels deep (§2: deeply nested brackets)
        val content = "(".repeat(500) + ")".repeat(500)
        val state = EditorState(content = content)
        val match = handler.getMatchingBracket(state, 0)
        assertEquals(999, match)
    }

    @Test
    fun testGetMatchingBracketNoMatch() {
        val state = EditorState(content = "hello")
        val match = handler.getMatchingBracket(state, 2)
        assertNull(match)
    }

    @Test
    fun testGetMatchingBracketOutOfBounds() {
        val state = EditorState(content = "hi")
        assertNull(handler.getMatchingBracket(state, -1))
        assertNull(handler.getMatchingBracket(state, 10))
    }

    @Test
    fun testGetMatchingBracketNonBracketChar() {
        val state = EditorState(content = "abc")
        assertNull(handler.getMatchingBracket(state, 1))
    }

    // --- isBalanced ---

    @Test
    fun testIsBalancedEmpty() {
        assertTrue(handler.isBalanced(""))
    }

    @Test
    fun testIsBalancedSimple() {
        assertTrue(handler.isBalanced("()"))
        assertTrue(handler.isBalanced("[]"))
        assertTrue(handler.isBalanced("{}"))
    }

    @Test
    fun testIsBalancedNested() {
        assertTrue(handler.isBalanced("([{}])"))
        assertTrue(handler.isBalanced("{[()]}"))
    }

    @Test
    fun testIsBalancedUnbalanced() {
        assertFalse(handler.isBalanced("("))
        assertFalse(handler.isBalanced(")"))
        assertFalse(handler.isBalanced("(]"))
        assertFalse(handler.isBalanced("([)]"))
    }

    @Test
    fun testIsBalancedWithText() {
        assertTrue(handler.isBalanced("def foo(): pass"))
        assertTrue(handler.isBalanced("x = [1, 2, 3]"))
        assertTrue(handler.isBalanced("d = {'a': 1}"))
    }

    // --- shouldAutoCloseAfter ---

    @Test
    fun testShouldAutoCloseAfterWhitespace() {
        assertTrue(handler.shouldAutoCloseAfter(EditorState(), '(', ' '))
        assertTrue(handler.shouldAutoCloseAfter(EditorState(), '[', '\t'))
    }

    @Test
    fun testShouldAutoCloseAfterClosingBracket() {
        assertTrue(handler.shouldAutoCloseAfter(EditorState(), '(', ')'))
        assertTrue(handler.shouldAutoCloseAfter(EditorState(), '[', ']'))
    }

    @Test
    fun testShouldAutoCloseAfterComma() {
        assertTrue(handler.shouldAutoCloseAfter(EditorState(), '(', ','))
        assertTrue(handler.shouldAutoCloseAfter(EditorState(), '"', ','))
    }

    @Test
    fun testShouldNotAutoCloseAfterLetter() {
        assertFalse(handler.shouldAutoCloseAfter(EditorState(), '(', 'a'))
        assertFalse(handler.shouldAutoCloseAfter(EditorState(), '"', 'x'))
    }

    @Test
    fun testShouldAutoCloseQuoteAfterColon() {
        assertTrue(handler.shouldAutoCloseAfter(EditorState(), '"', ':'))
    }

    @Test
    fun testShouldNotAutoCloseWithNullNext() {
        assertFalse(handler.shouldAutoCloseAfter(EditorState(), '(', null))
    }

    // --- BracketResult ---

    @Test
    fun testBracketResultDataClass() {
        val result = BracketResult("()", -1)
        assertEquals("()", result.text)
        assertEquals(-1, result.cursorOffset)
    }
}
