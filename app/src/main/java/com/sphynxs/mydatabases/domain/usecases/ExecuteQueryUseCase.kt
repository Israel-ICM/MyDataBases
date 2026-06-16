package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.models.QueryResult
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import javax.inject.Inject

/**
 * Use Case para ejecutar queries SELECT.
 * 
 * Delega la ejecución al repository y retorna los resultados obtenidos.
 * 
 * @param repository Repository para acceso a las operaciones de base de datos
 * @author israel-icm
 * @date 2026-06-12
 */
class ExecuteQueryUseCase @Inject constructor(
    private val repository: DatabaseRepository
) {
    
    /**
     * Ejecuta una query SELECT y retorna los resultados.
     * 
     * @param query SQL query con placeholders (?) para prepared statements
     * @param params Parámetros para reemplazar los placeholders (opcional)
     * @return Result con QueryResult conteniendo columnas y rows
     * @throws DatabaseError.ConnectionFailed si no hay motor conectado
     */
    suspend operator fun invoke(query: String, params: List<Any> = emptyList()): Result<QueryResult> {
        return repository.executeQuery(query, params)
    }
}
