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
                            // Truecolor 38;2;r;g;b — map to nearest base color.
                            val r = codes[i + 2].coerceIn(0, 255)
                            val g = codes[i + 3].coerceIn(0, 255)
                            val b = codes[i + 4].coerceIn(0, 255)
                            currentForeground = AnsiColor.from256(rgbTo256(r, g, b))
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
                            val r = codes[i + 2].coerceIn(0, 255)
                            val g = codes[i + 3].coerceIn(0, 255)
                            val b = codes[i + 4].coerceIn(0, 255)
                            currentBackground = AnsiColor.from256(rgbTo256(r, g, b))
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
        // SGR + CSI (cursor, erase, scroll) + OSC hyperlinks.
        return text
            .replace(Regex("\u001B\\[[0-9;?]*[a-zA-Z]"), "")
            .replace(Regex("\u001B\\].*?(\u0007|\u001B\\\\)"), "")
            .replace(Regex("\u001B[()][0-9A-B]"), "")
    }

    private fun rgbTo256(r: Int, g: Int, b: Int): Int {
        if (r == g && g == b) {
            if (r < 8) return 16
            if (r > 248) return 231
            return ((r - 8) / 10 + 232).coerceIn(232, 255)
        }
        return 16 + (r / 51) * 36 + (g / 51) * 6 + (b / 51)
    }

    fun hasAnsi(text: String): Boolean {
        return ANSI_PATTERN.containsMatchIn(text)
    }
}
