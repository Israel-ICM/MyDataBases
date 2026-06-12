package com.sphynxs.mydatabases.core.database.models

/**
 * Representa una foreign key (clave foránea) en una tabla.
 *
 * @property name Nombre del constraint
 * @property column Columna local
 * @property referencedTable Tabla referenciada
 * @property referencedColumn Columna referenciada
 * @property onDelete Acción al eliminar la fila referenciada
 * @property onUpdate Acción al actualizar la fila referenciada
 * @author israel-icm
 * @date 2026-06-11
 */
data class ForeignKey(
    val name: String,
    val column: String,
    val referencedTable: String,
    val referencedColumn: String,
    val onDelete: ReferentialAction,
    val onUpdate: ReferentialAction
)

/**
 * Acción referencial (ON DELETE / ON UPDATE).
 */
enum class ReferentialAction {
    /** Elimina/actualiza en cascada */
    CASCADE,

    /** Establece NULL */
    SET_NULL,

    /** Rechaza la operación */
    RESTRICT,

    /** No hace nada (default) */
    NO_ACTION
}
