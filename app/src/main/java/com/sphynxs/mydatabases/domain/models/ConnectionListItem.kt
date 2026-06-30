package com.sphynxs.mydatabases.domain.models

import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.core.database.models.ConnectionFolder

/**
 * Item de lista para mostrar conexiones organizadas en folders.
 *
 * Sealed class que representa los diferentes tipos de items que pueden
 * aparecer en la lista de conexiones:
 * - FolderItem: Un folder expandible con sus conexiones
 * - ConnectionItem: Una conexión individual (en root o dentro de folder)
 *
 * Permite renderizar una estructura jerárquica en un LazyColumn plano.
 *
 * @author israel-icm
 * @date 2026-06-30
 */
sealed class ConnectionListItem {
    
    /**
     * Item de folder expandible.
     *
     * @property folder Configuración del folder
     * @property connectionCount Número de conexiones dentro del folder
     * @property connections Lista de conexiones contenidas (vacío si colapsado)
     */
    data class FolderItem(
        val folder: ConnectionFolder,
        val connectionCount: Int,
        val connections: List<ConnectionConfig> = emptyList()
    ) : ConnectionListItem() {
        val id: String get() = folder.id
    }
    
    /**
     * Item de conexión individual.
     *
     * @property connection Configuración de la conexión
     * @property isInFolder Si la conexión está dentro de un folder (para indentación visual)
     */
    data class ConnectionItem(
        val connection: ConnectionConfig,
        val isInFolder: Boolean = false
    ) : ConnectionListItem() {
        val id: String get() = connection.id
    }
}
