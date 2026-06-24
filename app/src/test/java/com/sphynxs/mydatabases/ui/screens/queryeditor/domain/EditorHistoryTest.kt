package com.sphynxs.mydatabases.ui.screens.queryeditor.domain

import androidx.compose.ui.text.TextRange
import org.junit.Test
import org.junit.Assert.*

/**
 * TDD tests for EditorHistory.
 *
 * RED → GREEN → TRIANGULATE → REFACTOR
 * Spec: openspec/changes/editor-shortcuts-and-history/specs/editor-shortcuts/spec.md
 *
 * Scenarios tested:
 * - Push adds to undo stack
 * - Undo returns previous snapshot
 * - Redo reapplies undone snapshot
 * - canUndo/canRedo reflect stack states
 * - Consecutive single-char edits coalesce
 * - Newline insertion flushes coalesce buffer
 * - Cursor jump > 1 flushes buffer
 * - Paste (multi-char delta) flushes buffer
 * - Explicit flush() closes active coalesce
 * - 101st push drops oldest snapshot (bounded at 100)
 * - Multi-cursor snapshot restores all cursor positions
 * - New edit after undo clears redo stack
 *
 * @author israel-icm
 * @date 2026-06-24
 */
class EditorHistoryTest {

    /**
     * RED: Test that push adds to undo stack.
     * This test will FAIL because EditorHistory doesn't exist yet.
     */
    @Test
    fun pushAddsToUndoStack() {
        val history = EditorHistory()
        val snapshot = EditorSnapshot(
            text = "SELECT",
            selection = TextRange(6),
            cursorPositions = emptyList()
        )

        assertFalse("canUndo should be false initially", history.canUndo)

        history.push(snapshot, isCoalesceable = false)

        assertTrue("canUndo should be true after push", history.canUndo)
        assertFalse("canRedo should be false after push", history.canRedo)
    }

    /**
     * TRIANGULATE: Test that undo returns previous snapshot.
     */
    @Test
    fun undoReturnsPreviousSnapshot() {
        val history = EditorHistory()
        val snapshot1 = EditorSnapshot("SELECT", TextRange(6), emptyList())
        val snapshot2 = EditorSnapshot("SELECT *", TextRange(8), emptyList())

        history.push(snapshot1, isCoalesceable = false)
        history.push(snapshot2, isCoalesceable = false)

        val undone = history.undo()

        assertEquals(snapshot1, undone)
        assertTrue("canRedo should be true after undo", history.canRedo)
    }

    /**
     * TRIANGULATE: Test that redo reapplies undone snapshot.
     */
    @Test
    fun redoReappliesUndoneSnapshot() {
        val history = EditorHistory()
        val snapshot1 = EditorSnapshot("SELECT", TextRange(6), emptyList())
        val snapshot2 = EditorSnapshot("SELECT *", TextRange(8), emptyList())

        history.push(snapshot1, isCoalesceable = false)
        history.push(snapshot2, isCoalesceable = false)
        history.undo()

        val redone = history.redo()

        assertEquals(snapshot2, redone)
        assertFalse("canRedo should be false after redoing last change", history.canRedo)
    }

    /**
     * TRIANGULATE: Test undo on empty history returns null.
     */
    @Test
    fun undoOnEmptyHistoryReturnsNull() {
        val history = EditorHistory()

        val result = history.undo()

        assertNull("Undo on empty history should return null", result)
        assertFalse(history.canUndo)
    }

    /**
     * TRIANGULATE: Test redo on empty redo stack returns null.
     */
    @Test
    fun redoOnEmptyRedoStackReturnsNull() {
        val history = EditorHistory()
        val snapshot = EditorSnapshot("SELECT", TextRange(6), emptyList())
        history.push(snapshot, isCoalesceable = false)

        val result = history.redo()

        assertNull("Redo on empty redo stack should return null", result)
        assertFalse(history.canRedo)
    }

    /**
     * TRIANGULATE: Test consecutive single-char edits coalesce.
     * Typing "SELECT" (6 chars) should create only 1 snapshot.
     */
    @Test
    fun consecutiveSingleCharEditsCoalesce() {
        val history = EditorHistory()
        
        // Simulate typing "SELECT" one character at a time
        history.push(EditorSnapshot("S", TextRange(1), emptyList()), isCoalesceable = true)
        history.push(EditorSnapshot("SE", TextRange(2), emptyList()), isCoalesceable = true)
        history.push(EditorSnapshot("SEL", TextRange(3), emptyList()), isCoalesceable = true)
        history.push(EditorSnapshot("SELE", TextRange(4), emptyList()), isCoalesceable = true)
        history.push(EditorSnapshot("SELEC", TextRange(5), emptyList()), isCoalesceable = true)
        history.push(EditorSnapshot("SELECT", TextRange(6), emptyList()), isCoalesceable = true)

        // Undo once should remove all 6 chars
        val undone = history.undo()

        assertNull("After one undo, should be back to empty state", undone)
        assertFalse("Should not be able to undo further", history.canUndo)
    }

    /**
     * TRIANGULATE: Test newline insertion flushes coalesce buffer.
     */
    @Test
    fun newlineFlushesCoalesceBuffer() {
        val history = EditorHistory()
        
        // Type "SEL", then newline (flushes), then "FROM"
        history.push(EditorSnapshot("SEL", TextRange(3), emptyList()), isCoalesceable = true)
        history.push(EditorSnapshot("SEL\n", TextRange(4), emptyList()), isCoalesceable = false) // newline flushes
        history.push(EditorSnapshot("SEL\nFROM", TextRange(8), emptyList()), isCoalesceable = true)

        // First undo removes "FROM"
        val undone1 = history.undo()
        assertEquals("SEL\n", undone1?.text)

        // Second undo removes "SEL\n"
        val undone2 = history.undo()
        assertNull("Second undo should go back to empty", undone2)
    }

    /**
     * TRIANGULATE: Test cursor jump > 1 flushes buffer.
     */
    @Test
    fun cursorJumpFlushesBuffer() {
        val history = EditorHistory()
        
        // Type "SELECT", then jump cursor (position changes significantly), then type "FROM"
        history.push(EditorSnapshot("SELECT", TextRange(6), emptyList()), isCoalesceable = true)
        history.push(EditorSnapshot("SELECT     FROM", TextRange(15), emptyList()), isCoalesceable = false) // jump detected
        history.push(EditorSnapshot("SELECT     FROM users", TextRange(21), emptyList()), isCoalesceable = true)

        // First undo removes " users"
        val undone1 = history.undo()
        assertEquals("SELECT     FROM", undone1?.text)

        // Second undo removes "SELECT     FROM"
        val undone2 = history.undo()
        assertNull("Second undo should go back to empty", undone2)
    }

    /**
     * TRIANGULATE: Test paste (multi-char delta) flushes buffer.
     */
    @Test
    fun pasteFlushesBuffer() {
        val history = EditorHistory()
        
        // Type "SEL", then paste "ECT * FROM users" (multi-char insert)
        history.push(EditorSnapshot("SEL", TextRange(3), emptyList()), isCoalesceable = true)
        history.push(EditorSnapshot("SELECT * FROM users", TextRange(19), emptyList()), isCoalesceable = false) // paste detected

        // First undo removes paste
        val undone1 = history.undo()
        assertEquals("SEL", undone1?.text)

        // Second undo removes "SEL"
        val undone2 = history.undo()
        assertNull(undone2)
    }

    /**
     * TRIANGULATE: Test explicit flush() closes active coalesce.
     */
    @Test
    fun explicitFlushClosesCoalesce() {
        val history = EditorHistory()
        
        // Type "SELECT", flush, then type " FROM"
        history.push(EditorSnapshot("SELECT", TextRange(6), emptyList()), isCoalesceable = true)
        history.flush()
        history.push(EditorSnapshot("SELECT FROM", TextRange(11), emptyList()), isCoalesceable = true)

        // First undo removes " FROM"
        val undone1 = history.undo()
        assertEquals("SELECT", undone1?.text)

        // Second undo removes "SELECT"
        val undone2 = history.undo()
        assertNull(undone2)
    }

    /**
     * TRIANGULATE: Test 101st push drops oldest snapshot (bounded at 100).
     */
    @Test
    fun boundedAt100Snapshots() {
        val history = EditorHistory(maxSize = 3) // Use smaller limit for test speed
        
        history.push(EditorSnapshot("1", TextRange(1), emptyList()), isCoalesceable = false)
        history.push(EditorSnapshot("2", TextRange(1), emptyList()), isCoalesceable = false)
        history.push(EditorSnapshot("3", TextRange(1), emptyList()), isCoalesceable = false)
        history.push(EditorSnapshot("4", TextRange(1), emptyList()), isCoalesceable = false) // This should drop "1"

        // Undo 3 times
        val undo1 = history.undo() // back to "3"
        val undo2 = history.undo() // back to "2"
        val undo3 = history.undo() // back to null (oldest "1" was dropped)

        assertEquals("3", undo1?.text)
        assertEquals("2", undo2?.text)
        assertNull("Oldest snapshot should be dropped", undo3)
    }

    /**
     * TRIANGULATE: Test multi-cursor snapshot restores all cursor positions.
     */
    @Test
    fun multiCursorSnapshotRestoresAllCursors() {
        val history = EditorHistory()
        
        val snapshot1 = EditorSnapshot(
            text = "SELECT",
            selection = TextRange(6),
            cursorPositions = listOf(10, 25, 40)
        )
        val snapshot2 = EditorSnapshot(
            text = "SELECT X",
            selection = TextRange(8),
            cursorPositions = listOf(11, 26, 41) // cursors moved after insertion
        )

        history.push(snapshot1, isCoalesceable = false)
        history.push(snapshot2, isCoalesceable = false)

        val undone = history.undo()

        assertEquals(listOf(10, 25, 40), undone?.cursorPositions)
    }

    /**
     * TRIANGULATE: Test new edit after undo clears redo stack.
     */
    @Test
    fun newEditAfterUndoClearsRedoStack() {
        val history = EditorHistory()
        
        history.push(EditorSnapshot("SELECT", TextRange(6), emptyList()), isCoalesceable = false)
        history.push(EditorSnapshot("SELECT *", TextRange(8), emptyList()), isCoalesceable = false)
        history.undo() // Now redo stack has one item

        assertTrue("Redo should be available", history.canRedo)

        // Push new snapshot
        history.push(EditorSnapshot("SELECT users", TextRange(12), emptyList()), isCoalesceable = false)

        assertFalse("Redo should be cleared after new push", history.canRedo)
    }
}
