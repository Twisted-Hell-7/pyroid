package com.pythonide.data.editor.core

import com.pythonide.domain.model.editor.CursorPosition
import com.pythonide.domain.model.editor.EditorState
import com.pythonide.domain.model.editor.SearchMatch
import com.pythonide.domain.model.editor.SearchState

class SearchHandler {
    
    fun search(
        state: EditorState,
        query: String,
        isCaseSensitive: Boolean = false,
        isRegex: Boolean = false,
        isWholeWord: Boolean = false
    ): List<SearchMatch> {
        if (query.isEmpty()) return emptyList()
        
        val content = state.content
        val matches = mutableListOf<SearchMatch>()
        
        try {
            val pattern = if (isRegex) {
                val flags = if (isCaseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                Regex(query, flags)
            } else {
                val escaped = Regex.escape(query)
                val finalPattern = if (isWholeWord) "\\b$escaped\\b" else escaped
                Regex(finalPattern, if (isCaseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE))
            }
            
            var lastIndex = 0
            pattern.findAll(content).forEach { match ->
                val start = CursorPosition.fromOffset(content, match.range.first)
                val end = CursorPosition.fromOffset(content, match.range.last + 1)
                matches.add(SearchMatch(start, end, match.value))
                lastIndex = match.range.last + 1
            }
        } catch (e: Exception) {
            return emptyList()
        }
        
        return matches
    }
    
    fun findNext(
        state: EditorState,
        searchState: SearchState
    ): SearchMatch? {
        if (searchState.matches.isEmpty()) return null
        
        val currentPosition = state.cursorPosition
        val matches = searchState.matches
        
        val nextIndex = matches.indexOfFirst { match ->
            match.start > currentPosition || 
            (match.start == currentPosition && match.end > CursorPosition(currentPosition.line, currentPosition.column + 1))
        }
        
        return if (nextIndex >= 0) {
            matches[nextIndex]
        } else {
            matches.firstOrNull()
        }
    }
    
    fun findPrevious(
        state: EditorState,
        searchState: SearchState
    ): SearchMatch? {
        if (searchState.matches.isEmpty()) return null
        
        val currentPosition = state.cursorPosition
        val matches = searchState.matches
        
        val prevIndex = matches.indexOfLast { match ->
            match.start < currentPosition
        }
        
        return if (prevIndex >= 0) {
            matches[prevIndex]
        } else {
            matches.lastOrNull()
        }
    }
    
    fun replace(
        state: EditorState,
        match: SearchMatch,
        replacement: String
    ): EditorState {
        val startOffset = match.start.toOffset(state.content)
        val endOffset = match.end.toOffset(state.content)
        
        val newContent = state.content.substring(0, startOffset) + 
                replacement + 
                state.content.substring(endOffset)
        
        val newCursorPosition = CursorPosition.fromOffset(newContent, startOffset + replacement.length)
        
        return state.copy(
            content = newContent,
            lines = newContent.lines(),
            cursorPosition = newCursorPosition
        )
    }
    
    fun replaceAll(
        state: EditorState,
        query: String,
        replacement: String,
        isCaseSensitive: Boolean = false,
        isRegex: Boolean = false,
        isWholeWord: Boolean = false
    ): EditorState {
        val searchState = SearchState(
            query = query,
            isCaseSensitive = isCaseSensitive,
            isRegex = isRegex,
            isWholeWord = isWholeWord
        )
        
        val matches = search(state, query, isCaseSensitive, isRegex, isWholeWord)
        if (matches.isEmpty()) return state
        
        var newContent = state.content
        var offset = 0
        
        for (match in matches) {
            val startOffset = match.start.toOffset(state.content) + offset
            val endOffset = match.end.toOffset(state.content) + offset
            
            newContent = newContent.substring(0, startOffset) + 
                    replacement + 
                    newContent.substring(endOffset)
            
            offset += replacement.length - match.text.length
        }
        
        val newCursorPosition = CursorPosition.fromOffset(newContent, 0)
        
        return state.copy(
            content = newContent,
            lines = newContent.lines(),
            cursorPosition = newCursorPosition
        )
    }
    
    fun countMatches(
        state: EditorState,
        query: String,
        isCaseSensitive: Boolean = false,
        isRegex: Boolean = false,
        isWholeWord: Boolean = false
    ): Int {
        return search(state, query, isCaseSensitive, isRegex, isWholeWord).size
    }
    
    fun getCurrentMatchIndex(
        state: EditorState,
        searchState: SearchState
    ): Int {
        val currentPosition = state.cursorPosition
        return searchState.matches.indexOfFirst { match ->
            match.start == currentPosition || 
            (match.start.line == currentPosition.line && match.start.column <= currentPosition.column &&
             match.end.column > currentPosition.column)
        }
    }
}
