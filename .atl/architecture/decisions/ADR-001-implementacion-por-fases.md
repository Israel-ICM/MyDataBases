# ADR-001: Implementación por Fases (MySQL/MariaDB primero)

## Estado

✅ **Aceptado**

Fecha: 2026-06-11

## Contexto

Necesitamos decidir cómo implementar el soporte multi-motor de bases de datos.

**Opciones evaluadas**:

1. **Big bang**: Implementar MySQL, MariaDB, PostgreSQL y SQLite simultáneamente en v1.0
2. **Fases progresivas**: Empezar con MySQL/MariaDB en v1.0, luego expandir en v1.1+

**Riesgos de Big Bang**:

- ❌ Arquitectura compleja desde día 1
- ❌ Difícil validar si `DatabaseEngine` interface es realmente extensible
- ❌ Mantener 4 motores sin usuarios reales = desperdicio
- ❌ Bugs multiplicados por 4 motores
- ❌ Testing complejo (unit + integration para cada motor)
- ❌ Tiempo de desarrollo mucho mayor para v1.0

**Beneficios de Fases**:

- ✅ Validar arquitectura con 2 motores similares (MySQL/MariaDB)
- ✅ Lanzar v1.0 más rápido con funcionalidad completa
- ✅ Iterar en UI/UX con menos complejidad
- ✅ Feedback real de usuarios antes de escalar
- ✅ Probar extensibilidad real en v1.1 (PostgreSQL/SQLite)

## Decisión

**Implementar por fases**:

### v1.0: MySQL + MariaDB únicamente

**Razón**: 

- MySQL y MariaDB comparten sintaxis SQL y protocolo
- Son los 2 motores más usados en el mundo
- Permite validar toda la funcionalidad core rápido
- Arquitectura `DatabaseEngine` se diseña desde el principio para extensibilidad

**Funcionalidad completa en v1.0**:

- ✅ Conexiones
- ✅ Explorador de DB
- ✅ Editor SQL
- ✅ CRUD de datos
- ✅ SSH Tunneling
- ✅ Backup/Restore
- ✅ Export (CSV, JSON, SQL)

### v1.1: Agregar PostgreSQL + SQLite

**Razón**:

- PostgreSQL tiene sintaxis diferente (schemas, tipos, funciones)
- SQLite es local (sin servidor, solo archivo)
- Valida que la arquitectura es REALMENTE extensible
- Si agregar estos 2 motores NO rompe MySQL/MariaDB → arquitectura exitosa

### v1.4+: Cloud y motores adicionales

**Razón**:

- RDS/Aurora una vez que motores base estén sólidos
- SQL Server, Oracle, MongoDB según demanda de usuarios

## Consecuencias

### Positivas

- ✅ **Time-to-market más rápido**: v1.0 en menos tiempo
- ✅ **Arquitectura validada**: Si v1.1 sale bien, sabemos que podemos agregar cualquier motor
- ✅ **Menos bugs iniciales**: Solo 2 motores = menos superficie de bugs
- ✅ **Feedback temprano**: Usuarios reales validan UX antes de escalar
- ✅ **Código más limpio**: No anticipar problemas que tal vez nunca existan

### Negativas

- ⚠️ **Marketing inicial limitado**: "Solo MySQL/MariaDB" puede sonar limitado
  - **Mitigación**: Comunicar roadmap claro, v1.1 a los 2 meses
- ⚠️ **Usuarios de PostgreSQL esperan**: No pueden usar la app en v1.0
  - **Mitigación**: v1.1 sale rápido (arquitectura ya validada)

### Neutras

- 🔄 **Requiere disciplina arquitectónica**: `DatabaseEngine` DEBE diseñarse bien desde v1.0
  - **Mitigación**: SDD obligatorio, specs claras antes de codificar

## Implementación

### Estructura de Módulos

```
core-database/
├── engine/
│   ├── DatabaseEngine.kt          # Interface común (diseñada para extensibilidad)
│   ├── DatabaseEngineFactory.kt   # Factory (fácil agregar motores)
│   ├── mysql/
│   │   └── MySQLEngine.kt         # v1.0
│   └── mariadb/
│       └── MariaDBEngine.kt       # v1.0
│   # Futuro v1.1:
│   # ├── postgresql/
│   # │   └── PostgreSQLEngine.kt
│   # └── sqlite/
│   #     └── SQLiteEngine.kt
```

### Interface `DatabaseEngine`

**Debe soportar TODAS las operaciones desde v1.0**:

```kotlin
interface DatabaseEngine {
    // Conexión
    suspend fun connect(config: ConnectionConfig): Result<Connection>
    suspend fun disconnect()
    suspend fun testConnection(config: ConnectionConfig): Result<Boolean>
    
    // Metadatos
    suspend fun getDatabases(): Result<List<String>>
    suspend fun getTables(database: String): Result<List<Table>>
    suspend fun getColumns(table: String): Result<List<Column>>
    suspend fun getViews(database: String): Result<List<View>>
    suspend fun getProcedures(database: String): Result<List<Procedure>>
    suspend fun getTriggers(database: String): Result<List<Trigger>>
    
    // Queries
    suspend fun executeQuery(query: String): Result<QueryResult>
    suspend fun executeUpdate(query: String): Result<Int>
    
    // CRUD
    suspend fun insertRow(table: String, data: Map<String, Any?>): Result<Unit>
    suspend fun updateRow(table: String, id: Any, data: Map<String, Any?>): Result<Unit>
    suspend fun deleteRow(table: String, id: Any): Result<Unit>
    
    // Backup/Restore
    suspend fun createBackup(options: BackupOptions): Flow<BackupProgress>
    suspend fun restore(file: File, options: RestoreOptions): Flow<RestoreProgress>
    
    // Motor-específico
    fun getEngineName(): String
    fun getSupportedFeatures(): Set<DatabaseFeature>
}

enum class DatabaseFeature {
    STORED_PROCEDURES,
    TRIGGERS,
    VIEWS,
    SCHEMAS,
    SEQUENCES,
    CUSTOM_TYPES
}
```

### Criterio de Éxito para v1.1

**Agregar PostgreSQL y SQLite debe**:

- ✅ NO modificar `DatabaseEngine` interface (solo agregar métodos opcionales si es necesario)
- ✅ NO modificar código de MySQL/MariaDB
- ✅ NO romper tests existentes
- ✅ Reutilizar 90%+ de UI/ViewModels (solo cambiar engine)

**Si se cumple → arquitectura extensible validada → podemos agregar cualquier motor.**

## Alternativas Consideradas

### Alternativa 1: Implementar los 4 motores en v1.0

**Rechazada porque**:

- Riesgo alto de sobre-ingeniería
- Tiempo de desarrollo 3-4x mayor
- No sabríamos si la arquitectura funciona hasta que esté todo hecho
- Difícil iterar en UX con tanta complejidad

### Alternativa 2: Solo MySQL en v1.0

**Rechazada porque**:

- No valida extensibilidad (MariaDB es casi idéntico)
- Marketing muy limitado ("solo MySQL")
- No prueba `DatabaseEngineFactory`

### Alternativa 3: MySQL + PostgreSQL en v1.0

**Rechazada porque**:

- PostgreSQL tiene sintaxis MUY diferente
- MariaDB es más fácil y más usado que PostgreSQL
- Queremos validar arquitectura rápido, no complejidad máxima

## Notas

- Esta decisión NO significa que la arquitectura se diseñe solo para MySQL/MariaDB
- `DatabaseEngine` se diseña desde v1.0 pensando en PostgreSQL, SQLite, SQL Server, MongoDB
- Solo la IMPLEMENTACIÓN es por fases, no el DISEÑO

## Referencias

- `.atl/product/roadmap.md` — Roadmap actualizado con fases
- `.atl/product/vision.md` — Estrategia de implementación
- `.atl/agents/database-expert.md` — Patrón extensible con Strategy + Factory
