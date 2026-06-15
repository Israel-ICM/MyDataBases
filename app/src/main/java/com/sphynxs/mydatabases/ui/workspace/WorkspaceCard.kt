package com.sphynxs.mydatabases.ui.workspace

/**
 * Workspace card — sealed class que representa una tarjeta en el workspace multi-tab.
 *
 * El workspace permite abrir múltiples "cards" (tablas, queries, editores, etc.)
 * y navegar entre ellas con un HorizontalPager.
 *
 * Cada card tiene:
 * - id: Identificador único (ej: "table:db1:users")
 * - title: Título mostrado en la tab (ej: "users")
 *
 * Tipos disponibles:
 * - Table: Vista de tabla (rows + schema)
 * - Query: Editor de SQL (futuro)
 * - Editor: Editor de registro (futuro)
 * - Function: Visor de función/stored procedure (futuro)
 * - View: Visor de vista (futuro)
 *
 * @author israel-icm
 * @date 2026-06-15
 */
sealed class WorkspaceCard(
    open val id: String,
    open val title: String
) {
    /**
     * Card de tabla — muestra rows y schema de una tabla.
     *
     * @param id Identificador único (formato: "table:{connectionId}:{databaseName}:{tableName}")
     * @param title Título de la tab (usualmente el nombre de la tabla)
     * @param connectionId ID de la conexión
     * @param databaseName Nombre de la base de datos
     * @param tableName Nombre de la tabla
     */
    data class Table(
        override val id: String,
        override val title: String,
        val connectionId: String,
        val databaseName: String,
        val tableName: String
    ) : WorkspaceCard(id, title)

    // Future: Query, Editor, Function, View
}
