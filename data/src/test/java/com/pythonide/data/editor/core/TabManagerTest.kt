package com.pythonide.data.editor.core

import com.pythonide.domain.model.editor.CursorPosition
import com.pythonide.domain.model.editor.SplitMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TabManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var tabManager: TabManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        tabManager = TabManager()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Create tab ---

    @Test
    fun testCreateTab() = runTest {
        val tab = tabManager.createTab("test.py", "print('hello')")
        assertNotNull(tab.id)
        assertEquals("test.py", tab.title)
        assertEquals("print('hello')", tab.content)
        assertFalse(tab.isModified)
    }

    @Test
    fun testCreateTabSetsActive() = runTest {
        val tab = tabManager.createTab()
        val state = tabManager.state.first()
        assertEquals(tab.id, state.activeTabId)
        assertEquals(1, state.tabs.size)
    }

    @Test
    fun testCreateMultipleTabs() = runTest {
        tabManager.createTab("tab1")
        tabManager.createTab("tab2")
        tabManager.createTab("tab3")
        val state = tabManager.state.first()
        assertEquals(3, state.tabs.size)
        // Last created tab should be active
        assertEquals(state.tabs[2].id, state.activeTabId)
    }

    // --- Close tab ---

    @Test
    fun testCloseTab() = runTest {
        val tab = tabManager.createTab()
        tabManager.closeTab(tab.id)
        val state = tabManager.state.first()
        assertTrue(state.tabs.isEmpty())
        assertNull(state.activeTabId)
    }

    @Test
    fun testCloseActiveTabSelectsNext() = runTest {
        val tab1 = tabManager.createTab("tab1")
        val tab2 = tabManager.createTab("tab2")
        tabManager.closeTab(tab2.id)
        val state = tabManager.state.first()
        assertEquals(1, state.tabs.size)
        assertEquals(tab1.id, state.activeTabId)
    }

    @Test
    fun testCloseLastRemainingTab() = runTest {
        val tab = tabManager.createTab()
        tabManager.closeTab(tab.id)
        val state = tabManager.state.first()
        assertNull(state.activeTabId)
    }

    @Test
    fun testCloseNonExistentTab() = runTest {
        tabManager.createTab()
        tabManager.closeTab("nonexistent_id")
        val state = tabManager.state.first()
        assertEquals(1, state.tabs.size)
    }

    // --- Set active tab ---

    @Test
    fun testSetActiveTab() = runTest {
        val tab1 = tabManager.createTab("tab1")
        val tab2 = tabManager.createTab("tab2")
        tabManager.setActiveTab(tab1.id)
        val state = tabManager.state.first()
        assertEquals(tab1.id, state.activeTabId)
    }

    // --- Update tab content ---

    @Test
    fun testUpdateTabContent() = runTest {
        val tab = tabManager.createTab()
        tabManager.updateTabContent(tab.id, "new content")
        val updated = tabManager.getTab(tab.id)
        assertNotNull(updated)
        assertEquals("new content", updated!!.content)
        assertTrue(updated.isModified)
    }

    // --- Update tab title ---

    @Test
    fun testUpdateTabTitle() = runTest {
        val tab = tabManager.createTab("old.py")
        tabManager.updateTabTitle(tab.id, "new.py")
        val updated = tabManager.getTab(tab.id)
        assertEquals("new.py", updated!!.title)
    }

    // --- Update cursor position ---

    @Test
    fun testUpdateCursorPosition() = runTest {
        val tab = tabManager.createTab()
        tabManager.updateTabCursorPosition(tab.id, CursorPosition(5, 10))
        val updated = tabManager.getTab(tab.id)
        assertEquals(CursorPosition(5, 10), updated!!.cursorPosition)
    }

    // --- Update scroll offset ---

    @Test
    fun testUpdateScrollOffset() = runTest {
        val tab = tabManager.createTab()
        tabManager.updateTabScrollOffset(tab.id, 100)
        val updated = tabManager.getTab(tab.id)
        assertEquals(100, updated!!.scrollOffset)
    }

    // --- Mark tab saved ---

    @Test
    fun testMarkTabSaved() = runTest {
        val tab = tabManager.createTab()
        tabManager.updateTabContent(tab.id, "modified")
        assertTrue(tabManager.getTab(tab.id)!!.isModified)
        tabManager.markTabSaved(tab.id)
        assertFalse(tabManager.getTab(tab.id)!!.isModified)
    }

    // --- Split mode ---

    @Test
    fun testSetSplitMode() = runTest {
        tabManager.setSplitMode(SplitMode.HORIZONTAL)
        val state = tabManager.state.first()
        assertEquals(SplitMode.HORIZONTAL, state.splitMode)
    }

    @Test
    fun testSetSplitModeVertical() = runTest {
        tabManager.setSplitMode(SplitMode.VERTICAL)
        val state = tabManager.state.first()
        assertEquals(SplitMode.VERTICAL, state.splitMode)
    }

    @Test
    fun testSetSplitModeNone() = runTest {
        tabManager.setSplitMode(SplitMode.HORIZONTAL)
        tabManager.setSplitMode(SplitMode.NONE)
        val state = tabManager.state.first()
        assertEquals(SplitMode.NONE, state.splitMode)
    }

    // --- Get active tab ---

    @Test
    fun testGetActiveTab() = runTest {
        val tab = tabManager.createTab("active.py")
        val active = tabManager.getActiveTab()
        assertNotNull(active)
        assertEquals("active.py", active!!.title)
    }

    @Test
    fun testGetActiveTabEmpty() = runTest {
        val active = tabManager.getActiveTab()
        assertNull(active)
    }

    // --- Get all tabs ---

    @Test
    fun testGetAllTabs() = runTest {
        tabManager.createTab("a.py")
        tabManager.createTab("b.py")
        val all = tabManager.getAllTabs()
        assertEquals(2, all.size)
    }

    // --- Get modified tabs ---

    @Test
    fun testGetModifiedTabs() = runTest {
        val tab1 = tabManager.createTab("a.py")
        val tab2 = tabManager.createTab("b.py")
        tabManager.updateTabContent(tab1.id, "modified")
        val modified = tabManager.getModifiedTabs()
        assertEquals(1, modified.size)
        assertEquals(tab1.id, modified[0].id)
    }

    // --- Close all tabs ---

    @Test
    fun testCloseAllTabs() = runTest {
        tabManager.createTab("a.py")
        tabManager.createTab("b.py")
        tabManager.createTab("c.py")
        tabManager.closeAllTabs()
        val state = tabManager.state.first()
        assertTrue(state.tabs.isEmpty())
        assertNull(state.activeTabId)
    }

    // --- Close other tabs ---

    @Test
    fun testCloseOtherTabs() = runTest {
        val tab1 = tabManager.createTab("a.py")
        tabManager.createTab("b.py")
        tabManager.createTab("c.py")
        tabManager.closeOtherTabs(tab1.id)
        val state = tabManager.state.first()
        assertEquals(1, state.tabs.size)
        assertEquals(tab1.id, state.tabs[0].id)
        assertEquals(tab1.id, state.activeTabId)
    }

    // --- Move tab ---

    @Test
    fun testMoveTab() = runTest {
        val tab1 = tabManager.createTab("a.py")
        val tab2 = tabManager.createTab("b.py")
        tabManager.moveTab(0, 1)
        val state = tabManager.state.first()
        assertEquals(tab2.id, state.tabs[0].id)
        assertEquals(tab1.id, state.tabs[1].id)
    }

    @Test
    fun testMoveTabOutOfBounds() = runTest {
        tabManager.createTab("a.py")
        tabManager.moveTab(0, 5) // out of bounds, should be no-op
        val state = tabManager.state.first()
        assertEquals(1, state.tabs.size)
    }

    @Test
    fun testMoveTabNegativeIndex() = runTest {
        tabManager.createTab("a.py")
        tabManager.moveTab(-1, 0)
        val state = tabManager.state.first()
        assertEquals(1, state.tabs.size)
    }

    // --- Duplicate tab ---

    @Test
    fun testDuplicateTab() = runTest {
        val original = tabManager.createTab("original.py", "content here")
        val duplicate = tabManager.duplicateTab(original.id)
        assertNotNull(duplicate)
        assertEquals("original.py (Copy)", duplicate!!.title)
        assertEquals("content here", duplicate.content)
        val state = tabManager.state.first()
        assertEquals(2, state.tabs.size)
        assertEquals(duplicate.id, state.activeTabId)
    }

    @Test
    fun testDuplicateNonExistentTab() = runTest {
        val result = tabManager.duplicateTab("nonexistent")
        assertNull(result)
    }

    // --- Concurrency: rapid operations (§10: thread safety) ---

    @Test
    fun testRapidCreateClose() = runTest {
        // Create and close tabs rapidly (§10: concurrency stress)
        repeat(20) {
            val tab = tabManager.createTab("tab_$it")
            tabManager.closeTab(tab.id)
        }
        val state = tabManager.state.first()
        assertTrue(state.tabs.isEmpty())
    }

    @Test
    fun testRapidCreateMultiple() = runTest {
        repeat(50) {
            tabManager.createTab("tab_$it")
        }
        val state = tabManager.state.first()
        assertEquals(50, state.tabs.size)
    }
}
