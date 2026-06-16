package com.sphynxs.mydatabases.ui.screens.connections

import com.sphynxs.mydatabases.core.database.models.ConnectionConfig

/**
 * Estados posibles de la pantalla de lista de conexiones.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
sealed class ConnectionsUiState {
    /**
     * Cargando conexiones desde Room.
     */
    data object Loading : ConnectionsUiState()

    /**
     * Conexiones cargadas exitosamente.
     *
     * @property connections Lista de conexiones (puede estar vacía)
     */
    data class Success(val connections: List<ConnectionConfig>) : ConnectionsUiState()

    /**
     * Error al cargar conexiones.
     *
     * @property message Mensaje del error localizado
     */
    data class Error(val message: String) : ConnectionsUiState()
}
