package com.sphynxs.mydatabases.core.database.models

/**
 * Sealed class que representa todos los posibles errores del módulo de base de datos.
 *
 * Permite manejar errores de forma type-safe y exhaustiva.
 *
 * @author israel-icm
 * @date 2026-06-11
 */
sealed class DatabaseError(override val message: String) : Throwable(message) {

    /**
     * Error al intentar conectar al servidor de base de datos.
     *
     * @property reason Razón del fallo (ej: "Host unreachable", "Connection timeout")
     */
    data class ConnectionFailed(val reason: String) : DatabaseError("Connection failed: $reason")

    /**
     * Error de autenticación (credenciales inválidas).
     *
     * @property reason Razón del fallo (ej: "Access denied for user 'admin'")
     */
    data class AuthenticationFailed(val reason: String) : DatabaseError("Authentication failed: $reason")

    /**
     * Error al ejecutar una query SQL.
     *
     * @property query Query que falló
     * @property reason Razón del fallo (ej: "Table doesn't exist", "Syntax error")
     */
    data class QueryExecutionFailed(val query: String, val reason: String) : DatabaseError("Query execution failed: $reason\nQuery: $query")

    /**
     * Error de timeout en una operación.
     *
     * @property operation Operación que excedió el timeout (ej: "Connecting", "Executing query")
     */
    data class TimeoutError(val operation: String) : DatabaseError("Timeout during: $operation")

    /**
     * Error de configuración inválida.
     *
     * @property field Campo de configuración inválido
     * @property reason Razón de la invalidez
     */
    data class InvalidConfiguration(val field: String, val reason: String) : DatabaseError("Invalid configuration for '$field': $reason")

    /**
     * Error desconocido que envuelve una excepción genérica.
     *
     * @property throwable Excepción original
     */
    data class UnknownError(val throwable: Throwable) : DatabaseError("Unknown error: ${throwable.message}") {
        override val cause: Throwable? = throwable
    }
}
