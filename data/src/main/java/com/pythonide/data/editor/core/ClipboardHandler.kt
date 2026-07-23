package com.pythonide.data.editor.core

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.pythonide.domain.model.editor.CursorPosition
import com.pythonide.domain.model.editor.EditorState
import com.pythonide.domain.model.editor.Selection

class ClipboardHandler(private val context: Context) {
    
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    
    fun copy(state: EditorState): Boolean {
        val selection = state.selection ?: return false
        val normalized = selection.normalize()
        
        val startOffset = normalized.start.toOffset(state.content)
        val endOffset = normalized.end.toOffset(state.content)
        
        val text = state.content.substring(startOffset, endOffset)
        
        val clip = ClipData.newPlainText("code", text)
        clipboardManager.setPrimaryClip(clip)
        
        return true
    }
    
    fun cut(state: EditorState): Pair<EditorState, Boolean> {
        val selection = state.selection ?: return Pair(state, false)
        
        if (copy(state)) {
            val normalized = selection.normalize()
            val startOffset = normalized.start.toOffset(state.content)
            val endOffset = normalized.end.toOffset(state.content)
            
            val newContent = state.content.substring(0, startOffset) + 
                    state.content.substring(endOffset)
            
            val newState = state.copy(
                content = newContent,
                lines = newContent.lines(),
                cursorPosition = normalized.start,
                selection = null,
                isModified = true
            )
            
            return Pair(newState, true)
        }
        
        return Pair(state, false)
    }
    
    fun paste(state: EditorState): EditorState {
        val clip = clipboardManager.primaryClip ?: return state
        if (clip.itemCount == 0) return state
        
        val text = clip.getItemAt(0).text?.toString() ?: return state
        
        val position = state.cursorPosition
        val offset = position.toOffset(state.content)
        
        val newContent = state.content.substring(0, offset) + 
                text + 
                state.content.substring(offset)
        
        val newCursorPosition = CursorPosition.fromOffset(newContent, offset + text.length)
        
        return state.copy(
            content = newContent,
            lines = newContent.lines(),
            cursorPosition = newCursorPosition,
            selection = null,
            isModified = true
        )
    }
    
    fun selectAll(state: EditorState): EditorState {
        if (state.lines.isEmpty()) return state
        
        val lastLine = state.lines.size - 1
        val lastColumn = state.lines.last().length
        
        return state.copy(
            selection = Selection(
                CursorPosition(0, 0),
                CursorPosition(lastLine, lastColumn)
            )
        )
    }
    
    fun hasSelection(state: EditorState): Boolean {
        return state.selection?.isValid == true
    }
    
    fun getSelectedText(state: EditorState): String? {
        val selection = state.selection ?: return null
        val normalized = selection.normalize()
        
        val startOffset = normalized.start.toOffset(state.content)
        val endOffset = normalized.end.toOffset(state.content)
        
        return state.content.substring(startOffset, endOffset)
    }
    
    fun copyLine(state: EditorState): Boolean {
        val position = state.cursorPosition
        val line = state.lines.getOrNull(position.line) ?: return false
        
        val clip = ClipData.newPlainText("code", line + "\n")
        clipboardManager.setPrimaryClip(clip)
        
        return true
    }
    
    fun pasteFromHistory(state: EditorState, history: List<String>): EditorState {
        if (history.isEmpty()) return state
        
        val lastCopied = history.last()
        val position = state.cursorPosition
        val offset = position.toOffset(state.content)
        
        val newContent = state.content.substring(0, offset) + 
                lastCopied + 
                state.content.substring(offset)
        
        val newCursorPosition = CursorPosition.fromOffset(newContent, offset + lastCopied.length)
        
        return state.copy(
            content = newContent,
            lines = newContent.lines(),
            cursorPosition = newCursorPosition,
            selection = null,
            isModified = true
        )
    }
}
