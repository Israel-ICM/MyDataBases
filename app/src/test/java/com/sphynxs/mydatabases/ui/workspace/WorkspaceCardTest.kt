package com.sphynxs.mydatabases.ui.workspace

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for WorkspaceCard sealed class.
 *
 * TDD: RED → GREEN → TRIANGULATE → REFACTOR
 * Spec: openspec/changes/sql-editor/specs/workspace-cards/spec.md
 *
 * Scenarios tested:
 * - Query variant exists
 * - Two query cards coexist
 * - Mixed cards coexist (Table + Query)
 * - Stable id across re-renders
 *
 * @author israel-icm
 * @date 2026-06-23
 */
class WorkspaceCardTest {

    /**
     * Scenario: Query variant exists
     * GIVEN the workspace card sealed type is defined
     * WHEN code references `WorkspaceCard.Query`
     * THEN the type resolves and compiles
     * AND it carries the data needed to identify a query editor instance
     */
    @Test
    fun queryVariantExists() {
        val queryCard = WorkspaceCard.Query(
            id = "query:conn1:q1",
            title = "New Query",
            connectionId = "conn1",
            initialSql = null
        )

        assertNotNull(queryCard)
        assertEquals("query:conn1:q1", queryCard.id)
        assertEquals("New Query", queryCard.title)
        assertEquals("conn1", queryCard.connectionId)
        assertNull(queryCard.initialSql)
    }

    /**
     * Scenario: Two query cards coexist
     * GIVEN the user opens two `WorkspaceCard.Query` cards in sequence
     * WHEN both cards exist in the workspace
     * THEN each card has its own unique id
     */
    @Test
    fun twoQueryCardsCoexist() {
        val query1 = WorkspaceCard.Query(
            id = "query:conn1:q1",
            title = "Query 1",
            connectionId = "conn1",
            initialSql = "SELECT 1"
        )

        val query2 = WorkspaceCard.Query(
            id = "query:conn1:q2",
            title = "Query 2",
            connectionId = "conn1",
            initialSql = "SELECT 2"
        )

        val cards = listOf(query1, query2)

        assertEquals(2, cards.size)
        assertNotEquals(query1.id, query2.id)
        assertEquals("SELECT 1", query1.initialSql)
        assertEquals("SELECT 2", query2.initialSql)
    }

    /**
     * Scenario: Mixed cards coexist
     * GIVEN the user opens one `Table` card and one `Query` card
     * WHEN both cards exist in the workspace
     * THEN both render in the workspace stack
     */
    @Test
    fun mixedCardsCoexist() {
        val tableCard = WorkspaceCard.Table(
            id = "table:conn1:db1:users",
            title = "users",
            connectionId = "conn1",
            databaseName = "db1",
            tableName = "users"
        )

        val queryCard = WorkspaceCard.Query(
            id = "query:conn1:q1",
            title = "New Query",
            connectionId = "conn1",
            initialSql = null
        )

        val cards = listOf(tableCard, queryCard)

        assertEquals(2, cards.size)
        assertTrue(cards[0] is WorkspaceCard.Table)
        assertTrue(cards[1] is WorkspaceCard.Query)
    }

    /**
     * Scenario: Stable id across re-renders
     * GIVEN a `WorkspaceCard.Query` card with id `q1` is open
     * WHEN the workspace is re-rendered
     * THEN the card still resolves by id `q1`
     */
    @Test
    fun stableIdAcrossRerenders() {
        val original = WorkspaceCard.Query(
            id = "query:conn1:q1",
            title = "Query 1",
            connectionId = "conn1",
            initialSql = "SELECT 1"
        )

        // Simulate re-render (copy)
        val rerendered = WorkspaceCard.Query(
            id = "query:conn1:q1",
            title = "Query 1",
            connectionId = "conn1",
            initialSql = "SELECT 1"
        )

        assertEquals(original.id, rerendered.id)
        assertEquals(original, rerendered)  // data class equality
    }
}
