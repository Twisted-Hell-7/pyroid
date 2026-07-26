package com.pythonide.data.terminal

import com.pythonide.domain.model.terminal.AnsiColor
import com.pythonide.domain.model.terminal.AnsiSegment
import org.junit.Assert.*
import org.junit.Test

class TerminalEdgeCaseTest {

    // §5: Terminal - ANSI Parsing Edge Cases

    @Test
    fun testParseEmptyString() {
        val segments = AnsiParser.parse("")
        assertTrue(segments.isEmpty())
    }

    @Test
    fun testParsePlainText() {
        val segments = AnsiParser.parse("Hello, World!")
        assertEquals(1, segments.size)
        assertEquals("Hello, World!", segments[0].text)
        assertEquals(AnsiColor.DEFAULT, segments[0].foreground)
    }

    @Test
    fun testParseBasicColorCodes() {
        val text = "\u001b[31mRed Text\u001b[0m"
        val segments = AnsiParser.parse(text)
        assertTrue(segments.isNotEmpty())
        assertEquals("Red Text", segments[0].text)
    }

    @Test
    fun testParse256ColorCodes() {
        val text = "\u001b[38;5;196mRed 256\u001b[0m"
        val segments = AnsiParser.parse(text)
        assertTrue(segments.isNotEmpty())
        assertEquals("Red 256", segments[0].text)
    }

    @Test
    fun testParseRgbColorCodes() {
        val text = "\u001b[38;2;255;128;0mOrange RGB\u001b[0m"
        val segments = AnsiParser.parse(text)
        assertTrue(segments.isNotEmpty())
        assertEquals("Orange RGB", segments[0].text)
    }

    @Test
    fun testParseBackgroundColors() {
        val text = "\u001b[41mRed Background\u001b[0m"
        val segments = AnsiParser.parse(text)
        assertTrue(segments.isNotEmpty())
        assertEquals("Red Background", segments[0].text)
    }

    @Test
    fun testParseBoldText() {
        val text = "\u001b[1mBold Text\u001b[0m"
        val segments = AnsiParser.parse(text)
        assertTrue(segments.isNotEmpty())
        assertTrue(segments[0].bold)
    }

    @Test
    fun testParseItalicText() {
        val text = "\u001b[3mItalic Text\u001b[0m"
        val segments = AnsiParser.parse(text)
        assertTrue(segments.isNotEmpty())
        assertTrue(segments[0].italic)
    }

    @Test
    fun testParseUnderlineText() {
        val text = "\u001b[4mUnderline Text\u001b[0m"
        val segments = AnsiParser.parse(text)
        assertTrue(segments.isNotEmpty())
        assertTrue(segments[0].underline)
    }

    @Test
    fun testParseStrikethroughText() {
        val text = "\u001b[9mStrikethrough Text\u001b[0m"
        val segments = AnsiParser.parse(text)
        assertTrue(segments.isNotEmpty())
        assertTrue(segments[0].strikethrough)
    }

    @Test
    fun testParseDimText() {
        val text = "\u001b[2mDim Text\u001b[0m"
        val segments = AnsiParser.parse(text)
        assertTrue(segments.isNotEmpty())
        assertTrue(segments[0].dim)
    }

    @Test
    fun testParseResetAttributes() {
        val text = "\u001b[1m\u001b[3mBold and Italic\u001b[22m\u001b[23mReset Attributes"
        val segments = AnsiParser.parse(text)
        assertTrue(segments.isNotEmpty())
        val lastSegment = segments.last()
        assertFalse(lastSegment.bold)
        assertFalse(lastSegment.italic)
    }

    @Test
    fun testParseMixedAttributes() {
        val text = "\u001b[1;3;4mBold Italic Underline\u001b[0m"
        val segments = AnsiParser.parse(text)
        assertTrue(segments.isNotEmpty())
        assertTrue(segments[0].bold)
        assertTrue(segments[0].italic)
        assertTrue(segments[0].underline)
    }

    @Test
    fun testParseHighIntensityColors() {
        val text = "\u001b[90mBright Black\u001b[0m"
        val segments = AnsiParser.parse(text)
        assertTrue(segments.isNotEmpty())
    }

    @Test
    fun testParseHighIntensityBackground() {
        val text = "\u001b[100mBright Black Background\u001b[0m"
        val segments = AnsiParser.parse(text)
        assertTrue(segments.isNotEmpty())
    }

    // §5: Terminal - Malformed ANSI Sequences

    @Test
    fun testParseIncompleteEscapeSequence() {
        val text = "\u001b[31"
        val segments = AnsiParser.parse(text)
        // Should handle incomplete sequences gracefully
        assertTrue(segments.isNotEmpty())
    }

    @Test
    fun testParseInvalidColorCode() {
        val text = "\u001b[99mInvalid Color\u001b[0m"
        val segments = AnsiParser.parse(text)
        assertTrue(segments.isNotEmpty())
    }

    @Test
    fun testParseEmptyEscapeSequence() {
        val text = "\u001b[mEmpty"
        val segments = AnsiParser.parse(text)
        assertTrue(segments.isNotEmpty())
        assertEquals("Empty", segments[0].text)
    }

    @Test
    fun testParseConsecutiveEscapeSequences() {
        val text = "\u001b[31m\u001b[1m\u001b[4mRed Bold Underline\u001b[0m"
        val segments = AnsiParser.parse(text)
        assertTrue(segments.isNotEmpty())
        assertTrue(segments[0].bold)
        assertTrue(segments[0].underline)
    }

    @Test
    fun testParseEscapeSequenceInMiddle() {
        val text = "Before\u001b[31mMiddle\u001b[0mAfter"
        val segments = AnsiParser.parse(text)
        assertTrue(segments.size >= 2)
    }

    // §5: Terminal - Strip ANSI

    @Test
    fun testStripAnsiPlainText() {
        val text = "Hello, World!"
        val stripped = AnsiParser.stripAnsi(text)
        assertEquals("Hello, World!", stripped)
    }

    @Test
    fun testStripAnsiColoredText() {
        val text = "\u001b[31mRed Text\u001b[0m"
        val stripped = AnsiParser.stripAnsi(text)
        assertEquals("Red Text", stripped)
    }

    @Test
    fun testStripAnsiMultipleSequences() {
        val text = "\u001b[31mRed\u001b[0m Normal \u001b[34mBlue\u001b[0m"
        val stripped = AnsiParser.stripAnsi(text)
        assertEquals("Red Normal Blue", stripped)
    }

    @Test
    fun testHasAnsiPlainText() {
        val text = "Hello, World!"
        assertFalse(AnsiParser.hasAnsi(text))
    }

    @Test
    fun testHasAnsiColoredText() {
        val text = "\u001b[31mRed Text\u001b[0m"
        assertTrue(AnsiParser.hasAnsi(text))
    }

    // §5: Terminal - Color Mapping

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
    fun testAnsiColorFrom256() {
        // Test various 256 color codes
        val color0 = AnsiColor.from256(0)
        val color1 = AnsiColor.from256(1)
        val color255 = AnsiColor.from256(255)
        
        assertNotNull(color0)
        assertNotNull(color1)
        assertNotNull(color255)
    }

    // §5: Terminal - Segment Properties

    @Test
    fun testAnsiSegmentDefaults() {
        val segment = AnsiSegment(text = "test")
        assertEquals("test", segment.text)
        assertEquals(AnsiColor.DEFAULT, segment.foreground)
        assertEquals(AnsiColor.DEFAULT, segment.background)
        assertFalse(segment.bold)
        assertFalse(segment.italic)
        assertFalse(segment.underline)
        assertFalse(segment.dim)
        assertFalse(segment.strikethrough)
    }

    @Test
    fun testAnsiSegmentWithAllProperties() {
        val segment = AnsiSegment(
            text = "test",
            foreground = AnsiColor.RED,
            background = AnsiColor.BLUE,
            bold = true,
            italic = true,
            underline = true,
            dim = true,
            strikethrough = true
        )
        assertEquals("test", segment.text)
        assertEquals(AnsiColor.RED, segment.foreground)
        assertEquals(AnsiColor.BLUE, segment.background)
        assertTrue(segment.bold)
        assertTrue(segment.italic)
        assertTrue(segment.underline)
        assertTrue(segment.dim)
        assertTrue(segment.strikethrough)
    }

    // §5: Terminal - Long Output

    @Test
    fun testParseExtremelyLongLine() {
        val longLine = "x".repeat(500_000)
        val segments = AnsiParser.parse(longLine)
        assertTrue(segments.isNotEmpty())
        assertEquals(longLine, segments[0].text)
    }

    @Test
    fun testParseManyEscapeSequences() {
        val builder = StringBuilder()
        repeat(1000) { i ->
            builder.append("\u001b[${31 + (i % 7)}mLine $i\u001b[0m\n")
        }
        val segments = AnsiParser.parse(builder.toString())
        assertTrue(segments.isNotEmpty())
    }

    // §5: Terminal - ANSI State Leak

    @Test
    fun testNoAnsiStateLeakBetweenLines() {
        val text = "\u001b[31mRed\u001b[0m\nNormal"
        val segments = AnsiParser.parse(text)
        
        // Find the "Normal" segment
        val normalSegment = segments.find { it.text.contains("Normal") }
        assertNotNull(normalSegment)
        assertEquals(AnsiColor.DEFAULT, normalSegment!!.foreground)
    }

    @Test
    fun testResetBetweenColorChanges() {
        val text = "\u001b[31mRed\u001b[0m\u001b[32mGreen\u001b[0m"
        val segments = AnsiParser.parse(text)
        assertEquals(2, segments.size)
    }
}
