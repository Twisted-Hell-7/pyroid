package com.pythonide.app.viewmodel

import com.pythonide.data.editor.OptimizedTextBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testEditorStateInitialization() {
        val viewModel = TestEditorViewModel()

        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.currentFile.value)
        assertEquals("", viewModel.content.value)
    }

    @Test
    fun testSetContent() {
        val viewModel = TestEditorViewModel()

        viewModel.setContent("print('hello')")

        assertEquals("print('hello')", viewModel.content.value)
    }

    @Test
    fun testSetCurrentFile() {
        val viewModel = TestEditorViewModel()

        viewModel.setCurrentFile("/path/to/file.py")

        assertEquals("/path/to/file.py", viewModel.currentFile.value)
    }

    @Test
    fun testSetLoading() {
        val viewModel = TestEditorViewModel()

        viewModel.setLoading(true)
        assertTrue(viewModel.isLoading.value)

        viewModel.setLoading(false)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun testCursorPosition() {
        val viewModel = TestEditorViewModel()

        viewModel.setCursorPosition(10, 5)

        assertEquals(10, viewModel.cursorLine.value)
        assertEquals(5, viewModel.cursorColumn.value)
    }

    @Test
    fun testIsModified() {
        val viewModel = TestEditorViewModel()

        assertFalse(viewModel.isModified.value)

        viewModel.setModified(true)
        assertTrue(viewModel.isModified.value)
    }

    @Test
    fun testUndoRedo() {
        val viewModel = TestEditorViewModel()

        viewModel.setContent("line1")
        viewModel.setContent("line2")
        viewModel.setContent("line3")

        viewModel.undo()
        assertEquals("line2", viewModel.content.value)

        viewModel.undo()
        assertEquals("line1", viewModel.content.value)

        viewModel.redo()
        assertEquals("line2", viewModel.content.value)
    }

    @Test
    fun testSearchState() {
        val viewModel = TestEditorViewModel()

        assertFalse(viewModel.isSearchActive.value)
        assertEquals("", viewModel.searchQuery.value)

        viewModel.setSearchActive(true)
        viewModel.setSearchQuery("test")

        assertTrue(viewModel.isSearchActive.value)
        assertEquals("test", viewModel.searchQuery.value)
    }

    @Test
    fun testDebugState() {
        val viewModel = TestEditorViewModel()

        assertFalse(viewModel.isDebugging.value)
        assertNull(viewModel.debugSession.value)

        viewModel.setDebugging(true)
        assertTrue(viewModel.isDebugging.value)
    }
}

class TestEditorViewModel {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentFile = MutableStateFlow<String?>(null)
    val currentFile: StateFlow<String?> = _currentFile.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _cursorLine = MutableStateFlow(0)
    val cursorLine: StateFlow<Int> = _cursorLine.asStateFlow()

    private val _cursorColumn = MutableStateFlow(0)
    val cursorColumn: StateFlow<Int> = _cursorColumn.asStateFlow()

    private val _isModified = MutableStateFlow(false)
    val isModified: StateFlow<Boolean> = _isModified.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isDebugging = MutableStateFlow(false)
    val isDebugging: StateFlow<Boolean> = _isDebugging.asStateFlow()

    private val _debugSession = MutableStateFlow<Any?>(null)
    val debugSession: StateFlow<Any?> = _debugSession.asStateFlow()

    private val history = mutableListOf<String>()
    private var historyIndex = -1

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setCurrentFile(file: String) {
        _currentFile.value = file
    }

    fun setContent(newContent: String) {
        if (historyIndex < history.size - 1) {
            history.subList(historyIndex + 1, history.size).clear()
        }
        history.add(newContent)
        historyIndex = history.size - 1
        _content.value = newContent
        _isModified.value = true
    }

    fun setCursorPosition(line: Int, column: Int) {
        _cursorLine.value = line
        _cursorColumn.value = column
    }

    fun setModified(modified: Boolean) {
        _isModified.value = modified
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDebugging(debugging: Boolean) {
        _isDebugging.value = debugging
    }

    fun undo() {
        if (historyIndex > 0) {
            historyIndex--
            _content.value = history[historyIndex]
        }
    }

    fun redo() {
        if (historyIndex < history.size - 1) {
            historyIndex++
            _content.value = history[historyIndex]
        }
    }
}
