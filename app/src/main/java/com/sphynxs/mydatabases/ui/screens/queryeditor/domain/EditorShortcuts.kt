package com.sphynxs.mydatabases.ui.screens.queryeditor.domain

import androidx.compose.ui.input.key.Key

/**
 * Pure mapper from KeyEvent to ShortcutAction.
 *
 * Maps keyboard shortcuts to editor actions. Returns null for non-shortcuts
 * to allow normal key propagation (e.g., Tab, Backspace, typing).
 *
 * Spec: openspec/changes/editor-shortcuts-and-history/specs/editor-shortcuts/spec.md
 * Design: Pure function, no side effects, JVM-testable.
 *
 * Shortcut mappings:
 * - Ctrl+Enter → Run (execute SQL)
 * - Ctrl+S → Save (save query to file)
 * - Ctrl+Z → Undo (revert to previous state)
 * - Ctrl+Y → Redo (reapply undone change)
 * - Ctrl+Shift+Z → Redo (alternative binding)
 * - Ctrl+Shift+F → Format (format SQL text)
 * - Ctrl+Space → TriggerCompletion (show completion popup)
 *
 * @author israel-icm
 * @date 2026-06-24
 */
object EditorShortcuts {
    /**
     * Maps a key event to a shortcut action.
     *
     * @param key The key that was pressed
     * @param isCtrlPressed True if Ctrl key is held down
     * @param isShiftPressed True if Shift key is held down
     * @return The mapped ShortcutAction, or null if not a recognized shortcut
     */
    fun mapKeyEvent(
        key: Key,
        isCtrlPressed: Boolean,
        isShiftPressed: Boolean
    ): ShortcutAction? = when {
        isCtrlPressed && !isShiftPressed && key == Key.Enter -> ShortcutAction.Run
        isCtrlPressed && !isShiftPressed && key == Key.S -> ShortcutAction.Save
        isCtrlPressed && !isShiftPressed && key == Key.Z -> ShortcutAction.Undo
        isCtrlPressed && !isShiftPressed && key == Key.Y -> ShortcutAction.Redo
        isCtrlPressed && isShiftPressed && key == Key.Z -> ShortcutAction.Redo
        isCtrlPressed && isShiftPressed && key == Key.F -> ShortcutAction.Format
        isCtrlPressed && !isShiftPressed && key == Key.Spacebar -> ShortcutAction.TriggerCompletion
        else -> null
    }
}
