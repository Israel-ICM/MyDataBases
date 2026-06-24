package com.sphynxs.mydatabases.ui.screens.queryeditor.domain

import androidx.compose.ui.text.TextRange
import org.junit.Test
import org.junit.Assert.*

/**
 * TDD tests for EditorSnapshot data class.
 *
 * RED → GREEN → TRIANGULATE → REFACTOR
 * Spec: openspec/changes/editor-shortcuts-and-history/specs/editor-shortcuts/spec.md
 *
 * Scenarios tested:
 * - Snapshot captures text, selection, cursorPositions
 * - Two snapshots with identical state are equal
 * - copy() preserves all fields
 *
 * @author israel-icm
 * @date 2026-06-24
 */
class EditorSnapshotTest {

    /**
     * RED: Test that snapshot captures all fields.
     * This test will FAIL because EditorSnapshot doesn't exist yet.
     */
    @Test
    fun capturesTextSelectionAndCursors() {
        val snapshot = EditorSnapshot(
            text = "SELECT * FROM users",
            selection = TextRange(7, 8),
            cursorPositions = listOf(10, 25, 40)
        )

        assertEquals("SELECT * FROM users", snapshot.text)
        assertEquals(TextRange(7, 8), snapshot.selection)
        assertEquals(listOf(10, 25, 40), snapshot.cursorPositions)
    }

    /**
     * TRIANGULATE: Test equality with identical state.
     */
    @Test
    fun snapshotsWithIdenticalStateAreEqual() {
        val snapshot1 = EditorSnapshot(
            text = "SELECT *",
            selection = TextRange(0, 8),
            cursorPositions = listOf(5)
        )
        val snapshot2 = EditorSnapshot(
            text = "SELECT *",
            selection = TextRange(0, 8),
            cursorPositions = listOf(5)
        )

        assertEquals(snapshot1, snapshot2)
        assertEquals(snapshot1.hashCode(), snapshot2.hashCode())
    }

    /**
     * TRIANGULATE: Test copy() preserves all fields.
     */
    @Test
    fun copyPreservesAllFields() {
        val original = EditorSnapshot(
            text = "INSERT INTO",
            selection = TextRange(11),
            cursorPositions = emptyList()
        )
        
        val copied = original.copy(text = "INSERT INTO users")

        assertEquals("INSERT INTO users", copied.text)
        assertEquals(TextRange(11), copied.selection)
        assertEquals(emptyList<Int>(), copied.cursorPositions)
    }
}
