package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.domain.repositories.DatabaseRepository
import javax.inject.Inject

/**
 * Use case para desconectar de la base de datos activa.
 *
 * Cierra la conexión JDBC, el túnel SSH (si existe), y limpia
 * los recursos SSL temporales.
 *
 * @property repository Repositorio de base de datos
 *
 * @author israel-icm
 * @date 2026-06-30
 */
class DisconnectFromDatabaseUseCase @Inject constructor(
    private val repository: DatabaseRepository
) {
    /**
     * Desconecta de la base de datos activa.
     *
     * @return Result.success si se desconectó correctamente,
     *         Result.failure si no había conexión activa o falló
     */
    suspend operator fun invoke(): Result<Unit> {
        return repository.disconnect()
    }
}
