package com.sphynxs.mydatabases.core.database.engine

/**
 * Features (funcionalidades) que pueden soportar los motores de bases de datos.
 *
 * Permite habilitar/deshabilitar funcionalidad en la UI según el motor conectado.
 *
 * @author israel-icm
 * @date 2026-06-11
 */
enum class DatabaseFeature {
    /** Stored procedures */
    STORED_PROCEDURES,

    /** Triggers */
    TRIGGERS,

    /** Vistas */
    VIEWS,

    /** Eventos (MySQL) */
    EVENTS,

    /** Sequences (PostgreSQL, MariaDB) */
    SEQUENCES,

    /** Foreign keys */
    FOREIGN_KEYS,

    /** Transacciones */
    TRANSACTIONS,

    /** Full-text search */
    FULL_TEXT_SEARCH,

    /** Tipo de datos JSON */
    JSON_TYPE,

    /** Schemas (PostgreSQL) */
    SCHEMAS,

    /** Window functions (PostgreSQL, MySQL 8.0+) */
    WINDOW_FUNCTIONS,

    /** CTEs recursivos (PostgreSQL, MySQL 8.0+) */
    RECURSIVE_CTE
}
