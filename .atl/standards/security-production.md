# Seguridad para Bases de Datos de Producción

## CRÍTICO

MyDataBases permite conectarse a bases de datos de producción. Un error de seguridad puede resultar en:

- 💀 Pérdida de datos
- 💀 Exposición de información sensible
- 💀 Compromiso de credenciales
- 💀 Inyección SQL
- 💀 Ejecución de comandos destructivos accidentales

**La seguridad NO es opcional.**

---

## 1. Protección de Credenciales

### Encriptación Obligatoria

**TODAS las credenciales DEBEN estar encriptadas**:

- ✅ Passwords de conexiones
- ✅ SSH passwords
- ✅ Private keys SSH
- ✅ Passphrases
- ✅ Tokens de API (v1.2+)

**Usar Android Keystore**:

```kotlin
class CredentialEncryptor @Inject constructor() {
    
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }
    
    fun encrypt(plainText: String): EncryptedData {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray())
        
        return EncryptedData(encrypted, iv)
    }
    
    fun decrypt(encryptedData: EncryptedData): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            IvParameterSpec(encryptedData.iv)
        )
        
        val decrypted = cipher.doFinal(encryptedData.data)
        return String(decrypted)
    }
    
    private fun getOrCreateKey(): SecretKey {
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            ?: generateKey()
    }
    
    private fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build()
        )
        
        return keyGenerator.generateKey()
    }
    
    companion object {
        private const val KEY_ALIAS = "mydatabases_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
```

### NUNCA en Texto Plano

**PROHIBIDO**:

```kotlin
// ❌ NUNCA
val password = "mypassword123"
sharedPrefs.edit().putString("db_password", password).apply()

// ❌ NUNCA
Log.d("DB", "Password: $password")

// ❌ NUNCA
val config = "host=localhost;user=admin;password=secret"
```

**CORRECTO**:

```kotlin
// ✅ SÍ
val encryptedPassword = credentialEncryptor.encrypt(password)
database.saveEncryptedPassword(encryptedPassword)

// ✅ SÍ (sin password)
Log.d("DB", "Connecting to ${connection.host}")

// ✅ SÍ
val config = ConnectionConfig(
    host = "localhost",
    user = "admin",
    encryptedPassword = encryptedPassword
)
```

---

## 2. Prevención de Inyección SQL

### Usar Prepared Statements SIEMPRE

**PROHIBIDO** (SQL injection vulnerable):

```kotlin
// ❌ NUNCA
val query = "SELECT * FROM users WHERE email = '$userInput'"
connection.executeQuery(query)
```

**CORRECTO**:

```kotlin
// ✅ SÍ
val query = "SELECT * FROM users WHERE email = ?"
connection.executeQuery(query, listOf(userInput))
```

### Validar Input de Usuario

```kotlin
fun sanitizeTableName(name: String): String {
    // Solo permitir caracteres alfanuméricos y underscore
    if (!name.matches(Regex("^[a-zA-Z0-9_]+$"))) {
        throw InvalidTableNameException("Invalid table name: $name")
    }
    return name
}
```

### Escapar Identificadores

```kotlin
// MySQL
fun escapeIdentifier(name: String): String = "`$name`"

// PostgreSQL
fun escapeIdentifier(name: String): String = "\"$name\""

// Uso
val tableName = escapeIdentifier(userProvidedTableName)
val query = "SELECT * FROM $tableName"
```

---

## 3. Modo Read-Only (Seguridad de Producción)

### Prevenir UPDATE/DELETE Accidentales

```kotlin
data class ConnectionConfig(
    val host: String,
    val database: String,
    val credentials: Credentials,
    val readOnly: Boolean = false  // ← Añadir este flag
)

class DatabaseEngine {
    suspend fun executeQuery(query: String): Result<QueryResult> {
        if (connection.readOnly && isDMLStatement(query)) {
            return Result.failure(
                ReadOnlyException("Connection is in read-only mode. DML statements not allowed.")
            )
        }
        
        return connection.execute(query)
    }
    
    private fun isDMLStatement(query: String): Boolean {
        val trimmed = query.trim().uppercase()
        return trimmed.startsWith("UPDATE") ||
               trimmed.startsWith("DELETE") ||
               trimmed.startsWith("INSERT") ||
               trimmed.startsWith("DROP") ||
               trimmed.startsWith("TRUNCATE") ||
               trimmed.startsWith("ALTER")
    }
}
```

**UI**:

```
┌─────────────────────────────────────┐
│ Connection: Production MySQL        │
├─────────────────────────────────────┤
│ ⚠️ Read-Only Mode: ON               │
│                                     │
│ DML statements (UPDATE, DELETE)     │
│ are disabled to protect data.       │
│                                     │
│ [Disable Read-Only] (requires auth) │
└─────────────────────────────────────┘
```

### Confirmación para Operaciones Destructivas

```kotlin
@Composable
fun ConfirmDestructiveQuery(
    query: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("⚠️ Destructive Operation") },
        text = {
            Column {
                Text("You are about to execute:")
                Text(
                    text = query,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "⚠️ This cannot be undone!",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Execute Anyway")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}
```

---

## 4. SSL/TLS para Conexiones

### Forzar SSL en Producción

```kotlin
data class SSLConfig(
    val enabled: Boolean,
    val requireServerCertificate: Boolean = true,
    val certificatePath: String? = null,
    val allowSelfSigned: Boolean = false
)

// MySQL
val jdbcUrl = buildString {
    append("jdbc:mysql://${config.host}:${config.port}/${config.database}")
    if (config.ssl.enabled) {
        append("?useSSL=true")
        append("&requireSSL=true")
        if (config.ssl.allowSelfSigned) {
            append("&verifyServerCertificate=false")  // Solo para dev
        }
    }
}
```

### Validar Certificados

**Producción**:
- ✅ `requireSSL=true`
- ✅ `verifyServerCertificate=true`
- ❌ NUNCA `allowSelfSigned=true`

**Desarrollo**:
- ⚠️ `allowSelfSigned=true` SOLO en dev

---

## 5. Autenticación Biométrica

### Para Conexiones de Producción

```kotlin
class BiometricAuthManager @Inject constructor(
    private val context: Context
) {
    fun authenticate(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            context as FragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            }
        )
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancel")
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
}

// Uso
biometricAuthManager.authenticate(
    title = "Unlock Production Database",
    subtitle = "Verify your identity",
    onSuccess = { connectToDatabase() },
    onError = { error -> showError(error) }
)
```

---

## 6. Audit Log

### Registrar Acciones Críticas

```kotlin
data class AuditLog(
    val timestamp: Instant,
    val userId: String,
    val connectionId: String,
    val action: AuditAction,
    val query: String?,
    val success: Boolean,
    val errorMessage: String?
)

enum class AuditAction {
    CONNECT,
    DISCONNECT,
    EXECUTE_QUERY,
    UPDATE_DATA,
    DELETE_DATA,
    DROP_TABLE,
    BACKUP_DATABASE,
    RESTORE_DATABASE
}

class AuditLogger @Inject constructor(
    private val database: Database
) {
    suspend fun log(
        connectionId: String,
        action: AuditAction,
        query: String? = null,
        success: Boolean,
        errorMessage: String? = null
    ) {
        val log = AuditLog(
            timestamp = Clock.System.now(),
            userId = getUserId(),
            connectionId = connectionId,
            action = action,
            query = query,
            success = success,
            errorMessage = errorMessage
        )
        
        database.insertAuditLog(log)
    }
}

// Uso
auditLogger.log(
    connectionId = connection.id,
    action = AuditAction.DELETE_DATA,
    query = "DELETE FROM users WHERE id = 123",
    success = true
)
```

---

## 7. Timeouts y Límites

### Prevenir Queries Infinitas

```kotlin
data class QueryLimits(
    val maxExecutionTime: Duration = 30.seconds,
    val maxResultRows: Int = 10_000,
    val maxMemory: Long = 100.megabytes
)

suspend fun executeQueryWithLimits(
    query: String,
    limits: QueryLimits
): Result<QueryResult> = withTimeout(limits.maxExecutionTime) {
    val result = connection.execute(query)
    
    if (result.rowCount > limits.maxResultRows) {
        return@withTimeout Result.failure(
            TooManyRowsException("Query returned ${result.rowCount} rows, limit is ${limits.maxResultRows}")
        )
    }
    
    Result.success(result)
}
```

---

## 8. Permisos de Usuario

### Detectar Permisos Insuficientes

```kotlin
suspend fun checkPermissions(connection: Connection): Permissions {
    val grants = connection.executeQuery("SHOW GRANTS")
    
    return Permissions(
        canSelect = grants.contains("SELECT"),
        canInsert = grants.contains("INSERT"),
        canUpdate = grants.contains("UPDATE"),
        canDelete = grants.contains("DELETE"),
        canDrop = grants.contains("DROP"),
        canCreateUser = grants.contains("CREATE USER")
    )
}

// UI
if (!permissions.canDelete) {
    Button(
        onClick = { /* ... */ },
        enabled = false
    ) {
        Text("Delete (No Permission)")
    }
}
```

---

## Checklist de Seguridad

Antes de publicar:

- [ ] Credenciales encriptadas con Android Keystore
- [ ] Prepared statements en TODAS las queries
- [ ] Modo read-only disponible
- [ ] Confirmación para UPDATE/DELETE/DROP
- [ ] SSL/TLS habilitado por defecto
- [ ] Validación de certificados SSL
- [ ] Biometric auth para conexiones producción
- [ ] Audit log de acciones críticas
- [ ] Timeouts en queries
- [ ] Detección de permisos de usuario
- [ ] No loguear passwords/keys NUNCA
- [ ] ProGuard/R8 habilitado
- [ ] Root detection (opcional)
- [ ] Sin hardcoded secrets en código

---

**CRÍTICO**: La seguridad es RESPONSABILIDAD del desarrollador. Un error puede comprometer datos de producción de los usuarios.
