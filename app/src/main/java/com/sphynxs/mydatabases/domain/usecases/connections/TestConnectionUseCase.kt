package com.sphynxs.mydatabases.domain.usecases.connections

import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.domain.repositories.ConnectionRepository
import javax.inject.Inject

/**
 * Prueba una conexión sin guardarla.
 *
 * Intenta conectar y desconectar inmediatamente para validar credenciales.
 *
 * @property repository Repositorio de conexiones
 * @author israel-icm
 * @date 2026-06-12
 */
class TestConnectionUseCase @Inject constructor(
    private val repository: ConnectionRepository
) {
    /**
     * Prueba la conexión.
     *
     * @param config La configuración a probar
     * @return Result.success si conectó bien, Result.failure si falló
     */
    suspend operator fun invoke(config: ConnectionConfig): Result<Unit> =
        repository.testConnection(config)
}
