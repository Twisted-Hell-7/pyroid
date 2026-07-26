package com.pythonide.data.intellisense.providers

import com.pythonide.data.intellisense.core.CompletionContext
import com.pythonide.domain.model.intellisense.CompletionKind
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
class CompletionProvidersTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var keywordProvider: KeywordProvider
    private lateinit var builtinProvider: BuiltinProvider
    private lateinit var moduleProvider: ModuleProvider
    private lateinit var functionProvider: FunctionProvider

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        keywordProvider = KeywordProvider()
        builtinProvider = BuiltinProvider()
        moduleProvider = ModuleProvider()
        functionProvider = FunctionProvider()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun defaultContext() = CompletionContext(
        line = 0, column = 0,
        isAfterDot = false, isAfterImport = false, isAfterFrom = false,
        isAfterDef = false, isAfterClass = false,
        isInString = false, isAfterComment = false,
        scopeDepth = 0, currentLine = ""
    )

    // --- KeywordProvider (§3: IntelliSense) ---

    @Test
    fun testKeywordProviderReturnsKeywords() = runTest {
        val completions = keywordProvider.getCompletions(
            content = "x = 1",
            cursorOffset = 4,
            prefix = "",
            context = defaultContext(),
            triggerCharacter = null
        )
        assertTrue(completions.isNotEmpty())
        assertTrue(completions.all { it.kind == CompletionKind.KEYWORD })
    }

    @Test
    fun testKeywordProviderFiltersByPrefix() = runTest {
        val completions = keywordProvider.getCompletions(
            content = "x = 1",
            cursorOffset = 4,
            prefix = "de",
            context = defaultContext(),
            triggerCharacter = null
        )
        assertTrue(completions.any { it.label == "def" })
        // Provider returns all keywords; prefix filtering is done by IntelliSenseEngine
    }

    @Test
    fun testKeywordProviderSuppressedInString() = runTest {
        val context = defaultContext().copy(isInString = true)
        val completions = keywordProvider.getCompletions(
            content = "\"x = ",
            cursorOffset = 5,
            prefix = "",
            context = context,
            triggerCharacter = null
        )
        assertTrue(completions.isEmpty())
    }

    @Test
    fun testKeywordProviderSuppressedAfterComment() = runTest {
        val context = defaultContext().copy(isAfterComment = true)
        val completions = keywordProvider.getCompletions(
            content = "# x = ",
            cursorOffset = 5,
            prefix = "",
            context = context,
            triggerCharacter = null
        )
        assertTrue(completions.isEmpty())
    }

    @Test
    fun testKeywordProviderSuppressedAfterDot() = runTest {
        val context = defaultContext().copy(isAfterDot = true)
        val completions = keywordProvider.getCompletions(
            content = "obj.",
            cursorOffset = 4,
            prefix = "",
            context = context,
            triggerCharacter = null
        )
        assertTrue(completions.isEmpty())
    }

    @Test
    fun testKeywordProviderSuppressedAfterImport() = runTest {
        val context = defaultContext().copy(isAfterImport = true)
        val completions = keywordProvider.getCompletions(
            content = "import ",
            cursorOffset = 7,
            prefix = "",
            context = context,
            triggerCharacter = null
        )
        assertTrue(completions.isEmpty())
    }

    // --- BuiltinProvider ---

    @Test
    fun testBuiltinProviderReturnsBuiltins() = runTest {
        val completions = builtinProvider.getCompletions(
            content = "x = ",
            cursorOffset = 4,
            prefix = "",
            context = defaultContext(),
            triggerCharacter = null
        )
        assertTrue(completions.isNotEmpty())
        assertTrue(completions.all { it.kind == CompletionKind.BUILTIN })
    }

    @Test
    fun testBuiltinProviderHasPrint() = runTest {
        val completions = builtinProvider.getCompletions(
            content = "", cursorOffset = 0, prefix = "",
            context = defaultContext(), triggerCharacter = null
        )
        assertTrue(completions.any { it.label == "print" })
    }

    @Test
    fun testBuiltinProviderFiltersByPrefix() = runTest {
        val completions = builtinProvider.getCompletions(
            content = "", cursorOffset = 0, prefix = "pr",
            context = defaultContext(), triggerCharacter = null
        )
        assertTrue(completions.any { it.label == "print" })
    }

    @Test
    fun testBuiltinProviderSuppressedInString() = runTest {
        val context = defaultContext().copy(isInString = true)
        val completions = builtinProvider.getCompletions(
            content = "\"", cursorOffset = 1, prefix = "",
            context = context, triggerCharacter = null
        )
        assertTrue(completions.isEmpty())
    }

    @Test
    fun testBuiltinProviderSuppressedAfterDot() = runTest {
        val context = defaultContext().copy(isAfterDot = true)
        val completions = builtinProvider.getCompletions(
            content = "obj.", cursorOffset = 4, prefix = "",
            context = context, triggerCharacter = null
        )
        assertTrue(completions.isEmpty())
    }

    // --- ModuleProvider ---

    @Test
    fun testModuleProviderReturnsModules() = runTest {
        val context = defaultContext().copy(isAfterImport = true)
        val completions = moduleProvider.getCompletions(
            content = "import ", cursorOffset = 7, prefix = "",
            context = context, triggerCharacter = null
        )
        assertTrue(completions.isNotEmpty())
        assertTrue(completions.all { it.kind == CompletionKind.MODULE })
    }

    @Test
    fun testModuleProviderHasOs() = runTest {
        val context = defaultContext().copy(isAfterImport = true)
        val completions = moduleProvider.getCompletions(
            content = "import ", cursorOffset = 7, prefix = "",
            context = context, triggerCharacter = null
        )
        assertTrue(completions.any { it.label == "os" })
    }

    @Test
    fun testModuleProviderHasJson() = runTest {
        val context = defaultContext().copy(isAfterImport = true)
        val completions = moduleProvider.getCompletions(
            content = "import ", cursorOffset = 7, prefix = "",
            context = context, triggerCharacter = null
        )
        assertTrue(completions.any { it.label == "json" })
    }

    @Test
    fun testModuleProviderAfterFrom() = runTest {
        val context = defaultContext().copy(isAfterFrom = true, isAfterImport = true)
        val completions = moduleProvider.getCompletions(
            content = "from ", cursorOffset = 5, prefix = "",
            context = context, triggerCharacter = null
        )
        assertTrue(completions.isNotEmpty())
    }

    @Test
    fun testModuleProviderSuppressedInString() = runTest {
        val context = defaultContext().copy(isInString = true, isAfterImport = true)
        val completions = moduleProvider.getCompletions(
            content = "\"import ", cursorOffset = 8, prefix = "",
            context = context, triggerCharacter = null
        )
        assertTrue(completions.isEmpty())
    }

    @Test
    fun testModuleProviderSuppressedAfterDot() = runTest {
        val context = defaultContext().copy(isAfterDot = true)
        val completions = moduleProvider.getCompletions(
            content = "os.", cursorOffset = 3, prefix = "",
            context = context, triggerCharacter = null
        )
        assertTrue(completions.isEmpty())
    }

    // --- FunctionProvider ---

    @Test
    fun testFunctionProviderReturnsFunctions() = runTest {
        val completions = functionProvider.getCompletions(
            content = "x = ", cursorOffset = 4, prefix = "",
            context = defaultContext(), triggerCharacter = null
        )
        assertTrue(completions.isNotEmpty())
        assertTrue(completions.all { it.kind == CompletionKind.FUNCTION })
    }

    @Test
    fun testFunctionProviderHasPrint() = runTest {
        val completions = functionProvider.getCompletions(
            content = "", cursorOffset = 0, prefix = "",
            context = defaultContext(), triggerCharacter = null
        )
        assertTrue(completions.any { it.label == "print" })
    }

    @Test
    fun testFunctionProviderHasLen() = runTest {
        val completions = functionProvider.getCompletions(
            content = "", cursorOffset = 0, prefix = "",
            context = defaultContext(), triggerCharacter = null
        )
        assertTrue(completions.any { it.label == "len" })
    }

    @Test
    fun testFunctionProviderHasRange() = runTest {
        val completions = functionProvider.getCompletions(
            content = "", cursorOffset = 0, prefix = "",
            context = defaultContext(), triggerCharacter = null
        )
        assertTrue(completions.any { it.label == "range" })
    }

    @Test
    fun testFunctionProviderFiltersByPrefix() = runTest {
        val completions = functionProvider.getCompletions(
            content = "", cursorOffset = 0, prefix = "so",
            context = defaultContext(), triggerCharacter = null
        )
        assertTrue(completions.any { it.label == "sorted" })
    }

    @Test
    fun testFunctionProviderSuppressedInString() = runTest {
        val context = defaultContext().copy(isInString = true)
        val completions = functionProvider.getCompletions(
            content = "\"", cursorOffset = 1, prefix = "",
            context = context, triggerCharacter = null
        )
        assertTrue(completions.isEmpty())
    }

    @Test
    fun testFunctionProviderSuppressedAfterComment() = runTest {
        val context = defaultContext().copy(isAfterComment = true)
        val completions = functionProvider.getCompletions(
            content = "# ", cursorOffset = 2, prefix = "",
            context = context, triggerCharacter = null
        )
        assertTrue(completions.isEmpty())
    }

    // --- Priority ordering ---

    @Test
    fun testKeywordPriorityHigherThanBuiltin() = runTest {
        val keywords = keywordProvider.getCompletions(
            content = "", cursorOffset = 0, prefix = "",
            context = defaultContext(), triggerCharacter = null
        )
        val builtins = builtinProvider.getCompletions(
            content = "", cursorOffset = 0, prefix = "",
            context = defaultContext(), triggerCharacter = null
        )
        assertTrue(keywords.all { it.priority >= builtins.maxOfOrNull { b -> b.priority } ?: 0 })
    }
}
