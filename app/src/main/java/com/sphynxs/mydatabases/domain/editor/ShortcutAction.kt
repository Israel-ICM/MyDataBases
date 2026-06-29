package com.sphynxs.mydatabases.domain.editor

/**
 * Keyboard shortcut actions supported by the editor.
 */
sealed interface ShortcutAction {
    /** Execute query (Ctrl+Enter) */
    data object Run : ShortcutAction
    
    /** Save file (Ctrl+S) */
    data object Save : ShortcutAction
    
    /** Undo last change (Ctrl+Z) */
    data object Undo : ShortcutAction
    
    /** Redo last undone change (Ctrl+Y or Ctrl+Shift+Z) */
    data object Redo : ShortcutAction
    
    /** Format SQL (Ctrl+Shift+F) */
    data object Format : ShortcutAction
    
    /** Trigger code completion (Ctrl+Space) */
    data object TriggerCompletion : ShortcutAction
    
    /** Jump to matching bracket (Ctrl+Shift+\) - BR-2 */
    data object JumpToMatchingBracket : ShortcutAction
    
    /** Open find bar (Ctrl+F) - FR-1 */
    data object Find : ShortcutAction
    
    /** Open replace bar (Ctrl+H) - FR-9 */
    data object Replace : ShortcutAction
    
    /** Add cursor below, preserving column (Ctrl+Alt+Down) - MC-1 */
    data object AddCursorBelow : ShortcutAction
    
    /** Add cursor above, preserving column (Ctrl+Alt+Up) - MC-2 */
    data object AddCursorAbove : ShortcutAction
    
    /** Select next occurrence (Ctrl+D) - MC-3, MC-4, MC-5 */
    data object SelectNextOccurrence : ShortcutAction
}
