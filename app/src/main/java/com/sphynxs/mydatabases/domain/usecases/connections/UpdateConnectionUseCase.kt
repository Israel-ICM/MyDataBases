package com.sphynxs.mydatabases.domain.usecases.connections

import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.domain.repositories.ConnectionRepository
import javax.inject.Inject

/**
 * Actualiza una conexión existente.
 *
 * Internamente usa save() del repositorio, que reemplaza la conexión con el mismo ID.
 *
 * @property repository Repositorio de conexiones
 * @author israel-icm
 * @date 2026-06-12
 */
class UpdateConnectionUseCase @Inject constructor(
    private val repository: ConnectionRepository
) {
    /**
     * Actualiza la conexión.
     *
     * @param config La configuración actualizada (debe tener un ID existente)
     */
    suspend operator fun invoke(config: ConnectionConfig) = repository.save(config)
}
