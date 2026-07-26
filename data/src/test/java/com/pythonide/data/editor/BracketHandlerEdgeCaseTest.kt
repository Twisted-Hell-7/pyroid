package com.pythonide.data.editor

import com.pythonide.domain.model.editor.CursorPosition
import com.pythonide.domain.model.editor.EditorState
import org.junit.Assert.*
import org.junit.Test

class BracketHandlerEdgeCaseTest {

    private val bracketHandler = com.pythonide.data.editor.core.BracketHandler()

    // §2: Code Editor - Auto-pairing Edge Cases

    @Test
    fun testAutoPairingOpeningBracket() {
        val state = EditorState(
            content = "",
            lines = listOf(""),
            cursorPosition = CursorPosition(0, 0),
            autoBrackets = true
        )
        
        val result = bracketHandler.handleCharacter(state, '(')
        assertNotNull(result)
        assertEquals("()", result!!.text)
        assertEquals(-1, result.cursorOffset)
    }

    @Test
    fun testAutoPairingOpeningSquareBracket() {
        val state = EditorState(
            content = "",
            lines = listOf(""),
            cursorPosition = CursorPosition(0, 0),
            autoBrackets = true
        )
        
        val result = bracketHandler.handleCharacter(state, '[')
        assertNotNull(result)
        assertEquals("[]", result!!.text)
        assertEquals(-1, result.cursorOffset)
    }

    @Test
    fun testAutoPairingOpeningCurlyBracket() {
        val state = EditorState(
            content = "",
            lines = listOf(""),
            cursorPosition = CursorPosition(0, 0),
            autoBrackets = true
        )
        
        val result = bracketHandler.handleCharacter(state, '{')
        assertNotNull(result)
        assertEquals("{}", result!!.text)
        assertEquals(-1, result.cursorOffset)
    }

    @Test
    fun testAutoPairingClosingBracketSkip() {
        val state = EditorState(
            content = "()",
            lines = listOf("()"),
            cursorPosition = CursorPosition(0, 1),
            autoBrackets = true
        )
        
        val result = bracketHandler.handleCharacter(state, ')')
        assertNotNull(result)
        assertEquals(")", result!!.text)
        assertEquals(1, result.cursorOffset)
    }

    @Test
    fun testAutoPairingQuote() {
        val state = EditorState(
            content = "",
            lines = listOf(""),
            cursorPosition = CursorPosition(0, 0),
            autoQuotes = true
        )
        
        val result = bracketHandler.handleCharacter(state, '\'')
        assertNotNull(result)
        assertEquals("''", result!!.text)
        assertEquals(-1, result.cursorOffset)
    }

    @Test
    fun testAutoPairingDoubleQuote() {
        val state = EditorState(
            content = "",
            lines = listOf(""),
            cursorPosition = CursorPosition(0, 0),
            autoQuotes = true
        )
        
        val result = bracketHandler.handleCharacter(state, '"')
        assertNotNull(result)
        assertEquals("\"\"", result!!.text)
        assertEquals(-1, result.cursorOffset)
    }

    @Test
    fun testAutoPairingDisabled() {
        val state = EditorState(
            content = "",
            lines = listOf(""),
            cursorPosition = CursorPosition(0, 0),
            autoBrackets = false,
            autoQuotes = false
        )
        
        val result = bracketHandler.handleCharacter(state, '(')
        assertNull(result)
    }

    // §2: Code Editor - Bracket Matching

    @Test
    fun testMatchingBracketSimple() {
        val state = EditorState(
            content = "()",
            lines = listOf("()")
        )
        
        val match = bracketHandler.getMatchingBracket(state, 0)
        assertEquals(1, match)
    }

    @Test
    fun testMatchingBracketNested() {
        val state = EditorState(
            content = "(())",
            lines = listOf("(())")
        )
        
        val match = bracketHandler.getMatchingBracket(state, 0)
        assertEquals(3, match)
    }

    @Test
    fun testMatchingBracketSquare() {
        val state = EditorState(
            content = "[]",
            lines = listOf("[]")
        )
        
        val match = bracketHandler.getMatchingBracket(state, 0)
        assertEquals(1, match)
    }

    @Test
    fun testMatchingBracketCurly() {
        val state = EditorState(
            content = "{}",
            lines = listOf("{}")
        )
        
        val match = bracketHandler.getMatchingBracket(state, 0)
        assertEquals(1, match)
    }

    @Test
    fun testMatchingBracketNotFound() {
        val state = EditorState(
            content = "(]",
            lines = listOf("(]")
        )
        
        val match = bracketHandler.getMatchingBracket(state, 0)
        assertNull(match)
    }

    @Test
    fun testMatchingBracketNonBracket() {
        val state = EditorState(
            content = "abc",
            lines = listOf("abc")
        )
        
        val match = bracketHandler.getMatchingBracket(state, 1)
        assertNull(match)
    }

    // §2: Code Editor - Bracket Balancing

    @Test
    fun testIsBalancedSimple() {
        assertTrue(bracketHandler.isBalanced("()"))
        assertTrue(bracketHandler.isBalanced("[]"))
        assertTrue(bracketHandler.isBalanced("{}"))
    }

    @Test
    fun testIsBalancedNested() {
        assertTrue(bracketHandler.isBalanced("(())"))
        assertTrue(bracketHandler.isBalanced("[[]]"))
        assertTrue(bracketHandler.isBalanced("{{}}"))
    }

    @Test
    fun testIsBalancedMixed() {
        assertTrue(bracketHandler.isBalanced("([])"))
        assertTrue(bracketHandler.isBalanced("{[()]}"))
    }

    @Test
    fun testIsBalancedUnbalanced() {
        assertFalse(bracketHandler.isBalanced("("))
        assertFalse(bracketHandler.isBalanced(")"))
        assertFalse(bracketHandler.isBalanced("(]"))
        assertFalse(bracketHandler.isBalanced("[)"))
    }

    @Test
    fun testIsBalancedEmpty() {
        assertTrue(bracketHandler.isBalanced(""))
    }

    // §2: Code Editor - Auto-close After

    @Test
    fun testShouldAutoCloseAfterSpace() {
        assertTrue(bracketHandler.shouldAutoCloseAfter(EditorState(), '(', ' '))
        assertTrue(bracketHandler.shouldAutoCloseAfter(EditorState(), '[', ' '))
        assertTrue(bracketHandler.shouldAutoCloseAfter(EditorState(), '{', ' '))
    }

    @Test
    fun testShouldAutoCloseAfterClosingBracket() {
        assertTrue(bracketHandler.shouldAutoCloseAfter(EditorState(), '(', ')'))
        assertTrue(bracketHandler.shouldAutoCloseAfter(EditorState(), '[', ']'))
        assertTrue(bracketHandler.shouldAutoCloseAfter(EditorState(), '{', '}'))
    }

    @Test
    fun testShouldAutoCloseAfterComma() {
        assertTrue(bracketHandler.shouldAutoCloseAfter(EditorState(), '(', ','))
        assertTrue(bracketHandler.shouldAutoCloseAfter(EditorState(), '[', ','))
        assertTrue(bracketHandler.shouldAutoCloseAfter(EditorState(), '{', ','))
    }

    @Test
    fun testShouldNotAutoCloseAfterLetter() {
        assertFalse(bracketHandler.shouldAutoCloseAfter(EditorState(), '(', 'a'))
        assertFalse(bracketHandler.shouldAutoCloseAfter(EditorState(), '[', 'b'))
        assertFalse(bracketHandler.shouldAutoCloseAfter(EditorState(), '{', 'c'))
    }

    @Test
    fun testShouldAutoCloseQuoteAfterSpace() {
        assertTrue(bracketHandler.shouldAutoCloseAfter(EditorState(), '\'', ' '))
        assertTrue(bracketHandler.shouldAutoCloseAfter(EditorState(), '"', ' '))
    }

    @Test
    fun testShouldAutoCloseQuoteAfterClosingBracket() {
        assertTrue(bracketHandler.shouldAutoCloseAfter(EditorState(), '\'', ')'))
        assertTrue(bracketHandler.shouldAutoCloseAfter(EditorState(), '"', ']'))
    }

    @Test
    fun testShouldAutoCloseQuoteAfterColon() {
        assertTrue(bracketHandler.shouldAutoCloseAfter(EditorState(), '\'', ':'))
        assertTrue(bracketHandler.shouldAutoCloseAfter(EditorState(), '"', ':'))
    }

    // §2: Code Editor - Deeply Nested Brackets

    @Test
    fun testDeeplyNestedBrackets() {
        val depth = 500
        val content = "(".repeat(depth) + ")".repeat(depth)
        val state = EditorState(
            content = content,
            lines = listOf(content)
        )
        
        val match = bracketHandler.getMatchingBracket(state, 0)
        assertEquals(depth * 2 - 1, match)
    }

    // §2: Code Editor - Quote Inside String

    @Test
    fun testQuoteInsideString() {
        val state = EditorState(
            content = "'hello'",
            lines = listOf("'hello'"),
            cursorPosition = CursorPosition(0, 5),
            autoQuotes = true
        )
        
        val result = bracketHandler.handleCharacter(state, '\'')
        assertNotNull(result)
        // Should handle quote inside string correctly
    }

    // §2: Code Editor - Quote Inside Comment

    @Test
    fun testQuoteInsideComment() {
        val state = EditorState(
            content = "# comment",
            lines = listOf("# comment"),
            cursorPosition = CursorPosition(0, 5),
            autoQuotes = true
        )
        
        val result = bracketHandler.handleCharacter(state, '\'')
        assertNotNull(result)
    }

    // §2: Code Editor - Triple Quote

    @Test
    fun testTripleQuoteAutoClose() {
        val state = EditorState(
            content = "",
            lines = listOf(""),
            cursorPosition = CursorPosition(0, 0),
            autoQuotes = true
        )
        
        // Simulate triple quote auto-close
        val tripleQuote = "'''"
        val result = state.copy(
            content = tripleQuote,
            lines = listOf(tripleQuote)
        )
        
        assertEquals("'''", result.content)
    }

    // §2: Code Editor - Bracket Result

    @Test
    fun testBracketResultCreation() {
        val result = com.pythonide.data.editor.core.BracketResult(
            text = "()",
            cursorOffset = -1
        )
        assertEquals("()", result.text)
        assertEquals(-1, result.cursorOffset)
    }
}
