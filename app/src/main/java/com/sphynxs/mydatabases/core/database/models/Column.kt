package com.sphynxs.mydatabases.core.database.models

/**
 * Representa una columna de una tabla.
 *
 * @property name Nombre de la columna
 * @property type Tipo de datos (ej: "int(11)", "varchar(255)", "datetime")
 * @property nullable Si permite valores NULL
 * @property key Tipo de clave (PRIMARY, UNIQUE, MULTIPLE, NONE)
 * @property default Valor por defecto - null si no tiene
 * @property extra Información adicional (ej: "auto_increment", "on update CURRENT_TIMESTAMP")
 * @property comment Comentario de la columna
 * @author israel-icm
 * @date 2026-06-11
 */
data class Column(
    val name: String,
    val type: String,
    val nullable: Boolean,
    val key: ColumnKey,
    val default: String? = null,
    val extra: String? = null,
    val comment: String? = null
)

/**
 * Tipo de clave de una columna.
 */
enum class ColumnKey {
    /** Clave primaria */
    PRIMARY,

    /** Clave única */
    UNIQUE,

    /** Parte de un índice no único (múltiples columnas) */
    MULTIPLE,

    /** Sin clave */
    NONE
}
