package com.sphynxs.mydatabases.ui.screens.tables

import com.sphynxs.mydatabases.core.database.models.Table

/**
 * Estados de UI para la pantalla de lista de tablas.
 *
 * Representa los diferentes estados durante la carga de las tablas
 * de una base de datos seleccionada.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
sealed class TablesUiState {
    /**
     * Estado inicial mientras se cargan las tablas.
     */
    data object Loading : TablesUiState()

    /**
     * Tablas cargadas exitosamente.
     *
     * @property tables Lista de tablas disponibles en la base de datos
     */
    data class Success(val tables: List<Table>) : TablesUiState()

    /**
     * Base de datos sin tablas (vacía).
     */
    data object Empty : TablesUiState()

    /**
     * Error al cargar las tablas.
     *
     * @property message Mensaje de error localizado
     */
    data class Error(val message: String) : TablesUiState()
}
