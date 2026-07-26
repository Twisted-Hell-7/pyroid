package com.pythonide.data.editor.core

import com.pythonide.domain.model.editor.CursorPosition
import com.pythonide.domain.model.editor.EditorAction
import com.pythonide.domain.model.editor.EditorState
import com.pythonide.domain.model.editor.Selection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EditorStateManager {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val mutex = Mutex()
    private val maxUndoSize = 1000

    suspend fun initialize(content: String) = mutex.withLock {
        _state.update {
            it.copy(
                content = content,
                lines = content.lines(),
                cursorPosition = CursorPosition(0, 0),
                undoStack = emptyList(),
                redoStack = emptyList(),
                isModified = false
            )
        }
    }

    suspend fun insertText(text: String) = mutex.withLock {
        val currentState = _state.value
        if (currentState.isReadOnly) return@withLock

        val position = currentState.cursorPosition
        val offset = position.toOffset(currentState.content)
        
        val newContent = currentState.content.substring(0, offset) + 
                text + 
                currentState.content.substring(offset)
        
        val newCursorPosition = CursorPosition.fromOffset(newContent, offset + text.length)
        
        val action = EditorAction.Insert(position, text)
        val newUndoStack = (currentState.undoStack + action).takeLast(maxUndoSize)
        
        _state.update {
            it.copy(
                content = newContent,
                lines = newContent.lines(),
                cursorPosition = newCursorPosition,
                undoStack = newUndoStack,
                redoStack = emptyList(),
                isModified = true
            )
        }
    }

    suspend fun deleteCharacter(backward: Boolean = true) = mutex.withLock {
        val currentState = _state.value
        if (currentState.isReadOnly) return@withLock

        val position = currentState.cursorPosition
        val offset = position.toOffset(currentState.content)
        
        if (backward && offset == 0) return@withLock
        if (!backward && offset >= currentState.content.length) return@withLock
        
        val deleteOffset = if (backward) offset - 1 else offset
        val deletedChar = currentState.content[deleteOffset]
        
        val newContent = currentState.content.substring(0, deleteOffset) + 
                currentState.content.substring(deleteOffset + 1)
        
        val newCursorPosition = if (backward) {
            CursorPosition.fromOffset(newContent, deleteOffset)
        } else {
            position
        }
        
        val action = EditorAction.Delete(
            CursorPosition.fromOffset(currentState.content, deleteOffset),
            deletedChar.toString(),
            1
        )
        val newUndoStack = (currentState.undoStack + action).takeLast(maxUndoSize)
        
        _state.update {
            it.copy(
                content = newContent,
                lines = newContent.lines(),
                cursorPosition = newCursorPosition,
                undoStack = newUndoStack,
                redoStack = emptyList(),
                isModified = true
            )
        }
    }

    suspend fun deleteSelection() = mutex.withLock {
        val currentState = _state.value
        if (currentState.isReadOnly) return@withLock

        val selection = currentState.selection ?: return@withLock
        val normalized = selection.normalize()
        
        val startOffset = normalized.start.toOffset(currentState.content)
        val endOffset = normalized.end.toOffset(currentState.content)
        
        val deletedText = currentState.content.substring(startOffset, endOffset)
        val newContent = currentState.content.substring(0, startOffset) + 
                currentState.content.substring(endOffset)
        
        val action = EditorAction.Delete(normalized.start, deletedText, endOffset - startOffset)
        val newUndoStack = (currentState.undoStack + action).takeLast(maxUndoSize)
        
        _state.update {
            it.copy(
                content = newContent,
                lines = newContent.lines(),
                cursorPosition = normalized.start,
                selection = null,
                undoStack = newUndoStack,
                redoStack = emptyList(),
                isModified = true
            )
        }
    }

    suspend fun moveCursor(line: Int, column: Int) = mutex.withLock {
        val currentState = _state.value
        val maxLine = currentState.lines.size - 1
        val clampedLine = line.coerceIn(0, maxLine)
        val maxColumn = if (currentState.lines.isEmpty()) 0 else currentState.lines[clampedLine].length
        val clampedColumn = column.coerceIn(0, maxColumn)
        
        _state.update {
            it.copy(
                cursorPosition = CursorPosition(clampedLine, clampedColumn),
                selection = null
            )
        }
    }

    suspend fun moveCursorToOffset(offset: Int) = mutex.withLock {
        val currentState = _state.value
        val position = CursorPosition.fromOffset(currentState.content, offset.coerceIn(0, currentState.content.length))
        
        _state.update {
            it.copy(
                cursorPosition = position,
                selection = null
            )
        }
    }

    suspend fun select(start: CursorPosition, end: CursorPosition) = mutex.withLock {
        _state.update {
            it.copy(selection = Selection(start, end))
        }
    }

    suspend fun selectAll() = mutex.withLock {
        val currentState = _state.value
        if (currentState.content.isEmpty()) {
            _state.update { it.copy(selection = null) }
            return@withLock
        }
        
        val lastLine = currentState.lines.size - 1
        val lastColumn = currentState.lines.last().length
        
        _state.update {
            it.copy(
                selection = Selection(
                    CursorPosition(0, 0),
                    CursorPosition(lastLine, lastColumn)
                )
            )
        }
    }

    suspend fun undo() = mutex.withLock {
        val currentState = _state.value
        if (currentState.undoStack.isEmpty()) return@withLock
        
        val action = currentState.undoStack.last()
        val newUndoStack = currentState.undoStack.dropLast(1)
        
        when (action) {
            is EditorAction.Insert -> {
                val offset = action.position.toOffset(currentState.content)
                val newContent = currentState.content.substring(0, offset) + 
                        currentState.content.substring(offset + action.text.length)
                
                _state.update {
                    it.copy(
                        content = newContent,
                        lines = newContent.lines(),
                        cursorPosition = action.position,
                        undoStack = newUndoStack,
                        redoStack = it.redoStack + action
                    )
                }
            }
            is EditorAction.Delete -> {
                val offset = action.position.toOffset(currentState.content)
                val newContent = currentState.content.substring(0, offset) + 
                        action.text + 
                        currentState.content.substring(offset)
                
                _state.update {
                    it.copy(
                        content = newContent,
                        lines = newContent.lines(),
                        cursorPosition = CursorPosition.fromOffset(newContent, offset + action.text.length),
                        undoStack = newUndoStack,
                        redoStack = it.redoStack + action
                    )
                }
            }
            is EditorAction.Replace -> {
                val offset = action.position.toOffset(currentState.content)
                val newContent = currentState.content.substring(0, offset) + 
                        action.oldText + 
                        currentState.content.substring(offset + action.newText.length)
                
                _state.update {
                    it.copy(
                        content = newContent,
                        lines = newContent.lines(),
                        cursorPosition = action.position,
                        undoStack = newUndoStack,
                        redoStack = it.redoStack + action
                    )
                }
            }
        }
    }

    suspend fun redo() = mutex.withLock {
        val currentState = _state.value
        if (currentState.redoStack.isEmpty()) return@withLock
        
        val action = currentState.redoStack.last()
        val newRedoStack = currentState.redoStack.dropLast(1)
        
        when (action) {
            is EditorAction.Insert -> {
                val offset = action.position.toOffset(currentState.content)
                val newContent = currentState.content.substring(0, offset) + 
                        action.text + 
                        currentState.content.substring(offset)
                
                _state.update {
                    it.copy(
                        content = newContent,
                        lines = newContent.lines(),
                        cursorPosition = CursorPosition.fromOffset(newContent, offset + action.text.length),
                        undoStack = it.undoStack + action,
                        redoStack = newRedoStack
                    )
                }
            }
            is EditorAction.Delete -> {
                val offset = action.position.toOffset(currentState.content)
                val newContent = currentState.content.substring(0, offset) + 
                        currentState.content.substring(offset + action.length)
                
                _state.update {
                    it.copy(
                        content = newContent,
                        lines = newContent.lines(),
                        cursorPosition = action.position,
                        undoStack = it.undoStack + action,
                        redoStack = newRedoStack
                    )
                }
            }
            is EditorAction.Replace -> {
                val offset = action.position.toOffset(currentState.content)
                val newContent = currentState.content.substring(0, offset) + 
                        action.newText + 
                        currentState.content.substring(offset + action.oldText.length)
                
                _state.update {
                    it.copy(
                        content = newContent,
                        lines = newContent.lines(),
                        cursorPosition = CursorPosition.fromOffset(newContent, offset + action.newText.length),
                        undoStack = it.undoStack + action,
                        redoStack = newRedoStack
                    )
                }
            }
        }
    }

    suspend fun setWordWrap(enabled: Boolean) = mutex.withLock {
        _state.update { it.copy(wordWrap = enabled) }
    }

    suspend fun setFontSize(size: Int) = mutex.withLock {
        _state.update { it.copy(fontSize = size.coerceIn(8, 72)) }
    }

    suspend fun setShowLineNumbers(show: Boolean) = mutex.withLock {
        _state.update { it.copy(showLineNumbers = show) }
    }

    suspend fun setHighlightCurrentLine(highlight: Boolean) = mutex.withLock {
        _state.update { it.copy(highlightCurrentLine = highlight) }
    }

    suspend fun setAutoIndent(enabled: Boolean) = mutex.withLock {
        _state.update { it.copy(autoIndent = enabled) }
    }

    suspend fun setSmartIndent(enabled: Boolean) = mutex.withLock {
        _state.update { it.copy(smartIndent = enabled) }
    }

    suspend fun setAutoBrackets(enabled: Boolean) = mutex.withLock {
        _state.update { it.copy(autoBrackets = enabled) }
    }

    suspend fun setAutoQuotes(enabled: Boolean) = mutex.withLock {
        _state.update { it.copy(autoQuotes = enabled) }
    }

    suspend fun setReadOnly(readOnly: Boolean) = mutex.withLock {
        _state.update { it.copy(isReadOnly = readOnly) }
    }

    fun getCurrentState(): EditorState = _state.value
}
