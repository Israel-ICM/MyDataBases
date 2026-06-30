package com.sphynxs.mydatabases.domain.repositories

import com.sphynxs.mydatabases.core.database.models.ConnectionFolder
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para operaciones CRUD sobre folders de conexiones.
 *
 * Abstrae el acceso a la capa de persistencia (Room) para folders.
 *
 * @author israel-icm
 * @date 2026-06-30
 */
interface FolderRepository {
    
    /**
     * Obtiene todos los folders ordenados por `order`.
     *
     * @return Flow con la lista de folders actualizada reactivamente
     */
    fun getAllFolders(): Flow<List<ConnectionFolder>>
    
    /**
     * Obtiene un folder por su ID.
     *
     * @param id El ID del folder
     * @return El folder o null si no existe
     */
    suspend fun getById(id: String): ConnectionFolder?
    
    /**
     * Guarda un folder (crea o actualiza).
     *
     * @param folder El folder a guardar
     */
    suspend fun save(folder: ConnectionFolder)
    
    /**
     * Elimina un folder.
     *
     * IMPORTANTE: Las conexiones del folder NO se eliminan automáticamente.
     * Antes de llamar esto, decidir qué hacer con las conexiones:
     * - Opción A: Moverlas a root con moveConnectionsToRoot()
     * - Opción B: Eliminarlas también
     *
     * @param id El ID del folder a eliminar
     */
    suspend fun delete(id: String)
    
    /**
     * Alterna el estado expandido/colapsado de un folder.
     *
     * @param folderId El ID del folder
     * @param isExpanded true = expandido, false = colapsado
     */
    suspend fun toggleExpand(folderId: String, isExpanded: Boolean)
    
    /**
     * Actualiza el nombre de un folder.
     *
     * @param folderId El ID del folder
     * @param name El nuevo nombre
     */
    suspend fun updateName(folderId: String, name: String)
    
    /**
     * Actualiza el orden de un folder.
     *
     * @param folderId El ID del folder
     * @param order La nueva posición
     */
    suspend fun updateOrder(folderId: String, order: Int)
}
