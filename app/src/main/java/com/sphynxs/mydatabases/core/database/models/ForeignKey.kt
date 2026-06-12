package com.sphynxs.mydatabases.core.database.models

/**
 * Representa una foreign key constraint.
 *
 * Define la relación entre dos tablas:
 * - Columna local que referencia
 * - Tabla/columna referenciada
 * - Acciones ON DELETE y ON UPDATE
 *
 * @property name Nombre del constraint (ej: "fk_user_id")
 * @property column Nombre de la columna local
 * @property referencedTable Nombre de la tabla referenciada
 * @property referencedColumn Nombre de la columna referenciada
 * @property onDelete Acción al borrar el registro padre (CASCADE, SET_NULL, RESTRICT, NO_ACTION)
 * @property onUpdate Acción al actualizar el registro padre (CASCADE, SET_NULL, RESTRICT, NO_ACTION)
 *
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
 * Acción referencial de una foreign key.
 *
 * Define qué sucede cuando el registro padre es actualizado o borrado.
 *
 * @property CASCADE Propaga la acción a los registros hijos
 * @property SET_NULL Setea NULL en los registros hijos
 * @property RESTRICT Previene la acción si hay registros hijos
 * @property NO_ACTION Similar a RESTRICT (diferencia en timing)
 *
 * @author israel-icm
 * @date 2026-06-11
 */
enum class ReferentialAction {
    CASCADE,
    SET_NULL,
    RESTRICT,
    NO_ACTION
}
