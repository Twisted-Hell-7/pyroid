package com.pythonide.data.editor.core

import com.pythonide.domain.model.editor.CursorPosition
import com.pythonide.domain.model.editor.EditorTabState
import com.pythonide.domain.model.editor.SplitMode
import com.pythonide.domain.model.editor.Tab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class TabManager {
    
    private val _state = MutableStateFlow(EditorTabState())
    val state: StateFlow<EditorTabState> = _state.asStateFlow()
    
    private val mutex = Mutex()
    
    suspend fun createTab(
        title: String = "Untitled",
        content: String = ""
    ): Tab = mutex.withLock {
        val tab = Tab(
            id = UUID.randomUUID().toString(),
            title = title,
            content = content
        )
        
        _state.update { currentState ->
            currentState.copy(
                tabs = currentState.tabs + tab,
                activeTabId = tab.id
            )
        }
        
        tab
    }
    
    suspend fun closeTab(tabId: String) = mutex.withLock {
        val currentState = _state.value
        val tabIndex = currentState.tabs.indexOfFirst { it.id == tabId }
        if (tabIndex == -1) return@withLock
        
        val newTabs = currentState.tabs.filter { it.id != tabId }
        val newActiveId = when {
            currentState.activeTabId != tabId -> currentState.activeTabId
            newTabs.isEmpty() -> null
            tabIndex >= newTabs.size -> newTabs.last().id
            else -> newTabs[tabIndex].id
        }
        
        _state.update {
            it.copy(
                tabs = newTabs,
                activeTabId = newActiveId
            )
        }
    }
    
    suspend fun setActiveTab(tabId: String) = mutex.withLock {
        if (_state.value.tabs.any { it.id == tabId }) {
            _state.update { it.copy(activeTabId = tabId) }
        }
    }
    
    suspend fun updateTabContent(tabId: String, content: String) = mutex.withLock {
        _state.update { currentState ->
            currentState.copy(
                tabs = currentState.tabs.map { tab ->
                    if (tab.id == tabId) {
                        tab.copy(content = content, isModified = true)
                    } else {
                        tab
                    }
                }
            )
        }
    }
    
    suspend fun updateTabTitle(tabId: String, title: String) = mutex.withLock {
        _state.update { currentState ->
            currentState.copy(
                tabs = currentState.tabs.map { tab ->
                    if (tab.id == tabId) {
                        tab.copy(title = title)
                    } else {
                        tab
                    }
                }
            )
        }
    }
    
    suspend fun updateTabCursorPosition(tabId: String, position: CursorPosition) = mutex.withLock {
        _state.update { currentState ->
            currentState.copy(
                tabs = currentState.tabs.map { tab ->
                    if (tab.id == tabId) {
                        tab.copy(cursorPosition = position)
                    } else {
                        tab
                    }
                }
            )
        }
    }
    
    suspend fun updateTabScrollOffset(tabId: String, offset: Int) = mutex.withLock {
        _state.update { currentState ->
            currentState.copy(
                tabs = currentState.tabs.map { tab ->
                    if (tab.id == tabId) {
                        tab.copy(scrollOffset = offset)
                    } else {
                        tab
                    }
                }
            )
        }
    }
    
    suspend fun markTabSaved(tabId: String) = mutex.withLock {
        _state.update { currentState ->
            currentState.copy(
                tabs = currentState.tabs.map { tab ->
                    if (tab.id == tabId) {
                        tab.copy(isModified = false)
                    } else {
                        tab
                    }
                }
            )
        }
    }
    
    suspend fun setSplitMode(mode: SplitMode) = mutex.withLock {
        _state.update { it.copy(splitMode = mode) }
    }
    
    suspend fun getTab(tabId: String): Tab? {
        return _state.value.tabs.find { it.id == tabId }
    }
    
    suspend fun getActiveTab(): Tab? {
        val state = _state.value
        return state.tabs.find { it.id == state.activeTabId }
    }
    
    suspend fun getAllTabs(): List<Tab> {
        return _state.value.tabs
    }
    
    suspend fun getModifiedTabs(): List<Tab> {
        return _state.value.tabs.filter { it.isModified }
    }
    
    suspend fun closeAllTabs() = mutex.withLock {
        _state.update {
            it.copy(
                tabs = emptyList(),
                activeTabId = null
            )
        }
    }
    
    suspend fun closeOtherTabs(exceptTabId: String) = mutex.withLock {
        _state.update { currentState ->
            currentState.copy(
                tabs = currentState.tabs.filter { it.id == exceptTabId },
                activeTabId = exceptTabId
            )
        }
    }
    
    suspend fun moveTab(fromIndex: Int, toIndex: Int) = mutex.withLock {
        val currentState = _state.value
        if (fromIndex < 0 || fromIndex >= currentState.tabs.size ||
            toIndex < 0 || toIndex >= currentState.tabs.size) {
            return@withLock
        }
        
        val tabs = currentState.tabs.toMutableList()
        val tab = tabs.removeAt(fromIndex)
        tabs.add(toIndex, tab)
        
        _state.update { it.copy(tabs = tabs) }
    }
    
    suspend fun duplicateTab(tabId: String): Tab? = mutex.withLock {
        val original = _state.value.tabs.find { it.id == tabId } ?: return@withLock null
        
        val duplicate = Tab(
            id = UUID.randomUUID().toString(),
            title = "${original.title} (Copy)",
            content = original.content,
            cursorPosition = original.cursorPosition
        )
        
        _state.update { currentState ->
            val originalIndex = currentState.tabs.indexOfFirst { it.id == tabId }
            val newTabs = currentState.tabs.toMutableList()
            newTabs.add(originalIndex + 1, duplicate)
            
            currentState.copy(
                tabs = newTabs,
                activeTabId = duplicate.id
            )
        }
        
        duplicate
    }
}
