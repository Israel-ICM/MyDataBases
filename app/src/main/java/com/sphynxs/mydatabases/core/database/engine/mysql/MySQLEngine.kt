package com.sphynxs.mydatabases.core.database.engine.mysql

import android.content.Context
import com.sphynxs.mydatabases.core.database.engine.DatabaseEngine
import com.sphynxs.mydatabases.core.database.engine.DatabaseFeature
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.*
import com.sphynxs.mydatabases.domain.sql.ScriptExecutionProgress
import com.sphynxs.mydatabases.domain.sql.ScriptExecutionSummary
import com.sphynxs.mydatabases.domain.sql.ScriptStatement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.sql.SQLException
import java.sql.SQLNonTransientConnectionException
import java.sql.Statement
import java.util.concurrent.atomic.AtomicReference

/**
 * Implementación concreta de DatabaseEngine para MySQL 5.7+, 8.0+.
 * 
 * Features soportadas:
 * - Stored Procedures
 * - Triggers
 * - Views
 * - Events
 * - Foreign Keys
 * - Transactions
 * - Full-text search
 * - JSON type (MySQL 8.0+)
 * - SSL/TLS con certificados personalizados
 * - Autenticación mutua (mTLS)
 * 
 * @param context Contexto de Android para leer certificados SSL
 * @author israel-icm
 * @date 2026-06-12 (updated 2026-06-30 for SSL support)
 */
class MySQLEngine(private val context: Context) : DatabaseEngine {
    
    private var connectionPool: MySQLConnectionPool? = null
    private val metadataReader = MySQLMetadataReader()
    
    /**
     * Conecta al servidor MySQL usando la configuración provista.
     * Crea un connection pool con HikariCP y valida la conexión.
     * 
     * @param config Configuración de conexión (host, port, credenciales, etc.)
     * @return Result con Connection si exitoso, DatabaseError si falla
     * @throws DatabaseError.ConnectionFailed si el host no es alcanzable
     * @throws DatabaseError.AuthenticationFailed si las credenciales son inválidas
     * @throws DatabaseError.TimeoutError si excede el timeout configurado
     */
    override suspend fun connect(config: ConnectionConfig): Result<Connection> = withContext(Dispatchers.IO) {
        try {
            // Validar configuración
            validateConfig(config)
            
            // Crear connection pool con contexto para certificados SSL
            connectionPool = MySQLConnectionPool(config, context)
            
            // Test connection (NO cerrar la conexión JDBC para mantener el túnel SSH activo)
            val testConnection = connectionPool!!.getConnection()
            val version = testConnection.metaData.databaseProductVersion
            // NO cerrar testConnection - se cerrará con connectionPool.close()
            
            Result.success(Connection(
                id = config.id,
                type = DatabaseType.MYSQL,
                database = config.database,
                host = config.host,
                port = config.port,
                username = config.username,
                version = version,
                connectedAt = System.currentTimeMillis()
            ))
        } catch (e: Exception) {
            Result.failure(mapConnectionError(e, config.host))
        }
    }
    
    /**
     * Desconecta del servidor MySQL y libera todos los recursos del connection pool.
     * 
     * @return Result con Unit si exitoso, DatabaseError si falla
     */
    override suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            connectionPool?.close()
            connectionPool = null
        }
    }
    
    /**
     * Ejecuta una query SELECT y retorna los resultados con columnas y filas.
     * 
     * @param query SQL query con placeholders (?) para prepared statements
     * @param params Parámetros para reemplazar los placeholders
     * @return Result con QueryResult conteniendo columnas, rows y execution time
     * @throws DatabaseError.QueryExecutionFailed si la query tiene errores de sintaxis
     * @throws DatabaseError.TimeoutError si excede el read timeout
     */
    override suspend fun executeQuery(
        query: String,
        params: List<Any>
    ): Result<QueryResult> = withContext(Dispatchers.IO) {
        try {
            val connection = connectionPool?.getConnection() 
                ?: throw DatabaseError.ConnectionFailed("No conectado")
            
            val startTime = System.currentTimeMillis()
            
            val queryResult = connection.prepareStatement(query).use { statement ->
                // Bindear parámetros
                params.forEachIndexed { index, param ->
                    statement.setObject(index + 1, param)
                }
                
                statement.executeQuery().use { resultSet ->
                    val columns = (1..resultSet.metaData.columnCount).map { 
                        resultSet.metaData.getColumnName(it) 
                    }
                    
                    val rows = mutableListOf<Map<String, Any?>>()
                    while (resultSet.next()) {
                        val row = columns.associateWith { column ->
                            // Usar getString para leer datos crudos sin conversión de tipo
                            // Esto previene errores con datos corruptos o mal formateados
                            resultSet.getString(column)
                        }
                        rows.add(row)
                    }
                    
                    val executionTime = System.currentTimeMillis() - startTime
                    
                    QueryResult(
                        columns = columns,
                        rows = rows,
                        rowCount = rows.size,
                        executionTimeMs = executionTime
                    )
                }
            }
            
            Result.success(queryResult)
        } catch (e: Exception) {
            Result.failure(mapQueryError(e, query))
        }
    }
    
    /**
     * Ejecuta una query INSERT/UPDATE/DELETE y retorna el número de filas afectadas.
     * 
     * @param query SQL query con placeholders (?)
     * @param params Parámetros para prepared statement
     * @return Result con número de filas afectadas
     * @throws DatabaseError.QueryExecutionFailed si la query falla
     */
    override suspend fun executeUpdate(
        query: String,
        params: List<Any>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val connection = connectionPool?.getConnection() 
                ?: throw DatabaseError.ConnectionFailed("No conectado")
            
            val affectedRows = connection.prepareStatement(query).use { statement ->
                params.forEachIndexed { index, param ->
                    statement.setObject(index + 1, param)
                }
                
                statement.executeUpdate()
            }
            
            Result.success(affectedRows)
        } catch (e: Exception) {
            Result.failure(mapQueryError(e, query))
        }
    }
    
    /**
     * Ejecuta múltiples statements SQL en la MISMA conexión.
     * 
     * Permite que statements como USE DATABASE afecten a los siguientes.
     * Cada statement se ejecuta en secuencia usando la misma conexión del pool.
     * 
     * @param statements Lista de SQL statements a ejecutar
     * @return Result con lista de BatchStatementResult
     * @throws DatabaseError.QueryExecutionFailed si algún statement falla
     */
    override suspend fun executeBatch(
        statements: List<String>
    ): Result<List<com.sphynxs.mydatabases.domain.usecases.BatchStatementResult>> = withContext(Dispatchers.IO) {
        try {
            val connection = connectionPool?.getConnection() 
                ?: throw DatabaseError.ConnectionFailed("No conectado")
            
            val results = mutableListOf<com.sphynxs.mydatabases.domain.usecases.BatchStatementResult>()
            
            // Ejecutar todos los statements en la MISMA conexión
            connection.use { conn ->
                for (statement in statements) {
                    val startTime = System.currentTimeMillis()
                    val trimmed = statement.trim()
                    
                    if (trimmed.isEmpty()) continue
                    
                    // Detectar si es query o update
                    val isQuery = trimmed.uppercase().startsWith("SELECT") ||
                                  trimmed.uppercase().startsWith("SHOW") ||
                                  trimmed.uppercase().startsWith("DESCRIBE") ||
                                  trimmed.uppercase().startsWith("EXPLAIN")
                    
                    if (isQuery) {
                        // SELECT-like: retorna QueryResult
                        val resultSet = conn.createStatement().executeQuery(trimmed)
                        
                        // Leer metadata de columnas
                        val metaData = resultSet.metaData
                        val columnCount = metaData.columnCount
                        val columns = (1..columnCount).map { i ->
                            metaData.getColumnName(i)
                        }
                        
                        // Leer filas
                        val rows = mutableListOf<Map<String, Any?>>()
                        while (resultSet.next()) {
                            val row = columns.associateWith { columnName ->
                                resultSet.getObject(columnName)
                            }
                            rows.add(row)
                        }
                        
                        val queryResult = QueryResult(
                            columns = columns,
                            rows = rows,
                            rowCount = rows.size,
                            executionTimeMs = System.currentTimeMillis() - startTime
                        )
                        
                        results.add(
                            com.sphynxs.mydatabases.domain.usecases.BatchStatementResult(
                                sql = trimmed,
                                queryResult = queryResult,
                                affectedRows = null,
                                executionTimeMs = System.currentTimeMillis() - startTime,
                                isQuery = true
                            )
                        )
                    } else {
                        // INSERT/UPDATE/DELETE/DDL: retorna affected rows
                        val affectedRows = conn.createStatement().executeUpdate(trimmed)
                        
                        results.add(
                            com.sphynxs.mydatabases.domain.usecases.BatchStatementResult(
                                sql = trimmed,
                                queryResult = null,
                                affectedRows = affectedRows,
                                executionTimeMs = System.currentTimeMillis() - startTime,
                                isQuery = false
                            )
                        )
                    }
                }
            }
            
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(mapQueryError(e, "executeBatch"))
        }
    }
    
    /**
     * Executes an already-split [Flow] of statements sequentially on ONE held-open connection,
     * never buffering the source script or SELECT result sets (change `large-sql-script-execution`).
     *
     * SELECT-like statements use a minimal fetch size and only count rows (never `getObject`).
     * The first `SQLException` stops execution immediately (no rollback — DDL causes implicit
     * commits in MySQL/MariaDB, a whole-script rollback would be a lie) and is returned as
     * [Result.failure] with "stopped at statement N (line L)" embedded in the reason text.
     * Cancellation calls [Statement.cancel] on the in-flight JDBC statement from the cancelling
     * coroutine's completion handler, since plain coroutine cancellation cannot interrupt a
     * blocking JDBC call.
     *
     * @param statements Already-split stream of statements to execute in order
     * @param onProgress Invoked after each statement completes successfully
     * @return Result with [ScriptExecutionSummary] on completion, failure with embedded
     *   stopped-at context otherwise
     */
    @OptIn(InternalCoroutinesApi::class)
    override suspend fun executeScript(
        statements: Flow<ScriptStatement>,
        onProgress: suspend (ScriptExecutionProgress) -> Unit
    ): Result<ScriptExecutionSummary> = withContext(Dispatchers.IO) {
        val currentStatement = AtomicReference<Statement?>()
        val cancellationHandle = currentCoroutineContext()[Job]?.invokeOnCompletion(onCancelling = true) { cause ->
            if (cause is CancellationException) {
                runCatching { currentStatement.get()?.cancel() }
            }
        }
        try {
            val connection = connectionPool?.getConnection()
                ?: throw DatabaseError.ConnectionFailed("No conectado")

            var statementIndex = 0
            var executed = 0
            var selectRowsDiscarded = 0L

            connection.use { conn ->
                statements.collect { scriptStatement ->
                    statementIndex++
                    val trimmed = scriptStatement.sql.trim()
                    if (trimmed.isEmpty()) return@collect

                    val jdbcStatement = conn.createStatement()
                    currentStatement.set(jdbcStatement)
                    try {
                        val isSelectLike = trimmed.uppercase().let {
                            it.startsWith("SELECT") || it.startsWith("SHOW") ||
                                it.startsWith("DESCRIBE") || it.startsWith("EXPLAIN")
                        }
                        if (isSelectLike) {
                            jdbcStatement.fetchSize = Int.MIN_VALUE
                            jdbcStatement.executeQuery(trimmed).use { rs ->
                                while (rs.next()) selectRowsDiscarded++
                            }
                        } else {
                            jdbcStatement.executeUpdate(trimmed)
                        }
                        executed++
                        onProgress(ScriptExecutionProgress(statementIndex - 1, scriptStatement.lineNumber, null))
                    } catch (e: SQLException) {
                        throw ScriptExecutionStopped(statementIndex, scriptStatement.lineNumber, e)
                    } finally {
                        currentStatement.set(null)
                        runCatching { jdbcStatement.close() }
                    }
                }
            }

            Result.success(ScriptExecutionSummary(executed, null, selectRowsDiscarded))
        } catch (e: ScriptExecutionStopped) {
            Result.failure(
                DatabaseError.QueryExecutionFailed(
                    query = "statement #${e.statementIndex}",
                    reason = "Stopped at statement ${e.statementIndex} (line ${e.lineNumber}): " +
                        (e.cause?.message ?: "Unknown SQL error")
                )
            )
        } catch (e: Exception) {
            Result.failure(mapQueryError(e, "executeScript"))
        } finally {
            cancellationHandle?.dispose()
        }
    }

    /** Internal signal carrying which statement failed, without losing the native [SQLException]. */
    private class ScriptExecutionStopped(
        val statementIndex: Int,
        val lineNumber: Int,
        cause: SQLException
    ) : Exception(cause)

    /**
     * Lista todas las bases de datos disponibles en el servidor MySQL.
     * Excluye system databases (information_schema, mysql, performance_schema, sys).
     * 
     * @return Result con lista de Database ordenada alfabéticamente
     * @throws DatabaseError.QueryExecutionFailed si falla la query
     */
    override suspend fun getDatabases(): Result<List<Database>> = withContext(Dispatchers.IO) {
        try {
            val query = """
                SELECT 
                    SCHEMA_NAME as name,
                    DEFAULT_CHARACTER_SET_NAME as charset,
                    DEFAULT_COLLATION_NAME as collation
                FROM information_schema.SCHEMATA
                WHERE SCHEMA_NAME NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys')
                ORDER BY SCHEMA_NAME
            """.trimIndent()
            
            val databases = metadataReader.readDatabases(connectionPool!!.getConnection(), query)
            Result.success(databases)
        } catch (e: Exception) {
            Result.failure(mapQueryError(e, "getDatabases"))
        }
    }
    
    /**
     * Lista todas las tablas y vistas de una base de datos específica.
     * 
     * @param database Nombre de la base de datos
     * @return Result con lista de Table ordenada por nombre
     * @throws DatabaseError.QueryExecutionFailed si la database no existe
     */
    override suspend fun getTables(database: String): Result<List<Table>> = withContext(Dispatchers.IO) {
        try {
            val query = """
                SELECT 
                    TABLE_NAME as name,
                    TABLE_TYPE as type,
                    ENGINE as engine,
                    TABLE_ROWS as rowCount,
                    DATA_LENGTH as dataLength,
                    UNIX_TIMESTAMP(CREATE_TIME) * 1000 as createdAt,
                    TABLE_COMMENT as comment
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = ?
                ORDER BY TABLE_NAME
            """.trimIndent()
            
            val tables = metadataReader.readTables(connectionPool!!.getConnection(), query, database)
            Result.success(tables)
        } catch (e: Exception) {
            Result.failure(mapQueryError(e, "getTables"))
        }
    }
    
    /**
     * Lista todas las columnas de una tabla con metadata completa.
     * 
     * @param table Nombre de la tabla (ej: "users" o "mydb.users")
     * @return Result con lista de Column ordenada por posición
     * @throws DatabaseError.QueryExecutionFailed si la tabla no existe
     */
    override suspend fun getColumns(table: String): Result<List<Column>> = withContext(Dispatchers.IO) {
        try {
            val query = """
                SELECT 
                    COLUMN_NAME as name,
                    COLUMN_TYPE as type,
                    IS_NULLABLE as nullable,
                    COLUMN_KEY as `key`,
                    COLUMN_DEFAULT as default_value,
                    EXTRA as extra,
                    COLUMN_COMMENT as comment
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
                ORDER BY ORDINAL_POSITION
            """.trimIndent()
            
            val columns = metadataReader.readColumns(connectionPool!!.getConnection(), query, table)
            Result.success(columns)
        } catch (e: Exception) {
            Result.failure(mapQueryError(e, "getColumns"))
        }
    }
    
    /**
     * Lista todos los índices de una tabla.
     * 
     * @param table Nombre de la tabla
     * @return Result con lista de Index (índices compuestos agrupados)
     * @throws DatabaseError.QueryExecutionFailed si la tabla no existe
     */
    override suspend fun getIndexes(table: String): Result<List<Index>> = withContext(Dispatchers.IO) {
        try {
            val query = """
                SELECT 
                    INDEX_NAME as name,
                    COLUMN_NAME as column,
                    NON_UNIQUE as nonUnique,
                    INDEX_TYPE as type,
                    SEQ_IN_INDEX as position
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
                ORDER BY INDEX_NAME, SEQ_IN_INDEX
            """.trimIndent()
            
            val indexes = metadataReader.readIndexes(connectionPool!!.getConnection(), query, table)
            Result.success(indexes)
        } catch (e: Exception) {
            Result.failure(mapQueryError(e, "getIndexes"))
        }
    }
    
    /**
     * Lista todas las foreign keys de una tabla.
     * 
     * @param table Nombre de la tabla
     * @return Result con lista de ForeignKey con acciones de integridad referencial
     * @throws DatabaseError.QueryExecutionFailed si la tabla no existe
     */
    override suspend fun getForeignKeys(table: String): Result<List<ForeignKey>> = withContext(Dispatchers.IO) {
        try {
            val query = """
                SELECT 
                    CONSTRAINT_NAME as name,
                    COLUMN_NAME as column,
                    REFERENCED_TABLE_NAME as referencedTable,
                    REFERENCED_COLUMN_NAME as referencedColumn,
                    DELETE_RULE as onDelete,
                    UPDATE_RULE as onUpdate
                FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE() 
                  AND TABLE_NAME = ?
                  AND REFERENCED_TABLE_NAME IS NOT NULL
            """.trimIndent()
            
            val foreignKeys = metadataReader.readForeignKeys(connectionPool!!.getConnection(), query, table)
            Result.success(foreignKeys)
        } catch (e: Exception) {
            Result.failure(mapQueryError(e, "getForeignKeys"))
        }
    }
    
    /**
     * Inicia una transacción deshabilitando auto-commit.
     * 
     * @return Result con Transaction para hacer commit/rollback manual
     * @throws DatabaseError.ConnectionFailed si no hay conexión activa
     */
    override suspend fun beginTransaction(): Result<Transaction> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = connectionPool?.getConnection() 
                ?: throw DatabaseError.ConnectionFailed("No conectado")
            
            connection.autoCommit = false
            
            Transaction(
                connection = connection,
                onCommit = { connection.commit(); connection.autoCommit = true },
                onRollback = { connection.rollback(); connection.autoCommit = true }
            )
        }
    }
    
    /**
     * Retorna el conjunto de features soportadas por MySQL.
     * Útil para habilitar/deshabilitar funcionalidad en la UI según el motor.
     * 
     * @return Set de DatabaseFeature soportadas por MySQL
     */
    override fun getSupportedFeatures(): Set<DatabaseFeature> = setOf(
        DatabaseFeature.STORED_PROCEDURES,
        DatabaseFeature.TRIGGERS,
        DatabaseFeature.VIEWS,
        DatabaseFeature.EVENTS,
        DatabaseFeature.FOREIGN_KEYS,
        DatabaseFeature.TRANSACTIONS,
        DatabaseFeature.FULL_TEXT_SEARCH,
        DatabaseFeature.JSON_TYPE
    )
    
    /**
     * Obtiene la versión del servidor MySQL ejecutando SELECT VERSION().
     * 
     * @return Result con string de versión (ej: "8.0.33", "5.7.42")
     * @throws DatabaseError.ConnectionFailed si no hay conexión activa
     */
    override suspend fun getVersion(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val connection = connectionPool?.getConnection() 
                ?: throw DatabaseError.ConnectionFailed("No conectado")
            
            val version = connection.createStatement().use { statement ->
                statement.executeQuery("SELECT VERSION()").use { resultSet ->
                    if (resultSet.next()) {
                        resultSet.getString(1)
                    } else {
                        "Unknown"
                    }
                }
            }
            
            Result.success(version)
        } catch (e: Exception) {
            Result.failure(mapQueryError(e, "getVersion"))
        }
    }
    
    /**
     * Valida que la configuración de conexión tenga todos los campos requeridos.
     * 
     * @param config Configuración a validar
     * @throws IllegalArgumentException si algún campo es inválido
     */
    private fun validateConfig(config: ConnectionConfig) {
        require(config.host.isNotBlank()) { "Host no puede estar vacío" }
        require(config.port in 1..65535) { "Port debe estar entre 1 y 65535" }
        require(config.username.isNotBlank()) { "Username no puede estar vacío" }
    }
    
    /**
     * Función pura que mapea excepciones de conexión a DatabaseError específicos.
     * 
     * @param throwable Excepción original del JDBC driver
     * @param host Hostname usado en la conexión (para mensajes de error)
     * @return DatabaseError apropiado según el tipo de excepción
     */
    private fun mapConnectionError(throwable: Throwable, host: String): DatabaseError {
        return when {
            throwable is SQLNonTransientConnectionException -> 
                DatabaseError.ConnectionFailed("Host '$host' no alcanzable")
            
            throwable is SQLException && throwable.message?.contains("Access denied") == true ->
                DatabaseError.AuthenticationFailed("Usuario o contraseña incorrectos")

            throwable is SQLException && throwable.message?.contains("Communications link failure") == true ->
                DatabaseError.ConnectionFailed("No se pudo establecer comunicación con '$host'")

            throwable is SQLException && throwable.message?.contains("Connection refused") == true ->
                DatabaseError.ConnectionFailed("El servidor '$host' rechazó la conexión")

            throwable is SQLException && throwable.message?.contains("Unknown host") == true ->
                DatabaseError.ConnectionFailed("Host '$host' no encontrado")
            
            throwable is SocketTimeoutException ->
                DatabaseError.TimeoutError("Timeout conectando a $host")
            
            else ->
                DatabaseError.UnknownError(throwable)
        }
    }

    /**
     * Función pura que mapea excepciones de query a DatabaseError específicos.
     * 
     * @param throwable Excepción original del JDBC driver
     * @param context Contexto de la operación (query o nombre de método)
     * @return DatabaseError apropiado
     */
    private fun mapQueryError(throwable: Throwable, context: String): Throwable {
        return when (throwable) {
            is SQLException -> DatabaseError.QueryExecutionFailed(context, throwable.message ?: "Error desconocido")
            else -> throwable
        }
    }
    
    /**
     * Obtiene todos los character sets disponibles en el servidor MySQL.
     *
     * Ejecuta `SHOW CHARACTER SET` y parsea los resultados.
     *
     * @return Result con lista de CharacterSet
     * @throws DatabaseError.ConnectionFailed si no hay conexión activa
     * @throws DatabaseError.QueryExecutionFailed si la query falla
     *
     * @author israel-icm
     * @date 2026-06-19
     */
    suspend fun getCharacterSets(): Result<List<CharacterSet>> = withContext(Dispatchers.IO) {
        try {
            val connection = connectionPool?.getConnection()
                ?: throw DatabaseError.ConnectionFailed("No conectado")
            
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery("SHOW CHARACTER SET")
            
            val charsets = mutableListOf<CharacterSet>()
            while (resultSet.next()) {
                charsets.add(
                    CharacterSet(
                        name = resultSet.getString("Charset"),
                        description = resultSet.getString("Description"),
                        defaultCollation = resultSet.getString("Default collation"),
                        maxLength = resultSet.getInt("Maxlen")
                    )
                )
            }
            
            resultSet.close()
            statement.close()
            connection.close()
            
            Result.success(charsets)
        } catch (e: Exception) {
            Result.failure(mapQueryError(e, "SHOW CHARACTER SET"))
        }
    }
    
    /**
     * Obtiene todas las collations disponibles para un character set específico.
     *
     * Ejecuta `SHOW COLLATION WHERE Charset = ?` y parsea los resultados.
     *
     * @param charset Nombre del character set (ej: utf8mb4)
     * @return Result con lista de Collation
     * @throws DatabaseError.ConnectionFailed si no hay conexión activa
     * @throws DatabaseError.QueryExecutionFailed si la query falla
     *
     * @author israel-icm
     * @date 2026-06-19
     */
    suspend fun getCollations(charset: String): Result<List<Collation>> = withContext(Dispatchers.IO) {
        try {
            val connection = connectionPool?.getConnection()
                ?: throw DatabaseError.ConnectionFailed("No conectado")
            
            val statement = connection.prepareStatement("SHOW COLLATION WHERE Charset = ?")
            statement.setString(1, charset)
            val resultSet = statement.executeQuery()
            
            val collations = mutableListOf<Collation>()
            while (resultSet.next()) {
                collations.add(
                    Collation(
                        name = resultSet.getString("Collation"),
                        charset = resultSet.getString("Charset"),
                        id = resultSet.getInt("Id"),
                        isDefault = resultSet.getString("Default") == "Yes"
                    )
                )
            }
            
            resultSet.close()
            statement.close()
            connection.close()
            
            Result.success(collations)
        } catch (e: Exception) {
            Result.failure(mapQueryError(e, "SHOW COLLATION WHERE Charset = '$charset'"))
        }
    }
}
