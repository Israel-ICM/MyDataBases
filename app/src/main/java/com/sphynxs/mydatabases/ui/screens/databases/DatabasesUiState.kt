package com.sphynxs.mydatabases.ui.screens.databases

import com.sphynxs.mydatabases.core.database.models.Database

/**
 * Estados de UI para la pantalla de lista de bases de datos.
 *
 * Representa los diferentes estados durante la carga de las bases de datos
 * disponibles en el servidor conectado.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
sealed class DatabasesUiState {
    /**
     * Estado inicial mientras se cargan las bases de datos.
     */
    data object Loading : DatabasesUiState()

    /**
     * Bases de datos cargadas exitosamente.
     *
     * @property databases Lista de bases de datos disponibles
     */
    data class Success(val databases: List<Database>) : DatabasesUiState()

    /**
     * Error al cargar las bases de datos.
     *
     * @property message Mensaje de error localizado
     */
    data class Error(val message: String) : DatabasesUiState()
}
