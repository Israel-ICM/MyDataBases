package com.sphynxs.mydatabases.ui.screens.queryeditor.domain

import org.junit.Test
import org.junit.Assert.*

/**
 * TDD tests for ShortcutAction sealed interface.
 *
 * RED → GREEN → TRIANGULATE → REFACTOR
 * Spec: openspec/changes/editor-shortcuts-and-history/specs/editor-shortcuts/spec.md
 *
 * Scenarios tested:
 * - All 4 action types are distinct sealed instances
 * - Exhaustive when branches compile
 *
 * @author israel-icm
 * @date 2026-06-24
 */
class ShortcutActionTest {

    /**
     * RED: Test that all 4 action types exist.
     * This test will FAIL because ShortcutAction doesn't exist yet.
     */
    @Test
    fun allActionTypesAreDistinct() {
        val actions = listOf(
            ShortcutAction.Run,
            ShortcutAction.Save,
            ShortcutAction.Undo,
            ShortcutAction.Redo
        )

        assertEquals(4, actions.size)
        assertEquals(4, actions.distinct().size) // All distinct
    }

    /**
     * TRIANGULATE: Test exhaustive when branches compile.
     */
    @Test
    fun exhaustiveWhenBranchesCompile() {
        val action: ShortcutAction = ShortcutAction.Run

        val result = when (action) {
            is ShortcutAction.Run -> "run"
            is ShortcutAction.Save -> "save"
            is ShortcutAction.Undo -> "undo"
            is ShortcutAction.Redo -> "redo"
        }

        assertEquals("run", result)
    }

    /**
     * TRIANGULATE: Test each action type independently.
     */
    @Test
    fun eachActionTypeHasCorrectIdentity() {
        assertNotEquals(ShortcutAction.Run, ShortcutAction.Save)
        assertNotEquals(ShortcutAction.Undo, ShortcutAction.Redo)
        assertNotEquals(ShortcutAction.Run, ShortcutAction.Undo)
    }
}
