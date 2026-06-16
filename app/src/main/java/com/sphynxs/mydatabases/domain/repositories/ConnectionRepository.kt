package com.sphynxs.mydatabases.domain.repositories

import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para gestionar configuraciones de conexión a bases de datos.
 *
 * Maneja el CRUD de conexiones con encriptación de credenciales automática.
 * Las contraseñas se encriptan antes de guardar en Room y se desencriptan
 * al leer.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
interface ConnectionRepository {

    /**
     * Obtiene todas las conexiones guardadas.
     *
     * Las contraseñas ya vienen desencriptadas listas para usar.
     *
     * @return Flow con la lista de conexiones ordenadas por último uso
     */
    fun getAll(): Flow<List<ConnectionConfig>>

    /**
     * Obtiene una conexión específica por su ID.
     *
     * @param id El ID de la conexión
     * @return La conexión con password desencriptado o null si no existe
     */
    suspend fun getById(id: String): ConnectionConfig?

    /**
     * Guarda una nueva conexión o actualiza una existente.
     *
     * El password se encripta automáticamente antes de guardarlo en Room.
     *
     * @param config La configuración de conexión a guardar
     */
    suspend fun save(config: ConnectionConfig)

    /**
     * Elimina una conexión por su ID.
     *
     * @param id El ID de la conexión a eliminar
     */
    suspend fun delete(id: String)

    /**
     * Prueba la conexión sin guardarla.
     *
     * Intenta conectarse a la base de datos con las credenciales provistas
     * y desconecta inmediatamente. No guarda nada.
     *
     * @param config La configuración a probar
     * @return Result.success si conectó bien, Result.failure con error si falló
     */
    suspend fun testConnection(config: ConnectionConfig): Result<Unit>
}
