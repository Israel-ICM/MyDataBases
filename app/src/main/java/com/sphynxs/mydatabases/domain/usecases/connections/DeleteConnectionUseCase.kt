package com.sphynxs.mydatabases.domain.usecases.connections

import com.sphynxs.mydatabases.domain.repositories.ConnectionRepository
import javax.inject.Inject

/**
 * Elimina una conexión por su ID.
 *
 * @property repository Repositorio de conexiones
 * @author israel-icm
 * @date 2026-06-12
 */
class DeleteConnectionUseCase @Inject constructor(
    private val repository: ConnectionRepository
) {
    /**
     * Elimina la conexión.
     *
     * @param id El ID de la conexión a eliminar
     */
    suspend operator fun invoke(id: String) = repository.delete(id)
}
