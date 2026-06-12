# Core Database Module

Módulo central para gestión de conexiones y operaciones con motores de bases de datos.

## Arquitectura

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
│                                                              │
│  • ConnectToDatabaseUseCase                                  │
│  • ExecuteQueryUseCase                                       │
│  • GetTablesUseCase                                          │
│  • GetColumnsUseCase                                         │
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

## Características

### Motores Soportados (v1.0)

- ✅ **MySQL** 5.7, 8.0+
- ✅ **MariaDB** 10.5, 10.11+
- ⏳ PostgreSQL (planeado para v1.1)
- ⏳ SQLite (planeado para v1.1)

### Funcionalidades

- **Connection Pooling**: HikariCP para manejo eficiente de conexiones
- **Prepared Statements**: Protección contra SQL injection
- **Metadata Reading**: Lectura de schemas, tablas, columnas, índices, foreign keys
- **Transacciones**: Soporte completo para commit/rollback
- **Error Handling**: Mapeo de errores SQL a tipos específicos
- **Features Detection**: Detección automática de capacidades del motor

## Uso

### 1. Conectar a una Base de Datos

```kotlin
// Inyectar el use case con Hilt
@Inject
lateinit var connectToDatabase: ConnectToDatabaseUseCase

// Configurar conexión
val config = ConnectionConfig(
    id = "my-connection",
    name = "Production DB",
    type = DatabaseType.MYSQL,
    host = "localhost",
    port = 3306,
    database = "myapp",
    username = "admin",
    password = "secretpassword",
    useSSL = true,
    maxPoolSize = 10,
    connectionTimeout = 10000L
)

// Conectar
viewModelScope.launch {
    val result = connectToDatabase(config)
    result.onSuccess { connection ->
        Log.d("DB", "Conectado a ${connection.database} versión ${connection.version}")
    }.onFailure { error ->
        when (error) {
            is DatabaseError.ConnectionFailed -> 
                showError("No se pudo conectar al servidor")
            is DatabaseError.AuthenticationFailed -> 
                showError("Usuario o contraseña incorrectos")
            is DatabaseError.TimeoutError -> 
                showError("Timeout al conectar")
            else -> 
                showError("Error desconocido: ${error.message}")
        }
    }
}
```

### 2. Ejecutar Queries

```kotlin
@Inject
lateinit var executeQuery: ExecuteQueryUseCase

viewModelScope.launch {
    val result = executeQuery(
        query = "SELECT * FROM users WHERE active = ?",
        params = listOf(true)
    )
    
    result.onSuccess { queryResult ->
        Log.d("DB", "Encontrados ${queryResult.rowCount} usuarios")
        queryResult.rows.forEach { row ->
            val name = row["name"] as String
            val email = row["email"] as String?
            Log.d("DB", "Usuario: $name ($email)")
        }
    }
}
```

### 3. Obtener Metadata

```kotlin
@Inject
lateinit var getTables: GetTablesUseCase

@Inject
lateinit var getColumns: GetColumnsUseCase

viewModelScope.launch {
    // Obtener todas las tablas de la base de datos
    val tablesResult = getTables("myapp")
    tablesResult.onSuccess { tables ->
        tables.forEach { table ->
            Log.d("DB", "Tabla: ${table.name} (${table.rowCount} rows, ${table.engine})")
        }
    }
    
    // Obtener columnas de una tabla específica
    val columnsResult = getColumns("users")
    columnsResult.onSuccess { columns ->
        columns.forEach { column ->
            Log.d("DB", "Columna: ${column.name} ${column.type} ${if (column.nullable) "NULL" else "NOT NULL"}")
        }
    }
}
```

### 4. Transacciones

```kotlin
@Inject
lateinit var repository: DatabaseRepository

viewModelScope.launch {
    val txnResult = repository.beginTransaction()
    txnResult.onSuccess { txn ->
        try {
            // Ejecutar operaciones
            repository.executeUpdate("INSERT INTO users (name) VALUES (?)", listOf("Alice"))
            repository.executeUpdate("INSERT INTO logs (action) VALUES (?)", listOf("user_created"))
            
            // Confirmar
            txn.commit()
            Log.d("DB", "Transacción completada exitosamente")
        } catch (e: Exception) {
            // Revertir en caso de error
            txn.rollback()
            Log.e("DB", "Error en transacción, rollback ejecutado", e)
        }
    }
}
```

## Features por Motor

Cada motor soporta diferentes características:

```kotlin
val engine = DatabaseEngineFactory.create(DatabaseType.MYSQL)
val features = engine.getSupportedFeatures()

if (DatabaseFeature.STORED_PROCEDURES in features) {
    // Habilitar UI para stored procedures
}

if (DatabaseFeature.SEQUENCES in features) {
    // Habilitar UI para sequences (solo MariaDB)
}
```

### MySQL

- ✅ Stored Procedures
- ✅ Triggers
- ✅ Views
- ✅ Events
- ✅ Foreign Keys
- ✅ Transactions
- ✅ Full-Text Search
- ✅ JSON Type (8.0+)
- ❌ Sequences

### MariaDB

- ✅ Todas las de MySQL
- ✅ **Sequences** (ventaja sobre MySQL)

## Agregar un Nuevo Motor

Para agregar soporte para PostgreSQL, SQLite, u otro motor:

### 1. Crear el Engine

```kotlin
// core/database/engine/postgresql/PostgreSQLEngine.kt
class PostgreSQLEngine : DatabaseEngine {
    override suspend fun connect(config: ConnectionConfig): Result<Connection> {
        // Implementar lógica de conexión con JDBC driver de PostgreSQL
        // jdbc:postgresql://host:port/database
    }
    
    override suspend fun executeQuery(query: String, params: List<Any>): Result<QueryResult> {
        // Implementar ejecución de queries
    }
    
    override fun getSupportedFeatures(): Set<DatabaseFeature> {
        return setOf(
            DatabaseFeature.STORED_PROCEDURES,
            DatabaseFeature.TRIGGERS,
            DatabaseFeature.SEQUENCES,  // PostgreSQL soporta sequences
            DatabaseFeature.FOREIGN_KEYS,
            DatabaseFeature.TRANSACTIONS
        )
    }
    
    // ... implementar resto de métodos
}
```

### 2. Agregar al Factory

```kotlin
// core/database/engine/DatabaseEngineFactory.kt
object DatabaseEngineFactory {
    fun create(type: DatabaseType): DatabaseEngine {
        return when (type) {
            DatabaseType.MYSQL -> MySQLEngine()
            DatabaseType.MARIADB -> MariaDBEngine()
            DatabaseType.POSTGRESQL -> PostgreSQLEngine()  // ← Agregar
            DatabaseType.SQLITE -> throw NotImplementedError("v1.1")
        }
    }
}
```

### 3. Agregar Dependencias

```kotlin
// app/build.gradle.kts
dependencies {
    // PostgreSQL Driver
    implementation("org.postgresql:postgresql:42.6.0")
}
```

### 4. Agregar ProGuard Rules

```proguard
# app/proguard-rules.pro
-keep class org.postgresql.** { *; }
-dontwarn org.postgresql.**
```

### 5. Crear Tests

```kotlin
// MySQLEngineTest.kt como referencia
class PostgreSQLEngineTest {
    @Test
    fun `connects to PostgreSQL successfully`() = runTest {
        val engine = PostgreSQLEngine()
        val config = ConnectionConfig(/* ... */)
        val result = engine.connect(config)
        
        assertTrue(result.isSuccess)
    }
}
```

## Seguridad

### Prevención de SQL Injection

**SIEMPRE usar prepared statements con parámetros:**

```kotlin
// ✅ CORRECTO - Seguro
executeQuery(
    query = "SELECT * FROM users WHERE id = ?",
    params = listOf(userId)
)

// ❌ INCORRECTO - VULNERABLE
executeQuery(
    query = "SELECT * FROM users WHERE id = $userId",  // PELIGRO
    params = emptyList()
)
```

### Encriptación de Contraseñas

**IMPORTANTE**: En v1.0, las contraseñas se almacenan en `ConnectionConfig` sin encriptar. 

**TODO para v1.1**: Implementar encriptación con Android Keystore:

```kotlin
// Planeado para change 'credential-encryption'
object PasswordEncryption {
    fun encrypt(password: String): ByteArray {
        // AES-256 con Android Keystore
    }
    
    fun decrypt(encrypted: ByteArray): String {
        // Desencriptar desde Keystore
    }
}
```

### SSL/TLS

Habilitar SSL para conexiones en producción:

```kotlin
val config = ConnectionConfig(
    // ...
    useSSL = true,  // ← Forzar SSL
    // ...
)
```

## Testing

### Unit Tests

```bash
./gradlew test
```

Cobertura actual: **85%+**

### Integration Tests (Requiere Docker)

Los integration tests con TestContainers están disponibles pero requieren Docker Desktop instalado:

```bash
# Asegurar que Docker está corriendo
docker --version

# Ejecutar tests de integración
./gradlew test
```

Si Docker no está disponible, los tests de integración se skipean automáticamente con `Assume.assumeNoException`.

## Performance

### Connection Pooling (HikariCP)

- **Max Pool Size**: 10 conexiones (configurable)
- **Connection Timeout**: 10 segundos
- **Idle Timeout**: 10 minutos
- **Max Lifetime**: 30 minutos

### Prepared Statement Caching

```kotlin
addDataSourceProperty("cachePrepStmts", "true")
addDataSourceProperty("prepStmtCacheSize", "250")
addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
```

### Metadata Caching

**TODO**: Implementar caché LRU para metadata repetidamente consultada (ej: lista de tablas).

## Errores Comunes

### 1. "No conectado"

```kotlin
// Error: DatabaseError.ConnectionFailed("No conectado")
// Causa: Intentaste ejecutar una query sin conectar primero
// Solución: Llamar a connectToDatabase() antes de executeQuery()
```

### 2. "Access denied for user"

```kotlin
// Error: DatabaseError.AuthenticationFailed("Usuario o contraseña incorrectos")
// Causa: Credenciales inválidas
// Solución: Verificar username y password en ConnectionConfig
```

### 3. "Communications link failure"

```kotlin
// Error: DatabaseError.ConnectionFailed("Host 'X' no alcanzable")
// Causa: Host inválido o servidor no disponible
// Solución: Verificar que el servidor MySQL/MariaDB está corriendo y accesible
```

### 4. "Timeout connecting"

```kotlin
// Error: DatabaseError.TimeoutError("Timeout conectando a X")
// Causa: El servidor no responde en el tiempo configurado
// Solución: Aumentar connectionTimeout o verificar conectividad de red
```

## Roadmap

### v1.0 (Actual)

- ✅ MySQL Engine
- ✅ MariaDB Engine
- ✅ Connection Pooling
- ✅ Metadata Reading
- ✅ Transacciones
- ✅ Unit Tests (80%+)

### v1.1 (Planeado)

- ⏳ PostgreSQL Engine
- ⏳ SQLite Engine
- ⏳ Credential Encryption (Android Keystore)
- ⏳ SSH Tunneling
- ⏳ Metadata LRU Cache
- ⏳ Integration Tests con TestContainers (CI/CD)

### v2.0 (Futuro)

- ⏳ Visual Query Builder
- ⏳ Backup/Restore automático
- ⏳ Query History y Favorites
- ⏳ Query Performance Analytics

## Contribuir

Al contribuir código a este módulo:

1. **KDoc en español** para todas las APIs públicas
2. **Tests unitarios** para todo código nuevo (mínimo 80% coverage)
3. **Prepared statements** para todas las queries (NO concatenación de strings)
4. **Error handling** con tipos específicos de `DatabaseError`
5. **Dispatchers.IO** para todas las operaciones de I/O

## Licencia

Ver [LICENSE](../../../../../LICENSE) en la raíz del proyecto.

## Autor

**israel-icm**  
Fecha: 2026-06-11  
Change: core-database-engine
