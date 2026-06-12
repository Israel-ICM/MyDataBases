package com.sphynxs.mydatabases.core.database.models

/**
 * Sealed class que representa todos los errores posibles en operaciones de base de datos.
 *
 * Esta jerarquía de errores permite:
 * - Exhaustive when expressions (compiler verifica todos los casos)
 * - Errores específicos con contexto relevante
 * - Type-safe error handling
 *
 * Cada error extiende Throwable para poder ser lanzado como excepción.
 *
 * @author israel-icm
 * @date 2026-06-11
 */
sealed class DatabaseError : Throwable() {

    /**
     * Error al intentar conectar al servidor de base de datos.
     *
     * Causas comunes:
     * - Host inalcanzable (network error, firewall)
     * - Puerto incorrecto
     * - Servidor no responde
     *
     * @property reason Descripción del motivo del fallo
     */
    data class ConnectionFailed(val reason: String) : DatabaseError() {
        override val message: String = "Connection failed: $reason"
    }

    /**
     * Error de autenticación (usuario/contraseña incorrectos).
     *
     * @property reason Descripción del motivo del fallo
     */
    data class AuthenticationFailed(val reason: String) : DatabaseError() {
        override val message: String = "Authentication failed: $reason"
    }

    /**
     * Error al ejecutar una query SQL.
     *
     * Causas comunes:
     * - Sintaxis SQL inválida
     * - Tabla/columna no existe
     * - Permisos insuficientes
     * - Constraint violation
     *
     * @property query Query SQL que falló
     * @property reason Descripción del error
     */
    data class QueryExecutionFailed(
        val query: String,
        val reason: String
    ) : DatabaseError() {
        override val message: String = "Query execution failed: $reason\nQuery: $query"
    }

    /**
     * Error de timeout en operación de base de datos.
     *
     * Causas comunes:
     * - Query muy lenta (missing index, full table scan)
     * - Connection timeout
     * - Server overloaded
     *
     * @property operation Nombre de la operación que hizo timeout
     */
    data class TimeoutError(val operation: String) : DatabaseError() {
        override val message: String = "Operation timed out: $operation"
    }

    /**
     * Error de configuración inválida.
     *
     * Causas comunes:
     * - Port fuera de rango
     * - Host vacío
     * - Username vacío
     *
     * @property field Campo de configuración inválido
     * @property reason Descripción del error
     */
    data class InvalidConfiguration(
        val field: String,
        val reason: String
    ) : DatabaseError() {
        override val message: String = "Invalid configuration for '$field': $reason"
    }

    /**
     * Feature no soportada por el motor de base de datos.
     *
     * Ejemplo: SQLite no soporta STORED_PROCEDURES
     *
     * @property feature Nombre del feature no soportado
     */
    data class UnsupportedFeature(val feature: String) : DatabaseError() {
        override val message: String = "Unsupported feature: $feature"
    }

    /**
     * Error desconocido que no entra en ninguna otra categoría.
     *
     * Wrapper para excepciones inesperadas.
     *
     * @property throwable Excepción original
     */
    data class UnknownError(val throwable: Throwable) : DatabaseError() {
        override val message: String = "Unknown error: ${throwable.message}"
        override val cause: Throwable = throwable
    }
}
