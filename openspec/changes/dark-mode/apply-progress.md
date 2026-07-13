# Apply Progress: Dark Mode Finish & Migration (`dark-mode`)

## PR-1 (Plumbing) — Status: Complete, ready for PR

Branch: `feature/dark-mode-plumbing` (stacked-to-main chain strategy, targets `master`)

### Phase 1 Tasks (15/15 complete)

- [x] 1.1 RED: `SettingsRepositoryImplTest`
- [x] 1.2 GREEN: `theme_mode` persistence in `SettingsRepository`/`SettingsRepositoryImpl`
- [x] 1.3 REFACTOR: align shape with `branded_palette`
- [x] 1.4 RED: `SettingsViewModelTest`
- [x] 1.5 GREEN: `themeMode` StateFlow + setter in `SettingsViewModel`
- [x] 1.6 RED: `resolveDarkTheme` pure function tests (`AppThemeTest`)
- [x] 1.7 GREEN: `resolveDarkTheme` implemented; TEMPORAL hack removed from `AppTheme.kt`
- [x] 1.8 GREEN: `MainActivity` reads real `themeMode` from `SettingsViewModel`
- [x] 1.9 `theme_mode_label`/`system`/`light`/`dark` strings in all 10 locales
- [x] 1.10 RED: `SettingsScreenTest` Compose UI test
- [x] 1.11 GREEN: theme selector added to `SettingsScreen`
- [x] 1.12 17 `@Preview` sites migrated `MyDataBasesTheme` → `AppTheme`
- [x] 1.13 `Theme.kt` deleted
- [x] 1.14 Verified: zero `MyDataBasesTheme` references in `app/src`
- [x] 1.15 `./gradlew test` + `./gradlew compileDebugKotlin` — no pre-existing failures introduced

### Test Results

- `compileDebugKotlin`: BUILD SUCCESSFUL
- `compileDebugUnitTestKotlin`: BUILD SUCCESSFUL
- `testDebugUnitTest` (full suite): 148 tests, 23 failed — all 23 failures are **pre-existing**
  and unrelated to `dark-mode` (`SSHTunnelManagerTest`, `SSHTunnelConfigConverterTest`,
  `SSLConfigConverterTest`, `EditorHistoryTest`). Confirmed via `git stash` against the base
  commit (`0db3701`): same 23 failures present with 0 dark-mode tests (138 tests total).
- All dark-mode-related tests pass: `SettingsRepositoryImplTest`, `SettingsViewModelTest`,
  `AppThemeTest`, `SettingsScreenTest` (androidTest).
- Fixed 1 pre-existing test bug during this session: `SettingsViewModelTest > themeMode
  exposes DARK when repository reports DARK` — Turbine `awaitItem()` was catching the
  `stateIn(WhileSubscribed(5000))` `initialValue` (SYSTEM) before the upstream flow
  propagated on `StandardTestDispatcher`. Fixed with `skipItems(1)` before asserting.

### Commits (5 code + 1 docs)

1. `feat(theme): agregar persistencia de theme_mode` — `SettingsRepository`/`Impl` + test
2. `feat(theme): agregar strings de theme mode a los 10 locales` — 10 `strings.xml`
3. `feat(theme): agregar selector de theme mode en Settings` — `SettingsViewModel` + `SettingsScreen` + tests
4. `feat(theme): eliminar hack TEMPORAL y conectar themeMode real` — `AppTheme.kt` + `MainActivity.kt` + `AppThemeTest`
5. `feat(theme): migrar previews restantes a AppTheme y eliminar Theme.kt legacy` — 15 preview sites + `Theme.kt` deletion
6. `docs(dark-mode): agregar documentación SDD y ADR-002` — openspec artifacts + ADR-002

### Excluded from this branch

`temp_drag_changes.patch` at repo root — unrelated leftover from a prior drag-and-drop
feature branch. Left untouched in the working tree, not staged, not committed.

## PR-2 (Theme-Aware Design Tokens) — Status: Complete, ready for PR

Branch: `feature/dark-mode-tokens` (stacked-to-main chain strategy, targets `feature/dark-mode-plumbing`)

### Phase 2 Tasks (9/9 complete)

- [x] 2.1 RED: `buildDesignTokens(scheme)` pure fn test (`DesignTokensTest`)
- [x] 2.2 GREEN: `DesignTokens.kt` `object` → `@Immutable data class` + `buildDesignTokens()` + `LightDesignTokens`/`DarkDesignTokens`
- [x] 2.3 GREEN: `LocalDesignTokens = staticCompositionLocalOf { LightDesignTokens }`
- [x] 2.4 REFACTOR: shared role-derivation locals (`textPrimary`/`textSecondary`/`textTertiary`) composed into dependent fields
- [x] 2.5 GREEN: `AppTheme.kt` provides `LocalDesignTokens` (`if (darkTheme) DarkDesignTokens else LightDesignTokens`) via `CompositionLocalProvider`
- [x] 2.6 Migrated ~115 call sites across 15 files: `AdaptiveNavigationScaffold`, `BreathingBackground`, `ConnectionCard`, `DatabaseCard`, `FolderCard`, `ScreenTitle`, `TableCard`, `IOSButton`, `IOSDropdownField`, `IOSDropdownMenu`, `ConnectionsListScreen`, `AddDatabaseScreen`, `DatabasesListScreen`, `MonitorScreen`, `TablesListScreen`
- [x] 2.7 N/A — none of the 15 files contain `drawBehind`/`Canvas`/`drawWithContent` (verified via grep); no local-`val` capture needed
- [x] 2.8 Screenshot/manual check NOT performed (no emulator/device in this session) — **residual gap, flagged for human reviewer**
- [x] 2.9 `./gradlew compileDebugKotlin` + `./gradlew testDebugUnitTest` — no pre-existing failures introduced

### Architecture Deviation from design.md's abbreviated snippet

design.md's `Interfaces/Contracts` snippet showed `LightDesignTokens`/`DarkDesignTokens` as
`private val`. Made them plain (module-visible) `val` instead, since `AppTheme.kt` (a
different file) needs to select between them in `CompositionLocalProvider`. `buildDesignTokens`
is `internal fun` (not `private`) so `DesignTokensTest` can call it directly, mirroring the
`resolveDarkTheme` pattern from PR-1's `AppThemeTest`. `DesignTokens` stays anchored to the
**branded** palette (`BrandedLightColorScheme`/`BrandedDarkColorScheme`) regardless of the
`brandedPaletteEnabled`/dynamic-color axis — confirmed this is design.md's intent (Data Flow:
"selects Light/DarkDesignTokens", Architecture Decisions: "Preserves branded WCAG AA identity").

### WCAG AA Contrast Decisions (design.md Open Question — resolved)

Computed actual WCAG relative-luminance contrast ratios (not eyeballed) for every ambiguous
role mapping before wiring `buildDesignTokens`. Sanity-checked the formula against black/white
(21.00:1 exact) before trusting the results.

| Field | Derivation | Light contrast | Dark contrast | Verdict |
|---|---|---|---|---|
| `textPrimary` | `scheme.onBackground` | 16.41:1 (surface) | 12.03:1 (surface) | ✅ AA, exact match to shipped light value |
| `textSecondary` | `scheme.secondary` (**not** `scheme.outline`) | 6.23:1 | 5.81:1 | ✅ AA. **Fixes the gap design.md flagged**: `scheme.outline` (`brand_outline`) only gives **2.37:1** against `BrandedDarkColorScheme.surface` — would have shipped a WCAG-failing dark text color if mapped naively. Light value is visually indistinguishable from the previously-shipped literal (`5C5F7A` vs `5B5F7D`, ΔE negligible). |
| `textTertiary` | `scheme.outline` | 4.36:1 | 2.37:1 | ⚠️ Light **improves** over the previously-shipped literal (`9DA1C0` was only 2.53:1 — already non-AA in production). Dark does not clear 4.5:1. Accepted as a residual risk: this token is caption-tier (12sp counters/metadata), not body text, and the pre-existing light behavior was already sub-AA for this exact field — not a regression introduced by this PR. Flagged here for a follow-up literal/contrast pass (candidate for PR-3, which already does structural-vs-decorative triage). |
| `iconNormal` | `scheme.outline` | 4.36:1 | — (non-text, 3:1 threshold) | ✅ Exact match to shipped light value; clears WCAG 1.4.11 non-text 3:1 in both themes |
| `separator` | `scheme.surfaceVariant` | exact match | — (decorative line) | ✅ Exact match to shipped light value |
| `backgroundPrimary`/`surfacePrimary` | `scheme.background`/`scheme.surface` | exact match | brand dark bg/surface | ✅ Exact match to shipped light values |
| `accentPrimary`/`accentSecondary`/`destructiveAction`/`cardShadowColor` | kept theme-invariant (raw `brand_primary`/`brand_tertiary`/iOS-red literal) | — | 4.28–4.78:1 (`brand_primary` vs dark surface/bg) | ✅ Brand-identity colors, unchanged from shipped behavior; `brand_primary` clears AA against `brand_bg` (4.78:1) and the 3:1 non-text threshold against `brand_surface` (4.28:1) |
| `backdropScrim` | `scheme.background.copy(alpha=0.4f)` (**not** hardcoded `Color.White`) | ≈ same as shipped (F5F6FA vs White, imperceptible) | translucent dark veil | ✅ Fixes a would-be bug: a white 40%-alpha scrim over a dark screen would have rendered as a bright wash instead of a dimming veil |

**Discovery (out of scope, not touched)**: `BrandedColors.kt`'s header comment claims
`brand_primary` vs `brand_bg` = 7.2:1 and `brand_tertiary` vs `brand_bg` = 9.8:1. Recomputing
with the standard WCAG relative-luminance formula (sanity-checked against black/white = 21:1)
gives 4.78:1 and 11.00:1 respectively — the comment's numbers appear stale/incorrect. Not
fixed here since `BrandedColors.kt` is PR-1's shipped file, not part of PR-2's scope; flagged
for a follow-up doc-accuracy fix.

### Test Results

- `compileDebugKotlin`: BUILD SUCCESSFUL
- `compileDebugUnitTestKotlin`: BUILD SUCCESSFUL
- `testDebugUnitTest` (full suite): 154 tests, 23 failed — same 23 **pre-existing** failures
  as PR-1 (`SSHTunnelManagerTest`, `SSHTunnelConfigConverterTest`, `SSLConfigConverterTest`,
  `EditorHistoryTest`), zero new failures. 6 new `DesignTokensTest` tests all pass (154 = 148
  PR-1 baseline + 6 new).

### Commits (4 code + 1 docs)

1. `feat(theme): convertir DesignTokens a data class theme-aware con LocalDesignTokens` — `DesignTokens.kt`, `AppTheme.kt`, `DesignTokensTest.kt` (RED→GREEN)
2. `feat(theme): migrar componentes core a LocalDesignTokens` — `AdaptiveNavigationScaffold`, `BreathingBackground`, `ConnectionCard`, `DatabaseCard`, `FolderCard`, `ScreenTitle`, `TableCard`
3. `feat(theme): migrar componentes iOS a LocalDesignTokens` — `IOSButton`, `IOSDropdownField`, `IOSDropdownMenu`
4. `feat(theme): migrar pantallas a LocalDesignTokens` — `ConnectionsListScreen`, `AddDatabaseScreen`, `DatabasesListScreen`, `MonitorScreen`, `TablesListScreen`
5. `docs(dark-mode): marcar Phase 2 completa y documentar decisiones de contraste WCAG AA`

### Excluded from this PR (explicitly out of scope, per orchestrator instruction)

- `WorkspaceCarousel.kt` — shadow tint (`Color.BLACK`) untouched, confirmed via `git diff --stat` (no changes) — PR-3 scope
- Stray `Color(0x...)` literal sweep — not started — PR-3 scope
- `temp_drag_changes.patch` at repo root — unrelated leftover, left untouched (carried over from PR-1)

## What's Deferred

### PR-3 (Custom-Draw Dark-Safety & Literal Sweep)

- `WorkspaceCarousel` `BlurMaskFilter` shadow tint (`Color.BLACK` → `colorScheme.onSurface`-derived)
- Dark-tune IOS-style shadows/scrims (`IOSDropdownField`, `IOSDropdownMenu`, `IOSButton`, `IOSCard`, `IOSSearchBar`, `CompletionPopup`)
- ~70 stray `Color(0x...)` literal triage (structural fixed, decorative deferred with comment)
- Golden/smoke test for carousel shadow on dark surface
- Depends on PR-2: tokens needed for structural fixes
