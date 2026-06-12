# Tasks: Core Database Engine

**Fecha**: 2026-06-11  
**Autor**: israel-icm  
**Change**: core-database-engine  
**Status**: Ready for Implementation  

---

## Task Breakdown

### Task 1: Setup Dependencies y Estructura Base ✅

**Prioridad**: MUST  
**Estimación**: 1 hora  
**Dependencias**: Ninguna  

**Subtasks**:
1. ✅ Agregar dependencies a `build.gradle.kts`:
   - `mysql:mysql-connector-java:8.0.33`
   - `org.mariadb.jdbc:mariadb-java-client:3.1.4`
   - `com.zaxxer:HikariCP:5.0.1`
   - TestContainers para testing

2. ✅ Crear estructura de paquetes:
   ```
   core/database/
   ├── engine/
   ├── models/
   ├── repository/
   └── di/
   ```

3. ✅ Configurar ProGuard rules para JDBC drivers

**Acceptance Criteria**:
- Proyecto compila sin errores
- Dependencies se resuelven correctamente
- Estructura de paquetes creada

---

### Task 2: Crear Data Models ✅

**Prioridad**: MUST  
**Estimación**: 2 horas  
**Dependencias**: Task 1  

**Archivos a crear**:
1. `models/ConnectionConfig.kt` - @Parcelize data class
2. `models/Connection.kt` - Data class
3. `models/QueryResult.kt` - Data class
4. `models/Table.kt` + `TableType` enum
5. `models/Column.kt` + `ColumnKey` enum
6. `models/Database.kt` - Data class
7. `models/Index.kt` + `IndexType` enum
8. `models/ForeignKey.kt` + `ReferentialAction` enum
9. `models/Transaction.kt` - Data class con suspend commit/rollback
10. `models/DatabaseError.kt` - Sealed class

**Acceptance Criteria**:
- Todos los modelos compilan
- `@Parcelize` funciona en `ConnectionConfig`
- Sealed class `DatabaseError` tiene todos los casos
- KDoc completo con `@author israel-icm` y `@date 2026-06-11`

**Tests**:
```kotlin
@Test
fun `ConnectionConfig parcelable works`() {
    val config = ConnectionConfig(...)
    val parcel = Parcel.obtain()
    config.writeToParcel(parcel, 0)
    parcel.setDataPosition(0)
    val fromParcel = ConnectionConfig.createFromParcel(parcel)
    assertEquals(config, fromParcel)
}
```

---

### Task 3: Crear DatabaseEngine Interface y Enums ✅

**Prioridad**: MUST  
**Estimación**: 1 hora  
**Dependencias**: Task 2  

**Archivos a crear**:
1. `engine/DatabaseEngine.kt` - Interface con todos los métodos
2. `engine/DatabaseType.kt` - Enum (MYSQL, MARIADB, POSTGRESQL, SQLITE)
3. `engine/DatabaseFeature.kt` - Enum (STORED_PROCEDURES, TRIGGERS, etc.)

**Acceptance Criteria**:
- Interface `DatabaseEngine` tiene todos los métodos del spec
- Cada método tiene KDoc completo
- `DatabaseType` enum tiene displayName, defaultPort, iconRes
- `DatabaseFeature` enum tiene todos los features necesarios

---

### Task 4: Implementar MySQLConnectionPool ✅

**Prioridad**: MUST  
**Estimación**: 2 horas  
**Dependencias**: Task 2, Task 3  

**Archivo a crear**:
- `engine/mysql/MySQLConnectionPool.kt`

**Implementación**:
- Usar HikariCP
- Configurar pool settings (maxPoolSize, timeouts, etc.)
- SSL/TLS support
- Performance optimizations (cachePrepStmts, etc.)

**Acceptance Criteria**:
- Pool se crea correctamente con `ConnectionConfig`
- `getConnection()` retorna conexión válida
- `close()` libera recursos sin leaks
- SSL se habilita cuando `config.useSSL = true`

**Tests**:
```kotlin
@Test
fun `connection pool creates valid connections`() = runTest {
    val pool = MySQLConnectionPool(testConfig)
    val connection = pool.getConnection()
    assertNotNull(connection)
    assertTrue(connection.isValid(5))
    pool.close()
}
```

---

### Task 5: Implementar MySQLMetadataReader ✅

**Prioridad**: MUST  
**Estimación**: 3 horas  
**Dependencias**: Task 2  

**Archivo a crear**:
- `engine/mysql/MySQLMetadataReader.kt`

**Métodos**:
1. `readDatabases()` - Query a `information_schema.SCHEMATA`
2. `readTables()` - Query a `information_schema.TABLES`
3. `readColumns()` - Query a `information_schema.COLUMNS`
4. `readIndexes()` - Query a `information_schema.STATISTICS`
5. `readForeignKeys()` - Query a `information_schema.KEY_COLUMN_USAGE`

**Acceptance Criteria**:
- Todas las queries funcionan correctamente
- Parseo de tipos (`TableType`, `ColumnKey`, etc.) correcto
- Manejo de NULL values
- Ordenamiento correcto (alfabético para tablas, posición para columnas)

**Tests**:
```kotlin
@Test
fun `readTables returns list of tables`() = runTest {
    val connection = mockConnection()
    val reader = MySQLMetadataReader()
    val tables = reader.readTables(connection, QUERY, "test_db")
    assertTrue(tables.isNotEmpty())
}
```

---

### Task 6: Implementar MySQLEngine ✅

**Prioridad**: MUST  
**Estimación**: 4 horas  
**Dependencias**: Task 3, Task 4, Task 5  

**Archivo a crear**:
- `engine/mysql/MySQLEngine.kt`

**Métodos a implementar**:
1. `connect()` - Crear pool, test connection, mapear errores
2. `disconnect()` - Cerrar pool
3. `executeQuery()` - PreparedStatement, bindear params, parsear ResultSet
4. `executeUpdate()` - PreparedStatement, retornar affected rows
5. `getDatabases()` - Usar MySQLMetadataReader
6. `getTables()` - Usar MySQLMetadataReader
7. `getColumns()` - Usar MySQLMetadataReader
8. `getIndexes()` - Usar MySQLMetadataReader
9. `getForeignKeys()` - Usar MySQLMetadataReader
10. `beginTransaction()` - Set autoCommit = false
11. `getVersion()` - Query `SELECT VERSION()`
12. `getSupportedFeatures()` - Retornar Set de features

**Error Handling**:
- `SQLException` → `DatabaseError.QueryExecutionFailed`
- `SQLNonTransientConnectionException` → `DatabaseError.ConnectionFailed`
- `SocketTimeoutException` → `DatabaseError.TimeoutError`

**Acceptance Criteria**:
- Todos los métodos implementados
- Error handling completo
- Todo en `Dispatchers.IO`
- KDoc completo

**Tests**:
```kotlin
@Test
fun `connect returns success when credentials valid`() = runTest {
    val engine = MySQLEngine()
    val result = engine.connect(validConfig)
    assertTrue(result.isSuccess)
}

@Test
fun `executeQuery returns rows`() = runTest {
    val engine = MySQLEngine()
    engine.connect(validConfig)
    val result = engine.executeQuery("SELECT 1 as num")
    assertTrue(result.isSuccess)
    assertEquals(1, result.getOrNull()?.rowCount)
}
```

---

### Task 7: Implementar MariaDBEngine ✅

**Prioridad**: MUST  
**Estimación**: 1 hora  
**Dependencias**: Task 6  

**Archivo a crear**:
- `engine/mariadb/MariaDBEngine.kt`

**Implementación**:
- Delegar mayoría de métodos a `MySQLEngine`
- Override `getSupportedFeatures()` para agregar `SEQUENCES`

**Acceptance Criteria**:
- Conecta a MariaDB correctamente
- Todas las operaciones funcionan
- `getSupportedFeatures()` incluye `SEQUENCES`

**Tests**:
```kotlin
@Test
fun `MariaDBEngine supports SEQUENCES`() {
    val engine = MariaDBEngine()
    assertTrue(DatabaseFeature.SEQUENCES in engine.getSupportedFeatures())
}
```

---

### Task 8: Implementar DatabaseEngineFactory ✅

**Prioridad**: MUST  
**Estimación**: 30 minutos  
**Dependencias**: Task 6, Task 7  

**Archivo a crear**:
- `engine/DatabaseEngineFactory.kt`

**Implementación**:
```kotlin
object DatabaseEngineFactory {
    fun create(type: DatabaseType): DatabaseEngine {
        return when (type) {
            DatabaseType.MYSQL -> MySQLEngine()
            DatabaseType.MARIADB -> MariaDBEngine()
            DatabaseType.POSTGRESQL -> throw NotImplementedError("v1.1")
            DatabaseType.SQLITE -> throw NotImplementedError("v1.1")
        }
    }
}
```

**Acceptance Criteria**:
- Factory crea MySQLEngine cuando type = MYSQL
- Factory crea MariaDBEngine cuando type = MARIADB
- Factory lanza NotImplementedError para PostgreSQL y SQLite

**Tests**:
```kotlin
@Test
fun `factory creates MySQLEngine for MYSQL type`() {
    val engine = DatabaseEngineFactory.create(DatabaseType.MYSQL)
    assertTrue(engine is MySQLEngine)
}
```

---

### Task 9: Implementar DatabaseRepository

**Prioridad**: MUST  
**Estimación**: 2 horas  
**Dependencias**: Task 8  

**Archivos a crear**:
1. `repository/DatabaseRepository.kt` - Interface
2. `repository/DatabaseRepositoryImpl.kt` - Implementación

**Implementación**:
- Mantener referencia a `currentEngine`
- Delegar todas las llamadas al engine actual
- Retornar error si no hay engine conectado

**Acceptance Criteria**:
- Todos los métodos implementados
- Manejo correcto de "no conectado"
- Inyectable con Hilt

**Tests**:
```kotlin
@Test
fun `repository returns error when not connected`() = runTest {
    val repository = DatabaseRepositoryImpl(DatabaseEngineFactory)
    val result = repository.executeQuery("SELECT 1")
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is DatabaseError.ConnectionFailed)
}
```

---

### Task 10: Crear Hilt Module

**Prioridad**: MUST  
**Estimación**: 30 minutos  
**Dependencias**: Task 9  

**Archivo a crear**:
- `di/DatabaseModule.kt`

**Implementación**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabaseEngineFactory(): DatabaseEngineFactory = DatabaseEngineFactory
    
    @Provides
    @Singleton
    fun provideDatabaseRepository(
        factory: DatabaseEngineFactory
    ): DatabaseRepository = DatabaseRepositoryImpl(factory)
}
```

**Acceptance Criteria**:
- Module compila
- Repository se inyecta correctamente en ViewModels

---

### Task 11: Crear Use Cases

**Prioridad**: MUST  
**Estimación**: 1 hora  
**Dependencias**: Task 9  

**Archivos a crear**:
1. `domain/usecases/ConnectToDatabaseUseCase.kt`
2. `domain/usecases/ExecuteQueryUseCase.kt`
3. `domain/usecases/ExecuteUpdateUseCase.kt`
4. `domain/usecases/GetDatabasesUseCase.kt`
5. `domain/usecases/GetTablesUseCase.kt`
6. `domain/usecases/GetColumnsUseCase.kt`

**Acceptance Criteria**:
- Todos los use cases implementados
- Inyectable con Hilt
- KDoc completo

**Tests**:
```kotlin
@Test
fun `ConnectToDatabaseUseCase calls repository`() = runTest {
    val mockRepo = mockk<DatabaseRepository>()
    val useCase = ConnectToDatabaseUseCase(mockRepo)
    
    coEvery { mockRepo.connect(any()) } returns Result.success(mockConnection)
    
    val result = useCase(testConfig)
    assertTrue(result.isSuccess)
    
    coVerify { mockRepo.connect(testConfig) }
}
```

---

### Task 12: Unit Tests

**Prioridad**: MUST  
**Estimación**: 4 horas  
**Dependencias**: Task 1-11  

**Tests a crear**:
1. `MySQLEngineTest.kt` - Mock de connection pool y statements
2. `MariaDBEngineTest.kt` - Validar features
3. `DatabaseEngineFactoryTest.kt` - Verificar creación correcta
4. `DatabaseRepositoryImplTest.kt` - Mock de engine
5. `MySQLMetadataReaderTest.kt` - Mock de ResultSet
6. `QueryResultTest.kt` - Validar parseo
7. Todos los Use Cases

**Acceptance Criteria**:
- 80%+ code coverage
- Todos los tests pasan
- Edge cases cubiertos (null values, empty results, errors)

---

### Task 13: Integration Tests con Docker

**Prioridad**: SHOULD  
**Estimación**: 3 horas  
**Dependencias**: Task 12  

**Tests a crear**:
1. `MySQLEngineIntegrationTest.kt` - TestContainers MySQL 8.0
2. `MariaDBEngineIntegrationTest.kt` - TestContainers MariaDB 10.11

**Scenarios**:
- Conectar a instancia real
- Ejecutar queries SELECT/INSERT/UPDATE/DELETE
- Obtener metadata (databases, tables, columns)
- Transacciones commit/rollback
- Manejo de errores (credenciales inválidas, timeout)

**Acceptance Criteria**:
- Tests pasan con Docker disponible
- Tests se skipean gracefully si Docker no está disponible

---

### Task 14: ProGuard Rules ✅

**Prioridad**: MUST  
**Estimación**: 30 minutos  
**Dependencias**: Task 1  

**Archivo a crear/modificar**:
- `proguard-rules.pro`

**Rules necesarias**:
```proguard
# MySQL Connector
-keep class com.mysql.** { *; }
-dontwarn com.mysql.**

# MariaDB Client
-keep class org.mariadb.** { *; }
-dontwarn org.mariadb.**

# HikariCP
-keep class com.zaxxer.hikari.** { *; }
-dontwarn com.zaxxer.hikari.**

# JDBC
-keep class java.sql.** { *; }
-keep class javax.sql.** { *; }
```

**Acceptance Criteria**:
- APK release compila correctamente
- Drivers funcionan en release build
- APK size < 15MB

---

### Task 15: Documentación

**Prioridad**: SHOULD  
**Estimación**: 1 hora  
**Dependencias**: Task 1-14  

**Archivos a crear**:
1. README en `core/database/README.md` con:
   - Descripción del módulo
   - Arquitectura (diagrama)
   - Ejemplos de uso
   - Cómo agregar un nuevo motor

**Acceptance Criteria**:
- README completo y claro
- Ejemplos de código funcionan
- Diagrama de arquitectura incluido

---

## Testing Strategy Summary

### Unit Tests (80%+ coverage)
- ✅ MySQLEngine
- ✅ MariaDBEngine
- ✅ DatabaseEngineFactory
- ✅ DatabaseRepositoryImpl
- ✅ MySQLMetadataReader
- ✅ All Use Cases
- ✅ Data Models

### Integration Tests
- ✅ MySQL 8.0 (TestContainers)
- ✅ MariaDB 10.11 (TestContainers)

### Manual Testing
- ✅ Conectar a MySQL real
- ✅ Ejecutar queries complejas
- ✅ Probar timeout
- ✅ Probar SSL

---

## Estimated Total Time

| Task | Horas |
|------|-------|
| Task 1: Setup | 1h |
| Task 2: Models | 2h |
| Task 3: Interface | 1h |
| Task 4: ConnectionPool | 2h |
| Task 5: MetadataReader | 3h |
| Task 6: MySQLEngine | 4h |
| Task 7: MariaDBEngine | 1h |
| Task 8: Factory | 0.5h |
| Task 9: Repository | 2h |
| Task 10: Hilt | 0.5h |
| Task 11: Use Cases | 1h |
| Task 12: Unit Tests | 4h |
| Task 13: Integration Tests | 3h |
| Task 14: ProGuard | 0.5h |
| Task 15: Docs | 1h |
| **TOTAL** | **26.5h** |

**Estimación con buffer**: ~32 horas (4 días de trabajo)

---

## Implementation Order (Critical Path)

1. Task 1 → Task 2 → Task 3 (Setup base)
2. Task 4 → Task 5 (MySQL components)
3. Task 6 (MySQLEngine) ← CRITICAL
4. Task 7 → Task 8 (MariaDB + Factory)
5. Task 9 → Task 10 (Repository + DI)
6. Task 11 (Use Cases)
7. Task 12 → Task 13 (Testing)
8. Task 14 → Task 15 (Polish)

---

## Success Criteria

✅ Todos los tasks completados  
✅ 80%+ code coverage  
✅ Integration tests pasan  
✅ ProGuard build funciona  
✅ APK size < 15MB  
✅ Documentación completa  

---

**Status**: Ready to Start Implementation
