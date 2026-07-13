# Tasks: Dark Mode Finish & Migration (`dark-mode`)

Strict TDD active. RED→GREEN→REFACTOR triads apply to NEW logic only (per design.md); mechanical call-site renames are verification-only, out of TDD scope.

## Review Workload Forecast

Session budget cached at **800** lines (not the shared-skill default of 400); risk assessed against 800 below, 400-default noted where it changes the recommendation.

| Slice | Scope | Est. lines | Files |
|---|---|---|---|
| PR-1 Plumbing | persistence, un-hack `AppTheme`/`MainActivity`, delete `Theme.kt`, 17 `@Preview` sites, 10 locales, selector UI + tests | ~550–650 | ~30 |
| PR-2 DesignTokens | `object`→data class, `LocalDesignTokens`, ~120 sites/15 files | ~400–470 | ~16 |
| PR-3 Custom-draw + literals | Carousel/IOS shadows/scrims, ~70-literal triage, golden test | ~280–360 | ~15 |
| **Total (unchained)** | | **~1230–1480** | **~35–40** |

Single PR not viable (exceeds both budgets). 3-PR split fits under 800 each; PR-1 sits closest to the ceiling. Against the stricter 400 default, every slice is at/over budget — a finer split would break PR-1 into 1a (persistence+VM+selector, ~250) / 1b (entry-point unification, ~350–400), and PR-2 by consumer-file batch.

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: stacked-to-main (resolved — PR-1 targets `master`, PR-2 targets PR-1's branch, PR-3 targets PR-2's branch)
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | PR | Notes |
|------|------|-----|-------|
| 1 | Persistence + un-hacked entry point + preview migration + locales | PR-1 | Base TBD by chosen chain strategy; optionally split 1a/1b for strict-400 |
| 2 | Theme-aware `DesignTokens` + ~15 consumers | PR-2 | Depends on PR-1 (`AppTheme` must provide `LocalDesignTokens`) |
| 3 | Custom-draw dark-safety + literal sweep | PR-3 | Depends on PR-2 (tokens needed for structural fixes) |

## Phase 1 (PR-1): Plumbing & Canonical Entry Point

- [x] 1.1 RED: `SettingsRepositoryImplTest` — `observeThemeMode`/`setThemeMode` round-trip on fake `DataStore<Preferences>`, default `SYSTEM` when unset
- [x] 1.2 GREEN: add `theme_mode` `stringPreferencesKey`, `observeThemeMode()`/`setThemeMode()` to `SettingsRepository.kt` + `SettingsRepositoryImpl.kt`
- [x] 1.3 REFACTOR: align shape with existing `branded_palette` methods
- [x] 1.4 RED: `SettingsViewModelTest` — `themeMode` `StateFlow` + `setThemeMode()` (mockk repo, `kotlinx-coroutines-test`)
- [x] 1.5 GREEN: add `themeMode` `StateFlow` + setter to `SettingsViewModel.kt`, mirroring `brandedPalette`
- [x] 1.6 RED: pure `resolveDarkTheme(mode, systemDark): Boolean` — LIGHT, DARK, SYSTEM×{dark,light} cases
- [x] 1.7 GREEN: implement `resolveDarkTheme`; remove `TEMPORAL` hack in `AppTheme.kt`; wire `AppTheme(themeMode=)` against spec's base×branded table
- [x] 1.8 GREEN: `MainActivity.kt` reads `SettingsViewModel.themeMode`, passes into `AppTheme(themeMode=)` instead of hardcoded `SYSTEM`
- [x] 1.9 Add `theme_mode_label`/`system`/`light`/`dark` strings to all 10 `values*/strings.xml`
- [x] 1.10 RED: `SettingsScreen` Compose UI test — tapping System/Light/Dark invokes `setThemeMode` with expected value
- [x] 1.11 GREEN: add selector (`SingleChoiceSegmentedButtonRow`, fallback `RadioButton` column) to `SettingsScreen.kt` below branded toggle, `stringResource`-only labels
- [x] 1.12 Migrate 17 `@Preview` call sites off `MyDataBasesTheme` → `AppTheme(themeMode = ...)`: `MainActivity`, `ConnectionCard`, `DatabaseCard`, `DatabaseTypeSelector`, `EmptyState`, `ErrorCard`, `HeroConnectionCard`, `SectionCard`, `TableCard`, `ConnectionListSkeleton`, `DatabaseListSkeleton`, `TableListSkeleton`, `TableViewerSkeleton`, `DatabasesListScreen`, `TablesListScreen`, `TableViewerScreen`
- [x] 1.13 Delete `Theme.kt` (`MyDataBasesTheme` + alias + dead non-branded schemes)
- [x] 1.14 Verify: source-tree search for `MyDataBasesTheme` returns zero matches (spec scenario)
- [x] 1.15 Run `./gradlew test` + `./gradlew compileDebugKotlin`; confirm no pre-existing failures

## Phase 2 (PR-2): Theme-Aware Design Tokens

- [x] 2.1 RED: `buildDesignTokens(scheme)` pure fn test — light scheme → light values, dark scheme → dark values
- [x] 2.2 GREEN: `DesignTokens.kt` `object` → `@Immutable data class` + `buildDesignTokens()` + `LightDesignTokens`/`DarkDesignTokens`
- [x] 2.3 GREEN: add `LocalDesignTokens = staticCompositionLocalOf { LightDesignTokens }`
- [x] 2.4 REFACTOR: extract shared role-derivation locals (e.g. `textPrimary`) per design.md interface
- [x] 2.5 GREEN: `AppTheme.kt` provides `LocalDesignTokens` via `CompositionLocalProvider` alongside `MaterialTheme`
- [x] 2.6 Migrate `DesignTokens.X` → `LocalDesignTokens.current.x` (~115 sites) in: `AdaptiveNavigationScaffold`, `BreathingBackground`, `ConnectionCard`, `DatabaseCard`, `FolderCard`, `ScreenTitle`, `TableCard`, `IOSButton`, `IOSDropdownField`, `IOSDropdownMenu`, `ConnectionsListScreen`, `AddDatabaseScreen`, `DatabasesListScreen`, `MonitorScreen`, `TablesListScreen`
- [x] 2.7 In non-composable draw lambdas (`drawBehind`/`Canvas`), capture `LocalDesignTokens.current` to a local `val` first — N/A: none of the 15 migrated files contain `drawBehind`/`Canvas`/`drawWithContent` (verified via grep); all ~115 sites are already inside `@Composable` scope. `WorkspaceCarousel` (the one file with a draw lambda) is explicitly PR-3 scope, untouched.
- [x] 2.8 Manual/visual: screenshot check light+dark for each of the 15 migrated files — NOT performed (no emulator/device available in this session); compile + unit test verification done instead. Flagged as a residual manual-QA gap for a human reviewer before merge.
- [x] 2.9 Run `./gradlew test` + `./gradlew compileDebugKotlin`; confirm no pre-existing failures

## Phase 3 (PR-3): Custom-Draw Dark-Safety & Literal Sweep

- [ ] 3.1 GREEN: `WorkspaceCarousel.kt` `BlurMaskFilter` shadow — `Color.BLACK` → `colorScheme.onSurface`-derived tint
- [ ] 3.2 Dark-tune IOS shadows/scrims: `IOSDropdownField`, `IOSDropdownMenu`, `IOSButton`, `IOSCard`, `IOSSearchBar`, `CompletionPopup`
- [ ] 3.3 Dark-tune `BackdropScrim`, `BackgroundGradient*`, `CardShadowColor` usages
- [ ] 3.4 Triage ~70 stray `Color(0x...)` literals: structural (backgrounds/surfaces/scrims/body text) → fix onto tokens/`colorScheme`; decorative (low-alpha accents/glows) → defer with `// decorative, deferred: dark-mode` comment
- [ ] 3.5 RED: golden/smoke test — `WorkspaceCarousel` shadow visible on dark surface (one test, per design.md budget)
- [ ] 3.6 GREEN: confirm smoke test passes against 3.1
- [ ] 3.7 Manual/visual: verify light theme unaffected across all touched components (spec scenario)
- [ ] 3.8 Run `./gradlew test` + `./gradlew assembleDebug`; confirm no regressions
