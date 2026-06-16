# Spec — ux-polish

> Índice consolidado de las 6 capabilities NUEVAS introducidas por este cambio. Cada capability tiene su spec detallada en `openspec/changes/ux-polish/specs/<capability>/spec.md` (siguiendo la convención OpenSpec).

## Capabilities

| # | Capability             | Resumen                                                                      | Spec detallada                              |
|---|------------------------|------------------------------------------------------------------------------|---------------------------------------------|
| 1 | `ui-design-tokens`     | Tokens centralizados (Spacing 4dp, Shapes, Elevation, Motion) + `AppTheme`. | `specs/ui-design-tokens/spec.md`            |
| 2 | `ui-branded-theme`     | Paleta branded dark-first + selección dynamic/branded + edge-to-edge.       | `specs/ui-branded-theme/spec.md`            |
| 3 | `ui-adaptive-scaffold` | Scaffold Bottom/Rail/Drawer con destinos contextuales (Outside/Inside).     | `specs/ui-adaptive-scaffold/spec.md`        |
| 4 | `ui-premium-components`| Cards con elevación + skeletons por-pantalla + `EmptyState` + `ErrorCard`.  | `specs/ui-premium-components/spec.md`       |
| 5 | `ui-icon-system`       | Vectores XML custom + naming convention + wrapper `AppIcons`.               | `specs/ui-icon-system/spec.md`              |
| 6 | `ui-reduced-motion`    | Detección `Settings.Global.ANIMATOR_DURATION_SCALE` + `LocalReducedMotion`. | `specs/ui-reduced-motion/spec.md`           |

## Non-Functional Requirements (transversales)

| NFR             | Criterio                                                              | Capabilities afectadas             |
|-----------------|-----------------------------------------------------------------------|-------------------------------------|
| Performance     | Theme switch < 16 ms; skeleton render 60 fps                          | tokens, branded-theme, premium      |
| Accessibility   | Touch targets ≥ 48 dp; contraste ≥ 4.5:1 (WCAG AA)                    | branded-theme, adaptive, premium    |
| Compatibility   | API 29+                                                               | TODAS                               |
| Localización    | Todos los strings en `strings.xml` (es + en); cero hardcoded          | adaptive, premium, icon, reduced    |

## Capability Dependency Graph

```
ui-design-tokens  ────►  ui-branded-theme       (los esquemas Branded usan los tokens)
       │
       ├──────►  ui-adaptive-scaffold           (helpers consumen AppSpacing)
       │
       ├──────►  ui-premium-components          (cards/skeletons consumen AppShapes/AppElevation)
       │
       ├──────►  ui-reduced-motion              (AppMotion integra LocalReducedMotion)
       │
       └──────►  ui-icon-system                 (iconos respetan colorScheme.* del tema)
```

`ui-design-tokens` es la capability base; las demás la consumen pero no introducen ciclos.

## Edge Cases Globales (cross-capability)

#### Scenario: Dynamic color OFF en el sistema

- GIVEN dispositivo API 33 con dynamic color desactivado a nivel sistema
- AND `userPrefersBranded == false`
- WHEN se aplica `AppTheme`
- THEN se usa `BrandedDarkColorScheme` (fallback definido en `ui-branded-theme`)
- AND NO se intenta `dynamicDarkColorScheme()` (porque no está disponible)
- AND la app se ve branded sin que el usuario haya tenido que activar nada

#### Scenario: Usuario cambia a dynamic color mientras tiene branded ON

- GIVEN app abierta con `userPrefersBranded == true`
- WHEN el usuario va a Settings de la app y desactiva "Branded palette"
- THEN `userPrefersBranded` persiste en `false`
- AND la lógica de selección pasa a dynamic (si disponible) o Branded fallback
- AND el cambio se aplica en < 16 ms sin reiniciar la Activity

#### Scenario: Medium con 5 destinos en `NavigationRail`

- GIVEN tablet portrait (`Medium`) y usuario dentro de una conexión (`InsideConnection`)
- WHEN se renderiza el scaffold
- THEN los 5 destinos (Tablas, Vistas, Editor, Funciones, Backup) son visibles sin overflow
- AND el rail mide `5 × ~56 dp = 280 dp` mínimo, holgado en cualquier tablet portrait (alto ≥ 1024 dp)

#### Scenario: Reduced motion se activa durante el uso

- GIVEN app corriendo con animaciones normales
- WHEN el usuario activa "Animator duration scale = OFF" en Developer Options del sistema
- AND vuelve a la app
- THEN el `ContentObserver` notifica el cambio
- AND `LocalReducedMotion.current` pasa a `true`
- AND TODAS las animaciones futuras se vuelven instantáneas (incluyendo `animateContentSize` en cards y future shimmer en skeletons)

## Criterios de Aceptación del cambio completo

- [ ] Los 6 specs detallados existen en `openspec/changes/ux-polish/specs/<capability>/spec.md`.
- [ ] El proyecto compila (`./gradlew assembleDebug`) con tokens, branded theme y adaptive scaffold integrados.
- [ ] Los success criteria del proposal (10 ítems) se cumplen como gate de archive.
- [ ] No hay regresión funcional en flows existentes (Connection list, Database list, Table list, TableViewer).
- [ ] Lint y tests verdes.

## Referencias

- **Proposal**: Engram `sdd/ux-polish/proposal` (id #1851)
- **Exploration**: Engram `sdd/ux-polish/explore` (id #1850)
- **Filosofía UX**: `.atl/agents/ux-designer.md`
- **Estándar adaptativo**: `.atl/standards/adaptive-layouts.md`
