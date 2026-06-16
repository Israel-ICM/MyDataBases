package com.sphynxs.mydatabases.domain.usecases.connections

import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.domain.repositories.ConnectionRepository
import javax.inject.Inject

/**
 * Guarda una nueva conexión o actualiza una existente.
 *
 * El password se encripta automáticamente por el repositorio.
 *
 * @property repository Repositorio de conexiones
 * @author israel-icm
 * @date 2026-06-12
 */
class SaveConnectionUseCase @Inject constructor(
    private val repository: ConnectionRepository
) {
    /**
     * Guarda la conexión.
     *
     * @param config La configuración de conexión a guardar
     */
    suspend operator fun invoke(config: ConnectionConfig) = repository.save(config)
}
