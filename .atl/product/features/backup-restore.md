# Feature: Backup y Restore

## Visión

Backup y Restore son funcionalidades CRÍTICAS para cualquier cliente de base de datos profesional.

**Casos de uso**:

- Hacer backup antes de cambios peligrosos
- Migrar base de datos entre servidores
- Clonar base de datos de producción a desarrollo
- Recuperación ante desastres
- Exportar datos para análisis externo

---

## Requisitos Funcionales

### 1. Tipos de Backup

#### A. Full Backup (Estructura + Datos)

**Exporta**:

- ✅ CREATE TABLE statements
- ✅ CREATE VIEW statements
- ✅ CREATE PROCEDURE / FUNCTION statements
- ✅ CREATE TRIGGER statements
- ✅ Todos los datos (INSERT statements)
- ✅ Índices
- ✅ Constraints (FK, PK, UNIQUE)

**Formato de salida**: SQL script

```sql
-- MyDataBases Backup
-- Database: production_db
-- Date: 2026-06-11 15:30:00
-- Engine: MySQL 8.0.32

SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `email` varchar(255) UNIQUE NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `users` VALUES
(1, 'John Doe', 'john@example.com'),
(2, 'Jane Smith', 'jane@example.com');

SET FOREIGN_KEY_CHECKS=1;
```

#### B. Structure Only (Solo Estructura)

**Exporta**:

- ✅ CREATE TABLE statements
- ✅ CREATE VIEW statements
- ✅ CREATE PROCEDURE / FUNCTION statements
- ✅ Índices y constraints
- ❌ NO exporta datos

**Uso**: Clonar estructura sin datos, crear entorno de desarrollo

#### C. Data Only (Solo Datos)

**Exporta**:

- ✅ INSERT statements
- ❌ NO exporta CREATE TABLE

**Uso**: Importar datos a estructura existente

#### D. Incremental Backup (Futuro - v2.0)

**Exporta solo cambios desde último backup**:

- Nuevo para MyDataBases
- Requiere tracking de cambios
- Más eficiente para DBs grandes

### 2. Opciones de Backup

**Configuración**:

```
┌─────────────────────────────────────┐
│ Backup Options                      │
├─────────────────────────────────────┤
│ Backup Type:                        │
│  ● Full Backup                      │
│  ○ Structure Only                   │
│  ○ Data Only                        │
│                                     │
│ Tables: [Select Tables...]          │
│  ☑ users (15,234 rows)              │
│  ☑ products (5,678 rows)            │
│  ☑ orders (45,123 rows)             │
│                                     │
│ Options:                            │
│  ☑ Drop tables before create        │
│  ☑ Include views                    │
│  ☑ Include procedures               │
│  ☑ Include triggers                 │
│  ☑ Add comments                     │
│  ☑ Disable foreign key checks       │
│                                     │
│ Compression:                        │
│  ○ None                             │
│  ● GZIP (.sql.gz)                   │
│  ○ ZIP (.zip)                       │
│                                     │
│ Estimated Size: 12.5 MB             │
│ Compressed: ~2.3 MB                 │
│                                     │
│ [Start Backup]                      │
└─────────────────────────────────────┘
```

### 3. Formato de Salida

**Opciones**:

1. **SQL Script** (.sql)
   - Estándar
   - Puede ejecutarse directamente
   - Portable entre sistemas

2. **Compressed SQL** (.sql.gz, .sql.zip)
   - Ahorra espacio (70-90% compresión)
   - Recomendado para DBs grandes

3. **MyDataBases Format** (.mdb) - Futuro
   - Formato propietario optimizado
   - Incluye metadatos extra
   - Más rápido de importar

### 4. Destino del Backup

**Opciones**:

1. **Local Storage** (Downloads)
   ```
   /Download/mydata bases-backups/
     ├── production_db_2026-06-11_15-30.sql.gz
     ├── development_db_2026-06-10_10-15.sql
     └── ...
   ```

2. **Compartir** (Share)
   - Google Drive
   - Dropbox
   - Email
   - Cualquier app vía Share Sheet

3. **Cloud Sync** (v1.2+)
   - Backup automático a cloud de Sphynxs
   - Versionado automático
   - Encriptado end-to-end

4. **Scheduled Backups** (v2.0+)
   - Programar backups diarios/semanales
   - Auto-upload a cloud
   - Notificaciones de éxito/fallo

### 5. Progreso y Cancelación

**Durante backup**:

```
┌─────────────────────────────────────┐
│ Creating Backup...                  │
├─────────────────────────────────────┤
│ Database: production_db             │
│ Tables: 15 / 25                     │
│ Current: orders (45,123 rows)       │
│                                     │
│ ████████░░░░░░░░░░░░ 60%            │
│                                     │
│ Elapsed: 00:02:15                   │
│ Estimated: 00:01:30 remaining       │
│                                     │
│ Written: 8.5 MB                     │
│                                     │
│ [Cancel]                            │
└─────────────────────────────────────┘
```

**Cancelación**:

- Permitir cancelar en cualquier momento
- Eliminar archivo parcial
- Notificar que el backup está incompleto

---

## Restore (Importar)

### 1. Fuentes de Restore

**Desde**:

1. **Archivo Local**
   - Seleccionar .sql, .sql.gz, .sql.zip
   - Usar SAF (Storage Access Framework)

2. **Cloud** (v1.2+)
   - Listar backups en cloud
   - Descargar y restaurar

3. **Clipboard** (para queries pequeñas)
   - Pegar SQL y ejecutar

### 2. Opciones de Restore

```
┌─────────────────────────────────────┐
│ Restore Options                     │
├─────────────────────────────────────┤
│ File: production_db_backup.sql.gz   │
│ Size: 2.3 MB (compressed)           │
│ Detected: MySQL 8.0                 │
│                                     │
│ Target Database: [Select...]        │
│  ● Create new: production_db_copy   │
│  ○ Existing: development_db         │
│                                     │
│ Options:                            │
│  ☑ Drop existing tables             │
│  ☑ Stop on error                    │
│  ○ Continue on error (log errors)   │
│  ☑ Execute in transaction           │
│                                     │
│ ⚠️ Warning: This will drop all      │
│    existing tables in target DB.    │
│                                     │
│ [Start Restore]  [Cancel]           │
└─────────────────────────────────────┘
```

### 3. Validación Pre-Restore

**Antes de restaurar, validar**:

- ✅ Formato del archivo válido
- ✅ Motor compatible (MySQL backup → MySQL DB)
- ✅ Versión compatible (advertir si hay diferencias)
- ✅ Espacio disponible en DB
- ✅ Permisos suficientes

**Mostrar warnings**:

```
┌─────────────────────────────────────┐
│ ⚠️ Compatibility Warnings           │
├─────────────────────────────────────┤
│ • Backup from MySQL 8.0             │
│   Restoring to MySQL 5.7            │
│   → Some features may not work      │
│                                     │
│ • Backup contains 5 triggers        │
│   → Current user may not have       │
│     TRIGGER privilege               │
│                                     │
│ [Continue Anyway]  [Cancel]         │
└─────────────────────────────────────┘
```

### 4. Progreso de Restore

```
┌─────────────────────────────────────┐
│ Restoring Database...               │
├─────────────────────────────────────┤
│ File: production_db_backup.sql.gz   │
│                                     │
│ Statements executed: 1,523 / 2,845  │
│                                     │
│ ████████████░░░░░░░░ 53%            │
│                                     │
│ Current: INSERT INTO orders...      │
│                                     │
│ Elapsed: 00:01:45                   │
│ Estimated: 00:01:30 remaining       │
│                                     │
│ [Cancel]                            │
└─────────────────────────────────────┘
```

### 5. Manejo de Errores

**Si hay errores durante restore**:

```
┌─────────────────────────────────────┐
│ ⚠️ Restore Completed with Errors    │
├─────────────────────────────────────┤
│ Success: 2,800 / 2,845 statements   │
│ Errors: 45 statements               │
│                                     │
│ Common errors:                      │
│ • Line 1523: Table 'old_table'      │
│   doesn't exist (15 times)          │
│ • Line 2341: Duplicate key error    │
│   (30 times)                        │
│                                     │
│ [View Error Log]  [Close]           │
└─────────────────────────────────────┘
```

**Error Log**:

```
Line 1523: Table 'old_table' doesn't exist
  Statement: INSERT INTO old_table VALUES (...)

Line 1524: Table 'old_table' doesn't exist
  Statement: INSERT INTO old_table VALUES (...)

Line 2341: Duplicate entry '123' for key 'PRIMARY'
  Statement: INSERT INTO users (id, name) VALUES (123, 'Test')
```

**Opciones**:

- ☑ **Stop on error**: Detener inmediatamente
- ☐ **Continue on error**: Loguear error y continuar
- ☑ **Execute in transaction**: Rollback completo si hay error

---

## Data Transfer (Entre Bases de Datos)

### Caso de Uso

**Copiar datos de una DB a otra sin archivos intermedios**:

```
MySQL Production → PostgreSQL Development
```

### Funcionalidad

```
┌─────────────────────────────────────┐
│ Data Transfer                       │
├─────────────────────────────────────┤
│ Source:                             │
│  Connection: MySQL Production       │
│  Database: shop_db                  │
│  Tables: [Select...]                │
│   ☑ users                           │
│   ☑ products                        │
│   ☑ orders                          │
│                                     │
│ Target:                             │
│  Connection: PostgreSQL Dev         │
│  Database: shop_dev                 │
│                                     │
│ Options:                            │
│  ○ Copy structure + data            │
│  ○ Copy structure only              │
│  ● Copy data only                   │
│  ☑ Drop target tables first         │
│  ☑ Map data types automatically     │
│                                     │
│ [Start Transfer]                    │
└─────────────────────────────────────┘
```

**Mapeo de tipos**:

- MySQL `INT` → PostgreSQL `INTEGER`
- MySQL `DATETIME` → PostgreSQL `TIMESTAMP`
- MySQL `TINYINT(1)` → PostgreSQL `BOOLEAN`
- etc.

---

## Requisitos Técnicos

### Generación de SQL Dump

```kotlin
interface BackupService {
    suspend fun createBackup(
        connection: Connection,
        options: BackupOptions
    ): Flow<BackupProgress>
}

data class BackupOptions(
    val type: BackupType,
    val tables: List<String>,
    val includeViews: Boolean,
    val includeProcedures: Boolean,
    val includeTriggers: Boolean,
    val dropTablesFirst: Boolean,
    val compression: CompressionType,
    val outputPath: String
)

enum class BackupType {
    FULL, STRUCTURE_ONLY, DATA_ONLY
}

enum class CompressionType {
    NONE, GZIP, ZIP
}

data class BackupProgress(
    val stage: BackupStage,
    val currentTable: String?,
    val tablesProcessed: Int,
    val totalTables: Int,
    val bytesWritten: Long,
    val elapsedTime: Duration
)
```

### Implementación por Motor

**MySQL**:

```kotlin
class MySQLBackupService : BackupService {
    override suspend fun createBackup(
        connection: Connection,
        options: BackupOptions
    ): Flow<BackupProgress> = flow {
        val writer = createWriter(options.outputPath, options.compression)
        
        // Header
        writer.writeLine("-- MyDataBases Backup")
        writer.writeLine("-- Database: ${connection.database}")
        writer.writeLine("-- Date: ${LocalDateTime.now()}")
        
        // Disable FK checks
        writer.writeLine("SET FOREIGN_KEY_CHECKS=0;")
        
        // Tablas
        for (table in options.tables) {
            emit(BackupProgress(stage = BackupStage.TABLE, currentTable = table, ...))
            
            // Structure
            if (options.type != BackupType.DATA_ONLY) {
                val createTable = getCreateTableStatement(table)
                writer.writeLine(createTable)
            }
            
            // Data
            if (options.type != BackupType.STRUCTURE_ONLY) {
                val rows = getTableData(table)
                val inserts = generateInsertStatements(table, rows)
                writer.writeLine(inserts)
            }
        }
        
        writer.writeLine("SET FOREIGN_KEY_CHECKS=1;")
        writer.close()
        
        emit(BackupProgress(stage = BackupStage.COMPLETED, ...))
    }
    
    private suspend fun getCreateTableStatement(table: String): String {
        val result = executeQuery("SHOW CREATE TABLE `$table`")
        return result.getString("Create Table")
    }
}
```

**PostgreSQL**:

```kotlin
class PostgreSQLBackupService : BackupService {
    // Similar pero con sintaxis PostgreSQL
    // pg_dump-like output
}
```

### Compresión

```kotlin
class CompressionWriter(
    private val outputPath: String,
    private val type: CompressionType
) {
    private val writer: BufferedWriter = when (type) {
        CompressionType.NONE -> File(outputPath).bufferedWriter()
        CompressionType.GZIP -> GZIPOutputStream(FileOutputStream(outputPath)).bufferedWriter()
        CompressionType.ZIP -> {
            val zip = ZipOutputStream(FileOutputStream(outputPath))
            zip.putNextEntry(ZipEntry("backup.sql"))
            zip.bufferedWriter()
        }
    }
    
    fun writeLine(line: String) {
        writer.write(line)
        writer.newLine()
    }
    
    fun close() {
        writer.close()
    }
}
```

### Restore (Parsing y Ejecución)

```kotlin
interface RestoreService {
    suspend fun restore(
        connection: Connection,
        backupFile: File,
        options: RestoreOptions
    ): Flow<RestoreProgress>
}

data class RestoreOptions(
    val targetDatabase: String,
    val dropExistingTables: Boolean,
    val stopOnError: Boolean,
    val useTransaction: Boolean
)

class MySQLRestoreService : RestoreService {
    override suspend fun restore(
        connection: Connection,
        backupFile: File,
        options: RestoreOptions
    ): Flow<RestoreProgress> = flow {
        val reader = createReader(backupFile)
        val errors = mutableListOf<RestoreError>()
        
        if (options.useTransaction) {
            connection.startTransaction()
        }
        
        var lineNumber = 0
        var statement = StringBuilder()
        
        reader.forEachLine { line ->
            lineNumber++
            
            // Ignorar comentarios
            if (line.trim().startsWith("--")) return@forEachLine
            
            statement.append(line).append("\n")
            
            // Detectar fin de statement (;)
            if (line.trim().endsWith(";")) {
                try {
                    connection.execute(statement.toString())
                    emit(RestoreProgress(statementsExecuted = lineNumber, ...))
                } catch (e: SQLException) {
                    val error = RestoreError(lineNumber, statement.toString(), e.message)
                    errors.add(error)
                    
                    if (options.stopOnError) {
                        if (options.useTransaction) connection.rollback()
                        throw RestoreException(errors)
                    }
                }
                
                statement.clear()
            }
        }
        
        if (options.useTransaction) {
            connection.commit()
        }
        
        emit(RestoreProgress(stage = RestoreStage.COMPLETED, errors = errors))
    }
}
```

---

## Testing

### Unit Tests

```kotlin
@Test
fun `backup generates valid SQL`() = runTest {
    val options = BackupOptions(
        type = BackupType.FULL,
        tables = listOf("users"),
        includeViews = true,
        compression = CompressionType.NONE,
        outputPath = "test_backup.sql"
    )
    
    backupService.createBackup(mockConnection, options).collect()
    
    val backup = File("test_backup.sql").readText()
    assertTrue(backup.contains("CREATE TABLE `users`"))
    assertTrue(backup.contains("INSERT INTO `users`"))
}
```

### Integration Tests

```kotlin
@Test
fun `backup and restore roundtrip preserves data`() = runTest {
    // 1. Crear backup
    val backupFile = "roundtrip_test.sql"
    backupService.createBackup(sourceConnection, options).collect()
    
    // 2. Restaurar a nueva DB
    val targetConnection = createConnection("test_restore_db")
    restoreService.restore(targetConnection, File(backupFile), restoreOptions).collect()
    
    // 3. Comparar datos
    val sourceData = sourceConnection.executeQuery("SELECT * FROM users")
    val targetData = targetConnection.executeQuery("SELECT * FROM users")
    assertEquals(sourceData, targetData)
}
```

---

## Strings Localizados

**Inglés** (`values/strings.xml`):

```xml
<!-- Backup & Restore -->
<string name="backup_title">Backup Database</string>
<string name="backup_type">Backup Type</string>
<string name="backup_full">Full Backup</string>
<string name="backup_structure">Structure Only</string>
<string name="backup_data">Data Only</string>
<string name="backup_compression">Compression</string>
<string name="backup_start">Start Backup</string>
<string name="backup_progress">Creating backup...</string>
<string name="backup_success">Backup created successfully</string>
<string name="restore_title">Restore Database</string>
<string name="restore_file">Select Backup File</string>
<string name="restore_start">Start Restore</string>
<string name="restore_progress">Restoring database...</string>
<string name="restore_success">Database restored successfully</string>
<string name="restore_errors">Restore completed with %1$d errors</string>
```

**Español** (`values-es/strings.xml`):

```xml
<!-- Backup & Restore -->
<string name="backup_title">Respaldar Base de Datos</string>
<string name="backup_type">Tipo de Respaldo</string>
<string name="backup_full">Respaldo Completo</string>
<string name="backup_structure">Solo Estructura</string>
<string name="backup_data">Solo Datos</string>
<string name="backup_compression">Compresión</string>
<string name="backup_start">Iniciar Respaldo</string>
<string name="backup_progress">Creando respaldo...</string>
<string name="backup_success">Respaldo creado exitosamente</string>
<string name="restore_title">Restaurar Base de Datos</string>
<string name="restore_file">Seleccionar Archivo de Respaldo</string>
<string name="restore_start">Iniciar Restauración</string>
<string name="restore_progress">Restaurando base de datos...</string>
<string name="restore_success">Base de datos restaurada exitosamente</string>
<string name="restore_errors">Restauración completada con %1$d errores</string>
```

---

## Roadmap

### v1.0

- ✅ Full backup (SQL dump)
- ✅ Structure only backup
- ✅ Data only backup
- ✅ GZIP compression
- ✅ Restore from .sql files
- ✅ Progress tracking
- ✅ Error handling

### v1.2

- ✅ Cloud backup storage
- ✅ Backup encryption
- ✅ Backup history/versions

### v2.0

- ✅ Scheduled backups
- ✅ Incremental backups
- ✅ Point-in-time recovery
- ✅ Cross-engine transfer (MySQL → PostgreSQL)
- ✅ Data diff/sync

---

**CRÍTICO**: Backup/Restore es funcionalidad esperada en TODO cliente SQL profesional. DEBE estar en v1.0.
