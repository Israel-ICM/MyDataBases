package com.sphynxs.mydatabases.ui.screens.queryeditor.domain

import androidx.compose.ui.text.TextRange

/**
 * Immutable snapshot of editor state for undo/redo history.
 *
 * Captures text, selection, and multi-cursor positions atomically.
 * Used by EditorHistory to enable undo/redo operations.
 *
 * Spec: openspec/changes/editor-shortcuts-and-history/specs/editor-shortcuts/spec.md
 * Design: Pure data class, no Android dependencies (JVM-testable).
 *
 * @param text Current SQL text content
 * @param selection Current text selection range
 * @param cursorPositions Positions of all active cursors (empty if single cursor)
 *
 * @author israel-icm
 * @date 2026-06-24
 */
data class EditorSnapshot(
    val text: String,
    val selection: TextRange,
    val cursorPositions: List<Int>
)
