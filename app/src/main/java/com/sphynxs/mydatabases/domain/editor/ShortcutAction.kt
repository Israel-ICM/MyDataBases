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
}
