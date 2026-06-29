package com.sphynxs.mydatabases.domain.editor

import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for FindReplaceEngine (FR-1 through FR-15).
 *
 * Phase 5.1 — TDD tests for domain logic.
 *
 * @author israel-icm (SDD apply phase)
 * @date 2026-06-29
 */
class FindReplaceEngineTest {

    @Test
    fun `findAllMatches returns all occurrences case-insensitive`() {
        // FR-6: Match case OFF by default
        val text = "SELECT user_id FROM users WHERE user_id > 10"
        val matches = FindReplaceEngine.findAllMatches(text, "user_id", matchCase = false)
        
        assertThat(matches).hasSize(2)
        assertThat(matches[0]).isEqualTo(TextRange(7, 14))  // First user_id
        assertThat(matches[1]).isEqualTo(TextRange(32, 39)) // Second user_id
    }

    @Test
    fun `findAllMatches respects match case when enabled`() {
        // FR-6: Match case ON
        val text = "SELECT User_ID FROM users WHERE user_id > 10"
        val matches = FindReplaceEngine.findAllMatches(text, "user_id", matchCase = true)
        
        assertThat(matches).hasSize(1)
        assertThat(matches[0]).isEqualTo(TextRange(32, 39)) // Only lowercase match
    }

    @Test
    fun `findAllMatches whole word mode excludes partial matches`() {
        // FR-7: Whole word ON
        val text = "SELECT user_id, user_id_old FROM users"
        val matches = FindReplaceEngine.findAllMatches(text, "user_id", wholeWord = true)
        
        assertThat(matches).hasSize(1)
        assertThat(matches[0]).isEqualTo(TextRange(7, 14)) // Only whole word match, not user_id_old
    }

    @Test
    fun `findAllMatches regex mode supports patterns`() {
        // FR-8: Regex mode
        val text = "SELECT id1, id2, id3 FROM table"
        val matches = FindReplaceEngine.findAllMatches(text, "id\\d+", useRegex = true)
        
        assertThat(matches).hasSize(3)
    }

    @Test
    fun `findAllMatches returns empty on invalid regex`() {
        // FR-8: Invalid regex should not crash
        val text = "SELECT * FROM users"
        val matches = FindReplaceEngine.findAllMatches(text, "[invalid(", useRegex = true)
        
        assertThat(matches).isEmpty()
    }

    @Test
    fun `findNext wraps to beginning when no match after cursor`() {
        // FR-4: Navigate wraps
        val text = "SELECT user_id FROM users WHERE user_id > 10"
        val secondMatch = TextRange(32, 39)
        
        // Search from after second match (offset 40)
        val nextMatch = FindReplaceEngine.findNext(text, "user_id", fromOffset = 40, wrap = true)
        
        // Should wrap to first match
        assertThat(nextMatch).isEqualTo(TextRange(7, 14))
    }

    @Test
    fun `findNext stops at last match when wrap disabled`() {
        // FR-4: No wrap behavior
        val text = "SELECT user_id FROM users WHERE user_id > 10"
        
        val nextMatch = FindReplaceEngine.findNext(text, "user_id", fromOffset = 40, wrap = false)
        
        assertThat(nextMatch).isNull()
    }

    @Test
    fun `findPrevious wraps to end when no match before cursor`() {
        // FR-4: Shift+Enter wraps backwards
        val text = "SELECT user_id FROM users WHERE user_id > 10"
        
        // Search from before first match (offset 5)
        val prevMatch = FindReplaceEngine.findPrevious(text, "user_id", fromOffset = 5, wrap = true)
        
        // Should wrap to last match
        assertThat(prevMatch).isEqualTo(TextRange(32, 39))
    }

    @Test
    fun `replaceOne replaces single match correctly`() {
        // FR-10: Replace one
        val text = "SELECT user_id FROM users"
        val matchRange = TextRange(7, 14)
        
        val result = FindReplaceEngine.replaceOne(text, matchRange, "customer_id")
        
        assertThat(result).isEqualTo("SELECT customer_id FROM users")
    }

    @Test
    fun `replaceAll replaces all occurrences atomically`() {
        // FR-11: Replace all in single operation
        val text = "SELECT user_id FROM users WHERE user_id > 10"
        
        val result = FindReplaceEngine.replaceAll(text, "user_id", "customer_id")
        
        assertThat(result).isEqualTo("SELECT customer_id FROM users WHERE customer_id > 10")
    }

    @Test
    fun `replaceAll respects match case and whole word`() {
        // FR-12: Replace respects toggles
        val text = "SELECT User_ID, user_id_old FROM users"
        
        val result = FindReplaceEngine.replaceAll(
            text, 
            "user_id", 
            "customer_id",
            matchCase = true,
            wholeWord = false
        )
        
        // Only lowercase "user_id" should be replaced (case-sensitive)
        assertThat(result).isEqualTo("SELECT User_ID, customer_id_old FROM users")
    }

    @Test
    fun `empty query returns no matches`() {
        val text = "SELECT * FROM users"
        val matches = FindReplaceEngine.findAllMatches(text, "")
        
        assertThat(matches).isEmpty()
    }
}
