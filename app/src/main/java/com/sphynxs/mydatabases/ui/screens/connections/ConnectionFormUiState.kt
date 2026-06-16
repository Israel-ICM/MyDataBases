package com.sphynxs.mydatabases.ui.screens.connections

/**
 * Estados posibles del formulario de conexión.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
sealed class ConnectionFormUiState {
    /**
     * Estado inicial del formulario.
     */
    data object Idle : ConnectionFormUiState()

    /**
     * Guardando la conexión en Room.
     */
    data object Saving : ConnectionFormUiState()

    /**
     * Conexión guardada exitosamente.
     */
    data object Saved : ConnectionFormUiState()

    /**
     * Error al guardar.
     *
     * @property message Mensaje del error localizado
     */
    data class Error(val message: String) : ConnectionFormUiState()
}

/**
 * Estados posibles del test de conexión.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
sealed class ConnectionTestUiState {
    /**
     * Sin test en progreso.
     */
    data object Idle : ConnectionTestUiState()

    /**
     * Probando la conexión.
     */
    data object Testing : ConnectionTestUiState()

    /**
     * Test exitoso.
     */
    data object Success : ConnectionTestUiState()

    /**
     * Test falló.
     *
     * @property message Mensaje del error localizado
     */
    data class Error(val message: String) : ConnectionTestUiState()
}
