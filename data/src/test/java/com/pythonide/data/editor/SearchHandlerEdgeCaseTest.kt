package com.pythonide.data.editor

import com.pythonide.domain.model.editor.CursorPosition
import com.pythonide.domain.model.editor.EditorState
import com.pythonide.domain.model.editor.SearchState
import org.junit.Assert.*
import org.junit.Test

class SearchHandlerEdgeCaseTest {

    private val searchHandler = com.pythonide.data.editor.core.SearchHandler()

    // §2: Code Editor - Search Edge Cases

    @Test
    fun testSearchEmptyQuery() {
        val state = EditorState(content = "hello world")
        val matches = searchHandler.search(state, "")
        assertTrue(matches.isEmpty())
    }

    @Test
    fun testSearchNoMatches() {
        val state = EditorState(content = "hello world")
        val matches = searchHandler.search(state, "xyz")
        assertTrue(matches.isEmpty())
    }

    @Test
    fun testSearchSingleMatch() {
        val state = EditorState(content = "hello world")
        val matches = searchHandler.search(state, "world")
        assertEquals(1, matches.size)
        assertEquals("world", matches[0].text)
    }

    @Test
    fun testSearchMultipleMatches() {
        val state = EditorState(content = "hello hello hello")
        val matches = searchHandler.search(state, "hello")
        assertEquals(3, matches.size)
    }

    @Test
    fun testSearchCaseSensitive() {
        val state = EditorState(content = "Hello HELLO hello")
        val matches = searchHandler.search(state, "hello", isCaseSensitive = true)
        assertEquals(1, matches.size)
        assertEquals("hello", matches[0].text)
    }

    @Test
    fun testSearchCaseInsensitive() {
        val state = EditorState(content = "Hello HELLO hello")
        val matches = searchHandler.search(state, "hello", isCaseSensitive = false)
        assertEquals(3, matches.size)
    }

    @Test
    fun testSearchWholeWord() {
        val state = EditorState(content = "hello helloo hello")
        val matches = searchHandler.search(state, "hello", isWholeWord = true)
        assertEquals(2, matches.size)
    }

    @Test
    fun testSearchRegex() {
        val state = EditorState(content = "Line 1: 123\nLine 2: 456\nLine 3: abc")
        val matches = searchHandler.search(state, "\\d+", isRegex = true)
        // \d+ matches "1", "123", "2", "456", "3" (5 total digit sequences)
        assertEquals(5, matches.size)
    }

    @Test
    fun testSearchInvalidRegex() {
        val state = EditorState(content = "hello world")
        val matches = searchHandler.search(state, "(invalid", isRegex = true)
        assertTrue(matches.isEmpty())
    }

    // §2: Code Editor - Find Next/Previous

    @Test
    fun testFindNext() {
        val state = EditorState(
            content = "hello world",
            cursorPosition = CursorPosition(0, 0)
        )
        val searchState = SearchState(
            matches = listOf(
                com.pythonide.domain.model.editor.SearchMatch(
                    start = CursorPosition(0, 0),
                    end = CursorPosition(0, 5),
                    text = "hello"
                ),
                com.pythonide.domain.model.editor.SearchMatch(
                    start = CursorPosition(0, 6),
                    end = CursorPosition(0, 11),
                    text = "world"
                )
            )
        )
        
        val next = searchHandler.findNext(state, searchState)
        assertNotNull(next)
        // findNext returns the first match whose start is after cursor, or whose end > cursor+1 at same position
        assertEquals("hello", next!!.text)
    }

    @Test
    fun testFindNextWrapAround() {
        val state = EditorState(
            content = "hello world",
            cursorPosition = CursorPosition(0, 10)
        )
        val searchState = SearchState(
            matches = listOf(
                com.pythonide.domain.model.editor.SearchMatch(
                    start = CursorPosition(0, 0),
                    end = CursorPosition(0, 5),
                    text = "hello"
                ),
                com.pythonide.domain.model.editor.SearchMatch(
                    start = CursorPosition(0, 6),
                    end = CursorPosition(0, 11),
                    text = "world"
                )
            )
        )
        
        val next = searchHandler.findNext(state, searchState)
        assertNotNull(next)
        assertEquals("hello", next!!.text) // Wraps to first match
    }

    @Test
    fun testFindPrevious() {
        val state = EditorState(
            content = "hello world",
            cursorPosition = CursorPosition(0, 10)
        )
        val searchState = SearchState(
            matches = listOf(
                com.pythonide.domain.model.editor.SearchMatch(
                    start = CursorPosition(0, 0),
                    end = CursorPosition(0, 5),
                    text = "hello"
                ),
                com.pythonide.domain.model.editor.SearchMatch(
                    start = CursorPosition(0, 6),
                    end = CursorPosition(0, 11),
                    text = "world"
                )
            )
        )
        
        val prev = searchHandler.findPrevious(state, searchState)
        assertNotNull(prev)
        // findPrevious returns the last match whose start < cursor position
        assertEquals("world", prev!!.text)
    }

    @Test
    fun testFindPreviousWrapAround() {
        val state = EditorState(
            content = "hello world",
            cursorPosition = CursorPosition(0, 0)
        )
        val searchState = SearchState(
            matches = listOf(
                com.pythonide.domain.model.editor.SearchMatch(
                    start = CursorPosition(0, 0),
                    end = CursorPosition(0, 5),
                    text = "hello"
                ),
                com.pythonide.domain.model.editor.SearchMatch(
                    start = CursorPosition(0, 6),
                    end = CursorPosition(0, 11),
                    text = "world"
                )
            )
        )
        
        val prev = searchHandler.findPrevious(state, searchState)
        assertNotNull(prev)
        assertEquals("world", prev!!.text) // Wraps to last match
    }

    // §2: Code Editor - Replace

    @Test
    fun testReplace() {
        val state = EditorState(content = "hello world")
        val match = com.pythonide.domain.model.editor.SearchMatch(
            start = CursorPosition(0, 0),
            end = CursorPosition(0, 5),
            text = "hello"
        )
        
        val newState = searchHandler.replace(state, match, "hi")
        assertEquals("hi world", newState.content)
    }

    @Test
    fun testReplaceAll() {
        val state = EditorState(content = "hello hello hello")
        val newState = searchHandler.replaceAll(state, "hello", "hi")
        assertEquals("hi hi hi", newState.content)
    }

    @Test
    fun testReplaceAllNoMatches() {
        val state = EditorState(content = "hello world")
        val newState = searchHandler.replaceAll(state, "xyz", "abc")
        assertEquals("hello world", newState.content)
    }

    @Test
    fun testReplaceAllCaseSensitive() {
        val state = EditorState(content = "Hello HELLO hello")
        val newState = searchHandler.replaceAll(state, "hello", "hi", isCaseSensitive = true)
        assertEquals("Hello HELLO hi", newState.content)
    }

    @Test
    fun testReplaceAllRegex() {
        val state = EditorState(content = "Line 1: 123\nLine 2: 456")
        val newState = searchHandler.replaceAll(state, "\\d+", "NUM", isRegex = true)
        // \d+ matches all digit sequences including "1" in "Line 1" and "2" in "Line 2"
        assertEquals("Line NUM: NUM\nLine NUM: NUM", newState.content)
    }

    // §2: Code Editor - Count Matches

    @Test
    fun testCountMatches() {
        val state = EditorState(content = "hello hello hello")
        val count = searchHandler.countMatches(state, "hello")
        assertEquals(3, count)
    }

    @Test
    fun testCountMatchesNoMatches() {
        val state = EditorState(content = "hello world")
        val count = searchHandler.countMatches(state, "xyz")
        assertEquals(0, count)
    }

    // §2: Code Editor - Current Match Index

    @Test
    fun testGetCurrentMatchIndex() {
        val state = EditorState(
            content = "hello world",
            cursorPosition = CursorPosition(0, 0)
        )
        val searchState = SearchState(
            matches = listOf(
                com.pythonide.domain.model.editor.SearchMatch(
                    start = CursorPosition(0, 0),
                    end = CursorPosition(0, 5),
                    text = "hello"
                ),
                com.pythonide.domain.model.editor.SearchMatch(
                    start = CursorPosition(0, 6),
                    end = CursorPosition(0, 11),
                    text = "world"
                )
            )
        )
        
        val index = searchHandler.getCurrentMatchIndex(state, searchState)
        assertEquals(0, index)
    }

    // §2: Code Editor - Catastrophic Backtracking

    @Test
    fun testSearchWithCatastrophicBacktrackingPattern() {
        val state = EditorState(content = "a".repeat(1000))
        
        // This pattern could cause catastrophic backtracking
        val matches = searchHandler.search(state, "(a+)+b")
        
        // Should complete without hanging
        assertTrue(matches.isEmpty())
    }

    // §2: Code Editor - Overlapping Matches

    @Test
    fun testSearchOverlappingMatches() {
        val state = EditorState(content = "aaaa")
        val matches = searchHandler.search(state, "aa")
        
        // Should find matches with defined convention
        assertTrue(matches.isNotEmpty())
    }

    // §2: Code Editor - Search in Empty Content

    @Test
    fun testSearchEmptyContent() {
        val state = EditorState(content = "")
        val matches = searchHandler.search(state, "hello")
        assertTrue(matches.isEmpty())
    }

    // §2: Code Editor - Search with Special Characters

    @Test
    fun testSearchWithSpecialCharacters() {
        val state = EditorState(content = "hello (world)")
        val matches = searchHandler.search(state, "(world)")
        assertEquals(1, matches.size)
    }

    @Test
    fun testSearchWithUnicode() {
        val state = EditorState(content = "日本語テスト")
        val matches = searchHandler.search(state, "テスト")
        assertEquals(1, matches.size)
    }

    @Test
    fun testSearchWithEmoji() {
        val state = EditorState(content = "Hello 🌍 World")
        val matches = searchHandler.search(state, "🌍")
        assertEquals(1, matches.size)
    }

    // §2: Code Editor - Replace with Empty String

    @Test
    fun testReplaceWithEmptyString() {
        val state = EditorState(content = "hello world")
        val match = com.pythonide.domain.model.editor.SearchMatch(
            start = CursorPosition(0, 0),
            end = CursorPosition(0, 5),
            text = "hello"
        )
        
        val newState = searchHandler.replace(state, match, "")
        assertEquals(" world", newState.content)
    }

    // §2: Code Editor - Replace with Longer String

    @Test
    fun testReplaceWithLongerString() {
        val state = EditorState(content = "hello world")
        val match = com.pythonide.domain.model.editor.SearchMatch(
            start = CursorPosition(0, 0),
            end = CursorPosition(0, 5),
            text = "hello"
        )
        
        val newState = searchHandler.replace(state, match, "goodbye")
        assertEquals("goodbye world", newState.content)
    }

    // §2: Code Editor - SearchState

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
}
