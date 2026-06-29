package com.sphynxs.mydatabases.domain.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key

/**
 * Pure mapper for keyboard shortcuts.
 *
 * Maps KeyEvent to ShortcutAction for the SQL editor.
 */
object EditorShortcuts {
    /**
     * Map a keyboard event to a shortcut action.
     *
     * @param event KeyEvent to evaluate
     * @return ShortcutAction if matches a known shortcut, null otherwise
     */
    fun mapKeyEvent(event: KeyEvent): ShortcutAction? {
        if (!event.isCtrlPressed) return null
        
        return when (event.key) {
            Key.Enter -> ShortcutAction.Run
            Key.S -> ShortcutAction.Save
            Key.Z -> if (event.isShiftPressed) ShortcutAction.Redo else ShortcutAction.Undo
            Key.Y -> ShortcutAction.Redo
            Key.F -> if (event.isShiftPressed) ShortcutAction.Format else ShortcutAction.Find
            Key.H -> ShortcutAction.Replace
            Key.Spacebar -> ShortcutAction.TriggerCompletion
            Key.Backslash -> if (event.isShiftPressed) ShortcutAction.JumpToMatchingBracket else null
            else -> null
        }
    }
}
