# Tasks: PlayStation Design Language — Fase 1

## Review Workload Forecast

| Campo | Valor |
|-------|-------|
| Líneas estimadas cambiadas | 800-1000 (nuevos archivos: ~650, modificaciones: ~200-350) |
| Riesgo presupuesto 400 líneas | **HIGH** |
| Chained PRs recomendados | **Sí** |
| Split sugerido | PR 1 (Components) → PR 2 (Workspace) → PR 3 (Integration) |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Unidades de Trabajo Sugeridas

| Unidad | Objetivo | PR probable | Notas |
|--------|----------|-------------|-------|
| 1 | Design Components (HeroCard, SectionCard, Selector, XLShape) | PR 1 | Base para visual — ~280 líneas nuevas, standalone |
| 2 | Workspace Infrastructure (Manager + Overlay + State + Module) | PR 2 | Depende de PR 1 solo para tipos; ~380 líneas nuevas |
| 3 | Integration (ConnectionForm fix, ConnectionsList consume, TableCardContent extract, wiring) | PR 3 | Depende de PR 1 + PR 2; ~200-350 líneas modificadas |

**Rationale split**:
- PR 1 entrega componentes visuales standalone testables en isolation (preview)
- PR 2 entrega infraestructura Workspace sin tocar screens existentes (wiring mínimo para probar con mock)
- PR 3 conecta todo (form fix, list migration, table extract, overlay en MainActivity)

## Fase 1: Design Components (2.5h)

### 1.1 Crear `XLShape.kt` con corner radius 24dp
- **Archivo**: `app/src/main/java/com/sphynxs/mydatabases/ui/theme/XLShape.kt`
- **Acción**: Definir `val XLShape = RoundedCornerShape(24.dp)` como extensión de `Shapes`
- **Esfuerzo**: 15min
- **Verificación**: Importar en `HeroConnectionCard` sin error
```kotlin
// app/src/main/java/com/sphynxs/mydatabases/ui/theme/XLShape.kt
package com.sphynxs.mydatabases.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val XLShape = RoundedCornerShape(24.dp)
```

### 1.2 Crear `HeroConnectionCard.kt` (icon 88dp gradient, shadow 8dp, sin border)
- **Archivo**: `app/src/main/java/com/sphynxs/mydatabases/ui/components/HeroConnectionCard.kt`
- **Acción**:
  - Card width `screenWidth * 0.92f` (requiere `LocalConfiguration.current`)
  - Shape `XLShape`, elevation `8.dp`, border `null`
  - Row padding `24.dp`: Box gradient `88.dp` + Column metadata
  - Typography: `titleLarge` para nombre, `bodySmall + fontFamily = FontFamily.Monospace` para host:port
  - `StatusPill("Inactiva")` inline (Text + Surface rounded 100.dp, containerColor `tertiaryContainer`)
  - Gradient: `Brush.radialGradient()` con colores de `DbAccents` según tipo (mapeo MySQL/Postgres/MariaDB/SQLite)
- **Esfuerzo**: 1h
- **Verificación**: Preview con mock Connection → card ancha, icon grande con gradient, typography correcta, pill visible
```kotlin
@Composable
fun HeroConnectionCard(
    connection: Connection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val dbAccent = when (connection.type) {
        DatabaseType.MYSQL -> DbAccents.mysql
        DatabaseType.POSTGRESQL -> DbAccents.postgres
        DatabaseType.MARIADB -> DbAccents.mariadb
        DatabaseType.SQLITE -> DbAccents.sqlite
    }
    
    Card(
        onClick = onClick,
        modifier = modifier.width(screenWidth * 0.92f),
        shape = XLShape,
        border = null,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(dbAccent, dbAccent.copy(alpha = 0.6f))
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = connection.name,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${connection.host}:${connection.port}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "Inactiva",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
```

### 1.3 Crear `SectionCard.kt` (containerColor `surfaceContainer`, shape `large` 16dp)
- **Archivo**: `app/src/main/java/com/sphynxs/mydatabases/ui/components/SectionCard.kt`
- **Acción**:
  - Card colors `surfaceContainer`, shape `MaterialTheme.shapes.large`
  - Column padding `20.dp`
  - Text title `labelLarge + onSurfaceVariant`
  - Spacer `8.dp`
  - Slot `content: @Composable ColumnScope.() -> Unit`
- **Esfuerzo**: 30min
- **Verificación**: Preview con title "Identidad" + TextField mock → color container subtle, title arriba, spacing correcto
```kotlin
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
```

### 1.4 Crear `DatabaseTypeSelector.kt` (SegmentedButton M3)
- **Archivo**: `app/src/main/java/com/sphynxs/mydatabases/ui/components/DatabaseTypeSelector.kt`
- **Acción**:
  - SingleChoiceSegmentedButtonRow con 4 opciones: MySQL, PostgreSQL, MariaDB, SQLite
  - SegmentedButton por tipo con `selected`, `onClick`, icon + label
  - Icons: `Icons.Default.Storage` placeholder (spec no define íconos custom por DB)
- **Esfuerzo**: 30min
- **Verificación**: Preview con estado `DatabaseType.MYSQL` → botón MySQL selected, click cambia selección
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseTypeSelector(
    selected: DatabaseType,
    onSelected: (DatabaseType) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        DatabaseType.values().forEachIndexed { index, type ->
            SegmentedButton(
                selected = selected == type,
                onClick = { onSelected(type) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = DatabaseType.values().size
                ),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null
                    )
                }
            ) {
                Text(type.name)
            }
        }
    }
}
```

## Fase 2: Connection Form Redesign (2h)

### 2.1 Reescribir `ConnectionFormScreen` con 3 SectionCards
- **Archivo**: `app/src/main/java/com/sphynxs/mydatabases/ui/screens/connections/ConnectionFormScreen.kt` (modificar)
- **Acción**:
  - Reemplazar OutlinedTextFields sueltos con 3 `SectionCard`:
    1. **"Identidad"**: name, color
    2. **"Conexión"**: host, port, DatabaseTypeSelector (quita Dropdown)
    3. **"Autenticación"**: user, password (fix icon), database
  - Vertical arrangement spacing `16.dp` entre cards
  - Mantener botones abajo (FilledTonalButton guardar + OutlinedButton eliminar) con spacing `12.dp`
- **Esfuerzo**: 1h
- **Verificación**: Form muestra 3 secciones visuales claras, selector DB funciona, spacing cómodo

### 2.2 Fix password icon (Visibility/VisibilityOff)
- **Archivo**: `app/src/main/java/com/sphynxs/mydatabases/ui/screens/connections/ConnectionFormScreen.kt` (línea ~230)
- **Acción**:
  - Cambiar `Icons.Default.AccountBox` por `Icons.Default.Visibility` cuando `passwordVisible == false`
  - Cambiar a `Icons.Default.VisibilityOff` cuando `passwordVisible == true`
  - Verificar que `material-icons-extended` esté en `app/build.gradle.kts` dependencies (si falta, agregar)
- **Esfuerzo**: 15min
- **Verificación**: Click en icon togglea visibilidad Y cambia el ícono correctamente

### 2.3 Verificar dependencia `material-icons-extended`
- **Archivo**: `app/build.gradle.kts`
- **Acción**:
  - Buscar línea `implementation("androidx.compose.material:material-icons-extended:...")`
  - Si falta, agregar con versión compatible (`1.7.5` o variable `libs.versions.toml`)
- **Esfuerzo**: 15min
- **Verificación**: Sync Gradle sin errores, Visibility icons importan sin error

## Fase 3: Workspace Infrastructure (1.5h)

### 3.1 Crear `WorkspaceCard.kt` (sealed class)
- **Archivo**: `app/src/main/java/com/sphynxs/mydatabases/ui/workspace/WorkspaceCard.kt`
- **Acción**:
  - Sealed class `WorkspaceCard` con:
    - `data class TableCard(val connectionId: Long, val tableName: String)`
  - (Futuras: QueryCard, SchemaCard, etc. en Fase 2)
- **Esfuerzo**: 15min
- **Verificación**: Compila, importa en WorkspaceManager sin error
```kotlin
package com.sphynxs.mydatabases.ui.workspace

sealed class WorkspaceCard {
    data class TableCard(
        val connectionId: Long,
        val tableName: String
    ) : WorkspaceCard()
}
```

### 3.2 Crear `WorkspaceState.kt` (enum Collapsed/Expanded)
- **Archivo**: `app/src/main/java/com/sphynxs/mydatabases/ui/workspace/WorkspaceState.kt`
- **Acción**:
  - Enum class `WorkspaceState { Collapsed, Expanded }`
- **Esfuerzo**: 10min
- **Verificación**: Compila, usado en WorkspaceManager
```kotlin
package com.sphynxs.mydatabases.ui.workspace

enum class WorkspaceState {
    Collapsed,
    Expanded
}
```

### 3.3 Crear `WorkspaceManager.kt` (Hilt @Singleton)
- **Archivo**: `app/src/main/java/com/sphynxs/mydatabases/ui/workspace/WorkspaceManager.kt`
- **Acción**:
  - `@Singleton class WorkspaceManager @Inject constructor()`
  - `_cards = MutableStateFlow<List<WorkspaceCard>>(emptyList())`
  - `cards: StateFlow<List<WorkspaceCard>>`
  - `_state = MutableStateFlow(WorkspaceState.Collapsed)`
  - `state: StateFlow<WorkspaceState>`
  - `_activeCardIndex = MutableStateFlow(0)`
  - `activeCardIndex: StateFlow<Int>`
  - Methods:
    - `fun openCard(card: WorkspaceCard)` — agrega y expande
    - `fun closeCard(index: Int)` — remueve, ajusta active si necesario
    - `fun setActive(index: Int)`
    - `fun toggleState()`
    - `fun collapse()`
    - `fun expand()`
- **Esfuerzo**: 45min
- **Verificación**: Hilt inyecta sin error, StateFlows emiten correctamente
```kotlin
package com.sphynxs.mydatabases.ui.workspace

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceManager @Inject constructor() {
    private val _cards = MutableStateFlow<List<WorkspaceCard>>(emptyList())
    val cards: StateFlow<List<WorkspaceCard>> = _cards.asStateFlow()
    
    private val _state = MutableStateFlow(WorkspaceState.Collapsed)
    val state: StateFlow<WorkspaceState> = _state.asStateFlow()
    
    private val _activeCardIndex = MutableStateFlow(0)
    val activeCardIndex: StateFlow<Int> = _activeCardIndex.asStateFlow()
    
    fun openCard(card: WorkspaceCard) {
        _cards.value = _cards.value + card
        _activeCardIndex.value = _cards.value.lastIndex
        _state.value = WorkspaceState.Expanded
    }
    
    fun closeCard(index: Int) {
        val newCards = _cards.value.toMutableList().apply { removeAt(index) }
        _cards.value = newCards
        if (_activeCardIndex.value >= newCards.size && newCards.isNotEmpty()) {
            _activeCardIndex.value = newCards.lastIndex
        }
        if (newCards.isEmpty()) {
            _state.value = WorkspaceState.Collapsed
        }
    }
    
    fun setActive(index: Int) {
        if (index in _cards.value.indices) {
            _activeCardIndex.value = index
        }
    }
    
    fun toggleState() {
        _state.value = when (_state.value) {
            WorkspaceState.Collapsed -> WorkspaceState.Expanded
            WorkspaceState.Expanded -> WorkspaceState.Collapsed
        }
    }
    
    fun collapse() {
        _state.value = WorkspaceState.Collapsed
    }
    
    fun expand() {
        _state.value = WorkspaceState.Expanded
    }
}
```

### 3.4 Crear `WorkspaceModule.kt` (Hilt module)
- **Archivo**: `app/src/main/java/com/sphynxs/mydatabases/di/WorkspaceModule.kt`
- **Acción**:
  - `@Module @InstallIn(SingletonComponent::class)` vacío (WorkspaceManager usa `@Inject` constructor, no necesita `@Provides`)
  - Solo para estructura futura si se agregan bindings custom
- **Esfuerzo**: 10min
- **Verificación**: Build exitoso, Hilt encuentra WorkspaceManager
```kotlin
package com.sphynxs.mydatabases.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object WorkspaceModule {
    // WorkspaceManager usa @Inject constructor — binding automático
}
```

## Fase 4: WorkspaceOverlay (2.5h)

### 4.1 Crear `TableCardContent.kt` (extraer de TableViewerScreen)
- **Archivo**: `app/src/main/java/com/sphynxs/mydatabases/ui/workspace/TableCardContent.kt`
- **Acción**:
  - Copiar `TableViewerContent` privado de `TableViewerScreen.kt` → renombrar a `TableCardContent`
  - Remover Scaffold, TopAppBar, BackButton
  - Mantener `RowsTab` privado adentro del mismo archivo (copiar composable y viewmodel inline si es pequeño)
  - Mantener solo tab "Filas" (remover TabRow si tiene múltiples tabs, hardcodear solo RowsTab)
  - Params: `connectionId: Long, tableName: String, modifier: Modifier`
  - Usar `hiltViewModel()` para obtener ViewModel si TableViewerScreen ya lo tiene
- **Esfuerzo**: 1h
- **Verificación**: Preview con mock data → tabla se renderiza sin Scaffold, solo contenido de filas
```kotlin
@Composable
fun TableCardContent(
    connectionId: Long,
    tableName: String,
    modifier: Modifier = Modifier
) {
    // Extract RowsTab logic from TableViewerScreen
    // No Scaffold, no TopAppBar, solo el contenido de tabla
    Column(modifier = modifier.fillMaxSize()) {
        // RowsTab content inline
        Text("Table: $tableName") // Placeholder — real impl usa LazyColumn con rows
    }
}
```

### 4.2 Crear `WorkspaceOverlay.kt` — Collapsed State (3 cards + FAB)
- **Archivo**: `app/src/main/java/com/sphynxs/mydatabases/ui/workspace/WorkspaceOverlay.kt`
- **Acción**:
  - `@Composable fun WorkspaceOverlay(manager: WorkspaceManager)`
  - `val cards by manager.cards.collectAsState()`
  - `val state by manager.state.collectAsState()`
  - Si `cards.isEmpty()` → `return` (no renderizar nada)
  - Si `state == Collapsed`:
    - Row bottom-end con `Modifier.padding(16.dp)`
    - `cards.take(3)` → Box mini preview (scale 0.3, alpha 0.7, width 80.dp, height 120.dp, background surfaceContainerHigh, shape large)
    - FloatingActionButton "+" con `onClick = { manager.expand() }`
- **Esfuerzo**: 45min
- **Verificación**: Con 4 cards mock → solo 3 previews + FAB visibles, click FAB expande (checar con log)
```kotlin
@Composable
fun WorkspaceOverlay(
    manager: WorkspaceManager,
    modifier: Modifier = Modifier
) {
    val cards by manager.cards.collectAsState()
    val state by manager.state.collectAsState()
    
    if (cards.isEmpty()) return
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        when (state) {
            WorkspaceState.Collapsed -> {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cards.take(3).forEach { card ->
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(120.dp)
                                .graphicsLayer {
                                    scaleX = 0.3f
                                    scaleY = 0.3f
                                    alpha = 0.7f
                                }
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = MaterialTheme.shapes.large
                                )
                        )
                    }
                    FloatingActionButton(onClick = { manager.expand() }) {
                        Icon(Icons.Default.Add, contentDescription = "Expandir workspace")
                    }
                }
            }
            WorkspaceState.Expanded -> {
                // TODO: Phase 4.3
            }
        }
    }
}
```

### 4.3 Crear `WorkspaceOverlay.kt` — Expanded State (ModalBottomSheet + HorizontalPager)
- **Archivo**: `app/src/main/java/com/sphynxs/mydatabases/ui/workspace/WorkspaceOverlay.kt` (continuar)
- **Acción**:
  - Si `state == Expanded`:
    - ModalBottomSheet con `onDismissRequest = { manager.collapse() }`
    - `sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)`
    - Column: handle pill arriba (8.dp height, 32.dp width, containerColor onSurfaceVariant alpha 0.4)
    - HorizontalPager con `pagerState`, count `cards.size`
    - `Modifier.graphicsLayer` para peek effect:
      - `val pageOffset = (page - pagerState.currentPage) + pagerState.currentPageOffsetFraction`
      - `scaleX = lerp(0.94f, 1f, 1f - abs(pageOffset))`
      - `alpha = lerp(0.6f, 1f, 1f - abs(pageOffset))`
    - Dots indicator abajo (HorizontalPagerIndicator de accompanist o custom Row de Boxes)
    - Card content: cuando `card is WorkspaceCard.TableCard` → `TableCardContent(connectionId, tableName)`
- **Esfuerzo**: 1.5h (gesture handling + pager peek más complejo de lo esperado)
- **Verificación**: Swipe horizontal cambia cards con peek, drag down cierra sheet, dots indicator sincronizado
```kotlin
WorkspaceState.Expanded -> {
    val pagerState = rememberPagerState(
        initialPage = manager.activeCardIndex.collectAsState().value,
        pageCount = { cards.size }
    )
    
    ModalBottomSheet(
        onDismissRequest = { manager.collapse() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Handle pill
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(32.dp)
                    .height(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(100.dp)
                    )
            )
            
            // HorizontalPager con peek
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                pageSpacing = 16.dp,
                contentPadding = PaddingValues(horizontal = 32.dp)
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val pageOffset = (page - pagerState.currentPage) + pagerState.currentPageOffsetFraction
                            val absOffset = abs(pageOffset)
                            scaleX = lerp(0.94f, 1f, 1f - absOffset)
                            scaleY = scaleX
                            alpha = lerp(0.6f, 1f, 1f - absOffset)
                        }
                ) {
                    when (val card = cards[page]) {
                        is WorkspaceCard.TableCard -> {
                            TableCardContent(
                                connectionId = card.connectionId,
                                tableName = card.tableName
                            )
                        }
                    }
                }
            }
            
            // Dots indicator
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(cards.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (index == pagerState.currentPage) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                },
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}
```

## Fase 5: Integration (1.5h)

### 5.1 Modificar `ConnectionsListScreen.kt` para consumir `HeroConnectionCard`
- **Archivo**: `app/src/main/java/com/sphynxs/mydatabases/ui/screens/connections/ConnectionsListScreen.kt`
- **Acción**:
  - Reemplazar `ConnectionCard` actual con `HeroConnectionCard`
  - LazyRow horizontal arrangement spacing `16.dp`
  - Mantener onClick navegación existente
- **Esfuerzo**: 20min
- **Verificación**: Lista horizontal muestra hero cards anchas, scroll funciona, click navega

### 5.2 Modificar `DatabasesListScreen.kt` para abrir `WorkspaceCard.TableCard`
- **Archivo**: `app/src/main/java/com/sphynxs/mydatabases/ui/screens/databases/DatabasesListScreen.kt`
- **Acción**:
  - Inyectar `workspaceManager: WorkspaceManager = hiltViewModel()` (o usar `@Inject` si screen no es ViewModel-scoped)
  - En onClick de tabla: `workspaceManager.openCard(WorkspaceCard.TableCard(connectionId, tableName))`
  - Remover navegación a `/table/{id}/{name}` SI se quiere workspace exclusivo (proposal dice Workspace vive como overlay — confirmar si mantener navegación legacy o solo workspace)
- **Esfuerzo**: 20min
- **Verificación**: Click en tabla abre workspace expanded con TableCardContent

### 5.3 Modificar `TableViewerScreen.kt` como wrapper legacy (opcional)
- **Archivo**: `app/src/main/java/com/sphynxs/mydatabases/ui/screens/tableviewer/TableViewerScreen.kt`
- **Acción**:
  - Si se mantiene navegación `/table/{id}/{name}` (dual-path durante migración):
    - Scaffold + TopAppBar con BackButton
    - Body: `TableCardContent(connectionId, tableName)`
  - Si se elimina ruta legacy: marcar file como deprecated o eliminar
- **Esfuerzo**: 15min
- **Verificación**: Navegación legacy sigue funcionando SI se mantiene, o build sin errores SI se elimina

### 5.4 Agregar `WorkspaceOverlay` en `MainActivity` o `MyDataBasesNavHost`
- **Archivo**: `app/src/main/java/com/sphynxs/mydatabases/MainActivity.kt` o `ui/navigation/MyDataBasesNavHost.kt`
- **Acción**:
  - En el Box root (encima del NavHost o Scaffold):
    ```kotlin
    Box(Modifier.fillMaxSize()) {
        AdaptiveNavigationScaffold(...) // o NavHost
        WorkspaceOverlay(manager = hiltViewModel()) // z-index arriba
    }
    ```
  - Asegurar que WorkspaceManager se inyecta en scope correcto (Activity-scoped para persistir entre navegaciones)
- **Esfuerzo**: 20min
- **Verificación**: Workspace se renderiza encima del bottom bar, persiste al navegar entre pantallas

### 5.5 Testing manual end-to-end
- **Acción**:
  1. Abrir app → ConnectionsList muestra HeroConnectionCards anchas
  2. Ir a DatabasesList → click en tabla abre Workspace expanded
  3. Swipe horizontal entre cards (si hay múltiples abiertas)
  4. Drag down cierra Workspace → cards aparecen collapsed abajo con FAB "+"
  5. Click FAB vuelve a expandir
  6. ConnectionForm muestra 3 secciones, selector DB funciona, password icon correcto
- **Esfuerzo**: 15min
- **Verificación**: Flow completo sin crashes, gestures funcionan, visual cumple specs

## Fase 6: Cleanup (20min)

### 6.1 Remover `ConnectionCard.kt` viejo (si no se usa más)
- **Archivo**: `app/src/main/java/com/sphynxs/mydatabases/ui/components/ConnectionCard.kt`
- **Acción**: Eliminar archivo SI ConnectionsListScreen ya no lo importa
- **Esfuerzo**: 5min
- **Verificación**: Build sin errores de missing imports

### 6.2 Actualizar imports en archivos modificados
- **Acción**: Verificar que todos los archivos modificados importan `HeroConnectionCard`, `SectionCard`, `DatabaseTypeSelector`, `WorkspaceManager`, `XLShape` correctamente
- **Esfuerzo**: 10min
- **Verificación**: Build exitoso sin warnings de unused imports

### 6.3 Verificación final de dependencies
- **Archivo**: `app/build.gradle.kts`
- **Acción**:
  - Confirmar `material-icons-extended` presente
  - Sync Gradle
- **Esfuerzo**: 5min
- **Verificación**: Gradle sync exitoso, iconos Visibility importan sin error

---

## Totales

| Fase | Tareas | Esfuerzo Estimado |
|------|--------|-------------------|
| Fase 1: Design Components | 4 | 2.5h |
| Fase 2: Connection Form Redesign | 3 | 2h |
| Fase 3: Workspace Infrastructure | 4 | 1.5h |
| Fase 4: WorkspaceOverlay | 3 | 2.5h |
| Fase 5: Integration | 5 | 1.5h |
| Fase 6: Cleanup | 3 | 20min |
| **Total** | **22** | **~10.3h** |

## Orden de Implementación

**Dependencias críticas**:
1. Fase 1 (Components) ANTES de Fase 2 (Form consume SectionCard + Selector)
2. Fase 1 (XLShape, HeroCard) ANTES de Fase 5.1 (ConnectionsList consume)
3. Fase 3 (WorkspaceManager + Card) ANTES de Fase 4 (WorkspaceOverlay)
4. Fase 4 (TableCardContent) ANTES de Fase 4.3 (Expanded state renderiza content)
5. Fases 1-4 completas ANTES de Fase 5 (Integration wiring)

**Ruta crítica recomendada (chained PRs)**:
- **PR 1** (Fase 1 completa) → merge → **PR 2** (Fase 2 + Fase 3) → merge → **PR 3** (Fase 4 + Fase 5 + Fase 6)
- **Alternativa conservadora**: PR 1 (Fase 1) → PR 2 (Fase 3 + Fase 4.1) → PR 3 (Fase 2 + Fase 4.2-4.3) → PR 4 (Fase 5 + Fase 6)

## Testing por Fase

### Fase 1 Verification
- Preview de `HeroConnectionCard` con mock Connection → card ancha, gradient visible, typography correcta
- Preview de `SectionCard` con TextField mock → container color sutil, title arriba
- Preview de `DatabaseTypeSelector` con estado mock → selección funciona

### Fase 2 Verification
- Form renderiza 3 secciones visuales distintas
- Selector DB cambia tipo al hacer click
- Password icon togglea entre Visibility/VisibilityOff

### Fase 3 Verification
- WorkspaceManager se inyecta sin error de Hilt
- `openCard()` agrega card y expande state
- `closeCard()` remueve y ajusta active index

### Fase 4 Verification
- Collapsed state muestra máximo 3 previews + FAB
- Expanded state swipe horizontal cambia page con peek effect
- Drag down cierra ModalBottomSheet
- Dots indicator sincronizado con pager

### Fase 5 Verification
- ConnectionsList horizontal scroll con HeroCards
- DatabasesList click tabla → workspace expande
- TableCardContent renderiza tabla sin Scaffold
- WorkspaceOverlay persiste entre navegaciones

### Fase 6 Verification
- Build sin warnings
- No imports sin usar
- Gradle sync exitoso
