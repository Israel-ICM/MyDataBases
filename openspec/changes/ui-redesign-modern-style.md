# UI Redesign - Modern Style

**Type:** Feature  
**Status:** Completed  
**Date:** 2026-06-17

## Summary

Complete UI redesign with modern iOS-inspired style, branded color palette (violet + turquoise), breathing background animation, borderless cards with pronounced shadows, and Tabler Icons integration.

## Changes

### Design System
- Created `DesignTokens.kt` with centralized colors, typography, spacing
- Branded color palette: Violet #7C80E8 + Turquoise #8EE3D3
- Background gradient animated (breathing effect) between subtle violet/turquoise
- Removed gray colors entirely (user preference)

### Cards & Shadows
- Removed all border lines from cards
- Increased corner radius: 16dp → 24dp (more rounded)
- Increased shadow elevation: 4dp → 12dp (pronounced floating effect)
- Icon corner radius: 12dp → 16dp
- Shadow colors use branded violet with alpha

### Icons
- Integrated **Tabler Icons** library (`br.com.devsrsouza.compose.icons:tabler-icons:1.1.0`)
- Created `PhosphorAppIcons` object (naming kept for compatibility, uses Tabler internally)
- All icons migrated:
  - Database: `TablerIcons.Database`
  - Server/Connections: `TablerIcons.Server`
  - Tables: `TablerIcons.Table`
  - Settings: `TablerIcons.Settings`
  - Search, Edit, Delete, Add, etc: All Tabler
- Updated navigation (BottomBar, Rail, Drawer) to use Tabler
- `NavigationDestination` changed from `iconRes: Int` to `icon: ImageVector`

### Components
- `IOSCard`: Borderless, 24dp radius, 12dp shadow elevation, violet-tinted shadows
- `IOSSearchBar`: 26dp radius (almost round), black icons 24dp, white background
- `BreathingBackground`: Infinite animated gradient (8s cycle, FastOutSlowInEasing)
- `ConnectionCard`, `DatabaseCard`, `TableCard`: Unified style with icon gradients

### Screens
- All list screens use `BreathingBackground` with subtle violet→turquoise gradient
- Large titles iOS 26 style: 34sp Bold
- Search bars: 26dp radius, black text/icons, white background
- Cards: 56dp icons with gradient backgrounds

### MySQL Tolerance (Bonus Fix)
- Added `zeroDateTimeBehavior=convertToNull` to connection properties
- Added `jdbcCompliantTruncation=false` for data truncation tolerance
- Changed `resultSet.getObject()` to `resultSet.getString()` for raw data reading
- Now handles invalid dates (`0000-00-00`) and corrupted data like Navicat

## Files Modified

### New Files
- `app/src/main/java/com/sphynxs/mydatabases/ui/theme/DesignTokens.kt`
- `app/src/main/java/com/sphynxs/mydatabases/ui/components/ios/IOSCard.kt`
- `app/src/main/java/com/sphynxs/mydatabases/ui/components/ios/IOSSearchBar.kt`
- `app/src/main/java/com/sphynxs/mydatabases/ui/components/BreathingBackground.kt`
- `app/src/main/java/com/sphynxs/mydatabases/ui/components/PhosphorAppIcons.kt`

### Modified Files
- `app/build.gradle.kts` - Added Tabler Icons dependency
- `app/src/main/java/com/sphynxs/mydatabases/ui/components/ConnectionCard.kt`
- `app/src/main/java/com/sphynxs/mydatabases/ui/components/DatabaseCard.kt`
- `app/src/main/java/com/sphynxs/mydatabases/ui/components/TableCard.kt`
- `app/src/main/java/com/sphynxs/mydatabases/ui/components/DatabaseTypeCard.kt`
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/connections/ConnectionsListScreen.kt`
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/databases/DatabasesListScreen.kt`
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/tables/TablesListScreen.kt`
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/tableviewer/TableViewerScreen.kt`
- `app/src/main/java/com/sphynxs/mydatabases/ui/navigation/NavigationDestinations.kt`
- `app/src/main/java/com/sphynxs/mydatabases/ui/adaptive/AdaptiveNavigationScaffold.kt`
- `app/src/main/java/com/sphynxs/mydatabases/core/database/engine/mysql/MySQLConnectionPool.kt`
- `app/src/main/java/com/sphynxs/mydatabases/core/database/engine/mysql/MySQLEngine.kt`

## Technical Decisions

### Why Tabler Icons?
- Material Icons rejected as "horrible" by user
- Phosphor Icons: no stable Compose dependency available
- Tabler Icons: clean, minimal, consistent, works reliably
- Library: `br.com.devsrsouza.compose.icons:tabler-icons:1.1.0`

### Why getString() instead of getObject()?
- JDBC `getObject()` fails on invalid dates with strict type conversion
- `getString()` reads raw data like Navicat (no conversion)
- User requirement: show real data, never NULL

### Why breathing animation?
- User requested animated background
- Subtle 8-second cycle prevents distraction
- Uses branded colors (violet ↔ turquesa)
- No performance impact (single Float animation)

## Commits
- `a575b87` - feat: rediseño completo del sistema de UI con estilo moderno
- `471a0a2` - refactor: migrar todos los colores a paleta branded violeta/turquesa
- `5aece86` - feat: agregar animación 'breathing' al fondo de las pantallas
- `92ae9ac` - fix: reducir saturación del fondo a casi blanco
- `138ea4a` - adjust: aumentar levemente saturación del fondo
- `23a339a` - refactor: eliminar bordes de línea y aumentar radius de cards
- `8787810` - fix: aumentar elevación de sombras en cards
- `8e95e9c` - fix: cambiar ícono de databases de tabla a database
- `026a9dc` - feat: integrar Phosphor Icons para diseño minimalista
- `80fbb76` - feat: usar Phosphor Icons en DatabaseCard y TableCard
- `08c07c1` - feat: migrar todos los íconos a Phosphor Icons
- `dd9f8ec` - refactor: migrar DatabaseTypeCard a Phosphor Icons
- `91b6973` - fix: usar Material Icons Rounded en vez de Phosphor
- `bf3159e` - fix: resolver conflicto de nombres Table
- `df6ffb4` - feat: migrar íconos de navegación a Material Icons Rounded
- `baba5ec` - fix: cambiar ícono de connections a Dns para diferenciarlo de databases
- `2cf6b85` - fix: usar ícono Database específico en DatabaseCard
- `5fb37c7` - fix: revertir Database a Storage (Database no existe en Material Icons)
- `a56c89d` - feat: migrar todos los íconos a Tabler Icons
- `0436115` - fix: tolerancia con datos MySQL inválidos como Navicat

## Testing

✅ Build successful  
✅ All screens render correctly  
✅ Breathing animation smooth (8s cycle)  
✅ Icons display correctly (Tabler)  
✅ MySQL invalid data handled (0000-00-00 dates, corrupted fields)  
✅ Navigation icons updated (BottomBar, Rail, Drawer)  
✅ Cards shadow prominent and visible  

## User Feedback

- ✅ "Material Icons es horrible" → Switched to Tabler
- ✅ "No uses gris para nada es horrible" → Removed all grays
- ✅ "Un poco más fuerte el color" → Adjusted background saturation
- ✅ "No me gustan esos bordes de linea" → Removed borders, only shadows
- ✅ "La sombra es muy baja" → Increased to 12dp
- ✅ "Hay un ícono específico de database" → Tabler has `Database` icon
- ✅ "Debe mostrar los datos reales nunca null" → Fixed with getString()
