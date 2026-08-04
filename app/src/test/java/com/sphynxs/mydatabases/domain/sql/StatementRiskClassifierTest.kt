package com.sphynxs.mydatabases.domain.sql

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * TDD tests for `StatementRiskClassifier` (change `large-sql-script-execution`, Phase 3).
 *
 * Covers the full locked rule table from `large-sql-script-execution/spec.md`:
 * DDL, DELETE, UPDATE with/without top-level WHERE, INSERT, SELECT, and a
 * DELIMITER-defined stored-procedure body classifying by its leading DDL keyword.
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-07-31
 */
class StatementRiskClassifierTest {

    private fun statement(sql: String, hasTopLevelWhere: Boolean = false) =
        ScriptStatement(sql = sql, lineNumber = 1, hasTopLevelWhere = hasTopLevelWhere)

    // --- DDL: always CONFIRM ---

    @Test
    fun `classify returns DDL for CREATE`() {
        assertEquals(RiskCategory.DDL, StatementRiskClassifier.classify(statement("CREATE TABLE u (id INT)")))
    }

    @Test
    fun `classify returns DDL for ALTER`() {
        assertEquals(RiskCategory.DDL, StatementRiskClassifier.classify(statement("ALTER TABLE t ADD c INT")))
    }

    @Test
    fun `classify returns DDL for DROP`() {
        assertEquals(RiskCategory.DDL, StatementRiskClassifier.classify(statement("DROP TABLE t")))
    }

    @Test
    fun `classify returns DDL for TRUNCATE`() {
        assertEquals(RiskCategory.DDL, StatementRiskClassifier.classify(statement("TRUNCATE t")))
    }

    @Test
    fun `classify returns DDL for RENAME`() {
        assertEquals(RiskCategory.DDL, StatementRiskClassifier.classify(statement("RENAME TABLE a TO b")))
    }

    @Test
    fun `classify is case-insensitive on the leading DDL keyword`() {
        assertEquals(RiskCategory.DDL, StatementRiskClassifier.classify(statement("drop table t")))
    }

    // --- DELETE: always CONFIRM, regardless of WHERE ---

    @Test
    fun `classify returns DELETE for DELETE without WHERE`() {
        assertEquals(RiskCategory.DELETE, StatementRiskClassifier.classify(statement("DELETE FROM t")))
    }

    @Test
    fun `classify returns DELETE for DELETE with WHERE`() {
        assertEquals(
            RiskCategory.DELETE,
            StatementRiskClassifier.classify(statement("DELETE FROM t WHERE id = 1", hasTopLevelWhere = true))
        )
    }

    // --- UPDATE: depends on top-level WHERE ---

    @Test
    fun `classify returns UPDATE_NO_WHERE for UPDATE without top-level WHERE`() {
        assertEquals(
            RiskCategory.UPDATE_NO_WHERE,
            StatementRiskClassifier.classify(statement("UPDATE t SET x = 1", hasTopLevelWhere = false))
        )
    }

    @Test
    fun `classify returns null for UPDATE with top-level WHERE`() {
        assertNull(
            StatementRiskClassifier.classify(statement("UPDATE t SET x = 1 WHERE id = 5", hasTopLevelWhere = true))
        )
    }

    // --- INSERT / SELECT: never CONFIRM ---

    @Test
    fun `classify returns null for INSERT`() {
        assertNull(StatementRiskClassifier.classify(statement("INSERT INTO t VALUES (1)")))
    }

    @Test
    fun `classify returns null for SELECT`() {
        assertNull(StatementRiskClassifier.classify(statement("SELECT * FROM t")))
    }

    // --- DELIMITER-defined stored procedure body classifies by leading DDL keyword ---

    @Test
    fun `classify returns DDL for a CREATE PROCEDURE stored-procedure body`() {
        val procedureBody = "CREATE PROCEDURE p() BEGIN SELECT 1; SELECT 2; END"
        assertEquals(RiskCategory.DDL, StatementRiskClassifier.classify(statement(procedureBody)))
    }
}
