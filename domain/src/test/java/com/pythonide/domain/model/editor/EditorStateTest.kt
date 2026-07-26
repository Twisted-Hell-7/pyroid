package com.pythonide.domain.model.editor

import org.junit.Assert.*
import org.junit.Test

class EditorStateTest {

    // --- CursorPosition (§2: editor core) ---

    @Test
    fun testCursorPositionComparison() {
        assertTrue(CursorPosition(0, 0) < CursorPosition(1, 0))
        assertTrue(CursorPosition(0, 5) < CursorPosition(0, 10))
        assertTrue(CursorPosition(1, 0) > CursorPosition(0, 5))
        assertEquals(0, CursorPosition(1, 3).compareTo(CursorPosition(1, 3)))
    }

    @Test
    fun testCursorPositionToOffset() {
        val content = "line1\nline2\nline3"
        assertEquals(0, CursorPosition(0, 0).toOffset(content))
        assertEquals(5, CursorPosition(0, 5).toOffset(content))
        assertEquals(6, CursorPosition(1, 0).toOffset(content))
        assertEquals(11, CursorPosition(1, 5).toOffset(content))
    }

    @Test
    fun testCursorPositionToOffsetEmptyContent() {
        assertEquals(0, CursorPosition(0, 0).toOffset(""))
    }

    @Test
    fun testCursorPositionToOffsetBeyondEnd() {
        val content = "hi"
        // Should clamp to content length
        val offset = CursorPosition(5, 10).toOffset(content)
        assertEquals(content.length, offset)
    }

    @Test
    fun testCursorPositionFromOffset() {
        val content = "line1\nline2\nline3"
        assertEquals(CursorPosition(0, 0), CursorPosition.fromOffset(content, 0))
        assertEquals(CursorPosition(0, 5), CursorPosition.fromOffset(content, 5))
        assertEquals(CursorPosition(1, 0), CursorPosition.fromOffset(content, 6))
        assertEquals(CursorPosition(1, 5), CursorPosition.fromOffset(content, 11))
    }

    @Test
    fun testCursorPositionFromOffsetEmpty() {
        assertEquals(CursorPosition(0, 0), CursorPosition.fromOffset("", 0))
    }

    @Test
    fun testCursorPositionFromOffsetAtEnd() {
        val content = "abc"
        val pos = CursorPosition.fromOffset(content, 3)
        assertEquals(CursorPosition(0, 3), pos)
    }

    @Test
    fun testCursorPositionRoundtrip() {
        val content = "hello\nworld\nfoo"
        for (offset in 0..content.length) {
            val pos = CursorPosition.fromOffset(content, offset)
            val roundtripped = pos.toOffset(content)
            assertEquals(offset, roundtripped)
        }
    }

    // --- Selection ---

    @Test
    fun testSelectionIsValid() {
        assertTrue(Selection(CursorPosition(0, 0), CursorPosition(0, 5)).isValid)
        assertFalse(Selection(CursorPosition(0, 0), CursorPosition(0, 0)).isValid)
    }

    @Test
    fun testSelectionNormalize() {
        val sel = Selection(CursorPosition(1, 5), CursorPosition(0, 0))
        val normalized = sel.normalize()
        assertEquals(CursorPosition(0, 0), normalized.start)
        assertEquals(CursorPosition(1, 5), normalized.end)
    }

    @Test
    fun testSelectionAlreadyNormalized() {
        val sel = Selection(CursorPosition(0, 0), CursorPosition(1, 5))
        val normalized = sel.normalize()
        assertEquals(sel, normalized)
    }

    @Test
    fun testSelectionContains() {
        val sel = Selection(CursorPosition(0, 0), CursorPosition(0, 10))
        assertTrue(sel.contains(CursorPosition(0, 5)))
        assertTrue(sel.contains(CursorPosition(0, 0)))
        assertTrue(sel.contains(CursorPosition(0, 10)))
        assertFalse(sel.contains(CursorPosition(1, 0)))
        assertFalse(sel.contains(CursorPosition(0, 11)))
    }

    // --- EditorAction ---

    @Test
    fun testEditorActionInsert() {
        val action = EditorAction.Insert(CursorPosition(0, 5), "hello")
        assertEquals(CursorPosition(0, 5), action.position)
        assertEquals("hello", action.text)
    }

    @Test
    fun testEditorActionDelete() {
        val action = EditorAction.Delete(CursorPosition(0, 5), "hello", 5)
        assertEquals(5, action.length)
    }

    @Test
    fun testEditorActionReplace() {
        val action = EditorAction.Replace(CursorPosition(0, 0), "old", "new")
        assertEquals("old", action.oldText)
        assertEquals("new", action.newText)
    }

    // --- EditorState defaults ---

    @Test
    fun testEditorStateDefaults() {
        val state = EditorState()
        assertEquals("", state.content)
        assertEquals(CursorPosition(0, 0), state.cursorPosition)
        assertNull(state.selection)
        assertTrue(state.lines.isEmpty())
        assertTrue(state.undoStack.isEmpty())
        assertTrue(state.redoStack.isEmpty())
        assertFalse(state.isModified)
        assertTrue(state.wordWrap)
        assertEquals(14, state.fontSize)
        assertEquals(4, state.tabSize)
        assertTrue(state.showLineNumbers)
        assertTrue(state.highlightCurrentLine)
        assertTrue(state.autoIndent)
        assertTrue(state.smartIndent)
        assertTrue(state.autoBrackets)
        assertTrue(state.autoQuotes)
        assertFalse(state.isReadOnly)
    }

    // --- EditorConfig ---

    @Test
    fun testEditorConfigDefaults() {
        val config = EditorConfig()
        assertEquals(14, config.fontSize)
        assertEquals(4, config.tabSize)
        assertTrue(config.wordWrap)
        assertEquals(EditorTheme.DEFAULT, config.theme)
    }

    // --- SearchState ---

    @Test
    fun testSearchStateDefaults() {
        val state = SearchState()
        assertEquals("", state.query)
        assertEquals("", state.replacement)
        assertFalse(state.isCaseSensitive)
        assertFalse(state.isRegex)
        assertFalse(state.isWholeWord)
        assertTrue(state.matches.isEmpty())
        assertEquals(-1, state.currentMatchIndex)
        assertFalse(state.isOpen)
    }

    // --- Tab ---

    @Test
    fun testTabDefaults() {
        val tab = Tab(id = "1", title = "test.py", content = "")
        assertFalse(tab.isModified)
        assertEquals(CursorPosition(0, 0), tab.cursorPosition)
        assertEquals(0, tab.scrollOffset)
    }

    // --- EditorTabState ---

    @Test
    fun testEditorTabStateDefaults() {
        val state = EditorTabState()
        assertTrue(state.tabs.isEmpty())
        assertNull(state.activeTabId)
        assertEquals(SplitMode.NONE, state.splitMode)
    }

    // --- SplitMode ---

    @Test
    fun testSplitModeValues() {
        assertEquals(3, SplitMode.entries.size)
        assertTrue(SplitMode.entries.contains(SplitMode.NONE))
        assertTrue(SplitMode.entries.contains(SplitMode.HORIZONTAL))
        assertTrue(SplitMode.entries.contains(SplitMode.VERTICAL))
    }

    // --- EditorTheme enum (§2: 6 themes) ---

    @Test
    fun testEditorThemeCount() {
        assertEquals(6, EditorTheme.entries.size)
    }

    @Test
    fun testEditorThemesHaveUniqueBackgrounds() {
        val backgrounds = EditorTheme.entries.map { it.backgroundArgb }.toSet()
        assertEquals(6, backgrounds.size)
    }

    @Test
    fun testAllEditorThemesHaveColors() {
        for (theme in EditorTheme.entries) {
            assertTrue(theme.backgroundArgb != 0L)
            assertTrue(theme.foregroundArgb != 0L)
            assertTrue(theme.keywordArgb != 0L)
            assertTrue(theme.stringArgb != 0L)
            assertTrue(theme.numberArgb != 0L)
            assertTrue(theme.commentArgb != 0L)
        }
    }
}
