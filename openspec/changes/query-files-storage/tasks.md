# Tasks: Query Files List Screen + App-Managed Storage Location

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~3,050-3,450 lines (18 new files, 9 modified files) |
| Review budget applied | 800 changed lines (session default, matches `large-sql-script-execution`) |
| 800-line budget risk | High |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | 5 PRs (feature-branch-chain recommended; see below) |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Est. lines | Notes |
|------|------|-----------|-----------|-------|
| 1 | Domain foundations: `QueryFileStore`, `QueryFileInfo`, `AppFolder`/`segmentFor` (TDD), `RootResolution`, `ListQueryFilesUseCase`/`SaveQueryFileUseCase`/`DeleteQueryFileUseCase` (+ tests) | PR 1 | ~550-650 | No `DocumentFile`/Android dependency; pure JVM. Base: feature/tracker branch. |
| 2 | Data layer: `QueryStorageRootProvider`, `QueryStorageResolver` (root selection + permission-loss detection), `QueryFileStoreImpl` (+ unit tests w/ fakes, instrumented tests populated-not-run) + DI wiring (`RepositoryModule`, new `QueryStorageModule`) + `androidx.documentfile` Gradle dependency | PR 2 | ~750-850 | Depends on PR 1 interfaces/models. Correctness-critical (root resolution + fallback); base = PR 1 branch. |
| 3 | Settings integration: `SettingsRepository`/-Impl DataStore pref, `SettingsScreen`/VM storage row + `OpenDocumentTree` + reset action, migration-copy prompt UI, `MigrateQueryFilesUseCase` (+ tests), localization for settings row / migration prompt / SAF fallback notice (10 locales) | PR 3 | ~800-900 | Depends on PR 2 (`QueryFileStore.read/write` for copy). Base = PR 2 branch. |
| 4 | `query_files` route: `QueryFilesScreen` + `QueryFilesViewModel` state machine (+ tests, Compose UI test), localization for list/empty-state/FAB (10 locales) | PR 4 | ~650-700 | Depends on PR 2 (`QueryFileStore.list`) only, stacked after PR 3 for linear review order. Base = PR 3 branch. |
| 5 | Entry-point rewiring (both `MyDataBasesNavHost.kt` call sites → `Routes.QueryFiles`), regression check for `2026-06-30-new-query-modal-fix`, editor Save convergence (`ShortcutAction.Save` → `QueryFileStore.write`), storage ADR | PR 5 | ~300-350 | Depends on PR 4 (navigation target must exist) and PR 1/2 (`QueryFileStore.write`). Base = PR 4 branch. Manual verification (Phase 12) runs here, once the full chain is integrated. |

**Recommended chain strategy**: **feature-branch-chain** — 5 sequential layers (domain → data/storage → settings → list screen → entry-point/save-convergence), each depending on the previous. A tracker branch accumulates the full feature; PR 1 targets the tracker, PR 2 targets PR 1's branch, ..., PR 5 targets PR 4's branch. Only the tracker merges to `master`. This isolates the two highest-risk components (SAF root resolution/fallback in PR 2, and the user-visible Save-behavior change in PR 5) into their own reviewable slices, and matches the layered dependency graph better than independent stacked-to-main slices.

**Why not fewer/more PRs?** PR 2 (data layer) cannot be smaller without splitting `QueryStorageResolver` from `QueryFileStoreImpl`, which would force an intermediate PR with no working `list/write` yet — not independently useful. PR 3 and PR 4 both only strictly need PR 2, but are kept sequential (not parallel stacks) to match the reference change's linear-chain convention and keep review order simple. Localization for Settings/migration and List-screen strings stays with the PR that introduces the corresponding UI, not a separate localization-only PR, consistent with `large-sql-script-execution` Phase 12/19 precedent.

User must choose (or confirm) the chain strategy before `sdd-apply` proceeds (delivery strategy `ask-on-risk`).

---

## Phase 1: Domain — Models & AppFolder (TDD)

- [x] 1.1 Created `domain/repositories/QueryFileStore.kt`: interface with `list(engineType)`, `read(uri)`, `write(engineType, fileName, content)`, `delete(uri)` — no default engine param on any signature
- [x] 1.2 Created `domain/models/QueryFileInfo.kt`: `data class QueryFileInfo(val name: String, val uri: Uri, val lastModified: Long)`
- [x] 1.3 Created `domain/models/RootResolution.kt` — required adding the `androidx.documentfile:documentfile:1.0.1` Gradle dependency in THIS PR (not PR-2 as originally suggested in the Work Units table) since this domain model references `DocumentFile` directly and needs it to compile
- [x] 1.4 RED: wrote `AppFolderTest.kt` — 9 tests, exhaustive `segmentFor(DatabaseType)` for all 4 values, explicit guard against `displayName`/raw enum name, `resolve(engine, Queries)` for each engine
- [x] 1.5 GREEN: created `domain/models/AppFolder.kt` — sealed type with only `Queries`, `segmentFor`/`resolve` companion functions
- [x] 1.6 Confirmed zero Android/`Context`/`DocumentFile` dependency in `AppFolder.kt` itself (only `DatabaseType`) — fully JVM-unit-testable, verified by the passing pure-JUnit test run

## Phase 2: Domain — Use Cases (TDD)

- [x] 2.1 RED: wrote `ListQueryFilesUseCaseTest.kt` — delegates to `store.list(engine)`, propagates failure
- [x] 2.2 GREEN: created `domain/usecases/queryfiles/ListQueryFilesUseCase.kt`
- [x] 2.3 RED: wrote `SaveQueryFileUseCaseTest.kt` — delegates to `store.write(engine, name, content)`, propagates failure
- [x] 2.4 GREEN: created `domain/usecases/queryfiles/SaveQueryFileUseCase.kt`
- [x] 2.5 RED: wrote `DeleteQueryFileUseCaseTest.kt` — delegates to `store.delete(uri)`, propagates failure
- [x] 2.6 GREEN: created `domain/usecases/queryfiles/DeleteQueryFileUseCase.kt`
- [x] All RED confirmed genuine (compile failure, unresolved references) before implementing; 15/15 new tests GREEN (9 + 2 + 2 + 2); full suite 291+/314 passing, same 23 pre-existing unrelated failures; `assembleDebug` succeeds

## Phase 3: Data — Root Provider & Resolver (TDD)

- [x] 3.1 Created `data/storage/QueryStorageRootProvider.kt`: **interface** (not sealed class as originally sketched — an interface with `privateRoot()`/`safRoot(treeUri)` is simpler to mock in `QueryStorageResolverTest` than a sealed-variant design) + `DefaultQueryStorageRootProvider` real implementation using `getExternalFilesDir`/`DocumentFile.fromTreeUri`
- [x] 3.2 RED: wrote `QueryStorageResolverTest.kt` — 6 tests: null pref → private; valid SAF → SAF root; SAF provider returns null → Fallback; SAF exists()==false → Fallback; SAF canWrite()==false → Fallback; re-checks every call (not cached), demonstrated by flipping the mock between two calls on the same resolver instance
- [x] 3.3 GREEN: created `data/storage/QueryStorageResolver.kt`: `resolveRoot()` reads the pref via `.first()`, tries SAF when non-null, falls back to private on any unavailability
- [x] 3.4 Confirmed by test 6 (re-check every call) — no suppression state exists anywhere in `QueryStorageResolver`, matches the confirmed decision exactly

## Phase 4: Data — `QueryFileStoreImpl`

- [x] 4.1 Created `data/repositories/QueryFileStoreImpl.kt`: single implementation over `resolver.resolveRoot().root`, lazy `{engineSegment}/queries/` resolution via `AppFolder.segmentFor` + `DocumentFile.findFile`/`createDirectory`
- [x] 4.2 Implemented `list(engineType)`: filtered by `.sql` suffix (case-insensitive), maps to `QueryFileInfo`; `RootResolution.Fallback` case handled transparently via the common `root` property added to the sealed base (see deviation below)
- [x] 4.3 Implemented `read(uri)`: `contentResolver.openInputStream`
- [x] 4.4 Implemented `write(engineType, fileName, content)`: `findFile` first (overwrite-in-place, no duplicate), else `createFile`; `contentResolver.openOutputStream(uri, "wt")`
- [x] 4.5 Implemented `delete(uri)`: `DocumentFile.fromSingleUri(context, uri)?.delete()`
- [x] 4.6 Wrote `QueryFileStoreImplTest.kt` — 6 tests: case-insensitive `.sql` filter, missing-subfolder-returns-empty (not created by `list`), `Fallback` resolution still returns `Success` with the fallback folder's contents, per-engine isolation, lazy folder creation on `write`, overwrite-in-place on existing filename
- [x] 4.7 Populated (not executed — no device) `QueryFileStoreImplInstrumentedTest.kt`: write→list, write→read round-trip, delete→list, engine isolation, all against the real app-private root; noted a SAF-tree variant needs an interactively-granted tree and is left as a manual-extension note for whoever runs this

**Deviation**: `RootResolution` (from PR-1) needed a common `abstract val root: DocumentFile` added to its sealed base — the original PR-1 design only declared `root` per-subclass, which doesn't let callers write `resolver.resolveRoot().root` without an exhaustive `when`. Small, backward-compatible addition (both existing subclasses already had a same-named/-typed property, just needed `override`).

## Phase 5: DI Wiring & Dependency

- [x] 5.1 `androidx.documentfile:documentfile:1.0.1` was already added in PR-1 (see that phase's note — a domain model needed it to compile)
- [x] 5.2 Modified `core/di/RepositoryModule.kt`: added `@Binds abstract fun bindQueryFileStore(impl: QueryFileStoreImpl): QueryFileStore`
- [x] 5.3 Created `core/di/QueryStorageModule.kt`: `@Binds` for `QueryStorageRootProvider` → `DefaultQueryStorageRootProvider`. `QueryStorageResolver`/`QueryFileStoreImpl` need no explicit binding — Hilt constructs them directly via their `@Inject` constructors once their dependencies resolve
- [x] 5.4 Ran `./gradlew test` and `./gradlew assembleDebug` — both succeed; 329 total tests (15 new: 3 Settings + 6 Resolver + 6 Store), same 23 pre-existing unrelated failures, no new regressions

## Phase 6: Settings — Repository & DataStore Pref (TDD)

- [x] 6.1-6.3 **Pulled forward into PR-2**, not PR-3 as originally grouped: `QueryStorageResolver` (PR-2/Phase 3) already calls `SettingsRepository.observeQueryStorageTreeUri()` in real (non-mocked) code, so the interface + `SettingsRepositoryImpl` implementation had to exist for PR-2 to compile. Done: RED (3 new tests in `SettingsRepositoryImplTest.kt` — default null, round-trip, clear-on-null) → GREEN (`observeQueryStorageTreeUri()`/`setQueryStorageTreeUri()` added to the interface and implemented via `QUERY_STORAGE_TREE_URI_KEY = stringPreferencesKey(...)`, `Uri.parse`/`.toString()`). PR-3 only needs the UI + migration use case + `takePersistableUriPermission` wiring below.

## Phase 7: Settings — `takePersistableUriPermission` & Migration Use Case (TDD)

- [x] 7.1 RED: wrote `MigrateQueryFilesUseCaseTest.kt` — 3 tests. **Deviation**: signature is `invoke(oldRoot: DocumentFile, readContent, writeContent)`, no `newRoot`/no direct `QueryFileStore` dependency — the use case is a pure `DocumentFile` tree walker; the caller (Settings ViewModel) supplies `readContent`/`writeContent` typically backed by `QueryFileStore.read/write` called AFTER the pref switch, so writes naturally land in the new location without this use case needing to reference it
- [x] 7.2 GREEN: created `domain/usecases/queryfiles/MigrateQueryFilesUseCase.kt`
- [x] 7.3 Modified `SettingsScreen.kt`'s `OpenDocumentTree` launcher result handler: calls `context.contentResolver.takePersistableUriPermission(uri, FLAG_GRANT_READ|WRITE_URI_PERMISSION)` before calling `viewModel.onStorageTreeSelected(uri)`

## Phase 8: Settings — UI (Storage Row, Reset, Migration Prompt)

- [x] 8.1 Modified `SettingsScreen.kt` + `SettingsViewModel.kt`: added "Query storage location" row (current mode summary + "Change folder"/"Reset to default" actions)
- [x] 8.2-8.3 **Deviation from "one-time copy-prompt"**: the has-files check is a cheap structural scan (`SettingsViewModel.rootHasQueryFiles`, no content read) run BEFORE persisting — if files exist, shows the prompt and does NOT persist yet; `confirmMigration()`/`declineMigration()` both persist the new pref, only `confirmMigration()` also runs `MigrateQueryFilesUseCase`. Same flow for both "Change folder" and "Reset to default" (`onStorageTreeSelected(null)`)
- [x] 8.4 Partial-copy failures reported via `SnackbarHostState` (`lastMigrationFailureCount` StateFlow + `LaunchedEffect`, self-consuming so it doesn't re-show)
- [x] 8.5 Wrote `SettingsViewModelQueryStorageTest.kt` — 4 tests: prompt shown only with existing files, prompt skipped when empty (persists directly), confirm persists-then-copies, decline persists-without-copying. Also updated the pre-existing `SettingsViewModelTest.kt` (3 tests) for the new constructor params — confirmed still green, no regression

## Phase 9: Settings & SAF-Fallback Localization

- [x] 9.1 Added to `res/values/strings.xml`: `query_storage_location_title`, `_default_summary`, `_custom_summary`, `_change_folder`, `_reset_default`, `_migration_prompt_title/_message/_confirm/_decline`, `_migration_partial_failure`. **SAF permission-loss fallback notice text deferred** — `QueryStorageResolver` (PR-2) surfaces `RootResolution.Fallback(reason)` structurally, but no UI currently consumes/displays it (the List screen, PR-4, is where a user would notice the effect of a fallback — e.g. an unexpectedly-empty or different list); flagged as a gap to close in PR-4, not silently dropped
- [x] 9.2 Added the same 10 keys to all 10 locales (`values`, `es`, `ar`, `de`, `fr`, `hi`, `ja`, `pt-rBR`, `ru`, `zh-rCN`) — confirmed via `assembleDebug` succeeding (valid XML across all locale resource merges)
- [x] Ran `./gradlew test` and `./gradlew assembleDebug` — both succeed; 336 total tests, same 23 pre-existing unrelated failures, no new regressions
- [ ] 9.3 Every new string in the Settings storage row and migration dialog goes through `stringResource(...)` — run the android-dev hardcoded-`Text()` audit on the modified files, zero results

## Phase 10: Query Files List — ViewModel (TDD)

- [x] 10.1 RED: wrote `QueryFilesViewModelTest.kt` — 6 tests (sort with tiebreaker, Empty vs Error distinction, Error message, engine-in-context scoping, unknown connection, plus one for the fallback-notice addition below)
- [x] 10.2 GREEN: created `ui/screens/queryfiles/QueryFilesViewModel.kt` — `QueryFilesUiState` (`Idle/Loading/Success/Empty/Error`), injects `ListQueryFilesUseCase`, `ConnectionRepository`. **Addition beyond the literal task**: also injects `QueryStorageResolver` and exposes `showFallbackNotice: StateFlow<Boolean>` — this closes the gap flagged in PR-3/Phase 9 (`RootResolution.Fallback` had no UI consumer yet); re-checked on every `load()`/`refresh()`, never suppressed, matching the confirmed no-suppression decision
- [x] 10.3 Implemented `refresh(connectionId)` as a thin alias of `load(connectionId)` — same logic, exposed under its own name for call-site clarity at the resume hook

## Phase 11: Query Files List — Screen & Route

- [x] 11.1 Created `ui/screens/queryfiles/QueryFilesScreen.kt`
- [x] 11.2-11.4 Rendered `Success` (name + formatted `lastModified` via `SimpleDateFormat`), `Empty`, `Error`
- [x] 11.5 FAB calls `onOpenNewQueryOptions()` only — `NewQueryOptionsSheet` itself untouched, still owned by `MyDataBasesNavHost`
- [x] 11.6 Resume refresh implemented via `DisposableEffect` + `LifecycleEventObserver` on `LocalLifecycleOwner`, not `LifecycleEventEffect` — that API needs `androidx.lifecycle:lifecycle-runtime-compose`, not currently a dependency; the manual observer achieves the same behavior without adding one
- [x] 11.7 Uses `adaptivePadding(LocalWindowSizeClass.current)`
- [x] 11.8 Added `Routes.QueryFiles`
- [x] 11.9 Registered the route in `MyDataBasesNavHost.kt`

## Phase 12: Query Files List — Compose UI Test & Localization

- [ ] 12.1 **BLOCKED, not written**: same pre-existing `androidTest` source-set breakage documented in the `large-sql-script-execution` change (Phase 10.9) — still unresolved, not this change's job to fix
- [x] 12.2 Added to `res/values/strings.xml`: `query_files_title`, `query_files_empty_state`, `query_files_fab_description`, plus `query_storage_saf_fallback_notice` (the fallback-notice string, natural fit here since this screen is the first UI consumer)
- [x] 12.3 Added the same 4 keys to all 10 locales
- [x] 12.4 Every string in `QueryFilesScreen.kt` and the fallback banner goes through `stringResource(...)`

## Phase 13: Entry-Point Rewiring

- [x] 13.1-13.2 Both `MyDataBasesNavHost.kt` call sites (`onModalAction("new_query")`, `DatabaseActionMenuScreen.onNavigateToQueries`) now call `navController.navigate(Routes.QueryFiles.createRoute(...))` instead of `showNewQueryOptionsSheet = true`
- [x] 13.3 Confirmed: the `if (showNewQueryOptionsSheet) { ... }` sheet-render block itself is untouched; its only remaining trigger is the Phase 11.5 FAB callback

**Note**: Phase 13 was done together with Phase 11 in this same PR (not a separate PR-5 slice as tasks.md's original work-unit table suggested) — the route registration and its two callers are one cohesive, hard-to-split unit of work; splitting them would leave an intermediate state where the route exists but nothing navigates to it (or vice versa).

## Phase 14: Regression Check — `2026-06-30-new-query-modal-fix` Guarantees

- [x] 14.1 **Reasoned explicitly, not just asserted**: the original bug was `NewQueryScreen` being a non-rendering pseudo-destination (`LaunchedEffect` immediately calling `openQueryCard` with no UI of its own) wrapped in real navigation — that combination caused the blank-screen/wrong-menu/double-sheet symptoms. `QueryFilesScreen` is NOT that pattern: it's a genuine, fully-rendering destination (a real list UI) — using `navController.navigate` for it is the architecturally correct choice, not a regression of the fix. The fix's actual invariant (`NewQueryOptionsSheet` never wrapped in a route, always an overlay) still holds unchanged.
- [x] 14.2 Verified by reading the full `MyDataBasesNavHost.kt` diff: `showNewQueryOptionsSheet = true` appears in exactly one place — the Phase 11.5 FAB callback
- [x] 14.3 Confirmed: `NewQueryOptionsSheet`'s render block is unmodified, still a `WorkspaceOverlay`-level sibling, never a route — no double-sheet path exists

## Phase 15: Editor Save Convergence

> **Explicit scope boundary (confirmed)**: the pre-existing dead `ToolbarAction(id = "save", onClick = { /* TODO */ })` and `ToolbarAction(id = "open", onClick = { /* TODO */ })` stubs in `QueryEditorToolbarRow` (`QueryEditorScreen.kt` ~line 1000-1012) are OUT OF SCOPE for this change. Do not wire them. Only the real working Save path (the `ShortcutAction.Save` handler triggered by Ctrl+S / the Save shortcut, `QueryEditorScreen.kt` ~line 457-501) converges to `QueryFileStore` in this phase.

- [ ] 15.1 Modify `QueryEditorScreen.kt`'s `ShortcutAction.Save` branch: remove the inline `ContentValues`/`MediaStore`/`Environment.getExternalStorageDirectory()` legacy write entirely
- [ ] 15.2 Resolve the active connection's `DatabaseType` via `ConnectionRepository.getById(connectionId)?.type` (same repository/pattern the design specifies for engine-scoping)
- [ ] 15.3 Call `SaveQueryFileUseCase(engineType, fileName, sqlText.text)` → `QueryFileStore.write(...)`; on success keep the existing `savedFileName`/`showSaveDialog` UX (post-save dialog), on failure surface the existing error-log path with a user-visible message instead of silently swallowing (current `catch (e: Exception)` only logs — extend to also fail visibly, matching spec's "no silent discard" intent, kept in this phase since it's the same code path)
- [ ] 15.4 Confirm the explicit "Save As" export (`saveFileLauncher` / `CreateDocument` picker, `QueryEditorScreen.kt` ~line 274) is left COMPLETELY UNTOUCHED — verify by diff that no lines in that block change
- [ ] 15.5 Write a test at the appropriate testable boundary: since the file-write call site lives in the Composable per the project's established convention (I/O stays in the Screen layer, not the ViewModel — confirmed in `QueryEditorViewModel.kt`, mirrors the `large-sql-script-execution` Phase 9.3 precedent), add a unit test for `SaveQueryFileUseCase` (already covered by Phase 2.3/2.4) and document that the Composable-level wiring itself has no direct test coverage, consistent with the project's existing precedent for stateless/Composable-embedded I/O (e.g. `NewQueryOptionsSheet` has none either)

## Phase 16: Storage ADR & Final Audit

- [ ] 16.1 Create `.atl/architecture/decisions/ADR-003-query-file-storage.md`: documents the single-`QueryFileStoreImpl`-over-resolved-root decision, the `RootResolution` sealed contract, the always-surface (never suppressed) SAF fallback notice decision, and the engine-keyed `AppFolder` path layout — per `openspec/config.yaml` design rules requiring an ADR for this change
- [ ] 16.2 Run the android-dev hardcoded-`Text()` audit across ALL files touched by this change (Settings, QueryFiles screen, Editor); fix any stragglers before final commit
- [ ] 16.3 Run `./gradlew test` and `./gradlew assembleDebug`, confirm both succeed with no new regressions beyond documented pre-existing unrelated failures

## Phase 17: Manual Verification

> **BLOCKED, not executable in this automated session** — requires a physical device/emulator (`adb devices` expected empty in this environment) and/or removable SD card / real SAF picker interaction, mirroring the prior change's Phase 13 structure.

- [ ] 17.1 Run `./gradlew test` and verify every new unit test (AppFolder, use cases, resolver, `QueryFileStoreImpl`, Settings pref, migration, `QueryFilesViewModel`) passes — automatable, should be run before merge
- [ ] 17.2 Run `./gradlew assembleDebug` and verify the build succeeds — automatable, should be run before merge
- [ ] 17.3 Pick a SAF tree via the Settings "Change folder" picker, confirm persisted permission survives an app restart (`takePersistableUriPermission`) — requires device
- [ ] 17.4 Save a query from the editor for each of the 4 `DatabaseType` engines, confirm each lands in `MyDataBase/{engineType}/queries/` and appears in that engine's Query Files list without an app restart — requires device
- [ ] 17.5 Revoke the SAF grant (or remove the SD card) and confirm: fallback to app-private default, fallback notice shown, no crash — requires device + removable storage or a way to revoke a SAF grant
- [ ] 17.6 Confirm the SAF fallback notice is shown on EVERY subsequent resolve while the grant remains unavailable (not suppressed after the first showing) — requires device, directly verifies the confirmed no-suppression decision
- [ ] 17.7 Change the storage location with existing `.sql` files present: confirm the copy-prompt appears, Confirm copies without deleting originals, Decline leaves the new location empty and originals recoverable by reverting the pref — requires device
- [ ] 17.8 Change the storage location with an empty old location: confirm no prompt appears — requires device
- [ ] 17.9 Tap both "New Query" entry points (bottom-nav modal action and "Consultas" tile) from a real device: confirm both land on the Query Files list route, FAB opens the unchanged `NewQueryOptionsSheet`, no double sheet — requires device
- [ ] 17.10 Verify all new strings render correctly across at least English, Spanish, and one RTL locale (Arabic) on a real device — requires device
