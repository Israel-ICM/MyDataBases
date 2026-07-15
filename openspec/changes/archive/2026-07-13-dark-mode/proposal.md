# Proposal: Dark Mode support (`dark-mode`)

## Intent

Users cannot choose a dark appearance. The theming scaffolding is **already
largely built but disconnected** — this is a *finish-and-migrate* effort, NOT
greenfield theming work. Dark/light Material 3 `ColorScheme`s (branded, WCAG AA),
a `ThemeMode` enum (LIGHT/DARK/SYSTEM), and DataStore settings infra all exist,
but three gaps break the wire end-to-end and one large migration debt keeps most
screens light-only:

- `AppTheme.kt` has a `TEMPORAL` hack forcing branded colors, ignoring settings.
- `MainActivity.kt` hardcodes `ThemeMode.SYSTEM`; the user's choice is never persisted.
- No `theme_mode` DataStore key / repository method / Settings control exists.
- `DesignTokens.kt` is a static `object` of 18 hardcoded light-only colors used
  across ~15 files (~120 call sites) — it cannot read `MaterialTheme.colorScheme`,
  so its screens stay light even in dark mode. **This is the architectural crux.**

## Scope

### In Scope
- Settings toggle for System/Light/Dark, persisted via DataStore (`theme_mode` key).
- Un-hack `AppTheme` (read persisted preference) and fix `MainActivity` to consume it.
- Declare **`AppTheme` as the canonical theme entry point**; migrate remaining
  `MyDataBasesTheme` usages (incl. `@Preview`) and retire the deprecated alias.
- Make `DesignTokens` **theme-aware** via a CompositionLocal (`LocalDesignTokens`)
  supplying light/dark instances — stable call-site names where possible.
- Migrate high-density `DesignTokens.` consumers to the theme-aware accessor.
- Dark-safe custom-draw: `WorkspaceCarousel` `BlurMaskFilter` shadow and IOS
  component shadows/scrims (`IOSDropdownField/Menu`, `IOSCard`, `IOSSearchBar`,
  `CompletionPopup`, etc.); dark-tune scrim/gradients (`BackdropScrim`,
  `BackgroundGradient*`, `CardShadowColor`).
- Sweep ~70 stray `Color(0x...)` literals in screens/components onto tokens/`colorScheme`.
- New theme-toggle strings in **all 10 locales** (android-dev i18n rule).

### Out of Scope
- Dynamic / Material You color extraction (already present in `Theme.kt`; not
  expanded or made a first-class user setting here).
- Per-screen custom illustrations or bespoke dark artwork.
- Re-tuning the branded palette itself (already WCAG AA; kept as-is).
- Enabling Kover/detekt coverage gates (tracked as a separate follow-up per config).

## Capabilities

### New Capabilities
- `theme-mode`: user-selectable System/Light/Dark appearance, persisted across
  launches and applied app-wide through the canonical `AppTheme` entry point.

### Modified Capabilities
- None. `branded-palette` behavior is preserved; theme mode is a second,
  independent axis layered on top (interaction to be specified in the spec phase).

## Approach

Adopt exploration **Approach 1** (theme-aware `DesignTokens` via CompositionLocal)
over mapping straight onto `MaterialTheme.colorScheme`, to preserve the
hand-tuned branded WCAG AA identity. Sequence the work as: (1) plumbing —
persistence + Settings toggle + un-hack `AppTheme`/`MainActivity` + canonicalize
on `AppTheme`; (2) convert `DesignTokens` into a provided light/dark instance and
migrate top consumers; (3) dark-safe custom-draw and stray-literal sweep. Reuse
the existing `CompositionLocalProvider` pattern already present in `AppTheme`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `ui/theme/AppTheme.kt` | Modified | Remove TEMPORAL hack; read persisted `ThemeMode` + branded/dynamic |
| `ui/theme/DesignTokens.kt` | Modified | `object` → `LocalDesignTokens` light/dark instances (crux) |
| `ui/theme/Theme.kt` | Modified | Retire `MyDataBasesTheme` usage / deprecated alias |
| `MainActivity.kt` | Modified | Consume persisted `ThemeMode` instead of hardcoded SYSTEM |
| `domain/repositories/SettingsRepository.kt` | Modified | Add `observeThemeMode()` / `setThemeMode()` |
| `data/repositories/SettingsRepositoryImpl.kt` | Modified | Add `theme_mode` DataStore key (enum name as string) |
| `ui/screens/settings/SettingsViewModel.kt` | Modified | Expose `themeMode` StateFlow + setter |
| `ui/screens/settings/SettingsScreen.kt` | Modified | Add System/Light/Dark selector |
| `res/values*/strings.xml` (×10) | Modified | Theme-mode labels in all 10 locales |
| ~15 `DesignTokens.` consumers | Modified | Switch to theme-aware token accessor |
| `ui/workspace/WorkspaceCarousel.kt` + IOS components | Modified | Dark-safe shadows/scrims |
| ~15 screens/components | Modified | Stray `Color(0x...)` literal sweep |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `DesignTokens` `object`→provider refactor touches ~15 files; regression risk | High | Keep call-site names stable; isolate in its own PR slice; visual review light+dark |
| Two theme entry points (`MyDataBasesTheme` vs `AppTheme`) land fix in wrong place | Med | Canonicalize on `AppTheme` FIRST (in plumbing slice) before wiring |
| `WorkspaceCarousel` `Color.BLACK` shadow invisible/muddy on dark | High | Explicit dark-aware shadow color/alpha; custom-draw checklist |
| Light-tuned scrim/gradients break on dark bg | Med | Dark-tune tokens; verify each surface in dark |
| Missing a locale for new strings regresses build/UX | Med | android-dev rule: add strings to all 10 `strings.xml` before UI |
| `branded_palette` vs `theme_mode` (2 axes) confusing UX | Med | Spec phase MUST define how the axes interact before design |
| Thin theming/settings test coverage | Med | Add unit tests for persistence + Settings VM under strict TDD |

## Rollback Plan

Work is chainable (see note below). Per slice: revert the slice's commits/PR.
Because theme mode is additive and persistence defaults to `SYSTEM`, reverting
the Settings toggle restores prior behavior with no data migration. The
`DesignTokens` refactor is the only wide change — if regressions surface, revert
that slice independently; the plumbing slice remains valid on its own (dark mode
still flips `colorScheme`-based surfaces, just not `DesignTokens`-driven ones).

## Dependencies

- Existing branded dark/light `ColorScheme`s (`BrandedColors.kt`) — already present.
- DataStore-backed settings stack — already present.
- No new libraries required.

## Chaining Note (proposal-level flag — NOT a final decision)

Exploration rates this **LARGE (~30–40 files)** and recommends splitting into
**3 chained PRs**: (1) plumbing, (2) theme-aware `DesignTokens` + high-density
consumers, (3) custom-draw + stray-literal sweep. Chaining appears warranted to
keep each PR reviewable (< ~400 lines). **The final chain strategy is deferred to
the tasks phase and decided with the user (delivery strategy: ask-always).** This
note only carries the recommendation forward.

## Success Criteria

- [ ] Settings exposes a System/Light/Dark selector; choice persists across launches.
- [ ] `AppTheme` honors the persisted preference (no TEMPORAL hack); `MainActivity`
      no longer hardcodes `ThemeMode.SYSTEM`.
- [ ] `AppTheme` is the single canonical entry point; `MyDataBasesTheme` usages removed.
- [ ] `DesignTokens`-driven screens render correct colors in dark mode.
- [ ] `WorkspaceCarousel` and IOS component shadows/scrims read correctly on dark surfaces.
- [ ] New theme strings present in all 10 locales; no hardcoded UI text introduced.
- [ ] Impact stated for Compact/Medium/Expanded WindowSizeClass (theme is global; no layout change expected).
