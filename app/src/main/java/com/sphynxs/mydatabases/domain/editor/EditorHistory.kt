package com.sphynxs.mydatabases.domain.editor

/**
 * Manages undo/redo history for the editor with coalescing support.
 *
 * @property maxSnapshots Maximum number of snapshots to keep (default 100)
 * @property coalescingWindowMs Time window for coalescing consecutive edits (default 500ms)
 */
class EditorHistory(
    private val maxSnapshots: Int = 100,
    private val coalescingWindowMs: Long = 500
) {
    private val undoStack = ArrayDeque<EditorSnapshot>()
    private val redoStack = ArrayDeque<EditorSnapshot>()
    
    /**
     * Push a new snapshot to history.
     *
     * Applies coalescing logic: if the last snapshot is recent (within coalescingWindowMs),
     * replace it instead of adding a new one.
     *
     * Clears the redo stack.
     *
     * @param snapshot Snapshot to push
     */
    fun push(snapshot: EditorSnapshot) {
        val now = System.currentTimeMillis()
        val shouldCoalesce = undoStack.lastOrNull()?.let { last ->
            (now - last.timestamp) < coalescingWindowMs
        } ?: false
        
        if (shouldCoalesce) {
            // Replace last snapshot (coalesce)
            undoStack.removeLast()
        }
        
        undoStack.add(snapshot)
        
        // Enforce max limit (FIFO)
        while (undoStack.size > maxSnapshots) {
            undoStack.removeFirst()
        }
        
        // Clear redo stack on new edit
        redoStack.clear()
    }
    
    /**
     * Undo the last change.
     *
     * @return Snapshot to restore, or null if undo stack is empty
     */
    fun undo(): EditorSnapshot? {
        if (undoStack.isEmpty()) return null
        
        val snapshot = undoStack.removeLast()
        redoStack.add(snapshot)
        
        return undoStack.lastOrNull()
    }
    
    /**
     * Redo the last undone change.
     *
     * @return Snapshot to restore, or null if redo stack is empty
     */
    fun redo(): EditorSnapshot? {
        if (redoStack.isEmpty()) return null
        
        val snapshot = redoStack.removeLast()
        undoStack.add(snapshot)
        
        return snapshot
    }
    
    /**
     * Check if undo is available.
     */
    fun canUndo(): Boolean = undoStack.size > 1 // Need at least 2 (current + previous)
    
    /**
     * Check if redo is available.
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    
    /**
     * Clear all history.
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
    
    /**
     * Get current snapshot without modifying stacks.
     */
    fun current(): EditorSnapshot? = undoStack.lastOrNull()
}
