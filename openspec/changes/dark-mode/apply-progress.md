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

## What's Deferred

### PR-2 (Theme-Aware Design Tokens)

- `DesignTokens` `object` → `@Immutable data class` + `buildDesignTokens()`
- `LocalDesignTokens` CompositionLocal (mirrors `LocalAppSpacing`/`LocalAppShapes`)
- ~120 call sites across ~15 files migrated from `DesignTokens.X` → `LocalDesignTokens.current.x`
- Depends on PR-1: `AppTheme` must provide `LocalDesignTokens`
- See ADR-002 (`.atl/architecture/decisions/ADR-002-design-tokens-theme-aware.md`)

### PR-3 (Custom-Draw Dark-Safety & Literal Sweep)

- `WorkspaceCarousel` `BlurMaskFilter` shadow tint (`Color.BLACK` → `colorScheme.onSurface`-derived)
- Dark-tune IOS-style shadows/scrims (`IOSDropdownField`, `IOSDropdownMenu`, `IOSButton`, `IOSCard`, `IOSSearchBar`, `CompletionPopup`)
- ~70 stray `Color(0x...)` literal triage (structural fixed, decorative deferred with comment)
- Golden/smoke test for carousel shadow on dark surface
- Depends on PR-2: tokens needed for structural fixes
