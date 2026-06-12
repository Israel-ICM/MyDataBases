package com.sphynxs.mydatabases.core.database.models

/**
 * Resultado de una query SELECT.
 *
 * Contiene:
 * - Nombres de columnas retornadas
 * - Filas de datos (cada fila es un Map<columna, valor>)
 * - Metadata (rowCount, execution time, warnings)
 *
 * Example:
 * ```kotlin
 * val result = QueryResult(
 *     columns = listOf("id", "name", "email"),
 *     rows = listOf(
 *         mapOf("id" to 1, "name" to "Alice", "email" to "alice@example.com"),
 *         mapOf("id" to 2, "name" to "Bob", "email" to "bob@example.com")
 *     ),
 *     rowCount = 2,
 *     executionTimeMs = 150
 * )
 * ```
 *
 * @property columns Lista de nombres de columnas en el orden retornado por la query
 * @property rows Lista de filas, cada fila es un Map<columnName, value>
 * @property rowCount Número de filas retornadas
 * @property executionTimeMs Tiempo de ejecución de la query en milisegundos
 * @property warnings Lista de warnings retornados por el servidor (opcional)
 *
 * @author israel-icm
 * @date 2026-06-11
 */
data class QueryResult(
    val columns: List<String>,
    val rows: List<Map<String, Any?>>,
    val rowCount: Int,
    val executionTimeMs: Long,
    val warnings: List<String> = emptyList()
)
