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

- [ ] 7.1 RED: write `MigrateQueryFilesUseCaseTest.kt` (fake `QueryFileStore` for old/new root pair, or Mockk `QueryFileStore` twice) — scans all 4 `DatabaseType` engine subfolders in the old location for `.sql` files, returns a no-op result (no prompt signal) when none exist, copies (via `read` then `write`, never delete) each found file into the new location preserving per-engine partitioning when confirmed, leaves originals untouched, reports per-file failures by count without aborting remaining copies
- [ ] 7.2 GREEN: create `domain/usecases/queryfiles/MigrateQueryFilesUseCase.kt` implementing the above contract
- [ ] 7.3 Modify Settings storage-location change handler: on `OpenDocumentTree` result, call `takePersistableUriPermission` before persisting via `setQueryStorageTreeUri(uri)`

## Phase 8: Settings — UI (Storage Row, Reset, Migration Prompt)

- [ ] 8.1 Modify `ui/screens/settings/SettingsScreen.kt` + its ViewModel: add "Query storage location" row showing current mode (app-private default vs. SAF path summary), "Change folder" action launching `OpenDocumentTree`, and a "Reset to default" action calling `setQueryStorageTreeUri(null)`
- [ ] 8.2 Wire `OpenDocumentTree` result: before persisting the new pref, invoke `MigrateQueryFilesUseCase` to check the old location for `.sql` files; if any exist, show the one-time copy-prompt dialog ("Copy existing query files to the new location?") with Confirm/Decline; persist the new pref regardless of the user's choice per spec
- [ ] 8.3 Wire "Reset to default" the same way: check the current SAF location for files before reverting to `null`, same copy-prompt flow, same persist-regardless behavior
- [ ] 8.4 Report partial-copy failures via a snackbar showing the failed-file count; succeeded files remain, old files stay untouched
- [ ] 8.5 Write/extend `SettingsViewModelTest.kt` (or equivalent) covering: prompt shown only when old location has `.sql` files, prompt skipped when old location is empty, confirm triggers copy, decline leaves new location empty without touching old files

## Phase 9: Settings & SAF-Fallback Localization

- [ ] 9.1 Add to `res/values/strings.xml`: storage-location row title, current-location summary label, "Change folder" action, "Reset to default" action, migration prompt title/message/confirm/decline, SAF permission-loss fallback notice text, partial-copy-failure snackbar message
- [ ] 9.2 Add the same keys translated to all 10 locales: `values-es`, `values-ar`, `values-de`, `values-fr`, `values-hi`, `values-ja`, `values-pt-rBR`, `values-ru`, `values-zh-rCN` (per android-dev skill's required locale set — confirmed as this project's actual shipped set)
- [ ] 9.3 Every new string in the Settings storage row and migration dialog goes through `stringResource(...)` — run the android-dev hardcoded-`Text()` audit on the modified files, zero results

## Phase 10: Query Files List — ViewModel (TDD)

- [ ] 10.1 RED: write `QueryFilesViewModelTest.kt` — `Idle → Loading → Success(list)` for a populated folder sorted `lastModified` descending with name tiebreaker; empty-but-available folder maps to a distinct empty state (not `Error`); store failure maps to `Error` with a localized message; non-`.sql` files never appear (defense-in-depth even though `QueryFileStore.list` already filters); engine-in-context scoping — resolves `DatabaseType` via `ConnectionRepository.getById(connectionId).type` and calls `list(that engine)`; SAF-fallback path (store returns fallback-folder contents) still renders `Success`/empty, never `Error`
- [ ] 10.2 GREEN: create `ui/screens/queryfiles/QueryFilesViewModel.kt` — `@HiltViewModel` injecting `ListQueryFilesUseCase`, `ConnectionRepository`; sealed `QueryFilesUiState` (`Idle`, `Loading`, `Success(files: List<QueryFileInfo>)`, `Empty`, `Error(message: String)`); exposes `StateFlow<QueryFilesUiState>`; remains `Context`-free; listing runs off the main thread (`viewModelScope` + repository's own dispatcher)
- [ ] 10.3 Implement `refresh()` callable on screen resume (not just initial load) — no folder-watching, refresh-on-resume only per spec

## Phase 11: Query Files List — Screen & Route

- [ ] 11.1 Create `ui/screens/queryfiles/QueryFilesScreen.kt`: composable accepting `connectionId: String`, `onOpenNewQueryOptions: () -> Unit` (FAB callback), `onNavigateBack: () -> Unit`; hoists `QueryFilesViewModel` via `hiltViewModel()`
- [ ] 11.2 Render `Success`: list rows showing `name` + formatted `lastModified`, sorted as delivered by the ViewModel
- [ ] 11.3 Render `Empty`: empty-state content (copy + FAB still available)
- [ ] 11.4 Render `Error`: localized error message content
- [ ] 11.5 Add FAB: single trigger calling `onOpenNewQueryOptions()` — does NOT open `NewQueryOptionsSheet` directly from this file; the sheet stays owned/rendered at `MyDataBasesNavHost` level (already hoisted there since `large-sql-script-execution`), so this callback only flips the existing `showNewQueryOptionsSheet` state — no sheet-rendering code duplicated or moved
- [ ] 11.6 Call `viewModel.refresh()` on screen resume (`LifecycleEventEffect(Lifecycle.Event.ON_RESUME)` or existing project convention for resume hooks) so a query saved from the editor appears without an app restart
- [ ] 11.7 Layout uses `adaptivePadding(LocalWindowSizeClass.current)` for Compact/Medium/Expanded per existing project helper
- [ ] 11.8 Add `Routes.QueryFiles` to `ui/navigation/Routes.kt`: `data object QueryFiles : Routes("connection/{connectionId}/query_files")` with `createRoute(connectionId: String)`, following the existing single-arg contextual route pattern
- [ ] 11.9 Register `composable(Routes.QueryFiles.route, ...)` in `MyDataBasesNavHost.kt`: reads `connectionId`, renders `QueryFilesScreen(connectionId, onOpenNewQueryOptions = { showNewQueryOptionsSheet = true }, onNavigateBack = { navController.popBackStack() })`

## Phase 12: Query Files List — Compose UI Test & Localization

- [ ] 12.1 Write `QueryFilesScreenTest.kt` (Compose UI test, populated per project's existing androidTest limitation) covering: load → list renders → FAB click invokes `onOpenNewQueryOptions` (not a direct sheet render) happy path, and the empty-state rendering
- [ ] 12.2 Add to `res/values/strings.xml`: screen title, empty-state copy, FAB content description, per-item accessibility label if needed
- [ ] 12.3 Add the same keys translated to all 10 locales (`values`, `values-es`, `values-ar`, `values-de`, `values-fr`, `values-hi`, `values-ja`, `values-pt-rBR`, `values-ru`, `values-zh-rCN`)
- [ ] 12.4 Every string in `QueryFilesScreen.kt` goes through `stringResource(...)` — zero hardcoded `Text()` calls, confirmed via android-dev audit

## Phase 13: Entry-Point Rewiring

- [ ] 13.1 Modify `MyDataBasesNavHost.kt`'s `onModalAction` `"new_query"` branch (currently `showNewQueryOptionsSheet = true`, ~line 141-147): change to `navController.navigate(Routes.QueryFiles.createRoute(activeConnectionId))`
- [ ] 13.2 Modify `MyDataBasesNavHost.kt`'s `DatabaseActionMenuScreen` `onNavigateToQueries` callback (currently `showNewQueryOptionsSheet = true`, ~line 225-231): change to `navController.navigate(Routes.QueryFiles.createRoute(connectionId))`
- [ ] 13.3 Confirm the `NewQueryOptionsSheet` render block (the `if (showNewQueryOptionsSheet) { ... }` sibling inside `WorkspaceOverlay`) is left UNMODIFIED — it now becomes reachable only via the Phase 11 FAB callback, not via these two rewired call sites

## Phase 14: Regression Check — `2026-06-30-new-query-modal-fix` Guarantees

- [ ] 14.1 Write/extend a NavHost-level test (or targeted unit test on the composable logic, matching existing project precedent for this kind of check) verifying: both "new_query" modal action and the "Consultas" tile now trigger `navController.navigate` to `Routes.QueryFiles`, NEITHER sets `showNewQueryOptionsSheet = true` directly anymore
- [ ] 14.2 Verify by code inspection (documented in the PR description, same as `large-sql-script-execution` Phase 20 precedent) that `showNewQueryOptionsSheet` is set to `true` ONLY from the Phase 11.9 FAB-forwarded callback — no other call site remains
- [ ] 14.3 Confirm no double-sheet / no route-regression is possible by construction: the sheet still renders as a `WorkspaceOverlay`-level sibling (never a route), preserving the exact guarantee structure the reference change established

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
