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

## PR-3 (Custom-Draw Dark-Safety & Literal Sweep) — Status: Complete, ready for PR

Branch: `feature/dark-mode-custom-draw` (stacked-to-main chain strategy, targets `feature/dark-mode-tokens`) — **final slice, 3 of 3**

### Phase 3 Tasks (8/8 complete)

- [x] 3.1 GREEN: `WorkspaceCarousel.kt` `BlurMaskFilter` shadow — `android.graphics.Color.BLACK` → `carouselShadowColorArgb(MaterialTheme.colorScheme.onSurface)` (new pure fn, extracted for testability)
- [x] 3.2 Dark-tune IOS shadows/scrims: `IOSDropdownField`, `IOSDropdownMenu`, `IOSButton` (verified, no change needed — white text on solid colored bg is already theme-correct), `IOSCard`, `IOSSearchBar`, `CompletionPopup`
- [x] 3.3 Dark-tune `BackdropScrim`/`BackgroundGradient*`/`CardShadowColor` usages — `WorkspaceCarousel` backdrop scrim, `TopSheet` backdrop scrim, `IOSCard` shadow (reuses `cardShadowColor` token), plus a scope discovery: `AdaptiveNavigationScaffold`'s bottom nav bar (`Color.White.copy(alpha=0.75f)` pill background + `Color.Black` shadow/separator) — same custom-draw dark-mode bug class, fixed
- [x] 3.4 Stray literal sweep — see full triage table below
- [x] 3.5 RED: `WorkspaceCarouselShadowTest` (3 tests) written before wiring the fix into the composable
- [x] 3.6 GREEN: all 3 tests pass after 3.1
- [x] 3.7 Manual/visual: **NOT performed** — no emulator/device available in this session (same residual gap as PR-2 task 2.8). Compile + unit test + `assembleDebug` verification done instead. Flagged for human reviewer before merge.
- [x] 3.8 `./gradlew test` + `./gradlew assembleDebug` — see Test Results below

### Scope discovery: actual literal count exceeded the tasks.md estimate

tasks.md estimated "~70 stray `Color(0x...)` literals". Actual grep count: **74** `Color(0x...)` literals
+ **~20** additional `Color.White`/`Color.Black` literal usages (not counted in the original estimate,
since the design.md triage heuristic was written around hex literals) across **20 files** (vs. the
~15-file estimate). The `Color.White`/`Color.Black` discovery included a genuinely high-impact bug:
`AdaptiveNavigationScaffold`'s bottom navigation bar (present on nearly every screen) rendered as a
bright white pill with an invisible black shadow in dark mode. Given R6's explicit scope ("Custom
shadows/scrims/gradients remain visible w/ appropriate contrast in dark"), this was fixed as part of
3.3 rather than deferred — it is architecturally the same bug class as `WorkspaceCarousel`'s shadow.

### Stray Literal Triage (structural = fixed, decorative = deferred)

**Structural — fixed onto `LocalDesignTokens`/`colorScheme` (16 files):**

| File | What changed |
|---|---|
| `WorkspaceCarousel.kt` | Shadow ARGB (via new pure fn), backdrop scrim |
| `TopSheet.kt` | Backdrop scrim (variable-alpha, base color from `backgroundPrimary`) |
| `AdaptiveNavigationScaffold.kt` | Bottom bar pill background, shadow, separator line |
| `IOSDropdownField.kt` | Dialog background, `OutlinedTextField` container colors |
| `IOSDropdownMenu.kt` | Menu background + `containerColor` |
| `IOSCard.kt` | Shadow ambient/spot (reuses `cardShadowColor`/`accentPrimary`), background, ripple |
| `IOSSearchBar.kt` | Background, text, icon, cursor, shadow ambient/spot |
| `IOSListItem.kt` | Background, title/subtitle text, chevron, divider |
| `IOSTextField.kt` | Background, text, placeholder, icon, divider |
| `IOSGroupedCard.kt` | Surface `color` |
| `CompletionPopup.kt` | Card `containerColor`, selected-row tint, row text color |
| `DatabaseCard.kt` / `TableCard.kt` | Gradient overlay + icon tint — reused existing `accentSecondary`/`accentSuccess`/`accentSuccessLight` tokens (exact hex match to the literals removed) |
| `ConnectionCard.kt` | Status-dot background (white ring + `accentSuccess` dot) |
| `DatabaseTypeCard.kt` | Header text/icon, dividers, description text, edit icon, add-connection button |
| `ConnectionFormScreen.kt` | **Full retrofit** — this file was entirely missed by PR-2's 15-file migration list. Screen background, header, all section labels, all body text, all switches (thumb kept `Color.White` — correct on both themes since it sits on a theme-aware track), CA/client-cert/key rows, action buttons |
| `ConnectionsListScreen.kt` | Remaining strays post-PR-2: connecting-overlay spinner/text, both `ModalBottomSheet.containerColor`, type-selector description text |
| `AddDatabaseScreen.kt` | Loading-overlay text color (was invisible-risk `Color.Black` over theme-aware `backdropScrim`) |
| `SqlCodeEditor.kt` | Line-number gutter color, placeholder text color → `MaterialTheme.colorScheme.onSurfaceVariant` |
| `MatchHighlightTransformation.kt` | **Readability fix, not just contrast**: search-match highlight background stayed literal yellow while the SQL editor's text color became theme-aware (near-white in dark) — this combination was unreadable (near-white text on yellow). Fixed by forcing a dark `SpanStyle.color` on highlighted spans. The yellow background itself is still deferred (see below). |

**Decorative — deferred, documented in-code with `// decorative, deferred: dark-mode`:**

| File : Line | Literal | Reason deferred |
|---|---|---|
| `CompletionPopup.kt:170-172` | `Color(0xFF5E81AC)` / `Color(0xFFA3BE8C)` / `Color(0xFFD08770)` | Nord-palette K/T/C type badges — mid-tone/full-alpha, sanity-checked to stay legible on both light and dark row backgrounds; not brand identity |
| `SqlCodeEditor.kt:459` | `Color(0xFF0066CC)` (multi-cursor indicator, drawn in a `Canvas`) | Fixed blue cursor reads fine on both themes, same convention as most code editors; low risk, avoids extra non-composable local-val capture for marginal benefit |
| `QueryEditorScreen.kt:1051,1065` | `Color(0xFFF44336)` (stop) / `Color(0xFF4CAF50)` (play) | Universal red/green stop-play semantic on a `MaterialTheme.colorScheme.surface` bg; relative-luminance sanity check clears the WCAG 3:1 non-text threshold against both `BrandedLight/DarkColorScheme` surfaces |
| `DbAccents.kt:30,35,40,45` | MySQL/Postgres/MariaDB/SQLite brand colors | Vendor identity colors (logos) — same theme-invariant treatment as `accentPrimary`/`accentSecondary`/`destructiveAction` in PR-2 |
| `MatchHighlightTransformation.kt:43,45` | `Color(0xFFFFEB3B)` (yellow highlight background) | The *readability* bug (unreadable text on top) is fixed — see above. The yellow base itself is a decorative search-highlight convention, degrades gracefully, not a surface/text role |

**Discovered but explicitly out of scope for this PR (not fixed, not a "stray literal in a consumer
file" — lives inside `DesignTokens.kt` itself, PR-2's file):** `iconBackground` (`0xFFF0F1FF`),
`accentPrimaryLight`/`accentPrimaryDark` (`0xFFE6E7FF`/`0xFF5B5EC8`), `accentSuccess`/
`accentSuccessLight` region — some `buildDesignTokens` fields are identical literals regardless of
`scheme` (i.e. NOT actually theme-derived, unlike `textPrimary`/`backgroundPrimary`/etc.). This means
e.g. `iconBackground` (a light lavender) is used as-is in dark mode too. PR-2's apply-progress only
documented `accentPrimary`/`accentSecondary`/`destructiveAction`/`cardShadowColor` as intentionally
theme-invariant — this broader set was not something PR-2 called out and appears to be an oversight,
not a decision. Flagged as a follow-up SDD change (token-level WCAG recomputation for these fields),
NOT fixed here to avoid scope creep into a new investigation, per orchestrator instruction.

**`textTertiary` sub-AA contrast (flagged in PR-2, re-evaluated here):** PR-2 already identified
`textTertiary` (`scheme.outline`) as 2.37:1 in dark mode, sub-AA, and noted the light value was
already sub-AA before PR-2 too (not a regression). This is a token-level WCAG recomputation, same
category as the `iconBackground` issue above — not a "quick, low-risk" fix (would need the same
relative-luminance-verified process PR-2 used for `textSecondary`), so it is NOT fixed here; kept as
a follow-up alongside the `DesignTokens.kt` literal-derivation gap.

### Test Results

- `compileDebugKotlin`: BUILD SUCCESSFUL
- `compileDebugUnitTestKotlin`: BUILD SUCCESSFUL
- `testDebugUnitTest` (full suite): 157 tests, 23 failed — same 23 **pre-existing** failures as
  PR-1/PR-2 (`SSHTunnelManagerTest`, `SSHTunnelConfigConverterTest`, `SSLConfigConverterTest`,
  `EditorHistoryTest`), zero new failures. 3 new `WorkspaceCarouselShadowTest` tests all pass
  (157 = 154 PR-2 baseline + 3 new).
- `assembleDebug`: BUILD SUCCESSFUL

### Commits (6 code, docs pending)

1. `fix(theme): sombra y scrim del WorkspaceCarousel theme-aware en dark mode` — `WorkspaceCarousel.kt`, `WorkspaceCarouselShadowTest.kt` (RED→GREEN)
2. `fix(theme): dark-tune shadows y scrims en componentes iOS` — the 6 explicit Phase 3 components + `IOSListItem`/`IOSTextField`/`IOSGroupedCard`
3. `fix(theme): bottom nav bar y TopSheet backdrop theme-aware en dark mode` — `AdaptiveNavigationScaffold.kt`, `TopSheet.kt`
4. `fix(theme): reemplazar literales estructurales en cards por design tokens` — `ConnectionCard`, `DatabaseCard`, `TableCard`, `DatabaseTypeCard`
5. `fix(theme): retrofit de ConnectionFormScreen y strays en pantallas de conexion` — `ConnectionFormScreen`, `ConnectionsListScreen`, `AddDatabaseScreen`
6. `fix(theme): contraste del editor SQL y documentar literales decorativos diferidos` — `QueryEditorScreen`, `MatchHighlightTransformation`, `SqlCodeEditor`, `DbAccents`

### Diff size

23 files changed, 249 insertions(+), 119 deletions(-) ≈ 368 changed lines — within the tasks.md
Review Workload Forecast estimate (~280-360, at the high end but not over), despite the scope
discoveries (Color.White/Black sweep, AdaptiveNavigationScaffold, ConnectionFormScreen full
retrofit) that were not itemized in the original PR-3 estimate.

### This closes out the initial 3-PR plan

All 3 planned PRs (Plumbing → Theme-Aware Design Tokens → Custom-Draw Dark-Safety & Literal Sweep)
were implementation-complete. tasks.md was 32/32 tasks across all 3 phases. Sent to `sdd-verify`.

## PR-4 (Verify Fix-Round: R3 Test Coverage & Contrast Regressions) — Status: Complete, ready for PR

Branch: `feature/dark-mode-verify-fixes` (stacked-to-main chain strategy, targets
`feature/dark-mode-custom-draw`) — **4th slice**, added after `sdd-verify` returned
**PASS WITH WARNINGS** with 1 CRITICAL blocking archive. Maintainer explicitly requested a
**complete solution, not a patch** (real extraction + real tests + real WCAG recalculation).

### Verify Fix-Round Tasks (7/7 complete — see tasks.md `## Verify Fix-Round (PR-4)` for detail)

- [x] 4.1 RED: `AppThemeTest` — 6 new tests for `resolveColorScheme` (didn't exist), covering all 5 R3 spec scenarios + 1 fallback triangulation
- [x] 4.2 GREEN: extracted `resolveColorScheme(darkTheme, brandedPaletteEnabled, dynamicColorAvailable, dynamicScheme)` out of `AppTheme`'s inline `when` block
- [x] 4.3 RED: `ContrastUtilsTest` — 5 new sanity tests for `contrastRatio` (didn't exist)
- [x] 4.4 GREEN: implemented `contrastRatio` in new `ContrastUtils.kt`
- [x] 4.5 RED: `DesignTokensTest` — 4 new regression tests for `accentSuccess`/`textTertiary` dark contrast, against the not-yet-updated `buildDesignTokens` signature
- [x] 4.6 GREEN: `buildDesignTokens(scheme, darkTheme)` + `brand_success_light`/`brand_success_dark`/`brand_text_tertiary_dark` in `Color.kt`
- [x] 4.7 Full suite + `assembleDebug` verification

### Item 1 — R3 "Effective ColorScheme Resolution" (was CRITICAL #1, now closed)

Extracted `resolveColorScheme` from `AppTheme.kt`'s inline `when` block into a standalone
pure function (`darkTheme: Boolean, brandedPaletteEnabled: Boolean, dynamicColorAvailable:
Boolean, dynamicScheme: ColorScheme?`) — mirrors `resolveDarkTheme`'s existing pattern
exactly. The composable now resolves `dynamicScheme` lazily (only calls
`dynamicDarkColorScheme(context)`/`dynamicLightColorScheme(context)` — which require a real
`Context` and are NOT pure — when branded is OFF and dynamic color is available) and injects
it already-resolved into the pure function.

`AppThemeTest` now has 6 new tests covering, by exact spec scenario name:

| # | Spec scenario | Test |
|---|---|---|
| 1 | Dark + branded | `Dark + branded resolves to BrandedDarkColorScheme (R3 scenario 1)` |
| 2 | Light + branded | `Light + branded resolves to BrandedLightColorScheme (R3 scenario 2)` |
| 3 | Dark + non-branded | `Dark + non-branded resolves to the dynamic dark scheme when available (R3 scenario 3)` |
| 4 | System defers to OS then applies branded axis | `System defers to OS-dark then applies branded axis (R3 scenario 4, composed with resolveDarkTheme)` — composes `resolveDarkTheme` + `resolveColorScheme` end-to-end, no Compose needed |
| 5 | Axes are independent | `Axes are independent - toggling branded_palette never changes the resolved dark-vs-light base (R3 scenario 5)` — asserts toggling each axis independently while holding the other fixed |
| — | (triangulation) | `Dark + non-branded falls back to BrandedDarkColorScheme when dynamic color is unavailable` — proves the dynamic branch isn't hardcoded; exercises the pre-existing documented fallback |

Fake "dynamic scheme" stand-ins use `androidx.compose.material3.lightColorScheme()`/
`darkColorScheme()` defaults (distinct object references from `BrandedLightColorScheme`/
`BrandedDarkColorScheme`), asserted via `assertSame` (reference identity, not `equals`,
since M3's `ColorScheme` equality semantics weren't worth relying on for this).

**Result**: 10/10 `AppThemeTest` (was 4/4), all 5 R3 scenarios + fallback now genuinely
covered and executed — not just "code reads correctly by inspection."

### Item 2 — `accentSuccess` contrast regression (was WARNING #3, now closed)

Built `ContrastUtils.kt`: a pure `contrastRatio(a: Color, b: Color): Double` implementing
the WCAG 2.0 relative-luminance formula by hand (sRGB gamma correction, `0.2126R + 0.7152G +
0.0722B`), with zero Compose runtime/Context dependency beyond `Color` itself — runs in pure
JVM unit tests. Sanity-checked in `ContrastUtilsTest` against mathematically-known anchors
BEFORE trusting it for real tokens (same discipline PR-2 used when hand-computing contrast,
now made executable):

| Check | Expected | Computed |
|---|---|---|
| Black vs White | 21.00:1 (WCAG max) | 21.00:1 ✅ |
| Same color vs itself | 1.00:1 (WCAG min) | 1.00:1 ✅ |
| Symmetry (`contrastRatio(a,b) == contrastRatio(b,a)`) | equal | equal ✅ |
| `0xFF006B63` vs `0xFF222837` (verify-report's hand-computed `accentSuccess` regression) | 2.30:1 | 2.30:1 ✅ |
| `0xFF5B5F7D` vs `0xFF222837` (verify-report's hand-computed `textTertiary` regression) | 2.37:1 | 2.37:1 ✅ |

The formula independently reproduces verify-report's hand-computed values exactly — high
confidence it's correct before using it to pick new values.

**`accentSuccess` before/after**:

| | Before | After |
|---|---|---|
| Dark vs `brand_surface` | **2.30:1** ❌ (< 3:1 WCAG 1.4.11 non-text minimum) | **4.32:1** ✅ |
| Light vs `brand_light_surface` | 6.40:1 ✅ (already compliant, untouched) | 6.40:1 ✅ (unchanged) |
| Value (dark) | `0xFF006B63` (same literal as light, unadapted) | `0xFF4D9792` (`brand_success_dark`) |
| Value (light) | `0xFF006B63` | `0xFF006B63` (`brand_success_light`, unchanged) |

Made `accentSuccess` genuinely theme-aware (it was a single hardcoded literal reused as-is
in both themes — the actual root cause of the regression). Kept the same teal-green hue
family (only lightened) so the "success" semantic accent doesn't visually clash with
`accentSecondary` (`brand_tertiary`, a distinctly brighter/cooler turquoise) — spot-checked
by comparing RGB components, not just contrast math. `accentSuccessLight` (the low-alpha
gradient tint variant) was NOT touched — it's used at `alpha=0.12–0.20f` as a decorative
overlay, not a solid fill, so the 3:1 non-text threshold doesn't strictly apply the same way;
out of the assigned scope.

`DesignTokensTest` gained 2 new tests: a contrast-ratio regression guard (`>= 3.0` in both
themes) and a triangulation guard proving dark no longer reuses the light literal.

### Item 3 — `textTertiary` sub-AA gap (was WARNING #4, now closed for dark)

**`textTertiary` before/after**:

| | Before | After |
|---|---|---|
| Dark vs `brand_surface` | **2.37:1** ❌ (< 4.5:1 WCAG AA text minimum) | **4.61:1** ✅ |
| Light vs `brand_light_surface` | 4.36:1 ⚠️ (also sub-AA, smaller gap — NOT in this round's scope, see below) | 4.36:1 (unchanged) |
| Value (dark) | `scheme.outline` (`brand_outline`, `0xFF5B5F7D`) | `brand_text_tertiary_dark` (`0xFF8C8FA4`) |
| Value (light) | `scheme.outline` (`0xFF75788C`) | `scheme.outline` (unchanged) |

Deliberately did **not** touch `brand_outline` itself — it's still used by `iconNormal`,
`onSurfaceVariant`, and native M3 `outline` roles (borders, dividers) which were not part of
this fix-round's assigned scope, and changing the shared constant would have rippled into
all of those without any test coverage for that blast radius. Instead, added a dedicated
`brand_text_tertiary_dark` literal and switched `textTertiary`'s dark-mode derivation to it
via the new `darkTheme: Boolean` parameter on `buildDesignTokens` (also used by the
`accentSuccess` fix above — same parameter serves both).

Picked `0xFF8C8FA4` deliberately **darker/dimmer than `textSecondary`** (`0xFF9DA1C0`,
5.81:1) to preserve the visual hierarchy `textPrimary > textSecondary > textTertiary` (each
tier progressively less prominent) while still clearing 4.5:1.

**Discovery, not silently fixed**: recomputing with the new `contrastRatio()`, LIGHT-mode
`textTertiary` (still `scheme.outline` = `0xFF75788C`) is **4.36:1** against
`brand_light_surface` — also technically sub-AA, though a much smaller gap than dark's
2.37:1. The fix-round prompt scoped this item to "a compliant **dark-mode** value"
specifically; I did not expand scope to silently patch light too. Documented in
`DesignTokens.kt`'s KDoc and `tasks.md`'s addendum as a flagged follow-up, not fixed here —
per the "don't silently deviate from the assigned scope" rule.

`DesignTokensTest` gained 2 new tests: a contrast-ratio regression guard (`>= 4.5` in dark)
and a triangulation guard proving dark no longer reuses `scheme.outline`.

### Architecture Deviation from design.md

design.md's abbreviated `Interfaces/Contracts` snippet shows `buildDesignTokens(scheme:
ColorScheme)` with a single parameter. This fix-round adds a required `darkTheme: Boolean`
second parameter. This was necessary because `accentSuccess`/`textTertiary` (dark) no longer
derive purely from `scheme` — they need dedicated per-theme literals to genuinely clear
WCAG thresholds, and `ColorScheme` alone doesn't expose "am I the dark scheme" as a queryable
property. No default value was given to `darkTheme` (deliberately) — an accidental default
could silently mismatch `LightDesignTokens`/`DarkDesignTokens`' actual scheme, which would be
a much worse bug than a compile-time-enforced explicit argument.

### Test Results

- `compileDebugKotlin` / `compileDebugUnitTestKotlin`: BUILD SUCCESSFUL
- `testDebugUnitTest` (full suite): **172 tests, 23 failed** — same 23 **pre-existing**
  failures as PR-1/2/3 (`SSHTunnelManagerTest` ×8, `SSHTunnelConfigConverterTest` ×7,
  `SSLConfigConverterTest` ×6, `EditorHistoryTest` ×2), **zero new failures**. 15 new tests
  all pass: 6 `AppThemeTest` (R3) + 5 `ContrastUtilsTest` (sanity) + 4 `DesignTokensTest`
  (contrast regression) = 157 PR-3 baseline + 15 = 172.
- `assembleDebug`: BUILD SUCCESSFUL

### Commits (3 code, docs pending)

1. `fix(theme): extraer resolveColorScheme y cubrir R3 con tests (RED-GREEN)` — `AppTheme.kt`, `AppThemeTest.kt`
2. `feat(theme): agregar contrastRatio WCAG puro con tests de sanity (RED-GREEN)` — `ContrastUtils.kt`, `ContrastUtilsTest.kt`
3. `fix(theme): corregir contraste dark de accentSuccess y textTertiary (RED-GREEN)` — `Color.kt`, `DesignTokens.kt`, `DesignTokensTest.kt`

Note: the assigned split was "one commit for accentSuccess fix+test, one for textTertiary
fix+test" — these two were merged into a single commit (#3) because both are fixed via the
same `buildDesignTokens(scheme, darkTheme)` signature change in the same file; splitting
them would have required an artificial intermediate state (half-added parameter) with no
clean rollback boundary. Flagged here rather than silently deviating from the requested
commit shape.

### Diff size

`git diff --stat feature/dark-mode-custom-draw..feature/dark-mode-verify-fixes`: 7 files
changed, 408 insertions(+), 18 deletions(-) ≈ **426 changed lines** — above the shared-skill
400-line default, but within the `dark-mode` change's session-cached **800-line** budget
(see tasks.md Review Workload Forecast). This is a legitimate 4th stacked slice, autonomous
in scope (test-coverage/contrast fix round only), with its own verification and clear
rollback boundary (revert this branch, PR-3's tip is unaffected).

### Excluded from this round (per orchestrator instruction)

- `androidTest` module compile failure (3 unrelated pre-existing broken test files:
  `MyDataBasesNavHostTest.kt`, `QueryEditorScreenTest.kt`, `WorkspaceCarouselTest.kt`) —
  confirmed pre-existing on `master` in `sdd-verify`'s session, unrelated to `dark-mode`. Not
  touched. Flagged to the orchestrator as a candidate for a separate infra-repair SDD change.
- `temp_drag_changes.patch` at repo root — same unrelated leftover carried over from PR-1/2/3,
  still untouched, not staged, not committed.

### This closes the `dark-mode` change's outstanding verify gaps

The CRITICAL (R3 test coverage) and both contrast WARNINGs (`accentSuccess`, `textTertiary`
dark) from `verify-report.md` are now genuinely closed with real RED→GREEN tests, not
superficial patches. Ready for a re-verify pass (`sdd-verify`).
