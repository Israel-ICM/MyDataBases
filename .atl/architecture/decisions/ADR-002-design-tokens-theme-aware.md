# ADR-002: DesignTokens theme-aware vía CompositionLocal (dark mode)

## Estado

✅ **Aceptado**

Fecha: 2026-07-10

## Contexto

`DesignTokens` es un `object` con 18 colores hardcodeados (light-only), usado en
~15 archivos (~120 call sites). Al ser un `object` no-composable, no puede leer
`MaterialTheme.colorScheme`, por lo que esas pantallas quedan claras incluso en
modo oscuro. Change SDD `dark-mode` requiere resolverlo.

**Opciones evaluadas**:

1. **Mapear cada call site directamente a `MaterialTheme.colorScheme`** (ej.
   `DesignTokens.SurfacePrimary` → `MaterialTheme.colorScheme.surface`).
2. **Convertir `DesignTokens` en `data class` + `LocalDesignTokens`
   CompositionLocal**, con instancias light/dark derivadas de
   `BrandedLight/DarkColorScheme`.

## Decisión

**Opción 2**: `DesignTokens` pasa a ser una `data class` inmutable, con dos
instancias (`LightDesignTokens`/`DarkDesignTokens`) provistas vía
`LocalDesignTokens` (mismo patrón que `LocalAppSpacing`/`LocalAppShapes` ya
existente en `AppTheme.kt`).

**Razón**:

- `DesignTokens` codifica la identidad branded WCAG AA verificada
  (`SurfacePrimary`, `TextPrimary`, etc.), que NO es 1:1 con los roles genéricos
  de `MaterialTheme.colorScheme` — mapear directo perdería ese ajuste fino.
- Los ~120 call sites solo necesitan un rename mecánico
  (`DesignTokens.X` → `LocalDesignTokens.current.x`), sin reestructurar lógica.
- Reutiliza un patrón de CompositionLocal ya establecido en el proyecto.

**Alternativa descartada**: mapear a `colorScheme` habría sido más simple pero
requiere re-auditar cada call site para encontrar el rol M3 equivalente correcto,
con mayor riesgo de regresión visual y pérdida de la paleta branded.

## Consecuencias

- `ui/theme/Theme.kt` (`MyDataBasesTheme` + alias deprecated) queda muerto y se
  elimina — `AppTheme` es el único entry point de tema.
- Migración se divide en 3 PRs encadenados (plumbing / tokens / custom-draw +
  sweep) por tamaño (~30-40 archivos totales).
- Detalle completo: `openspec/changes/dark-mode/design.md`.
