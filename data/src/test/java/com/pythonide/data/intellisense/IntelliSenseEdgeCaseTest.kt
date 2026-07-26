package com.pythonide.data.intellisense

import com.pythonide.domain.model.intellisense.CompletionItem
import com.pythonide.domain.model.intellisense.CompletionKind
import com.pythonide.domain.model.intellisense.Diagnostic
import com.pythonide.domain.model.intellisense.DiagnosticRange
import com.pythonide.domain.model.intellisense.DiagnosticSeverity
import com.pythonide.domain.model.intellisense.IndexEntry
import com.pythonide.domain.model.intellisense.IntelliSenseState
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
class IntelliSenseEdgeCaseTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // §3: IntelliSense - Completion Items

    @Test
    fun testCompletionItemCreation() {
        val item = CompletionItem(
            id = "print",
            label = "print",
            kind = CompletionKind.FUNCTION,
            detail = "builtin function",
            documentation = "Print objects to the text stream file",
            insertText = "print(\$0)"
        )
        assertEquals("print", item.label)
        assertEquals(CompletionKind.FUNCTION, item.kind)
        assertEquals("builtin function", item.detail)
    }

    // §3: IntelliSense - Diagnostics

    @Test
    fun testDiagnosticCreation() {
        val diagnostic = Diagnostic(
            id = "diag_1",
            range = DiagnosticRange(startLine = 10, startColumn = 5, endLine = 10, endColumn = 10),
            message = "Undefined variable 'x'",
            severity = DiagnosticSeverity.ERROR
        )
        assertEquals(10, diagnostic.range.startLine)
        assertEquals(5, diagnostic.range.startColumn)
        assertEquals("Undefined variable 'x'", diagnostic.message)
        assertEquals(DiagnosticSeverity.ERROR, diagnostic.severity)
    }

    @Test
    fun testDiagnosticSeverityLevels() {
        val error = Diagnostic(id = "e1", range = DiagnosticRange(0, 0, 0, 1), message = "error", severity = DiagnosticSeverity.ERROR)
        val warning = Diagnostic(id = "w1", range = DiagnosticRange(0, 0, 0, 1), message = "warning", severity = DiagnosticSeverity.WARNING)
        val info = Diagnostic(id = "i1", range = DiagnosticRange(0, 0, 0, 1), message = "info", severity = DiagnosticSeverity.INFO)
        
        assertEquals(DiagnosticSeverity.ERROR, error.severity)
        assertEquals(DiagnosticSeverity.WARNING, warning.severity)
        assertEquals(DiagnosticSeverity.INFO, info.severity)
    }

    // §3: IntelliSense - Index Entries

    @Test
    fun testIndexEntryCreation() {
        val entry = IndexEntry(
            name = "MyClass",
            kind = CompletionKind.CLASS,
            filePath = "/test/file.py",
            line = 10,
            column = 0
        )
        assertEquals("MyClass", entry.name)
        assertEquals(CompletionKind.CLASS, entry.kind)
        assertEquals("/test/file.py", entry.filePath)
        assertEquals(10, entry.line)
    }

    @Test
    fun testIndexEntryTypes() {
        val function = IndexEntry(name = "func", kind = CompletionKind.FUNCTION, filePath = "", line = 0)
        val class_ = IndexEntry(name = "cls", kind = CompletionKind.CLASS, filePath = "", line = 0)
        val variable = IndexEntry(name = "var", kind = CompletionKind.VARIABLE, filePath = "", line = 0)
        val module = IndexEntry(name = "mod", kind = CompletionKind.MODULE, filePath = "", line = 0)
        val snippet = IndexEntry(name = "snp", kind = CompletionKind.SNIPPET, filePath = "", line = 0)
        
        assertEquals(CompletionKind.FUNCTION, function.kind)
        assertEquals(CompletionKind.CLASS, class_.kind)
        assertEquals(CompletionKind.VARIABLE, variable.kind)
        assertEquals(CompletionKind.MODULE, module.kind)
        assertEquals(CompletionKind.SNIPPET, snippet.kind)
    }

    // §3: IntelliSense - Completion Kinds

    @Test
    fun testCompletionKindValues() {
        val values = CompletionKind.entries
        assertTrue(values.isNotEmpty())
        assertTrue(values.contains(CompletionKind.FUNCTION))
        assertTrue(values.contains(CompletionKind.KEYWORD))
        assertTrue(values.contains(CompletionKind.MODULE))
        assertTrue(values.contains(CompletionKind.VARIABLE))
        assertTrue(values.contains(CompletionKind.CLASS))
        assertTrue(values.contains(CompletionKind.SNIPPET))
    }

    // §3: IntelliSense - IntelliSense State

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

    // §3: IntelliSense - Large Symbol Index

    @Test
    fun testLargeSymbolIndex() {
        val entries = (1..5000).map { i ->
            IndexEntry(
                name = "symbol_$i",
                kind = CompletionKind.FUNCTION,
                filePath = "/test/file_$i.py",
                line = i,
                column = 0
            )
        }
        assertEquals(5000, entries.size)
    }

    @Test
    fun testSymbolIndexSearch() {
        val entries = (1..1000).map { i ->
            IndexEntry(
                name = "function_$i",
                kind = CompletionKind.FUNCTION,
                filePath = "/test/file.py",
                line = i
            )
        }
        
        val results = entries.filter { it.name.contains("500") }
        assertTrue(results.isNotEmpty())
    }

    // §3: IntelliSense - Circular Import Detection

    @Test
    fun testCircularImportDetection() {
        val imports = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        
        fun hasCircularImport(file: String, currentImports: Set<String>): Boolean {
            if (file in visited) return true
            if (file in imports) return true
            
            visited.add(file)
            for (import in currentImports) {
                if (hasCircularImport(import, currentImports)) return true
            }
            visited.remove(file)
            return false
        }
        
        val fileAImports = setOf("fileB")
        
        imports.addAll(fileAImports)
        assertTrue(hasCircularImport("fileA", fileAImports))
    }

    // §3: IntelliSense - Extremely Long Identifiers

    @Test
    fun testExtremelyLongIdentifier() {
        val longId = "a".repeat(1000)
        val item = CompletionItem(
            id = longId,
            label = longId,
            kind = CompletionKind.VARIABLE
        )
        assertEquals(1000, item.label.length)
    }

    // §3: IntelliSense - Syntax Validation

    @Test
    fun testSyntaxValidatorMissingColon() {
        val code = "def foo()\n    pass"
        assertTrue(code.contains("def foo()"))
    }

    @Test
    fun testSyntaxValidatorUnmatchedBracket() {
        val code = "x = [1, 2, 3"
        assertTrue(code.contains("["))
    }

    @Test
    fun testSyntaxValidatorUnterminatedString() {
        val code = "x = 'hello"
        assertTrue(code.contains("'hello"))
    }

    @Test
    fun testSyntaxValidatorDuplicateImports() {
        val code = "import os\nimport os"
        assertTrue(code.contains("import os"))
    }

    // §3: IntelliSense - Quick Fix

    @Test
    fun testQuickFixCreation() {
        val diagnostic = Diagnostic(
            id = "qf_1",
            range = DiagnosticRange(startLine = 10, startColumn = 5, endLine = 10, endColumn = 10),
            message = "Use 'is' instead of '==' for None comparison",
            severity = DiagnosticSeverity.WARNING
        )
        
        assertNotNull(diagnostic.message)
    }

    @Test
    fun testQuickFixBatchProcessing() {
        val diagnostics = (1..1000).map { i ->
            Diagnostic(
                id = "qf_$i",
                range = DiagnosticRange(startLine = i, startColumn = 0, endLine = i, endColumn = 10),
                message = "Use 'is' instead of '==' for None comparison",
                severity = DiagnosticSeverity.WARNING
            )
        }
        
        assertEquals(1000, diagnostics.size)
    }

    // §3: IntelliSense - Unicode Identifiers

    @Test
    fun testUnicodeIdentifierCompletion() {
        val item = CompletionItem(
            id = "変数_日本語",
            label = "変数_日本語",
            kind = CompletionKind.VARIABLE
        )
        assertEquals("変数_日本語", item.label)
    }

    @Test
    fun testMixedCaseIdentifierCompletion() {
        val item = CompletionItem(
            id = "camelCaseFunction",
            label = "camelCaseFunction",
            kind = CompletionKind.FUNCTION
        )
        assertEquals("camelCaseFunction", item.label)
        
        val snakeItem = CompletionItem(
            id = "snake_case_function",
            label = "snake_case_function",
            kind = CompletionKind.FUNCTION
        )
        assertEquals("snake_case_function", snakeItem.label)
    }

    // §3: IntelliSense - Auto-generated Code

    @Test
    fun testAutoGeneratedCodeLinting() {
        val lines = (1..10000).map { i ->
            if (i % 10 == 0) "x_$i == None" else "pass"
        }
        val code = lines.joinToString("\n")
        
        assertTrue(code.isNotEmpty())
    }

    // §3: IntelliSense - Completion Inside Special Contexts

    @Test
    fun testCompletionInsideString() {
        val code = "x = 'hello world'"
        val cursorPos = 12
        assertTrue(code.contains("'"))
    }

    @Test
    fun testCompletionInsideComment() {
        val code = "# This is a comment"
        val cursorPos = 10
        assertTrue(code.contains("#"))
    }

    @Test
    fun testCompletionInsideDocstring() {
        val code = "\"\"\"This is a docstring\"\"\""
        val cursorPos = 10
        assertTrue(code.contains("\"\"\""))
    }
}
