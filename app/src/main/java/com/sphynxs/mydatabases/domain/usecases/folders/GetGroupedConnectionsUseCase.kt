package com.sphynxs.mydatabases.domain.usecases.folders

import com.sphynxs.mydatabases.data.local.dao.ConnectionDao
import com.sphynxs.mydatabases.domain.models.ConnectionListItem
import com.sphynxs.mydatabases.domain.repositories.ConnectionRepository
import com.sphynxs.mydatabases.domain.repositories.FolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Use case para obtener conexiones y folders combinados en una lista agrupada.
 *
 * Combina los flows de folders y conexiones para generar una estructura
 * jerárquica lista para mostrar en UI (LazyColumn).
 *
 * @property folderRepository Repositorio de folders
 * @property connectionDao DAO de conexiones (acceso directo para queries específicas)
 * @author israel-icm
 * @date 2026-06-30
 */
class GetGroupedConnectionsUseCase @Inject constructor(
    private val folderRepository: FolderRepository,
    private val connectionDao: ConnectionDao
) {
    /**
     * Obtiene la lista agrupada de folders y conexiones.
     *
     * Estructura retornada:
     * - Root connections (sin folder)
     * - Folders con sus conexiones (si están expandidos)
     *
     * Todo ordenado por el campo `order`.
     *
     * @return Flow con lista de ConnectionListItem (FolderItem | ConnectionItem)
     */
    operator fun invoke(): Flow<List<ConnectionListItem>> {
        return combine(
            folderRepository.getAllFolders(),
            connectionDao.getRootConnections()
        ) { folders, rootConnections ->
            buildList {
                // Primero agregar root connections (sin folder)
                rootConnections.forEach { entity ->
                    add(
                        ConnectionListItem.ConnectionItem(
                            connection = entity.toDomainWithoutDecryption(),
                            isInFolder = false
                        )
                    )
                }
                
                // Luego agregar folders
                folders.forEach { folder ->
                    // Obtener count de conexiones en el folder
                    // TODO: Esto debería ser parte del flow combinado para eficiencia
                    val connectionCount = 0  // Placeholder, necesita query suspendida
                    
                    add(
                        ConnectionListItem.FolderItem(
                            folder = folder,
                            connectionCount = connectionCount,
                            connections = emptyList()  // Se cargan bajo demanda cuando se expande
                        )
                    )
                }
            }.sortedBy { item ->
                when (item) {
                    is ConnectionListItem.FolderItem -> item.folder.order
                    is ConnectionListItem.ConnectionItem -> item.connection.order
                }
            }
        }
    }
}

// TODO: Crear mapper helper que NO desencripte passwords para listas
// Solo necesitamos id, name, host, port, type para mostrar en la lista
private fun com.sphynxs.mydatabases.data.local.entities.ConnectionEntity.toDomainWithoutDecryption(): com.sphynxs.mydatabases.core.database.models.ConnectionConfig {
    return com.sphynxs.mydatabases.core.database.models.ConnectionConfig(
        id = this.id,
        name = this.name,
        type = this.type,
        host = this.host,
        port = this.port,
        database = this.database,
        username = this.username,
        password = "",  // No desencriptar para listas
        useSSL = this.useSSL,
        sslConfig = null,  // No necesario para mostrar
        sshTunnelConfig = null,  // No necesario para mostrar
        connectionString = this.connectionString,
        connectionTimeout = this.connectionTimeout,
        readTimeout = this.readTimeout,
        maxPoolSize = this.maxPoolSize,
        createdAt = this.createdAt,
        lastUsedAt = this.lastUsedAt,
        folderId = this.folderId,
        order = this.order
    )
}
