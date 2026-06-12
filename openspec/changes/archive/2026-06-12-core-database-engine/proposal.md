# Proposal: Core Database Engine

**Fecha**: 2026-06-11  
**Autor**: israel-icm  
**Change**: core-database-engine  
**Status**: Proposed  

---

## Resumen Ejecutivo

Crear el módulo `core-database` con una arquitectura extensible basada en **Strategy Pattern + Factory Pattern** que permita soportar múltiples motores de bases de datos (MySQL, MariaDB, PostgreSQL, SQLite, etc.) sin modificar código existente al agregar nuevos motores.

---

## Objetivos

### Funcionales

1. ✅ Conectar a MySQL y MariaDB (v1.0)
2. ✅ Ejecutar queries SELECT/INSERT/UPDATE/DELETE
3. ✅ Obtener metadata (tablas, columnas, índices, constraints)
4. ✅ Ejecutar stored procedures y functions
5. ✅ Gestionar transacciones (commit/rollback)
6. ✅ Soportar prepared statements (seguridad)
7. ✅ Connection pooling para performance

### No Funcionales

1. ✅ **Extensibilidad**: Agregar PostgreSQL sin tocar código existente
2. ✅ **Testabilidad**: 80%+ cobertura con mocks
3. ✅ **Performance**: Pooling de conexiones, queries optimizadas
4. ✅ **Seguridad**: Android Keystore, prepared statements, SSL/TLS
5. ✅ **Cancellation**: Cancelar queries largas desde UI
6. ✅ **Error Handling**: Result wrappers, no crashes

---

## Arquitectura Propuesta

### Patrón: Strategy + Factory

```kotlin
// Strategy: Define el contrato común
interface DatabaseEngine {
    suspend fun connect(config: ConnectionConfig): Result<Connection>
    suspend fun disconnect(): Result<Unit>
    suspend fun executeQuery(query: String, params: List<Any> = emptyList()): Result<QueryResult>
    suspend fun executeUpdate(query: String, params: List<Any> = emptyList()): Result<Int>
    suspend fun getTables(database: String): Result<List<Table>>
    suspend fun getColumns(table: String): Result<List<Column>>
    suspend fun beginTransaction(): Result<Transaction>
    fun getSupportedFeatures(): Set<DatabaseFeature>
}

// Factory: Crea la estrategia correcta
object DatabaseEngineFactory {
    fun create(type: DatabaseType): DatabaseEngine {
        return when (type) {
            DatabaseType.MYSQL -> MySQLEngine()
            DatabaseType.MARIADB -> MariaDBEngine()
            DatabaseType.POSTGRESQL -> PostgreSQLEngine()
            DatabaseType.SQLITE -> SQLiteEngine()
        }
    }
}
```

---

## Módulos y Responsabilidades

### Módulo: `core-database`

**Ruta**: `app/src/main/java/com/sphynxs/mydatabases/core/database/`

#### Estructura

```
core/database/
├── engine/
│   ├── DatabaseEngine.kt              # Interface principal
│   ├── DatabaseEngineFactory.kt       # Factory
│   ├── DatabaseType.kt                # Enum(MYSQL, MARIADB, ...)
│   ├── DatabaseFeature.kt             # Enum(STORED_PROC, SCHEMAS, ...)
│   ├── mysql/
│   │   ├── MySQLEngine.kt
│   │   ├── MySQLConnectionPool.kt
│   │   └── MySQLQueryBuilder.kt
│   └── mariadb/
│       ├── MariaDBEngine.kt
│       └── MariaDBConnectionPool.kt
├── models/
│   ├── Connection.kt                  # Data class con estado de conexión
│   ├── ConnectionConfig.kt            # Host, port, user, pass, etc.
│   ├── QueryResult.kt                 # Rows + metadata
│   ├── Table.kt                       # Nombre, tipo, engine, etc.
│   ├── Column.kt                      # Nombre, tipo, nullable, default
│   ├── Database.kt                    # Nombre, charset, collation
│   ├── Transaction.kt                 # Transacción activa
│   └── DatabaseError.kt               # Sealed class de errores
├── repository/
│   └── DatabaseRepository.kt          # Capa intermedia (use case)
└── di/
    └── DatabaseModule.kt              # Hilt module
```

---

## Modelos de Datos

### ConnectionConfig

```kotlin
@Parcelize
data class ConnectionConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: DatabaseType,
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    val password: String,           // Encrypted con Android Keystore
    val useSSL: Boolean = true,
    val sshTunnelConfig: SSHTunnelConfig? = null,
    val connectionTimeout: Long = 10_000L,
    val readTimeout: Long = 30_000L,
    val maxPoolSize: Int = 10,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null
) : Parcelable
```

### QueryResult

```kotlin
data class QueryResult(
    val columns: List<String>,
    val rows: List<Map<String, Any?>>,
    val rowCount: Int,
    val executionTimeMs: Long,
    val warnings: List<String> = emptyList()
)
```

### Table

```kotlin
data class Table(
    val name: String,
    val database: String,
    val type: TableType,            // TABLE, VIEW, SYSTEM_TABLE
    val engine: String? = null,     // InnoDB, MyISAM (solo MySQL)
    val rowCount: Long? = null,
    val dataLength: Long? = null,
    val createdAt: Long? = null,
    val comment: String? = null
)

enum class TableType {
    TABLE, VIEW, SYSTEM_TABLE
}
```

### Column

```kotlin
data class Column(
    val name: String,
    val type: String,               // VARCHAR(255), INT, BIGINT, etc.
    val nullable: Boolean,
    val key: ColumnKey,             // PRI, UNI, MUL, null
    val default: String? = null,
    val extra: String? = null,      // auto_increment, on update CURRENT_TIMESTAMP
    val comment: String? = null
)

enum class ColumnKey {
    PRIMARY, UNIQUE, MULTIPLE, NONE
}
```

---

## DatabaseEngine Interface

```kotlin
interface DatabaseEngine {
    /**
     * Conecta al servidor de base de datos
     * @param config Configuración de conexión
     * @return Result con Connection o error
     */
    suspend fun connect(config: ConnectionConfig): Result<Connection>
    
    /**
     * Desconecta y libera recursos
     */
    suspend fun disconnect(): Result<Unit>
    
    /**
     * Ejecuta query SELECT
     * @param query SQL query
     * @param params Parámetros para prepared statement
     * @return Result con QueryResult o error
     */
    suspend fun executeQuery(
        query: String, 
        params: List<Any> = emptyList()
    ): Result<QueryResult>
    
    /**
     * Ejecuta INSERT/UPDATE/DELETE
     * @return Número de filas afectadas
     */
    suspend fun executeUpdate(
        query: String, 
        params: List<Any> = emptyList()
    ): Result<Int>
    
    /**
     * Lista todas las bases de datos
     */
    suspend fun getDatabases(): Result<List<Database>>
    
    /**
     * Lista tablas de una base de datos
     */
    suspend fun getTables(database: String): Result<List<Table>>
    
    /**
     * Lista columnas de una tabla
     */
    suspend fun getColumns(table: String): Result<List<Column>>
    
    /**
     * Lista índices de una tabla
     */
    suspend fun getIndexes(table: String): Result<List<Index>>
    
    /**
     * Lista foreign keys de una tabla
     */
    suspend fun getForeignKeys(table: String): Result<List<ForeignKey>>
    
    /**
     * Inicia una transacción
     */
    suspend fun beginTransaction(): Result<Transaction>
    
    /**
     * Features soportadas por este motor
     */
    fun getSupportedFeatures(): Set<DatabaseFeature>
    
    /**
     * Versión del motor
     */
    suspend fun getVersion(): Result<String>
}
```

---

## DatabaseType Enum

```kotlin
enum class DatabaseType(
    val displayName: String,
    val defaultPort: Int,
    val iconRes: Int
) {
    MYSQL(
        displayName = "MySQL",
        defaultPort = 3306,
        iconRes = R.drawable.ic_mysql
    ),
    MARIADB(
        displayName = "MariaDB",
        defaultPort = 3306,
        iconRes = R.drawable.ic_mariadb
    ),
    POSTGRESQL(
        displayName = "PostgreSQL",
        defaultPort = 5432,
        iconRes = R.drawable.ic_postgresql
    ),
    SQLITE(
        displayName = "SQLite",
        defaultPort = 0,
        iconRes = R.drawable.ic_sqlite
    )
}
```

---

## DatabaseFeature Enum

```kotlin
enum class DatabaseFeature {
    STORED_PROCEDURES,
    TRIGGERS,
    VIEWS,
    EVENTS,
    SCHEMAS,
    SEQUENCES,
    FOREIGN_KEYS,
    TRANSACTIONS,
    FULL_TEXT_SEARCH,
    JSON_TYPE,
    WINDOW_FUNCTIONS,
    RECURSIVE_CTE
}
```

---

## Dependencies

### build.gradle.kts (app)

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
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    
    // Testing
    testImplementation("org.testcontainers:mysql:1.19.0")
    testImplementation("org.testcontainers:mariadb:1.19.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
```

---

## Error Handling

### DatabaseError Sealed Class

```kotlin
sealed class DatabaseError : Throwable() {
    data class ConnectionFailed(val reason: String) : DatabaseError()
    data class AuthenticationFailed(val reason: String) : DatabaseError()
    data class QueryExecutionFailed(val query: String, val reason: String) : DatabaseError()
    data class TimeoutError(val operation: String) : DatabaseError()
    data class InvalidConfiguration(val field: String, val reason: String) : DatabaseError()
    data class UnsupportedFeature(val feature: DatabaseFeature) : DatabaseError()
    data class UnknownError(val throwable: Throwable) : DatabaseError()
}
```

---

## Connection Pooling Strategy

### HikariCP Config

```kotlin
class MySQLConnectionPool(private val config: ConnectionConfig) {
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = "jdbc:mysql://${config.host}:${config.port}/${config.database}"
        username = config.username
        password = config.password // Decrypt from Keystore
        maximumPoolSize = config.maxPoolSize
        connectionTimeout = config.connectionTimeout
        idleTimeout = 600_000L // 10 minutos
        maxLifetime = 1_800_000L // 30 minutos
        
        if (config.useSSL) {
            addDataSourceProperty("useSSL", "true")
            addDataSourceProperty("requireSSL", "true")
        }
    }
    
    private val dataSource = HikariDataSource(hikariConfig)
    
    suspend fun getConnection(): java.sql.Connection = withContext(Dispatchers.IO) {
        dataSource.connection
    }
    
    fun close() {
        dataSource.close()
    }
}
```

---

## Testing Strategy

### Unit Tests (80%+ coverage)

```kotlin
class MySQLEngineTest {
    private lateinit var engine: MySQLEngine
    private val mockPool: MySQLConnectionPool = mockk()
    
    @Before
    fun setup() {
        engine = MySQLEngine(connectionPool = mockPool)
    }
    
    @Test
    fun `connect returns success when credentials valid`() = runTest {
        val config = ConnectionConfig(
            name = "Test",
            type = DatabaseType.MYSQL,
            host = "localhost",
            port = 3306,
            database = "test_db",
            username = "root",
            password = "password"
        )
        
        coEvery { mockPool.getConnection() } returns mockk(relaxed = true)
        
        val result = engine.connect(config)
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `executeQuery returns rows when query valid`() = runTest {
        val mockConnection: java.sql.Connection = mockk()
        val mockStatement: PreparedStatement = mockk()
        val mockResultSet: ResultSet = mockk()
        
        coEvery { mockPool.getConnection() } returns mockConnection
        every { mockConnection.prepareStatement(any()) } returns mockStatement
        every { mockStatement.executeQuery() } returns mockResultSet
        
        // ... assertions
    }
}
```

### Integration Tests (Docker)

```kotlin
@Testcontainers
class MySQLEngineIntegrationTest {
    companion object {
        @Container
        val mysqlContainer = MySQLContainer<Nothing>("mysql:8.0").apply {
            withDatabaseName("test_db")
            withUsername("test")
            withPassword("test")
        }
    }
    
    @Test
    fun `connects to real MySQL instance`() = runTest {
        val config = ConnectionConfig(
            name = "Integration Test",
            type = DatabaseType.MYSQL,
            host = mysqlContainer.host,
            port = mysqlContainer.firstMappedPort,
            database = "test_db",
            username = "test",
            password = "test"
        )
        
        val engine = MySQLEngine()
        val result = engine.connect(config)
        
        assertTrue(result.isSuccess)
        
        // Verificar que puede ejecutar queries
        val queryResult = engine.executeQuery("SHOW TABLES")
        assertTrue(queryResult.isSuccess)
    }
}
```

---

## Security Requirements

1. **Credentials Encryption**:
   - Passwords NUNCA en plain text
   - Android Keystore para encriptar/desencriptar
   - Room almacena encrypted bytes

2. **Prepared Statements**:
   - SIEMPRE usar `PreparedStatement`
   - NUNCA concatenar SQL strings
   - Validar inputs

3. **SSL/TLS**:
   - Default: SSL habilitado
   - Opción de requerir certificados

4. **Timeouts**:
   - Connection timeout: 10s
   - Read timeout: 30s
   - Query timeout: 60s (configurable)

5. **SQL Injection Prevention**:
   - Prepared statements obligatorios
   - Sanitizar nombres de tablas/columnas
   - Whitelist de caracteres permitidos

---

## Performance Considerations

1. **Connection Pooling**: HikariCP con máximo 10 conexiones
2. **Lazy Loading**: Metadata se carga on-demand
3. **Pagination**: Queries grandes con LIMIT/OFFSET
4. **Background Threads**: Todo IO en `Dispatchers.IO`
5. **Cancel Support**: Flow cancellable para queries largas
6. **Cache**: Metadata (tablas, columnas) con TTL 5 minutos

---

## Risks & Mitigations

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| JDBC drivers grandes (10MB+) | APK size | High | ProGuard + R8, considerar split APKs |
| Connection leaks | Memory leaks | Medium | HikariCP auto-close, leak detection |
| Query timeout en queries largas | UX | High | Timeout configurable, UI cancellable |
| Compatibilidad MySQL vs MariaDB | Bugs | Medium | Integration tests con ambos |
| SSL certificates inválidos | Connection fails | Medium | Trust all certs en desarrollo, validar en producción |

---

## Out of Scope (v1.0)

❌ PostgreSQL (v1.1)  
❌ SQLite (v1.1)  
❌ NoSQL (MongoDB, Redis) (v3.1)  
❌ SSH Tunneling (v1.2)  
❌ Import/Export (v1.3)  
❌ Visual Query Builder (v2.0)  

---

## Success Criteria

✅ MySQLEngine conecta a MySQL 5.7+ y 8.0+  
✅ MariaDBEngine conecta a MariaDB 10.5+  
✅ Ejecuta queries SELECT/INSERT/UPDATE/DELETE  
✅ Obtiene metadata (tablas, columnas, índices)  
✅ Connection pooling funciona sin leaks  
✅ Tests unitarios 80%+ coverage  
✅ Integration tests con Docker pasan  
✅ APK size < 15MB con drivers incluidos  

---

## Next Steps

1. ✅ Proposal aprobada
2. ⏳ Crear `spec.md` con requisitos detallados
3. ⏳ Crear `design.md` con arquitectura técnica
4. ⏳ Crear `tasks.md` con tareas de implementación
5. ⏳ Implementar con SDD Apply

---

**Decisión**: Proceder con Strategy + Factory, HikariCP, JDBC drivers, TestContainers.
