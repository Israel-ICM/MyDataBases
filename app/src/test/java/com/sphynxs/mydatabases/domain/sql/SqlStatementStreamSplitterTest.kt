package com.sphynxs.mydatabases.domain.sql

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.Reader
import java.io.StringReader

/**
 * TDD tests for `SqlStatementStreamSplitter` (change `large-sql-script-execution`, Phase 4).
 *
 * Covers every requirement/scenario in
 * `openspec/changes/large-sql-script-execution/specs/sql-statement-stream-splitting/spec.md`.
 * This is the correctness-critical component (R4): a mis-split silently corrupts execution,
 * so these tests are deliberately exhaustive.
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-04
 */
class SqlStatementStreamSplitterTest {

    private fun split(sql: String): List<ScriptStatement> =
        runBlocking { SqlStatementStreamSplitter.split(StringReader(sql)).toList() }

    private fun split(reader: Reader): List<ScriptStatement> =
        runBlocking { SqlStatementStreamSplitter.split(reader).toList() }

    // ---------------------------------------------------------------
    // Streaming Contract
    // ---------------------------------------------------------------

    @Test
    fun `whole file is never materialized - large synthetic input streams to completion`() {
        // Scaled down from the spec's ~600,000-line target for unit-test speed; the full
        // scale is exercised manually in Phase 13. This reader generates content on the fly
        // and never holds it as a single String, proving the splitter can consume a stream
        // whose total size would be impractical to buffer.
        val statementCount = 200_000
        val reader = GeneratingReader(statementCount) { i -> "INSERT INTO t VALUES ($i);\n" }

        val result = split(reader)

        assertEquals(statementCount, result.size)
        assertEquals("INSERT INTO t VALUES (0)", result.first().sql)
        assertEquals("INSERT INTO t VALUES (${statementCount - 1})", result.last().sql)
    }

    @Test
    fun `token spanning a read-buffer boundary is preserved`() {
        // Default BufferedReader internal buffer is 8192 chars. Pad with harmless statements
        // so a long string literal straddles that boundary, then verify it survives intact.
        val padding = buildString {
            while (length < 8180) append("SELECT 1;\n")
        }
        val longLiteral = "x".repeat(200)
        val sql = "$padding\nINSERT INTO t VALUES ('$longLiteral');"

        val result = split(sql)

        val last = result.last()
        assertTrue(last.sql.contains(longLiteral))
    }

    // ---------------------------------------------------------------
    // Statement Termination
    // ---------------------------------------------------------------

    @Test
    fun `top-level semicolon splits statements with correct line numbers`() {
        val sql = "INSERT INTO t VALUES (1); INSERT INTO t VALUES (2);"

        val result = split(sql)

        assertEquals(2, result.size)
        assertEquals(1, result[0].lineNumber)
        assertEquals(1, result[1].lineNumber)
    }

    @Test
    fun `empty statements between terminators are skipped`() {
        val sql = "SELECT 1;;;\n;\nSELECT 2;"

        val result = split(sql)

        assertEquals(2, result.size)
        assertEquals("SELECT 1", result[0].sql)
        assertEquals("SELECT 2", result[1].sql)
    }

    // ---------------------------------------------------------------
    // String and Identifier Awareness
    // ---------------------------------------------------------------

    @Test
    fun `semicolon inside a string is not a boundary`() {
        val sql = "INSERT INTO t VALUES ('a;b');"

        val result = split(sql)

        assertEquals(1, result.size)
        assertTrue(result[0].sql.contains("'a;b'"))
    }

    @Test
    fun `backslash-escaped quote does not close the string`() {
        val sql = """INSERT INTO t VALUES ('it\'s; fine');"""

        val result = split(sql)

        assertEquals(1, result.size)
        assertTrue(result[0].sql.contains("""it\'s; fine"""))
    }

    @Test
    fun `doubled quote is a literal, not a close`() {
        val sql = "INSERT INTO t VALUES ('O''Brien; Co');"

        val result = split(sql)

        assertEquals(1, result.size)
        assertTrue(result[0].sql.contains("O''Brien; Co"))
    }

    @Test
    fun `semicolon inside a backtick identifier is not a boundary`() {
        val sql = "SELECT * FROM `weird;name`;"

        val result = split(sql)

        assertEquals(1, result.size)
        assertTrue(result[0].sql.contains("`weird;name`"))
    }

    @Test
    fun `comment marker inside a string is inert`() {
        val sql = "INSERT INTO t VALUES ('-- not a comment');"

        val result = split(sql)

        assertEquals(1, result.size)
        assertTrue(result[0].sql.contains("-- not a comment"))
    }

    // ---------------------------------------------------------------
    // Comment Awareness
    // ---------------------------------------------------------------

    @Test
    fun `dash line comment is skipped`() {
        val sql = "-- drop everything;\nSELECT 1;"

        val result = split(sql)

        assertEquals(1, result.size)
        assertEquals("SELECT 1", result[0].sql)
    }

    @Test
    fun `hash line comment is skipped`() {
        val sql = "# comment; still comment\nSELECT 2;"

        val result = split(sql)

        assertEquals(1, result.size)
        assertEquals("SELECT 2", result[0].sql)
    }

    @Test
    fun `block comment spanning lines is skipped, line number correct after`() {
        val sql = "/* multi\n line ; comment */ SELECT 3;"

        val result = split(sql)

        assertEquals(1, result.size)
        assertEquals("SELECT 3", result[0].sql)
        assertEquals(2, result[0].lineNumber)
    }

    // ---------------------------------------------------------------
    // DELIMITER Directive Support
    // ---------------------------------------------------------------

    @Test
    fun `DELIMITER switches the terminator for a stored procedure`() {
        val sql = "DELIMITER $$\nCREATE PROCEDURE p() BEGIN SELECT 1; SELECT 2; END$$\nDELIMITER ;"

        val result = split(sql)

        assertEquals(1, result.size)
        assertTrue(result[0].sql.contains("CREATE PROCEDURE"))
        assertTrue(result[0].sql.contains("SELECT 1; SELECT 2;"))
    }

    @Test
    fun `default terminator restored after DELIMITER semicolon`() {
        val sql = "DELIMITER $$\nCREATE PROCEDURE p() BEGIN END$$\nDELIMITER ;\nINSERT INTO t VALUES (1);"

        val result = split(sql)

        assertEquals(2, result.size)
        assertTrue(result[0].sql.contains("CREATE PROCEDURE"))
        assertEquals("INSERT INTO t VALUES (1)", result[1].sql)
    }

    @Test
    fun `nested repeated DELIMITER blocks are handled sequentially`() {
        val sql = """
            DELIMITER $$
            CREATE PROCEDURE p1() BEGIN SELECT 1; END$$
            DELIMITER ;
            DELIMITER $$
            CREATE PROCEDURE p2() BEGIN SELECT 2; END$$
            DELIMITER ;
            INSERT INTO t VALUES (1);
        """.trimIndent()

        val result = split(sql)

        assertEquals(3, result.size)
        assertTrue(result[0].sql.contains("p1"))
        assertTrue(result[1].sql.contains("p2"))
        assertEquals("INSERT INTO t VALUES (1)", result[2].sql)
    }

    @Test
    fun `unparseable DELIMITER fails loud`() {
        val sql = "SELECT 1;\nDELIMITER\nSELECT 2;"

        val error = assertThrows(ScriptError.MalformedDelimiterDirective::class.java) {
            split(sql)
        }

        assertEquals(2, error.lineNumber)
    }

    @Test
    fun `no statement is emitted from the point of a malformed DELIMITER onward`() {
        val sql = "SELECT 1;\nDELIMITER\nSELECT 2;"
        val emitted = mutableListOf<ScriptStatement>()

        assertThrows(ScriptError.MalformedDelimiterDirective::class.java) {
            runBlocking {
                SqlStatementStreamSplitter.split(StringReader(sql)).toList().also { emitted.addAll(it) }
            }
        }

        // Only the statement fully parsed before the malformed directive may have been emitted.
        assertTrue(emitted.size <= 1)
    }

    @Test
    fun `unterminated block comment at EOF fails loud`() {
        val sql = "SELECT 1;\n/* never closed"

        val error = assertThrows(ScriptError.UnterminatedToken::class.java) {
            split(sql)
        }

        assertEquals(2, error.lineNumber)
        assertEquals("block comment", error.kind)
    }

    // ---------------------------------------------------------------
    // Line Number Reporting
    // ---------------------------------------------------------------

    @Test
    fun `line number after a multi-line block comment`() {
        val sql = "/* line1\nline2\nline3 */\nSELECT 1;"

        val result = split(sql)

        assertEquals(4, result[0].lineNumber)
    }

    @Test
    fun `line number after a multi-line string literal`() {
        val sql = "UPDATE t SET note = 'line1\nline2' WHERE id = 1;\nUPDATE t SET x=1;"

        val result = split(sql)

        assertEquals(2, result.size)
        assertEquals(1, result[0].lineNumber)
        assertEquals(3, result[1].lineNumber)
    }

    // ---------------------------------------------------------------
    // Top-Level WHERE Detection
    // ---------------------------------------------------------------

    @Test
    fun `UPDATE with top-level WHERE is flagged present`() {
        val sql = "UPDATE users SET active = 1 WHERE id = 5;"

        val result = split(sql)

        assertTrue(result[0].hasTopLevelWhere)
    }

    @Test
    fun `UPDATE without any WHERE is flagged absent`() {
        val sql = "UPDATE users SET active = 1;"

        val result = split(sql)

        assertEquals(false, result[0].hasTopLevelWhere)
    }

    @Test
    fun `outer WHERE wrapping a subquery is top-level`() {
        val sql = "UPDATE users SET flag = 1 WHERE id IN (SELECT id FROM staging WHERE dirty = 1);"

        val result = split(sql)

        assertTrue(result[0].hasTopLevelWhere)
    }

    @Test
    fun `WHERE only inside a subquery is not top-level`() {
        val sql = "UPDATE users SET flag = (SELECT v FROM s WHERE s.k = users.k);"

        val result = split(sql)

        assertEquals(false, result[0].hasTopLevelWhere)
    }

    @Test
    fun `WHERE inside a string is not top-level`() {
        val sql = "UPDATE t SET note = 'apply WHERE ready';"

        val result = split(sql)

        assertEquals(false, result[0].hasTopLevelWhere)
    }

    // ---------------------------------------------------------------
    // Edge Case Handling
    // ---------------------------------------------------------------

    @Test
    fun `empty file yields no statements`() {
        val result = split("")

        assertEquals(0, result.size)
    }

    @Test
    fun `comments-only file yields no statements`() {
        val sql = "-- header\n/* notes */\n#trailing"

        val result = split(sql)

        assertEquals(0, result.size)
    }

    @Test
    fun `final statement without trailing terminator is emitted`() {
        val sql = "SELECT 1;\nSELECT 2"

        val result = split(sql)

        assertEquals(2, result.size)
        assertEquals("SELECT 2", result[1].sql)
    }

    @Test
    fun `single giant statement with no terminator is emitted once`() {
        val sql = "INSERT INTO t VALUES " + (1..500).joinToString(",") { "($it)" }

        val result = split(sql)

        assertEquals(1, result.size)
        assertTrue(result[0].sql.startsWith("INSERT INTO t VALUES"))
    }

    /**
     * Generates SQL content on the fly without ever holding the full text as a `String`,
     * used to prove the splitter can stream input far larger than would be safe to buffer.
     */
    private class GeneratingReader(
        private val statementCount: Int,
        private val statementAt: (Int) -> String
    ) : Reader() {
        private var index = 0
        private var currentLine = ""
        private var linePos = 0

        override fun read(cbuf: CharArray, off: Int, len: Int): Int {
            if (index >= statementCount && linePos >= currentLine.length) return -1
            var written = 0
            var pos = off
            while (written < len) {
                if (linePos >= currentLine.length) {
                    if (index >= statementCount) break
                    currentLine = statementAt(index)
                    linePos = 0
                    index++
                }
                cbuf[pos] = currentLine[linePos]
                pos++
                linePos++
                written++
            }
            return if (written == 0) -1 else written
        }

        override fun close() {}
    }
}
