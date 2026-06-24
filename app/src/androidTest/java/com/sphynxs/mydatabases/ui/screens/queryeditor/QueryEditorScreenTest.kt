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
}
