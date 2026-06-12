package com.sphynxs.mydatabases.core.database.engine.mysql

import com.sphynxs.mydatabases.core.database.engine.DatabaseEngine
import com.sphynxs.mydatabases.core.database.engine.DatabaseFeature
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.sql.SQLException
import java.sql.SQLNonTransientConnectionException

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
 * 
 * @author israel-icm
 * @date 2026-06-12
 */
class MySQLEngine : DatabaseEngine {
    
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
        runCatching {
            // Validar configuración
            validateConfig(config)
            
            // Crear connection pool
            connectionPool = MySQLConnectionPool(config)
            
            // Test connection
            val testConnection = connectionPool!!.getConnection()
            val version = testConnection.metaData.databaseProductVersion
            testConnection.close()
            
            Connection(
                id = config.id,
                type = DatabaseType.MYSQL,
                database = config.database,
                host = config.host,
                port = config.port,
                username = config.username,
                version = version,
                connectedAt = System.currentTimeMillis()
            )
        }.recoverCatching { throwable ->
            throw mapConnectionError(throwable, config.host)
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
        runCatching {
            val connection = connectionPool?.getConnection() 
                ?: throw DatabaseError.ConnectionFailed("No conectado")
            
            val startTime = System.currentTimeMillis()
            
            connection.prepareStatement(query).use { statement ->
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
                            resultSet.getObject(column)
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
        }.recoverCatching { throwable ->
            throw mapQueryError(throwable, query)
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
        runCatching {
            val connection = connectionPool?.getConnection() 
                ?: throw DatabaseError.ConnectionFailed("No conectado")
            
            connection.prepareStatement(query).use { statement ->
                params.forEachIndexed { index, param ->
                    statement.setObject(index + 1, param)
                }
                
                statement.executeUpdate()
            }
        }.recoverCatching { throwable ->
            throw mapQueryError(throwable, query)
        }
    }
    
    /**
     * Lista todas las bases de datos disponibles en el servidor MySQL.
     * Excluye system databases (information_schema, mysql, performance_schema, sys).
     * 
     * @return Result con lista de Database ordenada alfabéticamente
     * @throws DatabaseError.QueryExecutionFailed si falla la query
     */
    override suspend fun getDatabases(): Result<List<Database>> = withContext(Dispatchers.IO) {
        runCatching {
            val query = """
                SELECT 
                    SCHEMA_NAME as name,
                    DEFAULT_CHARACTER_SET_NAME as charset,
                    DEFAULT_COLLATION_NAME as collation
                FROM information_schema.SCHEMATA
                WHERE SCHEMA_NAME NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys')
                ORDER BY SCHEMA_NAME
            """.trimIndent()
            
            metadataReader.readDatabases(connectionPool!!.getConnection(), query)
        }.recoverCatching { throwable ->
            throw mapQueryError(throwable, "getDatabases")
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
        runCatching {
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
            
            metadataReader.readTables(connectionPool!!.getConnection(), query, database)
        }.recoverCatching { throwable ->
            throw mapQueryError(throwable, "getTables")
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
        runCatching {
            val query = """
                SELECT 
                    COLUMN_NAME as name,
                    COLUMN_TYPE as type,
                    IS_NULLABLE as nullable,
                    COLUMN_KEY as key,
                    COLUMN_DEFAULT as default_value,
                    EXTRA as extra,
                    COLUMN_COMMENT as comment
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
                ORDER BY ORDINAL_POSITION
            """.trimIndent()
            
            metadataReader.readColumns(connectionPool!!.getConnection(), query, table)
        }.recoverCatching { throwable ->
            throw mapQueryError(throwable, "getColumns")
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
        runCatching {
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
            
            metadataReader.readIndexes(connectionPool!!.getConnection(), query, table)
        }.recoverCatching { throwable ->
            throw mapQueryError(throwable, "getIndexes")
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
        runCatching {
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
            
            metadataReader.readForeignKeys(connectionPool!!.getConnection(), query, table)
        }.recoverCatching { throwable ->
            throw mapQueryError(throwable, "getForeignKeys")
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
        runCatching {
            val connection = connectionPool?.getConnection() 
                ?: throw DatabaseError.ConnectionFailed("No conectado")
            
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT VERSION()").use { resultSet ->
                    if (resultSet.next()) {
                        resultSet.getString(1)
                    } else {
                        "Unknown"
                    }
                }
            }
        }.recoverCatching { throwable ->
            throw mapQueryError(throwable, "getVersion")
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
        require(config.database.isNotBlank()) { "Database no puede estar vacío" }
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
}
