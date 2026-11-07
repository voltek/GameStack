package com.gamestack

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ClaudeMdBoundTest {

    private val text = File("../CLAUDE.md").readText().replace("\r\n", "\n")

    @Test
    fun `should keep CLAUDE md within its line bound`() {
        val lines = text.removeSuffix("\n").lines().size
        assertTrue(
            "CLAUDE.md has $lines lines, over $MAX_LINES: it loads on every request, so move " +
                "what binds one area to .claude/rules/ (ADR 0055).",
            lines <= MAX_LINES
        )
    }

    @Test
    fun `should keep CLAUDE md within its character bound`() {
        val characters = text.codePointCount(0, text.length)
        assertTrue(
            "CLAUDE.md has $characters characters, over $MAX_CHARACTERS: it loads on every " +
                "request, so move what binds one area to .claude/rules/ (ADR 0055).",
            characters <= MAX_CHARACTERS
        )
    }

    private companion object {
        const val MAX_LINES = 60
        const val MAX_CHARACTERS = 3_600
    }
}
