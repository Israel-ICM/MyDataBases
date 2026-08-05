package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.domain.sql.RiskCategory
import com.sphynxs.mydatabases.domain.sql.ScriptError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringReader

/**
 * TDD tests for `PreScanScriptUseCase` (change `large-sql-script-execution`, Phase 8).
 *
 * Pure domain use case: takes a caller-supplied `Reader` (Uri-to-Reader resolution stays with
 * the caller, e.g. the ViewModel via `ContentResolver`) so it is fully unit-testable without
 * Android framework mocking, consistent with the splitter's "Reader-in / Flow-out" contract.
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-04
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PreScanScriptUseCaseTest {

    private val useCase = PreScanScriptUseCase()

    @Test
    fun `aggregates correct counts and exact line numbers for a mixed script`() = runTest {
        val sql = """
            CREATE TABLE t (id INT);
            INSERT INTO t VALUES (1);
            DELETE FROM t WHERE id = 1;
            UPDATE t SET id = 2;
            SELECT * FROM t;
        """.trimIndent()

        val events = useCase(StringReader(sql)).toList()

        val completed = events.filterIsInstance<PreScanEvent.Completed>().single()
        val report = completed.report

        assertEquals(5, report.totalStatements)
        assertEquals(1, report.counts[RiskCategory.DDL])
        assertEquals(1, report.counts[RiskCategory.DELETE])
        assertEquals(1, report.counts[RiskCategory.UPDATE_NO_WHERE])
        assertEquals(listOf(1), report.lineNumbers[RiskCategory.DDL])
        assertEquals(listOf(3), report.lineNumbers[RiskCategory.DELETE])
        assertEquals(listOf(4), report.lineNumbers[RiskCategory.UPDATE_NO_WHERE])
        assertTrue(report.isRisky)
    }

    @Test
    fun `clean script reports isRisky false`() = runTest {
        val sql = "INSERT INTO t VALUES (1);\nSELECT * FROM t WHERE id = 1;\nUPDATE t SET x=1 WHERE id=1;"

        val events = useCase(StringReader(sql)).toList()

        val report = events.filterIsInstance<PreScanEvent.Completed>().single().report
        assertEquals(3, report.totalStatements)
        assertTrue(report.counts.values.all { it == 0 })
        assertTrue(!report.isRisky)
    }

    @Test
    fun `emits progress events as statements are scanned`() = runTest {
        val sql = "SELECT 1;\nSELECT 2;\nSELECT 3;"

        val events = useCase(StringReader(sql)).toList()

        val progressEvents = events.filterIsInstance<PreScanEvent.Progress>()
        assertEquals(3, progressEvents.size)
        assertEquals(1, progressEvents[0].statementsScanned)
        assertEquals(3, progressEvents[2].statementsScanned)
    }

    @Test
    fun `an unparseable DELIMITER surfaces as Error and never emits Completed`() = runTest {
        val sql = "SELECT 1;\nDELIMITER\nSELECT 2;"

        val events = useCase(StringReader(sql)).toList()

        assertTrue(events.none { it is PreScanEvent.Completed })
        val error = events.filterIsInstance<PreScanEvent.Error>().single()
        assertTrue(error.error is ScriptError.MalformedDelimiterDirective)
    }
}
