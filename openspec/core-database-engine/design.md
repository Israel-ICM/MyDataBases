# Design: Core Database Engine

**Fecha**: 2026-06-11  
**Autor**: israel-icm  
**Change**: core-database-engine  
**Status**: Draft  

---

## 1. Architecture Overview

### 1.1 Clean Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                      Presentation Layer                      │
│  (Compose UI, ViewModels, Navigation)                        │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                       Domain Layer                           │
│  (Use Cases, Repository Interface, Models)                   │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                        Data Layer                            │
│  (DatabaseRepository, DatabaseEngine, Room)                  │
│                                                              │
│  ┌────────────────────────────────────────────────┐         │
│  │         DatabaseEngineFactory                  │         │
│  └────────────────────────────────────────────────┘         │
│                     │                                        │
│        ┌────────────┼────────────┐                          │
│        ▼            ▼            ▼                          │
│  MySQLEngine  MariaDBEngine  PostgreSQLEngine              │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Module Structure

### 2.1 Package Organization

```
com.sphynxs.mydatabases/
├── core/
│   └── database/
│       ├── engine/
│       │   ├── DatabaseEngine.kt               # Interface principal
│       │   ├── DatabaseEngineFactory.kt        # Factory
│       │   ├── DatabaseType.kt                 # Enum de motores
│       │   ├── DatabaseFeature.kt              # Enum de features
│       │   ├── mysql/
│       │   │   ├── MySQLEngine.kt
│       │   │   ├── MySQLConnectionPool.kt
│       │   │   ├── MySQLQueryBuilder.kt
│       │   │   └── MySQLMetadataReader.kt
│       │   └── mariadb/
│       │       ├── MariaDBEngine.kt
│       │       └── MariaDBConnectionPool.kt
│       ├── models/
│       │   ├── Connection.kt
│       │   ├── ConnectionConfig.kt
│       │   ├── QueryResult.kt
│       │   ├── Table.kt
│       │   ├── Column.kt
│       │   ├── Database.kt
│       │   ├── Index.kt
│       │   ├── ForeignKey.kt
│       │   ├── Transaction.kt
│       │   └── DatabaseError.kt
│       ├── repository/
│       │   ├── DatabaseRepository.kt           # Interface
│       │   └── DatabaseRepositoryImpl.kt       # Implementación
│       └── di/
│           └── DatabaseModule.kt               # Hilt module
├── data/
│   └── local/
│       ├── dao/
│       │   └── ConnectionConfigDao.kt          # Room DAO
│       └── entities/
│           └── ConnectionConfigEntity.kt       # Room Entity
└── domain/
    └── usecases/
        ├── ConnectToDatabaseUseCase.kt
        ├── ExecuteQueryUseCase.kt
        ├── GetTablesUseCase.kt
        └── GetColumnsUseCase.kt
```

---

## 3. Core Components Design

### 3.1 DatabaseEngine Interface

```kotlin
/**
 * Interface que define las operaciones comunes para todos los motores de bases de datos.
 * Implementaciones concretas: MySQLEngine, MariaDBEngine, PostgreSQLEngine, SQLiteEngine.
 * 
 * @author israel-icm
 * @date 2026-06-11
 */
interface DatabaseEngine {
    
    /**
     * Conecta al servidor de base de datos usando la configuración provista.
     * 
     * @param config Configuración de conexión (host, port, credentials, etc.)
     * @return Result con Connection si exitoso, DatabaseError si falla
     * @throws DatabaseError.ConnectionFailed si el host no es alcanzable
     * @throws DatabaseError.AuthenticationFailed si las credenciales son inválidas
     * @throws DatabaseError.TimeoutError si excede el timeout configurado
     */
    suspend fun connect(config: ConnectionConfig): Result<Connection>
    
    /**
     * Desconecta del servidor y libera todos los recursos (conexiones pool).
     * 
     * @return Result con Unit si exitoso, DatabaseError si falla
     */
    suspend fun disconnect(): Result<Unit>
    
    /**
     * Ejecuta una query SELECT y retorna los resultados.
     * 
     * @param query SQL query con placeholders (?) para prepared statements
     * @param params Parámetros para reemplazar los placeholders
     * @return Result con QueryResult conteniendo columnas y rows
     * @throws DatabaseError.QueryExecutionFailed si la query tiene errores de sintaxis
     * @throws DatabaseError.TimeoutError si excede el read timeout
     */
    suspend fun executeQuery(
        query: String,
        params: List<Any> = emptyList()
    ): Result<QueryResult>
    
    /**
     * Ejecuta una query INSERT/UPDATE/DELETE y retorna el número de filas afectadas.
     * 
     * @param query SQL query con placeholders (?)
     * @param params Parámetros para prepared statement
     * @return Result con número de filas afectadas
     */
    suspend fun executeUpdate(
        query: String,
        params: List<Any> = emptyList()
    ): Result<Int>
    
    /**
     * Lista todas las bases de datos disponibles en el servidor.
     * Excluye system databases (information_schema, mysql, performance_schema, sys).
     * 
     * @return Result con lista de Database ordenada alfabéticamente
     */
    suspend fun getDatabases(): Result<List<Database>>
    
    /**
     * Lista todas las tablas y vistas de una base de datos específica.
     * 
     * @param database Nombre de la base de datos
     * @return Result con lista de Table ordenada por nombre
     */
    suspend fun getTables(database: String): Result<List<Table>>
    
    /**
     * Lista todas las columnas de una tabla con metadata completa.
     * 
     * @param table Nombre de la tabla (ej: "users" o "mydb.users")
     * @return Result con lista de Column ordenada por posición
     */
    suspend fun getColumns(table: String): Result<List<Column>>
    
    /**
     * Lista todos los índices de una tabla.
     * 
     * @param table Nombre de la tabla
     * @return Result con lista de Index
     */
    suspend fun getIndexes(table: String): Result<List<Index>>
    
    /**
     * Lista todas las foreign keys de una tabla.
     * 
     * @param table Nombre de la tabla
     * @return Result con lista de ForeignKey
     */
    suspend fun getForeignKeys(table: String): Result<List<ForeignKey>>
    
    /**
     * Inicia una transacción (deshabilita auto-commit).
     * 
     * @return Result con Transaction para hacer commit/rollback
     * @throws DatabaseError.UnsupportedFeature si el motor no soporta transacciones
     */
    suspend fun beginTransaction(): Result<Transaction>
    
    /**
     * Retorna el conjunto de features soportadas por este motor.
     * Útil para habilitar/deshabilitar funcionalidad en la UI.
     * 
     * @return Set de DatabaseFeature
     */
    fun getSupportedFeatures(): Set<DatabaseFeature>
    
    /**
     * Obtiene la versión del motor de base de datos.
     * 
     * @return Result con string de versión (ej: "8.0.33", "10.11.2-MariaDB")
     */
    suspend fun getVersion(): Result<String>
}
```

---

### 3.2 DatabaseEngineFactory

```kotlin
/**
 * Factory para crear instancias de DatabaseEngine según el tipo de motor.
 * 
 * Patrón: Factory Method
 * Propósito: Centralizar la creación de engines y facilitar testing (mockeable).
 * 
 * @author israel-icm
 * @date 2026-06-11
 */
object DatabaseEngineFactory {
    
    /**
     * Crea una instancia de DatabaseEngine según el tipo especificado.
     * 
     * @param type Tipo de motor (MYSQL, MARIADB, POSTGRESQL, SQLITE)
     * @return Instancia concreta de DatabaseEngine
     * @throws IllegalArgumentException si el tipo no está implementado
     */
    fun create(type: DatabaseType): DatabaseEngine {
        return when (type) {
            DatabaseType.MYSQL -> MySQLEngine()
            DatabaseType.MARIADB -> MariaDBEngine()
            DatabaseType.POSTGRESQL -> throw NotImplementedError("PostgreSQL será implementado en v1.1")
            DatabaseType.SQLITE -> throw NotImplementedError("SQLite será implementado en v1.1")
        }
    }
}
```

---

### 3.3 MySQLEngine Implementation

```kotlin
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
 * @date 2026-06-11
 */
class MySQLEngine : DatabaseEngine {
    
    private var connectionPool: MySQLConnectionPool? = null
    private val metadataReader = MySQLMetadataReader()
    
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
            // Mapear excepciones específicas
            when {
                throwable is SQLNonTransientConnectionException -> 
                    throw DatabaseError.ConnectionFailed("Host '${config.host}' no alcanzable")
                
                throwable is SQLException && throwable.message?.contains("Access denied") == true ->
                    throw DatabaseError.AuthenticationFailed("Usuario o contraseña incorrectos")
                
                throwable is SocketTimeoutException ->
                    throw DatabaseError.TimeoutError("Timeout conectando a ${config.host}")
                
                else ->
                    throw DatabaseError.UnknownError(throwable)
            }
        }
    }
    
    override suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            connectionPool?.close()
            connectionPool = null
        }
    }
    
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
            when (throwable) {
                is SQLException -> throw DatabaseError.QueryExecutionFailed(query, throwable.message ?: "Error desconocido")
                else -> throw throwable
            }
        }
    }
    
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
        }
    }
    
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
        }
    }
    
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
        }
    }
    
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
        }
    }
    
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
        }
    }
    
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
        }
    }
    
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
        }
    }
    
    private fun validateConfig(config: ConnectionConfig) {
        require(config.host.isNotBlank()) { "Host no puede estar vacío" }
        require(config.port in 1..65535) { "Port debe estar entre 1 y 65535" }
        require(config.database.isNotBlank()) { "Database no puede estar vacío" }
        require(config.username.isNotBlank()) { "Username no puede estar vacío" }
    }
}
```

---

### 3.4 MySQLConnectionPool

```kotlin
/**
 * Connection pool para MySQL usando HikariCP.
 * 
 * @param config Configuración de conexión
 * @author israel-icm
 * @date 2026-06-11
 */
class MySQLConnectionPool(private val config: ConnectionConfig) {
    
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = "jdbc:mysql://${config.host}:${config.port}/${config.database}"
        username = config.username
        password = config.password // TODO: Decrypt from Android Keystore
        
        // Pool settings
        maximumPoolSize = config.maxPoolSize
        minimumIdle = 2
        connectionTimeout = config.connectionTimeout
        idleTimeout = 600_000L // 10 minutos
        maxLifetime = 1_800_000L // 30 minutos
        
        // SSL settings
        if (config.useSSL) {
            addDataSourceProperty("useSSL", "true")
            addDataSourceProperty("requireSSL", "true")
        }
        
        // Performance settings
        addDataSourceProperty("cachePrepStmts", "true")
        addDataSourceProperty("prepStmtCacheSize", "250")
        addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        addDataSourceProperty("useServerPrepStmts", "true")
        addDataSourceProperty("useLocalSessionState", "true")
        addDataSourceProperty("rewriteBatchedStatements", "true")
        addDataSourceProperty("cacheResultSetMetadata", "true")
        addDataSourceProperty("cacheServerConfiguration", "true")
        addDataSourceProperty("elideSetAutoCommits", "true")
        addDataSourceProperty("maintainTimeStats", "false")
    }
    
    private val dataSource = HikariDataSource(hikariConfig)
    
    /**
     * Obtiene una conexión del pool.
     * HikariCP maneja automáticamente el pooling y timeout.
     */
    suspend fun getConnection(): java.sql.Connection = withContext(Dispatchers.IO) {
        dataSource.connection
    }
    
    /**
     * Cierra el pool y libera todas las conexiones.
     */
    fun close() {
        if (!dataSource.isClosed) {
            dataSource.close()
        }
    }
}
```

---

### 3.5 MySQLMetadataReader

```kotlin
/**
 * Helper para leer metadata de MySQL usando information_schema.
 * Separado del engine para Single Responsibility Principle.
 * 
 * @author israel-icm
 * @date 2026-06-11
 */
class MySQLMetadataReader {
    
    fun readDatabases(connection: java.sql.Connection, query: String): List<Database> {
        val databases = mutableListOf<Database>()
        
        connection.createStatement().use { statement ->
            statement.executeQuery(query).use { resultSet ->
                while (resultSet.next()) {
                    databases.add(
                        Database(
                            name = resultSet.getString("name"),
                            charset = resultSet.getString("charset"),
                            collation = resultSet.getString("collation")
                        )
                    )
                }
            }
        }
        
        return databases
    }
    
    fun readTables(connection: java.sql.Connection, query: String, database: String): List<Table> {
        val tables = mutableListOf<Table>()
        
        connection.prepareStatement(query).use { statement ->
            statement.setString(1, database)
            
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    tables.add(
                        Table(
                            name = resultSet.getString("name"),
                            database = database,
                            type = parseTableType(resultSet.getString("type")),
                            engine = resultSet.getString("engine"),
                            rowCount = resultSet.getLong("rowCount"),
                            dataLength = resultSet.getLong("dataLength"),
                            createdAt = resultSet.getLongOrNull("createdAt"),
                            comment = resultSet.getString("comment")
                        )
                    )
                }
            }
        }
        
        return tables
    }
    
    fun readColumns(connection: java.sql.Connection, query: String, table: String): List<Column> {
        val columns = mutableListOf<Column>()
        
        connection.prepareStatement(query).use { statement ->
            statement.setString(1, table)
            
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    columns.add(
                        Column(
                            name = resultSet.getString("name"),
                            type = resultSet.getString("type"),
                            nullable = resultSet.getString("nullable") == "YES",
                            key = parseColumnKey(resultSet.getString("key")),
                            default = resultSet.getString("default_value"),
                            extra = resultSet.getString("extra"),
                            comment = resultSet.getString("comment")
                        )
                    )
                }
            }
        }
        
        return columns
    }
    
    fun readIndexes(connection: java.sql.Connection, query: String, table: String): List<Index> {
        val indexMap = mutableMapOf<String, MutableList<String>>()
        val indexMetadata = mutableMapOf<String, Pair<Boolean, String>>()
        
        connection.prepareStatement(query).use { statement ->
            statement.setString(1, table)
            
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    val indexName = resultSet.getString("name")
                    val columnName = resultSet.getString("column")
                    val nonUnique = resultSet.getInt("nonUnique") == 1
                    val indexType = resultSet.getString("type")
                    
                    indexMap.getOrPut(indexName) { mutableListOf() }.add(columnName)
                    indexMetadata[indexName] = Pair(!nonUnique, indexType)
                }
            }
        }
        
        return indexMap.map { (name, columns) ->
            val (unique, type) = indexMetadata[name]!!
            Index(
                name = name,
                columns = columns,
                unique = unique,
                type = parseIndexType(type)
            )
        }
    }
    
    fun readForeignKeys(connection: java.sql.Connection, query: String, table: String): List<ForeignKey> {
        val foreignKeys = mutableListOf<ForeignKey>()
        
        connection.prepareStatement(query).use { statement ->
            statement.setString(1, table)
            
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    foreignKeys.add(
                        ForeignKey(
                            name = resultSet.getString("name"),
                            column = resultSet.getString("column"),
                            referencedTable = resultSet.getString("referencedTable"),
                            referencedColumn = resultSet.getString("referencedColumn"),
                            onDelete = parseReferentialAction(resultSet.getString("onDelete")),
                            onUpdate = parseReferentialAction(resultSet.getString("onUpdate"))
                        )
                    )
                }
            }
        }
        
        return foreignKeys
    }
    
    private fun parseTableType(type: String): TableType {
        return when (type.uppercase()) {
            "BASE TABLE" -> TableType.TABLE
            "VIEW" -> TableType.VIEW
            "SYSTEM VIEW" -> TableType.SYSTEM_TABLE
            else -> TableType.TABLE
        }
    }
    
    private fun parseColumnKey(key: String?): ColumnKey {
        return when (key?.uppercase()) {
            "PRI" -> ColumnKey.PRIMARY
            "UNI" -> ColumnKey.UNIQUE
            "MUL" -> ColumnKey.MULTIPLE
            else -> ColumnKey.NONE
        }
    }
    
    private fun parseIndexType(type: String): IndexType {
        return when (type.uppercase()) {
            "BTREE" -> IndexType.BTREE
            "HASH" -> IndexType.HASH
            "FULLTEXT" -> IndexType.FULLTEXT
            "SPATIAL" -> IndexType.SPATIAL
            else -> IndexType.BTREE
        }
    }
    
    private fun parseReferentialAction(action: String): ReferentialAction {
        return when (action.uppercase()) {
            "CASCADE" -> ReferentialAction.CASCADE
            "SET NULL" -> ReferentialAction.SET_NULL
            "RESTRICT" -> ReferentialAction.RESTRICT
            "NO ACTION" -> ReferentialAction.NO_ACTION
            else -> ReferentialAction.NO_ACTION
        }
    }
    
    private fun ResultSet.getLongOrNull(columnLabel: String): Long? {
        return try {
            val value = getLong(columnLabel)
            if (wasNull()) null else value
        } catch (e: Exception) {
            null
        }
    }
}
```

---

### 3.6 MariaDBEngine Implementation

```kotlin
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
 * @author israel-icm
 * @date 2026-06-11
 */
class MariaDBEngine : DatabaseEngine {
    
    // Delegamos la mayoría de la lógica a MySQLEngine
    private val delegate = MySQLEngine()
    
    override suspend fun connect(config: ConnectionConfig): Result<Connection> {
        // MariaDB usa el mismo protocolo JDBC que MySQL
        return delegate.connect(config.copy(type = DatabaseType.MARIADB))
    }
    
    override suspend fun disconnect(): Result<Unit> = delegate.disconnect()
    
    override suspend fun executeQuery(query: String, params: List<Any>): Result<QueryResult> =
        delegate.executeQuery(query, params)
    
    override suspend fun executeUpdate(query: String, params: List<Any>): Result<Int> =
        delegate.executeUpdate(query, params)
    
    override suspend fun getDatabases(): Result<List<Database>> = delegate.getDatabases()
    
    override suspend fun getTables(database: String): Result<List<Table>> = delegate.getTables(database)
    
    override suspend fun getColumns(table: String): Result<List<Column>> = delegate.getColumns(table)
    
    override suspend fun getIndexes(table: String): Result<List<Index>> = delegate.getIndexes(table)
    
    override suspend fun getForeignKeys(table: String): Result<List<ForeignKey>> = delegate.getForeignKeys(table)
    
    override suspend fun beginTransaction(): Result<Transaction> = delegate.beginTransaction()
    
    override suspend fun getVersion(): Result<String> = delegate.getVersion()
    
    override fun getSupportedFeatures(): Set<DatabaseFeature> = setOf(
        DatabaseFeature.STORED_PROCEDURES,
        DatabaseFeature.TRIGGERS,
        DatabaseFeature.VIEWS,
        DatabaseFeature.EVENTS,
        DatabaseFeature.SEQUENCES,  // ← MariaDB soporta SEQUENCES
        DatabaseFeature.FOREIGN_KEYS,
        DatabaseFeature.TRANSACTIONS,
        DatabaseFeature.FULL_TEXT_SEARCH,
        DatabaseFeature.JSON_TYPE
    )
}
```

---

## 4. Data Models

### 4.1 Connection

```kotlin
/**
 * Representa una conexión activa a una base de datos.
 * 
 * @author israel-icm
 * @date 2026-06-11
 */
data class Connection(
    val id: String,
    val type: DatabaseType,
    val database: String,
    val host: String,
    val port: Int,
    val username: String,
    val version: String,
    val connectedAt: Long
)
```

### 4.2 Transaction

```kotlin
/**
 * Representa una transacción activa.
 * 
 * @author israel-icm
 * @date 2026-06-11
 */
data class Transaction(
    private val connection: java.sql.Connection,
    private val onCommit: () -> Unit,
    private val onRollback: () -> Unit
) {
    suspend fun commit() = withContext(Dispatchers.IO) {
        onCommit()
    }
    
    suspend fun rollback() = withContext(Dispatchers.IO) {
        onRollback()
    }
}
```

### 4.3 Database

```kotlin
/**
 * Representa una base de datos en el servidor.
 * 
 * @author israel-icm
 * @date 2026-06-11
 */
data class Database(
    val name: String,
    val charset: String,
    val collation: String
)
```

---

## 5. Repository Layer

### 5.1 DatabaseRepository Interface

```kotlin
/**
 * Repository que abstrae el acceso a DatabaseEngine.
 * Permite cambiar la implementación sin afectar los UseCases.
 * 
 * @author israel-icm
 * @date 2026-06-11
 */
interface DatabaseRepository {
    suspend fun connect(config: ConnectionConfig): Result<Connection>
    suspend fun disconnect(): Result<Unit>
    suspend fun executeQuery(query: String, params: List<Any> = emptyList()): Result<QueryResult>
    suspend fun executeUpdate(query: String, params: List<Any> = emptyList()): Result<Int>
    suspend fun getDatabases(): Result<List<Database>>
    suspend fun getTables(database: String): Result<List<Table>>
    suspend fun getColumns(table: String): Result<List<Column>>
    suspend fun getIndexes(table: String): Result<List<Index>>
    suspend fun getForeignKeys(table: String): Result<List<ForeignKey>>
    suspend fun beginTransaction(): Result<Transaction>
    suspend fun getVersion(): Result<String>
    fun getSupportedFeatures(): Set<DatabaseFeature>
}
```

### 5.2 DatabaseRepositoryImpl

```kotlin
/**
 * Implementación del repository usando DatabaseEngine.
 * 
 * @param engineFactory Factory para crear DatabaseEngine
 * @author israel-icm
 * @date 2026-06-11
 */
class DatabaseRepositoryImpl @Inject constructor(
    private val engineFactory: DatabaseEngineFactory
) : DatabaseRepository {
    
    private var currentEngine: DatabaseEngine? = null
    
    override suspend fun connect(config: ConnectionConfig): Result<Connection> {
        currentEngine = engineFactory.create(config.type)
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
}
```

---

## 6. Dependency Injection

### 6.1 DatabaseModule

```kotlin
/**
 * Hilt module para inyección de dependencias del módulo database.
 * 
 * @author israel-icm
 * @date 2026-06-11
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabaseEngineFactory(): DatabaseEngineFactory {
        return DatabaseEngineFactory
    }
    
    @Provides
    @Singleton
    fun provideDatabaseRepository(
        factory: DatabaseEngineFactory
    ): DatabaseRepository {
        return DatabaseRepositoryImpl(factory)
    }
}
```

---

## 7. Use Cases (Domain Layer)

### 7.1 ConnectToDatabaseUseCase

```kotlin
/**
 * Use Case para conectar a una base de datos.
 * 
 * @author israel-icm
 * @date 2026-06-11
 */
class ConnectToDatabaseUseCase @Inject constructor(
    private val repository: DatabaseRepository
) {
    suspend operator fun invoke(config: ConnectionConfig): Result<Connection> {
        return repository.connect(config)
    }
}
```

### 7.2 ExecuteQueryUseCase

```kotlin
/**
 * Use Case para ejecutar queries SELECT.
 * 
 * @author israel-icm
 * @date 2026-06-11
 */
class ExecuteQueryUseCase @Inject constructor(
    private val repository: DatabaseRepository
) {
    suspend operator fun invoke(query: String, params: List<Any> = emptyList()): Result<QueryResult> {
        return repository.executeQuery(query, params)
    }
}
```

### 7.3 GetTablesUseCase

```kotlin
/**
 * Use Case para obtener la lista de tablas de una base de datos.
 * 
 * @author israel-icm
 * @date 2026-06-11
 */
class GetTablesUseCase @Inject constructor(
    private val repository: DatabaseRepository
) {
    suspend operator fun invoke(database: String): Result<List<Table>> {
        return repository.getTables(database)
    }
}
```

---

## 8. Error Handling Strategy

### 8.1 Error Mapping

```
SQLException → DatabaseError.QueryExecutionFailed
SQLNonTransientConnectionException → DatabaseError.ConnectionFailed
SQLException (Access denied) → DatabaseError.AuthenticationFailed
SocketTimeoutException → DatabaseError.TimeoutError
IllegalArgumentException → DatabaseError.InvalidConfiguration
NotImplementedError → DatabaseError.UnsupportedFeature
Throwable → DatabaseError.UnknownError
```

### 8.2 Logging

```kotlin
// En producción
Timber.e(throwable, "Error ejecutando query: $query")

// En desarrollo
Log.d("MySQLEngine", "Conectando a ${config.host}:${config.port}")
```

---

## 9. Security Design

### 9.1 Password Encryption (Android Keystore)

```kotlin
/**
 * Encripta passwords usando Android Keystore.
 * TODO: Implementar en siguiente change.
 * 
 * @author israel-icm
 * @date 2026-06-11
 */
object PasswordEncryption {
    fun encrypt(password: String): ByteArray {
        // Usar Android Keystore con AES-256
        TODO("Implementar en change 'credential-encryption'")
    }
    
    fun decrypt(encrypted: ByteArray): String {
        TODO("Implementar en change 'credential-encryption'")
    }
}
```

### 9.2 SQL Injection Prevention

```kotlin
// ✅ SIEMPRE usar prepared statements
val query = "SELECT * FROM users WHERE id = ?"
engine.executeQuery(query, listOf(userId))

// ❌ NUNCA concatenar
val query = "SELECT * FROM users WHERE id = $userId"  // PELIGRO
```

---

## 10. Testing Strategy

### 10.1 Unit Tests

```kotlin
@Test
fun `executeQuery returns success when query valid`() = runTest {
    // Arrange
    val mockEngine = mockk<DatabaseEngine>()
    val expectedResult = QueryResult(
        columns = listOf("id", "name"),
        rows = listOf(mapOf("id" to 1, "name" to "Test")),
        rowCount = 1,
        executionTimeMs = 50
    )
    coEvery { mockEngine.executeQuery(any(), any()) } returns Result.success(expectedResult)
    
    // Act
    val result = mockEngine.executeQuery("SELECT * FROM users", emptyList())
    
    // Assert
    assertTrue(result.isSuccess)
    assertEquals(expectedResult, result.getOrNull())
}
```

### 10.2 Integration Tests (Docker)

```kotlin
@Testcontainers
class MySQLEngineIntegrationTest {
    
    companion object {
        @Container
        val mysql = MySQLContainer<Nothing>("mysql:8.0").apply {
            withDatabaseName("test_db")
            withUsername("test")
            withPassword("test")
        }
    }
    
    @Test
    fun `connects to real MySQL instance`() = runTest {
        val config = ConnectionConfig(
            name = "Test",
            type = DatabaseType.MYSQL,
            host = mysql.host,
            port = mysql.firstMappedPort,
            database = "test_db",
            username = "test",
            password = "test"
        )
        
        val engine = MySQLEngine()
        val result = engine.connect(config)
        
        assertTrue(result.isSuccess)
    }
}
```

---

## 11. Performance Optimizations

### 11.1 Connection Pooling

- HikariCP con máximo 10 conexiones
- Connection timeout: 10s
- Idle timeout: 10 minutos
- Max lifetime: 30 minutos

### 11.2 Prepared Statement Caching

```kotlin
addDataSourceProperty("cachePrepStmts", "true")
addDataSourceProperty("prepStmtCacheSize", "250")
```

### 11.3 Metadata Caching

```kotlin
// TODO: Implementar cache en Repository
private val metadataCache = LruCache<String, List<Table>>(maxSize = 100)
```

---

## 12. Dependencies (build.gradle.kts)

```kotlin
dependencies {
    // MySQL Driver
    implementation("mysql:mysql-connector-java:8.0.33")
    
    // MariaDB Driver
    implementation("org.mariadb.jdbc:mariadb-java-client:3.1.4")
    
    // Connection Pooling
    implementation("com.zaxxer:HikariCP:5.0.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.testcontainers:mysql:1.19.0")
    testImplementation("org.testcontainers:mariadb:1.19.0")
    testImplementation("app.cash.turbine:turbine:1.0.0")
}
```

---

## 13. Out of Scope

❌ SSH Tunneling (change separado)  
❌ Credential encryption (change separado)  
❌ Backup/Restore (change separado)  
❌ PostgreSQL/SQLite engines (v1.1)  
❌ Query history/favorites (change separado)  
❌ Visual query builder (v2.0)  

---

## 14. Success Criteria

✅ MySQLEngine + MariaDBEngine implementados  
✅ DatabaseEngineFactory crea engines correctamente  
✅ Connection pooling funciona sin leaks  
✅ Todas las operaciones CRUD funcionan  
✅ Metadata (databases, tables, columns, indexes, FKs) se obtiene correctamente  
✅ Transacciones commit/rollback funcionan  
✅ Error handling robusto (no crashes)  
✅ 80%+ code coverage  
✅ Integration tests pasan  

---

**Status**: Ready for Implementation
