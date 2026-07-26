package com.pythonide.data.editor

import com.pythonide.domain.model.editor.CursorPosition
import com.pythonide.domain.model.editor.EditorTabState
import com.pythonide.domain.model.editor.SplitMode
import com.pythonide.domain.model.editor.Tab
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
class TabManagerEdgeCaseTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var tabManager: com.pythonide.data.editor.core.TabManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        tabManager = com.pythonide.data.editor.core.TabManager()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // §2: Code Editor - Tab Management

    @Test
    fun testCreateTab() = runTest {
        val tab = tabManager.createTab("test.py", "print('hello')")
        assertNotNull(tab.id)
        assertEquals("test.py", tab.title)
        assertEquals("print('hello')", tab.content)
    }

    @Test
    fun testCreateMultipleTabs() = runTest {
        val tab1 = tabManager.createTab("file1.py", "content1")
        val tab2 = tabManager.createTab("file2.py", "content2")
        val tab3 = tabManager.createTab("file3.py", "content3")
        
        val state = tabManager.state.value
        assertEquals(3, state.tabs.size)
        assertEquals(tab1.id, state.tabs[0].id)
        assertEquals(tab2.id, state.tabs[1].id)
        assertEquals(tab3.id, state.tabs[2].id)
    }

    @Test
    fun testSetActiveTab() = runTest {
        val tab1 = tabManager.createTab("file1.py", "content1")
        val tab2 = tabManager.createTab("file2.py", "content2")
        
        tabManager.setActiveTab(tab2.id)
        
        val state = tabManager.state.value
        assertEquals(tab2.id, state.activeTabId)
    }

    @Test
    fun testCloseTab() = runTest {
        val tab1 = tabManager.createTab("file1.py", "content1")
        val tab2 = tabManager.createTab("file2.py", "content2")
        
        tabManager.closeTab(tab1.id)
        
        val state = tabManager.state.value
        assertEquals(1, state.tabs.size)
        assertEquals(tab2.id, state.tabs[0].id)
    }

    @Test
    fun testCloseActiveTab() = runTest {
        val tab1 = tabManager.createTab("file1.py", "content1")
        val tab2 = tabManager.createTab("file2.py", "content2")
        
        tabManager.setActiveTab(tab1.id)
        tabManager.closeTab(tab1.id)
        
        val state = tabManager.state.value
        assertEquals(tab2.id, state.activeTabId)
    }

    @Test
    fun testCloseAllTabs() = runTest {
        tabManager.createTab("file1.py", "content1")
        tabManager.createTab("file2.py", "content2")
        tabManager.createTab("file3.py", "content3")
        
        tabManager.closeAllTabs()
        
        val state = tabManager.state.value
        assertTrue(state.tabs.isEmpty())
        assertNull(state.activeTabId)
    }

    @Test
    fun testUpdateTabContent() = runTest {
        val tab = tabManager.createTab("test.py", "original content")
        
        tabManager.updateTabContent(tab.id, "updated content")
        
        val state = tabManager.state.value
        val updatedTab = state.tabs.find { it.id == tab.id }
        assertEquals("updated content", updatedTab?.content)
        assertTrue(updatedTab?.isModified == true)
    }

    @Test
    fun testMoveTab() = runTest {
        val tab1 = tabManager.createTab("file1.py", "content1")
        val tab2 = tabManager.createTab("file2.py", "content2")
        val tab3 = tabManager.createTab("file3.py", "content3")
        
        tabManager.moveTab(0, 2) // Move tab1 to position 2
        
        val state = tabManager.state.value
        assertEquals(tab2.id, state.tabs[0].id)
        assertEquals(tab3.id, state.tabs[1].id)
        assertEquals(tab1.id, state.tabs[2].id)
    }

    // §2: Code Editor - Split Mode

    @Test
    fun testSetSplitMode() = runTest {
        tabManager.setSplitMode(SplitMode.HORIZONTAL)
        
        val state = tabManager.state.value
        assertEquals(SplitMode.HORIZONTAL, state.splitMode)
    }

    @Test
    fun testSetVerticalSplit() = runTest {
        tabManager.setSplitMode(SplitMode.VERTICAL)
        
        val state = tabManager.state.value
        assertEquals(SplitMode.VERTICAL, state.splitMode)
    }

    @Test
    fun testDisableSplitMode() = runTest {
        tabManager.setSplitMode(SplitMode.HORIZONTAL)
        tabManager.setSplitMode(SplitMode.NONE)
        
        val state = tabManager.state.value
        assertEquals(SplitMode.NONE, state.splitMode)
    }

    // §2: Code Editor - Tab Overflow

    @Test
    fun testTabOverflow() = runTest {
        // Create many tabs
        repeat(30) { i ->
            tabManager.createTab("file$i.py", "content$i")
        }
        
        val state = tabManager.state.value
        assertEquals(30, state.tabs.size)
    }

    // §2: Code Editor - Duplicate Tab

    @Test
    fun testDuplicateTab() = runTest {
        val tab = tabManager.createTab("test.py", "content")
        
        val duplicatedTab = tabManager.duplicateTab(tab.id)
        
        assertNotNull(duplicatedTab)
        assertNotEquals(tab.id, duplicatedTab?.id)
        assertEquals(tab.title, duplicatedTab?.title?.replace(" (Copy)", ""))
        assertEquals(tab.content, duplicatedTab?.content)
    }

    // §2: Code Editor - Tab State

    @Test
    fun testTabStateDefaults() {
        val state = EditorTabState()
        assertTrue(state.tabs.isEmpty())
        assertNull(state.activeTabId)
        assertEquals(SplitMode.NONE, state.splitMode)
    }

    @Test
    fun testTabDataClass() {
        val tab = Tab(
            id = "tab_1",
            title = "test.py",
            content = "print('hello')",
            isModified = true,
            cursorPosition = CursorPosition(1, 5),
            scrollOffset = 100
        )
        
        assertEquals("tab_1", tab.id)
        assertEquals("test.py", tab.title)
        assertEquals("print('hello')", tab.content)
        assertTrue(tab.isModified)
        assertEquals(CursorPosition(1, 5), tab.cursorPosition)
        assertEquals(100, tab.scrollOffset)
    }

    // §2: Code Editor - Get Tab by ID

    @Test
    fun testGetTabById() = runTest {
        val tab = tabManager.createTab("test.py", "content")
        
        val retrievedTab = tabManager.getTab(tab.id)
        assertNotNull(retrievedTab)
        assertEquals(tab.id, retrievedTab?.id)
    }

    @Test
    fun testGetTabByInvalidId() = runTest {
        val tab = tabManager.getTab("nonexistent_id")
        assertNull(tab)
    }

    // §2: Code Editor - Get Active Tab

    @Test
    fun testGetActiveTab() = runTest {
        val tab = tabManager.createTab("test.py", "content")
        
        val activeTab = tabManager.getActiveTab()
        assertNotNull(activeTab)
        assertEquals(tab.id, activeTab?.id)
    }

    // §2: Code Editor - Get All Tabs

    @Test
    fun testGetAllTabs() = runTest {
        tabManager.createTab("file1.py", "content1")
        tabManager.createTab("file2.py", "content2")
        
        val allTabs = tabManager.getAllTabs()
        assertEquals(2, allTabs.size)
    }

    // §2: Code Editor - Get Modified Tabs

    @Test
    fun testGetModifiedTabs() = runTest {
        val tab1 = tabManager.createTab("file1.py", "content1")
        val tab2 = tabManager.createTab("file2.py", "content2")
        
        tabManager.updateTabContent(tab1.id, "modified")
        
        val modifiedTabs = tabManager.getModifiedTabs()
        assertEquals(1, modifiedTabs.size)
        assertEquals(tab1.id, modifiedTabs[0].id)
    }

    // §2: Code Editor - Mark Tab Saved

    @Test
    fun testMarkTabSaved() = runTest {
        val tab = tabManager.createTab("test.py", "content")
        tabManager.updateTabContent(tab.id, "modified")
        
        tabManager.markTabSaved(tab.id)
        
        val state = tabManager.state.value
        val savedTab = state.tabs.find { it.id == tab.id }
        assertFalse(savedTab?.isModified == true)
    }

    // §2: Code Editor - Update Tab Title

    @Test
    fun testUpdateTabTitle() = runTest {
        val tab = tabManager.createTab("test.py", "content")
        
        tabManager.updateTabTitle(tab.id, "renamed.py")
        
        val state = tabManager.state.value
        val updatedTab = state.tabs.find { it.id == tab.id }
        assertEquals("renamed.py", updatedTab?.title)
    }

    // §2: Code Editor - Update Tab Cursor Position

    @Test
    fun testUpdateTabCursorPosition() = runTest {
        val tab = tabManager.createTab("test.py", "content")
        
        tabManager.updateTabCursorPosition(tab.id, CursorPosition(5, 10))
        
        val state = tabManager.state.value
        val updatedTab = state.tabs.find { it.id == tab.id }
        assertEquals(CursorPosition(5, 10), updatedTab?.cursorPosition)
    }

    // §2: Code Editor - Update Tab Scroll Offset

    @Test
    fun testUpdateTabScrollOffset() = runTest {
        val tab = tabManager.createTab("test.py", "content")
        
        tabManager.updateTabScrollOffset(tab.id, 100)
        
        val state = tabManager.state.value
        val updatedTab = state.tabs.find { it.id == tab.id }
        assertEquals(100, updatedTab?.scrollOffset)
    }

    // §2: Code Editor - Close Other Tabs

    @Test
    fun testCloseOtherTabs() = runTest {
        val tab1 = tabManager.createTab("file1.py", "content1")
        val tab2 = tabManager.createTab("file2.py", "content2")
        val tab3 = tabManager.createTab("file3.py", "content3")
        
        tabManager.closeOtherTabs(tab2.id)
        
        val state = tabManager.state.value
        assertEquals(1, state.tabs.size)
        assertEquals(tab2.id, state.tabs[0].id)
    }
}
