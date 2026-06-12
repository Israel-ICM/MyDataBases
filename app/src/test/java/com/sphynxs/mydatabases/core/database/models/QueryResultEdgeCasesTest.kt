package com.sphynxs.mydatabases.core.database.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Edge case tests para QueryResult.
 *
 * Tests verify:
 * - Manejo de valores NULL en rows
 * - Resultados vacíos
 * - Warnings presentes vs vacíos
 * - Columnas con nombres especiales
 *
 * @author israel-icm
 * @date 2026-06-12
 */
class QueryResultEdgeCasesTest {

    @Test
    fun `QueryResult soporta valores NULL en rows`() {
        // Given: Rows con valores NULL
        val rows = listOf(
            mapOf("id" to 1, "name" to "Alice", "email" to null),
            mapOf("id" to 2, "name" to null, "email" to "bob@example.com")
        )
        val result = QueryResult(
            columns = listOf("id", "name", "email"),
            rows = rows,
            rowCount = 2,
            executionTimeMs = 100L
        )

        // Then: Los valores NULL están presentes
        assertNull(result.rows[0]["email"])
        assertNull(result.rows[1]["name"])
        assertEquals(1, result.rows[0]["id"])
        assertEquals("bob@example.com", result.rows[1]["email"])
    }

    @Test
    fun `QueryResult con columnas pero sin rows retorna empty list`() {
        // Given: QueryResult sin rows
        val result = QueryResult(
            columns = listOf("id", "name"),
            rows = emptyList(),
            rowCount = 0,
            executionTimeMs = 50L
        )

        // Then: rows es una lista vacía
        assertTrue(result.rows.isEmpty())
        assertEquals(0, result.rowCount)
        assertEquals(2, result.columns.size)
    }

    @Test
    fun `QueryResult soporta warnings no vacíos`() {
        // Given: QueryResult con warnings
        val warnings = listOf(
            "1329: No data available for column 'deprecated_field'",
            "1364: Field 'address' doesn't have a default value"
        )
        val result = QueryResult(
            columns = listOf("id"),
            rows = listOf(mapOf("id" to 1)),
            rowCount = 1,
            executionTimeMs = 200L,
            warnings = warnings
        )

        // Then: Los warnings están presentes
        assertEquals(2, result.warnings.size)
        assertEquals("1329: No data available for column 'deprecated_field'", result.warnings[0])
        assertEquals("1364: Field 'address' doesn't have a default value", result.warnings[1])
    }

    @Test
    fun `QueryResult con tiempo de ejecución 0ms es válido`() {
        // Given: Query instantánea
        val result = QueryResult(
            columns = listOf("result"),
            rows = listOf(mapOf("result" to "cached")),
            rowCount = 1,
            executionTimeMs = 0L
        )

        // Then: executionTimeMs = 0 es válido
        assertEquals(0L, result.executionTimeMs)
    }

    @Test
    fun `QueryResult soporta columnas con nombres especiales`() {
        // Given: Columnas con nombres SQL reservados o especiales
        val result = QueryResult(
            columns = listOf("SELECT", "FROM", "table.column", "COUNT(*)"),
            rows = listOf(
                mapOf(
                    "SELECT" to 1,
                    "FROM" to "users",
                    "table.column" to "value",
                    "COUNT(*)" to 100
                )
            ),
            rowCount = 1,
            executionTimeMs = 150L
        )

        // Then: Las columnas especiales están accesibles
        assertEquals("users", result.rows[0]["FROM"])
        assertEquals("value", result.rows[0]["table.column"])
        assertEquals(100, result.rows[0]["COUNT(*)"])
    }

    @Test
    fun `QueryResult con gran número de rows es válido`() {
        // Given: 1000 rows
        val rows = (1..1000).map { id ->
            mapOf("id" to id, "name" to "User $id")
        }
        val result = QueryResult(
            columns = listOf("id", "name"),
            rows = rows,
            rowCount = 1000,
            executionTimeMs = 5000L
        )

        // Then: Todas las rows están presentes
        assertEquals(1000, result.rowCount)
        assertEquals(1000, result.rows.size)
        assertEquals("User 1", result.rows[0]["name"])
        assertEquals("User 1000", result.rows[999]["name"])
    }
}
