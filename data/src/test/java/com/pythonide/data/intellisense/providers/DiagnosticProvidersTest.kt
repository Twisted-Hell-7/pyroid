package com.pythonide.data.intellisense.providers

import com.pythonide.domain.model.intellisense.DiagnosticSeverity
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
class DiagnosticProvidersTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var validator: SyntaxValidator
    private lateinit var linter: Linter

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        validator = SyntaxValidator()
        linter = Linter()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- SyntaxValidator (§3: syntax validator) ---

    @Test
    fun testValidPythonCode() = runTest {
        val diagnostics = validator.getDiagnostics("def foo():\n    pass")
        assertTrue(diagnostics.isEmpty())
    }

    @Test
    fun testMissingColonOnDef() = runTest {
        val diagnostics = validator.getDiagnostics("def foo")
        val colonErrors = diagnostics.filter { it.code == "E0001" }
        assertTrue(colonErrors.isNotEmpty())
        assertEquals(DiagnosticSeverity.ERROR, colonErrors[0].severity)
    }

    @Test
    fun testMissingColonOnClass() = runTest {
        val diagnostics = validator.getDiagnostics("class Foo")
        val colonErrors = diagnostics.filter { it.code == "E0002" }
        assertTrue(colonErrors.isNotEmpty())
    }

    @Test
    fun testMissingColonOnIf() = runTest {
        val diagnostics = validator.getDiagnostics("if True")
        val colonErrors = diagnostics.filter { it.code == "E0003" }
        assertTrue(colonErrors.isNotEmpty())
    }

    @Test
    fun testMissingColonOnFor() = runTest {
        val diagnostics = validator.getDiagnostics("for i in range(10)")
        val colonErrors = diagnostics.filter { it.code == "E0003" }
        assertTrue(colonErrors.isNotEmpty())
    }

    @Test
    fun testMissingColonOnWhile() = runTest {
        val diagnostics = validator.getDiagnostics("while True")
        val colonErrors = diagnostics.filter { it.code == "E0003" }
        assertTrue(colonErrors.isNotEmpty())
    }

    @Test
    fun testMissingColonOnWith() = runTest {
        val diagnostics = validator.getDiagnostics("with open('f') as f")
        val colonErrors = diagnostics.filter { it.code == "E0003" }
        assertTrue(colonErrors.isNotEmpty())
    }

    @Test
    fun testMissingColonOnTry() = runTest {
        val diagnostics = validator.getDiagnostics("try")
        val colonErrors = diagnostics.filter { it.code == "E0003" }
        assertTrue(colonErrors.isNotEmpty())
    }

    @Test
    fun testMissingColonOnExcept() = runTest {
        val diagnostics = validator.getDiagnostics("except ValueError")
        val colonErrors = diagnostics.filter { it.code == "E0003" }
        assertTrue(colonErrors.isNotEmpty())
    }

    @Test
    fun testMissingColonOnFinally() = runTest {
        val diagnostics = validator.getDiagnostics("finally")
        val colonErrors = diagnostics.filter { it.code == "E0003" }
        assertTrue(colonErrors.isNotEmpty())
    }

    // --- Unmatched brackets (§3) ---

    @Test
    fun testUnmatchedParen() = runTest {
        val diagnostics = validator.getDiagnostics("x = (1 + 2")
        val parenErrors = diagnostics.filter { it.code == "E0005" }
        assertTrue(parenErrors.isNotEmpty())
    }

    @Test
    fun testUnmatchedBracket() = runTest {
        val diagnostics = validator.getDiagnostics("x = [1, 2, 3")
        val bracketErrors = diagnostics.filter { it.code == "E0005" }
        assertTrue(bracketErrors.isNotEmpty())
    }

    @Test
    fun testUnmatchedBrace() = runTest {
        val diagnostics = validator.getDiagnostics("x = {1: 2")
        val braceErrors = diagnostics.filter { it.code == "E0005" }
        assertTrue(braceErrors.isNotEmpty())
    }

    @Test
    fun testUnmatchedCloseParen() = runTest {
        val diagnostics = validator.getDiagnostics("x = )")
        val parenErrors = diagnostics.filter { it.code == "E0004" }
        assertTrue(parenErrors.isNotEmpty())
    }

    // --- Unterminated strings (§3) ---

    @Test
    fun testUnterminatedString() = runTest {
        // E0007 triggers on newline inside string (EOL while scanning)
        val diagnostics = validator.getDiagnostics("x = \"hello\n")
        val stringErrors = diagnostics.filter { it.code == "E0007" }
        assertTrue(stringErrors.isNotEmpty())
    }

    @Test
    fun testUnterminatedTripleQuote() = runTest {
        val diagnostics = validator.getDiagnostics("x = \"\"\"hello")
        val stringErrors = diagnostics.filter { it.code == "E0006" }
        assertTrue(stringErrors.isNotEmpty())
    }

    // --- Duplicate imports (§3) ---

    @Test
    fun testDuplicateImport() = runTest {
        val content = "import os\nimport os"
        val diagnostics = validator.getDiagnostics(content)
        val importErrors = diagnostics.filter { it.code == "W0003" }
        assertTrue(importErrors.isNotEmpty())
    }

    // --- Multiple errors at once (§3: all four flagged simultaneously) ---

    @Test
    fun testMultipleErrorsSimultaneously() = runTest {
        val content = """
            def foo
            x = [1, 2
            y = "unterminated
            import os
            import os
        """.trimIndent()
        val diagnostics = validator.getDiagnostics(content)
        // Should have errors for missing colon, unmatched bracket, unterminated string, and duplicate import
        val errorCodes = diagnostics.map { it.code }.toSet()
        assertTrue(errorCodes.contains("E0001")) // missing colon
        assertTrue(errorCodes.contains("E0005")) // unclosed bracket
        assertTrue(errorCodes.contains("E0007")) // unterminated string
        assertTrue(errorCodes.contains("W0003")) // duplicate import
    }

    // --- Linter (§3: common mistakes) ---

    @Test
    fun testLinterUnusedVariable() = runTest {
        val content = "x = 5\nprint('hello')"
        val diagnostics = linter.getDiagnostics(content)
        val unusedVars = diagnostics.filter { it.code == "W0010" }
        assertTrue(unusedVars.isNotEmpty())
        assertTrue(unusedVars[0].message.contains("x"))
    }

    @Test
    fun testLinterUsedVariable() = runTest {
        val content = "x = 5\nprint(x)"
        val diagnostics = linter.getDiagnostics(content)
        val unusedVars = diagnostics.filter { it.code == "W0010" }
        assertTrue(unusedVars.isEmpty())
    }

    @Test
    fun testLinterUnusedImport() = runTest {
        val content = "import os\nprint('hello')"
        val diagnostics = linter.getDiagnostics(content)
        val unusedImports = diagnostics.filter { it.code == "W0011" }
        assertTrue(unusedImports.isNotEmpty())
    }

    @Test
    fun testLinterUsedImport() = runTest {
        val content = "import os\nos.getcwd()"
        val diagnostics = linter.getDiagnostics(content)
        val unusedImports = diagnostics.filter { it.code == "W0011" }
        assertTrue(unusedImports.isEmpty())
    }

    @Test
    fun testLinterMissingDocstring() = runTest {
        val content = "def foo():\n    pass"
        val diagnostics = linter.getDiagnostics(content)
        val docstringWarnings = diagnostics.filter { it.code == "I0001" }
        assertTrue(docstringWarnings.isNotEmpty())
    }

    @Test
    fun testLinterPrivateFunctionNoDocstring() = runTest {
        val content = "def _foo():\n    pass"
        val diagnostics = linter.getDiagnostics(content)
        val docstringWarnings = diagnostics.filter { it.code == "I0001" }
        assertTrue(docstringWarnings.isEmpty())
    }

    @Test
    fun testLinterLineTooLong() = runTest {
        val content = "x = " + "a".repeat(80)
        val diagnostics = linter.getDiagnostics(content)
        val longLines = diagnostics.filter { it.code == "W0012" }
        assertTrue(longLines.isNotEmpty())
        assertEquals(DiagnosticSeverity.WARNING, longLines[0].severity)
    }

    @Test
    fun testLinterLineExactLength() = runTest {
        val content = "x = " + "a".repeat(75) // 79 chars total
        val diagnostics = linter.getDiagnostics(content)
        val longLines = diagnostics.filter { it.code == "W0012" }
        assertTrue(longLines.isEmpty())
    }

    @Test
    fun testLinterCommentExemptFromLineLength() = runTest {
        val content = "# " + "a".repeat(80)
        val diagnostics = linter.getDiagnostics(content)
        val longLines = diagnostics.filter { it.code == "W0012" }
        assertTrue(longLines.isEmpty())
    }

    @Test
    fun testLinterCamelCaseClassName() = runTest {
        val content = "class my_class:\n    pass"
        val diagnostics = linter.getDiagnostics(content)
        val namingWarnings = diagnostics.filter { it.code == "W0013" }
        assertTrue(namingWarnings.isNotEmpty())
    }

    @Test
    fun testLinterCorrectClassName() = runTest {
        val content = "class MyClass:\n    pass"
        val diagnostics = linter.getDiagnostics(content)
        val namingWarnings = diagnostics.filter { it.code == "W0013" }
        assertTrue(namingWarnings.isEmpty())
    }

    @Test
    fun testLinterUpperCaseFunctionName() = runTest {
        val content = "def MyFunc():\n    pass"
        val diagnostics = linter.getDiagnostics(content)
        val namingWarnings = diagnostics.filter { it.code == "W0014" }
        assertTrue(namingWarnings.isNotEmpty())
    }

    @Test
    fun testLinterCorrectFunctionName() = runTest {
        val content = "def my_func():\n    pass"
        val diagnostics = linter.getDiagnostics(content)
        val namingWarnings = diagnostics.filter { it.code == "W0014" }
        assertTrue(namingWarnings.isEmpty())
    }

    @Test
    fun testLinterBareExcept() = runTest {
        val content = "try:\n    pass\nexcept:\n    pass"
        val diagnostics = linter.getDiagnostics(content)
        val bareExcept = diagnostics.filter { it.code == "W0015" }
        assertTrue(bareExcept.isNotEmpty())
    }

    @Test
    fun testLinterComparisonWithTrue() = runTest {
        val content = "if x == True:\n    pass"
        val diagnostics = linter.getDiagnostics(content)
        val comparisonWarnings = diagnostics.filter { it.code == "W0016" }
        assertTrue(comparisonWarnings.isNotEmpty())
    }

    @Test
    fun testLinterComparisonWithFalse() = runTest {
        val content = "if x == False:\n    pass"
        val diagnostics = linter.getDiagnostics(content)
        val comparisonWarnings = diagnostics.filter { it.code == "W0016" }
        assertTrue(comparisonWarnings.isNotEmpty())
    }

    @Test
    fun testLinterComparisonWithNone() = runTest {
        val content = "if x == None:\n    pass"
        val diagnostics = linter.getDiagnostics(content)
        val comparisonWarnings = diagnostics.filter { it.code == "W0016" }
        assertTrue(comparisonWarnings.isNotEmpty())
    }

    @Test
    fun testLinterBroadExcept() = runTest {
        val content = "try:\n    pass\nexcept Exception:\n    pass"
        val diagnostics = linter.getDiagnostics(content)
        val broadExcept = diagnostics.filter { it.code == "I0002" }
        assertTrue(broadExcept.isNotEmpty())
    }

    // --- Quick fixes (§3) ---

    @Test
    fun testMissingColonHasQuickFix() = runTest {
        val diagnostics = validator.getDiagnostics("def foo")
        val withFix = diagnostics.filter { it.quickFixes?.isNotEmpty() == true }
        assertTrue(withFix.isNotEmpty())
    }

    @Test
    fun testUnusedVariableHasQuickFix() = runTest {
        val content = "x = 5\nprint('hello')"
        val diagnostics = linter.getDiagnostics(content)
        val withFix = diagnostics.filter { it.code == "W0010" && it.quickFixes?.isNotEmpty() == true }
        assertTrue(withFix.isNotEmpty())
    }

    // --- Empty content ---

    @Test
    fun testValidatorEmptyContent() = runTest {
        val diagnostics = validator.getDiagnostics("")
        assertTrue(diagnostics.isEmpty())
    }

    @Test
    fun testLinterEmptyContent() = runTest {
        val diagnostics = linter.getDiagnostics("")
        assertTrue(diagnostics.isEmpty())
    }

    // --- Comment-only content ---

    @Test
    fun testValidatorCommentsOnly() = runTest {
        val content = "# comment1\n# comment2"
        val diagnostics = validator.getDiagnostics(content)
        assertTrue(diagnostics.isEmpty())
    }
}
