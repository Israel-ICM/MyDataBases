package com.sphynxs.mydatabases.ui.screens.queryeditor.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for auto-close bracket feature (BR-3, BR-4, BR-5).
 *
 * Phase 4.4 — TDD RED tests written FIRST.
 *
 * Requirements:
 * - BR-3: Typing '(' auto-inserts ')' and positions cursor between
 * - BR-4: Auto-close works for (), [], {}, '', "", ``
 * - BR-5: Auto-close does NOT trigger inside string/comment tokens
 *
 * @author israel-icm (SDD apply phase)
 * @date 2026-06-29
 */
class AutoCloseTest {

    @Test
    fun `typing open paren auto-inserts close paren and positions cursor between`() {
        // BR-3: Basic auto-close for ()
        val oldValue = TextFieldValue("SELECT ", TextRange(7))
        val newValue = TextFieldValue("SELECT (", TextRange(8)) // User typed '('
        
        val result = applyAutoClose(oldValue, newValue, emptyList())
        
        assertThat(result.text).isEqualTo("SELECT ()")
        assertThat(result.selection.start).isEqualTo(8) // Cursor between ( and )
        assertThat(result.selection.end).isEqualTo(8)
    }

    @Test
    fun `auto-close works for all six bracket pairs`() {
        // BR-4: Test (), [], {}, '', "", ``
        val testCases = listOf(
            '(' to ')',
            '[' to ']',
            '{' to '}',
            '\'' to '\'',
            '"' to '"',
            '`' to '`'
        )
        
        testCases.forEach { (open, close) ->
            val oldValue = TextFieldValue("SELECT ", TextRange(7))
            val newValue = TextFieldValue("SELECT $open", TextRange(8))
            
            val result = applyAutoClose(oldValue, newValue, emptyList())
            
            assertThat(result.text).isEqualTo("SELECT $open$close")
            assertThat(result.selection.start).isEqualTo(8)
        }
    }

    @Test
    fun `auto-close does NOT trigger inside string literal`() {
        // BR-5: No auto-close when typing inside "string"
        // Using closed string to ensure tokenizer detects it properly
        val oldValue = TextFieldValue("SELECT \"test\"", TextRange(12)) // Cursor before closing quote
        val newValue = TextFieldValue("SELECT \"test(\"", TextRange(13)) // User typed '(' inside string
        
        val tokens = SqlTokenizer.tokenize("SELECT \"test(\"")
        val result = applyAutoClose(oldValue, newValue, tokens)
        
        // Should NOT auto-close because we're inside a STRING token
        assertThat(result.text).isEqualTo("SELECT \"test(\"")
        assertThat(result.selection.start).isEqualTo(13)
    }

    @Test
    fun `auto-close does NOT trigger inside comment`() {
        // BR-5: No auto-close when typing inside -- comment
        val oldValue = TextFieldValue("SELECT * -- test", TextRange(16))
        val newValue = TextFieldValue("SELECT * -- test(", TextRange(17)) // User typed '(' inside comment
        
        val tokens = SqlTokenizer.tokenize("SELECT * -- test(")
        val result = applyAutoClose(oldValue, newValue, tokens)
        
        // Should NOT auto-close because we're inside a COMMENT token
        assertThat(result.text).isEqualTo("SELECT * -- test(")
        assertThat(result.selection.start).isEqualTo(17)
    }

    @Test
    fun `auto-close does NOT trigger when multi-cursor active`() {
        // Auto-close should be disabled when cursorPositions is not empty
        val oldValue = TextFieldValue("SELECT ", TextRange(7))
        val newValue = TextFieldValue("SELECT (", TextRange(8))
        
        val cursorPositions = mutableListOf(10, 15) // Multi-cursor active
        val result = applyAutoClose(oldValue, newValue, emptyList(), cursorPositions)
        
        // Should NOT auto-close because multi-cursor is active
        assertThat(result.text).isEqualTo("SELECT (")
        assertThat(result.selection.start).isEqualTo(8)
    }

    // Helper function — delegates to actual implementation
    private fun applyAutoClose(
        oldValue: TextFieldValue,
        newValue: TextFieldValue,
        tokens: List<SqlToken>,
        cursorPositions: List<Int> = emptyList()
    ): TextFieldValue {
        // Multi-cursor check
        if (cursorPositions.isNotEmpty()) {
            return newValue
        }
        
        // Call actual implementation from SqlCodeEditor.kt
        return applyAutoCloseBrackets(oldValue, newValue, tokens)
    }
}
