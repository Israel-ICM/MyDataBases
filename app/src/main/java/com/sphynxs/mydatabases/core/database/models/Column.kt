package com.sphynxs.mydatabases.core.database.models

/**
 * Representa una columna de una tabla.
 *
 * Contiene metadata completa de la columna:
 * - Nombre y tipo de datos
 * - Nullability
 * - Key type (PRIMARY, UNIQUE, etc.)
 * - Default value
 * - Extra flags (auto_increment, on update, etc.)
 *
 * @property name Nombre de la columna
 * @property type Tipo de datos (ej: "int(11)", "varchar(255)", "datetime")
 * @property nullable Si la columna acepta NULL
 * @property key Tipo de clave (PRIMARY, UNIQUE, MULTIPLE, NONE)
 * @property default Valor por defecto - null si no tiene
 * @property extra Flags adicionales (auto_increment, on update CURRENT_TIMESTAMP, etc.) - null si no tiene
 * @property comment Comentario de la columna - null si no tiene
 *
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
 *
 * Basado en el campo COLUMN_KEY de information_schema.COLUMNS en MySQL.
 *
 * @property PRIMARY Primary key
 * @property UNIQUE Unique key
 * @property MULTIPLE Múltiples keys (índices compuestos)
 * @property NONE Sin clave
 *
 * @author israel-icm
 * @date 2026-06-11
 */
enum class ColumnKey {
    PRIMARY,
    UNIQUE,
    MULTIPLE,
    NONE
}
