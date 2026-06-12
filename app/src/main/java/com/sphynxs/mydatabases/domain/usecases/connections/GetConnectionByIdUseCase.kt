package com.sphynxs.mydatabases.domain.usecases.connections

import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.domain.repositories.ConnectionRepository
import javax.inject.Inject

/**
 * Obtiene una conexión específica por su ID.
 *
 * @property repository Repositorio de conexiones
 * @author israel-icm
 * @date 2026-06-12
 */
class GetConnectionByIdUseCase @Inject constructor(
    private val repository: ConnectionRepository
) {
    /**
     * Busca una conexión por ID.
     *
     * @param id El ID de la conexión
     * @return La conexión o null si no existe
     */
    suspend operator fun invoke(id: String): ConnectionConfig? = repository.getById(id)
}
