package com.sphynxs.mydatabases.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sphynxs.mydatabases.data.local.entities.ConnectionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones CRUD sobre conexiones en Room.
 *
 * Todas las operaciones son suspend para ejecutarse en background.
 * getAll() retorna un Flow para observar cambios reactivamente.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@Dao
interface ConnectionDao {

    /**
     * Inserta una nueva conexión o la reemplaza si ya existe.
     *
     * @param connection La conexión a guardar
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connection: ConnectionEntity)

    /**
     * Elimina una conexión.
     *
     * @param connection La conexión a eliminar
     */
    @Delete
    suspend fun delete(connection: ConnectionEntity)

    /**
     * Elimina una conexión por su ID.
     *
     * @param id El ID de la conexión a eliminar
     */
    @Query("DELETE FROM connections WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Obtiene una conexión por su ID.
     *
     * @param id El ID de la conexión
     * @return La conexión o null si no existe
     */
    @Query("SELECT * FROM connections WHERE id = :id")
    suspend fun getById(id: String): ConnectionEntity?

    /**
     * Obtiene todas las conexiones ordenadas por último uso (más reciente primero).
     *
     * @return Flow con la lista de conexiones actualizada reactivamente
     */
    @Query("SELECT * FROM connections ORDER BY last_used_at DESC, created_at DESC")
    fun getAll(): Flow<List<ConnectionEntity>>
    
    /**
     * Obtiene las conexiones del nivel root (sin folder) ordenadas por `order`.
     *
     * @return Flow con la lista de conexiones root
     */
    @Query("SELECT * FROM connections WHERE folder_id IS NULL ORDER BY `order` ASC")
    fun getRootConnections(): Flow<List<ConnectionEntity>>
    
    /**
     * Obtiene las conexiones dentro de un folder específico ordenadas por `order`.
     *
     * @param folderId El ID del folder
     * @return Flow con la lista de conexiones del folder
     */
    @Query("SELECT * FROM connections WHERE folder_id = :folderId ORDER BY `order` ASC")
    fun getConnectionsInFolder(folderId: String): Flow<List<ConnectionEntity>>
    
    /**
     * Mueve una conexión a un folder (o a root si folderId es null).
     *
     * @param connectionId El ID de la conexión a mover
     * @param folderId El ID del folder destino (null = root)
     */
    @Query("UPDATE connections SET folder_id = :folderId WHERE id = :connectionId")
    suspend fun moveToFolder(connectionId: String, folderId: String?)
    
    /**
     * Actualiza el orden de una conexión.
     *
     * @param connectionId El ID de la conexión
     * @param order La nueva posición
     */
    @Query("UPDATE connections SET `order` = :order WHERE id = :connectionId")
    suspend fun updateOrder(connectionId: String, order: Int)
    
    /**
     * Cuenta cuántas conexiones hay en un folder.
     *
     * @param folderId El ID del folder
     * @return Número de conexiones en el folder
     */
    @Query("SELECT COUNT(*) FROM connections WHERE folder_id = :folderId")
    suspend fun getConnectionCountInFolder(folderId: String): Int
    
    /**
     * Mueve todas las conexiones de un folder a root.
     *
     * @param folderId El ID del folder
     */
    @Query("UPDATE connections SET folder_id = NULL WHERE folder_id = :folderId")
    suspend fun moveConnectionsToRoot(folderId: String)

    /**
     * Actualiza el timestamp del último uso de una conexión.
     *
     * @param id El ID de la conexión
     * @param timestamp El nuevo timestamp en millis
     */
    @Query("UPDATE connections SET last_used_at = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: String, timestamp: Long)
}
