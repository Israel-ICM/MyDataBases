package com.sphynxs.mydatabases.core.database.repository

import android.content.Context
import com.sphynxs.mydatabases.core.database.engine.DatabaseEngine
import com.sphynxs.mydatabases.core.database.engine.DatabaseEngineFactory
import com.sphynxs.mydatabases.core.database.engine.DatabaseFeature
import com.sphynxs.mydatabases.core.database.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Implementación del repository usando DatabaseEngine.
 * 
 * Mantiene una referencia al motor actual y delega todas las operaciones.
 * Si no hay motor conectado, retorna DatabaseError.ConnectionFailed.
 * 
 * @param context Contexto de aplicación para leer certificados SSL
 * @param engineFactory Factory para crear instancias de DatabaseEngine
 * @author israel-icm
 * @date 2026-06-12 (updated 2026-06-30 for SSL support)
 */
class DatabaseRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engineFactory: DatabaseEngineFactory
) : DatabaseRepository {
    
    private var currentEngine: DatabaseEngine? = null
    
    override suspend fun connect(config: ConnectionConfig): Result<Connection> {
        currentEngine = engineFactory.create(config.type, context)
        return currentEngine!!.connect(config)
    }
    
    override suspend fun disconnect(): Result<Unit> {
        return currentEngine?.disconnect() ?: Result.success(Unit)
    }
    
    override suspend fun executeQuery(query: String, params: List<Any>): Result<QueryResult> {
        return currentEngine?.executeQuery(query, params)
            ?: Result.failure(DatabaseError.ConnectionFailed("No conectado"))
    }
    
    override suspend fun executeUpdate(query: String, params: List<Any>): Result<Int> {
        return currentEngine?.executeUpdate(query, params)
            ?: Result.failure(DatabaseError.ConnectionFailed("No conectado"))
    }
    
    override suspend fun executeBatch(statements: List<String>): Result<List<com.sphynxs.mydatabases.domain.usecases.BatchStatementResult>> {
        return currentEngine?.executeBatch(statements)
            ?: Result.failure(DatabaseError.ConnectionFailed("No conectado"))
    }
    
    override suspend fun getDatabases(): Result<List<Database>> {
        return currentEngine?.getDatabases()
            ?: Result.failure(DatabaseError.ConnectionFailed("No conectado"))
    }
    
    override suspend fun getTables(database: String): Result<List<Table>> {
        return currentEngine?.getTables(database)
            ?: Result.failure(DatabaseError.ConnectionFailed("No conectado"))
    }
    
    override suspend fun getColumns(table: String): Result<List<Column>> {
        return currentEngine?.getColumns(table)
            ?: Result.failure(DatabaseError.ConnectionFailed("No conectado"))
    }
    
    override suspend fun getIndexes(table: String): Result<List<Index>> {
        return currentEngine?.getIndexes(table)
            ?: Result.failure(DatabaseError.ConnectionFailed("No conectado"))
    }
    
    override suspend fun getForeignKeys(table: String): Result<List<ForeignKey>> {
        return currentEngine?.getForeignKeys(table)
            ?: Result.failure(DatabaseError.ConnectionFailed("No conectado"))
    }
    
    override suspend fun beginTransaction(): Result<Transaction> {
        return currentEngine?.beginTransaction()
            ?: Result.failure(DatabaseError.ConnectionFailed("No conectado"))
    }
    
    override suspend fun getVersion(): Result<String> {
        return currentEngine?.getVersion()
            ?: Result.failure(DatabaseError.ConnectionFailed("No conectado"))
    }
    
    override fun getSupportedFeatures(): Set<DatabaseFeature> {
        return currentEngine?.getSupportedFeatures() ?: emptySet()
    }
    
    override suspend fun getCharacterSets(): Result<List<CharacterSet>> {
        val engine = currentEngine as? com.sphynxs.mydatabases.core.database.engine.mysql.MySQLEngine
            ?: return Result.failure(DatabaseError.ConnectionFailed("No conectado a MySQL"))
        
        return engine.getCharacterSets()
    }
    
    override suspend fun getCollations(charset: String): Result<List<Collation>> {
        val engine = currentEngine as? com.sphynxs.mydatabases.core.database.engine.mysql.MySQLEngine
            ?: return Result.failure(DatabaseError.ConnectionFailed("No conectado a MySQL"))
        
        return engine.getCollations(charset)
    }
}
