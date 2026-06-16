package com.sphynxs.mydatabases.ui.screens.tableviewer

import com.sphynxs.mydatabases.core.database.models.Column
import com.sphynxs.mydatabases.core.database.models.QueryResult

/**
 * Estados de UI para la pantalla de visor de tabla.
 *
 * Representa los diferentes estados durante la carga de rows y schema
 * de una tabla seleccionada.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
sealed class TableViewerUiState {
    /**
     * Estado inicial mientras se cargan los datos de la tabla.
     */
    data object Loading : TableViewerUiState()

    /**
     * Datos de la tabla cargados exitosamente.
     *
     * @property rows Resultados de la query SELECT * LIMIT 1000
     * @property columns Metadata de las columnas (schema)
     */
    data class Success(
        val rows: QueryResult,
        val columns: List<Column>
    ) : TableViewerUiState()

    /**
     * Tabla vacía (sin filas).
     *
     * @property columns Metadata de las columnas (schema sigue disponible)
     */
    data class Empty(val columns: List<Column>) : TableViewerUiState()

    /**
     * Error al cargar los datos de la tabla.
     *
     * @property message Mensaje de error localizado
     */
    data class Error(val message: String) : TableViewerUiState()
}
