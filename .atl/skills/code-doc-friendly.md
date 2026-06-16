# Skill: Documentación de Código Amigable

## Cuándo Usar

Carga este skill cuando:

- Crees funciones, métodos, clases o interfaces nuevas
- Documentes código existente sin documentación
- Agregues parámetros o cambies firmas de métodos
- El usuario pida documentar código

## Reglas de Documentación

| Regla | Requerimiento |
|-------|--------------|
| **Siempre documentar** | TODO método público, clase, interface DEBE tener KDoc |
| **Autor obligatorio** | `@author israel-icm` en TODAS las clases y métodos públicos |
| **Fecha obligatoria** | `@date YYYY-MM-DD` con la fecha actual |
| **Idioma** | Español neutro (sin regionalismos) |
| **Tono amigable** | Sin formalismos, explicá como le explicarías a un amigo |
| **Corto y claro** | Máximo 2-3 líneas de descripción |
| **Ejemplos opcionales** | Agregar `@sample` solo si ayuda a entender |

## Formato KDoc

### Para Clases

```kotlin
/**
 * Descripción corta y amigable de qué hace la clase.
 *
 * Opcionalmente un párrafo extra si necesita más contexto.
 *
 * @author israel-icm
 * @date 2026-06-11
 */
class MiClase {
    // ...
}
```

### Para Métodos/Funciones

```kotlin
/**
 * Descripción amigable de qué hace el método.
 *
 * @param nombreParam Qué es este parámetro (sin "El parámetro que...")
 * @param otroParam Para qué sirve
 * @return Qué devuelve (sin "Devuelve un...")
 * @throws ExcepcionTipo Cuándo puede tirar esta excepción
 * @author israel-icm
 * @date 2026-06-11
 */
suspend fun miMetodo(nombreParam: String, otroParam: Int): Result<Data> {
    // ...
}
```

### Para Propiedades

```kotlin
/**
 * Qué representa esta propiedad.
 *
 * @author israel-icm
 * @date 2026-06-11
 */
val miPropiedad: String = "valor"
```

## Ejemplos Buenos vs Malos

### ❌ MAL (Formal, obvio, redundante)

```kotlin
/**
 * Función que se encarga de conectar a la base de datos.
 *
 * @param config El objeto de configuración que contiene los parámetros de conexión.
 * @return Devuelve un objeto Result que contiene la conexión o un error.
 */
suspend fun connect(config: ConnectionConfig): Result<Connection>
```

### ✅ BIEN (Amigable, directo, útil)

```kotlin
/**
 * Conecta a la base de datos usando la configuración que le pasas.
 *
 * @param config Tiene el host, puerto, usuario y contraseña
 * @return La conexión lista para usar o un error si falló
 * @author israel-icm
 * @date 2026-06-11
 */
suspend fun connect(config: ConnectionConfig): Result<Connection>
```

### ❌ MAL (Sin contexto, muy técnico)

```kotlin
/**
 * Ejecuta query.
 *
 * @param query String
 * @return QueryResult
 */
suspend fun executeQuery(query: String): Result<QueryResult>
```

### ✅ BIEN (Con contexto, amigable)

```kotlin
/**
 * Ejecuta una query SQL y te devuelve los resultados.
 *
 * Usa prepared statements automáticamente para evitar inyección SQL.
 *
 * @param query La query SQL que quieres ejecutar (ej: "SELECT * FROM users")
 * @return Los datos que devolvió la DB o un error si algo salió mal
 * @author israel-icm
 * @date 2026-06-11
 */
suspend fun executeQuery(query: String): Result<QueryResult>
```

## Estilo de Escritura

### Usar lenguaje natural

- ✅ "Conecta a la base de datos"
- ❌ "Establece una conexión con el servidor de base de datos"

### Hablar en segunda persona (tú implícito, sin voseo)

- ✅ "Le pasas el ID del usuario"
- ✅ "Devuelve null si no encuentra nada"
- ❌ "Se debe pasar el identificador del usuario"
- ❌ "Retorna valor nulo en caso de ausencia"

### Evitar obviedades

- ✅ "Host de la base de datos"
- ❌ "El host es el servidor donde está la base de datos"

### Dar contexto útil

- ✅ "Usa esto cuando necesitas ejecutar múltiples queries en una transacción"
- ❌ "Método para transacciones"

### Mencionar gotchas importantes

- ✅ "Ojo: esta operación puede tardar si la tabla es grande"
- ✅ "Solo funciona con MySQL y PostgreSQL, SQLite no lo soporta"

## Ejemplos Completos

### ViewModel

```kotlin
/**
 * Maneja el estado de la pantalla de conexiones.
 *
 * Carga las conexiones guardadas, permite crear/editar/eliminar y testear conexiones.
 *
 * @property connectionRepository Repositorio para acceder a las conexiones
 * @author israel-icm
 * @date 2026-06-11
 */
class ConnectionsViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository
) : ViewModel() {
    
    /**
     * Estado actual de la UI con la lista de conexiones.
     *
     * @author israel-icm
     * @date 2026-06-11
     */
    private val _uiState = MutableStateFlow(ConnectionsUiState())
    val uiState: StateFlow<ConnectionsUiState> = _uiState.asStateFlow()
    
    /**
     * Carga todas las conexiones guardadas desde la base de datos local.
     *
     * Actualiza el estado de la UI con las conexiones o muestra un error si falla.
     *
     * @author israel-icm
     * @date 2026-06-11
     */
    fun loadConnections() {
        viewModelScope.launch {
            connectionRepository.getAllConnections()
                .onSuccess { connections ->
                    _uiState.update { it.copy(connections = connections, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
        }
    }
}
```

### Repository

```kotlin
/**
 * Repositorio para gestionar conexiones de base de datos.
 *
 * Guarda, carga y elimina conexiones desde Room. Las contraseñas se guardan
 * encriptadas con Android Keystore.
 *
 * @property connectionDao DAO de Room para acceder a la tabla de conexiones
 * @property encryptor Servicio para encriptar/desencriptar contraseñas
 * @author israel-icm
 * @date 2026-06-11
 */
class ConnectionRepository @Inject constructor(
    private val connectionDao: ConnectionDao,
    private val encryptor: CredentialEncryptor
) {
    
    /**
     * Obtiene todas las conexiones guardadas.
     *
     * Las contraseñas vienen encriptadas, se desencriptan al momento de usarlas.
     *
     * @return Lista de conexiones o error si falla
     * @author israel-icm
     * @date 2026-06-11
     */
    suspend fun getAllConnections(): Result<List<Connection>> = runCatching {
        connectionDao.getAll().map { entity ->
            entity.toDomain()
        }
    }
}
```

### Data Class

```kotlin
/**
 * Configuración de una conexión de base de datos.
 *
 * Tiene todos los datos necesarios para conectarse: host, puerto, credenciales, etc.
 *
 * @property id ID único de la conexión (auto-generado)
 * @property name Nombre que le pusiste a la conexión (ej: "Producción MySQL")
 * @property type Tipo de motor (MySQL, PostgreSQL, etc)
 * @property host Dirección del servidor (ej: "localhost", "192.168.1.100")
 * @property port Puerto del servidor (ej: 3306 para MySQL)
 * @property database Nombre de la base de datos
 * @property username Usuario para conectarse
 * @property password Contraseña (se guarda encriptada)
 * @property sshTunnel Config del túnel SSH (null si no usa SSH)
 * @author israel-icm
 * @date 2026-06-11
 */
data class Connection(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: DatabaseType,
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    val password: String,
    val sshTunnel: SSHTunnelConfig? = null
)
```

## Multilenguaje en Strings

Para los strings de la UI (NO en KDoc, que va en español neutro):

### Español España (`values-es-rES/strings.xml`)

```xml
<string name="connection_test_success">La conexión ha funcionado correctamente</string>
<string name="connection_test_failed">Vaya, la conexión ha fallado. Revisa los datos</string>
```

### Español Latinoamérica (`values-es/strings.xml` - neutro)

```xml
<string name="connection_test_success">La conexión funcionó bien</string>
<string name="connection_test_failed">Uy, la conexión falló. Revisa los datos</string>
```

### Inglés USA (`values/strings.xml`)

```xml
<string name="connection_test_success">Connection worked great!</string>
<string name="connection_test_failed">Oops, connection failed. Check your settings</string>
```

## Checklist

Antes de commitear código:

- [ ] Todas las clases públicas tienen KDoc
- [ ] Todos los métodos públicos tienen KDoc
- [ ] Todas las properties públicas tienen KDoc
- [ ] `@author israel-icm` presente en todo
- [ ] `@date YYYY-MM-DD` con fecha correcta
- [ ] Descripción en español neutro
- [ ] Tono amigable (sin formalismos)
- [ ] Sin obviedades
- [ ] Con contexto útil cuando es necesario

## Excepciones

**NO documentar**:

- Métodos privados internos obvios
- Getters/setters autogenerados
- Overrides que no cambian comportamiento
- Tests (salvo que tengan lógica compleja)

---

**Recuerda**: La documentación es para que otro dev (o tú dentro de 6 meses) entienda rápido qué hace el código. Escribe como le explicarías a un compañero tomando café.
