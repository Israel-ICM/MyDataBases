package com.sphynxs.mydatabases.ui.screens.queryeditor.domain

/**
 * Sealed interface representing keyboard shortcut actions in the SQL editor.
 *
 * Mapped from KeyEvent by EditorShortcuts and dispatched by QueryEditorScreen.
 * Pure domain type, no Android framework dependencies (JVM-testable).
 *
 * Spec: openspec/changes/editor-shortcuts-and-history/specs/editor-shortcuts/spec.md
 * Design: Exhaustive when branches enforced by sealed type.
 *
 * Actions:
 * - Run: Execute SQL query (Ctrl+Enter)
 * - Save: Save query to file (Ctrl+S)
 * - Undo: Revert to previous editor state (Ctrl+Z)
 * - Redo: Reapply undone change (Ctrl+Y / Ctrl+Shift+Z)
 * - Format: Format SQL text (Ctrl+Shift+F)
 *
 * @author israel-icm
 * @date 2026-06-24
 */
sealed interface ShortcutAction {
    data object Run : ShortcutAction
    data object Save : ShortcutAction
    data object Undo : ShortcutAction
    data object Redo : ShortcutAction
    data object Format : ShortcutAction
}
