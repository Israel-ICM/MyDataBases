package com.sphynxs.mydatabases.domain.sql

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.Reader
import java.io.StringReader

/**
 * TDD tests for `LineThresholdGuard` (change `large-sql-script-execution`, Phase 2).
 *
 * Covers the fixed 50,000-line v1 threshold (strictly-greater-than comparison) and the
 * early-exit contract: the guard must not read past the (threshold + 1)th line.
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-07-31
 */
class LineThresholdGuardTest {

    @Test
    fun `exceedsThreshold returns true for 50001 lines`() {
        val content = buildString { repeat(50_001) { append("line;\n") } }

        val result = LineThresholdGuard.exceedsThreshold(StringReader(content))

        assertTrue(result)
    }

    @Test
    fun `exceedsThreshold returns false for exactly 50000 lines`() {
        val content = buildString { repeat(50_000) { append("line;\n") } }

        val result = LineThresholdGuard.exceedsThreshold(StringReader(content))

        assertFalse(result)
    }

    @Test
    fun `exceedsThreshold returns false for an 8000-line file`() {
        val content = buildString { repeat(8_000) { append("line;\n") } }

        val result = LineThresholdGuard.exceedsThreshold(StringReader(content))

        assertFalse(result)
    }

    @Test
    fun `exceedsThreshold stops reading shortly after the threshold-plus-one line`() {
        // 1,000,000 lines available, but the guard must exit long before consuming them all.
        val hugeContent = buildString { repeat(1_000_000) { append("line;\n") } }
        val countingReader = CountingReader(StringReader(hugeContent))

        val result = LineThresholdGuard.exceedsThreshold(countingReader)

        assertTrue(result)
        // 50,001 lines * 6 chars ("line;\n") = 300,006 chars; allow one extra buffer fill
        // of headroom (BufferedReader's internal chunking) but stay far below the
        // 1,000,000-line (6,000,000 char) full input.
        assertTrue(
            "expected early exit, but read ${countingReader.charsRead} chars",
            countingReader.charsRead < 400_000
        )
    }

    /** Wraps a [Reader] and counts every character actually pulled from the delegate. */
    private class CountingReader(private val delegate: Reader) : Reader() {
        var charsRead = 0
            private set

        override fun read(cbuf: CharArray, off: Int, len: Int): Int {
            val n = delegate.read(cbuf, off, len)
            if (n > 0) charsRead += n
            return n
        }

        override fun close() = delegate.close()
    }
}
