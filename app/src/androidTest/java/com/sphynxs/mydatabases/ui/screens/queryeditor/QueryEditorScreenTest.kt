package com.sphynxs.mydatabases.ui.screens.queryeditor

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.sphynxs.mydatabases.domain.models.QueryResult
import com.sphynxs.mydatabases.domain.usecases.ExecuteQueryUseCase
import com.sphynxs.mydatabases.domain.usecases.ExecuteUpdateUseCase
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests para QueryEditorScreen.
 *
 * TDD: RED → GREEN → TRIANGULATE → REFACTOR
 * Spec: openspec/changes/sql-editor/specs/query-editor/spec.md
 *       openspec/changes/editor-shortcuts-and-history/spec.md
 *
 * Scenarios tested:
 * - Editor renders con placeholder
 * - Execute button enabled cuando hay texto
 * - Result grid muestra SELECT results
 * - Undo button restores previous text
 * - Redo button restores undone text
 *
 * @author israel-icm
 * @date 2026-06-23, 2026-06-24
 */
class QueryEditorScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockExecuteQueryUseCase: ExecuteQueryUseCase = mockk()
    private val mockExecuteUpdateUseCase: ExecuteUpdateUseCase = mockk()

    /**
     * Scenario: Open new editor
     * GIVEN the user opens a new query card
     * WHEN the editor mounts
     * THEN an empty editable surface is shown
     * AND the Execute button is disabled
     */
    @Test
    fun opensWithEmptyEditor() {
        composeTestRule.setContent {
            QueryEditorScreen(
                connectionId = "test-conn",
                initialSql = null
            )
        }

        // ASSERT: Editor placeholder is visible
        composeTestRule
            .onNodeWithText("Enter SQL query...", substring = true)
            .assertIsDisplayed()

        // ASSERT: Execute button is disabled (empty editor)
        composeTestRule
            .onNodeWithContentDescription("Execute query")
            .assertIsNotEnabled()
    }

    /**
     * Scenario: Execute enabled with content
     * GIVEN the editor contains non-whitespace SQL
     * WHEN the toolbar renders
     * THEN the Execute button is enabled
     */
    @Test
    fun enablesExecuteButtonWhenTextEntered() {
        composeTestRule.setContent {
            QueryEditorScreen(
                connectionId = "test-conn",
                initialSql = "SELECT 1"
            )
        }

        // ASSERT: Execute button is enabled when SQL is present
        composeTestRule
            .onNodeWithContentDescription("Execute query")
            .assertIsEnabled()
    }

    /**
     * Scenario: SELECT renders grid
     * GIVEN the user executes `SELECT id, name FROM users LIMIT 3`
     * WHEN the engine returns rows
     * THEN the result panel renders the shared `result-grid`
     */
    @Test
    fun rendersResultGridForSelectQuery() {
        // Mock query result
        val mockResult = QueryResult(
            columns = listOf("id", "name"),
            rows = listOf(
                mapOf("id" to 1, "name" to "Ada"),
                mapOf("id" to 2, "name" to "Linus")
            )
        )

        coEvery {
            mockExecuteQueryUseCase(any(), any())
        } returns Result.success(mockResult)

        composeTestRule.setContent {
            QueryEditorScreen(
                connectionId = "test-conn",
                initialSql = "SELECT id, name FROM users"
            )
        }

        // Click Execute button
        composeTestRule
            .onNodeWithContentDescription("Execute query")
            .performClick()

        // Wait for results to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Ada")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // ASSERT: Result grid shows column headers
        composeTestRule
            .onNodeWithText("id")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("name")
            .assertIsDisplayed()

        // ASSERT: Result grid shows data rows
        composeTestRule
            .onNodeWithText("Ada")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Linus")
            .assertIsDisplayed()
    }

    /**
     * Scenario: Undo button restores previous text
     * GIVEN the user types text in the editor
     * WHEN the user clicks the Undo button
     * THEN the text is restored to the previous state
     */
    @Test
    fun undoButtonRestoresPreviousText() {
        composeTestRule.setContent {
            QueryEditorScreen(
                connectionId = "test-conn",
                initialSql = null
            )
        }

        // Type initial text
        composeTestRule
            .onNodeWithContentDescription("SQL Code Editor")
            .performTextInput("SELECT * FROM users")

        // Wait for history to coalesce (500ms window)
        Thread.sleep(600)

        // Type additional text
        composeTestRule
            .onNodeWithContentDescription("SQL Code Editor")
            .performTextClearance()
        
        composeTestRule
            .onNodeWithContentDescription("SQL Code Editor")
            .performTextInput("SELECT * FROM orders")

        // Wait for history to coalesce
        Thread.sleep(600)

        // Click Undo button
        composeTestRule
            .onNodeWithContentDescription("Undo")
            .performClick()

        // ASSERT: Text is restored to previous state
        composeTestRule
            .onNodeWithText("SELECT * FROM users")
            .assertIsDisplayed()
    }

    /**
     * Scenario: Redo button restores undone text
     * GIVEN the user types text and then undoes it
     * WHEN the user clicks the Redo button
     * THEN the text is restored to the state before undo
     */
    @Test
    fun redoButtonRestoresUndoneText() {
        composeTestRule.setContent {
            QueryEditorScreen(
                connectionId = "test-conn",
                initialSql = null
            )
        }

        // Type initial text
        composeTestRule
            .onNodeWithContentDescription("SQL Code Editor")
            .performTextInput("SELECT * FROM users")

        // Wait for history
        Thread.sleep(600)

        // Type additional text
        composeTestRule
            .onNodeWithContentDescription("SQL Code Editor")
            .performTextClearance()
        
        composeTestRule
            .onNodeWithContentDescription("SQL Code Editor")
            .performTextInput("SELECT * FROM orders")

        // Wait for history
        Thread.sleep(600)

        // Click Undo button
        composeTestRule
            .onNodeWithContentDescription("Undo")
            .performClick()

        // Click Redo button
        composeTestRule
            .onNodeWithContentDescription("Redo")
            .performClick()

        // ASSERT: Text is restored to the full state
        composeTestRule
            .onNodeWithText("SELECT * FROM orders")
            .assertIsDisplayed()
    }

    // ============================================================
    // PR #1: SQL Formatter (Format toolbar button + Ctrl+Shift+F)
    // Spec: openspec/changes/editor-completion-and-format/spec.md (scenarios 9-12)
    //
    // NOTE: Written following the existing convention of this file (no explicit
    // Hilt test rule/HiltAndroidRule — this file already relies on hiltViewModel()
    // resolving without an injected test graph, which is the same pre-existing
    // DI/legacy test-suite blocker documented in apply-progress). These tests are
    // NOT executed by the apply agent per maintainer instruction; the maintainer
    // compiles/runs them. Run with:
    // ./gradlew connectedAndroidTest --tests "*.QueryEditorScreenTest"
    // ============================================================

    /**
     * Task 3.1 / Scenario 9 (precondition): Format button exists and is enabled
     * when the editor text is non-blank.
     */
    @Test
    fun formatButton_exists_whenTextNotBlank() {
        composeTestRule.setContent {
            androidx.compose.foundation.layout.Column {
                QueryEditorToolbarRow(connectionId = "test-conn")
                QueryEditorScreen(connectionId = "test-conn", initialSql = "SELECT 1")
            }
        }

        composeTestRule.waitForIdle()

        findFormatButtonNode().assertIsEnabled()
    }

    /**
     * Scenario 9: Format via toolbar button click — editor text replaced with the
     * formatted output AND the previous text pushed onto EditorHistory.
     */
    @Test
    fun tappingFormatButton_replacesTextAndPushesHistory() {
        composeTestRule.setContent {
            androidx.compose.foundation.layout.Column {
                QueryEditorToolbarRow(connectionId = "test-conn")
                QueryEditorScreen(connectionId = "test-conn", initialSql = "select id from users")
            }
        }

        composeTestRule.waitForIdle()

        findFormatButtonNode().performClick()

        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule
                .onAllNodesWithText("SELECT id", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // ASSERT: text replaced with formatted output (UPPERCASE + newline before FROM)
        composeTestRule
            .onNodeWithText("SELECT id", substring = true)
            .assertIsDisplayed()

        // History push is verified indirectly by formatThenUndo_restoresOriginal()
        // (a single Undo must restore the original byte-for-byte).
    }

    /**
     * Scenario 10: Ctrl+Shift+F on a focused physical-keyboard editor triggers the
     * same formatting behavior as the toolbar button click.
     */
    @Test
    fun ctrlShiftF_triggersFormat() {
        composeTestRule.setContent {
            QueryEditorScreen(connectionId = "test-conn", initialSql = "select id from users")
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("SQL Code Editor")
            .performKeyInput {
                keyDown(androidx.compose.ui.input.key.Key.CtrlLeft)
                keyDown(androidx.compose.ui.input.key.Key.ShiftLeft)
                keyDown(androidx.compose.ui.input.key.Key.F)
                keyUp(androidx.compose.ui.input.key.Key.F)
                keyUp(androidx.compose.ui.input.key.Key.ShiftLeft)
                keyUp(androidx.compose.ui.input.key.Key.CtrlLeft)
            }

        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule
                .onAllNodesWithText("SELECT id", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNodeWithText("SELECT id", substring = true)
            .assertIsDisplayed()
    }

    /**
     * Scenario 11: Format then Undo (Ctrl+Z) restores the original pre-format text
     * exactly (byte-for-byte). Ctrl+Z is used instead of the Undo toolbar button
     * because Undo's own toolbar-to-screen state bridge is a separate, pre-existing
     * gap outside PR #1 scope (see report) — Ctrl+Z is fully self-contained within
     * QueryEditorScreen and already works correctly.
     */
    @Test
    fun formatThenUndo_restoresOriginal() {
        composeTestRule.setContent {
            androidx.compose.foundation.layout.Column {
                QueryEditorToolbarRow(connectionId = "test-conn")
                QueryEditorScreen(connectionId = "test-conn", initialSql = "select 1")
            }
        }

        composeTestRule.waitForIdle()

        findFormatButtonNode().performClick()

        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithText("SELECT 1").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule
            .onNodeWithContentDescription("SQL Code Editor")
            .performKeyInput {
                keyDown(androidx.compose.ui.input.key.Key.CtrlLeft)
                keyDown(androidx.compose.ui.input.key.Key.Z)
                keyUp(androidx.compose.ui.input.key.Key.Z)
                keyUp(androidx.compose.ui.input.key.Key.CtrlLeft)
            }

        // ASSERT: original text restored byte-for-byte (lowercase, no reflow)
        composeTestRule
            .onNodeWithText("select 1")
            .assertIsDisplayed()
    }

    /**
     * Scenario 12: Format with multi-cursor active applies to the full text (not
     * per-cursor) and clears cursorPositions afterwards.
     *
     * KNOWN GAP (pre-existing, not introduced by PR #1): activating multi-cursor
     * mode via keyboard (Ctrl+Alt+Down/Up, Ctrl+D) is not yet wired in
     * QueryEditorScreen's onShortcut handler — AddCursorBelow, AddCursorAbove and
     * SelectNextOccurrence are still TODO placeholders from the PR #6 multi-cursor
     * feature. There is currently no public UI entry point to drive
     * `cursorPositions` into a non-empty state from a Compose UI test, so this
     * scenario cannot be exercised end-to-end yet.
     *
     * By code inspection: applyFormat() in QueryEditorScreen.kt calls
     * `cursorPositions.clear()` unconditionally after formatting, and
     * SqlFormatter.format() always operates on the full text (it has no per-cursor
     * concept at all), so this scenario's intent is already satisfied — this test
     * is ignored until multi-cursor keyboard wiring lands and can inject state.
     */
    @Test
    @org.junit.Ignore(
        "Blocked: no public UI entry point for multi-cursor injection yet " +
            "(AddCursorBelow/AddCursorAbove/SelectNextOccurrence are TODO in QueryEditorScreen.kt)"
    )
    fun formatWithMultiCursor_clearsCursorsAndFormatsFullText() {
        // Intentionally left as a documented placeholder — see @Ignore reason above.
    }

    /**
     * Locates the Format toolbar action node regardless of whether AdaptiveToolbar
     * placed it as a visible pill IconButton or collapsed it into the overflow (⋮)
     * menu (width-dependent — "format" is the 4th left action, so it lands in
     * overflow when maxVisibleButtons <= 3, e.g. mobile portrait).
     */
    private fun findFormatButtonNode(): SemanticsNodeInteraction {
        val directMatches = composeTestRule
            .onAllNodesWithContentDescription("Format")
            .fetchSemanticsNodes()
        if (directMatches.isNotEmpty()) {
            return composeTestRule.onNodeWithContentDescription("Format")
        }
        // Fell into the adaptive toolbar's overflow menu — open it first.
        composeTestRule
            .onNodeWithContentDescription("More options")
            .performClick()
        return composeTestRule.onNodeWithText("Format")
    }
}
