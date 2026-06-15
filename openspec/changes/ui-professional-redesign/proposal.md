# Proposal: UI Professional Redesign

## Intent

La UI actual es Material 3 genérica: cards planas tipo Row, headers sin metadata, sin identidad visual por tipo de DB, sin jerarquía clara. Para la demo de HOY necesitamos elevar la percepción a "producto premium" estilo PlayStation App (hero visuals, gradients, depth) combinado con densidad funcional tipo Navicat (badges, stats, quick metadata). Cambio puramente visual; lógica intacta.

## Scope

### In Scope
- Rediseño de `ConnectionCard` (hero icon 56dp por tipo DB, status dot visual, gradient sutil, `titleLarge`).
- Rediseño de `DatabaseCard` (icono prominente, badges de metadata).
- Rediseño de `TableCard` (badge prominente de row count).
- `ConnectionsListScreen` con `LargeTopAppBar` (collapse on scroll) + stats header.
- `TableViewerScreen` tabs con iconos + badges.
- Helper de press animation reusable (`Modifier` con scale + elevation via `interactionSource`).

### Out of Scope
- Shimmer real en skeleton loaders (quedan estáticos por ahora).
- Grid layouts adaptativos (`LazyVerticalGrid`).
- Shared element transitions entre screens.
- Glassmorphism / blur effects.
- Status real de conexiones (el dot es visual hardcoded; integración con health-check va aparte).
- Nuevos componentes paralelos — todo es refactor in-place.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- None — el redesign es puramente visual sobre componentes existentes. No introduce ni cambia requerimientos funcionales a nivel spec. Tokens, lógica de navegación y modelos no se modifican.

## Approach

**Conservative Redesign — refactor in-place reusando el sistema de diseño existente.**

- Reusar tokens ya disponibles: `AppShapes.large` (20dp), `AppElevation.cardPressed` (8dp), `AppMotion.emphasized`, paleta branded violeta/turquesa.
- Reusar `AppIcons.Db.*` (MySql, Postgres, Sqlite, MariaDB, SqlServer) para hero icons coloridos sin assets nuevos.
- Mapear color de acento por tipo de DB (verde MySQL, azul Postgres, etc.) via función pura en `ui/components/`.
- Press feedback unificado: helper `Modifier.pressAnimation()` con `animateFloatAsState` sobre scale + elevation, fuente única.
- Orden de ataque por impacto visual descendente: ConnectionCard → headers → DatabaseCard/TableCard → TableViewer tabs → polish.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `ui/components/ConnectionCard.kt` | Modified | Hero icon 56dp, status dot, gradient bg, tipografía `titleLarge`. |
| `ui/components/DatabaseCard.kt` | Modified | Icono grande + badges de metadata. |
| `ui/components/TableCard.kt` | Modified | Row count como badge prominente. |
| `ui/screens/connections/ConnectionsListScreen.kt` | Modified | `LargeTopAppBar` + stats header (total conexiones). |
| `ui/screens/tableviewer/TableViewerScreen.kt` | Modified | Tabs con iconos + badges. |
| `ui/components/PressAnimation.kt` | New | Helper `Modifier` reusable. |
| `ui/components/DbAccent.kt` | New | Mapping `DbType -> Color` para acentos. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Gradients/elevación rompen contraste WCAG en dark mode | Med | Validar contraste en ambos themes antes de cerrar cada card. |
| Press animation lag en listas largas | Low | `interactionSource` + `animateFloatAsState` (cheap); medir si hay jank. |
| Scope creep — caer en Aggressive sin querer | High | Lista Out-of-Scope es vinculante; cualquier extra va a fase 2. |
| Regresión visual en pantallas no tocadas | Low | Sólo se editan archivos listados en Affected Areas. |

## Rollback Plan

Cambio acotado a archivos UI. Rollback = `git revert` del commit del change. Sin migraciones de datos, sin cambios de API, sin tocar DI ni navegación. Branch dedicado permite descartar sin afectar main.

## Dependencies

- Ninguna externa. Todos los tokens, iconos y motion specs ya existen en `ui/theme/tokens/` y `ui/icons/`.

## Success Criteria

- [ ] `ConnectionCard` muestra hero icon 56dp + status dot + gradient + título `titleLarge`.
- [ ] Identidad por tipo de DB inmediata (color de acento distinto MySQL/Postgres/SQLite/MariaDB/SqlServer).
- [ ] Jerarquía visual clara: títulos grandes, metadata secundaria pequeña.
- [ ] Press feedback animado (scale + elevation) consistente en los 3 cards.
- [ ] Headers informativos con stats visibles (al menos en ConnectionsList).
- [ ] `./gradlew assembleDebug` SUCCESS sin warnings nuevos.
- [ ] Sin regresiones en navegación ni en datos mostrados.
- [ ] Demo-ready en ≤6h de trabajo.
