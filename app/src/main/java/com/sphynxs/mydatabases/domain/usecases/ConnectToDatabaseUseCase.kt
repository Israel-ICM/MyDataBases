package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.models.Connection
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import javax.inject.Inject

/**
 * Use Case para conectar a una base de datos.
 * 
 * Delega la operación al repository y retorna el resultado de la conexión.
 * 
 * @param repository Repository para acceso a las operaciones de base de datos
 * @author israel-icm
 * @date 2026-06-12
 */
class ConnectToDatabaseUseCase @Inject constructor(
    private val repository: DatabaseRepository
) {
    
    /**
     * Conecta a una base de datos usando la configuración provista.
     * 
     * @param config Configuración de conexión (tipo, host, port, credenciales)
     * @return Result con Connection si exitoso, DatabaseError si falla
     */
    suspend operator fun invoke(config: ConnectionConfig): Result<Connection> {
        return repository.connect(config)
    }
}
