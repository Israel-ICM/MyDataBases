package com.sphynxs.mydatabases.domain.editor

import androidx.compose.ui.text.TextRange
import com.sphynxs.mydatabases.ui.screens.queryeditor.components.SqlToken
import com.sphynxs.mydatabases.ui.screens.queryeditor.components.TokenKind

/**
 * Pure JVM logic for multi-cursor operations (Phase 6.2 - PR #6).
 * 
 * MC-1: Ctrl+Alt+Down/Up add cursors on consecutive lines
 * MC-3: Ctrl+D finds next occurrence
 * MC-4: Ctrl+D selects word on first press
 */
object MultiCursorEngine {
    
    /**
     * Add a cursor on the line below, preserving column (MC-1.1).
     * 
     * @param layout Text layout result (for line calculations)
     * @param primarySelection Current primary cursor selection
     * @param targetColumn Column to preserve (0-indexed from line start)
     * @return New cursor position, or null if already at last line (MC-1.3)
     */
    fun <T : Any> addCursorBelow(
        layout: T,
        primarySelection: TextRange,
        targetColumn: Int
    ): TextRange? {
        // Extract methods via reflection to avoid Compose UI dependency in tests
        val getLineForOffset = layout.javaClass.getMethod("getLineForOffset", Int::class.java)
        val getLineStart = layout.javaClass.getMethod("getLineStart", Int::class.java)
        val getLineEnd = layout.javaClass.getMethod("getLineEnd", Int::class.java)
        val lineCountField = layout.javaClass.getMethod("getLineCount")
        
        val currentLine = getLineForOffset.invoke(layout, primarySelection.start) as Int
        val lineCount = lineCountField.invoke(layout) as Int
        
        // MC-1.3: No-op at last line
        if (currentLine == lineCount - 1) {
            return null
        }
        
        val nextLine = currentLine + 1
        val lineStart = getLineStart.invoke(layout, nextLine) as Int
        val lineEnd = getLineEnd.invoke(layout, nextLine) as Int
        
        // MC-1.2: Clamp to EOL
        val targetOffset = lineStart + targetColumn
        val clampedOffset = minOf(targetOffset, lineEnd)
        
        return TextRange(clampedOffset)
    }
    
    /**
     * Add a cursor on the line above, preserving column (MC-2.1).
     */
    fun <T : Any> addCursorAbove(
        layout: T,
        primarySelection: TextRange,
        targetColumn: Int
    ): TextRange? {
        val getLineForOffset = layout.javaClass.getMethod("getLineForOffset", Int::class.java)
        val getLineStart = layout.javaClass.getMethod("getLineStart", Int::class.java)
        val getLineEnd = layout.javaClass.getMethod("getLineEnd", Int::class.java)
        
        val currentLine = getLineForOffset.invoke(layout, primarySelection.start) as Int
        
        // No-op at first line
        if (currentLine == 0) {
            return null
        }
        
        val prevLine = currentLine - 1
        val lineStart = getLineStart.invoke(layout, prevLine) as Int
        val lineEnd = getLineEnd.invoke(layout, prevLine) as Int
        
        val targetOffset = lineStart + targetColumn
        val clampedOffset = minOf(targetOffset, lineEnd)
        
        return TextRange(clampedOffset)
    }
    
    /**
     * Find next occurrence of selected text (MC-3.1).
     * Case-sensitive search.
     * 
     * @return Range of next occurrence, or null if no more matches (MC-5.1)
     */
    fun findNextOccurrence(
        text: String,
        selectedText: String,
        fromOffset: Int
    ): TextRange? {
        if (selectedText.isEmpty()) return null
        
        val index = text.indexOf(selectedText, fromOffset)
        if (index == -1) return null
        
        return TextRange(index, index + selectedText.length)
    }
    
    /**
     * Select word at cursor offset (MC-4.1).
     * Uses tokens to find identifier boundaries.
     * 
     * @return Range of word, or collapsed range if not inside identifier
     */
    fun selectWordAtOffset(
        text: String,
        offset: Int,
        tokens: List<SqlToken>
    ): TextRange {
        val token = getTokenAtOffset(tokens, offset)
        
        return if (token != null && token.kind == TokenKind.IDENTIFIER) {
            TextRange(token.range.first, token.range.last + 1)
        } else {
            TextRange(offset)
        }
    }
    
    /**
     * Helper: Find token at offset.
     */
    private fun getTokenAtOffset(tokens: List<SqlToken>, offset: Int): SqlToken? {
        return tokens.firstOrNull { token ->
            offset in token.range
        }
    }
}
