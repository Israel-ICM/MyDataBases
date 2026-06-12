# Tasks: ux-polish — Foundation Premium UI

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | **PR 4a**: ~650 líneas, **PR 4b**: ~750 líneas, **PR 4c**: ~850 líneas (total: ~2,250 líneas) |
| 800-line budget risk | **Medium** — cada PR individualmente cabe en el presupuesto configurado de 800 líneas, pero 4c está ajustado |
| Chained PRs recommended | **No** — cada PR individual cabe en el presupuesto de 800 líneas; organización conceptual en 3 PRs es suficiente |
| Suggested split | **PR 4a** → **PR 4b** → **PR 4c** (todos a master, sin feature branches) |
| Delivery strategy | `auto-chain` (configurado por usuario) |
| Chain strategy | `stacked-to-main` (cada PR merge a master directamente) |

Decision needed before apply: **No**
Chained PRs recommended: **No**
Chain strategy: **stacked-to-main**
400-line budget risk: **Low** (presupuesto configurado es 800 líneas/PR)

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Foundation tokens + branded theme + edge-to-edge + reduced motion | PR 4a (~650 líneas) | Base para todo; merge a master; tests + docs incluidos |
| 2 | Adaptive scaffold + navigation context + iconos custom | PR 4b (~750 líneas) | Depende de PR 4a (usa tokens); merge a master después de 4a |
| 3 | Premium components (skeletons, cards, empty states) | PR 4c (~850 líneas) | Depende de PR 4a (usa tokens); puede ser paralelo a 4b o después |

---

## Fase 1: Foundation — Tokens + Branded Theme + Edge-to-Edge (PR 4a)

**Objetivo**: Crear los design tokens, el sistema de tema branded, y habilitar edge-to-edge correctamente. Esta fase es la base para todo lo demás.

**Estimación**: ~650 líneas (38 new files + 3 modified files)

---

### 1.1 Design Tokens — Spacing

- [ ] **1.1.1 [TDD RED]** Test: `AppSpacing` provee todos los valores definidos en spec
  - **Archivo**: `ui/theme/tokens/AppSpacingTest.kt` (nuevo)
  - **Acción**: Test unitario que verifica `AppSpacing().lg == 16.dp`, `.sm == 8.dp`, etc. (9 valores)
  - **Criterio**: Test falla (clase no existe todavía)
  - **Estimación**: Small (~15 líneas)

- [ ] **1.1.2 [TDD GREEN]** Implementar `AppSpacing` data class
  - **Archivo**: `ui/theme/tokens/Spacing.kt` (nuevo)
  - **Acción**: Crear `@Immutable data class AppSpacing(val none: Dp = 0.dp, val xxs: Dp = 2.dp, ..., val xxxl: Dp = 48.dp)`
  - **Criterio**: Test 1.1.1 pasa
  - **Estimación**: Trivial (~20 líneas)

- [ ] **1.1.3 [TDD GREEN]** Exponer `LocalAppSpacing` CompositionLocal
  - **Archivo**: `ui/theme/tokens/Spacing.kt` (modificar)
  - **Acción**: Crear `val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }`
  - **Criterio**: Se puede consumir como `LocalAppSpacing.current.lg` desde cualquier Composable
  - **Estimación**: Trivial (~5 líneas)

---

### 1.2 Design Tokens — Shapes

- [ ] **1.2.1 [TDD RED]** Test: `AppShapes` provee las 5 formas definidas en spec
  - **Archivo**: `ui/theme/tokens/AppShapesTest.kt` (nuevo)
  - **Acción**: Test unitario que verifica `AppShapes().medium == RoundedCornerShape(12.dp)`, etc.
  - **Criterio**: Test falla
  - **Estimación**: Small (~12 líneas)

- [ ] **1.2.2 [TDD GREEN]** Implementar `AppShapes` data class
  - **Archivo**: `ui/theme/tokens/Shapes.kt` (nuevo)
  - **Acción**: Crear `@Immutable data class AppShapes(val none: Shape = RoundedCornerShape(0.dp), ..., val extraLarge: Shape = RoundedCornerShape(28.dp))`
  - **Criterio**: Test 1.2.1 pasa
  - **Estimación**: Trivial (~18 líneas)

- [ ] **1.2.3 [TDD GREEN]** Exponer `LocalAppShapes` CompositionLocal
  - **Archivo**: `ui/theme/tokens/Shapes.kt` (modificar)
  - **Acción**: Crear `val LocalAppShapes = staticCompositionLocalOf { AppShapes() }`
  - **Criterio**: Consumible desde Composables
  - **Estimación**: Trivial (~5 líneas)

---

### 1.3 Design Tokens — Elevation

- [ ] **1.3.1 [TDD RED]** Test: `AppElevation` provee los 5 niveles definidos en spec
  - **Archivo**: `ui/theme/tokens/AppElevationTest.kt` (nuevo)
  - **Acción**: Test unitario que verifica `AppElevation().cardResting == 1.dp`, `.modal == 8.dp`, etc.
  - **Criterio**: Test falla
  - **Estimación**: Small (~10 líneas)

- [ ] **1.3.2 [TDD GREEN]** Implementar `AppElevation` data class
  - **Archivo**: `ui/theme/tokens/Elevation.kt` (nuevo)
  - **Acción**: Crear `@Immutable data class AppElevation(val none: Dp = 0.dp, val cardResting: Dp = 1.dp, ..., val modal: Dp = 8.dp)`
  - **Criterio**: Test 1.3.1 pasa
  - **Estimación**: Trivial (~12 líneas)

- [ ] **1.3.3 [TDD GREEN]** Exponer `LocalAppElevation` CompositionLocal
  - **Archivo**: `ui/theme/tokens/Elevation.kt` (modificar)
  - **Acción**: Crear `val LocalAppElevation = staticCompositionLocalOf { AppElevation() }`
  - **Criterio**: Consumible desde Composables
  - **Estimación**: Trivial (~5 líneas)

---

### 1.4 Design Tokens — Motion

- [ ] **1.4.1 [TDD RED]** Test: `AppMotion.durationOrInstant()` devuelve 0 cuando reduced=true
  - **Archivo**: `ui/theme/tokens/AppMotionTest.kt` (nuevo)
  - **Acción**: Test unitario que verifica `AppMotion().durationOrInstant(300, reduced = true) == 0`
  - **Criterio**: Test falla
  - **Estimación**: Small (~8 líneas)

- [ ] **1.4.2 [TDD GREEN]** Implementar `AppMotion` data class con durations y easings
  - **Archivo**: `ui/theme/tokens/Motion.kt` (nuevo)
  - **Acción**: Crear `@Immutable data class AppMotion(val instant: Int = 0, val fast: Int = 150, val medium: Int = 300, val slow: Int = 500, val standard: Easing = FastOutSlowInEasing, ..., fun durationOrInstant(base: Int, reduced: Boolean): Int = if (reduced) instant else base)`
  - **Criterio**: Test 1.4.1 pasa
  - **Estimación**: Small (~25 líneas)

- [ ] **1.4.3 [TDD GREEN]** Exponer `LocalAppMotion` CompositionLocal
  - **Archivo**: `ui/theme/tokens/Motion.kt` (modificar)
  - **Acción**: Crear `val LocalAppMotion = staticCompositionLocalOf { AppMotion() }`
  - **Criterio**: Consumible desde Composables
  - **Estimación**: Trivial (~5 líneas)

---

### 1.5 Reduced Motion Detection

- [ ] **1.5.1 [TDD RED]** Test: `rememberReducedMotion()` lee el setting correcto del sistema
  - **Archivo**: `ui/theme/tokens/ReducedMotionTest.kt` (nuevo)
  - **Acción**: Test de instrumentación que verifica lectura de `Settings.Global.ANIMATOR_DURATION_SCALE`
  - **Criterio**: Test falla
  - **Estimación**: Medium (~30 líneas, test de instrumentación con mock de ContentResolver)

- [ ] **1.5.2 [TDD GREEN]** Implementar `rememberReducedMotion(context)`
  - **Archivo**: `ui/theme/tokens/ReducedMotion.kt` (nuevo)
  - **Acción**: Crear función `@Composable rememberReducedMotion(context: Context): State<Boolean>` que lee `Settings.Global.ANIMATOR_DURATION_SCALE` y registra `ContentObserver` en `DisposableEffect`
  - **Criterio**: Test 1.5.1 pasa
  - **Estimación**: Medium (~45 líneas)

- [ ] **1.5.3 [TDD GREEN]** Exponer `LocalReducedMotion` CompositionLocal
  - **Archivo**: `ui/theme/tokens/ReducedMotion.kt` (modificar)
  - **Acción**: Crear `val LocalReducedMotion = staticCompositionLocalOf { mutableStateOf(false) }`
  - **Criterio**: Consumible desde Composables
  - **Estimación**: Trivial (~5 líneas)

---

### 1.6 Branded Color Tokens

- [ ] **1.6.1** Definir tokens de color branded en `Color.kt`
  - **Archivo**: `ui/theme/Color.kt` (modificar)
  - **Acción**: Agregar al final del archivo los 9 tokens: `val brand_bg = Color(0xFF1A1F2E)`, `val brand_surface = Color(0xFF222837)`, etc.
  - **Criterio**: Tokens compilables y referenciables
  - **Estimación**: Trivial (~15 líneas)

---

### 1.7 Branded ColorScheme

- [ ] **1.7.1 [TDD RED]** Test: `BrandedDarkColorScheme` mapea tokens correctamente
  - **Archivo**: `ui/theme/BrandedColorsTest.kt` (nuevo)
  - **Acción**: Test unitario que verifica `BrandedDarkColorScheme.primary == brand_primary`, `.background == brand_bg`, etc.
  - **Criterio**: Test falla
  - **Estimación**: Small (~20 líneas)

- [ ] **1.7.2 [TDD GREEN]** Implementar `BrandedDarkColorScheme`
  - **Archivo**: `ui/theme/BrandedColors.kt` (nuevo)
  - **Acción**: Crear `val BrandedDarkColorScheme = darkColorScheme(primary = brand_primary, background = brand_bg, ...)`
  - **Criterio**: Test 1.7.1 pasa; contraste WCAG AA verificado manualmente
  - **Estimación**: Small (~30 líneas)

- [ ] **1.7.3 [TDD GREEN]** Implementar `BrandedLightColorScheme`
  - **Archivo**: `ui/theme/BrandedColors.kt` (modificar)
  - **Acción**: Crear `val BrandedLightColorScheme = lightColorScheme(primary = brand_primary, background = Color(0xFFF5F6FA), onBackground = brand_bg, ...)`
  - **Criterio**: Contraste WCAG AA verificado; esquema funciona visualmente
  - **Estimación**: Small (~30 líneas)

---

### 1.8 Settings Repository (persistencia de branded palette)

- [ ] **1.8.1 [TDD RED]** Test: `SettingsRepository.observeBrandedPaletteEnabled()` emite valores correctos
  - **Archivo**: `data/repository/SettingsRepositoryTest.kt` (nuevo)
  - **Acción**: Test con fake `DataStore<Preferences>` que verifica emisión inicial `false`, cambio a `true` tras `setBrandedPaletteEnabled(true)`
  - **Criterio**: Test falla
  - **Estimación**: Medium (~40 líneas)

- [ ] **1.8.2 [TDD GREEN]** Implementar `SettingsRepository` interface
  - **Archivo**: `data/repository/SettingsRepository.kt` (nuevo)
  - **Acción**: Crear interface `SettingsRepository { fun observeBrandedPaletteEnabled(): Flow<Boolean>; suspend fun setBrandedPaletteEnabled(enabled: Boolean) }`
  - **Criterio**: Interface compila
  - **Estimación**: Trivial (~8 líneas)

- [ ] **1.8.3 [TDD GREEN]** Implementar `SettingsRepositoryImpl`
  - **Archivo**: `data/repository/SettingsRepository.kt` (modificar)
  - **Acción**: Crear `class SettingsRepositoryImpl @Inject constructor(private val dataStore: DataStore<Preferences>) : SettingsRepository { ... }` con lógica de lectura/escritura de DataStore
  - **Criterio**: Test 1.8.1 pasa
  - **Estimación**: Small (~25 líneas)

---

### 1.9 Settings ViewModel

- [ ] **1.9.1 [TDD RED]** Test: `SettingsViewModel.brandedPaletteEnabled` refleja el Flow del repository
  - **Archivo**: `ui/viewmodel/SettingsViewModelTest.kt` (nuevo)
  - **Acción**: Test con `TestCoroutineDispatcher` que verifica `StateFlow<Boolean>` se actualiza cuando el repository emite
  - **Criterio**: Test falla
  - **Estimación**: Medium (~35 líneas)

- [ ] **1.9.2 [TDD GREEN]** Implementar `SettingsViewModel`
  - **Archivo**: `ui/viewmodel/SettingsViewModel.kt` (nuevo)
  - **Acción**: Crear `@HiltViewModel class SettingsViewModel @Inject constructor(private val repo: SettingsRepository) : ViewModel() { val brandedPaletteEnabled = repo.observeBrandedPaletteEnabled().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false); fun setBrandedPaletteEnabled(enabled: Boolean) { viewModelScope.launch { repo.setBrandedPaletteEnabled(enabled) } } }`
  - **Criterio**: Test 1.9.1 pasa
  - **Estimación**: Small (~20 líneas)

---

### 1.10 AppTheme Wrapper

- [ ] **1.10.1 [TDD RED]** Test: `AppTheme` provee todos los CompositionLocal tokens
  - **Archivo**: `ui/theme/AppThemeTest.kt` (nuevo)
  - **Acción**: Compose UI test que verifica `setContent { AppTheme { TestComposable() } }` y dentro de `TestComposable` verifica `LocalAppSpacing.current.lg == 16.dp`, `LocalAppShapes.current.medium`, etc.
  - **Criterio**: Test falla
  - **Estimación**: Medium (~40 líneas)

- [ ] **1.10.2 [TDD GREEN]** Implementar `AppTheme` con lógica de selección branded/dynamic
  - **Archivo**: `ui/theme/AppTheme.kt` (nuevo)
  - **Acción**: Crear `@Composable fun AppTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit)` que: (1) lee `SettingsViewModel.brandedPaletteEnabled`, (2) selecciona ColorScheme según lógica del spec, (3) provee los 5 `CompositionLocal`, (4) envuelve `MaterialTheme`
  - **Criterio**: Test 1.10.1 pasa
  - **Estimación**: Large (~70 líneas)

- [ ] **1.10.3 [TDD GREEN]** Integrar `LocalReducedMotion` en `AppTheme`
  - **Archivo**: `ui/theme/AppTheme.kt` (modificar)
  - **Acción**: Llamar `rememberReducedMotion(LocalContext.current)` y proveer vía `LocalReducedMotion`
  - **Criterio**: `LocalReducedMotion.current` es consumible desde cualquier Composable hijo
  - **Estimación**: Trivial (~8 líneas)

---

### 1.11 Edge-to-Edge Fix

- [ ] **1.11.1** Eliminar `statusBarColor` set de `Theme.kt`
  - **Archivo**: `ui/theme/Theme.kt` (modificar)
  - **Acción**: ELIMINAR línea `window.statusBarColor = colorScheme.primary.toArgb()` y ELIMINAR la lógica de dynamic color manual (se mueve a `AppTheme`)
  - **Criterio**: Compilación verde; `Theme.kt` queda como alias limpio o deprecated
  - **Estimación**: Trivial (~10 líneas eliminadas)

- [ ] **1.11.2** Aplicar `isAppearanceLightStatusBars` en `AppTheme`
  - **Archivo**: `ui/theme/AppTheme.kt` (modificar)
  - **Acción**: En `SideEffect { ... }`, aplicar `WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme`
  - **Criterio**: Íconos de sistema (hora, batería) son blancos en dark mode, oscuros en light mode
  - **Estimación**: Small (~12 líneas)

- [ ] **1.11.3** Verificar que `MainActivity` llama `enableEdgeToEdge()`
  - **Archivo**: `MainActivity.kt` (modificar)
  - **Acción**: Asegurar que `onCreate` llama `enableEdgeToEdge()` antes de `setContent`; si ya lo hace, marcar como ✅; si no, agregar
  - **Criterio**: App muestra status bar transparente sin clipping
  - **Estimación**: Trivial (~5 líneas, verificación)

---

### 1.12 Settings Screen Mínimo (solo branded toggle)

- [ ] **1.12.1** Crear `SettingsScreen` con toggle de branded palette
  - **Archivo**: `ui/screens/SettingsScreen.kt` (nuevo)
  - **Acción**: Crear Composable que muestra `Scaffold` + `Column` con un `SwitchPreference` (título: `stringResource(R.string.settings_branded_palette_title)`, descripción: `stringResource(R.string.settings_branded_palette_description)`, state: `viewModel.brandedPaletteEnabled.collectAsState()`, onChange: `viewModel.setBrandedPaletteEnabled(it)`)
  - **Criterio**: Screen compilable y funcional
  - **Estimación**: Small (~35 líneas)

- [ ] **1.12.2** Agregar strings para Settings screen
  - **Archivo**: `res/values/strings.xml` (modificar)
  - **Acción**: Agregar `<string name="settings_branded_palette_title">Branded palette</string>`, `<string name="settings_branded_palette_description">Use custom branded colors instead of dynamic</string>`, `<string name="nav_settings">Settings</string>`
  - **Criterio**: Strings compilables y consumibles
  - **Estimación**: Trivial (~6 líneas)

- [ ] **1.12.3** Agregar traducciones españolas
  - **Archivo**: `res/values-es/strings.xml` (modificar)
  - **Acción**: Agregar traducciones: `settings_branded_palette_title` → "Paleta branded", `settings_branded_palette_description` → "Usar colores branded personalizados en lugar de dinámicos", `nav_settings` → "Configuración"
  - **Criterio**: Strings traducidos y consumibles
  - **Estimación**: Trivial (~6 líneas)

---

### 1.13 Consumo de Tokens (prueba en ConnectionListScreen)

- [ ] **1.13.1** Refactorizar `ConnectionListScreen` para consumir `AppSpacing.lg`
  - **Archivo**: `ui/screens/ConnectionListScreen.kt` (modificar)
  - **Acción**: CAMBIAR un padding hardcoded `16.dp` → `LocalAppSpacing.current.lg`
  - **Criterio**: Screen muestra padding correcto; prueba de que el sistema funciona end-to-end
  - **Estimación**: Trivial (~3 líneas modificadas)

---

### 1.14 Dependencias Gradle

- [ ] **1.14.1** Verificar dependencias en `build.gradle.kts`
  - **Archivo**: `app/build.gradle.kts` (modificar)
  - **Acción**: Verificar que `androidx.datastore:datastore-preferences:1.0.0` y `androidx.compose.material3:material3-window-size-class:1.2.0` están declarados; si no, agregar
  - **Criterio**: Proyecto compila sin errores de dependencia
  - **Estimación**: Trivial (~4 líneas)

---

### 1.15 Integration Tests PR 4a

- [ ] **1.15.1 [Integration Test]** Test: AppTheme cambia de branded a dynamic en runtime < 16ms
  - **Archivo**: `ui/theme/AppThemeIntegrationTest.kt` (nuevo)
  - **Acción**: Compose UI test que inicializa con `userPrefersBranded = false`, verifica `primary` de MaterialTheme, cambia a `true` vía ViewModel, verifica que `primary` cambió a `brand_primary`; mide tiempo con `measureNanoTime`
  - **Criterio**: Test pasa; cambio ocurre en < 16 ms
  - **Estimación**: Medium (~50 líneas)

---

## Fase 2: Adaptive Scaffold — Contexto de Navegación + Iconos (PR 4b)

**Objetivo**: Implementar el scaffold adaptativo que switchea entre BottomBar/Rail/Drawer según `WindowSizeClass`, con destinos contextuales basados en el route activo. Incluye todos los íconos custom.

**Estimación**: ~750 líneas (15 XML vectors + 6 new files + 1 modified file)

---

### 2.1 NavigationContext (derivación pura desde route)

- [ ] **2.1.1 [TDD RED]** Test: `NavigationContext.from()` parsea routes correctamente
  - **Archivo**: `ui/adaptive/NavigationContextTest.kt` (nuevo)
  - **Acción**: Test unitario que verifica `NavigationContext.from("connection/abc-123/tables") == InsideConnection("abc-123")`, `NavigationContext.from("connections") == OutsideConnection`, `NavigationContext.from(null) == OutsideConnection`
  - **Criterio**: Test falla
  - **Estimación**: Small (~18 líneas)

- [ ] **2.1.2 [TDD GREEN]** Implementar `NavigationContext` sealed class
  - **Archivo**: `ui/adaptive/NavigationContext.kt` (nuevo)
  - **Acción**: Crear `sealed class NavigationContext { object OutsideConnection; data class InsideConnection(val connectionId: String) }` con companion `fun from(route: String?): NavigationContext` usando regex `"connection/([^/]+)/.*"`
  - **Criterio**: Test 2.1.1 pasa
  - **Estimación**: Small (~25 líneas)

---

### 2.2 NavigationDestinations

- [ ] **2.2.1 [TDD RED]** Test: `destinationsForContext(OutsideConnection)` devuelve 2 destinos
  - **Archivo**: `ui/adaptive/NavigationDestinationsTest.kt` (nuevo)
  - **Acción**: Test unitario que verifica `destinationsForContext(OutsideConnection).size == 2`, ids son `"connections"` y `"settings"`
  - **Criterio**: Test falla
  - **Estimación**: Small (~12 líneas)

- [ ] **2.2.2 [TDD RED]** Test: `destinationsForContext(InsideConnection)` devuelve 5 destinos con connectionId interpolado
  - **Archivo**: `ui/adaptive/NavigationDestinationsTest.kt` (modificar)
  - **Acción**: Test unitario que verifica `destinationsForContext(InsideConnection("abc-123")).size == 5`, `route("abc-123")` contiene `"connection/abc-123/tables"`, etc.
  - **Criterio**: Test falla
  - **Estimación**: Small (~20 líneas)

- [ ] **2.2.3 [TDD GREEN]** Implementar `NavigationDestination` data class
  - **Archivo**: `ui/adaptive/NavigationDestinations.kt` (nuevo)
  - **Acción**: Crear `@Immutable data class NavigationDestination(val id: String, @StringRes val labelRes: Int, val icon: @Composable () -> Painter, val route: (connectionId: String?) -> String)`
  - **Criterio**: Data class compilable
  - **Estimación**: Trivial (~8 líneas)

- [ ] **2.2.4 [TDD GREEN]** Implementar `destinationsForContext()`
  - **Archivo**: `ui/adaptive/NavigationDestinations.kt` (modificar)
  - **Acción**: Crear función `fun destinationsForContext(context: NavigationContext): List<NavigationDestination>` que retorna los 2 destinos para `OutsideConnection` o los 5 para `InsideConnection` según el spec
  - **Criterio**: Tests 2.2.1 y 2.2.2 pasan
  - **Estimación**: Medium (~50 líneas)

---

### 2.3 Vectores XML Custom — Navegación

- [ ] **2.3.1** Crear íconos de navegación (7 vectores)
  - **Archivos**: `res/drawable/ic_nav_connections.xml`, `ic_nav_settings.xml`, `ic_nav_tables.xml`, `ic_nav_views.xml`, `ic_nav_editor.xml`, `ic_nav_functions.xml`, `ic_nav_backup.xml` (nuevos)
  - **Acción**: Para cada ícono, usar Material Symbols Rounded variant (según decisión de usuario) importado vía Vector Asset tool, renombrar según convención
  - **Criterio**: 7 vectores compilables, 24dp, tintable
  - **Estimación**: Medium (~120 líneas total — ~17 líneas/vector)

---

### 2.4 Vectores XML Custom — DB Providers

- [ ] **2.4.1** Crear íconos de DB providers (5 vectores)
  - **Archivos**: `res/drawable/ic_db_mysql.xml`, `ic_db_postgres.xml`, `ic_db_sqlite.xml`, `ic_db_mariadb.xml`, `ic_db_sqlserver.xml` (nuevos)
  - **Acción**: Importar vectores simplificados de logos oficiales (estilizados 24dp) vía Vector Asset tool
  - **Criterio**: 5 vectores compilables, tintable
  - **Estimación**: Medium (~85 líneas total — ~17 líneas/vector)

---

### 2.5 Vectores XML Custom — Estados

- [ ] **2.5.1** Crear íconos de estados (3 vectores)
  - **Archivos**: `res/drawable/ic_state_empty_connections.xml`, `ic_state_empty_tables.xml`, `ic_state_error.xml` (nuevos)
  - **Acción**: Crear ilustraciones simples (servidor con X, carpeta vacía, triángulo con exclamación) de 24dp como vectores XML
  - **Criterio**: 3 vectores compilables, tintable
  - **Estimación**: Small (~50 líneas total — ~17 líneas/vector)

---

### 2.6 AppIcons Wrapper

- [ ] **2.6.1** Implementar `AppIcons` object con nested objects
  - **Archivo**: `ui/components/AppIcons.kt` (nuevo)
  - **Acción**: Crear `object AppIcons { object Nav { val Connections: Painter @Composable get() = painterResource(R.drawable.ic_nav_connections); val Settings: Painter @Composable get() = painterResource(R.drawable.ic_nav_settings); ... }; object Db { val MySql: Painter @Composable get() = painterResource(R.drawable.ic_db_mysql); ... }; object State { val EmptyConnections: Painter @Composable get() = ...; ... } }`
  - **Criterio**: Todos los íconos son consumibles como `AppIcons.Nav.Tables`, etc.
  - **Estimación**: Small (~40 líneas)

---

### 2.7 AdaptiveSizing Helpers

- [ ] **2.7.1 [TDD RED]** Test: `adaptivePadding()` devuelve valores correctos por WindowSizeClass
  - **Archivo**: `ui/adaptive/AdaptiveSizingTest.kt` (nuevo)
  - **Acción**: Test unitario que verifica `adaptivePadding(Compact) == PaddingValues(16.dp)`, `adaptivePadding(Medium) == PaddingValues(24.dp)`, etc.
  - **Criterio**: Test falla
  - **Estimación**: Small (~15 líneas)

- [ ] **2.7.2 [TDD GREEN]** Implementar helpers `adaptivePadding()`, `adaptiveGridColumns()`, `adaptiveIconSize()`
  - **Archivo**: `ui/adaptive/AdaptiveSizing.kt` (nuevo)
  - **Acción**: Crear las 3 funciones según spec: `adaptivePadding(windowSizeClass): PaddingValues`, `adaptiveGridColumns(windowSizeClass): Int`, `adaptiveIconSize(windowSizeClass): Dp`
  - **Criterio**: Test 2.7.1 pasa; funciones consumibles
  - **Estimación**: Small (~30 líneas)

---

### 2.8 AdaptiveNavigationScaffold

- [ ] **2.8.1 [TDD RED]** Test: Scaffold conmuta entre BottomBar/Rail/Drawer según WindowSizeClass
  - **Archivo**: `ui/adaptive/AdaptiveNavigationScaffoldTest.kt` (nuevo)
  - **Acción**: Compose UI test parametrizado con `Compact`, `Medium`, `Expanded` que verifica presencia de `NavigationBar`, `NavigationRail`, `PermanentNavigationDrawer` respectivamente (usando `onNodeWithTag`)
  - **Criterio**: Test falla
  - **Estimación**: Medium (~50 líneas)

- [ ] **2.8.2 [TDD GREEN]** Implementar `AdaptiveNavigationScaffold`
  - **Archivo**: `ui/adaptive/AdaptiveNavigationScaffold.kt` (nuevo)
  - **Acción**: Crear `@Composable fun AdaptiveNavigationScaffold(windowSizeClass: WindowSizeClass, navigationContext: NavigationContext, currentRoute: String?, onDestinationSelected: (String) -> Unit, content: @Composable () -> Unit)` que switchea según `widthSizeClass` y renderiza los destinos correctos
  - **Criterio**: Test 2.8.1 pasa
  - **Estimación**: Large (~80 líneas)

---

### 2.9 Integración en AppNavHost

- [ ] **2.9.1** Envolver `NavHost` con `AdaptiveNavigationScaffold`
  - **Archivo**: `ui/navigation/AppNavHost.kt` (modificar)
  - **Acción**: Derivar `navigationContext` vía `remember(currentBackStackEntry) { NavigationContext.from(currentBackStackEntry?.destination?.route) }`, obtener `windowSizeClass` vía `calculateWindowSizeClass(activity = LocalContext.current as Activity)`, envolver `NavHost` existente con `AdaptiveNavigationScaffold(...)`
  - **Criterio**: App corre, scaffold muestra destinos correctos según contexto
  - **Estimación**: Medium (~40 líneas)

---

### 2.10 Strings de Navegación

- [ ] **2.10.1** Agregar strings de navegación en inglés
  - **Archivo**: `res/values/strings.xml` (modificar)
  - **Acción**: Agregar `nav_connections`, `nav_tables`, `nav_views`, `nav_editor`, `nav_functions`, `nav_backup` (6 strings)
  - **Criterio**: Strings compilables
  - **Estimación**: Trivial (~6 líneas)

- [ ] **2.10.2** Agregar traducciones españolas
  - **Archivo**: `res/values-es/strings.xml` (modificar)
  - **Acción**: Agregar traducciones: "Conexiones", "Tablas", "Vistas", "Editor", "Funciones", "Backup"
  - **Criterio**: Strings traducidos
  - **Estimación**: Trivial (~6 líneas)

---

### 2.11 Integration Tests PR 4b

- [ ] **2.11.1 [E2E Test]** Test: Navegación de `connections` a `connection/{id}/tables` cambia destinos de 2 a 5
  - **Archivo**: `ui/navigation/NavigationContextE2ETest.kt` (nuevo)
  - **Acción**: Compose UI test que navega de `connections` a `connection/abc-123/tables`, verifica que aparecen 5 destinos (Tablas, Vistas, Editor, Funciones, Backup)
  - **Criterio**: Test pasa; destinos son visibles
  - **Estimación**: Medium (~45 líneas)

- [ ] **2.11.2 [E2E Test]** Test: Rotación de dispositivo mantiene destino seleccionado y contexto
  - **Archivo**: `ui/navigation/NavigationContextE2ETest.kt` (modificar)
  - **Acción**: Test que navega a `connection/abc-123/editor`, rota pantalla, verifica que sigue en `editor` y scaffold muestra los 5 destinos `InsideConnection`
  - **Criterio**: Test pasa; estado preservado
  - **Estimación**: Medium (~35 líneas)

---

## Fase 3: Premium Components — Skeletons + Cards + Empty States (PR 4c)

**Objetivo**: Refactorizar cards existentes con elevación y tokens, crear skeletons por-pantalla, crear `EmptyState`, y consumir todo desde los 4 screens principales.

**Estimación**: ~850 líneas (8 new files + 8 modified files)

---

### 3.1 Skeleton Primitives

- [ ] **3.1.1 [TDD RED]** Test: `SkeletonBox` renderiza con color placeholder correcto
  - **Archivo**: `ui/components/skeleton/PrimitivesTest.kt` (nuevo)
  - **Acción**: Compose UI test que verifica `SkeletonBox(width = 100.dp, height = 50.dp)` renderiza un `Box` con background gris (`surfaceVariant`) y shape `AppShapes.medium`
  - **Criterio**: Test falla
  - **Estimación**: Small (~15 líneas)

- [ ] **3.1.2 [TDD GREEN]** Implementar primitivas `SkeletonBox`, `SkeletonText`, `SkeletonCircle`
  - **Archivo**: `ui/components/skeleton/Primitives.kt` (nuevo)
  - **Acción**: Crear los 3 Composables: `SkeletonBox(width, height, shape = AppShapes.medium, modifier)`, `SkeletonText(width = 120.dp, lines = 1, modifier)`, `SkeletonCircle(size, modifier)` — todos con background `MaterialTheme.colorScheme.surfaceVariant`
  - **Criterio**: Test 3.1.1 pasa
  - **Estimación**: Small (~40 líneas)

---

### 3.2 ConnectionListSkeleton

- [ ] **3.2.1** Implementar `ConnectionListSkeleton`
  - **Archivo**: `ui/components/skeleton/ConnectionListSkeleton.kt` (nuevo)
  - **Acción**: Composable que renderiza 5 placeholders imitando `ConnectionCard` (cada placeholder: `SkeletonCircle` + 2 `SkeletonText` en `Row`)
  - **Criterio**: Skeleton visualmente coherente con `ConnectionCard`
  - **Estimación**: Small (~35 líneas)

---

### 3.3 DatabaseListSkeleton

- [ ] **3.3.1** Implementar `DatabaseListSkeleton`
  - **Archivo**: `ui/components/skeleton/DatabaseListSkeleton.kt` (nuevo)
  - **Acción**: Composable que renderiza 6 placeholders imitando `DatabaseCard`
  - **Criterio**: Skeleton visualmente coherente
  - **Estimación**: Small (~35 líneas)

---

### 3.4 TableListSkeleton

- [ ] **3.4.1** Implementar `TableListSkeleton`
  - **Archivo**: `ui/components/skeleton/TableListSkeleton.kt` (nuevo)
  - **Acción**: Composable que renderiza 8 placeholders imitando `TableCard`
  - **Criterio**: Skeleton visualmente coherente
  - **Estimación**: Small (~35 líneas)

---

### 3.5 TableViewerSkeleton

- [ ] **3.5.1** Implementar `TableViewerSkeleton`
  - **Archivo**: `ui/components/skeleton/TableViewerSkeleton.kt` (nuevo)
  - **Acción**: Composable que renderiza grid 10×4 imitando filas de tabla (usando `LazyColumn` de `SkeletonBox`)
  - **Criterio**: Skeleton visualmente coherente con tabla real
  - **Estimación**: Medium (~45 líneas)

---

### 3.6 EmptyState Component

- [ ] **3.6.1** Implementar `EmptyState`
  - **Archivo**: `ui/components/EmptyState.kt` (nuevo)
  - **Acción**: Crear `@Composable fun EmptyState(icon: Painter, title: String, description: String? = null, action: (@Composable () -> Unit)? = null)` con layout centrado verticalmente, ícono 96dp, texto con `MaterialTheme.typography.headlineSmall` para título
  - **Criterio**: Componente consumible y visualmente correcto
  - **Estimación**: Small (~40 líneas)

---

### 3.7 EmptyState Strings

- [ ] **3.7.1** Agregar strings de empty states en inglés
  - **Archivo**: `res/values/strings.xml` (modificar)
  - **Acción**: Agregar `empty_connections_title` ("No connections"), `empty_connections_description` ("Add a connection to get started"), `empty_tables_title` ("No tables"), `empty_tables_description` ("This database has no tables")
  - **Criterio**: Strings compilables
  - **Estimación**: Trivial (~8 líneas)

- [ ] **3.7.2** Agregar traducciones españolas
  - **Archivo**: `res/values-es/strings.xml` (modificar)
  - **Acción**: Agregar traducciones: "Sin conexiones", "Agrega una conexión para empezar", "Sin tablas", "Esta base de datos no tiene tablas"
  - **Criterio**: Strings traducidos
  - **Estimación**: Trivial (~8 líneas)

---

### 3.8 Refactorizar ConnectionCard

- [ ] **3.8.1** Aplicar elevación, spacing tokens, AppIcons, animateContentSize a `ConnectionCard`
  - **Archivo**: `ui/components/ConnectionCard.kt` (modificar)
  - **Acción**: CAMBIAR `Card(colors = CardDefaults.cardColors(containerColor = surfaceVariant))` → `Card(modifier = Modifier.shadow(LocalAppElevation.current.cardResting, LocalAppShapes.current.medium), shape = LocalAppShapes.current.medium)`, CAMBIAR padding `16.dp` → `LocalAppSpacing.current.lg`, CAMBIAR spacing `4.dp`, `8.dp` → `LocalAppSpacing.current.xxs`, `.sm`, AGREGAR `Modifier.animateContentSize()` al `Column` interno
  - **Criterio**: Card muestra elevación sutil, spacing correcto, listo para motion futuro
  - **Estimación**: Small (~25 líneas modificadas)

---

### 3.9 Refactorizar DatabaseCard

- [ ] **3.9.1** Aplicar elevación, spacing tokens, AppIcons, animateContentSize a `DatabaseCard`
  - **Archivo**: `ui/components/DatabaseCard.kt` (modificar)
  - **Acción**: Aplicar mismo refactor que `ConnectionCard`
  - **Criterio**: Idem 3.8.1
  - **Estimación**: Small (~25 líneas modificadas)

---

### 3.10 Refactorizar TableCard

- [ ] **3.10.1** Aplicar elevación, spacing tokens, AppIcons, animateContentSize a `TableCard`
  - **Archivo**: `ui/components/TableCard.kt` (modificar)
  - **Acción**: Aplicar mismo refactor que `ConnectionCard`
  - **Criterio**: Idem 3.8.1
  - **Estimación**: Small (~25 líneas modificadas)

---

### 3.11 Refactorizar ErrorCard

- [ ] **3.11.1** Mejorar layout y añadir ícono custom a `ErrorCard`
  - **Archivo**: `ui/components/ErrorCard.kt` (modificar)
  - **Acción**: CAMBIAR layout de `Column` centrada a `Card` con background `errorContainer`, AGREGAR ícono `AppIcons.State.Error` (32dp) a la izquierda del texto, CAMBIAR `Button` a `OutlinedButton` con color `error`, HACER `onRetry` opcional (`onRetry: (() -> Unit)? = null`), CAMBIAR spacing hardcoded a `LocalAppSpacing.current.lg`, AGREGAR max lines en descripción (4 líneas con `TextOverflow.Ellipsis`)
  - **Criterio**: `ErrorCard` visualmente mejorada, coherente con design system
  - **Estimación**: Medium (~35 líneas modificadas)

---

### 3.12 Integrar Skeletons en ConnectionListScreen

- [ ] **3.12.1** Reemplazar `CircularProgressIndicator` por `ConnectionListSkeleton` en estado Loading
  - **Archivo**: `ui/screens/ConnectionListScreen.kt` (modificar)
  - **Acción**: En el branch del estado `Loading`, reemplazar `CircularProgressIndicator()` por `ConnectionListSkeleton()`
  - **Criterio**: Loading muestra skeleton en vez de spinner
  - **Estimación**: Trivial (~3 líneas modificadas)

- [ ] **3.12.2** Reemplazar lógica de lista vacía por `EmptyState`
  - **Archivo**: `ui/screens/ConnectionListScreen.kt` (modificar)
  - **Acción**: En el branch `if (connections.isEmpty())`, reemplazar por `EmptyState(icon = AppIcons.State.EmptyConnections, title = stringResource(R.string.empty_connections_title), description = stringResource(R.string.empty_connections_description), action = { Button(...) })`
  - **Criterio**: Estado vacío muestra `EmptyState` ilustrado
  - **Estimación**: Small (~10 líneas modificadas)

---

### 3.13 Integrar Skeletons en DatabaseListScreen

- [ ] **3.13.1** Reemplazar `CircularProgressIndicator` por `DatabaseListSkeleton`
  - **Archivo**: `ui/screens/DatabaseListScreen.kt` (modificar)
  - **Acción**: Idem 3.12.1 para `DatabaseListSkeleton`
  - **Criterio**: Idem 3.12.1
  - **Estimación**: Trivial (~3 líneas)

- [ ] **3.13.2** Reemplazar lógica de lista vacía por `EmptyState`
  - **Archivo**: `ui/screens/DatabaseListScreen.kt` (modificar)
  - **Acción**: Idem 3.12.2 con `EmptyState` de databases
  - **Criterio**: Idem 3.12.2
  - **Estimación**: Small (~10 líneas)

---

### 3.14 Integrar Skeletons en TableListScreen

- [ ] **3.14.1** Reemplazar `CircularProgressIndicator` por `TableListSkeleton`
  - **Archivo**: `ui/screens/TableListScreen.kt` (modificar)
  - **Acción**: Idem 3.12.1 para `TableListSkeleton`
  - **Criterio**: Idem 3.12.1
  - **Estimación**: Trivial (~3 líneas)

- [ ] **3.14.2** Reemplazar lógica de lista vacía por `EmptyState`
  - **Archivo**: `ui/screens/TableListScreen.kt` (modificar)
  - **Acción**: Idem 3.12.2 con `EmptyState` de tables
  - **Criterio**: Idem 3.12.2
  - **Estimación**: Small (~10 líneas)

---

### 3.15 Integrar Skeletons en TableViewerScreen

- [ ] **3.15.1** Reemplazar `CircularProgressIndicator` por `TableViewerSkeleton`
  - **Archivo**: `ui/screens/TableViewerScreen.kt` (modificar)
  - **Acción**: Idem 3.12.1 para `TableViewerSkeleton`
  - **Criterio**: Idem 3.12.1
  - **Estimación**: Trivial (~3 líneas)

---

### 3.16 Integration Tests PR 4c

- [ ] **3.16.1 [Integration Test]** Test: `ConnectionListSkeleton` renderiza 5 placeholders
  - **Archivo**: `ui/components/skeleton/ConnectionListSkeletonTest.kt` (nuevo)
  - **Acción**: Compose UI test que renderiza `ConnectionListSkeleton()`, cuenta cantidad de `SkeletonBox` renderizados (debería ser 5 placeholders completos)
  - **Criterio**: Test pasa
  - **Estimación**: Small (~20 líneas)

- [ ] **3.16.2 [Integration Test]** Test: `EmptyState` renderiza ícono, título, descripción y botón
  - **Archivo**: `ui/components/EmptyStateTest.kt` (nuevo)
  - **Acción**: Compose UI test que renderiza `EmptyState(icon, title, description, action = { Button(...) })`, verifica presencia de nodos con texto `title`, `description`, `Button`
  - **Criterio**: Test pasa
  - **Estimación**: Small (~25 líneas)

- [ ] **3.16.3 [E2E Test]** Test: Settings toggle "Branded palette" cambia tema en < 16 ms
  - **Archivo**: `ui/screens/SettingsScreenE2ETest.kt` (nuevo)
  - **Acción**: Compose UI test que activa el toggle, mide tiempo de recomposición con `measureNanoTime`, verifica que cambio ocurre en < 16 ms
  - **Criterio**: Test pasa; NFR cumplido
  - **Estimación**: Medium (~40 líneas)

---

## Resumen de Estimación

| Fase | Tareas | Líneas estimadas |
|------|--------|------------------|
| Fase 1 (PR 4a) | 50 tareas | ~650 líneas |
| Fase 2 (PR 4b) | 31 tareas | ~750 líneas |
| Fase 3 (PR 4c) | 37 tareas | ~850 líneas |
| **Total** | **118 tareas** | **~2,250 líneas** |

---

## Orden de Implementación Recomendado

1. **PR 4a** — Foundation (tokens + branded + edge-to-edge + reduced motion) → merge a master
2. **PR 4b** — Adaptive Scaffold (contexto + navegación + iconos) → merge a master después de 4a
3. **PR 4c** — Premium Components (skeletons + cards + empty states) → merge a master después de 4a (puede ser paralelo a 4b o después)

**Bloqueadores**:
- PR 4b depende de PR 4a (necesita tokens y `AppTheme`)
- PR 4c depende de PR 4a (necesita tokens) pero es **independiente** de PR 4b (puede ser paralelo)

**Gates de merge por PR**:
- **PR 4a**: Lint verde, app compila, `ConnectionListScreen` consume `AppSpacing.lg`, toggle de branded palette cambia tema visiblemente
- **PR 4b**: App corre en Compact/Medium/Expanded, muestra BottomBar/Rail/Drawer respectivamente, navegación funciona, destinos cambian al entrar/salir de conexión
- **PR 4c**: Todos los screens muestran skeletons durante carga, `EmptyState` cuando vacío, cards tienen elevación, ningún screen referencia `Icons.Default.*` directamente

---

## Próximo Paso

**Ready for implementation (sdd-apply)** — no se requiere decisión adicional del usuario antes de aplicar.

Comenzar con **PR 4a — Foundation** para sentar la base del design system.
