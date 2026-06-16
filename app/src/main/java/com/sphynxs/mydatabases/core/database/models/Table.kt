package com.sphynxs.mydatabases.core.database.models

/**
 * Representa una tabla o vista en la base de datos.
 *
 * @property name Nombre de la tabla
 * @property database Nombre de la base de datos a la que pertenece
 * @property type Tipo (TABLE, VIEW, SYSTEM_TABLE)
 * @property engine Motor de almacenamiento (ej: InnoDB, MyISAM) - null para vistas
 * @property rowCount Número aproximado de filas - null si no disponible
 * @property dataLength Tamaño en bytes de los datos - null si no disponible
 * @property createdAt Timestamp de creación - null si no disponible
 * @property comment Comentario de la tabla
 * @author israel-icm
 * @date 2026-06-11
 */
data class Table(
    val name: String,
    val database: String,
    val type: TableType,
    val engine: String? = null,
    val rowCount: Long? = null,
    val dataLength: Long? = null,
    val createdAt: Long? = null,
    val comment: String? = null
)

/**
 * Tipo de tabla.
 */
enum class TableType {
    /** Tabla normal */
    TABLE,

    /** Vista */
    VIEW,

    /** Tabla del sistema */
    SYSTEM_TABLE
}
