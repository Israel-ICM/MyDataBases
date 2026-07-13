# Exploration: Dark Mode support (`dark-mode`)

## Current State

Dark mode is **partially built but NOT wired end-to-end**. The prompt's premise ("no dark mode implementation exists") is inaccurate — the theming scaffolding is substantial. The real work is *finishing and connecting* what exists, plus migrating hardcoded light-only colors.

What already exists:

- **`ThemeMode` enum** (`domain/models/ThemeMode.kt`): `LIGHT | DARK | SYSTEM`. Complete.
- **Two theme entry points**:
  - `Theme.kt` → `MyDataBasesTheme(...)`: full Material 3 `DarkColorScheme` + `LightColorScheme`, honors `isSystemInDarkTheme()`, has dynamic color (Android 12+). Marked partly deprecated; a `MyDataBasesThemeDeprecated` alias points consumers to `AppTheme`.
  - `AppTheme.kt` → `AppTheme(themeMode, content)`: the *intended* main theme. Wires design tokens via `CompositionLocalProvider` and switches `BrandedDark/LightColorScheme` by `darkTheme`.
- **Branded color schemes** (`BrandedColors.kt`): complete `BrandedDarkColorScheme` + `BrandedLightColorScheme`, both Material 3 `ColorScheme`s with WCAG AA contrast noted in docs.
- **Settings stack** (DataStore-backed): `SettingsRepository` (interface) + `SettingsRepositoryImpl` (DataStore), `SettingsViewModel`, `SettingsScreen`, `SettingsModule` (Hilt DI). DataStore is the persistence mechanism.
- **i18n scaffolding**: 10 languages present (`values`, `-es`, `-ar`, `-de`, `-fr`, `-hi`, `-ja`, `-pt-rBR`, `-ru`, `-zh-rCN`) — matches the android-dev skill's required set. Settings strings already exist: `settings_title`, `settings_branded_palette_title`, `settings_branded_palette_description`.

### The three blocking gaps

1. **`AppTheme` ignores user preference.** `AppTheme.kt` lines 67–69 contain a `TEMPORAL` hack that *always* forces branded colors and never reads `SettingsViewModel`. The `themeMode` param is honored for dark/light detection, but the branded-vs-dynamic toggle is dead.
2. **`MainActivity` hardcodes `ThemeMode.SYSTEM`.** `MainActivity.kt` line 58 has `remember { mutableStateOf(ThemeMode.SYSTEM) }` with a comment "será dinámico en PR #2". The theme mode is never persisted or read back — the user's choice has no path to the theme.
3. **Settings has no theme-mode toggle.** `SettingsScreen` + `SettingsViewModel` + `SettingsRepository` only expose a `branded_palette_enabled` boolean. There is **no `ThemeMode` persistence** (no `theme_mode` DataStore key, no repository method, no UI control for system/light/dark).

### The migration debt (the large part)

`DesignTokens.kt` is a **hardcoded light-mode-only** object: every color is a literal `Color(0x...)` (e.g. `SurfacePrimary = Color.White`, `TextPrimary = Color(0xFF1A1F2E)`, gradient/shadow colors) with **zero theme awareness**. It is referenced in **15 files** (≈120 call sites). Because these are `object` vals (not `@Composable`), they cannot read `MaterialTheme.colorScheme` — they are static. Any screen drawing from `DesignTokens` will render light colors even in dark mode.

Engram obs #1933 ("dropdown respeta dark mode vía DesignTokens.SurfacePrimary") is **misleading**: `SurfacePrimary` is `Color.White`, hardcoded. That fix made the dropdown consistent with the (light) token, not dark-aware.

## Affected Areas

### Theme core (small, surgical)
- `ui/theme/AppTheme.kt` — remove the TEMPORAL hack; read branded/dynamic + theme mode from settings.
- `ui/theme/DesignTokens.kt` — **central blocker**. Must become theme-aware (composable accessor or dark/light variants). 18 hardcoded colors.
- `MainActivity.kt` — replace hardcoded `ThemeMode.SYSTEM` with persisted value from `SettingsViewModel`.
- `ui/theme/Theme.kt` / `Color.kt` / `BrandedColors.kt` — already dark-ready; likely no change beyond dedup.

### Settings stack (small)
- `domain/repositories/SettingsRepository.kt` — add `observeThemeMode()` / `setThemeMode()`.
- `data/repositories/SettingsRepositoryImpl.kt` — add `theme_mode` DataStore key (store enum name as string).
- `ui/screens/settings/SettingsViewModel.kt` — expose `themeMode` StateFlow + setter.
- `ui/screens/settings/SettingsScreen.kt` — add system/light/dark selector (segmented/radio).
- `res/values*/strings.xml` (×10) — add theme-mode labels (title, system, light, dark) in all 10 languages.

### Color migration debt (the bulk — 15 `DesignTokens.` consumers)
High-density consumers: `ConnectionsListScreen.kt` (21), `IOSDropdownField.kt` (20), `ConnectionCard.kt` (15), `AddDatabaseScreen.kt` (12), `DatabaseCard.kt` (8), `ScreenTitle.kt` (8), `TableCard.kt` (8), plus `DatabasesListScreen`, `TablesListScreen`, `IOSButton`, `BreathingBackground`, `AdaptiveNavigationScaffold`, `FolderCard`, `IOSDropdownMenu`, `MonitorScreen`.

Separately, **19 files** hold **185 raw `Color(0x...)`** literals; excluding the 4 theme files (`Color.kt` 57, `BrandedColors.kt` 36, `DesignTokens.kt` 18, `DbAccents.kt` 4 = 115 legitimate palette defs), ~**70 stray literals across 15 screens/components** (`ConnectionFormScreen.kt` 23, `DatabaseTypeCard.kt` 9, `IOSSearchBar.kt` 5, `CompletionPopup.kt` 5, several IOS components) are candidates for token/`colorScheme` migration.

### Structural / custom-draw risks
- `ui/workspace/WorkspaceCarousel.kt` — draws a **hand-rolled shadow via `BlurMaskFilter` with `android.graphics.Color.BLACK`** (light-mode assumption). 11 shadow-related refs. Needs dark-aware shadow color/alpha.
- IOS components with `drawBehind`/`.shadow` and hardcoded tints: `IOSDropdownField`, `IOSDropdownMenu`, `IOSCard`, `IOSSearchBar`, `CompletionPopup`, `DraggableCard`, `ErrorCard`.
- `DesignTokens.CardShadowColor`, gradient backgrounds (`BackgroundGradientStart/End`), `BackdropScrim = Color.White.copy(...)` — all light-tuned; scrim on dark bg will look wrong.

### Healthy baseline
**31 files** already use `MaterialTheme.colorScheme` correctly — those need little/no change and prove the app *can* theme cleanly once tokens follow suit.

## Approaches

1. **Make `DesignTokens` theme-aware, minimal churn** — Convert `DesignTokens` into a composable-accessible provider (e.g. `LocalDesignTokens` CompositionLocal supplying a `light`/`dark` instance, or a `@Composable DesignTokens.current`). Keep call-site names stable where possible.
   - Pros: Single source of truth; ~15 consumer files change import/access pattern, not every color; fits existing `CompositionLocalProvider` pattern already in `AppTheme`.
   - Cons: Touches many files; `object` → provided-instance is a mechanical but wide refactor.
   - Effort: **Medium–High**.

2. **Map `DesignTokens` onto `MaterialTheme.colorScheme`** — Replace token color literals with references to `colorScheme` roles, deleting the standalone palette where possible.
   - Pros: Fewest concepts; leverages the 31 already-correct files; dark mode "just works" via ColorScheme.
   - Cons: Some tokens (gradients, accent light/dark shades, shadow color) have no direct M3 role → still need a small theme-aware supplement; risk of subtle visual regressions in the branded look.
   - Effort: **Medium–High**.

3. **Wire settings + theme mode only, defer full migration** — Ship the plumbing (theme-mode persistence, Settings toggle, un-hack `AppTheme`, fix `MainActivity`) and the `WorkspaceCarousel` shadow, but leave `DesignTokens` migration for a follow-up.
   - Pros: Small, shippable first slice; user can toggle dark mode and see `colorScheme`-based surfaces flip.
   - Cons: Screens driven by `DesignTokens` stay light in dark mode → inconsistent/broken dark UX; not a complete feature.
   - Effort: **Small** (but incomplete).

## Recommendation

**Chain it.** Combine Approach 3 as the first PR, then Approach 1 (theme-aware `DesignTokens`) as the substantive second (and possibly third) PR:

- **PR 1 — Plumbing (small):** add `theme_mode` to `SettingsRepository`/Impl (DataStore) + `SettingsViewModel`; add system/light/dark selector to `SettingsScreen` with strings in all 10 languages; un-hack `AppTheme` (read preference); make `MainActivity` consume persisted `ThemeMode`. Deletes/uses the existing `Theme.kt` vs `AppTheme.kt` duplication decision.
- **PR 2 — Theme-aware `DesignTokens` + high-density consumers (medium/large):** introduce `LocalDesignTokens` with dark/light instances; migrate the top consumers (`ConnectionsListScreen`, `IOSDropdownField`, `ConnectionCard`, `AddDatabaseScreen`, cards).
- **PR 3 — Custom-draw + stray literals (medium):** dark-aware `WorkspaceCarousel` shadow; IOS component shadows/scrims; sweep the ~70 stray `Color(0x...)` literals in screens/components.

Prefer Approach 1 over 2 to preserve the deliberate branded identity (the palette was hand-tuned for WCAG AA); mapping straight onto `colorScheme` risks losing that.

## Risks

- **`DesignTokens` is an `object`, not composable** — cannot read theme; the conversion touches ~15 files and is the single biggest source of effort/regression risk.
- **Two competing theme entry points** (`MyDataBasesTheme` vs `AppTheme`) + a deprecated alias — must decide the canonical one before wiring, or the fix lands in the wrong place. `MainActivity` uses `AppTheme`; the `@Preview` still uses `MyDataBasesTheme`.
- **`WorkspaceCarousel` `BlurMaskFilter` uses `Color.BLACK`** — pure black shadow on a dark surface disappears/looks muddy; needs explicit dark treatment. Custom-draw code is easy to miss in a colorScheme sweep.
- **Light-tuned scrim/gradients** (`BackdropScrim = white α`, `BackgroundGradient*`, `CardShadowColor` violet α) will visually break on dark backgrounds.
- **i18n discipline (android-dev skill):** every new theme-toggle string must land in all 10 `strings.xml` files or the build/UX regresses per project rules.
- **Existing `branded_palette` toggle semantics** overlap with theme mode — need a clear UX for how "branded vs dynamic" and "system/light/dark" interact (2 independent axes), or consolidate.
- **No visible tests** for theming/settings beyond the "TDD GREEN" note on the repo impl — regression coverage for the migration is thin.

## Scope Estimate

**LARGE** — warrants chaining into ~3 PRs.

- Plumbing slice: ~7 files (settings stack ×4, `MainActivity`, `AppTheme`, +10 `strings.xml`).
- Token migration: ~15 `DesignTokens` consumers + `DesignTokens.kt` itself.
- Custom-draw/stray-literal sweep: ~15 files.
- **Rough total: ~30–40 files** touched (theme core, settings, 10 locale files, and the color-consuming screens/components).

## Ready for Proposal

**Yes.** The proposal should (1) explicitly correct the "greenfield" premise — this is *finish + migrate*, not build-from-scratch; (2) pick the canonical theme entry point (`AppTheme`); (3) commit to the theme-aware `DesignTokens` strategy (Approach 1); and (4) plan the work as a chained PR set (plumbing → token migration → custom-draw sweep) given the LARGE scope.
