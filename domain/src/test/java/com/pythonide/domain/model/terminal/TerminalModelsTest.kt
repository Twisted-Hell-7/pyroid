package com.pythonide.domain.model.terminal

import org.junit.Assert.*
import org.junit.Test

class TerminalModelsTest {

    // --- Terminal ---

    @Test
    fun testTerminalDefaults() {
        val terminal = Terminal()
        assertTrue(terminal.id.isNotEmpty())
        assertTrue(terminal.name.startsWith("Terminal "))
        assertTrue(terminal.isActive)
        assertTrue(terminal.createdAt > 0)
    }

    @Test
    fun testTerminalCustomName() {
        val terminal = Terminal(name = "My Terminal")
        assertEquals("My Terminal", terminal.name)
    }

    // --- TerminalEntry ---

    @Test
    fun testTerminalEntryDefaults() {
        val entry = TerminalEntry(
            terminalId = "term1",
            type = TerminalEntryType.OUTPUT,
            content = "hello"
        )
        assertTrue(entry.id.isNotEmpty())
        assertEquals("term1", entry.terminalId)
        assertEquals(TerminalEntryType.OUTPUT, entry.type)
        assertEquals("hello", entry.content)
        assertTrue(entry.timestamp > 0)
        assertTrue(entry.ansiFormatted.isEmpty())
    }

    // --- TerminalEntryType ---

    @Test
    fun testTerminalEntryTypeValues() {
        assertEquals(5, TerminalEntryType.entries.size)
        assertTrue(TerminalEntryType.entries.contains(TerminalEntryType.INPUT))
        assertTrue(TerminalEntryType.entries.contains(TerminalEntryType.OUTPUT))
        assertTrue(TerminalEntryType.entries.contains(TerminalEntryType.ERROR))
        assertTrue(TerminalEntryType.entries.contains(TerminalEntryType.SYSTEM))
        assertTrue(TerminalEntryType.entries.contains(TerminalEntryType.TIMESTAMP))
    }

    // --- AnsiSegment ---

    @Test
    fun testAnsiSegmentDefaults() {
        val segment = AnsiSegment(text = "hello")
        assertEquals("hello", segment.text)
        assertEquals(AnsiColor.DEFAULT, segment.foreground)
        assertEquals(AnsiColor.DEFAULT, segment.background)
        assertFalse(segment.bold)
        assertFalse(segment.italic)
        assertFalse(segment.underline)
        assertFalse(segment.dim)
        assertFalse(segment.strikethrough)
    }

    @Test
    fun testAnsiSegmentWithAttributes() {
        val segment = AnsiSegment(
            text = "bold red",
            foreground = AnsiColor.RED,
            bold = true
        )
        assertEquals(AnsiColor.RED, segment.foreground)
        assertTrue(segment.bold)
    }

    // --- AnsiColor ---

    @Test
    fun testAnsiColorEntries() {
        assertEquals(17, AnsiColor.entries.size)
    }

    @Test
    fun testAnsiColorDefault() {
        assertEquals(-1, AnsiColor.DEFAULT.code)
        assertEquals(0xFFCCCCCC, AnsiColor.DEFAULT.hex)
    }

    @Test
    fun testAnsiColorRed() {
        assertEquals(1, AnsiColor.RED.code)
    }

    @Test
    fun testAnsiColorFromCode() {
        assertEquals(AnsiColor.BLACK, AnsiColor.fromCode(0))
        assertEquals(AnsiColor.RED, AnsiColor.fromCode(1))
        assertEquals(AnsiColor.DEFAULT, AnsiColor.fromCode(999))
    }

    @Test
    fun testAnsiColorFrom256() {
        assertEquals(AnsiColor.DEFAULT, AnsiColor.from256(-1))
        assertEquals(AnsiColor.BLACK, AnsiColor.from256(0))
    }

    // --- TerminalConfig ---

    @Test
    fun testTerminalConfigDefaults() {
        val config = TerminalConfig()
        assertTrue(config.showTimestamps)
        assertEquals(13, config.fontSize)
        assertEquals(10000, config.maxLines)
        assertTrue(config.enableColors)
        assertFalse(config.enableBell)
        assertEquals(5000, config.scrollbackLines)
    }

    // --- TerminalState ---

    @Test
    fun testTerminalStateDefaults() {
        val state = TerminalState()
        assertEquals(1, state.terminals.size)
        assertTrue(state.entries.isEmpty())
        assertTrue(state.commandHistory.isEmpty())
        assertEquals(-1, state.historyIndex)
        assertEquals("", state.currentInput)
        assertFalse(state.isExecuting)
    }

    @Test
    fun testTerminalStateActiveTerminal() {
        val state = TerminalState()
        assertNotNull(state.activeTerminal)
        assertEquals(state.terminals[0].id, state.activeTerminalId)
    }

    @Test
    fun testTerminalStateActiveEntries() {
        val terminal = Terminal()
        val entry = TerminalEntry(
            terminalId = terminal.id,
            type = TerminalEntryType.OUTPUT,
            content = "hello"
        )
        val state = TerminalState(
            terminals = listOf(terminal),
            activeTerminalId = terminal.id,
            entries = mapOf(terminal.id to listOf(entry))
        )
        assertEquals(1, state.activeEntries.size)
    }

    @Test
    fun testTerminalStateNoActiveTerminal() {
        val state = TerminalState(terminals = emptyList(), activeTerminalId = "")
        assertNull(state.activeTerminal)
        assertTrue(state.activeEntries.isEmpty())
    }
}
