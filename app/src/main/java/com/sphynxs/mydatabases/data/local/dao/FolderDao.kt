package com.sphynxs.mydatabases.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sphynxs.mydatabases.core.database.models.ConnectionFolder
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones CRUD sobre folders de conexiones en Room.
 *
 * Permite crear, listar, actualizar y eliminar folders organizadores.
 *
 * @author israel-icm
 * @date 2026-06-30
 */
@Dao
interface FolderDao {

    /**
     * Inserta un nuevo folder o lo reemplaza si ya existe.
     *
     * @param folder El folder a guardar
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: ConnectionFolder)

    /**
     * Elimina un folder.
     *
     * IMPORTANTE: Antes de llamar esto, mover las conexiones del folder a root
     * usando ConnectionDao.moveConnectionsToRoot() para evitar huérfanos.
     *
     * @param folder El folder a eliminar
     */
    @Delete
    suspend fun delete(folder: ConnectionFolder)

    /**
     * Elimina un folder por su ID.
     *
     * @param id El ID del folder a eliminar
     */
    @Query("DELETE FROM connection_folders WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Obtiene un folder por su ID.
     *
     * @param id El ID del folder
     * @return El folder o null si no existe
     */
    @Query("SELECT * FROM connection_folders WHERE id = :id")
    suspend fun getById(id: String): ConnectionFolder?

    /**
     * Obtiene todos los folders ordenados por `order`.
     *
     * @return Flow con la lista de folders actualizada reactivamente
     */
    @Query("SELECT * FROM connection_folders ORDER BY `order` ASC")
    fun getAllFolders(): Flow<List<ConnectionFolder>>

    /**
     * Actualiza el estado expandido/colapsado de un folder.
     *
     * @param folderId El ID del folder
     * @param isExpanded true = expandido, false = colapsado
     */
    @Query("UPDATE connection_folders SET is_expanded = :isExpanded WHERE id = :folderId")
    suspend fun updateExpandState(folderId: String, isExpanded: Boolean)

    /**
     * Actualiza el orden de un folder.
     *
     * @param folderId El ID del folder
     * @param order La nueva posición
     */
    @Query("UPDATE connection_folders SET `order` = :order WHERE id = :folderId")
    suspend fun updateOrder(folderId: String, order: Int)
    
    /**
     * Actualiza el nombre de un folder.
     *
     * @param folderId El ID del folder
     * @param name El nuevo nombre
     */
    @Query("UPDATE connection_folders SET name = :name WHERE id = :folderId")
    suspend fun updateName(folderId: String, name: String)
}
