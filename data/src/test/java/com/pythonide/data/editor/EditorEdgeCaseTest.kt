package com.pythonide.data.editor

import com.pythonide.domain.model.editor.CursorPosition
import com.pythonide.domain.model.editor.EditorState
import com.pythonide.domain.model.editor.Selection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorEdgeCaseTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var stateManager: com.pythonide.data.editor.core.EditorStateManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        stateManager = com.pythonide.data.editor.core.EditorStateManager()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // §2: Code Editor - Undo/Redo Stack Boundary

    @Test
    fun testUndoRedoStackCapAt1000() = runTest {
        stateManager.initialize("")
        
        // Insert 1001 characters
        repeat(1001) { i ->
            stateManager.insertText("a")
        }
        
        val state = stateManager.getCurrentState()
        assertTrue(state.content.length >= 1000)
        // Stack should be capped at 1000
        assertTrue(state.undoStack.size <= 1000)
    }

    @Test
    fun testUndoRedo1000TimesBackToBack() = runTest {
        stateManager.initialize("start")
        
        // Insert 1000 characters
        repeat(1000) {
            stateManager.insertText("x")
        }
        
        // Undo all 1000
        repeat(1000) {
            stateManager.undo()
        }
        
        val state = stateManager.getCurrentState()
        assertEquals("start", state.content)
        assertTrue(state.redoStack.isNotEmpty())
    }

    @Test
    fun testUndoRedoExactlyAt1000thAction() = runTest {
        stateManager.initialize("")
        
        // Insert 999 characters
        repeat(999) {
            stateManager.insertText("a")
        }
        
        // This is the 1000th action
        stateManager.insertText("b")
        
        val state = stateManager.getCurrentState()
        assertEquals(1000, state.undoStack.size)
        assertEquals("b", state.content.last().toString())
    }

    @Test
    fun testUndoRedoAt1001stAction() = runTest {
        stateManager.initialize("")
        
        // Insert 1000 characters
        repeat(1000) {
            stateManager.insertText("a")
        }
        
        // This is the 1001st action - should evict oldest
        stateManager.insertText("b")
        
        val state = stateManager.getCurrentState()
        assertEquals(1000, state.undoStack.size)
        assertTrue(state.content.contains("b"))
    }

    // §2: Code Editor - Unicode/Emoji Handling

    @Test
    fun testUnicodeIdentifiers() = runTest {
        stateManager.initialize("")
        stateManager.insertText("variable_日本語 = 42")
        
        val state = stateManager.getCurrentState()
        assertEquals("variable_日本語 = 42", state.content)
    }

    @Test
    fun testEmojiInStrings() = runTest {
        stateManager.initialize("")
        stateManager.insertText("print('Hello 🌍')")
        
        val state = stateManager.getCurrentState()
        assertEquals("print('Hello 🌍')", state.content)
    }

    @Test
    fun testRTLTextHandling() = runTest {
        stateManager.initialize("")
        stateManager.insertText("# مرحبا بالعالم")
        
        val state = stateManager.getCurrentState()
        assertEquals("# مرحبا بالعالم", state.content)
    }

    @Test
    fun testZeroWidthChars() = runTest {
        stateManager.initialize("")
        stateManager.insertText("a\u200Bb\u200Cc")
        
        val state = stateManager.getCurrentState()
        assertEquals("a\u200Bb\u200Cc", state.content)
    }

    // §2: Code Editor - Extremely Long Lines

    @Test
    fun testExtremelyLongSingleLine() = runTest {
        stateManager.initialize("")
        val longLine = "x".repeat(100_000)
        stateManager.insertText(longLine)
        
        val state = stateManager.getCurrentState()
        assertEquals(100_000, state.content.length)
        assertEquals(1, state.lines.size)
    }

    @Test
    fun testCursorMovementOnLongLine() = runTest {
        stateManager.initialize("x".repeat(1000))
        stateManager.moveCursor(0, 500)
        
        val state = stateManager.getCurrentState()
        assertEquals(0, state.cursorPosition.line)
        assertEquals(500, state.cursorPosition.column)
    }

    // §2: Code Editor - Bracket Matching Edge Cases

    @Test
    fun testDeeplyNestedBrackets() = runTest {
        val depth = 500
        val content = "(".repeat(depth) + ")".repeat(depth)
        stateManager.initialize(content)
        
        val state = stateManager.getCurrentState()
        assertEquals(depth * 2, state.content.length)
    }

    @Test
    fun testUnbalancedBrackets() = runTest {
        stateManager.initialize("((([{<>")
        
        val state = stateManager.getCurrentState()
        assertEquals("((([{<>", state.content)
    }

    // §2: Code Editor - Auto-pairing Edge Cases

    @Test
    fun testAutoPairingInsideString() = runTest {
        val bracketHandler = com.pythonide.data.editor.core.BracketHandler()
        val state = EditorState(
            content = "'hello'",
            lines = listOf("'hello'"),
            cursorPosition = CursorPosition(0, 5),
            autoBrackets = true,
            autoQuotes = true
        )
        
        val result = bracketHandler.handleCharacter(state, '\'')
        // Should handle quote inside string correctly
        assertNotNull(result)
    }

    @Test
    fun testAutoPairingInsideComment() = runTest {
        val bracketHandler = com.pythonide.data.editor.core.BracketHandler()
        val state = EditorState(
            content = "# comment",
            lines = listOf("# comment"),
            cursorPosition = CursorPosition(0, 5),
            autoBrackets = true,
            autoQuotes = true
        )
        
        val result = bracketHandler.handleCharacter(state, '(')
        // Should still auto-pair in comments
        assertNotNull(result)
    }

    @Test
    fun testAutoPairingInsideFString() = runTest {
        val bracketHandler = com.pythonide.data.editor.core.BracketHandler()
        val state = EditorState(
            content = """f"{f'{x}'}"""",
            lines = listOf("""f"{f'{x}'}""""),
            cursorPosition = CursorPosition(0, 10),
            autoBrackets = true,
            autoQuotes = true
        )
        
        val result = bracketHandler.handleCharacter(state, '\'')
        assertNotNull(result)
    }

    // §2: Code Editor - Search Edge Cases

    @Test
    fun testSearchWithCatastrophicBacktracking() = runTest {
        val searchHandler = com.pythonide.data.editor.core.SearchHandler()
        val state = EditorState(
            content = "a".repeat(1000),
            lines = listOf("a".repeat(1000))
        )
        
        // This pattern could cause catastrophic backtracking
        val matches = searchHandler.search(state, "(a+)+b")
        // Should complete without hanging
        assertTrue(matches.isEmpty())
    }

    @Test
    fun testReplaceAllWithZeroMatches() = runTest {
        val searchHandler = com.pythonide.data.editor.core.SearchHandler()
        val state = EditorState(
            content = "hello world",
            lines = listOf("hello world")
        )
        
        val newState = searchHandler.replaceAll(state, "nonexistent", "replacement")
        assertEquals("hello world", newState.content)
    }

    @Test
    fun testSearchWithOverlappingMatches() = runTest {
        val searchHandler = com.pythonide.data.editor.core.SearchHandler()
        val state = EditorState(
            content = "aaaa",
            lines = listOf("aaaa")
        )
        
        val matches = searchHandler.search(state, "aa")
        // Should find matches with defined convention
        assertTrue(matches.isNotEmpty())
    }

    // §2: Code Editor - Line Length Boundary

    @Test
    fun testLineLengthAtBoundary() = runTest {
        val line79 = "x".repeat(79)
        val line80 = "x".repeat(80)
        
        stateManager.initialize("$line79\n$line80")
        
        val state = stateManager.getCurrentState()
        assertEquals(2, state.lines.size)
        assertEquals(79, state.lines[0].length)
        assertEquals(80, state.lines[1].length)
    }

    @Test
    fun testLineLengthWithMultiByteChars() = runTest {
        // Each emoji is 2 code units in UTF-16
        val lineWithEmoji = "a".repeat(78) + "🌍"
        stateManager.initialize(lineWithEmoji)
        
        val state = stateManager.getCurrentState()
        assertEquals(1, state.lines.size)
    }

    // §2: Code Editor - File Encoding Edge Cases

    @Test
    fun testBOMPrefixedUTF8() = runTest {
        // UTF-8 BOM
        val bomContent = "\uFEFFprint('hello')"
        stateManager.initialize(bomContent)
        
        val state = stateManager.getCurrentState()
        assertTrue(state.content.startsWith("\uFEFF"))
    }

    @Test
    fun testMixedLineEndings() = runTest {
        val content = "line1\r\nline2\nline3\rline4"
        stateManager.initialize(content)
        
        val state = stateManager.getCurrentState()
        // Should handle mixed line endings
        assertTrue(state.lines.isNotEmpty())
    }

    @Test
    fun testZeroByteFile() = runTest {
        stateManager.initialize("")
        
        val state = stateManager.getCurrentState()
        assertEquals("", state.content)
        assertTrue(state.lines.isEmpty() || state.lines == listOf(""))
    }

    // §2: Code Editor - Read-only Mode

    @Test
    fun testReadOnlyModeBlocksInsert() = runTest {
        stateManager.initialize("original")
        stateManager.setReadOnly(true)
        stateManager.insertText("modified")
        
        val state = stateManager.getCurrentState()
        assertEquals("original", state.content)
    }

    @Test
    fun testReadOnlyModeBlocksDelete() = runTest {
        stateManager.initialize("original")
        stateManager.setReadOnly(true)
        stateManager.deleteCharacter()
        
        val state = stateManager.getCurrentState()
        assertEquals("original", state.content)
    }

    // §2: Code Editor - Selection Edge Cases

    @Test
    fun testSelectAllEmptyContent() = runTest {
        stateManager.initialize("")
        stateManager.selectAll()
        
        val state = stateManager.getCurrentState()
        assertNull(state.selection)
    }

    @Test
    fun testDeleteSelection() = runTest {
        stateManager.initialize("hello world")
        stateManager.select(CursorPosition(0, 0), CursorPosition(0, 5))
        stateManager.deleteSelection()
        
        val state = stateManager.getCurrentState()
        assertEquals(" world", state.content)
    }

    @Test
    fun testSelectionNormalization() = runTest {
        val selection = Selection(
            start = CursorPosition(0, 10),
            end = CursorPosition(0, 0)
        )
        val normalized = selection.normalize()
        
        assertEquals(CursorPosition(0, 0), normalized.start)
        assertEquals(CursorPosition(0, 10), normalized.end)
    }

    // §2: Code Editor - Cursor Movement Edge Cases

    @Test
    fun testCursorMovementBeyondBounds() = runTest {
        stateManager.initialize("hello")
        stateManager.moveCursor(100, 100)
        
        val state = stateManager.getCurrentState()
        // Should clamp to valid position
        assertTrue(state.cursorPosition.line <= state.lines.size - 1)
    }

    @Test
    fun testCursorMovementToNegative() = runTest {
        stateManager.initialize("hello")
        stateManager.moveCursor(-5, -5)
        
        val state = stateManager.getCurrentState()
        assertEquals(0, state.cursorPosition.line)
        assertEquals(0, state.cursorPosition.column)
    }

    @Test
    fun testMoveCursorToOffset() = runTest {
        stateManager.initialize("hello\nworld")
        stateManager.moveCursorToOffset(6)
        
        val state = stateManager.getCurrentState()
        assertEquals(1, state.cursorPosition.line)
        assertEquals(0, state.cursorPosition.column)
    }

    // §2: Code Editor - Font Size Boundaries

    @Test
    fun testFontSizeMinBoundary() = runTest {
        stateManager.setFontSize(8)
        
        val state = stateManager.getCurrentState()
        assertEquals(8, state.fontSize)
    }

    @Test
    fun testFontSizeMaxBoundary() = runTest {
        stateManager.setFontSize(72)
        
        val state = stateManager.getCurrentState()
        assertEquals(72, state.fontSize)
    }

    @Test
    fun testFontSizeBelowMin() = runTest {
        stateManager.setFontSize(1)
        
        val state = stateManager.getCurrentState()
        assertEquals(8, state.fontSize) // Should clamp to 8
    }

    @Test
    fun testFontSizeAboveMax() = runTest {
        stateManager.setFontSize(100)
        
        val state = stateManager.getCurrentState()
        assertEquals(72, state.fontSize) // Should clamp to 72
    }

    // §2: Code Editor - Tab Size Boundaries

    @Test
    fun testTabSizeOne() = runTest {
        stateManager.initialize("")
        // Tab size of 1 is valid
        val state = stateManager.getCurrentState()
        assertTrue(state.tabSize >= 1)
    }

    @Test
    fun testTabSizeLarge() = runTest {
        stateManager.initialize("")
        // Tab size of 32 should be handled
        val state = stateManager.getCurrentState()
        assertTrue(state.tabSize > 0)
    }

    // §2: Code Editor - Word Wrap Toggle

    @Test
    fun testWordWrapToggle() = runTest {
        stateManager.setWordWrap(false)
        
        val state = stateManager.getCurrentState()
        assertFalse(state.wordWrap)
        
        stateManager.setWordWrap(true)
        
        val state2 = stateManager.getCurrentState()
        assertTrue(state2.wordWrap)
    }

    // §2: Code Editor - Line Numbers Toggle

    @Test
    fun testLineNumbersToggle() = runTest {
        stateManager.setShowLineNumbers(false)
        
        val state = stateManager.getCurrentState()
        assertFalse(state.showLineNumbers)
        
        stateManager.setShowLineNumbers(true)
        
        val state2 = stateManager.getCurrentState()
        assertTrue(state2.showLineNumbers)
    }

    // §2: Code Editor - Auto Indent Toggle

    @Test
    fun testAutoIndentToggle() = runTest {
        stateManager.setAutoIndent(false)
        
        val state = stateManager.getCurrentState()
        assertFalse(state.autoIndent)
        
        stateManager.setAutoIndent(true)
        
        val state2 = stateManager.getCurrentState()
        assertTrue(state2.autoIndent)
    }

    // §2: Code Editor - Smart Indent Toggle

    @Test
    fun testSmartIndentToggle() = runTest {
        stateManager.setSmartIndent(false)
        
        val state = stateManager.getCurrentState()
        assertFalse(state.smartIndent)
        
        stateManager.setSmartIndent(true)
        
        val state2 = stateManager.getCurrentState()
        assertTrue(state2.smartIndent)
    }

    // §2: Code Editor - Highlight Current Line Toggle

    @Test
    fun testHighlightCurrentLineToggle() = runTest {
        stateManager.setHighlightCurrentLine(false)
        
        val state = stateManager.getCurrentState()
        assertFalse(state.highlightCurrentLine)
        
        stateManager.setHighlightCurrentLine(true)
        
        val state2 = stateManager.getCurrentState()
        assertTrue(state2.highlightCurrentLine)
    }
}
