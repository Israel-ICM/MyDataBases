package com.sphynxs.mydatabases.domain.sql

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TDD tests for `ScriptModels` (change `large-sql-script-execution`, Phase 1).
 *
 * Only `RiskReport.isRisky` carries logic (branching on category counts); the other
 * models (`ScriptStatement`, `RiskCategory`, `ScriptExecutionProgress`,
 * `ScriptExecutionSummary`, `ScriptError`) are pure data holders with no behavior to
 * triangulate, per strict-tdd's structural-skip rule.
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-07-31
 */
class ScriptModelsTest {

    @Test
    fun `isRisky is true when any risk category count is greater than zero`() {
        val report = RiskReport(
            totalStatements = 5,
            counts = mapOf(RiskCategory.DDL to 1, RiskCategory.DELETE to 0, RiskCategory.UPDATE_NO_WHERE to 0),
            lineNumbers = mapOf(RiskCategory.DDL to listOf(3), RiskCategory.DELETE to emptyList(), RiskCategory.UPDATE_NO_WHERE to emptyList())
        )

        assertTrue(report.isRisky)
    }

    @Test
    fun `isRisky is false when every risk category count is zero`() {
        val report = RiskReport(
            totalStatements = 5,
            counts = mapOf(RiskCategory.DDL to 0, RiskCategory.DELETE to 0, RiskCategory.UPDATE_NO_WHERE to 0),
            lineNumbers = mapOf(RiskCategory.DDL to emptyList(), RiskCategory.DELETE to emptyList(), RiskCategory.UPDATE_NO_WHERE to emptyList())
        )

        assertFalse(report.isRisky)
    }

    @Test
    fun `isRisky is true when only DELETE count is positive`() {
        val report = RiskReport(
            totalStatements = 10,
            counts = mapOf(RiskCategory.DDL to 0, RiskCategory.DELETE to 2, RiskCategory.UPDATE_NO_WHERE to 0),
            lineNumbers = mapOf(RiskCategory.DDL to emptyList(), RiskCategory.DELETE to listOf(4, 9), RiskCategory.UPDATE_NO_WHERE to emptyList())
        )

        assertTrue(report.isRisky)
    }

    @Test
    fun `isRisky is false for an empty counts map`() {
        val report = RiskReport(totalStatements = 0, counts = emptyMap(), lineNumbers = emptyMap())

        assertFalse(report.isRisky)
    }
}
