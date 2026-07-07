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
 * **[REVISED 2026-07-07]** Rewritten for ADR 7 (statement + list breaking via depth-tracked
 * state machine). `SELECT`/`FROM`/`WHERE` now break unconditionally onto their own clause
 * line with a 2-space-indented body; `INSERT INTO`/`VALUES` parenthesized comma lists break
 * one item per line; multi-statement input splits on top-level `;`. See design.md ADR 7
 * "Backward Compatibility" table for the exact reasoning behind each rewritten scenario below.
 *
 * Spec: openspec/changes/editor-completion-and-format/spec.md (scenarios 1-8f)
 * Design: ADR 2 — Formatter is pure function, idempotent. ADR 7 — depth-tracked state machine.
 *
 * @author israel-icm
 * @date 2026-07-07
 */
class SqlFormatterTest {

    // Scenario 1: Format simple SELECT with WHERE — [REVISED] column-per-line + FROM/WHERE indent
    @Test
    fun format_simpleSelectWithWhere_producesExpectedLayout() {
        val input = "select id, name from users where active = 1"

        val expected = """
            SELECT
              id,
              name
            FROM
              users
            WHERE
              active = 1
        """.trimIndent()

        val result = SqlFormatter.format(input)

        assertThat(result).isEqualTo(expected)
    }

    // Scenario 2: Format multi-table JOIN — [REVISED] same FROM/WHERE indent change, JOIN/ON unaffected
    @Test
    fun format_innerJoinWithOnPredicate_indentsOnUnderJoin() {
        val input = "select u.id from users u inner join orders o on u.id = o.user_id where o.total > 100"

        val expected = """
            SELECT
              u.id
            FROM
              users u
            INNER JOIN orders o
              ON u.id = o.user_id
            WHERE
              o.total > 100
        """.trimIndent()

        val result = SqlFormatter.format(input)

        assertThat(result).isEqualTo(expected)
    }

    // Scenario 3: Format nested subquery (flat indent past paren-depth 1) — [REVISED] outer WHERE
    // now indents (depth 0), inner subquery FROM/WHERE (depth >= 1) stay flat, unchanged.
    // Name kept unchanged per tasks.md Phase 1B instruction (spec Scenario 3).
    @Test
    fun format_nestedSubquery_uppercasesKeywordsWithoutDeepIndent() {
        val input = "select id from users where id in (select user_id from orders where total > 100)"

        val expected = """
            SELECT
              id
            FROM
              users
            WHERE
              id IN (SELECT user_id FROM orders WHERE total > 100)
        """.trimIndent()

        val result = SqlFormatter.format(input)

        assertThat(result).isEqualTo(expected)
    }

    // Scenario 4: Preserve string literals (single and double quotes) — [REVISED] projection breaks
    @Test
    fun format_stringLiterals_preservedVerbatim() {
        val input = "select 'select from where' as label, \"JOIN\" as kw from t"

        val expected = """
            SELECT
              'select from where' AS label,
              "JOIN" AS kw
            FROM
              t
        """.trimIndent()

        val result = SqlFormatter.format(input)

        assertThat(result).isEqualTo(expected)
    }

    // Scenario 5: Preserve inline (`--`) comments — [REVISED] SELECT alone even with 1 column
    @Test
    fun format_lineComment_preservedVerbatim() {
        val input = "select id from users -- foo bar select from where"

        val expected = """
            SELECT
              id
            FROM
              users -- foo bar select from where
        """.trimIndent()

        val result = SqlFormatter.format(input)

        assertThat(result).isEqualTo(expected)
    }

    // Scenario 6: Preserve block (`/* ... */`) comments — [REVISED] SELECT alone, comment stays inline
    @Test
    fun format_blockComment_preservedVerbatim() {
        val input = "select id /* explanation FROM WHERE */ from users"

        val expected = """
            SELECT
              id /* explanation FROM WHERE */
            FROM
              users
        """.trimIndent()

        val result = SqlFormatter.format(input)

        assertThat(result).isEqualTo(expected)
    }

    // Scenario 7a: Break SELECT projection list column-per-line — SUPERSEDES + renames the old
    // format_projectionList_keptOnOneLine (explicit reversal, see proposal.md REVISED 2026-07-07)
    @Test
    fun format_projectionList_breaksColumnPerLine() {
        val input = "SELECT a, b, c FROM t"

        val expected = """
            SELECT
              a,
              b,
              c
            FROM
              t
        """.trimIndent()

        val result = SqlFormatter.format(input)

        assertThat(result).isEqualTo(expected)
    }

    // Scenario 8: Idempotent formatting — unaffected by the ADR 7 revision (relative equality
    // assertion, not an exact-string check); kept as-is.
    @Test
    fun format_isIdempotent_acrossAllGoldenFixtures() {
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

    // Additional triangulation tests (edge cases) — unaffected by the ADR 7 revision

    @Test
    fun format_emptyString_returnsEmpty() {
        val result = SqlFormatter.format("")
        assertThat(result).isEmpty()
    }

    @Test
    fun format_onlyWhitespace_returnsEmpty() {
        val result = SqlFormatter.format("   \n\t  ")
        assertThat(result).isEmpty()
    }

    @Test
    fun format_mixedCaseKeywords_allUppercase() {
        val input = "SeLeCt id FrOm users WhErE active = 1"

        val result = SqlFormatter.format(input)

        assertThat(result).contains("SELECT")
        assertThat(result).contains("FROM")
        assertThat(result).contains("WHERE")
        assertThat(result).doesNotContain("SeLeCt")
        assertThat(result).doesNotContain("FrOm")
    }

    // Triangulation: semicolon at end preserved — [REVISED] same SELECT/FROM layout change
    @Test
    fun format_trailingSemicolon_preserved() {
        val input = "select id from users;"

        val expected = """
            SELECT
              id
            FROM
              users;
        """.trimIndent()

        val result = SqlFormatter.format(input)

        assertThat(result).isEqualTo(expected)
    }

    // Scenario 8a: Multi-statement split and independent formatting — maintainer's authoritative
    // golden-fixture example (INSERT/VALUES list-breaking + SELECT breaking + `;\n` join).
    @Test
    fun format_maintainerExample_producesExactLayout() {
        val input = "INSERT INTO `glo_service` (`gsv_id`,`gsv_tipo_modulo`,`gsv_almacen_siap`) " +
            "VALUES (2033, 'CRM', 'CRM');SELECT `gsv_id`,`gsv_tipo_modulo`, `gsv_almacen_siap` " +
            "FROM glo_service WHERE a > b;"

        val expected = """
            INSERT INTO `glo_service`
            (
              `gsv_id`,
              `gsv_tipo_modulo`,
              `gsv_almacen_siap`
            )
            VALUES
            (
              2033,
              'CRM',
              'CRM'
            );
            SELECT
              `gsv_id`,
              `gsv_tipo_modulo`,
              `gsv_almacen_siap`
            FROM
              glo_service
            WHERE
              a > b;
        """.trimIndent()

        val result = SqlFormatter.format(input)

        assertThat(result).isEqualTo(expected)
    }

    // Scenario 8d: FROM table indented under FROM
    @Test
    fun format_fromTable_indentedUnderFrom() {
        val input = "SELECT id FROM users"

        val expected = """
            SELECT
              id
            FROM
              users
        """.trimIndent()

        val result = SqlFormatter.format(input)

        assertThat(result).isEqualTo(expected)
    }

    // Scenario 8e: WHERE condition indented under WHERE
    @Test
    fun format_whereCondition_indentedUnderWhere() {
        val input = "SELECT id FROM users WHERE a > b"

        val expected = """
            SELECT
              id
            FROM
              users
            WHERE
              a > b
        """.trimIndent()

        val result = SqlFormatter.format(input)

        assertThat(result).isEqualTo(expected)
    }

    // Scenario 8b: INSERT INTO column list breaks per line
    @Test
    fun format_insertColumnList_breaksPerLine() {
        val input = "INSERT INTO users (id, name, email) VALUES (1, 'a', 'b')"

        val expected = """
            INSERT INTO users
            (
              id,
              name,
              email
            )
            VALUES
            (
              1,
              'a',
              'b'
            )
        """.trimIndent()

        val result = SqlFormatter.format(input)

        assertThat(result).isEqualTo(expected)
    }

    // Scenario 8c: VALUES tuple breaks per line (same input/pattern as 8b — the "VALUES" side of it)
    @Test
    fun format_valuesTuple_breaksPerLine() {
        val input = "INSERT INTO users (id, name, email) VALUES (1, 'a', 'b')"

        val expected = """
            INSERT INTO users
            (
              id,
              name,
              email
            )
            VALUES
            (
              1,
              'a',
              'b'
            )
        """.trimIndent()

        val result = SqlFormatter.format(input)

        assertThat(result).isEqualTo(expected)
    }

    // Scenario 7b: Single-item parenthesized list still breaks (no count==1 special case)
    @Test
    fun format_singleItemList_stillBreaks() {
        val input = "INSERT INTO t (id) VALUES (1);"

        val expected = """
            INSERT INTO t
            (
              id
            )
            VALUES
            (
              1
            );
        """.trimIndent()

        val result = SqlFormatter.format(input)

        assertThat(result).isEqualTo(expected)
    }

    // Scenario 8f: Nested parens beyond depth 1 left flat (explicit non-goal). `MAX` is a
    // recognized keyword (SqlKeywords.KEYWORDS) so it uppercases even inside the flat subquery.
    @Test
    fun format_deepNesting_leftFlatDepth1Only() {
        val input = "INSERT INTO t (id) VALUES ((SELECT max(x) FROM y))"

        val expected = """
            INSERT INTO t
            (
              id
            )
            VALUES
            (
              (SELECT MAX(x) FROM y)
            )
        """.trimIndent()

        val result = SqlFormatter.format(input)

        assertThat(result).isEqualTo(expected)
    }

    // Scenario 8 companion: Idempotent formatting across multi-statement + list-breaking inputs
    @Test
    fun format_isIdempotent_onMultiStatementAndBrokenLists() {
        val testCases = listOf(
            "INSERT INTO `glo_service` (`gsv_id`,`gsv_tipo_modulo`,`gsv_almacen_siap`) " +
                "VALUES (2033, 'CRM', 'CRM');SELECT `gsv_id`,`gsv_tipo_modulo`, `gsv_almacen_siap` " +
                "FROM glo_service WHERE a > b;",
            "INSERT INTO t (id) VALUES (1);",
            "INSERT INTO t (id) VALUES ((SELECT max(x) FROM y))",
            "SELECT a, b, c FROM t WHERE a > 1"
        )

        for (input in testCases) {
            val formattedOnce = SqlFormatter.format(input)
            val formattedTwice = SqlFormatter.format(formattedOnce)

            assertThat(formattedTwice).isEqualTo(formattedOnce)
        }
    }
}
