package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.models.QueryResult
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import javax.inject.Inject

/**
 * Use Case para ejecutar múltiples statements SQL en la MISMA conexión.
 * 
 * Esto permite que statements como USE DATABASE afecten a los statements siguientes,
 * manteniendo el contexto de base de datos activa.
 * 
 * @param repository Repository para acceso a las operaciones de base de datos
 * @author israel-icm
 * @date 2026-06-23
 */
class ExecuteBatchStatementsUseCase @Inject constructor(
    private val repository: DatabaseRepository
) {
    
    /**
     * Ejecuta una lista de statements SQL en secuencia usando la MISMA conexión.
     * 
     * Cada statement puede ser SELECT (retorna QueryResult) o INSERT/UPDATE/DELETE/DDL
     * (retorna affectedRows).
     * 
     * @param statements Lista de SQL statements a ejecutar
     * @return Result con lista de BatchStatementResult
     */
    suspend operator fun invoke(statements: List<String>): Result<List<BatchStatementResult>> {
        return repository.executeBatch(statements)
    }
}

/**
 * Resultado de ejecutar un statement dentro de un batch.
 * 
 * @property sql Statement SQL ejecutado
 * @property queryResult Result de SELECT (null si no es query)
 * @property affectedRows Filas afectadas por INSERT/UPDATE/DELETE (null si es query)
 * @property executionTimeMs Tiempo de ejecución en ms
 * @property isQuery true si es SELECT-like, false si es INSERT/UPDATE/DELETE/DDL
 */
data class BatchStatementResult(
    val sql: String,
    val queryResult: QueryResult? = null,
    val affectedRows: Int? = null,
    val executionTimeMs: Long,
    val isQuery: Boolean
)
