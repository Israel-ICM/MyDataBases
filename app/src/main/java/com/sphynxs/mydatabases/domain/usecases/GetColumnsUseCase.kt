package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.models.Column
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import javax.inject.Inject

/**
 * Use Case para obtener la lista de columnas de una tabla.
 * 
 * Delega la operación al repository y retorna la lista ordenada por posición.
 * 
 * @param repository Repository para acceso a las operaciones de base de datos
 * @author israel-icm
 * @date 2026-06-12
 */
class GetColumnsUseCase @Inject constructor(
    private val repository: DatabaseRepository
) {
    
    /**
     * Lista todas las columnas de una tabla con metadata completa.
     * 
     * @param table Nombre de la tabla (ej: "users" o "mydb.users")
     * @return Result con lista de Column ordenada por posición
     * @throws DatabaseError.ConnectionFailed si no hay motor conectado
     */
    suspend operator fun invoke(table: String): Result<List<Column>> {
        return repository.getColumns(table)
    }
}
