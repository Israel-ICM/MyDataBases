package com.sphynxs.mydatabases.ui.screens.queryeditor

import com.sphynxs.mydatabases.core.database.models.QueryResult
import com.sphynxs.mydatabases.domain.models.StatementResult

/**
 * UI state para el editor de queries SQL.
 *
 * Estados del editor:
 * - Idle: Listo para ejecutar, ningún resultado/error visible
 * - Running: Query en ejecución, loading indicator visible, Execute disabled, Cancel enabled
 * - SelectResult: Última query fue SELECT-like, mostrar ResultGrid
 * - UpdateSummary: Última query fue INSERT/UPDATE/DELETE o mix, mostrar tabla de summary
 * - Error: Query falló, mostrar error card con mensaje
 *
 * @author israel-icm
 * @date 2026-06-23
 */
sealed class QueryEditorUiState {

    /** Estado inicial — listo para ejecutar. */
    data object Idle : QueryEditorUiState()

    /** Query en ejecución. */
    data object Running : QueryEditorUiState()

    /**
     * Resultado de SELECT-like statement (última query fue SELECT, SHOW, DESCRIBE, EXPLAIN, WITH).
     *
     * @property result Resultado de la query (columnas + rows)
     * @property executionTimeMs Tiempo total de ejecución (suma de todos los statements si multi)
     */
    data class SelectResult(
        val result: QueryResult,
        val executionTimeMs: Long
    ) : QueryEditorUiState()

    /**
     * Summary de statements ejecutados (INSERT, UPDATE, DELETE, DDL o mix con SELECT).
     *
     * @property results Lista de resultados de cada statement (sql, affectedRows, time, isQuery)
     */
    data class UpdateSummary(
        val results: List<StatementResult>
    ) : QueryEditorUiState()

    /**
     * Error al ejecutar query.
     *
     * @property message Mensaje de error del engine
     * @property failedStatement Statement SQL que falló (opcional)
     */
    data class Error(
        val message: String,
        val failedStatement: String? = null
    ) : QueryEditorUiState()
}
