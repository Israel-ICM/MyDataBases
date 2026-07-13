# Design: Dark Mode Finish & Migration (`dark-mode`)

## Technical Approach

Two independent axes drive the final `ColorScheme`: `theme_mode` (SYSTEM/LIGHT/DARK, new)
resolves light-vs-dark; `branded_palette` (existing) resolves branded-vs-dynamic. `AppTheme`
resolves both, provides one `ColorScheme` to `MaterialTheme` plus one `DesignTokens`
instance to a new `LocalDesignTokens` CompositionLocal, mirroring the existing
`LocalAppSpacing`/`LocalAppShapes` pattern. Everything else plugs into this resolved state.

## Architecture Decisions

| Decision | Choice | Alternatives | Rationale |
|---|---|---|---|
| Token theming | `data class` + `LocalDesignTokens` (light/dark instances) | Map sites onto `colorScheme` | Preserves branded WCAG AA identity; ~120 sites need only a rename |
| Theme entry point | Delete `MyDataBasesTheme` + alias; `AppTheme` sole entry point | Deprecate-only | Non-branded schemes in `Theme.kt` become dead once `AppTheme`'s unwired logic activates |
| `theme_mode` ownership | `MainActivity` reads VM, passes into `AppTheme(themeMode=)` | `AppTheme` self-fetches | `AppTheme` already needs its own VM for `brandedPaletteEnabled`; parameter stays Preview-friendly |
| Carousel shadow tint | `colorScheme.onSurface` vs `Color.BLACK` | `colorScheme.scrim` | Black-on-dark is invisible; `onSurface` contrasts in both, matching Material's lighter-overlay dark-elevation technique |
| Stray-literal triage | Structural vs decorative split (below) | Fix all ~70 now | Full sweep balloons PR-3; decorative literals degrade gracefully |

## Data Flow

    SettingsRepository (DataStore) ──▶ SettingsViewModel ──▶ MainActivity(themeMode)
                                              │                        │ passed in
                                              ▼                        ▼
                                    AppTheme(themeMode) — own VM call for branded
                                              │ resolves darkTheme + Branded/Dynamic scheme
                                              │ selects Light/DarkDesignTokens
                                              ▼
                          CompositionLocalProvider(LocalDesignTokens) + MaterialTheme
                                              │
                          ┌───────────────────┴───────────────────┐
                          ▼                                       ▼
               MaterialTheme.colorScheme                 LocalDesignTokens.current
               (31 healthy consumers)                    (~15 files, ~120 sites)

## File Changes

| File | Action | Description |
|---|---|---|
| `ui/theme/DesignTokens.kt` | Modify | `object` → `data class` + `Light/DarkDesignTokens` + `LocalDesignTokens` |
| `ui/theme/AppTheme.kt` | Modify | Remove hack; wire `brandedPaletteEnabled`; provide `LocalDesignTokens` |
| `ui/theme/Theme.kt` | Delete | `MyDataBasesTheme` + alias + non-branded schemes, all dead |
| `MainActivity.kt` | Modify | Read `themeMode` from VM; migrate `@Preview` off old theme |
| `SettingsRepository(Impl).kt` | Modify | Add `observeThemeMode()`/`setThemeMode()`, new `stringPreferencesKey` |
| `SettingsViewModel.kt` | Modify | Add `themeMode` StateFlow + setter, mirrors branded toggle |
| `SettingsScreen.kt` | Modify | Add System/Light/Dark selector below branded toggle |
| `WorkspaceCarousel.kt` | Modify | Shadow tint from `onSurface`; scrim from `colorScheme` |
| ~15 `DesignTokens.` consumers (proposal's list) | Modify | Rename `DesignTokens.X` → `LocalDesignTokens.current.x` |
| ~17 files, `MyDataBasesTheme` in `@Preview` | Modify | Swap to `AppTheme(themeMode = ...)` |
| `res/values*/strings.xml` (×10) | Modify | Theme-mode labels, all locales |
| ~70 stray `Color(0x...)` literals | Partial | Structural subset only; triage below |

## Interfaces / Contracts

```kotlin
@Immutable
data class DesignTokens(
    val backgroundPrimary: Color, val surfacePrimary: Color, val textPrimary: Color,
    // ... remaining color fields; typography/spacing/icon-size fields are
    // theme-INVARIANT but stay here too, for one consistent access pattern
)

val LocalDesignTokens = staticCompositionLocalOf { LightDesignTokens }
private val LightDesignTokens = buildDesignTokens(BrandedLightColorScheme)
private val DarkDesignTokens = buildDesignTokens(BrandedDarkColorScheme)

private fun buildDesignTokens(scheme: ColorScheme): DesignTokens {
    val textPrimary = scheme.onBackground // local val: roles compose from it
    return DesignTokens(textPrimary = textPrimary, cardTitleColor = textPrimary, ...)
}
```

**Migration rule**: `DesignTokens.SurfacePrimary` → `LocalDesignTokens.current.surfacePrimary`
(PascalCase static → camelCase instance). **Gotcha**: must read inside `@Composable` scope —
in non-composable draw lambdas (`drawBehind`, `Canvas`), capture to a local `val` first, same
as `MaterialTheme.colorScheme` already does in `WorkspaceCarouselItem`.

`SettingsRepository` gains `observeThemeMode()`/`setThemeMode(mode)`, same shape as the
existing branded-palette methods.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | `observeThemeMode`/`setThemeMode` round-trip | JUnit4 + fake `DataStore<Preferences>` |
| Unit | `SettingsViewModel.themeMode` StateFlow + setter | Mockk repository, `kotlinx-coroutines-test` |
| Unit | Dark-theme resolution (LIGHT/DARK/SYSTEM → boolean) | Extract `resolveDarkTheme(mode, systemDark)` as pure function |
| UI (instrumentation) | `SettingsScreen` taps invoke `setThemeMode` | Compose UI Test |
| Manual/visual | Token migration + Carousel shadow/scrim | Screenshot check light+dark, no contrast tooling |

**Risk**: no existing coverage for the ~15 `DesignTokens` consumers or `WorkspaceCarousel`.
Strict TDD applies to NEW code; retrofitting ~120 call sites is out of scope here — budget
one smoke/golden test per high-traffic screen in PR-2/PR-3.

## Migration / Rollout

Informative only — final chaining decided at tasks phase with the user (ask-always):

1. **PR-1**: `theme_mode` key/repo/VM + selector; un-hack `AppTheme`; fix `MainActivity`;
   delete `Theme.kt`; migrate `@Preview`; 10 locales.
2. **PR-2**: `DesignTokens` → `LocalDesignTokens`; migrate ~15 consumers.
3. **PR-3**: Carousel shadow/scrim; structural literals; IOS shadows/scrims from proposal.

**Stray-literal triage**: ask *"if the background flips light↔dark, does this color still
read against its adjacent surface?"* — **structural** (must-fix): backgrounds, surfaces,
scrims, body text. **decorative** (defer): low-alpha accent tints/glows that degrade
gracefully. Default to structural when unsure. No data migration; `theme_mode` defaults to
`SYSTEM`, purely additive.

## Open Questions

- [ ] Exact WCAG AA contrast for `textSecondary`/`accentPrimary` in `DarkDesignTokens` —
      light literals don't map 1:1 onto `BrandedLightColorScheme` roles; check during PR-2.
- [ ] `SettingsScreen` widget: recommend `SingleChoiceSegmentedButtonRow` (idiomatic M3),
      `RadioButton` column as fallback.
