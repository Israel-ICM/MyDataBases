package com.sphynxs.mydatabases.domain.editor

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for SqlKeywords — single source of truth for SQL keywords.
 *
 * TDD RED-first workflow:
 * 1. Write failing test
 * 2. Implement minimum code to pass
 * 3. Refactor
 *
 * Spec: openspec/changes/editor-completion-and-format/spec.md
 * Design: ADR 1 — SqlKeywords as single source of truth
 *
 * @author israel-icm
 * @date 2026-06-24
 */
class SqlKeywordsTest {

    @Test
    fun keywords_notEmpty() {
        // RED: This will FAIL until SqlKeywords.KEYWORDS is created
        assertThat(SqlKeywords.KEYWORDS).isNotEmpty()
    }

    @Test
    fun keywords_containsCanonicalSet() {
        // RED: Validates presence of core SQL keywords
        val requiredKeywords = setOf(
            "SELECT", "FROM", "WHERE", "JOIN", 
            "INSERT", "UPDATE", "DELETE"
        )
        assertThat(SqlKeywords.KEYWORDS).containsAtLeastElementsIn(requiredKeywords)
    }

    @Test
    fun keywords_allUppercase() {
        // RED: Ensures all keywords are normalized to UPPERCASE
        val allUppercase = SqlKeywords.KEYWORDS.all { keyword ->
            keyword == keyword.uppercase()
        }
        assertThat(allUppercase).isTrue()
    }
}
