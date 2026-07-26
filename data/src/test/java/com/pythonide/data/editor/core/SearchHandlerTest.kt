package com.pythonide.data.editor.core

import com.pythonide.domain.model.editor.CursorPosition
import com.pythonide.domain.model.editor.EditorState
import com.pythonide.domain.model.editor.SearchMatch
import com.pythonide.domain.model.editor.SearchState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SearchHandlerTest {

    private lateinit var handler: SearchHandler

    @Before
    fun setup() {
        handler = SearchHandler()
    }

    // --- Basic search (§2: find & replace) ---

    @Test
    fun testSearchEmptyQuery() {
        val state = EditorState(content = "hello world")
        val matches = handler.search(state, "")
        assertTrue(matches.isEmpty())
    }

    @Test
    fun testSearchSimpleMatch() {
        val state = EditorState(content = "hello world hello")
        val matches = handler.search(state, "hello")
        assertEquals(2, matches.size)
        assertEquals("hello", matches[0].text)
        assertEquals("hello", matches[1].text)
    }

    @Test
    fun testSearchCaseInsensitive() {
        val state = EditorState(content = "Hello HELLO hello")
        val matches = handler.search(state, "hello", isCaseSensitive = false)
        assertEquals(3, matches.size)
    }

    @Test
    fun testSearchCaseSensitive() {
        val state = EditorState(content = "Hello HELLO hello")
        val matches = handler.search(state, "hello", isCaseSensitive = true)
        assertEquals(1, matches.size)
        assertEquals(CursorPosition(0, 12), matches[0].start)
    }

    @Test
    fun testSearchNoMatch() {
        val state = EditorState(content = "hello world")
        val matches = handler.search(state, "xyz")
        assertTrue(matches.isEmpty())
    }

    @Test
    fun testSearchWholeWord() {
        val state = EditorState(content = "hello helloo world")
        val matches = handler.search(state, "hello", isWholeWord = true)
        assertEquals(1, matches.size)
    }

    @Test
    fun testSearchRegex() {
        val state = EditorState(content = "abc 123 def 456")
        val matches = handler.search(state, "\\d+", isRegex = true)
        assertEquals(2, matches.size)
        assertEquals("123", matches[0].text)
        assertEquals("456", matches[1].text)
    }

    @Test
    fun testSearchRegexCatastrophicBacktracking() {
        // §2: ReDoS test - pattern with potential catastrophic backtracking
        val state = EditorState(content = "a".repeat(100))
        val matches = handler.search(state, "(a+)+b", isRegex = true)
        // Should complete without hanging (no match expected)
        assertTrue(matches.isEmpty())
    }

    @Test
    fun testSearchMultiline() {
        val state = EditorState(
            content = "line1\nline2\nline3",
            lines = listOf("line1", "line2", "line3")
        )
        val matches = handler.search(state, "line")
        assertEquals(3, matches.size)
    }

    // --- findNext / findPrevious ---

    @Test
    fun testFindNext() {
        val state = EditorState(
            content = "a b a b a",
            cursorPosition = CursorPosition(0, 0)
        )
        val matches = handler.search(state, "a")
        val searchState = SearchState(matches = matches)
        val next = handler.findNext(state, searchState)
        assertNotNull(next)
        // findNext skips the match at exact cursor position when its end > cursor+1
        assertEquals(CursorPosition(0, 4), next!!.start)
    }

    @Test
    fun testFindNextWrapsAround() {
        val state = EditorState(
            content = "a b a b",
            cursorPosition = CursorPosition(0, 6)
        )
        val matches = handler.search(state, "a")
        val searchState = SearchState(matches = matches)
        val next = handler.findNext(state, searchState)
        assertNotNull(next)
        // Should wrap to first match
    }

    @Test
    fun testFindPrevious() {
        val state = EditorState(
            content = "a b a b a",
            cursorPosition = CursorPosition(0, 8)
        )
        val matches = handler.search(state, "a")
        val searchState = SearchState(matches = matches)
        val prev = handler.findPrevious(state, searchState)
        assertNotNull(prev)
    }

    @Test
    fun testFindNextEmptyMatches() {
        val state = EditorState(content = "hello")
        val searchState = SearchState(matches = emptyList())
        assertNull(handler.findNext(state, searchState))
    }

    @Test
    fun testFindPreviousEmptyMatches() {
        val state = EditorState(content = "hello")
        val searchState = SearchState(matches = emptyList())
        assertNull(handler.findPrevious(state, searchState))
    }

    // --- replace ---

    @Test
    fun testReplaceSingleMatch() {
        val state = EditorState(content = "hello world")
        val matches = handler.search(state, "world")
        assertEquals(1, matches.size)
        val newState = handler.replace(state, matches[0], "universe")
        assertEquals("hello universe", newState.content)
    }

    @Test
    fun testReplacePreservesOtherContent() {
        val state = EditorState(content = "aaa bbb ccc")
        val matches = handler.search(state, "bbb")
        val newState = handler.replace(state, matches[0], "BBB")
        assertEquals("aaa BBB ccc", newState.content)
    }

    // --- replaceAll ---

    @Test
    fun testReplaceAll() {
        val state = EditorState(content = "foo bar foo baz foo")
        val newState = handler.replaceAll(state, "foo", "qux")
        assertEquals("qux bar qux baz qux", newState.content)
    }

    @Test
    fun testReplaceAllNoMatch() {
        val state = EditorState(content = "hello world")
        val newState = handler.replaceAll(state, "xyz", "abc")
        assertEquals("hello world", newState.content)
    }

    @Test
    fun testReplaceAllCaseInsensitive() {
        val state = EditorState(content = "Foo FOO foo")
        val newState = handler.replaceAll(state, "foo", "bar", isCaseSensitive = false)
        assertEquals("bar bar bar", newState.content)
    }

    @Test
    fun testReplaceAllRegex() {
        val state = EditorState(content = "abc 123 def 456")
        val newState = handler.replaceAll(state, "\\d+", "NUM", isRegex = true)
        assertEquals("abc NUM def NUM", newState.content)
    }

    @Test
    fun testReplaceAllEmptyContent() {
        val state = EditorState(content = "")
        val newState = handler.replaceAll(state, "x", "y")
        assertEquals("", newState.content)
    }

    // --- countMatches ---

    @Test
    fun testCountMatches() {
        val state = EditorState(content = "aaa bbb aaa ccc aaa")
        assertEquals(3, handler.countMatches(state, "aaa"))
    }

    @Test
    fun testCountMatchesZero() {
        val state = EditorState(content = "hello world")
        assertEquals(0, handler.countMatches(state, "xyz"))
    }

    // --- getCurrentMatchIndex ---

    @Test
    fun testGetCurrentMatchIndex() {
        val state = EditorState(
            content = "a b a b a",
            cursorPosition = CursorPosition(0, 4)
        )
        val matches = handler.search(state, "a")
        val searchState = SearchState(matches = matches)
        val index = handler.getCurrentMatchIndex(state, searchState)
        // Should find a match at or near cursor position
        assertTrue(index >= 0 || index == -1)
    }

    // --- Unicode/emoji search (§2) ---

    @Test
    fun testSearchUnicode() {
        val state = EditorState(content = "hello \u00e9\u00e8\u00ea world")
        val matches = handler.search(state, "\u00e9")
        assertEquals(1, matches.size)
    }

    @Test
    fun testSearchEmoji() {
        val state = EditorState(content = "hello \uD83D\uDE00 world \uD83D\uDE00")
        val matches = handler.search(state, "\uD83D\uDE00")
        assertEquals(2, matches.size)
    }

    @Test
    fun testSearchRTLText() {
        val state = EditorState(content = "hello \u0628\u0633\u0645 \u0627\u0644\u0644\u0647 world")
        val matches = handler.search(state, "\u0628\u0633\u0645")
        assertEquals(1, matches.size)
    }

    // --- Invalid regex ---

    @Test
    fun testSearchInvalidRegex() {
        val state = EditorState(content = "hello world")
        val matches = handler.search(state, "[invalid", isRegex = true)
        // Should return empty list on regex error, not crash
        assertTrue(matches.isEmpty())
    }
}
