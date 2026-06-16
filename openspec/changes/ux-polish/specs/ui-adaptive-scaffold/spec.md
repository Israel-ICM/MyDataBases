# Especificación: ui-adaptive-scaffold

## Propósito

Proveer un scaffold de navegación adaptativo que conmute entre `NavigationBar`, `NavigationRail` y `PermanentNavigationDrawer` según `WindowSizeClass`, exponiendo destinos **contextuales** que dependen de si el usuario está fuera o dentro de una conexión activa.

## Requirements

### Requirement: Conmutación por WindowSizeClass

El sistema MUST exponer `AdaptiveNavigationScaffold(windowSizeClass, navigationContext, currentRoute, onDestinationSelected, content)`.

El scaffold MUST seleccionar el componente de navegación según `windowSizeClass.widthSizeClass`:

| WidthSizeClass | Componente                  | Ejemplo dispositivo            |
|----------------|-----------------------------|--------------------------------|
| `Compact`      | `NavigationBar` (bottom)    | Pixel 6 portrait               |
| `Medium`       | `NavigationRail` (left)     | Pixel Tablet portrait, foldable |
| `Expanded`     | `PermanentNavigationDrawer` (left, 280 dp) | Pixel Tablet landscape |

#### Scenario: Compact muestra BottomBar

- GIVEN un dispositivo Pixel 6 portrait (ancho ~411 dp)
- WHEN se renderiza `AdaptiveNavigationScaffold`
- THEN aparece `NavigationBar` abajo
- AND NO aparece `NavigationRail` ni `Drawer`

#### Scenario: Medium muestra NavigationRail

- GIVEN un dispositivo Pixel Tablet portrait (ancho ~800 dp)
- WHEN se renderiza `AdaptiveNavigationScaffold`
- THEN aparece `NavigationRail` a la izquierda
- AND NO aparece `NavigationBar`

#### Scenario: Expanded muestra Drawer permanente

- GIVEN un dispositivo Pixel Tablet landscape (ancho ~1280 dp)
- WHEN se renderiza `AdaptiveNavigationScaffold`
- THEN aparece `PermanentNavigationDrawer` con ancho 280 dp
- AND el contenido ocupa el resto del ancho

### Requirement: Contexto de navegación contextual

El sistema MUST exponer un sealed type:

```kotlin
sealed class NavigationContext {
    object OutsideConnection : NavigationContext()
    data class InsideConnection(val connectionId: String) : NavigationContext()
}
```

Los destinos visibles dependen del contexto:

**OutsideConnection** (2 destinos):
| ID            | Label (es)      | Label (en)    | Ícono                 | Route       |
|---------------|-----------------|---------------|------------------------|-------------|
| `connections` | "Conexiones"    | "Connections" | `AppIcons.Nav.Connections` | `connections` |
| `settings`    | "Configuración" | "Settings"    | `AppIcons.Nav.Settings`    | `settings`    |

**InsideConnection** (5 destinos):
| ID         | Label (es)  | Label (en)  | Ícono                  | Route                |
|------------|-------------|-------------|-------------------------|----------------------|
| `tables`   | "Tablas"    | "Tables"    | `AppIcons.Nav.Tables`   | `connection/{id}/tables` |
| `views`    | "Vistas"    | "Views"     | `AppIcons.Nav.Views`    | `connection/{id}/views`  |
| `editor`   | "Editor"    | "Editor"    | `AppIcons.Nav.Editor`   | `connection/{id}/editor` |
| `functions`| "Funciones" | "Functions" | `AppIcons.Nav.Functions`| `connection/{id}/functions` |
| `backup`   | "Backup"    | "Backup"    | `AppIcons.Nav.Backup`   | `connection/{id}/backup` |

#### Scenario: Cambio de contexto al entrar a conexión

- GIVEN usuario en `connections` (contexto `OutsideConnection`)
- AND ve los destinos "Conexiones" y "Configuración"
- WHEN tap en una `ConnectionCard` y se navega a `connection/abc-123/tables`
- THEN el contexto cambia a `InsideConnection("abc-123")`
- AND los destinos visibles son "Tablas", "Vistas", "Editor", "Funciones", "Backup"
- AND el destino "Tablas" aparece seleccionado

#### Scenario: Cambio de contexto al salir de conexión

- GIVEN usuario en `connection/abc-123/tables` (contexto `InsideConnection`)
- WHEN tap en back hasta volver a `connections`
- THEN el contexto vuelve a `OutsideConnection`
- AND los destinos visibles son "Conexiones" y "Configuración"

### Requirement: Derivación del contexto desde route

El `NavigationContext` MUST derivarse puramente del route activo del `NavController`, NO de estado paralelo.

Algoritmo:

```
si currentRoute coincide con "connection/{id}/..." → InsideConnection(id)
sino → OutsideConnection
```

#### Scenario: Sin estado paralelo

- GIVEN navegación rápida entre `connection/abc-123/tables` y `connections`
- WHEN el route cambia
- THEN `NavigationContext` se deriva en el mismo frame
- AND nunca aparecen brevemente destinos viejos (no hay "flash" de Tablas en pantalla Conexiones)

### Requirement: Helpers adaptativos

El sistema MUST exponer en `ui/adaptive/`:

- `adaptivePadding(windowSizeClass): PaddingValues`
  - `Compact` → 16 dp
  - `Medium` → 24 dp
  - `Expanded` → 32 dp

- `adaptiveGridColumns(windowSizeClass): Int`
  - `Compact` → 2
  - `Medium` → 3
  - `Expanded` → 4

- `adaptiveIconSize(windowSizeClass): Dp`
  - `Compact` → 48 dp
  - `Medium` → 56 dp
  - `Expanded` → 64 dp

#### Scenario: Grid de providers se adapta

- GIVEN pantalla Home con `LazyVerticalGrid`
- WHEN `windowSizeClass.widthSizeClass == Expanded`
- THEN `adaptiveGridColumns()` devuelve 4
- AND la grid renderiza 4 columnas

### Requirement: Touch targets accesibles

Todo `NavigationBarItem` / `NavigationRailItem` / `NavigationDrawerItem` MUST tener un touch target efectivo ≥ 48 dp (cumplimiento WCAG y Material 3).

## Non-Functional Requirements

- **Performance**: Cambio de WindowSizeClass (rotación, fold/unfold) MUST re-renderizar el scaffold en ≤ 1 frame.
- **Accessibility**: Cada destino MUST tener `contentDescription` localizado.
- **Localización**: Todos los labels MUST consumirse vía `stringResource(R.string.nav_*)`, nunca hardcoded.

## Edge Cases

#### Scenario: Medium con 5 destinos (InsideConnection)

- GIVEN tablet portrait (`Medium`) y contexto `InsideConnection`
- WHEN se renderiza `NavigationRail` con 5 destinos
- THEN los 5 items son visibles
- AND ninguno se trunca ni se solapa
- AND el rail mide al menos `5 × 56dp = 280dp` de alto (cabe sin scroll en cualquier tablet portrait)

#### Scenario: Foldable cambia de Compact a Medium

- GIVEN dispositivo Pixel Fold cerrado (`Compact`)
- AND scaffold renderiza `NavigationBar`
- WHEN el usuario despliega (`Medium`)
- THEN el scaffold conmuta a `NavigationRail` automáticamente
- AND la navegación no se pierde (el destino seleccionado se mantiene)

#### Scenario: Rotación dispositivo en InsideConnection

- GIVEN usuario en `connection/abc-123/editor` en portrait (`Compact`)
- WHEN rota a landscape (`Expanded` en tablet, `Compact` en teléfono)
- THEN en tablet: aparece `PermanentNavigationDrawer` con los 5 destinos InsideConnection
- AND el destino "Editor" sigue seleccionado
- AND el contenido del editor no se reinicia

#### Scenario: Deep link directo a InsideConnection

- GIVEN app cerrada
- WHEN el sistema abre la app con un deep link a `connection/abc-123/tables`
- THEN el scaffold inicia directamente con `NavigationContext.InsideConnection("abc-123")`
- AND muestra los 5 destinos correctos

## Constraints

- Min SDK 29 (API 29). `material3-window-size-class` 1.2.0+.
- El scaffold MUST NOT mantener estado de "destinos seleccionados" propio; siempre deriva del `NavController`.
- El sistema MUST NOT exponer un `NavigationContext` mutable desde fuera; solo se calcula internamente desde el route.
