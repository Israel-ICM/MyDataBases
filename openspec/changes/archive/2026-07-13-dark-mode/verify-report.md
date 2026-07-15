# Verification Report

**Change**: dark-mode
**Version**: cumulative tip of `feature/dark-mode-verify-fixes` (PR-1 #10 + PR-2 #11 + PR-3 #12 + PR-4 #13, none merged to `master`)
**Mode**: Strict TDD

## Re-verify History

- **Pass 1** (tip of `feature/dark-mode-custom-draw`, PR-1/2/3): **PASS WITH WARNINGS**, 1 CRITICAL blocking archive (R3 zero test coverage) + 5 WARNINGS (`SettingsScreenTest`/androidTest infra breakage, R5/R6 untestable-without-infra scenarios, newly-discovered `accentSuccess` dark contrast regression 2.30:1, `textTertiary` dark contrast 2.37:1) + 3 SUGGESTIONS.
- **Pass 2 (this report)** — re-verify after **PR-4** (`feature/dark-mode-verify-fixes`, targeted fix round): confirms CRITICAL #1 genuinely closed (real RED→GREEN tests, not just claimed), independently recomputes both contrast fixes, confirms `contrastRatio()` util itself is correct, re-runs the full suite, and confirms zero regressions in PR-1/2/3's previously-passing scope. **2 of the original 5 pass-1 WARNINGS are now closed** (`accentSuccess`, `textTertiary` dark); the remaining 2 (androidTest infra breakage, R5/R6 untestable-without-infra scenarios) are unchanged/out-of-scope for this round and carried forward as known/accepted, plus 1 new sub-finding (light-mode `textTertiary` gap) that PR-4 itself discovered and explicitly deferred.

## Completeness

| Metric | Value |
|--------|-------|
| Tasks total | 32 (original) + fix-round items tracked in `apply-progress.md` |
| Tasks complete | 32/32 original + all 3 PR-4 fix items |
| Tasks incomplete | 0 |

## Build & Tests Execution

**Build**: ✅ Passed
```text
./gradlew.bat assembleDebug --console=plain
BUILD SUCCESSFUL in 3s
42 actionable tasks: 42 up-to-date
```

**Tests**: ⚠️ 172 total / 149 passed / 23 failed (unit, `testDebugUnitTest`)
```text
./gradlew.bat testDebugUnitTest --console=plain
172 tests completed, 23 failed

23 failures — re-confirmed EXACT name-for-name match against pass-1 baseline (same 23,
same classes), all pre-existing/unrelated to dark-mode:
  SSHTunnelManagerTest         (8 — UnknownHostException, no network in this sandbox)
  SSHTunnelConfigConverterTest (7 — AssertionError/RuntimeException, unrelated to theming)
  SSLConfigConverterTest       (6 — same class of pre-existing converter bugs)
  EditorHistoryTest            (2 — coalescing/push assertions, unrelated)

0 NEW failures. 172 - 157 (pass-1 baseline) = 15 new tests added by PR-4
(6 AppThemeTest + 5 ContrastUtilsTest + 4 DesignTokensTest), ALL green.
```

**Instrumented tests (`androidTest`)**: ❌ Still cannot execute — **pre-existing, unrelated infra breakage, unchanged since pass 1**
```text
./gradlew.bat compileDebugAndroidTestKotlin --console=plain
BUILD FAILED

Re-ran the compile check this session — confirmed via `git diff --stat` that PR-4 touched
only theme-package files + docs (AppTheme.kt, Color.kt, ContrastUtils.kt, DesignTokens.kt,
AppThemeTest.kt, ContrastUtilsTest.kt, DesignTokensTest.kt, + apply-progress/tasks/verify-report
docs). The 3 broken androidTest files (MyDataBasesNavHostTest.kt, QueryEditorScreenTest.kt,
WorkspaceCarouselTest.kt) are untouched. This remains a pre-existing, project-wide,
out-of-scope infra gap — NOT re-flagged as a new issue, carried forward as known/accepted
(see WARNING #2 below).
```

**Coverage**: ➖ Not available (Jacoco/Kover not configured — pre-existing project gap, unchanged)

## Spec Compliance Matrix

| Req | Scenario | Test | Result |
|-----|----------|------|--------|
| R1 | User selects Light | `SettingsScreenTest.tappingLight_invokesOnSelectWithLight` | ⚠️ WARNING — test exists, correct assertions, **still never executed** (androidTest module doesn't compile, pre-existing/unrelated cause, unchanged since pass 1) |
| R1 | User selects Dark | `SettingsScreenTest.tappingDark_invokesOnSelectWithDark` | ⚠️ WARNING — same as above |
| R1 | User selects System | `SettingsScreenTest.tappingSystem_invokesOnSelectWithSystem` | ⚠️ WARNING — same as above |
| R1 | Selection survives restart | (none directly) | ⚠️ PARTIAL — inferred from R2's DataStore round-trip test; unchanged |
| R2 | Default on first launch = SYSTEM | `SettingsRepositoryImplTest.observeThemeMode returns SYSTEM by default` | ✅ COMPLIANT — ran, passed |
| R2 | Write then observe (DARK, survives process death) | `SettingsRepositoryImplTest.setThemeMode with DARK...` | ✅ COMPLIANT (observe path); process-death persistence is DataStore's platform guarantee |
| R3 | Dark + branded → BrandedDarkColorScheme | `AppThemeTest.` *Dark + branded resolves to BrandedDarkColorScheme (R3 scenario 1)* | ✅ COMPLIANT — ran, passed. `resolveColorScheme()` extracted as pure fn (no Compose/Context dep), `assertSame(BrandedDarkColorScheme, result)` |
| R3 | Light + branded → BrandedLightColorScheme | `AppThemeTest.` *Light + branded resolves to BrandedLightColorScheme (R3 scenario 2)* | ✅ COMPLIANT — ran, passed |
| R3 | Dark + non-branded → non-branded dark | `AppThemeTest.` *Dark + non-branded resolves to the dynamic dark scheme when available (R3 scenario 3)* | ✅ COMPLIANT — ran, passed. Plus a 6th triangulation test proving the fallback-to-branded path when dynamic color is unavailable (not hardcoded to always return the fake) |
| R3 | System + OS-dark + branded → BrandedDark | `AppThemeTest.` *System defers to OS-dark then applies branded axis (R3 scenario 4, composed with resolveDarkTheme)* | ✅ COMPLIANT — ran, passed. Composes `resolveDarkTheme(SYSTEM, systemInDarkTheme=true)` with `resolveColorScheme(...)` |
| R3 | Axes independent | `AppThemeTest.` *Axes are independent - toggling branded_palette never changes the resolved dark-vs-light base (R3 scenario 5)* | ✅ COMPLIANT — ran, passed. Asserts identity across 6 combinations: fixed-dark/toggle-branded, fixed-light/toggle-branded, fixed-branded/toggle-dark |
| R4 | No remaining `MyDataBasesTheme` references | source-tree grep | ✅ COMPLIANT — unchanged, re-confirmed |
| R4 | Previews render via AppTheme | (none — no Compose Preview render test) | ⚠️ PARTIAL — unchanged |
| R5 | Token reads dark value | `DesignTokensTest.buildDesignTokens with dark scheme...` | ✅ COMPLIANT — ran, passed |
| R5 | Token reads light value | `DesignTokensTest.buildDesignTokens with light scheme...` | ✅ COMPLIANT — ran, passed |
| R5 | Token tracks live theme change (recomposition) | (none) | ⚠️ WARNING — UNTESTED, unchanged; requires androidTest (currently broken infra) |
| R6 | Carousel shadow visible on dark | `WorkspaceCarouselShadowTest` (3 tests) | ✅ COMPLIANT — ran, passed |
| R6 | Scrims and gradients adapt | (none) | ⚠️ WARNING — UNTESTED, unchanged |
| R6 | Light unaffected | (none) | ⚠️ WARNING — UNTESTED, unchanged |
| R7 | Strings present in every locale | direct inspection (all 10 `strings.xml`) | ✅ COMPLIANT — unchanged, re-confirmed |
| R7 | No hardcoded selector text | direct inspection | ✅ COMPLIANT — unchanged, re-confirmed |

**Compliance summary**: 13/21 fully COMPLIANT (up from 8/21), 8/21 PARTIAL/WARNING (test-infra gaps, code correct — unchanged), **0/21 CRITICAL (down from 5/21 — R3 fully closed this round)**.

*(Note: the table enumerates 21 distinct testable scenario rows; the spec document's summary line states "24 scenarios" across 7 requirements — this minor counting discrepancy pre-dates PR-4, is not part of this fix round's scope, and does not affect the CRITICAL closure finding.)*

## Correctness (Static Evidence)

| Requirement | Status | Notes |
|------------|--------|-------|
| R1 Selector UI | ✅ Implemented | Unchanged since pass 1 |
| R2 Persistence | ✅ Implemented | Unchanged since pass 1 |
| R3 Resolution | ✅ Implemented **and now tested** | `resolveColorScheme(darkTheme, brandedPaletteEnabled, dynamicColorAvailable, dynamicScheme)` extracted as a pure function (verified: no Compose/Context/Activity dependency in its signature or body — only `ColorScheme`/`Boolean` params), mirroring the already-tested `resolveDarkTheme`. `AppTheme.kt`'s `@Composable` body now only resolves `dynamicScheme` (impure, needs `Context`) and delegates the decision entirely to the pure function |
| R4 Entry point migration | ✅ Implemented | Unchanged since pass 1 |
| R5 Theme-aware tokens | ⚠️ Mostly implemented | Unchanged since pass 1 (`iconBackground`/`accentPrimaryLight`/`accentPrimaryDark` still theme-invariant, confirmed still dead code — zero call sites, re-checked) |
| R6 Dark-safe custom-draw | ✅ Implemented, contrast regression now fixed | `accentSuccess` and `textTertiary` (dark) both independently re-verified above 3:1 / 4.5:1 respectively |
| R7 Localized strings | ✅ Implemented | Unchanged since pass 1 |

## Coherence (Design)

| Decision | Followed? | Notes |
|----------|-----------|-------|
| `DesignTokens` object → data class + `LocalDesignTokens` | ✅ Yes | Unchanged since pass 1 |
| Full deletion of `Theme.kt` | ✅ Yes | Unchanged since pass 1 |
| `theme_mode` ownership | ✅ Yes | Unchanged since pass 1 |
| WorkspaceCarousel shadow fix | ✅ Yes | Unchanged since pass 1 |
| `buildDesignTokens(scheme, darkTheme)` now takes a required 2nd param | ⚠️ Deviation from design.md's abbreviated single-param snippet, but justified and documented | Necessary to let `accentSuccess`/`textTertiary` branch on `darkTheme` directly instead of deriving purely from `scheme` (which doesn't itself carry a "this is dark" flag distinguishable from all other schemes). Documented in KDoc (`DesignTokens.kt`) and `apply-progress.md`. Does not break any spec requirement — WARNING-level only, not blocking |
| "Non-branded" = dynamic color fallback logic | ⚠️ Pre-existing, not introduced by dark-mode | Unchanged since pass 1 finding |

## Issues Found

### CRITICAL

None. **Original CRITICAL #1 (R3 zero test coverage) is CLOSED.**

Evidence: `resolveColorScheme()` extracted to `AppTheme.kt` as an `internal` pure function (lines 177-186) — no `Composable`, `Context`, `Activity`, or `LocalX.current` dependency in its signature or body, taking only `Boolean`/`ColorScheme?` params, callable directly from a JVM unit test. `AppThemeTest.kt` adds 6 new tests (+1 fallback triangulation) mapped 1:1 against the spec's 5 R3 scenarios (verified individually above, not just counted): scenario 1 (Dark+branded), scenario 2 (Light+branded), scenario 3 (Dark+non-branded, using the dynamic scheme as the non-branded stand-in), scenario 4 (System+OS-dark+branded, composed with `resolveDarkTheme`), scenario 5 (axes independent, 6 identity assertions across all 4 combinations of the 2 axes). All 6 tests ran and passed in this session's `testDebugUnitTest` run (172 total, 0 theme-package failures among the 23 pre-existing failures).

### WARNING

1. **`SettingsScreenTest` (R1's only test) and the whole `androidTest` module still cannot execute — pre-existing, unrelated infra breakage, unchanged since pass 1.** Re-confirmed this session: `compileDebugAndroidTestKotlin` still fails; PR-4's diff touched only theme-package `.kt` files and docs, none of the 3 broken androidTest files (`MyDataBasesNavHostTest.kt`, `QueryEditorScreenTest.kt`, `WorkspaceCarouselTest.kt`). **Known, accepted** — out of this fix round's declared scope (R3 test coverage + 2 named contrast regressions only). Recommend a separate follow-up SDD change to repair androidTest infra.

2. **5 spec scenarios across R1, R5, R6 still rely on untested Compose runtime behavior** (selector taps, recomposition, scrim/gradient visual adaptation, light-theme non-regression) — same root cause as WARNING #1 (broken androidTest module). Code verified correct by direct inspection; unchanged since pass 1. **Known, accepted.**

3. **Light-mode `textTertiary` sub-AA gap — confirmed still present, correctly out of this fix round's scope.** Independently recomputed: `scheme.outline` (`0xFF75788C`) vs `brand_light_surface` (`0xFFFFFFFF`) = **4.36:1**, matching PR-4's own self-documented discovery in `DesignTokens.kt` KDoc almost exactly (self-reported 4.36:1). Below the 4.5:1 WCAG AA text minimum, but a much smaller gap than the dark-mode regression that was fixed (2.37:1 → 4.61:1). PR-4 deliberately did not touch this — explicitly flagged in KDoc as a follow-up, not silently left undocumented. **Known, accepted** — recommend a small dedicated follow-up to fix `textTertiary` in light mode (likely the same pattern: a dedicated `brand_text_tertiary_light` literal instead of reusing `scheme.outline`).

### CLOSED THIS ROUND (for traceability, not re-flagged)

- ~~`accentSuccess` dark contrast 2.30:1 (below WCAG 1.4.11 3:1 non-text minimum)~~ → **FIXED**, independently recomputed this session at **4.3157:1** (matches PR-4's self-reported 4.32:1). New `brand_success_dark = 0xFF4D9792` in `Color.kt`; light unchanged (`brand_success_light = 0xFF006B63`, recomputed 6.3961:1). Regression-guarded by `DesignTokensTest` (2 tests, both green).
- ~~`textTertiary` dark contrast 2.37:1 (below WCAG AA 4.5:1 text minimum)~~ → **FIXED**, independently recomputed this session at **4.6121:1** (matches PR-4's self-reported 4.61:1). New `brand_text_tertiary_dark = 0xFF8C8FA4` in `Color.kt`, used only for the dark branch of `buildDesignTokens`; `brand_outline` itself deliberately untouched (still feeds `iconNormal`/`onSurfaceVariant`/M3 borders). Regression-guarded by `DesignTokensTest` (2 tests, both green).
- `contrastRatio(a, b)` WCAG util itself spot-checked against known reference pairs this session: black-vs-white = **21.0000** (exact WCAG maximum), identical-color = **1.0000** (exact minimum) — formula is a correct, standard sRGB relative-luminance implementation (linearization threshold 0.03928, exponent 2.4, `(L1+0.05)/(L2+0.05)`). `ContrastUtilsTest`'s own regression anchors (2.30:1 old `accentSuccess`, 2.37:1 old `textTertiary`) also match this session's independent recomputation exactly.

### SUGGESTION

4. Fix light-mode `textTertiary` (4.36:1) in a small dedicated follow-up, closing the gap PR-4 itself surfaced (see WARNING #3).
5. Once androidTest infra is repaired (WARNING #1), retro-actively run `SettingsScreenTest` to convert its 3 WARNING-level scenarios to COMPLIANT.
6. `temp_drag_changes.patch` still untracked at repo root — unrelated to dark-mode, clean up before final merge to avoid PR-diff confusion.

## Verdict

**PASS WITH WARNINGS** — CRITICAL #1 is genuinely closed with real, scenario-mapped, executed tests (not just claimed). The 2 remaining WARNINGS (androidTest infra breakage; light-mode `textTertiary` sub-AA gap) are known, accepted, explicitly out of this fix round's declared scope, and do not block archive. No new regressions found in PR-1/2/3's previously-passing scope (172 tests, same 23 pre-existing/unrelated failures, 0 new failures; `assembleDebug` BUILD SUCCESSFUL). **Ready for `sdd-archive`.**
