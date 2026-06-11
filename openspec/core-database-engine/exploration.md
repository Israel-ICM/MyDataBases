# Exploration: Core Database Engine

**Fecha**: 2026-06-11  
**Autor**: israel-icm  
**Change**: core-database-engine  

---

## Problema

Necesitamos una arquitectura extensible para soportar múltiples motores de bases de datos (MySQL, MariaDB, PostgreSQL, SQLite, SQL Server, Oracle, MongoDB) sin duplicar código ni romper funcionalidad existente al agregar nuevos motores.

### Requisitos

1. **Extensibilidad**: Agregar un nuevo motor debe ser crear una clase nueva, sin modificar código existente
2. **Abstracción común**: Todos los motores comparten operaciones básicas (conectar, ejecutar queries, obtener metadatos)
3. **Motor-específico**: Cada motor puede tener features únicas (ej: PostgreSQL schemas, MySQL eventos)
4. **Testing**: Fácil de mockear para tests
5. **Performance**: No sacrificar performance por abstracción

### Motores Fase 1 (v1.0)

- MySQL
- MariaDB

### Motores Futuro

- PostgreSQL (v1.1)
- SQLite (v1.1)
- Amazon RDS/Aurora (v1.4)
- SQL Server (v3.1)
- Oracle (v3.1)
- MongoDB (v3.1)

---

## Alternativas Investigadas

### Opción 1: Herencia (Rejected)

```kotlin
abstract class DatabaseEngine {
    abstract fun connect()
    abstract fun executeQuery()
}

class MySQLEngine : DatabaseEngine() { ... }
class PostgreSQLEngine : DatabaseEngine() { ... }
```

**Pros**:
- Simple
- Reutilización de código base

**Cons**:
- ❌ Acoplamiento fuerte
- ❌ Difícil mockear
- ❌ No permite composición
- ❌ Violación de LSP si motores tienen comportamientos muy diferentes

**Decisión**: ❌ Rechazado

---

### Opción 2: Interface + Factory (Selected ✅)

```kotlin
interface DatabaseEngine {
    suspend fun connect(config: ConnectionConfig): Result<Connection>
    suspend fun executeQuery(query: String): Result<QueryResult>
}

class MySQLEngine : DatabaseEngine { ... }
class PostgreSQLEngine : DatabaseEngine { ... }

object DatabaseEngineFactory {
    fun create(type: DatabaseType): DatabaseEngine
}
```

**Pros**:
- ✅ Bajo acoplamiento
- ✅ Fácil testear (mock interface)
- ✅ Composición over herencia
- ✅ Open/Closed Principle
- ✅ Cada motor encapsula su lógica

**Cons**:
- Más clases
- Factory puede crecer

**Decisión**: ✅ **ELEGIDA**

---

### Opción 3: Plugin System con ServiceLoader (Over-engineering)

```kotlin
interface DatabaseEnginePlugin {
    fun supports(type: DatabaseType): Boolean
    fun create(): DatabaseEngine
}

// ServiceLoader discovery
```

**Pros**:
- Plugins dinámicos
- No modificar factory

**Cons**:
- ❌ Complejidad innecesaria para v1.0
- ❌ Overhead de ServiceLoader
- ❌ Difícil debug

**Decisión**: ❌ Rechazado (over-engineering)

---

## Arquitectura Propuesta

### Strategy Pattern + Factory Pattern

```
┌─────────────────────────────────────────┐
│         DatabaseEngineFactory           │
│  (crea el motor según DatabaseType)     │
└─────────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│         DatabaseEngine (interface)      │
│  - connect()                            │
│  - executeQuery()                       │
│  - getTables()                          │
│  - ...                                  │
└─────────────────────────────────────────┘
         ▲                ▲              ▲
         │                │              │
    MySQLEngine    MariaDBEngine   PostgreSQLEngine
```

### Módulo: `core-database`

```
core-database/
├── engine/
│   ├── DatabaseEngine.kt           # Interface común
│   ├── DatabaseEngineFactory.kt    # Factory
│   ├── DatabaseType.kt             # Enum de motores
│   ├── DatabaseFeature.kt          # Enum de features
│   ├── mysql/
│   │   └── MySQLEngine.kt
│   └── mariadb/
│       └── MariaDBEngine.kt
├── models/
│   ├── Connection.kt
│   ├── ConnectionConfig.kt
│   ├── QueryResult.kt
│   ├── Table.kt
│   ├── Column.kt
│   ├── Database.kt
│   └── ...
└── repository/
    └── DatabaseRepository.kt       # Capa de acceso
```

---

## Features por Motor

| Feature | MySQL | MariaDB | PostgreSQL | SQLite |
|---------|-------|---------|------------|--------|
| Connect | ✅ | ✅ | ✅ | ✅ |
| Execute Query | ✅ | ✅ | ✅ | ✅ |
| Prepared Statements | ✅ | ✅ | ✅ | ✅ |
| Transactions | ✅ | ✅ | ✅ | ✅ |
| Stored Procedures | ✅ | ✅ | ✅ | ❌ |
| Triggers | ✅ | ✅ | ✅ | ✅ |
| Views | ✅ | ✅ | ✅ | ✅ |
| Schemas | ❌ | ❌ | ✅ | ❌ |
| Sequences | ❌ | ✅ | ✅ | ❌ |
| Events | ✅ | ✅ | ❌ | ❌ |

**Solución**: `DatabaseEngine.getSupportedFeatures(): Set<DatabaseFeature>`

---

## Drivers

### MySQL

```gradle
implementation("mysql:mysql-connector-java:8.0.33")
```

### MariaDB

```gradle
implementation("org.mariadb.jdbc:mariadb-java-client:3.1.4")
```

### PostgreSQL (futuro)

```gradle
implementation("org.postgresql:postgresql:42.6.0")
```

### SQLite (futuro)

```gradle
// Android incluye SQLite nativo
```

---

## Connection Pooling

### Opción 1: HikariCP

```gradle
implementation("com.zaxxer:HikariCP:5.0.1")
```

**Pros**: Mejor performance, configuración simple  
**Cons**: Overhead para SQLite local

**Decisión**: Usar para MySQL/MariaDB/PostgreSQL, no para SQLite

---

## Testing Strategy

### Unit Tests

```kotlin
class MySQLEngineTest {
    private val mockConnection: Connection = mockk()
    private val engine = MySQLEngine()
    
    @Test
    fun `connect returns success when credentials valid`() = runTest {
        // ...
    }
}
```

### Integration Tests

```kotlin
@Test
fun `MySQL engine connects to real database`() = runTest {
    // Requiere Docker con MySQL
    val config = ConnectionConfig(
        host = "localhost",
        port = 3306,
        database = "test_db",
        username = "test",
        password = "test"
    )
    
    val result = engine.connect(config)
    assertTrue(result.isSuccess)
}
```

### Test Containers

```gradle
testImplementation("org.testcontainers:mysql:1.19.0")
testImplementation("org.testcontainers:mariadb:1.19.0")
```

---

## Seguridad

1. **Prepared Statements**: SIEMPRE, nunca concatenar strings
2. **Credential Encryption**: Android Keystore para passwords
3. **SSL/TLS**: Soportar conexiones encriptadas
4. **Timeout**: Evitar queries infinitas
5. **Connection Limits**: Pool size limitado

---

## Performance Considerations

1. **Lazy Loading**: No cargar todas las tablas de golpe
2. **Pagination**: Queries grandes deben paginar
3. **Connection Reuse**: Pool de conexiones
4. **Background Threads**: IO en Dispatchers.IO
5. **Cancel Support**: Cancelar queries largas

---

## Próximos Pasos

1. ✅ Exploration completada
2. ⏳ Crear Proposal con decisiones finales
3. ⏳ Crear Spec detallada
4. ⏳ Crear Design técnico
5. ⏳ Crear Tasks de implementación

---

**Decisión Final**: Interface + Factory con Strategy Pattern, HikariCP para pooling, JDBC drivers, TestContainers para integration tests.
