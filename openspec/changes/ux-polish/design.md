# Design: ux-polish — Foundation Premium UI

## Technical Approach

Implementar las **6 capabilities definidas en spec** como una **foundation visual progresiva**, estructurada en 3 PRs encadenados conceptualmente para mantener cada PR < 400 líneas y reviewable. La arquitectura se basa en **design tokens centralizados** (capability base) consumidos por **branded theme**, **adaptive scaffold**, **premium components**, **icon system** y **reduced motion**. El approach privilegia **composición sobre herencia** (primitivas reusables para skeletons), **derivación pura desde estado existente** (NavigationContext desde route activo, sin estado paralelo) y **lazy loading de recursos** (vectores XML, dynamic color solo cuando disponible).

La integración respeta la arquitectura existente: **Clean Architecture** (UI no accede data directamente), **Hilt DI** para repositorios, **Jetpack Compose puro** (cero XML), y **API 29+ sin fallback manual** (capacidades modernas se detectan vía `Build.VERSION.SDK_INT`).

## Architecture Decisions

### Decision: Tokens expuestos vía CompositionLocal, NO extension properties en MaterialTheme

**Choice**: Exponer `LocalAppSpacing`, `LocalAppShapes`, `LocalAppElevation`, `LocalAppMotion` como `CompositionLocal` **separados**, provistos por `AppTheme`, y consumibles como `LocalAppSpacing.current.lg`.

**Alternatives considered**:
- **Extension properties sobre `MaterialTheme`**: `MaterialTheme.appSpacing` usando `@Composable fun MaterialTheme.appSpacing` que lee el `CompositionLocal` internamente → más ergonómico pero NO funciona en contexto `@Composable` sin `MaterialTheme` envolvente (falla en tests unitarios que solo usan `CompositionLocalProvider`).
- **Object global inmutable**: `AppSpacing.lg` sin `CompositionLocal` → no permite swap dinámico (futuras customizaciones) y rompe el paradigma de Compose (todo vía composition).

**Rationale**: `CompositionLocal` es el mecanismo canónico de Compose para propagación de contexto (MaterialTheme usa internamente `LocalColorScheme`, `LocalTypography`, etc.). Separar cada token en su propio `CompositionLocal` permite swap independiente en el futuro (ej.: cambiar solo Spacing sin tocar Elevation) y testing unitario trivial (proveer solo el `Local` necesario).

**Consequences**:
- ✅ Testing unitario limpio: cada test provee solo los `Local` que consume.
- ✅ Futureproof: se puede agregar `AppSpacing.override(...)` sin romper API.
- ⚠️ Sintaxis más verbosa que extension property: `LocalAppSpacing.current.lg` vs hipotético `MaterialTheme.appSpacing.lg`. Aceptable porque la ganancia en testability compensa.

### Decision: Branded palette persiste en DataStore como Flow<Boolean>, NO como StateFlow en ViewModel

**Choice**: Crear `SettingsRepository` con método `observeBrandedPaletteEnabled(): Flow<Boolean>` que lee desde `DataStore<Preferences>`, y exponer desde `SettingsViewModel` como `StateFlow<Boolean>` (conversión del Flow vía `stateIn`).

**Alternatives considered**:
- **StateFlow directo en ViewModel sin Flow intermedio**: ViewModel sostiene `MutableStateFlow` y escribe a DataStore en `onChanged` → pierde la single source of truth (DataStore); durante rotación de pantalla el StateFlow podría no sincronizarse si el write falla.
- **SharedPreferences**: más simple pero **deprecated** y menos Compose-friendly (callback para observación, no Flow).
- **In-memory flag sin persistencia**: usuario pierde preferencia al cerrar app → UX inaceptable.

**Rationale**: DataStore es el estándar de persistencia en Android moderno (reemplazo de SharedPreferences) y expone **Flow nativo**, ideal para Compose. El ViewModel convierte el Flow a StateFlow vía `stateIn(viewModelScope)` para garantizar hot stream y recomposición eficiente. El repository sigue Clean Architecture (UI no accede DataStore directamente).

**Consequences**:
- ✅ Persistencia automática y robusta (DataStore maneja I/O async, corrupción de archivo, etc.).
- ✅ Observación reactiva: cambio en Settings screen → Flow emite → UI se recompone en < 16 ms (criterio NFR del spec).
- ⚠️ Requiere agregar `androidx.datastore:datastore-preferences` a Gradle; ya está presente en proyectos modernos, verificar en `build.gradle`.

### Decision: NavigationContext derivado puramente desde currentBackStackEntry, NO desde ViewModel de navegación

**Choice**: El `NavController.currentBackStackEntryAsState()` se mapea a `NavigationContext` mediante función pura `NavigationContext.from(route: String?)` que parsea la ruta activa y devuelve `OutsideConnection` o `InsideConnection(connectionId)`.

**Alternatives considered**:
- **Estado mutable en ViewModel**: `NavigationViewModel` sostiene `StateFlow<NavigationContext>` y lo actualiza vía observer del NavController → estado paralelo duplicado (route en NavController + context en ViewModel); riesgo de desincronización si la actualización llega tarde.
- **Derivar solo al entrar a pantalla**: cada Screen deriva su contexto localmente → duplica lógica en todos los screens.

**Rationale**: NavController ya ES la single source of truth del route activo. Derivar el contexto en tiempo de composición (`remember(currentBackStackEntry) { NavigationContext.from(route) }`) garantiza sincronización perfecta sin estado paralelo. El spec prohíbe explícitamente estado paralelo para evitar "flash" de destinos viejos (Req: Sin estado paralelo).

**Consequences**:
- ✅ Zero-state paralelo: imposible desincronizar.
- ✅ Testing trivial: `NavigationContext.from("connection/abc-123/tables")` → `InsideConnection("abc-123")` (pure function).
- ⚠️ Parsing ligero del route en cada recomposición cuando cambia `currentBackStackEntry`; costo O(1) trivial (regex o `startsWith("connection/")`).

### Decision: Skeletons compuestos desde primitivas (SkeletonBox/Text/Circle), NO monoliths por-screen

**Choice**: Exponer primitivas **públicas** (`SkeletonBox`, `SkeletonText`, `SkeletonCircle`) en `ui/components/skeleton/Primitives.kt`, y componer los skeletons por-pantalla (`ConnectionListSkeleton`, `DatabaseListSkeleton`, etc.) desde esas primitivas en archivos separados (`ui/components/skeleton/ConnectionListSkeleton.kt`).

**Alternatives considered**:
- **Monoliths privados**: cada skeleton es self-contained con su propio `Box` gris interno → duplica código (mismo color placeholder, mismo shape) en 4 archivos.
- **Skeleton único genérico parametrizado**: `GenericSkeleton(rows, columns, hasIcon)` → pierde fidelidad visual (el spec requiere que cada skeleton **reproduzca la silueta exacta** del contenido real).

**Rationale**: Primitivas reusables reducen superficie de cambio (cuando entre shimmer animation en el cambio de motion, se agrega solo en `SkeletonBox` y los 4 skeletons lo heredan gratis). Archivos separados por pantalla mantienen cohesión (cada skeleton vive junto a su use case) sin acoplamiento (primitivas son stateless).

**Consequences**:
- ✅ Futura animación shimmer se agrega en 1 lugar.
- ✅ Testing de primitivas independiente del layout de cada screen.
- ⚠️ 4 archivos adicionales (`ConnectionListSkeleton.kt`, etc.) vs 1 monolith; aceptable porque cada uno es < 50 líneas.

### Decision: Vectores XML custom en res/drawable/, NO compose ImageVector inline

**Choice**: Todos los íconos custom se definen como vectores **XML** en `res/drawable/ic_<dominio>_<nombre>.xml` y se exponen vía `AppIcons` que devuelve `Painter` desde `painterResource(R.drawable.*)`.

**Alternatives considered**:
- **Compose `ImageVector` inline**: definir cada ícono como `ImageVector.Builder().addPath(...)` en código Kotlin → permite iconos dinámicos pero ocupa más APK (Kotlin bytecode vs XML comprimido) y es menos readable (paths en strings vs declarativo XML).
- **SVG runtime con `coil-svg`**: descargar SVGs desde CDN → latencia, dependencia de red, no funciona offline.

**Rationale**: XML es el estándar Android para vectores, altamente optimizado (AAPT2 compila a binario), cacheable por sistema, tintable vía atributo `android:tint` o desde Compose vía `Icon(tint = ...)`, y compatible con Android Studio Vector Asset tool (import desde Figma/Illustrator).

**Consequences**:
- ✅ APK size impact mínimo: 15 vectores simples ≈ < 30 KB (spec NFR).
- ✅ Vector Asset tool permite import directo desde diseño.
- ✅ Tinte dinámico funciona out-of-the-box.
- ⚠️ Agregar nuevo ícono requiere crear XML + registrar en `AppIcons`; workflow manual pero simple (< 2 min).

### Decision: Reduced motion detectado en AppTheme con ContentObserver, NO en MainActivity

**Choice**: `AppTheme` internamente lee `Settings.Global.ANIMATOR_DURATION_SCALE` en `remember` y registra un `ContentObserver` que emite a un `State<Boolean>`, provisto vía `LocalReducedMotion`.

**Alternatives considered**:
- **MainActivity provee el CompositionLocal**: Activity lee el setting y lo pasa a `AppTheme` como parámetro → acopla MainActivity a lógica de tema; MainActivity debería ser thin.
- **Sin observación, solo lectura inicial**: no detecta cambios en runtime → rompe spec (Req: Observación de cambios en runtime).

**Rationale**: `AppTheme` ya es el provider de contexto visual (`ColorScheme`, `Typography`, tokens); agregar `LocalReducedMotion` es cohesivo. El `ContentObserver` vive en el lifecycle de `AppTheme` (registrado en `DisposableEffect`, desregistrado al salir) y mantiene MainActivity limpio. El spec normativo dice: **"el provisor es responsabilidad de AppTheme"** (línea 110 de `ui-reduced-motion/spec.md`).

**Consequences**:
- ✅ MainActivity queda thin (solo `setContent { AppTheme { ... } }`).
- ✅ Lifecycle correcto: observer se registra/desregistra con el Composable, no con la Activity (importante si AppTheme se usa en múltiples activities futuras).
- ⚠️ `ContentObserver` requiere `Context`; se obtiene vía `LocalContext.current` dentro de AppTheme.

## Data Flow

### Theme Selection Flow

```
Usuario cambia "Branded palette" en Settings
     │
     └─► SettingsViewModel.setBrandedPaletteEnabled(true)
              │
              └─► SettingsRepository.setBrandedPaletteEnabled(true)
                       │
                       └─► DataStore.edit { prefs[KEY] = true }
                                │
                                └─► Flow<Boolean> emite true
                                         │
                                         └─► StateFlow en ViewModel actualiza
                                                  │
                                                  └─► AppTheme observa StateFlow
                                                           │
                                                           ├─► Lógica de selección:
                                                           │   userPrefersBranded == true?
                                                           │   → BrandedDarkColorScheme
                                                           │   : supportsDynamic?
                                                           │     → dynamicDarkColorScheme(context)
                                                           │     : BrandedDarkColorScheme (fallback)
                                                           │
                                                           └─► MaterialTheme re-renderiza con nuevo ColorScheme
                                                                    │
                                                                    └─► Toda la UI se recompone en < 16 ms
```

### Navigation Context Derivation

```
Usuario tap en ConnectionCard("abc-123")
     │
     └─► NavController.navigate("connection/abc-123/tables")
              │
              └─► currentBackStackEntry cambia
                       │
                       └─► AdaptiveNavigationScaffold recompone
                                │
                                └─► NavigationContext.from(currentBackStackEntry.destination.route)
                                         │
                                         ├─► route.startsWith("connection/")?
                                         │   → Regex.find("connection/([^/]+)/")
                                         │   → InsideConnection(connectionId = "abc-123")
                                         │   : OutsideConnection
                                         │
                                         └─► destinations = when (context) {
                                                  InsideConnection → [Tables, Views, Editor, Functions, Backup]
                                                  OutsideConnection → [Connections, Settings]
                                             }
                                                  │
                                                  └─► NavigationBar/Rail/Drawer renderiza con destinations
```

### Reduced Motion Propagation

```
Sistema (Settings Global)
     │
     ├─► ANIMATOR_DURATION_SCALE = 0f
     │        │
     │        └─► ContentObserver.onChange()
     │                 │
     │                 └─► isReducedMotion.value = true
     │                          │
     │                          └─► LocalReducedMotion emite true
     │
     └─► Composable que anima
              │
              └─► val reduced = LocalReducedMotion.current
                       │
                       └─► val duration = AppMotion.durationOrInstant(AppMotion.medium, reduced)
                                │ (si reduced == true → devuelve 0)
                                │
                                └─► animateFloatAsState(durationMillis = 0)
                                         │
                                         └─► Animación instantánea
```

## File Changes

### Files to CREATE

| File | Description |
|------|-------------|
| `ui/theme/tokens/Spacing.kt` | Define `AppSpacing` data class con valores `xs` (4dp), `sm` (8dp), ..., `xxxl` (48dp). |
| `ui/theme/tokens/Shapes.kt` | Define `AppShapes` con `small` (8dp), `medium` (12dp), `large` (20dp), `extraLarge` (28dp). |
| `ui/theme/tokens/Elevation.kt` | Define `AppElevation` con `none` (0dp), `cardResting` (1dp), `cardHover` (3dp), `cardPressed` (6dp), `modal` (8dp). |
| `ui/theme/tokens/Motion.kt` | Define `AppMotion` con durations (`instant`, `fast`, `medium`, `slow`) y easings (`standard`, `decelerate`, `accelerate`, `emphasized`). Función `durationOrInstant(base, reduced)`. |
| `ui/theme/tokens/ReducedMotion.kt` | Define `LocalReducedMotion` CompositionLocal y función `rememberReducedMotion(context)` que lee Settings.Global + registra ContentObserver. |
| `ui/theme/BrandedColors.kt` | Define tokens `brand_bg`, `brand_surface`, `brand_primary`, etc. (Color vals), y `BrandedDarkColorScheme`, `BrandedLightColorScheme` (ColorScheme). |
| `ui/theme/AppTheme.kt` | Wrapper sobre `MaterialTheme` que: (1) provee `LocalAppSpacing/Shapes/Elevation/Motion/ReducedMotion`, (2) aplica lógica de selección branded/dynamic, (3) elimina `statusBarColor` set (migrado a MainActivity edge-to-edge), (4) sobrescribe `MaterialTheme.shapes` con `AppShapes`. |
| `data/repository/SettingsRepository.kt` | Repository con `observeBrandedPaletteEnabled(): Flow<Boolean>` y `setBrandedPaletteEnabled(enabled: Boolean)`. Usa DataStore. |
| `ui/viewmodel/SettingsViewModel.kt` | ViewModel que expone `brandedPaletteEnabled: StateFlow<Boolean>` y `setBrandedPaletteEnabled(enabled: Boolean)`. Hilt injectable. |
| `ui/adaptive/AdaptiveNavigationScaffold.kt` | Scaffold que switchea NavigationBar/Rail/Drawer según `windowSizeClass.widthSizeClass`. Recibe `navigationContext`, `currentRoute`, `destinations`, `onDestinationSelected`, `content`. |
| `ui/adaptive/NavigationContext.kt` | Sealed class: `OutsideConnection`, `InsideConnection(connectionId)`. Companion `from(route: String?): NavigationContext`. |
| `ui/adaptive/NavigationDestinations.kt` | Data class `NavigationDestination(id, labelRes, iconPainter, route)`. Functions `destinationsForContext(NavigationContext): List<NavigationDestination>`. |
| `ui/adaptive/AdaptiveSizing.kt` | Functions: `adaptivePadding(WindowSizeClass): PaddingValues`, `adaptiveGridColumns(WindowSizeClass): Int`, `adaptiveIconSize(WindowSizeClass): Dp`. |
| `ui/components/skeleton/Primitives.kt` | `SkeletonBox(width, height, shape, modifier)`, `SkeletonText(width, lines, modifier)`, `SkeletonCircle(size, modifier)`. |
| `ui/components/skeleton/ConnectionListSkeleton.kt` | Composable que renderiza 5 placeholders imitando `ConnectionCard`. |
| `ui/components/skeleton/DatabaseListSkeleton.kt` | Composable que renderiza 6 placeholders imitando `DatabaseCard`. |
| `ui/components/skeleton/TableListSkeleton.kt` | Composable que renderiza 8 placeholders imitando `TableCard`. |
| `ui/components/skeleton/TableViewerSkeleton.kt` | Composable que renderiza grid 10×4 imitando filas de tabla. |
| `ui/components/EmptyState.kt` | Composable: `EmptyState(icon, title, description?, action?)`. Centrado vertical con ícono 96dp. |
| `ui/components/AppIcons.kt` | Object `AppIcons` con nested objects `Nav`, `Db`, `State`, `Action`, `Editor`. Cada ícono es `val X: Painter @Composable get() = painterResource(R.drawable.ic_...)`. |
| `res/drawable/ic_nav_connections.xml` | Vector XML 24dp para destino Conexiones (ej: ícono de servidor). |
| `res/drawable/ic_nav_settings.xml` | Vector XML 24dp para Settings (ej: ícono de engranaje). |
| `res/drawable/ic_nav_tables.xml` | Vector XML 24dp para Tablas (ej: ícono de grid). |
| `res/drawable/ic_nav_views.xml` | Vector XML 24dp para Vistas (ej: ícono de eye). |
| `res/drawable/ic_nav_editor.xml` | Vector XML 24dp para Editor (ej: ícono de código). |
| `res/drawable/ic_nav_functions.xml` | Vector XML 24dp para Funciones (ej: ícono de función matemática). |
| `res/drawable/ic_nav_backup.xml` | Vector XML 24dp para Backup (ej: ícono de nube con flecha arriba). |
| `res/drawable/ic_db_mysql.xml` | Vector XML 24dp logo MySQL (estilizado). |
| `res/drawable/ic_db_postgres.xml` | Vector XML 24dp logo PostgreSQL (estilizado). |
| `res/drawable/ic_db_sqlite.xml` | Vector XML 24dp logo SQLite (estilizado). |
| `res/drawable/ic_db_mariadb.xml` | Vector XML 24dp logo MariaDB (estilizado). |
| `res/drawable/ic_db_sqlserver.xml` | Vector XML 24dp logo SQL Server (estilizado). |
| `res/drawable/ic_state_empty_connections.xml` | Vector XML 24dp ilustración "sin conexiones" (ej: servidor con X). |
| `res/drawable/ic_state_empty_tables.xml` | Vector XML 24dp ilustración "sin tablas" (ej: carpeta vacía). |
| `res/drawable/ic_state_error.xml` | Vector XML 24dp ilustración error (ej: triángulo con exclamación). |
| `res/values/strings.xml` | Strings nuevos: `nav_connections`, `nav_settings`, `nav_tables`, `nav_views`, `nav_editor`, `nav_functions`, `nav_backup`, `empty_connections_title`, `empty_connections_description`, `empty_tables_title`, `empty_tables_description`, `settings_branded_palette_title`, `settings_branded_palette_description`, `loading`, `error_retry`. |
| `res/values-es/strings.xml` | Traducciones españolas de los strings anteriores. |

### Files to MODIFY

| File | Action | Description |
|------|--------|-------------|
| `ui/theme/Theme.kt` | Refactor | ELIMINAR `window.statusBarColor = colorScheme.primary.toArgb()`. ELIMINAR la lógica de dynamic color manual (se mueve a `AppTheme`). Mantener wrapper `MyDataBasesTheme` como alias de `AppTheme` temporalmente para compatibilidad (deprecated en comentario, remover en cambio posterior). |
| `ui/theme/Color.kt` | Extend | AGREGAR tokens branded (`brand_bg`, etc.) al final del archivo. Los esquemas `DarkColorScheme` y `LightColorScheme` existentes quedan como estaban (Material default fallback si algo falla). |
| `ui/components/ConnectionCard.kt` | Refactor | CAMBIAR `Card(colors = CardDefaults.cardColors(containerColor = surfaceVariant))` → `Card(modifier = Modifier.shadow(AppElevation.cardResting, AppShapes.medium), shape = AppShapes.medium, colors = CardDefaults.cardColors())`. CAMBIAR padding hardcoded `16.dp` → `AppSpacing.lg`. CAMBIAR spacing hardcoded `4.dp`, `8.dp` → `AppSpacing.xxs`, `AppSpacing.sm`. REEMPLAZAR `Icons.Default.Edit/Delete` por `AppIcons.Action.Edit/Delete` (agregar esos vectores al set inicial si no existen; si no, diferir y crear issue). AGREGAR `Modifier.animateContentSize()` al contenedor `Column` interno (preparado para motion futuro). |
| `ui/components/DatabaseCard.kt` | Refactor | Aplicar mismo refactor que `ConnectionCard`: elevación, spacing tokens, AppIcons, animateContentSize. |
| `ui/components/TableCard.kt` | Refactor | Aplicar mismo refactor que `ConnectionCard`: elevación, spacing tokens, AppIcons, animateContentSize. |
| `ui/components/ErrorCard.kt` | Refactor | CAMBIAR layout de `Column` centrada a `Card` con background `errorContainer`. AGREGAR ícono `AppIcons.State.Error` (32dp) a la izquierda del texto. CAMBIAR `Button` a `OutlinedButton` con color `error`. HACER `onRetry` opcional (`onRetry: (() -> Unit)? = null`); si `null`, no renderizar botón. CAMBIAR spacing hardcoded a `AppSpacing.lg`. AGREGAR max lines en descripción (4 líneas con `TextOverflow.Ellipsis`). |
| `MainActivity.kt` | Modify | ASEGURAR que `enableEdgeToEdge()` está llamado en `onCreate` antes de `setContent`. CAMBIAR `setContent { MyDataBasesTheme { ... } }` → `setContent { AppTheme { ... } }` (si `MyDataBasesTheme` queda como alias, no cambia; si se depreca, actualizar). ELIMINAR cualquier código que setee `window.statusBarColor` manualmente. |
| `ui/navigation/AppNavHost.kt` | Modify | ENVOLVER el contenido actual del `NavHost` con `AdaptiveNavigationScaffold(windowSizeClass = ..., navigationContext = derivedContext, currentRoute = currentRoute, onDestinationSelected = { navController.navigate(it) }, content = { ... existing NavHost ... })`. DERIVAR `navigationContext` vía `remember(currentBackStackEntry) { NavigationContext.from(currentBackStackEntry?.destination?.route) }`. OBTENER `windowSizeClass` vía `calculateWindowSizeClass(activity = LocalContext.current as Activity)`. |
| `ui/screens/ConnectionListScreen.kt` | Modify | REEMPLAZAR `CircularProgressIndicator` en estado `Loading` por `ConnectionListSkeleton()`. REEMPLAZAR lógica de "lista vacía" (if `connections.isEmpty()`) por `EmptyState(icon = AppIcons.State.EmptyConnections, title = stringResource(R.string.empty_connections_title), description = stringResource(R.string.empty_connections_description), action = { Button(...) })`. |
| `ui/screens/DatabaseListScreen.kt` | Modify | Aplicar mismo cambio que `ConnectionListScreen`: `DatabaseListSkeleton` para loading, `EmptyState` para vacío. |
| `ui/screens/TableListScreen.kt` | Modify | Aplicar mismo cambio: `TableListSkeleton`, `EmptyState`. |
| `ui/screens/TableViewerScreen.kt` | Modify | Aplicar mismo cambio: `TableViewerSkeleton` para loading (no hay estado vacío en viewer, solo error o data). |
| `app/build.gradle.kts` | Modify | VERIFICAR que `androidx.datastore:datastore-preferences:1.0.0` está declarado. VERIFICAR que `androidx.compose.material3:material3-window-size-class:1.2.0` está declarado. Si no, agregar. |

### Files to DELETE

Ninguno. Este cambio extiende la base existente sin romper API pública.

## Interfaces / Contracts

### AppSpacing

```kotlin
@Immutable
data class AppSpacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
)

val LocalAppSpacing: ProvidableCompositionLocal<AppSpacing> =
    staticCompositionLocalOf { AppSpacing() }
```

### AppMotion

```kotlin
@Immutable
data class AppMotion(
    val instant: Int = 0,
    val fast: Int = 150,
    val medium: Int = 300,
    val slow: Int = 500,
    val standard: Easing = FastOutSlowInEasing,
    val decelerate: Easing = LinearOutSlowInEasing,
    val accelerate: Easing = FastOutLinearInEasing,
    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
) {
    fun durationOrInstant(base: Int, reduced: Boolean): Int =
        if (reduced) instant else base
}

val LocalAppMotion: ProvidableCompositionLocal<AppMotion> =
    staticCompositionLocalOf { AppMotion() }
```

### NavigationContext

```kotlin
sealed class NavigationContext {
    object OutsideConnection : NavigationContext()
    data class InsideConnection(val connectionId: String) : NavigationContext()

    companion object {
        private val connectionRouteRegex = Regex("connection/([^/]+)/.*")

        fun from(route: String?): NavigationContext {
            if (route == null) return OutsideConnection
            val match = connectionRouteRegex.find(route)
            return if (match != null) {
                InsideConnection(connectionId = match.groupValues[1])
            } else {
                OutsideConnection
            }
        }
    }
}
```

### NavigationDestination

```kotlin
@Immutable
data class NavigationDestination(
    val id: String,
    @StringRes val labelRes: Int,
    val icon: @Composable () -> Painter,
    val route: (connectionId: String?) -> String,
)

fun destinationsForContext(context: NavigationContext): List<NavigationDestination> =
    when (context) {
        is NavigationContext.OutsideConnection -> listOf(
            NavigationDestination(
                id = "connections",
                labelRes = R.string.nav_connections,
                icon = { AppIcons.Nav.Connections },
                route = { "connections" },
            ),
            NavigationDestination(
                id = "settings",
                labelRes = R.string.nav_settings,
                icon = { AppIcons.Nav.Settings },
                route = { "settings" },
            ),
        )
        is NavigationContext.InsideConnection -> listOf(
            NavigationDestination(
                id = "tables",
                labelRes = R.string.nav_tables,
                icon = { AppIcons.Nav.Tables },
                route = { "connection/${context.connectionId}/tables" },
            ),
            NavigationDestination(
                id = "views",
                labelRes = R.string.nav_views,
                icon = { AppIcons.Nav.Views },
                route = { "connection/${context.connectionId}/views" },
            ),
            NavigationDestination(
                id = "editor",
                labelRes = R.string.nav_editor,
                icon = { AppIcons.Nav.Editor },
                route = { "connection/${context.connectionId}/editor" },
            ),
            NavigationDestination(
                id = "functions",
                labelRes = R.string.nav_functions,
                icon = { AppIcons.Nav.Functions },
                route = { "connection/${context.connectionId}/functions" },
            ),
            NavigationDestination(
                id = "backup",
                labelRes = R.string.nav_backup,
                icon = { AppIcons.Nav.Backup },
                route = { "connection/${context.connectionId}/backup" },
            ),
        )
    }
```

### SettingsRepository

```kotlin
interface SettingsRepository {
    fun observeBrandedPaletteEnabled(): Flow<Boolean>
    suspend fun setBrandedPaletteEnabled(enabled: Boolean)
}

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    companion object {
        private val BRANDED_PALETTE_KEY = booleanPreferencesKey("branded_palette_enabled")
    }

    override fun observeBrandedPaletteEnabled(): Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[BRANDED_PALETTE_KEY] ?: false }

    override suspend fun setBrandedPaletteEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[BRANDED_PALETTE_KEY] = enabled }
    }
}
```

### AppTheme Integration

```kotlin
@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemInDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemInDarkTheme
    }

    // Observe branded palette preference
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val userPrefersBranded by settingsViewModel.brandedPaletteEnabled.collectAsState()

    // Reduced motion detection
    val isReducedMotion = rememberReducedMotion(context)

    // Color scheme selection
    val colorScheme = when {
        userPrefersBranded -> if (darkTheme) BrandedDarkColorScheme else BrandedLightColorScheme
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> if (darkTheme) BrandedDarkColorScheme else BrandedLightColorScheme
    }

    // Edge-to-edge setup (status bar appearance)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalAppSpacing provides AppSpacing(),
        LocalAppShapes provides AppShapes(),
        LocalAppElevation provides AppElevation(),
        LocalAppMotion provides AppMotion(),
        LocalReducedMotion provides isReducedMotion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes(
                small = LocalAppShapes.current.small,
                medium = LocalAppShapes.current.medium,
                large = LocalAppShapes.current.large,
                extraLarge = LocalAppShapes.current.extraLarge,
            ),
            content = content,
        )
    }
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `NavigationContext.from(route)` parsea correctamente routes `OutsideConnection` e `InsideConnection` | Kotlin test con `assertEquals(InsideConnection("abc-123"), NavigationContext.from("connection/abc-123/tables"))`. Casos: route válido, route `null`, route inválido. |
| Unit | `AppMotion.durationOrInstant(base, reduced)` devuelve 0 cuando `reduced == true`, `base` cuando `false` | Kotlin test: `assertEquals(0, AppMotion().durationOrInstant(300, true))`. |
| Unit | `SettingsRepository.observeBrandedPaletteEnabled()` emite valores correctos desde DataStore | Kotlinx-coroutines-test con fake `DataStore<Preferences>`. Verificar emisión inicial `false`, cambio a `true` tras `setBrandedPaletteEnabled(true)`. |
| Unit | `destinationsForContext(OutsideConnection)` devuelve 2 destinos (Connections, Settings) | Kotlin test: `assertEquals(2, destinationsForContext(OutsideConnection).size)`. |
| Unit | `destinationsForContext(InsideConnection("id"))` devuelve 5 destinos con `connectionId` interpolado en route | Kotlin test: verificar que `route(null)` contiene `"connection/id/tables"`. |
| Integration | `AppTheme` provee todos los `CompositionLocal` correctamente | Compose UI test: crear `ComposeTestRule`, setear `setContent { AppTheme { TestComposable() } }`, verificar que `LocalAppSpacing.current.lg == 16.dp` dentro de `TestComposable`. |
| Integration | `AdaptiveNavigationScaffold` conmuta NavigationBar/Rail/Drawer según `WindowSizeClass` | Compose UI test parametrizado con `Compact`, `Medium`, `Expanded`. Verificar presencia de `NavigationBar`/`NavigationRail`/`PermanentNavigationDrawer` vía `onNodeWithTag` (agregar `testTag` al scaffold). |
| Integration | Cambio de `userPrefersBranded` en runtime re-renderiza `AppTheme` con `BrandedDarkColorScheme` | Compose UI test: iniciar con `userPrefersBranded = false`, verificar color `primary` de MaterialTheme, cambiar a `true` vía ViewModel, verificar que `primary` cambió a `brand_primary`. |
| Integration | `EmptyState` renderiza ícono, título, descripción y botón correctamente | Compose UI test: renderizar `EmptyState(icon, title, description, action = { Button(...) })`, verificar presencia de nodos con texto `title`, `description`, `Button`. |
| Integration | `ConnectionListSkeleton` renderiza 5 placeholders | Compose UI test: renderizar `ConnectionListSkeleton()`, contar cantidad de `SkeletonBox` renderizados (debería ser 5 × cantidad de boxes por card). Opcional: usar `testTag` en cada placeholder. |
| E2E | Navegación de `connections` a `connection/abc-123/tables` cambia destinos de 2 a 5 | Espresso/Compose UI test: iniciar en `ConnectionListScreen`, tap en una `ConnectionCard`, verificar que aparecen 5 destinos en NavigationBar/Rail con labels "Tablas", "Vistas", etc. |
| E2E | Rotación de dispositivo mantiene destino seleccionado y contexto | Espresso/Compose UI test: navegar a `connection/abc-123/editor`, rotar pantalla, verificar que sigue en `editor` y scaffold muestra los 5 destinos `InsideConnection`. |
| E2E | Settings toggle "Branded palette" cambia tema en < 16 ms | Espresso + systrace: activar branded, medir tiempo hasta que `AppTheme` re-compone. Criterio: < 16 ms (1 frame a 60 fps). |

## Migration / Rollout

### Migration Plan

**Phase 1: Foundation (PR 4a)**

- Crear archivos de tokens (`Spacing.kt`, `Shapes.kt`, `Elevation.kt`, `Motion.kt`, `ReducedMotion.kt`, `BrandedColors.kt`, `AppTheme.kt`).
- Crear `SettingsRepository` y `SettingsViewModel` con lógica de branded palette.
- Refactorizar `Theme.kt` para eliminar `statusBarColor` y delegar a `AppTheme`.
- Asegurar `MainActivity.enableEdgeToEdge()`.
- **Consumidores iniciales**: modificar **solo 1 screen de prueba** (`ConnectionListScreen`) para consumir `AppSpacing.lg` en 1 lugar, validando que el sistema funciona end-to-end.
- **Gate de merge**: Lint verde, app compila, screen de prueba muestra spacing correcto, toggle de branded palette en Settings (pantalla mínima solo con el toggle, sin otras settings) cambia tema visiblemente.

**Phase 2: Adaptive Scaffold (PR 4b)**

- Crear archivos de adaptive (`AdaptiveNavigationScaffold.kt`, `NavigationContext.kt`, `NavigationDestinations.kt`, `AdaptiveSizing.kt`).
- Modificar `AppNavHost.kt` para envolver con `AdaptiveNavigationScaffold`.
- **Crear los 15 vectores XML custom** (navegación + DB providers + estados) y `AppIcons.kt`.
- **Consumidores**: todos los screens existentes ahora están envueltos por scaffold, pero **no cambian su código interno todavía**.
- **Gate de merge**: App corre en emuladores Compact/Medium/Expanded, muestra BottomBar/Rail/Drawer respectivamente, navegación funciona, destinos cambian correctamente al entrar/salir de conexión.

**Phase 3: Premium Components (PR 4c)**

- Crear primitivas de skeleton (`Primitives.kt`) y los 4 skeletons por-pantalla.
- Crear `EmptyState.kt`.
- Refactorizar `ConnectionCard`, `DatabaseCard`, `TableCard`, `ErrorCard` con elevación, AppIcons, spacing tokens, `animateContentSize`.
- Modificar `ConnectionListScreen`, `DatabaseListScreen`, `TableListScreen`, `TableViewerScreen` para usar skeletons y `EmptyState`.
- **Gate de merge**: Todos los screens muestran skeletons durante carga, `EmptyState` cuando vacío, cards tienen elevación sutil, ningún screen referencia `Icons.Default.*` directamente, lint verde, tests existentes siguen pasando.

### Feature Flags

No se usan feature flags. El cambio es **opt-in por diseño**:

- **Branded palette**: default `false` (dynamic color cuando disponible), usuario lo activa manualmente en Settings.
- **Adaptive scaffold**: siempre activo (no rompe funcionalidad; solo cambia componente de navegación).
- **Premium components**: reemplazo directo de componentes básicos; no hay estado intermedio.

### Rollback Plan

- **PR 4a revertido**: `git revert <commit>` → vuelve `MyDataBasesTheme` original, screen de prueba vuelve a literales `16.dp`, toggle de branded palette desaparece. No rompe screens (solo 1 screen consumía tokens).
- **PR 4b revertido**: `git revert <commit>` → `AppNavHost` vuelve sin scaffold, navegación funciona single-pane como antes.
- **PR 4c revertido**: `git revert <commit>` → skeletons desaparecen (vuelve `CircularProgressIndicator`), cards vuelven sin elevación, íconos vuelven a `Icons.Default.*` temporalmente (rompe si PR 4b ya los removió; en ese caso revertir PR 4b también, o crear hotfix que restaura imports de `Icons.Default`).

### Data Migration

No aplica. `DataStore` crea el archivo `settings.preferences_pb` automáticamente al primer write; no requiere migración desde SharedPreferences porque el proyecto es nuevo y no tiene preferencias legacy.

## Open Questions

- [x] **¿Dónde viven los vectores XML de providers DB (MySQL, PostgreSQL, etc.)?** → `res/drawable/`. Nombrados `ic_db_mysql.xml`, etc.
- [x] **¿ContentObserver de reduced motion se registra en MainActivity o en AppTheme?** → **AppTheme** (spec normativo línea 110).
- [x] **¿Settings screen ya existe o se crea en este cambio?** → Se crea **mínimamente** en PR 4a (solo toggle de branded palette + navegación desde scaffold en PR 4b). Settings completo (theme mode, language, etc.) se difiere al cambio #6 del roadmap. Este cambio solo agrega lo mínimo para probar branded palette toggle.
- [x] **¿MyDataBasesTheme se depreca o se mantiene como alias?** → **Mantener como alias temporalmente** (`typealias MyDataBasesTheme = AppTheme`? No, Compose no soporta typealias de Composables; en su lugar, `MyDataBasesTheme` queda como wrapper que llama `AppTheme` con comentario `@Deprecated("Use AppTheme directly", ReplaceWith("AppTheme"))`). Remover en cambio posterior cuando todos los consumers estén migrados.
- [ ] **¿Vectores XML se diseñan from scratch o se importan desde biblioteca (ej: Material Symbols)?** → **From scratch** si hay diseñador disponible (identidad única); **Material Symbols Extended** como fallback temporal (importar vía Vector Asset tool, renombrar según convención). **Decisión pendiente de diseñador**; bloquea PR 4b pero no PR 4a.
- [ ] **¿`calculateWindowSizeClass` requiere dependencia adicional o ya está disponible?** → Requiere `androidx.compose.material3:material3-window-size-class:1.2.0` (verificar en `build.gradle`; si no está, agregar en PR 4b).

## Next Steps

1. **Implementar PR 4a (Foundation)**: crear tokens, `AppTheme`, `SettingsRepository`, refactorizar `Theme.kt`, agregar toggle de branded palette en Settings screen mínimo, consumir tokens en 1 screen de prueba.
2. **Implementar PR 4b (Adaptive Scaffold)**: crear scaffold + context + helpers, envolver `AppNavHost`, crear los 15 vectores XML (o importar temporalmente desde Material Symbols con naming correcto), crear `AppIcons`.
3. **Implementar PR 4c (Premium Components)**: crear skeletons, `EmptyState`, refactorizar cards, migrar screens a skeletons/EmptyState.
4. **Ejecutar tests de integración** tras cada PR: verificar que la app compila, corre en Compact/Medium/Expanded, y no hay regresión funcional.
5. **Lanzar sdd-tasks** para descomponer estos 3 PRs en tareas implementables.
