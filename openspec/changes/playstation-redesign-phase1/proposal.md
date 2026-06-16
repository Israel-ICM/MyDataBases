# Proposal: PlayStation Redesign — Fase 1

## Intent

Material 3 estándar se percibe como genérico y poco profesional ("sinceramente no lo veo nada profesional" — feedback usuario tras `ui-professional-redesign`). Fase 1 reemplaza el shell visual por un design language custom inspirado en PlayStation App: hero cards grandes con gradient, sin borders, mucho aire, y un **workspace multi-tab tipo card stack** (bottom sheet con swipe horizontal entre tablas abiertas + drag vertical para minimizar/expandir).

## Scope

### In Scope
- **ConnectionsList**: `HeroConnectionCard` nuevo (icon 88dp + radial gradient `DbAccents`, shadow 8dp, shape XL 24dp, sin borders, host:port mono).
- **Connection Form**: 3 `SectionCard` (Identidad, Conexión, Auth), `SegmentedButton` M3 para DatabaseType (MySQL/Postgres/MariaDB/SQLite), fix password icon (`Visibility`/`VisibilityOff`), jerarquía de botones (Save filled / Test text).
- **Workspace infrastructure**: `WorkspaceManager` singleton Hilt con `StateFlow<List<WorkspaceCard>>`, `activeIndex`, `state`. `WorkspaceCard.Table(connectionId, databaseName, tableName)` único variant.
- **WorkspaceOverlay**: `ModalBottomSheet` M3 sobre NavHost (z-index por encima del bottom bar), `HorizontalPager` interno con card stack peek (scale 0.94f, alpha 0.6f en cards detrás), handle pill 32×4dp, dots indicator 6dp.
- **Estados del sheet**: Collapsed muestra peek de 3 cards + FAB "+"; Expanded muestra todas con scroll horizontal (sin límite).
- **TableViewer adaptation**: extraer `TableCardContent(databaseName, tableName)` sin `Scaffold`, solo tab "Filas", header provisto por workspace. Wrapper `TableViewerScreen` legacy preservado para NavHost.

### Out of Scope (Fase 2)
- Query editor / Table structure editor / Backup / Function / View cards.
- TableViewer tabs **SQL** e **Info** (solo "Filas" en Fase 1).
- Spring physics animations y 3D rotation (usar tween/emphasized).
- Status timestamp real (hardcoded "Inactiva").
- Persistencia de WorkspaceCards entre restarts de la app.

### User Decisions (confirmadas)
1. Status pill: **"Inactiva" estático** (timestamp diferido).
2. Workspace Z-index: **arriba del bottom bar**.
3. Cards: **sin límite**; Collapsed = 3 cards + "+", Expanded = todas con scroll horizontal.
4. TableViewer: **solo tab "Filas"** en Fase 1.
5. **`material-icons-extended`** se agrega si no está como dependency.
6. DB selector: **`SegmentedButton` M3 simple**.

## Capabilities

### New Capabilities
- `workspace-multi-tab`: sistema de workspace card stack (abrir/cerrar/navegar tablas en bottom sheet con swipe horizontal y drag vertical).

### Modified Capabilities
- `connection-form`: agrega selector de `DatabaseType` (antes hardcoded) y reestructura UI en secciones.
- `table-viewer`: ahora puede renderizarse como card content dentro del workspace overlay (antes full-screen only).
- `connections-list`: hero cards custom reemplazan layout M3 genérico.

## Approach

**Hybrid Pragmático**: M3 customizado donde aporta (Card, SegmentedButton, ModalBottomSheet, HorizontalPager) + composables custom donde M3 estorba (HeroConnectionCard, SectionCard, peek stack). No tocamos lógica de datos ni ViewModels existentes — solo UI shell y un nuevo `WorkspaceManager`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `ui/connections/HeroConnectionCard.kt` | New | Hero card custom (88dp icon gradient, shadow 8dp) |
| `ui/connections/ConnectionFormScreen.kt` | Modified | 3 SectionCards + SegmentedButton + icon fix |
| `ui/workspace/WorkspaceManager.kt` | New | Singleton Hilt con StateFlow de cards |
| `ui/workspace/WorkspaceOverlay.kt` | New | ModalBottomSheet + HorizontalPager + peek |
| `ui/workspace/WorkspaceCard.kt` | New | Sealed class (solo `Table` en Fase 1) |
| `ui/tableviewer/TableCardContent.kt` | New | Extracción sin Scaffold, solo "Filas" |
| `ui/tableviewer/TableViewerScreen.kt` | Modified | Wrapper legacy para rutas NavHost |
| `MainActivity.kt` o `AppScaffold` | Modified | Montar `WorkspaceOverlay` sobre NavHost |
| `build.gradle.kts` (app) | Modified | Agregar `material-icons-extended` si falta |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Gesture conflict (drag vertical vs HorizontalPager vs scroll interno) | High | `nestedScroll` + `sheetState` M3 ya resuelve verticalmente; pager maneja horizontal |
| `WorkspaceManager` `@Singleton` persiste cards entre conexiones distintas | Med | Limpiar cards al cambiar `activeConnectionId`; documentar scope |
| `material-icons-extended` no presente o aumenta APK size | Low | Verificar antes de agregar; aceptable para Fase 1 |
| Card stack peek con muchas cards degrada performance | Low | Solo renderizar 3 visibles + lazy en HorizontalPager |

## Rollback Plan

`git revert` del merge completo. Fase 1 no toca capa de datos, repositorios, ni ViewModels — solo UI y un manager nuevo aislado. Las rutas NavHost legacy (`TableViewerScreen` wrapper) siguen funcionando aunque se quite el overlay.

## Dependencies

- `androidx.compose.material:material-icons-extended` (agregar si no está).
- `androidx.compose.material3` con `ModalBottomSheet`, `SegmentedButton`, `HorizontalPager` (ya presentes).
- Hilt para `WorkspaceManager` (ya configurado).

## Success Criteria

- [ ] ConnectionsList se ve radicalmente distinto a Material 3 genérico (hero cards 88dp gradient, sin borders).
- [ ] Connection Form muestra 3 secciones visuales y el `SegmentedButton` cambia el `DatabaseType`.
- [ ] Password icon usa `Visibility`/`VisibilityOff` correctamente.
- [ ] Se pueden abrir múltiples tablas desde `DatabasesList` y aparecen en el workspace.
- [ ] Swipe horizontal cambia entre tablas; drag vertical minimiza/expande el sheet.
- [ ] Collapsed muestra peek de 3 cards + FAB "+"; Expanded muestra todas con scroll horizontal.
- [ ] `TableCardContent` muestra filas correctamente (tab "Filas" funcional).
- [ ] Workspace overlay queda **por encima** del bottom bar.
- [ ] Build SUCCESS, sin crashes en flujo: crear conexión → abrir DB → abrir 2+ tablas → swipe → minimizar.
