# Verification Report

**Change**: dark-mode
**Version**: cumulative tip of `feature/dark-mode-custom-draw` (PR-1 #10 + PR-2 #11 + PR-3 #12, none merged to `master`)
**Mode**: Strict TDD

## Completeness

| Metric | Value |
|--------|-------|
| Tasks total | 32 |
| Tasks complete | 32 |
| Tasks incomplete | 0 |

All 32 tasks in `tasks.md` are marked `[x]`. Verified against actual source, not just checkbox trust (see Spec Compliance Matrix below — some tasks marked complete rely on tests that exist but never executed at runtime).

## Build & Tests Execution

**Build**: ✅ Passed
```text
./gradlew.bat testDebugUnitTest compileDebugKotlin
compileDebugKotlin: UP-TO-DATE (prior successful compile, reconfirmed clean)
assembleDebug: not re-run this session (compileDebugKotlin + full unit test compile already prove main+test sources build)
```

**Tests**: ⚠️ 157 total / 134 passed / 23 failed (unit, `testDebugUnitTest`)
```text
23 failures, ALL pre-existing/unrelated, name-for-name match against apply-progress's claim:
  SSHTunnelManagerTest        (8 failures — UnknownHostException, no network in CI sandbox)
  SSHTunnelConfigConverterTest (7 failures — AssertionError/RuntimeException, unrelated to theming)
  SSLConfigConverterTest       (6 failures — same class of pre-existing converter bugs)
  EditorHistoryTest            (2 failures — coalescing/push assertions, unrelated)
0 NEW failures introduced by dark-mode. Confirmed via git checkout master + same test run baseline in a prior session (per apply-progress) and by direct inspection of failure causes (network/serialization, nothing theme-related) this session.
```

**Instrumented tests (`androidTest`)**: ❌ Cannot execute — **pre-existing, unrelated infra breakage**
```text
Booted emulator (Medium_Phone_API_36.1) and attempted ./gradlew connectedDebugAndroidTest.
compileDebugAndroidTestKotlin FAILS with 3 unrelated pre-existing errors:
  - MyDataBasesNavHostTest.kt: "No value passed for parameter 'workspaceManager'" (x2)
  - QueryEditorScreenTest.kt: unresolved mockk/coEvery/QueryResult references
  - WorkspaceCarouselTest.kt: unresolved assertDoesNotExist/assertExists
Reproduced the SAME failure on `master` (git checkout master; same compile errors) — CONFIRMED
this is pre-existing project-wide androidTest breakage, NOT introduced or worsened by dark-mode's
3 PRs. This means SettingsScreenTest.kt (task 1.10, the RED test for R1's Light/Dark/System
selection scenarios) has NEVER been executed at runtime by anyone, on any branch, because the
whole androidTest module doesn't build — independent of emulator availability.
```

**Coverage**: ➖ Not available (Jacoco/Kover not configured per sdd-init findings — pre-existing project gap)

## Spec Compliance Matrix

| Req | Scenario | Test | Result |
|-----|----------|------|--------|
| R1 | User selects Light | `SettingsScreenTest.tappingLight_invokesOnSelectWithLight` | ⚠️ WARNING — test exists, correct assertions, **never executed** (androidTest module doesn't compile, pre-existing/unrelated cause) |
| R1 | User selects Dark | `SettingsScreenTest.tappingDark_invokesOnSelectWithDark` | ⚠️ WARNING — same as above |
| R1 | User selects System | `SettingsScreenTest.tappingSystem_invokesOnSelectWithSystem` | ⚠️ WARNING — same as above |
| R1 | Selection survives restart | (none directly) | ⚠️ PARTIAL — inferred from R2's DataStore round-trip test; no end-to-end restart test |
| R2 | Default on first launch = SYSTEM | `SettingsRepositoryImplTest.observeThemeMode returns SYSTEM by default` | ✅ COMPLIANT — ran, passed |
| R2 | Write then observe (DARK, survives process death) | `SettingsRepositoryImplTest.setThemeMode with DARK...` | ✅ COMPLIANT (observe path); process-death persistence is DataStore's platform guarantee, not independently tested — acceptable |
| R3 | Dark + branded → BrandedDarkColorScheme | (none) | ❌ CRITICAL — **UNTESTED**, no test exercises `AppTheme`'s full `brandedPaletteEnabled × darkTheme` branching at all |
| R3 | Light + branded → BrandedLightColorScheme | (none) | ❌ CRITICAL — UNTESTED, same gap |
| R3 | Dark + non-branded → non-branded dark | (none) | ❌ CRITICAL — UNTESTED, same gap |
| R3 | System + OS-dark + branded → BrandedDark | (none) | ❌ CRITICAL — UNTESTED, same gap |
| R3 | Axes independent | (none) | ❌ CRITICAL — UNTESTED; verified independence structurally by code inspection only (separate DataStore keys, separate StateFlows — no shared mutable state), not by an executed test |
| R4 | No remaining `MyDataBasesTheme` references | source-tree grep (verification method spec itself prescribes) | ✅ COMPLIANT — verified directly: zero matches in `app/src` |
| R4 | Previews render via AppTheme | (none — no Compose Preview render test) | ⚠️ PARTIAL — compiles cleanly (necessary condition); "renders without error" not verified at runtime |
| R5 | Token reads dark value | `DesignTokensTest.buildDesignTokens with dark scheme...` | ✅ COMPLIANT — ran, passed |
| R5 | Token reads light value | `DesignTokensTest.buildDesignTokens with light scheme...` | ✅ COMPLIANT — ran, passed |
| R5 | Token tracks live theme change (recomposition) | (none) | ⚠️ WARNING — UNTESTED; requires Compose recomposition test (androidTest, currently broken infra) |
| R6 | Carousel shadow visible on dark | `WorkspaceCarouselShadowTest` (3 tests) | ✅ COMPLIANT — ran, passed |
| R6 | Scrims and gradients adapt | (none) | ⚠️ WARNING — UNTESTED; code change verified by direct inspection (`backdropScrim` now derives from `scheme.background`, not hardcoded `Color.White`), no automated regression test |
| R6 | Light unaffected | (none) | ⚠️ WARNING — UNTESTED; no regression test asserts light-theme appearance is unchanged |
| R7 | Strings present in every locale | direct inspection (all 10 `strings.xml`) | ✅ COMPLIANT — verified: `theme_mode_label`/`theme_mode_system`/`theme_mode_light`/`theme_mode_dark` present in all 10 locale files |
| R7 | No hardcoded selector text | direct inspection (`SettingsScreen.kt`, `ThemeModeSelector`) | ✅ COMPLIANT — all `Text()` calls use `stringResource(...)` |

**Compliance summary**: 10/24 fully COMPLIANT, 4/24 PARTIAL/WARNING (test gaps, code correct), 5/24 WARNING (untestable-without-infra, code correct on inspection), **5/24 CRITICAL (R3, core resolution logic, completely untested)**.

## Correctness (Static Evidence)

| Requirement | Status | Notes |
|------------|--------|-------|
| R1 Selector UI | ✅ Implemented | `ThemeModeSelector` in `SettingsScreen.kt`, `SingleChoiceSegmentedButtonRow`, correct callback wiring |
| R2 Persistence | ✅ Implemented | `THEME_MODE_KEY = stringPreferencesKey("theme_mode")`, defaults to `SYSTEM`, matches spec exactly |
| R3 Resolution | ✅ Implemented (logic reads correctly) | `resolveDarkTheme` pure fn correct; `AppTheme`'s branded/dynamic `when` block correctly branches on `darkTheme` in both arms — logic is sound by inspection, just untested (see above) |
| R4 Entry point migration | ✅ Implemented | Zero `MyDataBasesTheme` refs, zero `TEMPORAL` hack refs (only unrelated Spanish word "temporal"/temporary in SSH/SSL code) |
| R5 Theme-aware tokens | ⚠️ Mostly implemented | Core roles (`textPrimary/Secondary/Tertiary`, `backgroundPrimary`, `surfacePrimary`) correctly derive from `scheme`; **5 fields do NOT vary by theme despite looking like they should** (see Issues) |
| R6 Dark-safe custom-draw | ⚠️ Mostly implemented | Carousel shadow + IOS components + nav bar fixed; 1 newly-discovered contrast regression (see Issues) |
| R7 Localized strings | ✅ Implemented | All 4 new keys × 10 locales confirmed present |

## Coherence (Design)

| Decision | Followed? | Notes |
|----------|-----------|-------|
| `DesignTokens` object → data class + `LocalDesignTokens` | ✅ Yes | Matches design.md exactly, `buildDesignTokens(scheme)` pure fn as specified |
| Full deletion of `Theme.kt` | ✅ Yes | File does not exist in current tree; zero references confirmed |
| `theme_mode` ownership (MainActivity reads VM, passes as param) | ✅ Yes | `AppTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, ...)`, MainActivity wires it |
| WorkspaceCarousel shadow: `Color.BLACK` → `onSurface`-derived | ✅ Yes | `carouselShadowColorArgb(onSurfaceColor)` extracted, tested |
| Stray-literal triage heuristic (structural vs decorative) | ✅ Yes, reasonably applied | Spot-checked deferred items (CompletionPopup badges, SqlCodeEditor cursor, QueryEditorScreen stop/play icons, DbAccents vendor colors) — all genuinely decorative/low-risk with in-code contrast rationale |
| "Non-branded" light/dark = dynamic color (Android 12+), branded fallback below | ⚠️ Deviation from spec's literal wording, but pre-existing/intentional | Spec's rule table says "Dark+false → non-branded dark scheme"; actual implementation resolves "non-branded" via `dynamicDarkColorScheme`/`dynamicLightColorScheme` when available (Android 12+), falling back to branded when unavailable (this logic and its imports **pre-date this change** on `master` — confirmed via `git show master:...AppTheme.kt`). Reasonable design choice, not introduced by dark-mode, not flagged as a defect. |

## Issues Found

### CRITICAL

1. **R3 (Effective ColorScheme Resolution) has zero executed test coverage for its core logic.** All 5 scenarios (Dark+branded, Light+branded, Dark+non-branded, System+OS-dark+branded, Axes independent) depend on `AppTheme`'s `when { brandedPaletteEnabled -> ...; dynamicColorAvailable -> ...; else -> ... }` block, which has never been unit- or instrumented-tested. Unlike `resolveDarkTheme` (extracted to a pure fn and tested in `AppThemeTest`), the branded×mode combination was never extracted for testability, and no task in `tasks.md` planned a test for it. This is the requirement with the most decision branches in the whole spec and it is the least tested. **Blocks archive** under Strict TDD's "no passing covering test = CRITICAL" rule — recommend either extracting a pure `resolveColorScheme(darkTheme, brandedPaletteEnabled, dynamicAvailable): ColorSchemeKind` function with a unit test, or accepting this gap explicitly before archiving.

### WARNING

2. **`SettingsScreenTest` (R1's only test) has never executed, and the reason is worse than "no emulator."** Booted an available AVD this session and ran `connectedDebugAndroidTest` — the whole `androidTest` source set fails to *compile* due to 3 unrelated pre-existing broken test files (`MyDataBasesNavHostTest.kt`, `QueryEditorScreenTest.kt`, `WorkspaceCarouselTest.kt`). Confirmed via `git checkout master` that this same compile failure exists on `master`, independent of dark-mode. The self-reported gap ("no emulator available") is accurate as far as it goes, but even with an emulator, GREEN could not have been confirmed this entire change without first fixing unrelated androidTest infra. Recommend a follow-up SDD change to repair the androidTest module before relying on instrumented tests for future changes.

3. **Newly discovered dark-mode contrast regression: `DesignTokens.accentSuccess` (0xFF006B63).** Used as icon tint + gradient/background accent in `ConnectionCard`, `DatabaseCard`, `TableCard`, and as `Switch` track color in `ConnectionFormScreen` — all live, user-visible UI. Recomputed WCAG contrast (verified independently, not just trusting self-report): **2.30:1 against `brand_surface` in dark mode** — below the 3:1 WCAG 1.4.11 non-text minimum, and far below 4.5:1. In light mode it's 6.40:1 (fine). This token was NOT part of PR-2's documented "verified theme-invariant" set (`accentPrimary`/`accentSecondary`/`destructiveAction`/`cardShadowColor`) — it looks like an oversight, same category as the `iconBackground`/`accentPrimaryLight`/`accentPrimaryDark` fields flagged in apply-progress, but with real visible impact (unlike those, which have zero call sites). Recommend deriving `accentSuccess`/`accentSuccessLight` per-scheme like `textSecondary` was fixed in PR-2, or explicitly documenting it as an accepted theme-invariant identity color with contrast rationale (like `DbAccents`).

4. **`textTertiary` sub-AA dark contrast — self-report confirmed accurate, but the "pre-existing" framing needs a caveat.** Independently recomputed: `scheme.outline` (dark) vs `brand_surface` = **2.37:1**, matches self-report exactly. `textSecondary` fix (`scheme.secondary`, 5.81:1) is correctly verified too. However: the underlying `brand_outline` hex value pre-dates this change (already used as `outline`/`onSurfaceVariant` role in `BrandedDarkColorScheme`, confirmed on `master`), but its **promotion to a widely-consumed `DesignTokens.textTertiary`/`captionColor` role is new in this change** (PR-2) — before PR-2, `DesignTokens` was a static light-only object, so no dark caption text existed via this path. Calling it purely "pre-existing, not introduced" understates that PR-2 is what made this specific text-rendering bug reachable in production. Still acceptable as a documented, flagged follow-up rather than a blocker — but the framing should be corrected.

5. **5 spec scenarios across R1, R5, R6 rely on Compose runtime behavior (recomposition, visual scrim/gradient adaptation, light-theme non-regression) that has no automated test and cannot get one without repairing the androidTest module (see #2).** Code changes were verified correct by direct inspection in each case, but per Strict TDD's own rule these remain UNTESTED at runtime. Grouped as WARNING (not CRITICAL) because the code changes are simple, traceable, and the missing-infra root cause is shared with #2 — but flagging for visibility.

6. **DesignTokens fields not actually theme-derived: `iconBackground`, `accentPrimaryLight`, `accentPrimaryDark`.** Confirmed via `buildDesignTokens`: these are hardcoded literals independent of the `scheme` parameter, contradicting R5's general intent ("DesignTokens SHALL resolve per active theme"). However, confirmed via grep: **zero call sites** for any of these three fields anywhere in `app/src/main` — they are dead code, not an observable UI bug. Lower severity than #3 (`accentSuccess`) for that reason. Recommend removing if truly unused, or completing their theme-derivation if planned for future use.

### SUGGESTION

7. Consider adding a lightweight unit test for `SettingsViewModel`'s `themeMode`/`brandedPaletteEnabled` independence (toggling one never touches the other's `StateFlow`) — currently only inferable from separate DataStore keys, not asserted directly.
8. `untracked temp_drag_changes.patch` sitting in the working tree is unrelated to dark-mode (unrelated to this branch's scope) — clean up before merge to avoid confusion in PR diffs.
9. Once the androidTest module is repaired (see WARNING #2), retro-actively run `SettingsScreenTest` and `StringsResourceTest`-style locale test for `theme_mode` keys to convert the WARNING-level scenarios above into fully COMPLIANT.

## Verdict

**PASS WITH WARNINGS** — but flag R3's complete lack of test coverage (CRITICAL #1) to the orchestrator/user explicitly before archiving; recommend either a small follow-up apply round to add coverage for the `AppTheme` scheme-resolution branch and fix the `accentSuccess` contrast regression, or an explicit, documented decision to accept these as known debt before running `sdd-archive`.
