# Estándares de Localización (Multilenguaje)

La aplicación DEBE soportar múltiples idiomas desde la v1.0.

## Idiomas Soportados

### v1.0 (Lanzamiento Inicial)

- **Español** (es)
- **Inglés** (en) — Idioma por defecto

### Futuro (según demanda)

- **Portugués** (pt-BR) — Brasil
- **Francés** (fr)
- **Alemán** (de)
- **Chino Simplificado** (zh-CN)
- **Japonés** (ja)
- **Coreano** (ko)

## Estructura de Recursos

### Organización de Strings

```
res/
├── values/                    # Inglés (por defecto)
│   └── strings.xml
├── values-es/                 # Español
│   └── strings.xml
├── values-pt-rBR/             # Portugués (Brasil) - futuro
│   └── strings.xml
└── values-night/              # Dark theme (sin strings aquí)
    └── colors.xml
```

### Archivo `strings.xml`

**Estructura organizada**:

```xml
<resources>
    <!-- App Name -->
    <string name="app_name">MyDataBases</string>
    
    <!-- Authentication Screen -->
    <string name="auth_title">Welcome to MyDataBases</string>
    <string name="auth_subtitle">Professional Database Client for Android</string>
    <string name="auth_google">Sign in with Google</string>
    <string name="auth_apple">Sign in with Apple</string>
    <string name="auth_email">Sign in with Email</string>
    
    <!-- Home Screen -->
    <string name="home_title">Database Providers</string>
    <string name="home_mysql">MySQL</string>
    <string name="home_postgresql">PostgreSQL</string>
    
    <!-- Connection Screen -->
    <string name="connection_title">Connections</string>
    <string name="connection_add">Add Connection</string>
    <string name="connection_edit">Edit Connection</string>
    <string name="connection_delete">Delete Connection</string>
    <string name="connection_test">Test Connection</string>
    
    <!-- Connection Form -->
    <string name="form_connection_name">Connection Name</string>
    <string name="form_host">Host</string>
    <string name="form_port">Port</string>
    <string name="form_database">Database</string>
    <string name="form_username">Username</string>
    <string name="form_password">Password</string>
    <string name="form_ssl_enabled">Enable SSL</string>
    
    <!-- Common Actions -->
    <string name="action_save">Save</string>
    <string name="action_cancel">Cancel</string>
    <string name="action_delete">Delete</string>
    <string name="action_edit">Edit</string>
    <string name="action_close">Close</string>
    <string name="action_search">Search</string>
    <string name="action_filter">Filter</string>
    <string name="action_export">Export</string>
    
    <!-- Errors -->
    <string name="error_connection_failed">Connection failed. Please check your credentials.</string>
    <string name="error_network">No internet connection</string>
    <string name="error_generic">Something went wrong. Please try again.</string>
    
    <!-- Success Messages -->
    <string name="success_connection">Connected successfully</string>
    <string name="success_saved">Saved successfully</string>
    <string name="success_deleted">Deleted successfully</string>
</resources>
```

### Versión en Español (`values-es/strings.xml`)

```xml
<resources>
    <!-- App Name -->
    <string name="app_name">MyDataBases</string>
    
    <!-- Authentication Screen -->
    <string name="auth_title">Bienvenido a MyDataBases</string>
    <string name="auth_subtitle">Cliente Profesional de Bases de Datos para Android</string>
    <string name="auth_google">Iniciar sesión con Google</string>
    <string name="auth_apple">Iniciar sesión con Apple</string>
    <string name="auth_email">Iniciar sesión con Email</string>
    
    <!-- Home Screen -->
    <string name="home_title">Proveedores de Bases de Datos</string>
    <string name="home_mysql">MySQL</string>
    <string name="home_postgresql">PostgreSQL</string>
    
    <!-- Connection Screen -->
    <string name="connection_title">Conexiones</string>
    <string name="connection_add">Agregar Conexión</string>
    <string name="connection_edit">Editar Conexión</string>
    <string name="connection_delete">Eliminar Conexión</string>
    <string name="connection_test">Probar Conexión</string>
    
    <!-- Connection Form -->
    <string name="form_connection_name">Nombre de Conexión</string>
    <string name="form_host">Host</string>
    <string name="form_port">Puerto</string>
    <string name="form_database">Base de Datos</string>
    <string name="form_username">Usuario</string>
    <string name="form_password">Contraseña</string>
    <string name="form_ssl_enabled">Habilitar SSL</string>
    
    <!-- Common Actions -->
    <string name="action_save">Guardar</string>
    <string name="action_cancel">Cancelar</string>
    <string name="action_delete">Eliminar</string>
    <string name="action_edit">Editar</string>
    <string name="action_close">Cerrar</string>
    <string name="action_search">Buscar</string>
    <string name="action_filter">Filtrar</string>
    <string name="action_export">Exportar</string>
    
    <!-- Errors -->
    <string name="error_connection_failed">Conexión fallida. Por favor verifica tus credenciales.</string>
    <string name="error_network">Sin conexión a internet</string>
    <string name="error_generic">Algo salió mal. Por favor intenta nuevamente.</string>
    
    <!-- Success Messages -->
    <string name="success_connection">Conectado exitosamente</string>
    <string name="success_saved">Guardado exitosamente</string>
    <string name="success_deleted">Eliminado exitosamente</string>
</resources>
```

## Reglas de Localización

### 1. NUNCA Hardcodear Strings

**Mal**:

```kotlin
@Composable
fun LoginButton() {
    Button(onClick = { /* ... */ }) {
        Text("Login")  // ❌ Hardcoded
    }
}
```

**Bien**:

```kotlin
@Composable
fun LoginButton() {
    Button(onClick = { /* ... */ }) {
        Text(stringResource(R.string.auth_login))  // ✅ Localizado
    }
}
```

### 2. Usar String Resources en Compose

```kotlin
import androidx.compose.ui.res.stringResource

@Composable
fun GreetingScreen() {
    Text(text = stringResource(R.string.welcome_message))
}
```

### 3. Strings con Parámetros

**strings.xml**:

```xml
<string name="connection_status">Connected to %1$s as %2$s</string>
```

**Compose**:

```kotlin
Text(
    text = stringResource(
        R.string.connection_status,
        "localhost",
        "admin"
    )
)
```

### 4. Plurales

**strings.xml**:

```xml
<plurals name="tables_count">
    <item quantity="one">%d table</item>
    <item quantity="other">%d tables</item>
</plurals>
```

**Español (`values-es/strings.xml`)**:

```xml
<plurals name="tables_count">
    <item quantity="one">%d tabla</item>
    <item quantity="other">%d tablas</item>
</plurals>
```

**Compose**:

```kotlin
import androidx.compose.ui.res.pluralStringResource

Text(
    text = pluralStringResource(R.plurals.tables_count, count, count)
)
```

### 5. Content Descriptions (Accesibilidad)

**Siempre localizar**:

```kotlin
Icon(
    Icons.Default.Delete,
    contentDescription = stringResource(R.string.action_delete)
)
```

### 6. Fechas y Números

**Fechas**:

```kotlin
import java.text.DateFormat
import java.util.Locale

val dateFormat = DateFormat.getDateInstance(
    DateFormat.MEDIUM,
    Locale.getDefault()
)
val formattedDate = dateFormat.format(Date())
```

**Números**:

```kotlin
import java.text.NumberFormat

val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
val formattedNumber = numberFormat.format(12345.67)
```

## Selección de Idioma

### Detección Automática

La app usa el idioma del sistema por defecto.

### Cambio Manual (Feature)

Permitir al usuario elegir idioma manualmente:

```kotlin
// Settings ViewModel
fun changeLanguage(languageCode: String) {
    dataStore.updateLanguage(languageCode)
    // Reiniciar Activity para aplicar cambios
}
```

**Idiomas disponibles**:

```kotlin
enum class Language(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    PORTUGUESE("pt-BR", "Português"),
    // ...
}
```

## Testing de Localización

### 1. Pseudolocalización

Habilitar en Developer Options:

- Settings → System → Developer Options → Pseudolocales

Detecta:

- Strings hardcodeados
- Layouts que no se adaptan
- Textos cortados

### 2. Tests Automatizados

```kotlin
@Test
fun allStringsHaveTranslations() {
    val englishStrings = getStrings(Locale.ENGLISH)
    val spanishStrings = getStrings(Locale("es"))
    
    assertEquals(
        "Missing Spanish translations",
        englishStrings.keys,
        spanishStrings.keys
    )
}
```

### 3. Revisión Manual

- Testear app en cada idioma soportado
- Verificar que textos no se corten
- Verificar traducciones contextuales correctas

## Traducción de Play Store

### Títulos y Descripciones

**Inglés**:

- Title: `MyDataBases`
- Short: `Professional database client for MySQL, PostgreSQL, SQLite`
- Long: [4000 chars detallando features]

**Español**:

- Title: `MyDataBases`
- Short: `Cliente profesional de bases de datos MySQL, PostgreSQL, SQLite`
- Long: [4000 chars en español]

### Screenshots

- Crear screenshots en cada idioma
- Mostrar UI en el idioma correspondiente
- Usar textos de ejemplo en el idioma correcto

## Herramientas Recomendadas

- **Android Studio String Editor**: Ver todas las traducciones en una tabla
- **Crowdin / Lokalise**: Plataformas de traducción colaborativa
- **Google Translate**: Solo para placeholder, NUNCA para producción
- **Traductores profesionales**: Para idiomas críticos

## Checklist de Localización

Antes de cada release:

- [ ] Todos los strings están en `strings.xml`
- [ ] No hay strings hardcodeados en código
- [ ] Español completamente traducido
- [ ] Plurales definidos correctamente
- [ ] Content descriptions localizadas
- [ ] Fechas y números usan formato local
- [ ] App testeada en español e inglés
- [ ] Screenshots de Play Store en ambos idiomas
- [ ] Descripciones de Play Store traducidas

---

**Recordá**: La localización NO es traducir al final. Es diseñar pensando en múltiples idiomas desde el principio.
