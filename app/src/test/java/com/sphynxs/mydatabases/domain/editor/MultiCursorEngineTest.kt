package com.sphynxs.mydatabases.domain.editor

import androidx.compose.ui.text.TextRange
import com.sphynxs.mydatabases.ui.screens.queryeditor.components.SqlToken
import com.sphynxs.mydatabases.ui.screens.queryeditor.components.TokenKind
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for MultiCursorEngine (Phase 6.2 - PR #6).
 * Pure JVM domain logic for multi-cursor operations.
 */
class MultiCursorEngineTest {
    
    // Phase 6.2.1 — TDD RED: addCursorBelow with column preservation
    @Test
    fun `addCursorBelow preserves column`() {
        val text = "SELECT *\nFROM users\nWHERE id = 1"
        val layout = FakeTextLayoutResult(text)
        val primarySelection = TextRange(4) // Line 0, col 4 ("ECT")
        
        val result = MultiCursorEngine.addCursorBelow(layout, primarySelection, targetColumn = 4)
        
        assertNotNull(result)
        // Line 1 start = 9 ("FROM users"), col 4 = offset 13 ("M")
        assertEquals(TextRange(13), result)
    }
    
    // Phase 6.2.3 — TDD RED: addCursorBelow clamps to EOL
    @Test
    fun `addCursorBelow clamps to EOL when target column exceeds line length`() {
        val text = "SELECT *\nFROM\nWHERE id = 1"
        val layout = FakeTextLayoutResult(text)
        val primarySelection = TextRange(6) // Line 0, col 6 (after *)
        
        val result = MultiCursorEngine.addCursorBelow(layout, primarySelection, targetColumn = 6)
        
        assertNotNull(result)
        // Line 1 = "FROM" (4 chars), should clamp to col 4 (offset 13)
        val line1Start = 9
        val line1End = 13
        assertEquals(TextRange(line1End), result)
    }
    
    // Phase 6.2.5 — TDD RED: addCursorBelow returns null at last line
    @Test
    fun `addCursorBelow returns null at last line`() {
        val text = "SELECT *\nFROM users"
        val layout = FakeTextLayoutResult(text)
        val primarySelection = TextRange(10) // Line 1 (last line)
        
        val result = MultiCursorEngine.addCursorBelow(layout, primarySelection, targetColumn = 0)
        
        assertNull(result)
    }
    
    // Phase 6.2.7 — TDD RED: addCursorAbove preserves column
    @Test
    fun `addCursorAbove preserves column`() {
        val text = "SELECT *\nFROM users\nWHERE id = 1"
        val layout = FakeTextLayoutResult(text)
        val primarySelection = TextRange(13) // Line 1, col 4 ("M")
        
        val result = MultiCursorEngine.addCursorAbove(layout, primarySelection, targetColumn = 4)
        
        assertNotNull(result)
        // Line 0 start = 0, col 4 = offset 4 ("ECT")
        assertEquals(TextRange(4), result)
    }
    
    // Phase 6.2.9 — TDD RED: findNextOccurrence case-sensitive
    @Test
    fun `findNextOccurrence case-sensitive finds next match`() {
        val text = "user_id WHERE user_id = 1 AND user_name = 'test'"
        val selectedText = "user_id"
        
        val result = MultiCursorEngine.findNextOccurrence(text, selectedText, fromOffset = 1)
        
        assertNotNull(result)
        assertEquals(TextRange(14, 21), result) // Second occurrence
    }
    
    // Phase 6.2.11 — TDD RED: findNextOccurrence returns null when exhausted
    @Test
    fun `findNextOccurrence returns null when no more matches`() {
        val text = "user_id WHERE user_id = 1"
        val selectedText = "user_id"
        
        val result = MultiCursorEngine.findNextOccurrence(text, selectedText, fromOffset = 22)
        
        assertNull(result)
    }
    
    // Phase 6.2.13 — TDD RED: selectWordAtOffset expands to identifier boundaries
    @Test
    fun `selectWordAtOffset expands to identifier boundaries when inside word`() {
        val text = "SELECT user_id FROM users"
        val tokens = listOf(
            SqlToken(range = 0..5, kind = TokenKind.KEYWORD), // "SELECT"
            SqlToken(range = 7..13, kind = TokenKind.IDENTIFIER), // "user_id"
            SqlToken(range = 15..18, kind = TokenKind.KEYWORD), // "FROM"
            SqlToken(range = 20..24, kind = TokenKind.IDENTIFIER) // "users"
        )
        
        val result = MultiCursorEngine.selectWordAtOffset(text, offset = 10, tokens) // Inside "user_id"
        
        assertEquals(TextRange(7, 14), result) // Full "user_id" range (range.last + 1)
    }
    
    @Test
    fun `selectWordAtOffset returns collapsed range when not inside identifier`() {
        val text = "SELECT * FROM users"
        val tokens = listOf(
            SqlToken(range = 0..5, kind = TokenKind.KEYWORD), // "SELECT"
            SqlToken(range = 7..7, kind = TokenKind.OPERATOR) // "*"
        )
        
        val result = MultiCursorEngine.selectWordAtOffset(text, offset = 7, tokens) // On "*"
        
        assertEquals(TextRange(7), result) // Collapsed
    }
    
    // Fake layout for testing (no Compose UI)
    private class FakeTextLayoutResult(private val text: String) {
        fun getLineForOffset(offset: Int): Int {
            return text.substring(0, offset).count { it == '\n' }
        }
        
        fun getLineStart(line: Int): Int {
            var currentLine = 0
            var offset = 0
            while (currentLine < line && offset < text.length) {
                if (text[offset] == '\n') currentLine++
                offset++
            }
            return offset
        }
        
        fun getLineEnd(line: Int): Int {
            val start = getLineStart(line)
            var end = start
            while (end < text.length && text[end] != '\n') {
                end++
            }
            return end
        }
        
        val lineCount: Int
            get() = text.count { it == '\n' } + 1
    }
}
