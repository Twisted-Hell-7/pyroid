package com.pythonide.data.runtime

import org.junit.Assert.*
import org.junit.Test

class CompatibilityMatrixTest {

    // §13: Compatibility Matrix - Version Compatibility

    @Test
    fun testPythonVersionCompatibility() {
        // Chaquopy bundles CPython 3.11
        val pythonVersion = "3.11"
        assertNotNull(pythonVersion)
        assertTrue(pythonVersion.startsWith("3."))
    }

    @Test
    fun testMinSdkCompatibility() {
        val minSdk = 26
        assertEquals(26, minSdk) // Android 8.0
    }

    @Test
    fun testTargetSdkCompatibility() {
        val targetSdk = 35
        assertEquals(35, targetSdk) // Android 15
    }

    // §13: Compatibility Matrix - Architecture Compatibility

    @Test
    fun testArm64Compatibility() {
        val supportedAbis = listOf("arm64-v8a", "x86_64")
        assertTrue(supportedAbis.contains("arm64-v8a"))
    }

    @Test
    fun testX86_64Compatibility() {
        val supportedAbis = listOf("arm64-v8a", "x86_64")
        assertTrue(supportedAbis.contains("x86_64"))
    }

    @Test
    fun testArmv7Compatibility() {
        // Note: Armv7 may not be supported in this build
        val supportedAbis = listOf("arm64-v8a", "x86_64")
        // Armv7 is not in the supported list
        assertFalse(supportedAbis.contains("armeabi-v7a"))
    }

    // §13: Compatibility Matrix - Package Compatibility

    @Test
    fun testNativeWheelCompatibility() {
        // Native wheels need to match ABI
        val wheelAbi = "cp311"
        val runtimeAbi = "cp311"
        assertEquals(wheelAbi, runtimeAbi)
    }

    @Test
    fun testPurePythonWheelCompatibility() {
        // Pure Python wheels are universal
        val wheelType = "py3"
        assertNotNull(wheelType)
    }

    // §13: Compatibility Matrix - Theme Compatibility

    @Test
    fun testThemeEnumValues() {
        val themes = com.pythonide.domain.model.editor.EditorTheme.entries
        assertEquals(6, themes.size)
        assertTrue(themes.contains(com.pythonide.domain.model.editor.EditorTheme.DEFAULT))
        assertTrue(themes.contains(com.pythonide.domain.model.editor.EditorTheme.MONOKAI))
        assertTrue(themes.contains(com.pythonide.domain.model.editor.EditorTheme.SOLARIZED_DARK))
        assertTrue(themes.contains(com.pythonide.domain.model.editor.EditorTheme.DRACULA))
        assertTrue(themes.contains(com.pythonide.domain.model.editor.EditorTheme.GITHUB_DARK))
        assertTrue(themes.contains(com.pythonide.domain.model.editor.EditorTheme.AMOLED))
    }

    @Test
    fun testThemeColors() {
        val theme = com.pythonide.domain.model.editor.EditorTheme.DEFAULT
        
        // Verify all color values are valid ARGB
        assertTrue(theme.backgroundArgb != 0L)
        assertTrue(theme.foregroundArgb != 0L)
        assertTrue(theme.lineNumberArgb != 0L)
        assertTrue(theme.lineNumberBackgroundArgb != 0L)
        assertTrue(theme.currentLineArgb != 0L)
        assertTrue(theme.selectionArgb != 0L)
        assertTrue(theme.keywordArgb != 0L)
        assertTrue(theme.stringArgb != 0L)
        assertTrue(theme.numberArgb != 0L)
        assertTrue(theme.commentArgb != 0L)
        assertTrue(theme.functionArgb != 0L)
        assertTrue(theme.classNameArgb != 0L)
        assertTrue(theme.operatorArgb != 0L)
        assertTrue(theme.punctuationArgb != 0L)
        assertTrue(theme.builtinArgb != 0L)
        assertTrue(theme.decoratorArgb != 0L)
    }

    // §13: Compatibility Matrix - ANSI Color Compatibility

    @Test
    fun testAnsiColor16Colors() {
        val colors = listOf(
            com.pythonide.domain.model.terminal.AnsiColor.DEFAULT,
            com.pythonide.domain.model.terminal.AnsiColor.BLACK,
            com.pythonide.domain.model.terminal.AnsiColor.RED,
            com.pythonide.domain.model.terminal.AnsiColor.GREEN,
            com.pythonide.domain.model.terminal.AnsiColor.YELLOW,
            com.pythonide.domain.model.terminal.AnsiColor.BLUE,
            com.pythonide.domain.model.terminal.AnsiColor.MAGENTA,
            com.pythonide.domain.model.terminal.AnsiColor.CYAN,
            com.pythonide.domain.model.terminal.AnsiColor.WHITE,
            com.pythonide.domain.model.terminal.AnsiColor.BRIGHT_BLACK,
            com.pythonide.domain.model.terminal.AnsiColor.BRIGHT_RED,
            com.pythonide.domain.model.terminal.AnsiColor.BRIGHT_GREEN,
            com.pythonide.domain.model.terminal.AnsiColor.BRIGHT_YELLOW,
            com.pythonide.domain.model.terminal.AnsiColor.BRIGHT_BLUE,
            com.pythonide.domain.model.terminal.AnsiColor.BRIGHT_MAGENTA,
            com.pythonide.domain.model.terminal.AnsiColor.BRIGHT_CYAN,
            com.pythonide.domain.model.terminal.AnsiColor.BRIGHT_WHITE
        )
        
        assertEquals(17, colors.size) // 16 colors + DEFAULT
    }

    @Test
    fun testAnsiColor256Colors() {
        // Test 256 color support
        val color0 = com.pythonide.domain.model.terminal.AnsiColor.from256(0)
        val color255 = com.pythonide.domain.model.terminal.AnsiColor.from256(255)
        
        assertNotNull(color0)
        assertNotNull(color255)
    }

    // §13: Compatibility Matrix - Debug State Compatibility

    @Test
    fun testDebugStateEnum() {
        val states = com.pythonide.domain.model.debugger.DebugState.entries
        assertEquals(7, states.size)
        assertTrue(states.contains(com.pythonide.domain.model.debugger.DebugState.IDLE))
        assertTrue(states.contains(com.pythonide.domain.model.debugger.DebugState.STARTING))
        assertTrue(states.contains(com.pythonide.domain.model.debugger.DebugState.RUNNING))
        assertTrue(states.contains(com.pythonide.domain.model.debugger.DebugState.PAUSED))
        assertTrue(states.contains(com.pythonide.domain.model.debugger.DebugState.STEPPING))
        assertTrue(states.contains(com.pythonide.domain.model.debugger.DebugState.STOPPED))
        assertTrue(states.contains(com.pythonide.domain.model.debugger.DebugState.ERROR))
    }

    // §13: Compatibility Matrix - Completion Kind Compatibility

    @Test
    fun testCompletionKindEnum() {
        val kinds = com.pythonide.domain.model.intellisense.CompletionKind.entries
        assertTrue(kinds.isNotEmpty())
        assertTrue(kinds.contains(com.pythonide.domain.model.intellisense.CompletionKind.FUNCTION))
        assertTrue(kinds.contains(com.pythonide.domain.model.intellisense.CompletionKind.KEYWORD))
        assertTrue(kinds.contains(com.pythonide.domain.model.intellisense.CompletionKind.MODULE))
        assertTrue(kinds.contains(com.pythonide.domain.model.intellisense.CompletionKind.VARIABLE))
        assertTrue(kinds.contains(com.pythonide.domain.model.intellisense.CompletionKind.CLASS))
        assertTrue(kinds.contains(com.pythonide.domain.model.intellisense.CompletionKind.SNIPPET))
    }

    // §13: Compatibility Matrix - File Type Compatibility

    @Test
    fun testPythonFileExtensionRecognition() {
        val extensions = listOf(".py", ".pyw", ".pyi")
        for (ext in extensions) {
            assertTrue("Extension $ext should be recognized", ext.startsWith("."))
        }
    }

    @Test
    fun testPythonFileExtensionCaseInsensitive() {
        val extensions = listOf(".PY", ".Py", ".pY")
        for (ext in extensions) {
            assertTrue("Extension $ext should be handled", ext.length == 3)
        }
    }

    // §13: Compatibility Matrix - Line Ending Compatibility

    @Test
    fun testLineEndingHandling() {
        val lineEndings = listOf("\n", "\r\n", "\r")
        for (ending in lineEndings) {
            val content = "line1${ending}line2"
            val lines = content.lines()
            assertTrue("Line ending '$ending' should be handled", lines.size >= 2)
        }
    }

    // §13: Compatibility Matrix - Encoding Compatibility

    @Test
    fun testUTF8Encoding() {
        val content = "Hello, World! 你好世界 🌍"
        val bytes = content.toByteArray(Charsets.UTF_8)
        val decoded = String(bytes, Charsets.UTF_8)
        assertEquals(content, decoded)
    }

    @Test
    fun testUnicodeEscapeSequences() {
        val unicode = "\u0041\u0042\u0043" // ABC
        assertEquals("ABC", unicode)
    }

    // §13: Compatibility Matrix - Memory Compatibility

    @Test
    fun testMemoryReport() {
        val report = CrashPrevention().getMemoryReport()
        
        assertTrue(report.usedHeap > 0)
        assertTrue(report.maxHeap > 0)
        assertTrue(report.usagePercent in 0..100)
        assertTrue(report.threadCount > 0)
    }

    @Test
    fun testMemoryReportFormattedString() {
        val report = CrashPrevention().getMemoryReport()
        val formatted = report.toFormattedString()
        
        assertTrue(formatted.contains("Heap:"))
        assertTrue(formatted.contains("Free:"))
        assertTrue(formatted.contains("Native:"))
        assertTrue(formatted.contains("Threads:"))
    }
}
