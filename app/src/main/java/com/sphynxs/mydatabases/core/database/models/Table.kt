package com.sphynxs.mydatabases.core.database.models

/**
 * Representa una tabla o vista en una base de datos.
 *
 * Contiene metadata completa:
 * - Nombre y tipo (TABLE, VIEW, SYSTEM_TABLE)
 * - Storage engine (InnoDB, MyISAM, etc.)
 * - Estadísticas (row count, data size)
 * - Timestamps y comentarios
 *
 * @property name Nombre de la tabla
 * @property database Nombre de la base de datos a la que pertenece
 * @property type Tipo de tabla (TABLE, VIEW, SYSTEM_TABLE)
 * @property engine Storage engine (InnoDB, MyISAM, etc.) - null para vistas
 * @property rowCount Número aproximado de filas - null si no disponible
 * @property dataLength Tamaño de los datos en bytes - null si no disponible
 * @property createdAt Timestamp de creación (milisegundos desde epoch) - null si no disponible
 * @property comment Comentario de la tabla - null si no tiene
 *
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
 * Tipo de tabla en el motor de base de datos.
 *
 * @property TABLE Tabla normal (BASE TABLE)
 * @property VIEW Vista (definición de query almacenada)
 * @property SYSTEM_TABLE Tabla del sistema (information_schema, mysql.*)
 *
 * @author israel-icm
 * @date 2026-06-11
 */
enum class TableType {
    TABLE,
    VIEW,
    SYSTEM_TABLE
}
