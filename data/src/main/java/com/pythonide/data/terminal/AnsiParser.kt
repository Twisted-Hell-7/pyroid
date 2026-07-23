package com.pythonide.data.terminal

import com.pythonide.domain.model.terminal.AnsiColor
import com.pythonide.domain.model.terminal.AnsiSegment

object AnsiParser {

    private val ANSI_PATTERN = Regex("\u001b\\[([0-9;]*)m")

    fun parse(text: String): List<AnsiSegment> {
        val segments = mutableListOf<AnsiSegment>()
        var currentForeground = AnsiColor.DEFAULT
        var currentBackground = AnsiColor.DEFAULT
        var currentBold = false
        var currentItalic = false
        var currentUnderline = false
        var currentDim = false
        var currentStrikethrough = false

        var lastIndex = 0
        val matches = ANSI_PATTERN.findAll(text)

        for (match in matches) {
            if (match.range.first > lastIndex) {
                val plainText = text.substring(lastIndex, match.range.first)
                if (plainText.isNotEmpty()) {
                    segments.add(
                        AnsiSegment(
                            text = plainText,
                            foreground = currentForeground,
                            background = currentBackground,
                            bold = currentBold,
                            italic = currentItalic,
                            underline = currentUnderline,
                            dim = currentDim,
                            strikethrough = currentStrikethrough
                        )
                    )
                }
            }

            val codes = match.groupValues[1]
                .split(";")
                .mapNotNull { it.toIntOrNull() }

            var i = 0
            while (i < codes.size) {
                when (codes[i]) {
                    0 -> {
                        currentForeground = AnsiColor.DEFAULT
                        currentBackground = AnsiColor.DEFAULT
                        currentBold = false
                        currentItalic = false
                        currentUnderline = false
                        currentDim = false
                        currentStrikethrough = false
                    }
                    1 -> currentBold = true
                    2 -> currentDim = true
                    3 -> currentItalic = true
                    4 -> currentUnderline = true
                    9 -> currentStrikethrough = true
                    22 -> {
                        currentBold = false
                        currentDim = false
                    }
                    23 -> currentItalic = false
                    24 -> currentUnderline = false
                    29 -> currentStrikethrough = false
                    in 30..37 -> currentForeground = AnsiColor.fromCode(codes[i] - 30)
                    38 -> {
                        if (i + 1 < codes.size && codes[i + 1] == 5 && i + 2 < codes.size) {
                            currentForeground = AnsiColor.from256(codes[i + 2])
                            i += 2
                        } else if (i + 1 < codes.size && codes[i + 1] == 2 && i + 4 < codes.size) {
                            currentForeground = AnsiColor.DEFAULT
                            i += 4
                        }
                    }
                    39 -> currentForeground = AnsiColor.DEFAULT
                    in 40..47 -> currentBackground = AnsiColor.fromCode(codes[i] - 40)
                    48 -> {
                        if (i + 1 < codes.size && codes[i + 1] == 5 && i + 2 < codes.size) {
                            currentBackground = AnsiColor.from256(codes[i + 2])
                            i += 2
                        } else if (i + 1 < codes.size && codes[i + 1] == 2 && i + 4 < codes.size) {
                            currentBackground = AnsiColor.DEFAULT
                            i += 4
                        }
                    }
                    49 -> currentBackground = AnsiColor.DEFAULT
                    in 90..97 -> currentForeground = AnsiColor.fromCode(codes[i] - 90 + 8)
                    in 100..107 -> currentBackground = AnsiColor.fromCode(codes[i] - 100 + 8)
                }
                i++
            }

            lastIndex = match.range.last + 1
        }

        if (lastIndex < text.length) {
            val remaining = text.substring(lastIndex)
            if (remaining.isNotEmpty()) {
                segments.add(
                    AnsiSegment(
                        text = remaining,
                        foreground = currentForeground,
                        background = currentBackground,
                        bold = currentBold,
                        italic = currentItalic,
                        underline = currentUnderline,
                        dim = currentDim,
                        strikethrough = currentStrikethrough
                    )
                )
            }
        }

        if (segments.isEmpty() && text.isNotEmpty()) {
            segments.add(AnsiSegment(text = text))
        }

        return segments
    }

    fun stripAnsi(text: String): String {
        return ANSI_PATTERN.replace(text, "")
    }

    fun hasAnsi(text: String): Boolean {
        return ANSI_PATTERN.containsMatchIn(text)
    }
}
