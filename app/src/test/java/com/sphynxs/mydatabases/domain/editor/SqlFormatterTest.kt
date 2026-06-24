package com.sphynxs.mydatabases.domain.editor

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for SqlFormatter — pure SQL formatter with idempotency contract.
 *
 * TDD RED-first workflow:
 * 1. Write failing test (scenario from spec.md)
 * 2. Implement minimum code to pass
 * 3. Triangulate with additional test cases
 * 4. Refactor
 *
 * Spec: openspec/changes/editor-completion-and-format/spec.md (scenarios 1-8)
 * Design: ADR 2 — Formatter is pure function, idempotent
 *
 * @author israel-icm
 * @date 2026-06-24
 */
class SqlFormatterTest {

    // Scenario 1: Format simple SELECT with WHERE
    @Test
    fun format_simpleSelectWithWhere_producesExpectedLayout() {
        // RED: This will FAIL until SqlFormatter.format() is implemented
        val input = "select id, name from users where active = 1"
        
        val expected = """
            SELECT id, name
            FROM users
            WHERE active = 1
        """.trimIndent()
        
        val result = SqlFormatter.format(input)
        
        assertThat(result).isEqualTo(expected)
    }

    // Scenario 2: Format multi-table JOIN
    @Test
    fun format_innerJoinWithOnPredicate_indentsOnUnderJoin() {
        // RED: Tests JOIN newline + ON indentation (2 spaces)
        val input = "select u.id from users u inner join orders o on u.id = o.user_id where o.total > 100"
        
        val expected = """
            SELECT u.id
            FROM users u
            INNER JOIN orders o
              ON u.id = o.user_id
            WHERE o.total > 100
        """.trimIndent()
        
        val result = SqlFormatter.format(input)
        
        assertThat(result).isEqualTo(expected)
    }

    // Scenario 3: Format nested subquery (flat indent for v1)
    @Test
    fun format_nestedSubquery_uppercasesKeywordsWithoutDeepIndent() {
        // RED: Subquery keywords UPPERCASE, but NO deep indentation (flat for v1)
        val input = "select id from users where id in (select user_id from orders where total > 100)"
        
        val expected = """
            SELECT id
            FROM users
            WHERE id IN (SELECT user_id
            FROM orders
            WHERE total > 100)
        """.trimIndent()
        
        val result = SqlFormatter.format(input)
        
        assertThat(result).isEqualTo(expected)
    }

    // Scenario 4: Preserve string literals (single and double quotes)
    @Test
    fun format_stringLiterals_preservedVerbatim() {
        // RED: Keywords inside strings MUST NOT be uppercased
        val input = "select 'select from where' as label, \"JOIN\" as kw from t"
        
        val expected = """
            SELECT 'select from where' AS label, "JOIN" AS kw
            FROM t
        """.trimIndent()
        
        val result = SqlFormatter.format(input)
        
        assertThat(result).isEqualTo(expected)
    }

    // Scenario 5: Preserve inline (`--`) comments
    @Test
    fun format_lineComment_preservedVerbatim() {
        // RED: Comments MUST NOT be modified (preserve verbatim)
        val input = "select id from users -- foo bar select from where"
        
        val expected = """
            SELECT id
            FROM users -- foo bar select from where
        """.trimIndent()
        
        val result = SqlFormatter.format(input)
        
        assertThat(result).isEqualTo(expected)
    }

    // Scenario 6: Preserve block (`/* ... */`) comments
    @Test
    fun format_blockComment_preservedVerbatim() {
        // RED: Block comments MUST NOT be modified
        val input = "select id /* explanation FROM WHERE */ from users"
        
        val expected = """
            SELECT id /* explanation FROM WHERE */
            FROM users
        """.trimIndent()
        
        val result = SqlFormatter.format(input)
        
        assertThat(result).isEqualTo(expected)
    }

    // Scenario 7: Preserve user projection formatting (do not break at commas)
    @Test
    fun format_projectionList_keptOnOneLine() {
        // RED: Projection list MUST stay on one line (user's choice)
        val input = "SELECT a, b, c FROM t"
        
        val expected = """
            SELECT a, b, c
            FROM t
        """.trimIndent()
        
        val result = SqlFormatter.format(input)
        
        assertThat(result).isEqualTo(expected)
    }

    // Scenario 8: Idempotent formatting
    @Test
    fun format_isIdempotent_acrossAllGoldenFixtures() {
        // RED: format(format(x)) MUST equal format(x)
        val testCases = listOf(
            "select id from users where active = 1",
            "select u.id from users u inner join orders o on u.id = o.user_id",
            "select 'select' as kw from t",
            "select id -- comment\nfrom users"
        )
        
        for (input in testCases) {
            val formattedOnce = SqlFormatter.format(input)
            val formattedTwice = SqlFormatter.format(formattedOnce)
            
            assertThat(formattedTwice).isEqualTo(formattedOnce)
        }
    }

    // Additional triangulation tests (edge cases)
    
    @Test
    fun format_emptyString_returnsEmpty() {
        // Triangulation: empty input
        val result = SqlFormatter.format("")
        assertThat(result).isEmpty()
    }

    @Test
    fun format_onlyWhitespace_returnsEmpty() {
        // Triangulation: only whitespace
        val result = SqlFormatter.format("   \n\t  ")
        assertThat(result).isEmpty()
    }

    @Test
    fun format_mixedCaseKeywords_allUppercase() {
        // Triangulation: mixed case keywords normalized
        val input = "SeLeCt id FrOm users WhErE active = 1"
        
        val result = SqlFormatter.format(input)
        
        assertThat(result).contains("SELECT")
        assertThat(result).contains("FROM")
        assertThat(result).contains("WHERE")
        assertThat(result).doesNotContain("SeLeCt")
        assertThat(result).doesNotContain("FrOm")
    }

    @Test
    fun format_trailingSemicolon_preserved() {
        // Triangulation: semicolon at end preserved
        val input = "select id from users;"
        
        val expected = """
            SELECT id
            FROM users;
        """.trimIndent()
        
        val result = SqlFormatter.format(input)
        
        assertThat(result).isEqualTo(expected)
    }
}
