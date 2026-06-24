package com.sphynxs.mydatabases.ui.screens.queryeditor.domain

/**
 * Bounded undo/redo history for the SQL editor with coalescing logic.
 *
 * Maintains two stacks:
 * - Undo stack: stores past snapshots
 * - Redo stack: stores undone snapshots (cleared on new edit)
 *
 * Coalescing rules (consecutive single-char edits collapse):
 * - Flush triggers: newline, cursor jump > 1, paste (multi-char), blur, explicit flush()
 * - Max 100 snapshots (head drops past limit)
 *
 * Spec: openspec/changes/editor-shortcuts-and-history/specs/editor-shortcuts/spec.md
 * Design: Pure domain class, no Android dependencies (JVM-testable).
 *
 * @param maxSize Maximum number of snapshots to retain (default 100)
 *
 * @author israel-icm
 * @date 2026-06-24
 */
class EditorHistory(private val maxSize: Int = 100) {
    private val undoStack = ArrayDeque<EditorSnapshot>()
    private val redoStack = ArrayDeque<EditorSnapshot>()
    private var coalesceBuffer: EditorSnapshot? = null

    /**
     * True if undo is available (undo stack is non-empty).
     */
    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    /**
     * True if redo is available (redo stack is non-empty).
     */
    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    /**
     * Pushes a new snapshot to history.
     *
     * If isCoalesceable is true, consecutive single-char edits will collapse
     * into a single snapshot. Otherwise, the snapshot is immediately committed.
     *
     * Clears redo stack on new push (redo only available after undo).
     *
     * @param snapshot The editor state to save
     * @param isCoalesceable True if this edit can be coalesced with previous edits
     */
    fun push(snapshot: EditorSnapshot, isCoalesceable: Boolean) {
        // Clear redo stack on new edit
        redoStack.clear()

        if (isCoalesceable) {
            // Update coalesce buffer (will be committed on flush)
            coalesceBuffer = snapshot
        } else {
            // Flush any active coalesce first
            flushCoalesceBuffer()
            // Then commit this snapshot
            commitSnapshot(snapshot)
        }
    }

    /**
     * Reverts to the previous snapshot.
     *
     * @return The previous snapshot, or null if undo stack is empty
     */
    fun undo(): EditorSnapshot? {
        // Flush coalesce buffer first
        flushCoalesceBuffer()

        if (undoStack.isEmpty()) return null

        val current = undoStack.removeLast()
        redoStack.addLast(current)

        return undoStack.lastOrNull()
    }

    /**
     * Reapplies the most recently undone snapshot.
     *
     * @return The redone snapshot, or null if redo stack is empty
     */
    fun redo(): EditorSnapshot? {
        if (redoStack.isEmpty()) return null

        val snapshot = redoStack.removeLast()
        undoStack.addLast(snapshot)

        return snapshot
    }

    /**
     * Explicitly flushes the active coalesce buffer.
     *
     * Use when coalescing should end (e.g., 500ms idle, blur event).
     */
    fun flush() {
        flushCoalesceBuffer()
    }

    /**
     * Commits the coalesce buffer to the undo stack (if any).
     */
    private fun flushCoalesceBuffer() {
        coalesceBuffer?.let { snapshot ->
            commitSnapshot(snapshot)
            coalesceBuffer = null
        }
    }

    /**
     * Commits a snapshot to the undo stack, enforcing max size.
     */
    private fun commitSnapshot(snapshot: EditorSnapshot) {
        undoStack.addLast(snapshot)

        // Enforce max size (drop oldest if over limit)
        while (undoStack.size > maxSize) {
            undoStack.removeFirst()
        }
    }
}
