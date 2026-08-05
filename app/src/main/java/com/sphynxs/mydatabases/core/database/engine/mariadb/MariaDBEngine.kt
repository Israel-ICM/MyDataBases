package com.sphynxs.mydatabases.core.database.engine.mariadb

import android.content.Context
import com.sphynxs.mydatabases.core.database.engine.DatabaseEngine
import com.sphynxs.mydatabases.core.database.engine.DatabaseFeature
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.engine.mysql.MySQLEngine
import com.sphynxs.mydatabases.core.database.models.*
import com.sphynxs.mydatabases.domain.sql.ScriptExecutionProgress
import com.sphynxs.mydatabases.domain.sql.ScriptExecutionSummary
import com.sphynxs.mydatabases.domain.sql.ScriptStatement
import kotlinx.coroutines.flow.Flow

/**
 * Implementación concreta de DatabaseEngine para MariaDB 10.5+.
 * 
 * MariaDB es un fork de MySQL, por lo tanto comparte la mayoría de la lógica.
 * Diferencias principales:
 * - Soporta SEQUENCES (MySQL no)
 * - Thread pool scheduling diferente
 * - Sistema de replicación mejorado
 * 
 * Para v1.0, reutilizamos MySQLEngine con pequeñas adaptaciones.
 * 
 * @param context Contexto de Android para leer certificados SSL
 * @author israel-icm
 * @date 2026-06-12 (updated 2026-06-30 for SSL support)
 */
class MariaDBEngine(context: Context) : DatabaseEngine {
    
    // Delegamos la mayoría de la lógica a MySQLEngine
    private val delegate = MySQLEngine(context)
    
    /**
     * Conecta al servidor MariaDB usando la configuración provista.
     * MariaDB usa el mismo protocolo JDBC que MySQL.
     * 
     * @param config Configuración de conexión (host, port, credenciales, etc.)
     * @return Result con Connection si exitoso, DatabaseError si falla
     */
    override suspend fun connect(config: ConnectionConfig): Result<Connection> {
        // MariaDB usa el mismo protocolo JDBC que MySQL
        return delegate.connect(config.copy(type = DatabaseType.MARIADB))
    }
    
    /**
     * Desconecta del servidor MariaDB y libera todos los recursos del connection pool.
     * 
     * @return Result con Unit si exitoso, DatabaseError si falla
     */
    override suspend fun disconnect(): Result<Unit> = delegate.disconnect()
    
    /**
     * Ejecuta una query SELECT y retorna los resultados.
     * 
     * @param query SQL query con placeholders (?) para prepared statements
     * @param params Parámetros para reemplazar los placeholders
     * @return Result con QueryResult conteniendo columnas y rows
     */
    override suspend fun executeQuery(query: String, params: List<Any>): Result<QueryResult> =
        delegate.executeQuery(query, params)
    
    /**
     * Ejecuta una query INSERT/UPDATE/DELETE y retorna el número de filas afectadas.
     * 
     * @param query SQL query con placeholders (?)
     * @param params Parámetros para prepared statement
     * @return Result con número de filas afectadas
     */
    override suspend fun executeUpdate(query: String, params: List<Any>): Result<Int> =
        delegate.executeUpdate(query, params)
    
    /**
     * Ejecuta múltiples statements SQL en la MISMA conexión.
     * 
     * @param statements Lista de SQL statements a ejecutar
     * @return Result con lista de BatchStatementResult
     */
    override suspend fun executeBatch(statements: List<String>): Result<List<com.sphynxs.mydatabases.domain.usecases.BatchStatementResult>> =
        delegate.executeBatch(statements)

    /**
     * Executes a streamed script the same way as MySQL — MariaDB uses the identical JDBC
     * driver/protocol, delegated to a single implementation point (change `large-sql-script-execution`).
     *
     * @param statements Already-split stream of statements to execute in order
     * @param onProgress Invoked after each statement completes successfully
     * @return Result with the execution summary, or a failure with stopped-at context
     */
    override suspend fun executeScript(
        statements: Flow<ScriptStatement>,
        onProgress: suspend (ScriptExecutionProgress) -> Unit
    ): Result<ScriptExecutionSummary> = delegate.executeScript(statements, onProgress)
    
    /**
     * Lista todas las bases de datos disponibles en el servidor MariaDB.
     * Excluye system databases (information_schema, mysql, performance_schema, sys).
     * 
     * @return Result con lista de Database ordenada alfabéticamente
     */
    override suspend fun getDatabases(): Result<List<Database>> = delegate.getDatabases()
    
    /**
     * Lista todas las tablas y vistas de una base de datos específica.
     * 
     * @param database Nombre de la base de datos
     * @return Result con lista de Table ordenada por nombre
     */
    override suspend fun getTables(database: String): Result<List<Table>> = delegate.getTables(database)
    
    /**
     * Lista todas las columnas de una tabla con metadata completa.
     * 
     * @param table Nombre de la tabla (ej: "users" o "mydb.users")
     * @return Result con lista de Column ordenada por posición
     */
    override suspend fun getColumns(table: String): Result<List<Column>> = delegate.getColumns(table)
    
    /**
     * Lista todos los índices de una tabla.
     * 
     * @param table Nombre de la tabla
     * @return Result con lista de Index
     */
    override suspend fun getIndexes(table: String): Result<List<Index>> = delegate.getIndexes(table)
    
    /**
     * Lista todas las foreign keys de una tabla.
     * 
     * @param table Nombre de la tabla
     * @return Result con lista de ForeignKey
     */
    override suspend fun getForeignKeys(table: String): Result<List<ForeignKey>> = delegate.getForeignKeys(table)
    
    /**
     * Inicia una transacción (deshabilita auto-commit).
     * 
     * @return Result con Transaction para hacer commit/rollback
     */
    override suspend fun beginTransaction(): Result<Transaction> = delegate.beginTransaction()
    
    /**
     * Obtiene la versión del motor MariaDB.
     * 
     * @return Result con string de versión (ej: "10.11.2-MariaDB")
     */
    override suspend fun getVersion(): Result<String> = delegate.getVersion()
    
    /**
     * Retorna el conjunto de features soportadas por MariaDB.
     * 
     * MariaDB soporta todas las features de MySQL PLUS:
     * - SEQUENCES (CREATE SEQUENCE, ALTER SEQUENCE, DROP SEQUENCE)
     * 
     * @return Set de DatabaseFeature con SEQUENCES incluida
     */
    override fun getSupportedFeatures(): Set<DatabaseFeature> = setOf(
        DatabaseFeature.STORED_PROCEDURES,
        DatabaseFeature.TRIGGERS,
        DatabaseFeature.VIEWS,
        DatabaseFeature.EVENTS,
        DatabaseFeature.SEQUENCES,  // ← MariaDB soporta SEQUENCES (diferencia clave vs MySQL)
        DatabaseFeature.FOREIGN_KEYS,
        DatabaseFeature.TRANSACTIONS,
        DatabaseFeature.FULL_TEXT_SEARCH,
        DatabaseFeature.JSON_TYPE
    )
}
