package com.pythonide.domain.model.intellisense

import org.junit.Assert.*
import org.junit.Test

class IntelliSenseModelsTest {

    // --- CompletionItem ---

    @Test
    fun testCompletionItemDefaults() {
        val item = CompletionItem(id = "1", label = "foo", kind = CompletionKind.KEYWORD)
        assertEquals("1", item.id)
        assertEquals("foo", item.label)
        assertEquals(CompletionKind.KEYWORD, item.kind)
        assertNull(item.detail)
        assertNull(item.documentation)
        assertNull(item.snippet)
        assertNull(item.parameters)
        assertNull(item.returnType)
        assertFalse(item.deprecated)
        assertEquals(0, item.priority)
        assertEquals("foo", item.insertText)
        assertEquals("foo", item.filterText)
        assertEquals(0, item.sortOrder)
    }

    @Test
    fun testCompletionItemWithParameters() {
        val item = CompletionItem(
            id = "1",
            label = "print",
            kind = CompletionKind.FUNCTION,
            parameters = listOf(
                ParameterInfo(name = "objects"),
                ParameterInfo(name = "sep", defaultValue = " ")
            ),
            returnType = "None"
        )
        assertEquals(2, item.parameters!!.size)
        assertEquals("None", item.returnType)
    }

    // --- CompletionKind ---

    @Test
    fun testCompletionKindValues() {
        assertEquals(21, CompletionKind.entries.size)
        assertTrue(CompletionKind.entries.contains(CompletionKind.KEYWORD))
        assertTrue(CompletionKind.entries.contains(CompletionKind.BUILTIN))
        assertTrue(CompletionKind.entries.contains(CompletionKind.MODULE))
        assertTrue(CompletionKind.entries.contains(CompletionKind.FUNCTION))
        assertTrue(CompletionKind.entries.contains(CompletionKind.CLASS))
        assertTrue(CompletionKind.entries.contains(CompletionKind.SNIPPET))
    }

    // --- ParameterInfo ---

    @Test
    fun testParameterInfoDefaults() {
        val param = ParameterInfo(name = "x")
        assertEquals("x", param.name)
        assertNull(param.type)
        assertNull(param.documentation)
        assertNull(param.defaultValue)
        assertFalse(param.isOptional)
    }

    @Test
    fun testParameterInfoOptional() {
        val param = ParameterInfo(name = "sep", isOptional = true, defaultValue = " ")
        assertTrue(param.isOptional)
        assertEquals(" ", param.defaultValue)
    }

    // --- Diagnostic ---

    @Test
    fun testDiagnosticDefaults() {
        val diag = Diagnostic(
            id = "d1",
            range = DiagnosticRange(0, 0, 0, 10),
            severity = DiagnosticSeverity.ERROR,
            message = "error"
        )
        assertNull(diag.code)
        assertEquals("pylint", diag.source)
        assertNull(diag.relatedInformation)
        assertNull(diag.tags)
        assertNull(diag.quickFixes)
    }

    @Test
    fun testDiagnosticWithQuickFix() {
        val fix = QuickFix(
            id = "f1",
            label = "Add colon",
            kind = QuickFixKind.QUICK_FIX,
            diagnostics = listOf("E0001")
        )
        val diag = Diagnostic(
            id = "d1",
            range = DiagnosticRange(0, 0, 0, 10),
            severity = DiagnosticSeverity.ERROR,
            message = "missing colon",
            quickFixes = listOf(fix)
        )
        assertEquals(1, diag.quickFixes!!.size)
        assertEquals("Add colon", diag.quickFixes!![0].label)
    }

    // --- DiagnosticSeverity ---

    @Test
    fun testDiagnosticSeverityValues() {
        assertEquals(4, DiagnosticSeverity.entries.size)
        assertTrue(DiagnosticSeverity.entries.contains(DiagnosticSeverity.ERROR))
        assertTrue(DiagnosticSeverity.entries.contains(DiagnosticSeverity.WARNING))
        assertTrue(DiagnosticSeverity.entries.contains(DiagnosticSeverity.INFO))
        assertTrue(DiagnosticSeverity.entries.contains(DiagnosticSeverity.HINT))
    }

    // --- DiagnosticTag ---

    @Test
    fun testDiagnosticTagValues() {
        assertEquals(2, DiagnosticTag.entries.size)
        assertTrue(DiagnosticTag.entries.contains(DiagnosticTag.UNNECESSARY))
        assertTrue(DiagnosticTag.entries.contains(DiagnosticTag.DEPRECATED))
    }

    // --- QuickFixKind ---

    @Test
    fun testQuickFixKindValues() {
        assertEquals(4, QuickFixKind.entries.size)
        assertTrue(QuickFixKind.entries.contains(QuickFixKind.QUICK_FIX))
        assertTrue(QuickFixKind.entries.contains(QuickFixKind.REFACTOR))
        assertTrue(QuickFixKind.entries.contains(QuickFixKind.SOURCE))
        assertTrue(QuickFixKind.entries.contains(QuickFixKind.ERROR))
    }

    // --- Snippet ---

    @Test
    fun testSnippetDefaults() {
        val snippet = Snippet(prefix = "def", body = "def \${1:name}():\n    \${2:pass}", description = "Function")
        assertEquals("def", snippet.prefix)
        assertEquals("python", snippet.scope)
        assertTrue(snippet.placeholders.isEmpty())
    }

    @Test
    fun testSnippetWithPlaceholders() {
        val snippet = Snippet(
            prefix = "def",
            body = "def \${1:name}():\n    \${2:pass}",
            description = "Function",
            placeholders = listOf(
                SnippetPlaceholder(index = 1, placeholder = "name"),
                SnippetPlaceholder(index = 2, placeholder = "pass", defaultValue = "pass")
            )
        )
        assertEquals(2, snippet.placeholders.size)
        assertEquals("pass", snippet.placeholders[1].defaultValue)
    }

    // --- ImportSuggestion ---

    @Test
    fun testImportSuggestionDefaults() {
        val suggestion = ImportSuggestion(module = "os", name = "getcwd")
        assertEquals("os", suggestion.module)
        assertNull(suggestion.fromModule)
        assertEquals("getcwd", suggestion.name)
        assertNull(suggestion.alias)
        assertFalse(suggestion.isUsed)
        assertEquals(0, suggestion.line)
    }

    // --- IndexEntry ---

    @Test
    fun testIndexEntryDefaults() {
        val entry = IndexEntry(name = "foo", kind = CompletionKind.FUNCTION)
        assertNull(entry.filePath)
        assertEquals(0, entry.line)
        assertEquals(0, entry.column)
        assertNull(entry.signature)
        assertNull(entry.documentation)
        assertNull(entry.type)
        assertNull(entry.scope)
        assertTrue(entry.isExported)
    }

    // --- IndexStats ---

    @Test
    fun testIndexStats() {
        val stats = IndexStats(
            totalFiles = 10,
            totalEntries = 100,
            lastUpdated = System.currentTimeMillis(),
            indexingTimeMs = 500
        )
        assertEquals(10, stats.totalFiles)
        assertEquals(100, stats.totalEntries)
        assertEquals(500, stats.indexingTimeMs)
    }

    // --- IntelliSenseConfig ---

    @Test
    fun testIntelliSenseConfigDefaults() {
        val config = IntelliSenseConfig()
        assertTrue(config.enableAutoComplete)
        assertTrue(config.enableParameterHints)
        assertTrue(config.enableDocumentation)
        assertTrue(config.enableSnippets)
        assertTrue(config.enableImportSuggestions)
        assertTrue(config.enableDiagnostics)
        assertTrue(config.enableLinting)
        assertTrue(config.enableQuickFixes)
        assertEquals(300L, config.autoCompleteDelay)
        assertEquals(50, config.maxCompletions)
        assertTrue(config.enableBackgroundIndexing)
    }

    // --- IntelliSenseState ---

    @Test
    fun testIntelliSenseStateDefaults() {
        val state = IntelliSenseState()
        assertTrue(state.completions.isEmpty())
        assertNull(state.parameterHints)
        assertTrue(state.diagnostics.isEmpty())
        assertFalse(state.isCompletionsVisible)
        assertFalse(state.isParameterHintsVisible)
        assertEquals(0, state.selectedIndex)
        assertEquals(0, state.triggerOffset)
        assertNull(state.triggerCharacter)
    }

    // --- ParameterHints ---

    @Test
    fun testParameterHintsDefaults() {
        val hints = ParameterHints(
            functionName = "print",
            parameters = listOf(ParameterInfo(name = "objects"))
        )
        assertEquals(0, hints.activeParameter)
        assertNull(hints.documentation)
    }

    // --- TextEdit ---

    @Test
    fun testTextEdit() {
        val edit = TextEdit(
            range = DiagnosticRange(0, 0, 0, 5),
            newText = "hello"
        )
        assertEquals("hello", edit.newText)
        assertEquals(0, edit.range.startLine)
    }

    // --- Command ---

    @Test
    fun testCommand() {
        val cmd = Command(id = "quickfix.add_colon", title = "Add colon")
        assertEquals("quickfix.add_colon", cmd.id)
        assertEquals("Add colon", cmd.title)
        assertNull(cmd.arguments)
    }
}
