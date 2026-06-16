package com.sphynxs.mydatabases.domain.usecases.connections

import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.domain.repositories.ConnectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Obtiene todas las conexiones guardadas.
 *
 * Retorna un Flow que se actualiza automáticamente cuando hay cambios.
 *
 * @property repository Repositorio de conexiones
 * @author israel-icm
 * @date 2026-06-12
 */
class GetConnectionsUseCase @Inject constructor(
    private val repository: ConnectionRepository
) {
    /**
     * Obtiene todas las conexiones como Flow reactivo.
     *
     * @return Flow con la lista de conexiones (se actualiza si hay cambios en Room)
     */
    operator fun invoke(): Flow<List<ConnectionConfig>> = repository.getAll()
}
