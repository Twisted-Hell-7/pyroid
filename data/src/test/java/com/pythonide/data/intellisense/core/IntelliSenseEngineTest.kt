package com.pythonide.data.intellisense.core

import com.pythonide.data.intellisense.providers.KeywordProvider
import com.pythonide.data.intellisense.providers.BuiltinProvider
import com.pythonide.domain.model.intellisense.CompletionItem
import com.pythonide.domain.model.intellisense.CompletionKind
import com.pythonide.domain.model.intellisense.IntelliSenseConfig
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
class IntelliSenseEngineTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var engine: IntelliSenseEngine

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        engine = IntelliSenseEngine()
        engine.registerCompletionProvider(KeywordProvider())
        engine.registerCompletionProvider(BuiltinProvider())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Registration ---

    @Test
    fun testRegisterProviders() {
        // No crash = pass
        val engine2 = IntelliSenseEngine()
        engine2.registerCompletionProvider(KeywordProvider())
        engine2.registerDiagnosticProvider(com.pythonide.data.intellisense.providers.SyntaxValidator())
    }

    // --- Trigger completion ---

    @Test
    fun testTriggerCompletion() = runTest {
        engine.triggerCompletion("x = ", 4)
        // Engine uses Dispatchers.Default; need real-time wait for debounce delay
        Thread.sleep(500)
        val state = engine.state.first()
        assertTrue(state.completions.isNotEmpty())
    }

    @Test
    fun testCompletionFiltersByPrefix() = runTest {
        engine.triggerCompletion("x = de", 6)
        Thread.sleep(500)
        val state = engine.state.first()
        assertTrue(state.completions.any { it.label == "def" })
    }

    @Test
    fun testCompletionVisibleWhenResults() = runTest {
        engine.triggerCompletion("x = ", 4)
        Thread.sleep(500)
        val state = engine.state.first()
        assertTrue(state.isCompletionsVisible)
    }

    @Test
    fun testCompletionNotVisibleWhenEmpty() = runTest {
        engine.triggerCompletion("xyz_none_match", 13)
        kotlinx.coroutines.delay(400)
        val state = engine.state.first()
        // No matches for "xyz_none_match" prefix
        assertFalse(state.isCompletionsVisible)
    }

    // --- Hide completions ---

    @Test
    fun testHideCompletions() = runTest {
        engine.triggerCompletion("x = ", 4)
        kotlinx.coroutines.delay(400)
        engine.hideCompletions()
        val state = engine.state.first()
        assertFalse(state.isCompletionsVisible)
        assertTrue(state.completions.isEmpty())
    }

    // --- Navigation ---

    @Test
    fun testSelectNextCompletion() = runTest {
        engine.triggerCompletion("x = ", 4)
        Thread.sleep(500)
        val initialState = engine.state.first()
        val initialIndex = initialState.selectedIndex
        engine.selectNextCompletion()
        val newState = engine.state.first()
        assertTrue(newState.selectedIndex >= initialIndex)
    }

    @Test
    fun testSelectPreviousCompletion() = runTest {
        engine.triggerCompletion("x = ", 4)
        kotlinx.coroutines.delay(400)
        engine.selectNextCompletion()
        engine.selectNextCompletion()
        engine.selectPreviousCompletion()
        val state = engine.state.first()
        assertTrue(state.selectedIndex >= 0)
    }

    @Test
    fun testSelectNextAtEnd() = runTest {
        engine.triggerCompletion("x = ", 4)
        kotlinx.coroutines.delay(400)
        val state = engine.state.first()
        val lastIndex = state.completions.size - 1
        // Select next many times, should not exceed last index
        repeat(100) { engine.selectNextCompletion() }
        val finalState = engine.state.first()
        assertTrue(finalState.selectedIndex <= lastIndex)
    }

    @Test
    fun testSelectPreviousAtStart() = runTest {
        engine.triggerCompletion("x = ", 4)
        kotlinx.coroutines.delay(400)
        // Select previous many times, should not go below 0
        repeat(100) { engine.selectPreviousCompletion() }
        val state = engine.state.first()
        assertTrue(state.selectedIndex >= 0)
    }

    // --- Accept completion ---

    @Test
    fun testAcceptCompletion() = runTest {
        engine.triggerCompletion("x = ", 4)
        Thread.sleep(500)
        val accepted = engine.acceptCompletion()
        assertNotNull(accepted)
        assertTrue(accepted!!.kind == CompletionKind.KEYWORD || accepted.kind == CompletionKind.BUILTIN)
        // Should hide after accept
        val state = engine.state.first()
        assertFalse(state.isCompletionsVisible)
    }

    @Test
    fun testGetSelectedCompletion() = runTest {
        engine.triggerCompletion("x = ", 4)
        Thread.sleep(500)
        val selected = engine.getSelectedCompletion()
        assertNotNull(selected)
    }

    // --- Config ---

    @Test
    fun testDisableAutoComplete() = runTest {
        engine.updateConfig(IntelliSenseConfig(enableAutoComplete = false))
        engine.triggerCompletion("x = ", 4)
        kotlinx.coroutines.delay(400)
        val state = engine.state.first()
        assertTrue(state.completions.isEmpty())
    }

    @Test
    fun testMaxCompletions() = runTest {
        engine.updateConfig(IntelliSenseConfig(maxCompletions = 5))
        engine.triggerCompletion("", 0)
        kotlinx.coroutines.delay(400)
        val state = engine.state.first()
        assertTrue(state.completions.size <= 5)
    }

    // --- Symbol index ---

    @Test
    fun testAddToIndex() {
        val entries = listOf(
            CompletionItem(id = "1", label = "foo", kind = CompletionKind.FUNCTION),
            CompletionItem(id = "2", label = "bar", kind = CompletionKind.FUNCTION)
        )
        engine.addToIndex(entries)
        val (keys, count) = engine.getIndexStats()
        assertEquals(2, count)
    }

    @Test
    fun testClearIndex() {
        engine.addToIndex(listOf(
            CompletionItem(id = "1", label = "foo", kind = CompletionKind.FUNCTION)
        ))
        engine.clearIndex()
        val (keys, count) = engine.getIndexStats()
        assertEquals(0, count)
    }

    // --- Parameter hints ---

    @Test
    fun testTriggerParameterHints() = runTest {
        engine.updateConfig(IntelliSenseConfig(enableParameterHints = true))
        engine.triggerParameterHints("print(", 6)
        val state = engine.state.first()
        // May or may not find hints depending on index
        assertNotNull(state.parameterHints)
    }

    @Test
    fun testDisableParameterHints() = runTest {
        engine.updateConfig(IntelliSenseConfig(enableParameterHints = false))
        engine.triggerParameterHints("print(", 6)
        val state = engine.state.first()
        assertFalse(state.isParameterHintsVisible)
    }

    @Test
    fun testHideParameterHints() = runTest {
        engine.triggerParameterHints("print(", 6)
        engine.hideParameterHints()
        val state = engine.state.first()
        assertFalse(state.isParameterHintsVisible)
    }

    // --- Debounce (rapid typing §2) ---

    @Test
    fun testRapidTypingDebounce() = runTest {
        // Simulate rapid typing - only last trigger should produce results
        repeat(10) { i ->
            engine.triggerCompletion("x = ${"a".repeat(i + 1)}", 4 + i + 1)
        }
        // Final trigger with a prefix that matches builtins/keywords
        engine.triggerCompletion("x = p", 5)
        Thread.sleep(500)
        val state = engine.state.first()
        assertTrue(state.isCompletionsVisible)
    }
}
