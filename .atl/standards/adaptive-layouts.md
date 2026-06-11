# Layouts Adaptativos (Tablets y Pantallas Grandes)

La aplicación DEBE funcionar perfectamente en tablets, foldables y pantallas grandes.

## Dispositivos Objetivo

### Teléfonos (Compact)

- **Tamaño**: < 600dp de ancho
- **Orientación**: Portrait primero
- **Navegación**: Bottom Navigation o Navigation Drawer
- **Ejemplos**: Pixel 6, Samsung S23, iPhone 14 Pro

### Tablets (Medium/Expanded)

- **Tamaño**: ≥ 600dp de ancho
- **Orientación**: Portrait y Landscape
- **Navegación**: Navigation Rail o Permanent Drawer
- **Ejemplos**: iPad, Galaxy Tab S8, Pixel Tablet

### Foldables

- **Tamaño**: Variable (según estado plegado/desplegado)
- **Adaptación**: Cambiar layout según estado
- **Ejemplos**: Galaxy Z Fold, Pixel Fold

### Desktop/ChromeOS (Futuro)

- **Tamaño**: > 840dp de ancho
- **Navegación**: Permanent Drawer + múltiples paneles
- **Ejemplos**: ChromeOS, Windows 11 con Android apps

## Window Size Classes

Usar `WindowSizeClass` de Material 3 para adaptar layouts.

### Implementación

**Agregar dependencia**:

```gradle
dependencies {
    implementation "androidx.compose.material3:material3-window-size-class:1.2.0"
}
```

**Uso en Compose**:

```kotlin
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass

@Composable
fun MyDataBasesApp() {
    val windowSizeClass = calculateWindowSizeClass(this)
    
    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> CompactLayout()
        WindowWidthSizeClass.Medium -> MediumLayout()
        WindowWidthSizeClass.Expanded -> ExpandedLayout()
    }
}
```

### Size Classes

| Clase | Ancho | Dispositivo | Layout |
|-------|-------|-------------|--------|
| **Compact** | < 600dp | Teléfonos | Single pane, bottom nav |
| **Medium** | 600dp - 839dp | Tablets pequeños, foldables | Dual pane, navigation rail |
| **Expanded** | ≥ 840dp | Tablets grandes, desktop | Triple pane, permanent drawer |

## Patrones de Navegación Adaptativa

### 1. Navegación por Tamaño de Pantalla

```kotlin
@Composable
fun AdaptiveNavigationScaffold(
    windowSizeClass: WindowSizeClass,
    content: @Composable (PaddingValues) -> Unit
) {
    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            // Teléfono: Bottom Navigation
            Scaffold(
                bottomBar = { BottomNavigationBar() }
            ) { paddingValues ->
                content(paddingValues)
            }
        }
        
        WindowWidthSizeClass.Medium -> {
            // Tablet pequeño: Navigation Rail
            Row {
                NavigationRail()
                Scaffold { paddingValues ->
                    content(paddingValues)
                }
            }
        }
        
        WindowWidthSizeClass.Expanded -> {
            // Tablet grande: Permanent Drawer
            PermanentNavigationDrawer(
                drawerContent = { NavigationDrawerContent() }
            ) {
                Scaffold { paddingValues ->
                    content(paddingValues)
                }
            }
        }
    }
}
```

### 2. Bottom Navigation (Compact)

**Para teléfonos**:

```kotlin
@Composable
fun BottomNavigationBar() {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_home)) },
            selected = currentRoute == "home",
            onClick = { navController.navigate("home") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Storage, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_connections)) },
            selected = currentRoute == "connections",
            onClick = { navController.navigate("connections") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_settings)) },
            selected = currentRoute == "settings",
            onClick = { navController.navigate("settings") }
        )
    }
}
```

### 3. Navigation Rail (Medium)

**Para tablets pequeños**:

```kotlin
@Composable
fun NavigationRail() {
    NavigationRail(
        header = {
            FloatingActionButton(
                onClick = { /* Nueva conexión */ }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) {
        NavigationRailItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_home)) },
            selected = currentRoute == "home",
            onClick = { navController.navigate("home") }
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Storage, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_connections)) },
            selected = currentRoute == "connections",
            onClick = { navController.navigate("connections") }
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_settings)) },
            selected = currentRoute == "settings",
            onClick = { navController.navigate("settings") }
        )
    }
}
```

### 4. Permanent Drawer (Expanded)

**Para tablets grandes**:

```kotlin
@Composable
fun NavigationDrawerContent() {
    PermanentDrawerSheet(
        modifier = Modifier.width(280.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                label = { Text(stringResource(R.string.nav_home)) },
                selected = currentRoute == "home",
                onClick = { navController.navigate("home") }
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Storage, contentDescription = null) },
                label = { Text(stringResource(R.string.nav_connections)) },
                selected = currentRoute == "connections",
                onClick = { navController.navigate("connections") }
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text(stringResource(R.string.nav_settings)) },
                selected = currentRoute == "settings",
                onClick = { navController.navigate("settings") }
            )
        }
    }
}
```

## Layouts Multi-Pane

### List-Detail Pattern

**Teléfono (Compact)**: Vista de lista → Vista de detalle (navegación secuencial)

**Tablet (Medium/Expanded)**: Lista + Detalle lado a lado

```kotlin
@Composable
fun ConnectionsScreen(windowSizeClass: WindowSizeClass) {
    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            // Teléfono: Single pane
            if (selectedConnection == null) {
                ConnectionListPane()
            } else {
                ConnectionDetailPane(selectedConnection)
            }
        }
        
        WindowWidthSizeClass.Medium, WindowWidthSizeClass.Expanded -> {
            // Tablet: Dual pane
            Row(modifier = Modifier.fillMaxSize()) {
                // Lista (izquierda)
                ConnectionListPane(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                )
                
                // Detalle (derecha)
                if (selectedConnection != null) {
                    ConnectionDetailPane(
                        connection = selectedConnection,
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight()
                    )
                } else {
                    EmptyDetailPane(
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}
```

### Database Explorer Multi-Pane (Tablet Grande)

**Para tablets grandes**: 3 paneles

```kotlin
@Composable
fun DatabaseExplorerExpandedLayout() {
    Row(modifier = Modifier.fillMaxSize()) {
        // Panel 1: Tree de base de datos (izquierda)
        DatabaseTreePane(
            modifier = Modifier
                .weight(0.25f)
                .fillMaxHeight()
        )
        
        // Panel 2: Lista de tablas/vistas (centro)
        DatabaseObjectsPane(
            modifier = Modifier
                .weight(0.35f)
                .fillMaxHeight()
        )
        
        // Panel 3: Vista de datos (derecha)
        DataViewPane(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
        )
    }
}
```

## Grids Adaptativos

### Providers Grid (Home Screen)

```kotlin
@Composable
fun ProvidersGrid(windowSizeClass: WindowSizeClass) {
    val columns = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2        // Teléfono: 2 columnas
        WindowWidthSizeClass.Medium -> 3         // Tablet pequeño: 3 columnas
        WindowWidthSizeClass.Expanded -> 4       // Tablet grande: 4 columnas
        else -> 2
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(databaseProviders) { provider ->
            ProviderCard(provider)
        }
    }
}
```

## Espaciado Adaptativo

### Padding y Margins

```kotlin
@Composable
fun AdaptivePadding(windowSizeClass: WindowSizeClass): PaddingValues {
    return when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> PaddingValues(16.dp)
        WindowWidthSizeClass.Medium -> PaddingValues(24.dp)
        WindowWidthSizeClass.Expanded -> PaddingValues(32.dp)
        else -> PaddingValues(16.dp)
    }
}
```

### Tamaños de Componentes

```kotlin
@Composable
fun ProviderCard(
    provider: DatabaseProvider,
    windowSizeClass: WindowSizeClass
) {
    val iconSize = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 48.dp
        WindowWidthSizeClass.Medium -> 64.dp
        WindowWidthSizeClass.Expanded -> 72.dp
        else -> 48.dp
    }
    
    Card {
        Column {
            Icon(
                painter = painterResource(provider.icon),
                contentDescription = null,
                modifier = Modifier.size(iconSize)
            )
            Text(provider.name)
        }
    }
}
```

## Orientación (Portrait/Landscape)

### Detectar Orientación

```kotlin
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun OrientationAwareLayout() {
    val configuration = LocalConfiguration.current
    
    when (configuration.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> {
            PortraitLayout()
        }
        Configuration.ORIENTATION_LANDSCAPE -> {
            LandscapeLayout()
        }
    }
}
```

### Tablet Landscape (Aprovechar espacio horizontal)

```kotlin
@Composable
fun ConnectionFormLandscape() {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Columna izquierda
        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = connectionName,
                onValueChange = { connectionName = it },
                label = { Text(stringResource(R.string.form_connection_name)) }
            )
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text(stringResource(R.string.form_host)) }
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Columna derecha
        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.form_username)) }
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.form_password)) }
            )
        }
    }
}
```

## Soporte de Foldables

### Detectar Postura del Dispositivo

```kotlin
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

@Composable
fun FoldableAwareLayout() {
    val windowInfoTracker = WindowInfoTracker.getOrCreate(LocalContext.current)
    
    // Detectar si está plegado, semi-plegado, etc.
    // Ajustar layout según postura
}
```

### Usar Hinge (Bisagra) como Separador Natural

En dispositivos foldables, usar la bisagra para dividir contenido:

- **Panel izquierdo**: Lista de conexiones
- **Panel derecho**: Detalle de conexión

## Testing en Múltiples Tamaños

### Usar Preview con Diferentes Dispositivos

```kotlin
@Preview(name = "Phone", device = Devices.PIXEL_4)
@Preview(name = "Tablet", device = Devices.PIXEL_C)
@Preview(name = "Foldable", device = Devices.FOLDABLE)
@Composable
fun ConnectionScreenPreview() {
    MyDataBasesTheme {
        ConnectionScreen()
    }
}
```

### Emuladores Recomendados

- **Teléfono**: Pixel 6 (API 34)
- **Tablet**: Pixel Tablet (API 34)
- **Foldable**: Pixel Fold (API 34)

### Testing Manual

- Rotar dispositivo (portrait/landscape)
- Cambiar tamaño de fuente (accesibilidad)
- Cambiar densidad de pantalla
- Plegar/desplegar (foldables)

## Checklist de Tablets

Antes de cada release:

- [ ] UI funciona en Compact, Medium y Expanded
- [ ] Navegación adaptativa implementada
- [ ] Layouts multi-pane en tablets
- [ ] Grids usan columnas adaptativas
- [ ] Espaciado apropiado para cada tamaño
- [ ] Portrait y Landscape soportados
- [ ] Testeado en emulador de tablet
- [ ] Screenshots de tablet para Play Store
- [ ] Descripción de Play Store menciona soporte de tablets

## Play Store Optimization

### Declarar Soporte de Tablets

**AndroidManifest.xml**:

```xml
<supports-screens
    android:smallScreens="false"
    android:normalScreens="true"
    android:largeScreens="true"
    android:xlargeScreens="true" />
```

### Screenshots de Tablet

Play Store requiere screenshots separados:

- **Teléfono**: Mínimo 2 (obligatorio)
- **Tablet 7"**: Mínimo 2 (opcional pero recomendado)
- **Tablet 10"**: Mínimo 2 (opcional pero recomendado)

**Tamaños**:

- Tablet 7": 1024 x 600 o 1920 x 1200
- Tablet 10": 2048 x 1536 o 2560 x 1800

---

**Recordá**: MyDataBases es una herramienta profesional. Los usuarios profesionales usan tablets. El soporte de tablets NO es opcional.
