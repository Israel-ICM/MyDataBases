# Exploration: query-files-storage

> Status: exploration only. No decisions locked, no proposal/spec/design. This document
> grounds the next proposal phase in the real codebase and enumerates the feasibility,
> integration points, risks, and open questions.

## Goal (as gathered)

Insert a **query files list screen** between the two existing "New Query" entry points
and the existing 3-option selector (`NewQueryOptionsSheet`). Today both entry points open
the selector directly. The desired flow becomes:

```
entry point  ->  Query Files List screen (lists .sql files in a folder)
                    └── floating "+" button  ->  existing NewQueryOptionsSheet (3 options)
```

The folder on device storage is the **source of truth** for the list (no new Room/DataStore
metadata table tracking individual files). A Settings preference lets the user redirect this
folder to a user-chosen location (SD card / SAF tree). The storage-location abstraction must
generalize to future app-managed folders (backups/exports later), without over-building them now.

## Current State (grounded in code)

### Two entry points to the selector — both need to change

Both currently flip the same hoisted flag `showNewQueryOptionsSheet = true`:

1. **Bottom-nav modal action** — `MyDataBasesNavHost.kt:141-147`, `onModalAction("new_query")`.
2. **`DatabaseActionMenuScreen` "Consultas" tile** — `MyDataBasesNavHost.kt:225-231`,
   `onNavigateToQueries`.

The selector itself is rendered as a **sibling overlay** of `AdaptiveNavigationScaffold`
(`MyDataBasesNavHost.kt:449-458`), deliberately NOT nested in `DatabasesListScreen`/
`TablesListScreen` because `new_query` is a modal action declared in both menus. Comment at
lines 443-448 documents this and the `2026-06-30-new-query-modal-fix` guarantee (no route
change, no double sheet). **Any list screen we insert must respect this** — either it becomes
a real navigable route, or it stays an overlay; mixing will risk regressing that guarantee.

### `NewQueryOptionsSheet` (the 3-option selector — unchanged target)

`ui/screens/databases/NewQueryOptionsSheet.kt` — stateless `ModalBottomSheet`, 3 rows
(New Query / Open Query File / Run Script), callbacks hoisted to the NavHost. The three
callbacks currently wired (`MyDataBasesNavHost.kt:450-457`):
- `onNewQuery` → `workspaceManager.openQueryCard(connectionId, initialSql = null)`
- `onOpenQueryFile` → `openQueryFileLauncher.launch("*/*")` (GetContent, line-threshold guard)
- `onRunScript` → `runScriptLauncher.launch("*/*")` (GetContent → RunScript route)

The selector does not need to change internally; the "+" FAB on the new list screen simply
triggers the same overlay.

### Existing SAF usage — single-file only, must coexist (not be replaced)

- `MyDataBasesNavHost.kt:85-116` — two `GetContent()` launchers (open query file / run script).
- `QueryEditorScreen.kt:254-290` — `openFileLauncher` (`GetContent()`) and `saveFileLauncher`
  (`CreateDocument("text/plain")`).
- `QueryEditorScreen.kt:457-499` — the **Save shortcut** writes via **`MediaStore`** to
  `Documents/MyDatabase/query` (API 29+) or `Environment.getExternalStorageDirectory()/MyDatabase/query`
  (legacy). This is a THIRD, divergent save location and is neither app-private nor SAF-tree.

> **Key observation:** there are already THREE inconsistent file destinations in play
> (SAF `CreateDocument` picker, MediaStore `Documents/MyDatabase/query`, legacy external dir).
> None of them is the app-private folder this feature proposes. The list screen's "source of
> truth folder" will be a FOURTH location unless the proposal decides to converge them.

### Storage precedents in the codebase

- **`CertificateReader.kt:34,97`** — uses `context.cacheDir/ssl-certs` (app-private CACHE,
  not `getExternalFilesDir`). This is the only `File`-based app-private precedent, and it is
  cache (evictable), not a durable files location.
- **No** usage anywhere of `getExternalFilesDir`, `filesDir` (durable), `OpenDocumentTree`,
  `DocumentFile`, or `takePersistableUriPermission`. Verified by content search across
  `app/src/main/java`. This feature is **fully greenfield** for both durable app-private
  storage and SAF-tree storage.
- **No SSH-key file storage** found (ssh tunneling handles keys differently; no on-disk
  key folder precedent to mirror).

### Settings / DataStore pattern (where the new preference plugs in)

- `domain/repositories/SettingsRepository.kt` — interface, currently
  `observe/setBrandedPaletteEnabled` + `observe/setThemeMode`. Explicitly comments "Future:
  language, etc." — designed to grow.
- `data/repositories/SettingsRepositoryImpl.kt` — `DataStore<Preferences>`,
  `booleanPreferencesKey` / `stringPreferencesKey` pattern. A **`stringPreferencesKey`
  storing a persisted SAF tree URI** (or null → app-private default) fits this exactly.
- `core/di/SettingsModule.kt` — single `DataStore<Preferences>` singleton ("settings").
- `ui/screens/settings/SettingsScreen.kt` + `SettingsViewModel.kt` — Switch + segmented
  selector pattern; a new "Query storage location" row (showing current path + a
  "Change folder" action launching `OpenDocumentTree`) plugs in here cleanly.

### Architecture conventions (`.atl/architecture/`)

- ADR-001 (phased implementation), ADR-002 (design tokens). **No existing storage/
  file-management ADR.** Per `openspec/config.yaml` design rules, this change SHOULD
  produce a new ADR under `.atl/architecture/decisions/` for the storage abstraction.
- Config confirms: Clean Architecture (presentation/domain/data), Hilt DI mandatory,
  Strict TDD (`tdd: true`, `./gradlew test`), localized strings es+en required for all
  user-facing text, adaptive behavior per WindowSizeClass required for UI.

## Feasibility Assessment — two storage models

### Model A: app-private external (`getExternalFilesDir`)
- No runtime permission; app-private; **cleared on uninstall**; `File`-based API.
- Trivial to list (`File.listFiles { it.extension == "sql" }`), read, write.
- Default location the user asked for. Zero permission friction.
- Downside: not user-visible in a file manager without effort; wiped on uninstall.

### Model B: user-chosen SAF tree (`OpenDocumentTree` + `takePersistableUriPermission`)
- One-time system picker; persisted URI permission **survives process death and reboots**
  (must re-take on each pick; persisted grants are capped by the OS, ~128 by default).
- Works across storage volumes **including removable SD cards** — satisfies requirement #3.
- `File` APIs do NOT work on a `content://` tree URI. Listing/reading/writing requires either
  `DocumentFile` (simple, slower) or the `DocumentsContract` query API (faster, more code).
- **Requires adding the `androidx.documentfile:documentfile` dependency** — NOT currently in
  the Gradle files (verified). `activity-compose:1.10.1` already provides the
  `OpenDocumentTree` contract, so no new activity-result dependency is needed.

### Can ONE abstraction unify both? — realistic, with a caveat
A domain-level abstraction (e.g. `QueryFileStore` / `AppFileLocation`) exposing
`list(): List<QueryFileRef>`, `read(ref): String`, `write(name, content): QueryFileRef`,
`delete(ref)` is realistic. Two implementations (private-`File` vs SAF-`DocumentFile`) sit
behind it, selected by the persisted Settings preference.

- **`DocumentFile` CAN represent both**: `DocumentFile.fromFile(File)` wraps an app-private
  `File`, and `DocumentFile.fromTreeUri(context, uri)` wraps a SAF tree. So the list screen
  and read/write logic can operate on a single `DocumentFile`-shaped type with ONE code path.
  This is the strongest unification lever and worth validating in design.
- **Caveat / open question:** the app's existing open/run flows consume a `Uri` fed to
  `contentResolver.openInputStream(uri)` (works for both `file://` and `content://`), while
  the Run Script flow (`pendingScriptUri`) also expects a `Uri`. A `DocumentFile`-based
  abstraction should expose `.uri` so it plugs into the EXISTING `openInputStream`-based
  consumers without a second divergent path. This looks compatible but must be proven in design.

### Reuse-oriented shape (requirement #4, without over-building)
Model the abstraction around a **named location** concept, e.g. an enum/sealed
`AppFolder { Queries, /* future: Backups, Exports */ }` resolved to a concrete base
`DocumentFile` (private subdir OR SAF child). Build ONLY `Queries` now, but keep the resolver
and the store generic so future folders reuse it. Do NOT introduce backups/exports code now.

## Integration Points

| Concern | Existing artifact | Integration |
|---|---|---|
| Entry point 1 | `MyDataBasesNavHost.kt:141-147` | Navigate to list screen instead of setting the sheet flag |
| Entry point 2 | `MyDataBasesNavHost.kt:225-231` | Same — route to list screen |
| Selector reuse | `NewQueryOptionsSheet.kt` | Unchanged; triggered by list screen "+" FAB |
| Overlay guarantee | `MyDataBasesNavHost.kt:443-458` | Preserve no-double-sheet / no-route-regression rule |
| New route | `Routes.kt` | Add `QueryFiles` route (contextual `connection/{id}/...` per convention) |
| Preference | `SettingsRepository(+Impl)` | Add `observe/setQueryStoragePath` (nullable SAF URI string) |
| Settings UI | `SettingsScreen.kt` | New row: current location + "Change folder" (OpenDocumentTree) |
| DI | `SettingsModule.kt` + new module | Provide the store impl(s); resolve default via `ApplicationContext` |
| Store impl | none (greenfield) | New `data/` classes; add `androidx.documentfile` dependency |
| ADR | `.atl/architecture/decisions/` | New ADR for the storage abstraction |
| i18n | `strings.xml` (es+en, +8 locales per android-dev skill) | List screen, empty state, Settings row, errors |

## Risks

1. **Overlay-vs-route regression.** Inserting a list screen must not reintroduce the
   double-sheet / route-change bug guarded by `2026-06-30-new-query-modal-fix`
   (`MyDataBasesNavHost.kt:443-448`). Decide route-vs-overlay carefully.
2. **Four divergent save destinations.** The QueryEditor Save shortcut writes to MediaStore
   `Documents/MyDatabase/query` (`QueryEditorScreen.kt:461-496`), separate from the SAF
   `CreateDocument` picker and separate from this feature's folder. If saves don't target the
   new folder, the list will look empty/stale even after the user "saved" a query. High
   confusion risk — needs an explicit decision (see open questions).
3. **SAF permission loss.** Persisted URI grants can be revoked (folder deleted, SD card
   removed, OS grant cap exceeded, app data cleared). The list screen needs a graceful
   "location unavailable → re-pick or fall back to default" path.
4. **New dependency.** `androidx.documentfile` must be added; `DocumentFile` tree listing is
   noticeably slower than `File.listFiles` for large folders (per-entry IPC). Likely fine for
   a `.sql` list, but worth noting for large directories.
5. **Uninstall data loss (Model A default).** App-private external is wiped on uninstall;
   users may lose "saved" queries unknowingly. UX/messaging consideration.
6. **Scoped storage correctness.** `getExternalFilesDir` needs no permission (good), but the
   legacy `Environment.getExternalStorageDirectory()` path in the current editor Save is
   fragile on modern Android and should not be extended by this feature.
7. **i18n breadth.** android-dev skill mandates 10 locales; config mandates es+en minimum.
   All new user-facing strings must be added across the required locale set.

## Open Questions for Proposal / Design

1. **Route vs overlay:** should the query files list be a real `NavHost` destination
   (`Routes.QueryFiles`) or an overlay like the current selector? Real route is cleaner for a
   full screen with a FAB, but must preserve the two-entry-point + no-double-sheet guarantee.
2. **Abstraction shape:** confirm `DocumentFile`-backed `QueryFileStore` with a generic
   `AppFolder` resolver (Queries only now). Exact interface (`list/read/write/delete`) and
   whether it lives in `domain` (interface) + `data` (two impls) per Clean Architecture.
3. **Do "New Query" saves auto-target this folder?** If yes, converge the QueryEditor Save
   shortcut (currently MediaStore) onto the same store so saved queries appear in the list.
   If no, the list only shows externally-placed/opened files and may confuse users. This is
   the single biggest product decision.
4. **"Open Query File" / "Run Script" interplay:** when a user opens an arbitrary file via the
   existing GetContent pickers, does it get copied into the managed folder (so it shows up in
   the list), or does the list only reflect files already inside the folder? Requirement #1
   says the folder is the source of truth — clarify whether "loaded previously" implies a copy.
5. **Migration when the user switches location:** if the user redirects the folder while
   `.sql` files already exist in the old (default) location, do we (a) offer to copy/move
   existing files, (b) leave them behind, or (c) merge views? Requirement is silent; needs a
   decision + possibly a one-time migration prompt.
6. **Connection scoping:** query files today flow through `connectionId`
   (`openQueryCard(connectionId, ...)`). Is the query folder global (all connections) or
   scoped per connection/database? Requirement implies global; confirm.
7. **Default subdirectory name & MIME/extension filter:** confirm `.sql`-only listing and the
   default subfolder name under `getExternalFilesDir` (e.g. `queries/`).
8. **Sorting / metadata for the list:** since there's no DB tracking, sort by file
   lastModified/name from the folder itself — confirm acceptable (no "created vs loaded"
   distinction is possible without metadata).

## Ready for Proposal

**Yes.** The feature is technically feasible and greenfield. A unifying `DocumentFile`-based
abstraction is realistic and plugs into existing `openInputStream`-based consumers, the
DataStore Settings pattern, and the two entry points. The proposal must resolve the 8 open
questions above — most importantly (Q3) whether query saves converge onto this folder, since
that determines whether the list is actually useful or perpetually appears empty.
