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
 *
 * Scenarios tested:
 * - Editor renders con placeholder
 * - Execute button enabled cuando hay texto
 * - Result grid muestra SELECT results
 *
 * @author israel-icm
 * @date 2026-06-23
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
}
