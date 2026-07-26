package com.pythonide.data.intellisense.core

import com.pythonide.domain.model.intellisense.CompletionKind
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
class BackgroundIndexerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var indexer: BackgroundIndexer

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        indexer = BackgroundIndexer()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Index file ---

    @Test
    fun testIndexFileWithClass() = runTest {
        val content = """
            class MyClass:
                pass
        """.trimIndent()
        indexer.indexFile("test.py", content)
        val entry = indexer.getEntry("MyClass")
        assertNotNull(entry)
        assertEquals(CompletionKind.CLASS, entry!!.kind)
    }

    @Test
    fun testIndexFileWithFunction() = runTest {
        val content = """
            def my_func():
                pass
        """.trimIndent()
        indexer.indexFile("test.py", content)
        val entry = indexer.getEntry("my_func")
        assertNotNull(entry)
        assertEquals(CompletionKind.FUNCTION, entry!!.kind)
    }

    @Test
    fun testIndexFileWithAsyncFunction() = runTest {
        val content = """
            async def my_async_func():
                pass
        """.trimIndent()
        indexer.indexFile("test.py", content)
        val entry = indexer.getEntry("my_async_func")
        assertNotNull(entry)
        assertEquals(CompletionKind.METHOD, entry!!.kind)
    }

    @Test
    fun testIndexFileWithVariable() = runTest {
        val content = "x = 42"
        indexer.indexFile("test.py", content)
        val entry = indexer.getEntry("x")
        assertNotNull(entry)
        assertEquals(CompletionKind.VARIABLE, entry!!.kind)
        assertEquals("int", entry!!.type)
    }

    @Test
    fun testIndexFileWithImport() = runTest {
        val content = "import os"
        indexer.indexFile("test.py", content)
        val entry = indexer.getEntry("os")
        assertNotNull(entry)
        assertEquals(CompletionKind.MODULE, entry!!.kind)
    }

    @Test
    fun testIndexFileWithFromImport() = runTest {
        val content = "from os import path"
        indexer.indexFile("test.py", content)
        val entry = indexer.getEntry("path")
        assertNotNull(entry)
        assertEquals(CompletionKind.IMPORT, entry!!.kind)
    }

    @Test
    fun testIndexFileWithDocstring() = runTest {
        val content = "def my_func():\n    \"\"\"This is a docstring.\"\"\"\n    pass"
        indexer.indexFile("test.py", content)
        val entry = indexer.getEntry("my_func")
        assertNotNull(entry)
        assertTrue(entry!!.documentation!!.contains("docstring"))
    }

    // --- §D69: Line indexing consistency ---

    @Test
    fun testIndexLineNumbers() = runTest {
        val content = "x = 1\ny = 2\ndef foo():\n    pass"
        indexer.indexFile("test.py", content)
        val x = indexer.getEntry("x")
        val y = indexer.getEntry("y")
        val foo = indexer.getEntry("foo")
        assertEquals(0, x!!.line)
        assertEquals(1, y!!.line)
        assertEquals(2, foo!!.line)
    }

    // --- Search entries ---

    @Test
    fun testSearchEntries() = runTest {
        indexer.indexFile("test.py", "def my_function():\n    pass\nclass MyClass:\n    pass")
        val results = indexer.searchEntries("my")
        assertTrue(results.any { it.name == "my_function" })
        assertTrue(results.any { it.name == "MyClass" })
    }

    @Test
    fun testSearchEntriesCaseInsensitive() = runTest {
        indexer.indexFile("test.py", "def MyFunc():\n    pass")
        val results = indexer.searchEntries("myfunc")
        assertEquals(1, results.size)
    }

    // --- Multiple files ---

    @Test
    fun testIndexMultipleFiles() = runTest {
        val files = mapOf(
            "a.py" to "class A:\n    pass",
            "b.py" to "def b_func():\n    pass"
        )
        indexer.indexFiles(files)
        assertNotNull(indexer.getEntry("A"))
        assertNotNull(indexer.getEntry("b_func"))
        val stats = indexer.stats.first()
        assertEquals(2, stats.totalFiles)
    }

    @Test
    fun testIndexFilesClearsPrevious() = runTest {
        indexer.indexFile("a.py", "class Old:\n    pass")
        indexer.indexFiles(mapOf("b.py" to "class New:\n    pass"))
        assertNull(indexer.getEntry("Old"))
        assertNotNull(indexer.getEntry("New"))
    }

    // --- Remove file ---

    @Test
    fun testRemoveFile() = runTest {
        indexer.indexFile("test.py", "class MyClass:\n    pass")
        assertNotNull(indexer.getEntry("MyClass"))
        indexer.removeFile("test.py")
        assertNull(indexer.getEntry("MyClass"))
    }

    @Test
    fun testRemoveNonExistentFile() = runTest {
        indexer.removeFile("nonexistent.py") // should not crash
    }

    // --- Clear index ---

    @Test
    fun testClearIndex() = runTest {
        indexer.indexFile("test.py", "class MyClass:\n    pass")
        indexer.clearIndex()
        assertNull(indexer.getEntry("MyClass"))
        val stats = indexer.stats.first()
        assertEquals(0, stats.totalEntries)
    }

    // --- Convert to completion items ---

    @Test
    fun testConvertToCompletionItems() = runTest {
        indexer.indexFile("test.py", "def my_func():\n    pass\nclass MyClass:\n    pass")
        val items = indexer.convertToCompletionItems()
        assertTrue(items.any { it.label == "my_func" && it.kind == CompletionKind.FUNCTION })
        assertTrue(items.any { it.label == "MyClass" && it.kind == CompletionKind.CLASS })
    }

    // --- §A22: Empty file indexing ---

    @Test
    fun testIndexEmptyFile() = runTest {
        indexer.indexFile("empty.py", "")
        val stats = indexer.stats.first()
        assertEquals(0, stats.totalEntries)
    }

    @Test
    fun testIndexCommentsOnly() = runTest {
        indexer.indexFile("comments.py", "# comment1\n# comment2")
        val stats = indexer.stats.first()
        assertEquals(0, stats.totalEntries)
    }

    // --- Variable type inference ---

    @Test
    fun testInferTypeString() = runTest {
        indexer.indexFile("test.py", "x = \"hello\"")
        val entry = indexer.getEntry("x")
        assertEquals("str", entry!!.type)
    }

    @Test
    fun testInferTypeBool() = runTest {
        indexer.indexFile("test.py", "flag = True")
        val entry = indexer.getEntry("flag")
        assertEquals("bool", entry!!.type)
    }

    @Test
    fun testInferTypeNone() = runTest {
        indexer.indexFile("test.py", "val = None")
        val entry = indexer.getEntry("val")
        assertEquals("None", entry!!.type)
    }

    @Test
    fun testInferTypeFloat() = runTest {
        indexer.indexFile("test.py", "pi = 3.14")
        val entry = indexer.getEntry("pi")
        assertEquals("float", entry!!.type)
    }

    @Test
    fun testInferTypeList() = runTest {
        indexer.indexFile("test.py", "items = [1, 2, 3]")
        val entry = indexer.getEntry("items")
        assertEquals("list", entry!!.type)
    }

    @Test
    fun testInferTypeDict() = runTest {
        indexer.indexFile("test.py", "d = {'a': 1}")
        val entry = indexer.getEntry("d")
        assertEquals("dict", entry!!.type)
    }

    // --- Scope tracking ---

    @Test
    fun testClassScope() = runTest {
        val content = """
            class Outer:
                def method(self):
                    pass
        """.trimIndent()
        indexer.indexFile("test.py", content)
        val method = indexer.getEntry("method")
        assertNotNull(method)
        assertEquals("Outer", method!!.scope)
    }

    // --- Get entries for file ---

    @Test
    fun testGetEntriesForFile() = runTest {
        indexer.indexFile("a.py", "class A:\n    pass")
        indexer.indexFile("b.py", "class B:\n    pass")
        val aEntries = indexer.getEntriesForFile("a.py")
        assertEquals(1, aEntries.size)
        assertEquals("A", aEntries[0].name)
    }

    @Test
    fun testGetEntriesForNonExistentFile() = runTest {
        val entries = indexer.getEntriesForFile("nonexistent.py")
        assertTrue(entries.isEmpty())
    }

    // --- Get all entries ---

    @Test
    fun testGetAllEntries() = runTest {
        indexer.indexFile("a.py", "class A:\n    pass")
        indexer.indexFile("b.py", "def b_func():\n    pass")
        val all = indexer.getAllEntries()
        assertEquals(2, all.size)
    }

    // --- Stats ---

    @Test
    fun testStatsUpdate() = runTest {
        val initialStats = indexer.stats.first()
        assertEquals(0, initialStats.totalFiles)
        assertEquals(0, initialStats.totalEntries)

        indexer.indexFile("test.py", "x = 1\ndef foo():\n    pass")
        val updatedStats = indexer.stats.first()
        assertEquals(1, updatedStats.totalFiles)
        assertEquals(2, updatedStats.totalEntries)
    }

    // --- Private variable exclusion ---

    @Test
    fun testPrivateVariableExcluded() = runTest {
        indexer.indexFile("test.py", "_private = 1")
        assertNull(indexer.getEntry("_private"))
    }

    @Test
    fun testDunderVariableIncluded() = runTest {
        indexer.indexFile("test.py", "__all__ = ['a', 'b']")
        val entry = indexer.getEntry("__all__")
        assertNotNull(entry)
    }
}
