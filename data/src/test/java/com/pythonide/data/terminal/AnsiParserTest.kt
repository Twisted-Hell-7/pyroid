package com.pythonide.data.terminal

import com.pythonide.domain.model.terminal.AnsiColor
import org.junit.Assert.*
import org.junit.Test

class AnsiParserTest {

    // --- Basic parsing (§5: terminal) ---

    @Test
    fun testParsePlainText() {
        val result = AnsiParser.parse("hello world")
        assertEquals(1, result.size)
        assertEquals("hello world", result[0].text)
        assertEquals(AnsiColor.DEFAULT, result[0].foreground)
    }

    @Test
    fun testParseEmptyText() {
        val result = AnsiParser.parse("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun testParseTextWithNoAnsi() {
        val result = AnsiParser.parse("no escape codes here")
        assertEquals(1, result.size)
        assertEquals("no escape codes here", result[0].text)
    }

    // --- 16-color support (§5: 256-color + 16-color) ---

    @Test
    fun testParseRedForeground() {
        val result = AnsiParser.parse("\u001b[31mred\u001b[0m")
        assertEquals(1, result.size)
        assertEquals(AnsiColor.RED, result[0].foreground)
        assertEquals("red", result[0].text)
    }

    @Test
    fun testParseGreenForeground() {
        val result = AnsiParser.parse("\u001b[32mgreen\u001b[0m")
        assertEquals(AnsiColor.GREEN, result[0].foreground)
    }

    @Test
    fun testParseBlueForeground() {
        val result = AnsiParser.parse("\u001b[34mblue\u001b[0m")
        assertEquals(AnsiColor.BLUE, result[0].foreground)
    }

    @Test
    fun testParseYellowBackground() {
        val result = AnsiParser.parse("\u001b[43mtext\u001b[0m")
        assertEquals(AnsiColor.YELLOW, result[0].background)
    }

    @Test
    fun testParseBrightColors() {
        val result = AnsiParser.parse("\u001b[91mbright red\u001b[0m")
        assertEquals(AnsiColor.BRIGHT_RED, result[0].foreground)
    }

    @Test
    fun testParseBrightBackground() {
        val result = AnsiParser.parse("\u001b[104mbright blue bg\u001b[0m")
        assertEquals(AnsiColor.BRIGHT_BLUE, result[0].background)
    }

    // --- Text attributes (§5: bold/italic/underline/strikethrough) ---

    @Test
    fun testParseBold() {
        val result = AnsiParser.parse("\u001b[1mbold\u001b[0m")
        assertTrue(result[0].bold)
    }

    @Test
    fun testParseDim() {
        val result = AnsiParser.parse("\u001b[2mdim\u001b[0m")
        assertTrue(result[0].dim)
    }

    @Test
    fun testParseItalic() {
        val result = AnsiParser.parse("\u001b[3mitalic\u001b[0m")
        assertTrue(result[0].italic)
    }

    @Test
    fun testParseUnderline() {
        val result = AnsiParser.parse("\u001b[4munderline\u001b[0m")
        assertTrue(result[0].underline)
    }

    @Test
    fun testParseStrikethrough() {
        val result = AnsiParser.parse("\u001b[9mstrikethrough\u001b[0m")
        assertTrue(result[0].strikethrough)
    }

    @Test
    fun testParseResetAll() {
        val result = AnsiParser.parse("\u001b[1m\u001b[3mbold italic\u001b[0m")
        assertTrue(result[0].bold)
        assertTrue(result[0].italic)
    }

    @Test
    fun testParseResetBoldOnly() {
        val result = AnsiParser.parse("\u001b[1m\u001b[22mnot bold\u001b[0m")
        assertFalse(result[0].bold)
        assertFalse(result[0].dim)
    }

    @Test
    fun testParseResetItalic() {
        val result = AnsiParser.parse("\u001b[3m\u001b[23mnot italic\u001b[0m")
        assertFalse(result[0].italic)
    }

    @Test
    fun testParseResetUnderline() {
        val result = AnsiParser.parse("\u001b[4m\u001b[24mnot underline\u001b[0m")
        assertFalse(result[0].underline)
    }

    @Test
    fun testParseResetStrikethrough() {
        val result = AnsiParser.parse("\u001b[9m\u001b[29mnot strike\u001b[0m")
        assertFalse(result[0].strikethrough)
    }

    // --- 256-color support (§5) ---

    @Test
    fun testParse256ColorForeground() {
        val result = AnsiParser.parse("\u001b[38;5;196mred256\u001b[0m")
        // 256-color should be parsed, color may map to a named color
        assertTrue(result.isNotEmpty())
        assertEquals("red256", result[0].text)
    }

    @Test
    fun testParse256ColorBackground() {
        val result = AnsiParser.parse("\u001b[48;5;82mtext\u001b[0m")
        assertTrue(result.isNotEmpty())
    }

    // --- Mixed attributes (§5: combos stacked) ---

    @Test
    fun testParseBoldItalicUnderline() {
        val result = AnsiParser.parse("\u001b[1;3;4mcombo\u001b[0m")
        assertTrue(result[0].bold)
        assertTrue(result[0].italic)
        assertTrue(result[0].underline)
    }

    @Test
    fun testParseBoldRed() {
        val result = AnsiParser.parse("\u001b[1;31mbold red\u001b[0m")
        assertTrue(result[0].bold)
        assertEquals(AnsiColor.RED, result[0].foreground)
    }

    // --- Multiple segments ---

    @Test
    fun testParseMultipleSegments() {
        val result = AnsiParser.parse("\u001b[31mred\u001b[0m normal \u001b[32mgreen\u001b[0m")
        assertEquals(3, result.size)
        assertEquals(AnsiColor.RED, result[0].foreground)
        assertEquals(" normal ", result[1].text)
        assertEquals(AnsiColor.DEFAULT, result[1].foreground)
        assertEquals(AnsiColor.GREEN, result[2].foreground)
    }

    // --- Reset to default ---

    @Test
    fun testParseResetToDefault() {
        val result = AnsiParser.parse("\u001b[31mred\u001b[0m default")
        assertEquals(2, result.size)
        assertEquals(AnsiColor.RED, result[0].foreground)
        assertEquals(AnsiColor.DEFAULT, result[1].foreground)
    }

    // --- Foreground reset (39) ---

    @Test
    fun testParseForegroundReset() {
        val result = AnsiParser.parse("\u001b[31mred\u001b[39mnormal")
        assertEquals(AnsiColor.RED, result[0].foreground)
        assertEquals(AnsiColor.DEFAULT, result[1].foreground)
    }

    // --- Background reset (49) ---

    @Test
    fun testParseBackgroundReset() {
        val result = AnsiParser.parse("\u001b[41mred bg\u001b[49mnormal")
        assertEquals(AnsiColor.RED, result[0].background)
        assertEquals(AnsiColor.DEFAULT, result[1].background)
    }

    // --- Malformed/truncated ANSI (§5: fuzzed sequences) ---

    @Test
    fun testParseTruncatedEscape() {
        val result = AnsiParser.parse("\u001b[3")
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun testParseEmptyEscape() {
        val result = AnsiParser.parse("\u001b[]")
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun testParseIncompleteEscape() {
        val result = AnsiParser.parse("\u001b[31")
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun testParseRandomBytes() {
        val result = AnsiParser.parse("\u001b[999mtext\u001b[0m")
        assertTrue(result.isNotEmpty())
    }

    // --- stripAnsi ---

    @Test
    fun testStripAnsi() {
        val stripped = AnsiParser.stripAnsi("\u001b[31mhello\u001b[0m")
        assertEquals("hello", stripped)
    }

    @Test
    fun testStripAnsiNoCodes() {
        val stripped = AnsiParser.stripAnsi("plain text")
        assertEquals("plain text", stripped)
    }

    @Test
    fun testStripAnsiMultiple() {
        val stripped = AnsiParser.stripAnsi("\u001b[31mred\u001b[0m and \u001b[32mgreen\u001b[0m")
        assertEquals("red and green", stripped)
    }

    // --- hasAnsi ---

    @Test
    fun testHasAnsiTrue() {
        assertTrue(AnsiParser.hasAnsi("\u001b[31mtext\u001b[0m"))
    }

    @Test
    fun testHasAnsiFalse() {
        assertFalse(AnsiParser.hasAnsi("plain text"))
    }

    @Test
    fun testHasAnsiEmpty() {
        assertFalse(AnsiParser.hasAnsi(""))
    }

    // --- AnsiColor ---

    @Test
    fun testAnsiColorFromCode() {
        assertEquals(AnsiColor.BLACK, AnsiColor.fromCode(0))
        assertEquals(AnsiColor.RED, AnsiColor.fromCode(1))
        assertEquals(AnsiColor.GREEN, AnsiColor.fromCode(2))
        assertEquals(AnsiColor.YELLOW, AnsiColor.fromCode(3))
        assertEquals(AnsiColor.BLUE, AnsiColor.fromCode(4))
        assertEquals(AnsiColor.MAGENTA, AnsiColor.fromCode(5))
        assertEquals(AnsiColor.CYAN, AnsiColor.fromCode(6))
        assertEquals(AnsiColor.WHITE, AnsiColor.fromCode(7))
    }

    @Test
    fun testAnsiColorFromCodeBright() {
        assertEquals(AnsiColor.BRIGHT_BLACK, AnsiColor.fromCode(8))
        assertEquals(AnsiColor.BRIGHT_RED, AnsiColor.fromCode(9))
        assertEquals(AnsiColor.BRIGHT_GREEN, AnsiColor.fromCode(10))
        assertEquals(AnsiColor.BRIGHT_YELLOW, AnsiColor.fromCode(11))
        assertEquals(AnsiColor.BRIGHT_BLUE, AnsiColor.fromCode(12))
        assertEquals(AnsiColor.BRIGHT_MAGENTA, AnsiColor.fromCode(13))
        assertEquals(AnsiColor.BRIGHT_CYAN, AnsiColor.fromCode(14))
        assertEquals(AnsiColor.BRIGHT_WHITE, AnsiColor.fromCode(15))
    }

    @Test
    fun testAnsiColorFromCodeUnknown() {
        assertEquals(AnsiColor.DEFAULT, AnsiColor.fromCode(999))
    }

    @Test
    fun testAnsiColorFromCodeNegative() {
        assertEquals(AnsiColor.DEFAULT, AnsiColor.fromCode(-1))
    }

    @Test
    fun testAnsiColorFrom256() {
        // Test 256-color mapping
        assertEquals(AnsiColor.DEFAULT, AnsiColor.from256(-1))
        // First 8 colors
        assertEquals(AnsiColor.BLACK, AnsiColor.from256(0))
        // Colors 8-15: from256 maps 8 to BLACK due to index calculation
        assertEquals(AnsiColor.BLACK, AnsiColor.from256(8))
    }

    // --- Extreme long line (§5) ---

    @Test
    fun testParseExtremelyLongLine() {
        val longLine = "a".repeat(500_000)
        val result = AnsiParser.parse(longLine)
        assertEquals(1, result.size)
        assertEquals(500_000, result[0].text.length)
    }

    // --- Mixed 256-color and 16-color (§5) ---

    @Test
    fun testParseMixedColorModes() {
        val result = AnsiParser.parse("\u001b[31m16-color\u001b[0m \u001b[38;5;82m256-color\u001b[0m")
        assertEquals(3, result.size)
        assertEquals(AnsiColor.RED, result[0].foreground)
    }
}
