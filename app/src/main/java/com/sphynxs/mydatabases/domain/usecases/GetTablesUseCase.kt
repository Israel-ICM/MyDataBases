package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.models.Table
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import javax.inject.Inject

/**
 * Use Case para obtener la lista de tablas de una base de datos.
 * 
 * Delega la operación al repository y retorna la lista ordenada por nombre.
 * 
 * @param repository Repository para acceso a las operaciones de base de datos
 * @author israel-icm
 * @date 2026-06-12
 */
class GetTablesUseCase @Inject constructor(
    private val repository: DatabaseRepository
) {
    
    /**
     * Lista todas las tablas y vistas de una base de datos específica.
     * 
     * @param database Nombre de la base de datos
     * @return Result con lista de Table ordenada por nombre
     * @throws DatabaseError.ConnectionFailed si no hay motor conectado
     */
    suspend operator fun invoke(database: String): Result<List<Table>> {
        return repository.getTables(database)
    }
}
