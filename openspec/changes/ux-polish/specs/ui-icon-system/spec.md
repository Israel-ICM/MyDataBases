# Especificación: ui-icon-system

## Propósito

Reemplazar el uso disperso de `Icons.Default.*` por un sistema de íconos custom basado en vectores XML, expuestos vía wrapper `AppIcons` que devuelve `Painter`, con naming convention consistente por dominio.

## Requirements

### Requirement: Naming convention de vectores XML

Todos los vectores MUST vivir en `res/drawable/` y seguir el patrón:

```
ic_<dominio>_<nombre>.xml
```

**Dominios permitidos**:

| Dominio       | Prefijo         | Ejemplos                                         |
|---------------|-----------------|--------------------------------------------------|
| Navegación    | `ic_nav_`       | `ic_nav_connections`, `ic_nav_settings`, `ic_nav_tables`, `ic_nav_views`, `ic_nav_editor`, `ic_nav_functions`, `ic_nav_backup` |
| Bases de datos| `ic_db_`        | `ic_db_mysql`, `ic_db_postgres`, `ic_db_sqlite`, `ic_db_mariadb`, `ic_db_sqlserver` |
| Estados       | `ic_state_`     | `ic_state_empty_connections`, `ic_state_empty_tables`, `ic_state_error`, `ic_state_success` |
| Acciones      | `ic_action_`    | `ic_action_add`, `ic_action_refresh`, `ic_action_delete`, `ic_action_search` |
| Editor        | `ic_editor_`    | `ic_editor_run`, `ic_editor_save`, `ic_editor_history`                       |

#### Scenario: Naming válido

- GIVEN un vector nuevo para "estado vacío de vistas"
- WHEN se nombra `ic_state_empty_views.xml`
- THEN cumple la convención (prefijo `ic_state_`)

#### Scenario: Naming inválido (rechazado en review)

- GIVEN un vector llamado `database_icon.xml`
- WHEN se intenta agregar al proyecto
- THEN review MUST rechazarlo por no seguir la convención

### Requirement: Tamaño canónico

Todos los vectores MUST exportarse con:

- `android:width="24dp"` y `android:height="24dp"` como tamaño canónico.
- `android:viewportWidth="24"` y `android:viewportHeight="24"`.
- Tinte aplicado vía `android:tint="?attr/colorOnSurface"` o dinámicamente desde Compose (`Icon(tint = ...)`), NUNCA hardcoded en el path.

El uso del ícono define el tamaño final vía `Modifier.size()`:

- 24 dp (estándar, dentro de `Icon` Material 3).
- 32 dp (medium, en cards).
- 48 dp (touch targets, FAB).
- 64 dp (grid de providers).
- 96 dp (`EmptyState`).

Los vectores MUST escalar correctamente a cualquier tamaño (vector path bien diseñado, no bitmap embebido).

#### Scenario: Ícono de provider escala bien

- GIVEN `ic_db_mysql.xml`
- WHEN se renderiza a 24 dp y a 96 dp
- THEN se ve nítido en ambos tamaños
- AND respeta el tinte solicitado

### Requirement: Wrapper AppIcons

El sistema MUST exponer un objeto `AppIcons` con sub-objetos por dominio:

```kotlin
object AppIcons {
    object Nav {
        val Connections: Painter @Composable get() = painterResource(R.drawable.ic_nav_connections)
        val Settings: Painter    @Composable get() = painterResource(R.drawable.ic_nav_settings)
        val Tables: Painter      @Composable get() = painterResource(R.drawable.ic_nav_tables)
        val Views: Painter       @Composable get() = painterResource(R.drawable.ic_nav_views)
        val Editor: Painter      @Composable get() = painterResource(R.drawable.ic_nav_editor)
        val Functions: Painter   @Composable get() = painterResource(R.drawable.ic_nav_functions)
        val Backup: Painter      @Composable get() = painterResource(R.drawable.ic_nav_backup)
    }
    object Db { val MySql; val Postgres; val Sqlite; val MariaDb; val SqlServer /* … */ }
    object State { val EmptyConnections; val EmptyTables; val Error; val Success /* … */ }
    object Action { val Add; val Refresh; val Delete; val Search /* … */ }
    object Editor { val Run; val Save; val History /* … */ }
}
```

#### Scenario: Consumo desde Composable

- GIVEN un Composable que necesita el ícono de MySQL
- WHEN escribe `Icon(painter = AppIcons.Db.MySql, contentDescription = "MySQL")`
- THEN renderiza el vector correcto

### Requirement: Prohibición de uso directo de Icons.Default

Ningún Composable de `ui/screens/` ni `ui/components/` (excepto `AppIcons.kt` mismo) MUST referenciar `androidx.compose.material.icons.Icons.Default.*` ni `Icons.Filled.*` ni `Icons.Outlined.*` directamente.

Si un Composable necesita un ícono que aún no existe como vector custom, MUST agregarse al set custom primero y exponerse vía `AppIcons`.

#### Scenario: Lint sobre import prohibido

- GIVEN un PR que importa `androidx.compose.material.icons.Icons` en un screen
- WHEN el reviewer revisa
- THEN MUST rechazar el cambio
- AND solicitar uso de `AppIcons`

### Requirement: Set mínimo viable inicial

El cambio `ux-polish` MUST agregar al menos los siguientes 15 vectores como set inicial:

**Navegación (7)**:
- `ic_nav_connections`, `ic_nav_settings`, `ic_nav_tables`, `ic_nav_views`, `ic_nav_editor`, `ic_nav_functions`, `ic_nav_backup`.

**Providers DB (5)**:
- `ic_db_mysql`, `ic_db_postgres`, `ic_db_sqlite`, `ic_db_mariadb`, `ic_db_sqlserver`.

**Estados (3)**:
- `ic_state_empty_connections`, `ic_state_empty_tables`, `ic_state_error`.

Iconos adicionales (`Action.*`, `Editor.*`, `State.Success`, etc.) se agregan en cambios siguientes bajo demanda.

#### Scenario: Set inicial completo

- GIVEN cambio `ux-polish` mergeado
- WHEN se inspecciona `res/drawable/`
- THEN existen al menos los 15 vectores listados
- AND `AppIcons` los expone

## Non-Functional Requirements

- **Performance**: `painterResource()` MUST cachearse por Compose; no re-cargar el vector en cada recomposición.
- **APK size impact**: 15 vectores XML simples ≈ < 30 KB total agregado.
- **Accessibility**: Cada `Icon(painter = AppIcons.X)` MUST recibir `contentDescription` (no `null`) cuando es informativo; `null` solo si es decorativo y va junto a texto que lo describe.

## Edge Cases

#### Scenario: Vector no existe (drawable faltante)

- GIVEN `AppIcons.Db.Oracle` referenciado pero `ic_db_oracle.xml` aún no agregado
- WHEN compila
- THEN compilación FALLA (resource not found)
- AND el reviewer detecta el faltante antes de merge

#### Scenario: Ícono de provider en card de conexión

- GIVEN `ConnectionCard` mostrando una conexión MySQL
- WHEN se renderiza
- THEN `AppIcons.Db.MySql` se muestra a 32 dp
- AND el tinte sigue el `primary` del tema actual (branded o dynamic)

#### Scenario: Tema cambia (branded ↔ dynamic)

- GIVEN ícono renderizado con tinte `primary`
- WHEN el tema cambia
- THEN el ícono se re-tinta automáticamente al nuevo `primary`

## Constraints

- Los vectores MUST ser `<vector>` puros (no `<animated-vector>` en esta capability).
- Los vectores MUST NO usar gradientes complejos en el set mínimo (mantener simples para performance y fácil tinte).
- `AppIcons` MUST vivir en `ui/components/AppIcons.kt`.
- Los `contentDescription` de íconos MUST estar localizados (`strings.xml` es + en).
