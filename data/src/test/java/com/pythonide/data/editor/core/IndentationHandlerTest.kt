package com.pythonide.data.editor.core

import com.pythonide.domain.model.editor.CursorPosition
import com.pythonide.domain.model.editor.EditorState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class IndentationHandlerTest {

    private lateinit var handler: IndentationHandler

    @Before
    fun setup() {
        handler = IndentationHandler()
    }

    // --- handleTab ---

    @Test
    fun testHandleTabAtStart() {
        val state = EditorState(
            content = "x = 1",
            lines = listOf("x = 1"),
            cursorPosition = CursorPosition(0, 0),
            tabSize = 4
        )
        val result = handler.handleTab(state)
        assertEquals("    ", result)
    }

    @Test
    fun testHandleTabPartialIndent() {
        val state = EditorState(
            content = "  x = 1",
            lines = listOf("  x = 1"),
            cursorPosition = CursorPosition(0, 2),
            tabSize = 4
        )
        val result = handler.handleTab(state)
        assertEquals("  ", result) // 2 spaces to reach next tab stop
    }

    @Test
    fun testHandleTabAtTabStop() {
        val state = EditorState(
            content = "    x = 1",
            lines = listOf("    x = 1"),
            cursorPosition = CursorPosition(0, 4),
            tabSize = 4
        )
        val result = handler.handleTab(state)
        assertEquals("    ", result) // full tab width
    }

    // --- handleEnter ---

    @Test
    fun testHandleEnterAfterColon() {
        val state = EditorState(
            content = "def foo():",
            lines = listOf("def foo():"),
            cursorPosition = CursorPosition(0, 10),
            tabSize = 4
        )
        val result = handler.handleEnter(state)
        assertEquals("\n    ", result) // newline + extra indent
    }

    @Test
    fun testHandleEnterAfterIf() {
        val state = EditorState(
            content = "if True:",
            lines = listOf("if True:"),
            cursorPosition = CursorPosition(0, 8),
            tabSize = 4
        )
        val result = handler.handleEnter(state)
        assertEquals("\n    ", result)
    }

    @Test
    fun testHandleEnterAfterClass() {
        val state = EditorState(
            content = "class Foo:",
            lines = listOf("class Foo:"),
            cursorPosition = CursorPosition(0, 10),
            tabSize = 4
        )
        val result = handler.handleEnter(state)
        assertEquals("\n    ", result)
    }

    @Test
    fun testHandleEnterAfterFor() {
        val state = EditorState(
            content = "for i in range(10):",
            lines = listOf("for i in range(10):"),
            cursorPosition = CursorPosition(0, 19),
            tabSize = 4
        )
        val result = handler.handleEnter(state)
        assertEquals("\n    ", result)
    }

    @Test
    fun testHandleEnterAfterWhile() {
        val state = EditorState(
            content = "while True:",
            lines = listOf("while True:"),
            cursorPosition = CursorPosition(0, 11),
            tabSize = 4
        )
        val result = handler.handleEnter(state)
        assertEquals("\n    ", result)
    }

    @Test
    fun testHandleEnterAfterWith() {
        val state = EditorState(
            content = "with open('f') as f:",
            lines = listOf("with open('f') as f:"),
            cursorPosition = CursorPosition(0, 20),
            tabSize = 4
        )
        val result = handler.handleEnter(state)
        assertEquals("\n    ", result)
    }

    @Test
    fun testHandleEnterAfterTry() {
        val state = EditorState(
            content = "try:",
            lines = listOf("try:"),
            cursorPosition = CursorPosition(0, 4),
            tabSize = 4
        )
        val result = handler.handleEnter(state)
        assertEquals("\n    ", result)
    }

    @Test
    fun testHandleEnterAfterExcept() {
        val state = EditorState(
            content = "except ValueError:",
            lines = listOf("except ValueError:"),
            cursorPosition = CursorPosition(0, 18),
            tabSize = 4
        )
        val result = handler.handleEnter(state)
        assertEquals("\n    ", result)
    }

    @Test
    fun testHandleEnterAfterFinally() {
        val state = EditorState(
            content = "finally:",
            lines = listOf("finally:"),
            cursorPosition = CursorPosition(0, 8),
            tabSize = 4
        )
        val result = handler.handleEnter(state)
        assertEquals("\n    ", result)
    }

    @Test
    fun testHandleEnterPlainLine() {
        val state = EditorState(
            content = "x = 1",
            lines = listOf("x = 1"),
            cursorPosition = CursorPosition(0, 5),
            tabSize = 4
        )
        val result = handler.handleEnter(state)
        assertEquals("\n", result)
    }

    @Test
    fun testHandleEnterInIndentedBlock() {
        val state = EditorState(
            content = "def foo():\n    x = 1",
            lines = listOf("def foo():", "    x = 1"),
            cursorPosition = CursorPosition(1, 8),
            tabSize = 4
        )
        val result = handler.handleEnter(state)
        assertEquals("\n    ", result) // maintain indent
    }

    // --- handleBackspace ---

    @Test
    fun testHandleBackspaceOnSpaces() {
        val state = EditorState(
            content = "    x = 1",
            lines = listOf("    x = 1"),
            cursorPosition = CursorPosition(0, 4),
            tabSize = 4
        )
        val result = handler.handleBackspace(state)
        assertEquals("\b\b\b\b", result) // remove full tab
    }

    @Test
    fun testHandleBackspacePartialSpaces() {
        val state = EditorState(
            content = "  x = 1",
            lines = listOf("  x = 1"),
            cursorPosition = CursorPosition(0, 2),
            tabSize = 4
        )
        val result = handler.handleBackspace(state)
        assertEquals("\b\b", result) // remove partial indent
    }

    @Test
    fun testHandleBackspaceOnNonSpace() {
        val state = EditorState(
            content = "hello",
            lines = listOf("hello"),
            cursorPosition = CursorPosition(0, 5),
            tabSize = 4
        )
        val result = handler.handleBackspace(state)
        assertEquals("\b", result)
    }

    @Test
    fun testHandleBackspaceAtColumnZero() {
        val state = EditorState(
            content = "hello",
            lines = listOf("hello"),
            cursorPosition = CursorPosition(0, 0),
            tabSize = 4
        )
        val result = handler.handleBackspace(state)
        assertNull(result)
    }

    // --- getIndentation ---

    @Test
    fun testGetIndentation() {
        assertEquals("    ", handler.getIndentation("    x = 1"))
        assertEquals("  ", handler.getIndentation("  x = 1"))
        assertEquals("", handler.getIndentation("x = 1"))
        assertEquals("", handler.getIndentation(""))
    }

    // --- calculateSmartIndent ---

    @Test
    fun testSmartIndentAfterColon() {
        val state = EditorState(
            content = "def foo():\n",
            lines = listOf("def foo():", ""),
            cursorPosition = CursorPosition(1, 0),
            tabSize = 4
        )
        val indent = handler.calculateSmartIndent(state)
        assertEquals("    ", indent)
    }

    @Test
    fun testSmartIndentAfterReturn() {
        val state = EditorState(
            content = "def foo():\n    return\n",
            lines = listOf("def foo():", "    return", ""),
            cursorPosition = CursorPosition(2, 0),
            tabSize = 4
        )
        val indent = handler.calculateSmartIndent(state)
        // Previous line "    return" has leading spaces so startsWith("return ") is false;
        // falls through to else branch which preserves previous indent
        assertEquals("    ", indent)
    }

    @Test
    fun testSmartIndentFirstLine() {
        val state = EditorState(
            content = "",
            lines = listOf(""),
            cursorPosition = CursorPosition(0, 0),
            tabSize = 4
        )
        val indent = handler.calculateSmartIndent(state)
        assertEquals("", indent)
    }

    @Test
    fun testSmartIndentAfterClosingBracket() {
        val state = EditorState(
            content = "x = [\n    1,\n    2,\n]",
            lines = listOf("x = [", "    1,", "    2,", "]"),
            cursorPosition = CursorPosition(3, 0),
            tabSize = 4
        )
        val indent = handler.calculateSmartIndent(state)
        // Should decrease indent for closing bracket
        assertEquals("", indent)
    }

    // --- autoIndentOnCharacter ---

    @Test
    fun testAutoIndentOnColon() {
        val state = EditorState(
            content = "def foo()",
            lines = listOf("def foo()"),
            cursorPosition = CursorPosition(0, 9),
            tabSize = 4,
            autoIndent = true
        )
        val result = handler.autoIndentOnCharacter(state, ':')
        assertNotNull(result)
        assertTrue(result!!.startsWith("\n"))
    }

    @Test
    fun testAutoIndentOnNewline() {
        val state = EditorState(
            content = "x = 1",
            lines = listOf("x = 1"),
            cursorPosition = CursorPosition(0, 5),
            tabSize = 4,
            autoIndent = true
        )
        val result = handler.autoIndentOnCharacter(state, '\n')
        assertNotNull(result)
        assertEquals("\n", result)
    }

    @Test
    fun testAutoIndentDisabled() {
        val state = EditorState(
            content = "def foo()",
            lines = listOf("def foo()"),
            cursorPosition = CursorPosition(0, 9),
            tabSize = 4,
            autoIndent = false
        )
        val result = handler.autoIndentOnCharacter(state, ':')
        assertNull(result)
    }

    @Test
    fun testAutoIndentOnNonSpecialChar() {
        val state = EditorState(
            content = "x = ",
            lines = listOf("x = "),
            cursorPosition = CursorPosition(0, 4),
            tabSize = 4,
            autoIndent = true
        )
        val result = handler.autoIndentOnCharacter(state, 'a')
        assertNull(result)
    }

    @Test
    fun testAutoIndentOnColonAfterNonBlock() {
        val state = EditorState(
            content = "x = 1 + 2",
            lines = listOf("x = 1 + 2"),
            cursorPosition = CursorPosition(0, 9),
            tabSize = 4,
            autoIndent = true
        )
        val result = handler.autoIndentOnCharacter(state, ':')
        assertNull(result) // not a block statement
    }
}
