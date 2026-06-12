package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.models.Database
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import javax.inject.Inject

/**
 * Use Case para obtener la lista de bases de datos disponibles.
 * 
 * Delega la operación al repository y retorna la lista ordenada alfabéticamente.
 * 
 * @param repository Repository para acceso a las operaciones de base de datos
 * @author israel-icm
 * @date 2026-06-12
 */
class GetDatabasesUseCase @Inject constructor(
    private val repository: DatabaseRepository
) {
    
    /**
     * Lista todas las bases de datos disponibles en el servidor.
     * 
     * Excluye system databases (information_schema, mysql, performance_schema, sys).
     * 
     * @return Result con lista de Database ordenada alfabéticamente
     * @throws DatabaseError.ConnectionFailed si no hay motor conectado
     */
    suspend operator fun invoke(): Result<List<Database>> {
        return repository.getDatabases()
    }
}
