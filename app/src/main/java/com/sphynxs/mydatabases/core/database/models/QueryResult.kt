package com.sphynxs.mydatabases.core.database.models

/**
 * Resultado de ejecutar una query SELECT.
 *
 * @property columns Lista de nombres de columnas
 * @property rows Lista de filas (cada fila es un Map de columna -> valor)
 * @property rowCount Número de filas retornadas
 * @property executionTimeMs Tiempo de ejecución en milisegundos
 * @property warnings Lista de warnings generados por la query (opcional)
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
