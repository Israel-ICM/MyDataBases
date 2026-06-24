package com.sphynxs.mydatabases.domain.completion

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for SqlCompletionProvider.
 *
 * TDD: RED → GREEN → TRIANGULATE
 * Spec: openspec/changes/editor-completion-and-format/spec.md (scenarios 13-28)
 *
 * @author israel-icm
 * @date 2026-06-24
 */
class SqlCompletionProviderTest {

    // Scenario 13: Show keyword suggestions after 2+ chars
    @Test
    fun getSuggestions_twoCharPrefix_returnsKeywordMatches() {
        // GIVEN
        val prefix = "SEL"

        // WHEN
        val suggestions = SqlCompletionProvider.getSuggestions(prefix)

        // THEN
        assertThat(suggestions).isNotEmpty()
        assertThat(suggestions.map { it.text }).contains("SELECT")
        assertThat(suggestions.first().kind).isEqualTo(CompletionKind.KEYWORD)
    }

    // Scenario 14: No auto-popup for 1 char (caller responsibility, but provider should work)
    @Test
    fun getSuggestions_oneCharPrefix_returnsMatches() {
        // GIVEN
        val prefix = "S"

        // WHEN
        val suggestions = SqlCompletionProvider.getSuggestions(prefix)

        // THEN
        assertThat(suggestions).isNotEmpty()
        assertThat(suggestions.map { it.text }).containsAtLeast("SELECT", "SET", "SHOW")
    }

    // Scenario 15: Blank prefix returns empty
    @Test
    fun getSuggestions_blankPrefix_returnsEmpty() {
        // GIVEN
        val prefix = ""

        // WHEN
        val suggestions = SqlCompletionProvider.getSuggestions(prefix)

        // THEN
        assertThat(suggestions).isEmpty()
    }

    // Scenario 17: Context ranking - after FROM → tables first
    @Test
    fun getSuggestions_afterFrom_rankTablesFirst() {
        // GIVEN
        val prefix = "u"
        val context = "SELECT * FROM "
        val schema = SchemaSnapshot(
            databaseName = "test_db",
            tables = listOf("users", "orders"),
            columns = emptyMap()
        )

        // WHEN
        val suggestions = SqlCompletionProvider.getSuggestions(prefix, context, schema)

        // THEN
        assertThat(suggestions).isNotEmpty()
        val topSuggestion = suggestions.first()
        assertThat(topSuggestion.text).isEqualTo("users")
        assertThat(topSuggestion.kind).isEqualTo(CompletionKind.TABLE)
        assertThat(topSuggestion.score).isEqualTo(100.0)
    }

    // Scenario 18: Context ranking - after SELECT → columns first
    @Test
    fun getSuggestions_afterSelect_rankColumnsFirst() {
        // GIVEN
        val prefix = "i"
        val context = "SELECT "
        val schema = SchemaSnapshot(
            databaseName = "test_db",
            tables = listOf("users"),
            columns = mapOf(
                "users" to listOf(
                    ColumnInfo("id", "INT"),
                    ColumnInfo("name", "VARCHAR")
                )
            )
        )

        // WHEN
        val suggestions = SqlCompletionProvider.getSuggestions(prefix, context, schema)

        // THEN
        assertThat(suggestions).isNotEmpty()
        val topSuggestion = suggestions.first()
        assertThat(topSuggestion.text).isEqualTo("id")
        assertThat(topSuggestion.kind).isEqualTo(CompletionKind.COLUMN)
        assertThat(topSuggestion.score).isEqualTo(100.0)
    }

    // Scenario 19: Schema available → show tables + columns + keywords
    @Test
    fun getSuggestions_schemaAvailable_showsAllKinds() {
        // GIVEN
        val prefix = "u"
        val schema = SchemaSnapshot(
            databaseName = "test_db",
            tables = listOf("users", "orders"),
            columns = mapOf(
                "users" to listOf(ColumnInfo("user_id", "INT"))
            )
        )

        // WHEN
        val suggestions = SqlCompletionProvider.getSuggestions(prefix, "", schema)

        // THEN
        assertThat(suggestions).isNotEmpty()
        val kinds = suggestions.map { it.kind }.toSet()
        assertThat(kinds).containsAtLeast(CompletionKind.KEYWORD, CompletionKind.TABLE)
        // Keywords: UPDATE, UNION, USE
        // Tables: users
    }

    // Scenario 20: Schema unavailable → show keywords only
    @Test
    fun getSuggestions_schemaNull_showsKeywordsOnly() {
        // GIVEN
        val prefix = "SEL"

        // WHEN
        val suggestions = SqlCompletionProvider.getSuggestions(prefix, "", null)

        // THEN
        assertThat(suggestions).isNotEmpty()
        assertThat(suggestions.map { it.kind }).containsExactly(CompletionKind.KEYWORD)
    }

    // Scenario 21: Column suggestions show type
    @Test
    fun getSuggestions_columnMatch_includesType() {
        // GIVEN
        val prefix = "id"
        val schema = SchemaSnapshot(
            databaseName = "test_db",
            tables = listOf("users"),
            columns = mapOf(
                "users" to listOf(ColumnInfo("id", "INT"))
            )
        )

        // WHEN
        val suggestions = SqlCompletionProvider.getSuggestions(prefix, "", schema)

        // THEN
        val columnSuggestion = suggestions.find { it.kind == CompletionKind.COLUMN }
        assertThat(columnSuggestion).isNotNull()
        assertThat(columnSuggestion?.detail).isEqualTo("INT")
    }

    // Scenario: Case-insensitive matching
    @Test
    fun getSuggestions_mixedCasePrefix_matchesCaseInsensitive() {
        // GIVEN
        val prefix = "SeLeCt"

        // WHEN
        val suggestions = SqlCompletionProvider.getSuggestions(prefix)

        // THEN
        assertThat(suggestions.map { it.text }).contains("SELECT")
    }

    // Scenario: Limit to top 20 suggestions
    @Test
    fun getSuggestions_manyMatches_limitsTo20() {
        // GIVEN
        val prefix = "s" // Matches many keywords
        val largeTables = (1..50).map { "schema_table_$it" }
        val schema = SchemaSnapshot(
            databaseName = "test_db",
            tables = largeTables,
            columns = emptyMap()
        )

        // WHEN
        val suggestions = SqlCompletionProvider.getSuggestions(prefix, "", schema)

        // THEN
        assertThat(suggestions.size).isAtMost(20)
    }

    // Scenario: Alphabetical sorting within same score
    @Test
    fun getSuggestions_sameScore_sortsAlphabetically() {
        // GIVEN
        val prefix = "se"

        // WHEN
        val suggestions = SqlCompletionProvider.getSuggestions(prefix)

        // THEN
        val keywordSuggestions = suggestions.filter { it.kind == CompletionKind.KEYWORD }
        assertThat(keywordSuggestions.map { it.text }).isInOrder()
    }

    // Scenario: After JOIN → rank tables first
    @Test
    fun getSuggestions_afterJoin_rankTablesFirst() {
        // GIVEN
        val prefix = "o"
        val context = "SELECT * FROM users JOIN "
        val schema = SchemaSnapshot(
            databaseName = "test_db",
            tables = listOf("orders", "products"),
            columns = emptyMap()
        )

        // WHEN
        val suggestions = SqlCompletionProvider.getSuggestions(prefix, context, schema)

        // THEN
        val topSuggestion = suggestions.first()
        assertThat(topSuggestion.text).isEqualTo("orders")
        assertThat(topSuggestion.kind).isEqualTo(CompletionKind.TABLE)
    }

    // Scenario: After WHERE → rank columns first
    @Test
    fun getSuggestions_afterWhere_rankColumnsFirst() {
        // GIVEN
        val prefix = "n"
        val context = "SELECT * FROM users WHERE "
        val schema = SchemaSnapshot(
            databaseName = "test_db",
            tables = listOf("users"),
            columns = mapOf(
                "users" to listOf(
                    ColumnInfo("name", "VARCHAR"),
                    ColumnInfo("notes", "TEXT")
                )
            )
        )

        // WHEN
        val suggestions = SqlCompletionProvider.getSuggestions(prefix, context, schema)

        // THEN
        val topTwo = suggestions.take(2)
        assertThat(topTwo.map { it.kind }).containsExactly(CompletionKind.COLUMN, CompletionKind.COLUMN)
        assertThat(topTwo.map { it.text }).containsExactly("name", "notes")
    }

    // Scenario: After comma → rank columns first
    @Test
    fun getSuggestions_afterComma_rankColumnsFirst() {
        // GIVEN
        val prefix = "e"
        val context = "SELECT id, "
        val schema = SchemaSnapshot(
            databaseName = "test_db",
            tables = listOf("users"),
            columns = mapOf(
                "users" to listOf(ColumnInfo("email", "VARCHAR"))
            )
        )

        // WHEN
        val suggestions = SqlCompletionProvider.getSuggestions(prefix, context, schema)

        // THEN
        val topSuggestion = suggestions.first()
        assertThat(topSuggestion.text).isEqualTo("email")
        assertThat(topSuggestion.kind).isEqualTo(CompletionKind.COLUMN)
    }
}
