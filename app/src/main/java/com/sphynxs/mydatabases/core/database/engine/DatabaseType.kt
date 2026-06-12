package com.sphynxs.mydatabases.core.database.engine

/**
 * Enum que representa los diferentes tipos de motores de bases de datos soportados.
 *
 * Cada tipo define:
 * - displayName: Nombre legible para UI
 * - defaultPort: Puerto por defecto del motor
 *
 * @property displayName Nombre del motor para mostrar en UI
 * @property defaultPort Puerto por defecto del motor
 *
 * @author israel-icm
 * @date 2026-06-11
 */
enum class DatabaseType(
    val displayName: String,
    val defaultPort: Int
) {
    /**
     * MySQL 5.7+, 8.0+
     */
    MYSQL("MySQL", 3306),

    /**
     * MariaDB 10.5+, 10.11+, 11.0+
     */
    MARIADB("MariaDB", 3306),

    /**
     * PostgreSQL 12+, 13+, 14+, 15+
     * (Implementación futura en v1.1)
     */
    POSTGRESQL("PostgreSQL", 5432),

    /**
     * SQLite (archivo local)
     * (Implementación futura en v1.1)
     */
    SQLITE("SQLite", 0)
}
