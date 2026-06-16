package com.sphynxs.mydatabases.domain.usecases.connections

import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.domain.repositories.ConnectionRepository
import javax.inject.Inject

/**
 * Use case para obtener una conexión por su ID.
 *
 * @property repository El repositorio de conexiones
 *
 * @author israel-icm
 * @date 2026-06-15
 */
class GetConnectionUseCase @Inject constructor(
    private val repository: ConnectionRepository
) {
    /**
     * Obtiene una conexión por su ID.
     *
     * @param connectionId El ID de la conexión
     * @return La configuración de conexión, o null si no existe
     */
    suspend operator fun invoke(connectionId: String): ConnectionConfig? {
        return repository.getById(connectionId)
    }
}
