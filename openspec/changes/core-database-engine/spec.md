# Specification: Core Database Engine

**Fecha**: 2026-06-11  
**Autor**: israel-icm  
**Change**: core-database-engine  
**Status**: Draft  

---

## 1. Overview

Este spec define los requisitos detallados para el módulo `core-database` que provee una capa de abstracción extensible para conectar y operar con múltiples motores de bases de datos (MySQL, MariaDB, PostgreSQL, SQLite).

---

## 2. Functional Requirements

### FR-001: Conexión a Base de Datos

**Prioridad**: MUST  
**Actor**: Usuario  

**Descripción**:  
El sistema debe permitir conectar a servidores MySQL y MariaDB usando credenciales de usuario.

**Acceptance Criteria**:
- ✅ Conectar con host, port, database, username, password
- ✅ Soportar SSL/TLS
- ✅ Timeout configurable (default: 10s)
- ✅ Retornar `Result<Connection>` con éxito o error específico
- ✅ Connection pooling automático (HikariCP)

**Example**:
```kotlin
val config = ConnectionConfig(
    name = "Production DB",
    type = DatabaseType.MYSQL,
    host = "db.example.com",
    port = 3306,
    database = "myapp",
    username = "admin",
    password = "encrypted_password",
    useSSL = true
)

val result = engine.connect(config)
when (result) {
    is Success -> println("Connected: ${result.value.database}")
    is Failure -> println("Error: ${result.error.message}")
}
```

**Error Scenarios**:
- Host unreachable → `DatabaseError.ConnectionFailed`
- Invalid credentials → `DatabaseError.AuthenticationFailed`
- Timeout → `DatabaseError.TimeoutError`

---

### FR-002: Ejecutar Query SELECT

**Prioridad**: MUST  
**Actor**: Usuario  

**Descripción**:  
El sistema debe ejecutar queries SELECT y retornar resultados con columnas + rows.

**Acceptance Criteria**:
- ✅ Ejecutar query con prepared statements
- ✅ Soportar parámetros (`?` placeholders)
- ✅ Retornar `QueryResult` con columnas, rows, rowCount, executionTime
- ✅ Soportar queries sin resultados (ej: `SELECT * FROM users WHERE id = 999`)
- ✅ Timeout configurable (default: 30s)
- ✅ Cancelable desde UI

**Example**:
```kotlin
val query = "SELECT id, name, email FROM users WHERE status = ? LIMIT ?"
val params = listOf("active", 100)

val result = engine.executeQuery(query, params)

result.onSuccess { queryResult ->
    println("Columns: ${queryResult.columns}")
    queryResult.rows.forEach { row ->
        println("ID: ${row["id"]}, Name: ${row["name"]}")
    }
    println("Execution time: ${queryResult.executionTimeMs}ms")
}
```

**Edge Cases**:
- Query vacío → `DatabaseError.QueryExecutionFailed`
- Sintaxis inválida → `DatabaseError.QueryExecutionFailed`
- Tabla no existe → `DatabaseError.QueryExecutionFailed`
- Timeout excedido → `DatabaseError.TimeoutError`

---

### FR-003: Ejecutar Query UPDATE/INSERT/DELETE

**Prioridad**: MUST  
**Actor**: Usuario  

**Descripción**:  
El sistema debe ejecutar queries de modificación y retornar el número de filas afectadas.

**Acceptance Criteria**:
- ✅ Ejecutar INSERT/UPDATE/DELETE con prepared statements
- ✅ Retornar número de filas afectadas
- ✅ Soportar batch inserts (opcional)
- ✅ Auto-commit por defecto, manual con transacciones

**Example**:
```kotlin
// INSERT
val insertQuery = "INSERT INTO users (name, email, status) VALUES (?, ?, ?)"
val insertParams = listOf("John Doe", "john@example.com", "active")
val insertResult = engine.executeUpdate(insertQuery, insertParams)

insertResult.onSuccess { affectedRows ->
    println("Inserted $affectedRows row(s)")
}

// UPDATE
val updateQuery = "UPDATE users SET status = ? WHERE id = ?"
val updateParams = listOf("inactive", 42)
val updateResult = engine.executeUpdate(updateQuery, updateParams)

updateResult.onSuccess { affectedRows ->
    println("Updated $affectedRows row(s)")
}

// DELETE
val deleteQuery = "DELETE FROM users WHERE status = ?"
val deleteParams = listOf("deleted")
val deleteResult = engine.executeUpdate(deleteQuery, deleteParams)

deleteResult.onSuccess { affectedRows ->
    println("Deleted $affectedRows row(s)")
}
```

---

### FR-004: Listar Bases de Datos

**Prioridad**: MUST  
**Actor**: Usuario  

**Descripción**:  
El sistema debe listar todas las bases de datos disponibles en el servidor.

**Acceptance Criteria**:
- ✅ Retornar lista de `Database` con nombre, charset, collation
- ✅ Excluir system databases (`information_schema`, `mysql`, `performance_schema`, `sys`)
- ✅ Ordenar alfabéticamente

**Example**:
```kotlin
val result = engine.getDatabases()

result.onSuccess { databases ->
    databases.forEach { db ->
        println("Database: ${db.name}, Charset: ${db.charset}")
    }
}
```

**MySQL Query**:
```sql
SELECT 
    SCHEMA_NAME as name,
    DEFAULT_CHARACTER_SET_NAME as charset,
    DEFAULT_COLLATION_NAME as collation
FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys')
ORDER BY SCHEMA_NAME
```

---

### FR-005: Listar Tablas

**Prioridad**: MUST  
**Actor**: Usuario  

**Descripción**:  
El sistema debe listar todas las tablas y vistas de una base de datos.

**Acceptance Criteria**:
- ✅ Retornar lista de `Table` con nombre, tipo, engine, rowCount, dataLength
- ✅ Soportar filtrado por tipo (TABLE, VIEW, SYSTEM TABLE)
- ✅ Incluir metadata (fecha creación, comentario)

**Example**:
```kotlin
val result = engine.getTables("myapp")

result.onSuccess { tables ->
    tables.forEach { table ->
        println("Table: ${table.name}, Type: ${table.type}, Rows: ${table.rowCount}")
    }
}
```

**MySQL Query**:
```sql
SELECT 
    TABLE_NAME as name,
    TABLE_TYPE as type,
    ENGINE as engine,
    TABLE_ROWS as rowCount,
    DATA_LENGTH as dataLength,
    CREATE_TIME as createdAt,
    TABLE_COMMENT as comment
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = ?
ORDER BY TABLE_NAME
```

---

### FR-006: Listar Columnas

**Prioridad**: MUST  
**Actor**: Usuario  

**Descripción**:  
El sistema debe listar todas las columnas de una tabla con metadata completa.

**Acceptance Criteria**:
- ✅ Retornar lista de `Column` con nombre, tipo, nullable, key, default, extra
- ✅ Identificar primary keys, unique keys, foreign keys
- ✅ Mostrar auto_increment, generated columns

**Example**:
```kotlin
val result = engine.getColumns("users")

result.onSuccess { columns ->
    columns.forEach { col ->
        val key = if (col.key == ColumnKey.PRIMARY) " [PK]" else ""
        val nullable = if (col.nullable) "NULL" else "NOT NULL"
        println("${col.name} ${col.type} $nullable$key")
    }
}
```

**MySQL Query**:
```sql
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
```

---

### FR-007: Listar Índices

**Prioridad**: SHOULD  
**Actor**: Usuario  

**Descripción**:  
El sistema debe listar todos los índices de una tabla.

**Acceptance Criteria**:
- ✅ Retornar lista de `Index` con nombre, columnas, unique, tipo
- ✅ Soportar índices compuestos (múltiples columnas)
- ✅ Distinguir PRIMARY, UNIQUE, FULLTEXT, SPATIAL

**Example**:
```kotlin
val result = engine.getIndexes("users")

result.onSuccess { indexes ->
    indexes.forEach { idx ->
        val type = if (idx.unique) "UNIQUE" else "INDEX"
        println("$type ${idx.name} (${idx.columns.joinToString(", ")})")
    }
}
```

**MySQL Query**:
```sql
SELECT 
    INDEX_NAME as name,
    COLUMN_NAME as column,
    NON_UNIQUE as nonUnique,
    INDEX_TYPE as type,
    SEQ_IN_INDEX as position
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
ORDER BY INDEX_NAME, SEQ_IN_INDEX
```

---

### FR-008: Listar Foreign Keys

**Prioridad**: SHOULD  
**Actor**: Usuario  

**Descripción**:  
El sistema debe listar todas las foreign keys de una tabla.

**Acceptance Criteria**:
- ✅ Retornar lista de `ForeignKey` con nombre, columna, tabla referenciada, columna referenciada
- ✅ Mostrar ON DELETE y ON UPDATE actions

**Example**:
```kotlin
val result = engine.getForeignKeys("orders")

result.onSuccess { fks ->
    fks.forEach { fk ->
        println("${fk.name}: ${fk.column} -> ${fk.referencedTable}.${fk.referencedColumn}")
        println("  ON DELETE ${fk.onDelete}, ON UPDATE ${fk.onUpdate}")
    }
}
```

**MySQL Query**:
```sql
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
```

---

### FR-009: Transacciones

**Prioridad**: MUST  
**Actor**: Usuario  

**Descripción**:  
El sistema debe soportar transacciones con commit y rollback.

**Acceptance Criteria**:
- ✅ `beginTransaction()` deshabilita auto-commit
- ✅ `commit()` confirma cambios
- ✅ `rollback()` revierte cambios
- ✅ Auto-rollback en caso de error
- ✅ Nested transactions no soportadas (error)

**Example**:
```kotlin
val transaction = engine.beginTransaction().getOrThrow()

try {
    engine.executeUpdate("INSERT INTO users (name) VALUES (?)", listOf("Alice"))
    engine.executeUpdate("INSERT INTO orders (user_id) VALUES (?)", listOf(1))
    
    transaction.commit()
    println("Transaction committed")
} catch (e: Exception) {
    transaction.rollback()
    println("Transaction rolled back: ${e.message}")
}
```

---

### FR-010: Obtener Versión del Motor

**Prioridad**: SHOULD  
**Actor**: Sistema  

**Descripción**:  
El sistema debe detectar la versión del motor de base de datos.

**Acceptance Criteria**:
- ✅ Retornar string con versión (ej: `8.0.33`, `10.11.2-MariaDB`)
- ✅ Útil para features específicas de versión

**Example**:
```kotlin
val result = engine.getVersion()

result.onSuccess { version ->
    println("Database version: $version")
}
```

**MySQL Query**:
```sql
SELECT VERSION() as version
```

---

### FR-011: Features Soportadas

**Prioridad**: MUST  
**Actor**: Sistema  

**Descripción**:  
Cada motor debe declarar qué features soporta.

**Acceptance Criteria**:
- ✅ `getSupportedFeatures()` retorna `Set<DatabaseFeature>`
- ✅ UI puede deshabilitar opciones no soportadas

**Example**:
```kotlin
val features = engine.getSupportedFeatures()

if (DatabaseFeature.STORED_PROCEDURES in features) {
    // Mostrar botón "Create Procedure"
}

if (DatabaseFeature.SCHEMAS in features) {
    // Mostrar lista de schemas (PostgreSQL)
}
```

**Features Matrix**:
```kotlin
// MySQLEngine
override fun getSupportedFeatures() = setOf(
    DatabaseFeature.STORED_PROCEDURES,
    DatabaseFeature.TRIGGERS,
    DatabaseFeature.VIEWS,
    DatabaseFeature.EVENTS,
    DatabaseFeature.FOREIGN_KEYS,
    DatabaseFeature.TRANSACTIONS,
    DatabaseFeature.FULL_TEXT_SEARCH,
    DatabaseFeature.JSON_TYPE
)

// PostgreSQLEngine (futuro)
override fun getSupportedFeatures() = setOf(
    DatabaseFeature.STORED_PROCEDURES,
    DatabaseFeature.TRIGGERS,
    DatabaseFeature.VIEWS,
    DatabaseFeature.SCHEMAS,  // PostgreSQL tiene schemas
    DatabaseFeature.SEQUENCES,
    DatabaseFeature.FOREIGN_KEYS,
    DatabaseFeature.TRANSACTIONS,
    DatabaseFeature.WINDOW_FUNCTIONS,
    DatabaseFeature.RECURSIVE_CTE
)
```

---

## 3. Non-Functional Requirements

### NFR-001: Performance

**Requirement**:  
Las operaciones de base de datos deben ser performantes y no bloquear el UI thread.

**Acceptance Criteria**:
- ✅ Todo IO en `Dispatchers.IO`
- ✅ Connection pooling con HikariCP (máx 10 conexiones)
- ✅ Queries largas cancelables desde UI
- ✅ Metadata cacheada con TTL 5 minutos
- ✅ Lazy loading de tablas/columnas

**Metrics**:
- Conexión inicial: < 2s
- Query simple (< 100 rows): < 500ms
- Listar tablas (< 100 tablas): < 1s
- Listar columnas (< 50 columnas): < 500ms

---

### NFR-002: Security

**Requirement**:  
Las credenciales y comunicación deben estar protegidas.

**Acceptance Criteria**:
- ✅ Passwords encriptados con Android Keystore
- ✅ NUNCA plain text en logs
- ✅ SSL/TLS habilitado por defecto
- ✅ Prepared statements SIEMPRE (no concatenación)
- ✅ SQL Injection prevention
- ✅ Connection timeout para evitar hanging connections

**Security Checklist**:
```kotlin
// ✅ CORRECTO
val query = "SELECT * FROM users WHERE id = ?"
engine.executeQuery(query, listOf(userId))

// ❌ INCORRECTO (SQL Injection)
val query = "SELECT * FROM users WHERE id = $userId"
engine.executeQuery(query)
```

---

### NFR-003: Testability

**Requirement**:  
El código debe ser fácilmente testeable con mocks e integration tests.

**Acceptance Criteria**:
- ✅ 80%+ code coverage en unit tests
- ✅ Interface `DatabaseEngine` permite mocking
- ✅ Integration tests con TestContainers
- ✅ Tests de timeout, error handling, edge cases

**Test Structure**:
```
test/
├── unit/
│   ├── engine/
│   │   ├── MySQLEngineTest.kt
│   │   ├── MariaDBEngineTest.kt
│   │   └── DatabaseEngineFactoryTest.kt
│   └── models/
│       └── QueryResultTest.kt
└── integration/
    ├── MySQLEngineIntegrationTest.kt
    └── MariaDBEngineIntegrationTest.kt
```

---

### NFR-004: Extensibility

**Requirement**:  
Agregar un nuevo motor de base de datos NO debe modificar código existente.

**Acceptance Criteria**:
- ✅ Crear nueva clase `PostgreSQLEngine : DatabaseEngine`
- ✅ Agregar caso en `DatabaseEngineFactory`
- ✅ Agregar `DatabaseType.POSTGRESQL`
- ✅ Sin cambios en UI, ViewModels, Repository

**Open/Closed Principle**:
```kotlin
// ✅ SOLO agregar, NO modificar
enum class DatabaseType {
    MYSQL,
    MARIADB,
    POSTGRESQL,  // ← NUEVO
    SQLITE       // ← NUEVO
}

object DatabaseEngineFactory {
    fun create(type: DatabaseType): DatabaseEngine {
        return when (type) {
            DatabaseType.MYSQL -> MySQLEngine()
            DatabaseType.MARIADB -> MariaDBEngine()
            DatabaseType.POSTGRESQL -> PostgreSQLEngine()  // ← NUEVO
            DatabaseType.SQLITE -> SQLiteEngine()          // ← NUEVO
        }
    }
}
```

---

### NFR-005: Error Handling

**Requirement**:  
Todos los errores deben ser manejados gracefully sin crashes.

**Acceptance Criteria**:
- ✅ `Result<T>` wrapping en todas las suspend functions
- ✅ Sealed class `DatabaseError` para errores específicos
- ✅ Logs con stack traces para debugging
- ✅ User-friendly error messages en UI

**Error Categories**:
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

### NFR-006: APK Size

**Requirement**:  
Los JDBC drivers NO deben inflar excesivamente el APK.

**Acceptance Criteria**:
- ✅ ProGuard/R8 habilitado
- ✅ APK size con MySQL + MariaDB drivers < 15MB
- ✅ Considerar split APKs si supera 20MB

**build.gradle.kts**:
```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

---

## 4. Data Models

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
    val password: String,              // Encrypted
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
    val type: TableType,
    val engine: String? = null,
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
    val type: String,
    val nullable: Boolean,
    val key: ColumnKey,
    val default: String? = null,
    val extra: String? = null,
    val comment: String? = null
)

enum class ColumnKey {
    PRIMARY, UNIQUE, MULTIPLE, NONE
}
```

### Index

```kotlin
data class Index(
    val name: String,
    val columns: List<String>,
    val unique: Boolean,
    val type: IndexType
)

enum class IndexType {
    BTREE, HASH, FULLTEXT, SPATIAL
}
```

### ForeignKey

```kotlin
data class ForeignKey(
    val name: String,
    val column: String,
    val referencedTable: String,
    val referencedColumn: String,
    val onDelete: ReferentialAction,
    val onUpdate: ReferentialAction
)

enum class ReferentialAction {
    CASCADE, SET_NULL, RESTRICT, NO_ACTION
}
```

---

## 5. Edge Cases & Error Scenarios

### EC-001: Connection Timeout

**Scenario**: Host no responde en 10 segundos  
**Expected**: `DatabaseError.TimeoutError("Connection timeout")`

### EC-002: Invalid Credentials

**Scenario**: Username/password incorrectos  
**Expected**: `DatabaseError.AuthenticationFailed("Access denied for user 'admin'")`

### EC-003: Database No Existe

**Scenario**: `USE non_existent_db`  
**Expected**: `DatabaseError.QueryExecutionFailed("Unknown database 'non_existent_db'")`

### EC-004: Query Syntax Error

**Scenario**: `SELECT * FORM users` (typo)  
**Expected**: `DatabaseError.QueryExecutionFailed("You have an error in your SQL syntax")`

### EC-005: Connection Pool Exhausted

**Scenario**: 11 conexiones simultáneas con maxPoolSize=10  
**Expected**: Esperar hasta que se libere una conexión (HikariCP maneja)

### EC-006: Query Muy Larga

**Scenario**: `SELECT * FROM huge_table` (10M rows)  
**Expected**: Timeout después de 30s → `DatabaseError.TimeoutError("Query timeout")`

### EC-007: Tabla No Existe

**Scenario**: `SELECT * FROM non_existent_table`  
**Expected**: `DatabaseError.QueryExecutionFailed("Table 'myapp.non_existent_table' doesn't exist")`

### EC-008: NULL Values

**Scenario**: Columna con `NULL` en `Map<String, Any?>`  
**Expected**: `row["column"] == null` (mantener null)

### EC-009: Transacción Sin Commit

**Scenario**: `beginTransaction()` pero se cierra la app  
**Expected**: Auto-rollback

### EC-010: SSL Certificate Inválido

**Scenario**: Certificado SSL expirado  
**Expected**: `DatabaseError.ConnectionFailed("SSL certificate problem")`

---

## 6. Testing Scenarios

### TS-001: Connect Success

```kotlin
@Test
fun `connect returns success when credentials valid`() = runTest {
    val result = engine.connect(validConfig)
    assertTrue(result.isSuccess)
}
```

### TS-002: Connect Failure - Invalid Host

```kotlin
@Test
fun `connect returns error when host unreachable`() = runTest {
    val config = validConfig.copy(host = "invalid.host")
    val result = engine.connect(config)
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is DatabaseError.ConnectionFailed)
}
```

### TS-003: Execute Query Success

```kotlin
@Test
fun `executeQuery returns rows when query valid`() = runTest {
    val query = "SELECT id, name FROM users LIMIT 10"
    val result = engine.executeQuery(query)
    
    result.onSuccess { queryResult ->
        assertEquals(listOf("id", "name"), queryResult.columns)
        assertTrue(queryResult.rowCount <= 10)
    }
}
```

### TS-004: Execute Query Empty Result

```kotlin
@Test
fun `executeQuery returns empty when no rows match`() = runTest {
    val query = "SELECT * FROM users WHERE id = ?"
    val result = engine.executeQuery(query, listOf(999999))
    
    result.onSuccess { queryResult ->
        assertEquals(0, queryResult.rowCount)
        assertTrue(queryResult.rows.isEmpty())
    }
}
```

### TS-005: Execute Update Success

```kotlin
@Test
fun `executeUpdate returns affected rows`() = runTest {
    val query = "UPDATE users SET status = ? WHERE id = ?"
    val result = engine.executeUpdate(query, listOf("inactive", 1))
    
    result.onSuccess { affectedRows ->
        assertEquals(1, affectedRows)
    }
}
```

### TS-006: Transaction Commit

```kotlin
@Test
fun `transaction commits successfully`() = runTest {
    val transaction = engine.beginTransaction().getOrThrow()
    
    engine.executeUpdate("INSERT INTO users (name) VALUES (?)", listOf("Test"))
    transaction.commit()
    
    val result = engine.executeQuery("SELECT * FROM users WHERE name = ?", listOf("Test"))
    assertTrue(result.isSuccess)
}
```

### TS-007: Transaction Rollback

```kotlin
@Test
fun `transaction rolls back on error`() = runTest {
    val transaction = engine.beginTransaction().getOrThrow()
    
    try {
        engine.executeUpdate("INSERT INTO users (name) VALUES (?)", listOf("Test"))
        engine.executeUpdate("INVALID SQL")
        transaction.commit()
        fail("Should have thrown exception")
    } catch (e: Exception) {
        transaction.rollback()
    }
    
    val result = engine.executeQuery("SELECT * FROM users WHERE name = ?", listOf("Test"))
    result.onSuccess { queryResult ->
        assertEquals(0, queryResult.rowCount)
    }
}
```

### TS-008: Get Tables Success

```kotlin
@Test
fun `getTables returns list of tables`() = runTest {
    val result = engine.getTables("myapp")
    
    result.onSuccess { tables ->
        assertTrue(tables.isNotEmpty())
        tables.forEach { table ->
            assertNotNull(table.name)
            assertNotNull(table.type)
        }
    }
}
```

### TS-009: Get Columns Success

```kotlin
@Test
fun `getColumns returns list of columns`() = runTest {
    val result = engine.getColumns("users")
    
    result.onSuccess { columns ->
        assertTrue(columns.isNotEmpty())
        val idColumn = columns.find { it.name == "id" }
        assertNotNull(idColumn)
        assertEquals(ColumnKey.PRIMARY, idColumn?.key)
    }
}
```

### TS-010: Connection Pool Reuse

```kotlin
@Test
fun `connection pool reuses connections`() = runTest {
    repeat(20) {
        engine.executeQuery("SELECT 1")
    }
    // HikariCP debe reutilizar conexiones sin crear 20 nuevas
}
```

---

## 7. Out of Scope

❌ SSH Tunneling (será en `ssh-tunneling` change)  
❌ Backup/Restore (será en `backup-restore` change)  
❌ Import/Export CSV/JSON (será en `import-export` change)  
❌ Visual Query Builder (v2.0)  
❌ PostgreSQL/SQLite engines (v1.1)  
❌ NoSQL support (v3.1)  

---

## 8. Dependencies

```kotlin
// build.gradle.kts (app)
dependencies {
    // MySQL Driver
    implementation("mysql:mysql-connector-java:8.0.33")
    
    // MariaDB Driver
    implementation("org.mariadb.jdbc:mariadb-java-client:3.1.4")
    
    // Connection Pooling
    implementation("com.zaxxer:HikariCP:5.0.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Testing
    testImplementation("org.testcontainers:mysql:1.19.0")
    testImplementation("org.testcontainers:mariadb:1.19.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
```

---

## 9. Success Criteria

✅ MySQLEngine conecta a MySQL 5.7, 8.0, 8.1  
✅ MariaDBEngine conecta a MariaDB 10.5, 10.11, 11.0  
✅ Ejecuta queries SELECT/INSERT/UPDATE/DELETE con prepared statements  
✅ Obtiene metadata completa (databases, tables, columns, indexes, foreign keys)  
✅ Connection pooling funciona sin leaks  
✅ Transacciones commit/rollback funcionan  
✅ 80%+ code coverage en unit tests  
✅ Integration tests con Docker pasan  
✅ Maneja errores gracefully (no crashes)  
✅ APK size < 15MB con drivers incluidos  

---

**Status**: Ready for Design Phase
