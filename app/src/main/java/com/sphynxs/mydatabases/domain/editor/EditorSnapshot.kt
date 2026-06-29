package com.sphynxs.mydatabases.domain.editor

import androidx.compose.ui.text.TextRange

/**
 * Immutable snapshot of editor state for undo/redo.
 *
 * @property text Current text content
 * @property selection Current cursor selection
 * @property cursorPositions Multi-cursor positions (backward compat, derived from cursorSelections)
 * @property cursorSelections Multi-cursor selections with ranges (MC-8, Phase 6.1)
 * @property timestamp When snapshot was created (for coalescing)
 */
data class EditorSnapshot(
    val text: String,
    val selection: TextRange,
    val cursorPositions: List<Int>,
    val cursorSelections: List<TextRange>? = null,
    val timestamp: Long = System.currentTimeMillis()
)
