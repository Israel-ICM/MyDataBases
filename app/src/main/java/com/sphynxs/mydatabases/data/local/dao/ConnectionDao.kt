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
     * Actualiza el timestamp del último uso de una conexión.
     *
     * @param id El ID de la conexión
     * @param timestamp El nuevo timestamp en millis
     */
    @Query("UPDATE connections SET last_used_at = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: String, timestamp: Long)
}
