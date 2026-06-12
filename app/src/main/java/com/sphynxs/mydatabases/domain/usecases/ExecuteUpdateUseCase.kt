package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import javax.inject.Inject

/**
 * Use Case para ejecutar queries INSERT/UPDATE/DELETE.
 * 
 * Delega la ejecución al repository y retorna el número de filas afectadas.
 * 
 * @param repository Repository para acceso a las operaciones de base de datos
 * @author israel-icm
 * @date 2026-06-12
 */
class ExecuteUpdateUseCase @Inject constructor(
    private val repository: DatabaseRepository
) {
    
    /**
     * Ejecuta una query INSERT/UPDATE/DELETE.
     * 
     * @param query SQL query con placeholders (?) para prepared statements
     * @param params Parámetros para reemplazar los placeholders (opcional)
     * @return Result con número de filas afectadas
     * @throws DatabaseError.ConnectionFailed si no hay motor conectado
     */
    suspend operator fun invoke(query: String, params: List<Any> = emptyList()): Result<Int> {
        return repository.executeUpdate(query, params)
    }
}
