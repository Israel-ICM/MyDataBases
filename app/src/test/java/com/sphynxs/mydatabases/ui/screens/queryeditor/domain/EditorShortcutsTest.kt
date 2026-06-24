package com.sphynxs.mydatabases.ui.screens.queryeditor.domain

import androidx.compose.ui.input.key.Key
import org.junit.Test
import org.junit.Assert.*

/**
 * TDD tests for EditorShortcuts mapper.
 *
 * RED → GREEN → TRIANGULATE → REFACTOR
 * Spec: openspec/changes/editor-shortcuts-and-history/specs/editor-shortcuts/spec.md
 *
 * Scenarios tested (table-driven):
 * - Ctrl+Enter → Run
 * - Ctrl+S → Save
 * - Ctrl+Z → Undo
 * - Ctrl+Y → Redo
 * - Ctrl+Shift+Z → Redo
 * - Non-shortcuts → null (propagate)
 *
 * @author israel-icm
 * @date 2026-06-24
 */
class EditorShortcutsTest {

    /**
     * RED: Test Ctrl+Enter maps to Run.
     * This test will FAIL because EditorShortcuts.mapKeyEvent doesn't exist yet.
     */
    @Test
    fun ctrlEnterMapsToRun() {
        val action = EditorShortcuts.mapKeyEvent(
            key = Key.Enter,
            isCtrlPressed = true,
            isShiftPressed = false
        )

        assertEquals(ShortcutAction.Run, action)
    }

    /**
     * TRIANGULATE: Test Ctrl+S maps to Save.
     */
    @Test
    fun ctrlSMapsToSave() {
        val action = EditorShortcuts.mapKeyEvent(
            key = Key.S,
            isCtrlPressed = true,
            isShiftPressed = false
        )

        assertEquals(ShortcutAction.Save, action)
    }

    /**
     * TRIANGULATE: Test Ctrl+Z maps to Undo.
     */
    @Test
    fun ctrlZMapsToUndo() {
        val action = EditorShortcuts.mapKeyEvent(
            key = Key.Z,
            isCtrlPressed = true,
            isShiftPressed = false
        )

        assertEquals(ShortcutAction.Undo, action)
    }

    /**
     * TRIANGULATE: Test Ctrl+Y maps to Redo.
     */
    @Test
    fun ctrlYMapsToRedo() {
        val action = EditorShortcuts.mapKeyEvent(
            key = Key.Y,
            isCtrlPressed = true,
            isShiftPressed = false
        )

        assertEquals(ShortcutAction.Redo, action)
    }

    /**
     * TRIANGULATE: Test Ctrl+Shift+Z maps to Redo (alternative binding).
     */
    @Test
    fun ctrlShiftZMapsToRedo() {
        val action = EditorShortcuts.mapKeyEvent(
            key = Key.Z,
            isCtrlPressed = true,
            isShiftPressed = true
        )

        assertEquals(ShortcutAction.Redo, action)
    }

    /**
     * TRIANGULATE: Test non-shortcuts return null (propagation).
     */
    @Test
    fun nonShortcutsReturnNull() {
        val testCases = listOf(
            Triple(Key.A, true, false),          // Ctrl+A (select all — not our shortcut)
            Triple(Key.Tab, false, false),       // Tab (indent — not our shortcut)
            Triple(Key.Backspace, false, false), // Backspace (normal editing)
            Triple(Key.Z, false, false),         // Z without Ctrl (normal typing)
            Triple(Key.Enter, false, false)      // Enter without Ctrl (newline)
        )

        testCases.forEach { (key, ctrl, shift) ->
            val action = EditorShortcuts.mapKeyEvent(
                key = key,
                isCtrlPressed = ctrl,
                isShiftPressed = shift
            )
            assertNull("Expected null for key=$key, ctrl=$ctrl, shift=$shift", action)
        }
    }
}
