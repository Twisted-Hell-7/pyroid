package com.pythonide.data.editor.core

import com.pythonide.domain.model.editor.CursorPosition
import com.pythonide.domain.model.editor.EditorAction
import com.pythonide.domain.model.editor.EditorState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorStateManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var manager: EditorStateManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        manager = EditorStateManager()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- §D69: 0- vs 1-indexing consistency ---

    @Test
    fun testCursorPositionConsistencyZeroIndex() = runTest {
        // CursorPosition should consistently use 0-based indexing
        val pos = CursorPosition(0, 0)
        assertEquals(0, pos.line)
        assertEquals(0, pos.column)
    }

    @Test
    fun testCursorPositionConsistencyRoundtrip() = runTest {
        // Offsets should round-trip correctly for all positions
        val content = "line1\nline2\nline3\nline4\nline5"
        for (offset in 0..content.length) {
            val pos = CursorPosition.fromOffset(content, offset)
            val roundtripped = pos.toOffset(content)
            assertEquals("Roundtrip failed at offset $offset", offset, roundtripped)
        }
    }

    @Test
    fun testCursorPositionAtLineBoundaries() = runTest {
        val content = "abc\ndef\nghi"
        // "abc" = offsets 0-3, newline at 3, "def" = offsets 4-7, newline at 7, "ghi" = offsets 8-10
        assertEquals(CursorPosition(0, 0), CursorPosition.fromOffset(content, 0))
        assertEquals(CursorPosition(0, 3), CursorPosition.fromOffset(content, 3))
        assertEquals(CursorPosition(1, 0), CursorPosition.fromOffset(content, 4))
        assertEquals(CursorPosition(1, 3), CursorPosition.fromOffset(content, 7))
        assertEquals(CursorPosition(2, 0), CursorPosition.fromOffset(content, 8))
        assertEquals(CursorPosition(2, 3), CursorPosition.fromOffset(content, 11))
    }

    // --- §D70: Tab-size boundary values ---

    @Test
    fun testTabSizeOne() = runTest {
        manager.initialize("x = 1")
        manager.moveCursor(0, 0)
        val state = manager.state.first()
        // With tab size 1, indentation should be 1 space
        val handler = com.pythonide.data.editor.core.IndentationHandler()
        val tabState = state.copy(tabSize = 1)
        val result = handler.handleTab(tabState)
        assertEquals(" ", result)
    }

    @Test
    fun testTabSize32() = runTest {
        manager.initialize("x = 1")
        manager.moveCursor(0, 0)
        val state = manager.state.first()
        val handler = com.pythonide.data.editor.core.IndentationHandler()
        val tabState = state.copy(tabSize = 32)
        val result = handler.handleTab(tabState)
        assertEquals(" ".repeat(32), result)
    }

    @Test
    fun testFontSizeBoundaryMin() = runTest {
        manager.initialize("")
        manager.setFontSize(8)
        val state = manager.state.first()
        assertEquals(8, state.fontSize)
    }

    @Test
    fun testFontSizeBoundaryMax() = runTest {
        manager.initialize("")
        manager.setFontSize(72)
        val state = manager.state.first()
        assertEquals(72, state.fontSize)
    }

    @Test
    fun testFontSizeBelowMin() = runTest {
        manager.initialize("")
        manager.setFontSize(1)
        val state = manager.state.first()
        assertEquals(8, state.fontSize) // clamped to min
    }

    @Test
    fun testFontSizeAboveMax() = runTest {
        manager.initialize("")
        manager.setFontSize(100)
        val state = manager.state.first()
        assertEquals(72, state.fontSize) // clamped to max
    }

    // --- §D71: Chunked-load boundary at 999/1000/1001 lines ---

    @Test
    fun testChunkedLoad999Lines() = runTest {
        val buffer = com.pythonide.data.editor.OptimizedTextBuffer(chunkSize = 1000)
        val file = java.io.File(System.getProperty("java.io.tmpdir"), "test_999.py")
        file.writeText((1..999).joinToString("\n") { "line_$it" })
        buffer.loadFile(file)
        assertEquals(999, buffer.getLineCount())
        assertEquals("line_1", buffer.getLine(0))
        assertEquals("line_999", buffer.getLine(998))
        file.delete()
    }

    @Test
    fun testChunkedLoad1000Lines() = runTest {
        val buffer = com.pythonide.data.editor.OptimizedTextBuffer(chunkSize = 1000)
        val file = java.io.File(System.getProperty("java.io.tmpdir"), "test_1000.py")
        file.writeText((1..1000).joinToString("\n") { "line_$it" })
        buffer.loadFile(file)
        assertEquals(1000, buffer.getLineCount())
        assertEquals("line_1", buffer.getLine(0))
        assertEquals("line_1000", buffer.getLine(999))
        file.delete()
    }

    @Test
    fun testChunkedLoad1001Lines() = runTest {
        val buffer = com.pythonide.data.editor.OptimizedTextBuffer(chunkSize = 1000)
        val file = java.io.File(System.getProperty("java.io.tmpdir"), "test_1001.py")
        file.writeText((1..1001).joinToString("\n") { "line_$it" })
        buffer.loadFile(file)
        assertEquals(1001, buffer.getLineCount())
        assertEquals("line_1", buffer.getLine(0))
        assertEquals("line_1000", buffer.getLine(999))
        assertEquals("line_1001", buffer.getLine(1000))
        file.delete()
    }

    @Test
    fun testChunkedLoadBoundaryLineNotDuplicated() = runTest {
        // §D71: confirm no off-by-one dropping or duplicating the boundary line
        val buffer = com.pythonide.data.editor.OptimizedTextBuffer(chunkSize = 10)
        val file = java.io.File(System.getProperty("java.io.tmpdir"), "test_boundary.py")
        val lines = (1..25).map { "line_$it" }
        file.writeText(lines.joinToString("\n"))
        buffer.loadFile(file)
        assertEquals(25, buffer.getLineCount())
        for (i in 0 until 25) {
            assertEquals("line_${i + 1}", buffer.getLine(i))
        }
        file.delete()
    }

    // --- §D72: Undo-stack boundary at exactly 1000th vs 1001st action ---

    @Test
    fun testUndoStack1000Actions() = runTest {
        manager.initialize("")
        // Insert 1000 characters
        repeat(1000) { i ->
            manager.insertText("a")
        }
        val state = manager.state.first()
        assertEquals(1000, state.undoStack.size)
        assertEquals("a".repeat(1000), state.content)
    }

    @Test
    fun testUndoStack1001ActionsEvictsOldest() = runTest {
        manager.initialize("")
        // Insert 1001 characters - should evict oldest
        repeat(1001) { i ->
            manager.insertText("a")
        }
        val state = manager.state.first()
        // maxUndoSize = 1000, so stack should be capped at 1000
        assertTrue(state.undoStack.size <= 1000)
        assertEquals("a".repeat(1001), state.content)
    }

    @Test
    fun testUndoAfterEviction() = runTest {
        manager.initialize("")
        // Insert 1001 chars, then undo once
        repeat(1001) { i ->
            manager.insertText(if (i < 1000) "a" else "b")
        }
        manager.undo()
        val state = manager.state.first()
        // After undoing the last "b", should have 1000 "a"s
        assertEquals("a".repeat(1000), state.content)
    }

    // --- §D73: Find/replace overlapping-match counting ---

    @Test
    fun testSearchOverlappingNonOverlapping() = runTest {
        // §D73: searching "aa" in "aaaa" - non-overlapping should find 2
        val handler = com.pythonide.data.editor.core.SearchHandler()
        val state = EditorState(content = "aaaa")
        val matches = handler.search(state, "aa", isRegex = false)
        // Non-overlapping: "aa" at 0-1, "aa" at 2-3
        assertEquals(2, matches.size)
    }

    @Test
    fun testSearchOverlappingRegex() = runTest {
        // §D73: overlapping regex search with lookahead
        val handler = com.pythonide.data.editor.core.SearchHandler()
        val state = EditorState(content = "aaaa")
        // Using lookahead for overlapping matches - findAll finds non-overlapping by default
        val matches = handler.search(state, "aa(?=a)", isRegex = true)
        // Only 1 match: "aa" at 0-1 (followed by a); position 2-3 not followed by a
        assertEquals(1, matches.size)
    }

    @Test
    fun testSearchOverlappingCount() = runTest {
        val handler = com.pythonide.data.editor.core.SearchHandler()
        val state = EditorState(content = "aaaa")
        val count = handler.countMatches(state, "aa")
        assertEquals(2, count)
    }

    // --- §A22: Empty states don't crash ---

    @Test
    fun testEmptyContentEditorState() = runTest {
        manager.initialize("")
        val state = manager.state.first()
        assertEquals("", state.content)
        assertTrue(state.lines.isEmpty() || state.lines == listOf(""))
        assertEquals(CursorPosition(0, 0), state.cursorPosition)
    }

    @Test
    fun testEmptyContentInsertAndUndo() = runTest {
        manager.initialize("")
        manager.insertText("hello")
        manager.undo()
        val state = manager.state.first()
        assertEquals("", state.content)
    }

    @Test
    fun testEmptyContentDeleteCharacter() = runTest {
        manager.initialize("")
        manager.deleteCharacter(backward = true)
        val state = manager.state.first()
        assertEquals("", state.content)
    }

    @Test
    fun testEmptyContentSelectAll() = runTest {
        manager.initialize("")
        manager.selectAll()
        val state = manager.state.first()
        assertNull(state.selection)
    }

    // --- §B44: Double-tap protection (debounce) ---

    @Test
    fun testRapidInsertNoCorruption() = runTest {
        // §B44: rapid double-tap shouldn't corrupt state
        manager.initialize("")
        repeat(50) {
            manager.insertText("x")
        }
        val state = manager.state.first()
        assertEquals("x".repeat(50), state.content)
    }

    // --- Read-only mode ---

    @Test
    fun testReadOnlyPreventsInsert() = runTest {
        manager.initialize("existing")
        manager.setReadOnly(true)
        manager.insertText("new")
        val state = manager.state.first()
        assertEquals("existing", state.content)
    }

    @Test
    fun testReadOnlyPreventsDelete() = runTest {
        manager.initialize("existing")
        manager.setReadOnly(true)
        manager.deleteCharacter()
        val state = manager.state.first()
        assertEquals("existing", state.content)
    }

    // --- Cursor clamping ---

    @Test
    fun testMoveCursorClampsToValidRange() = runTest {
        manager.initialize("ab\ncd")
        manager.moveCursor(100, 100) // way out of bounds
        val state = manager.state.first()
        // Should clamp to last line, last column
        assertEquals(1, state.cursorPosition.line)
        assertEquals(2, state.cursorPosition.column)
    }

    @Test
    fun testMoveCursorNegative() = runTest {
        manager.initialize("hello")
        manager.moveCursor(-5, -5)
        val state = manager.state.first()
        assertEquals(0, state.cursorPosition.line)
        assertEquals(0, state.cursorPosition.column)
    }

    // --- Word wrap, line numbers, highlight ---

    @Test
    fun testSetWordWrap() = runTest {
        manager.initialize("")
        manager.setWordWrap(false)
        assertFalse(manager.state.first().wordWrap)
        manager.setWordWrap(true)
        assertTrue(manager.state.first().wordWrap)
    }

    @Test
    fun testSetShowLineNumbers() = runTest {
        manager.initialize("")
        manager.setShowLineNumbers(false)
        assertFalse(manager.state.first().showLineNumbers)
    }

    @Test
    fun testSetHighlightCurrentLine() = runTest {
        manager.initialize("")
        manager.setHighlightCurrentLine(false)
        assertFalse(manager.state.first().highlightCurrentLine)
    }

    @Test
    fun testSetAutoIndent() = runTest {
        manager.initialize("")
        manager.setAutoIndent(false)
        assertFalse(manager.state.first().autoIndent)
    }

    @Test
    fun testSetSmartIndent() = runTest {
        manager.initialize("")
        manager.setSmartIndent(false)
        assertFalse(manager.state.first().smartIndent)
    }

    @Test
    fun testSetAutoBrackets() = runTest {
        manager.initialize("")
        manager.setAutoBrackets(false)
        assertFalse(manager.state.first().autoBrackets)
    }

    @Test
    fun testSetAutoQuotes() = runTest {
        manager.initialize("")
        manager.setAutoQuotes(false)
        assertFalse(manager.state.first().autoQuotes)
    }
}
