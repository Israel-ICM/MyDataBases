# Proposal: Query Files List Screen + App-Managed Storage Location

## Intent

Both "New Query" entry points open the 3-option `NewQueryOptionsSheet` directly, so
the user has no persistent, browsable list of the `.sql` queries they create. This
change inserts a **Query Files list screen** between the entry points and the
selector. The screen lists `.sql` files from an app-managed folder (the folder IS the
source of truth — no metadata DB), with a floating "+" button that opens the existing
`NewQueryOptionsSheet` unchanged. A Settings preference lets the user redirect the
folder to any location (including a removable SD card) via SAF. The underlying
storage abstraction must be reusable for future app-managed folders without building
those folders now.

This solves a concrete product gap and, critically, forces convergence of the **four
divergent save destinations** the exploration found — otherwise the list would appear
perpetually empty even after the user "saves" a query.

## Scope

### In Scope
- New Query Files list screen: lists `.sql` files, empty state, FAB → existing selector.
- Reusable storage abstraction: `domain` interface (`QueryFileStore` over a generic
  `AppFolder` resolver **that takes a `DatabaseType` engine key on every path-resolving
  call**) + two `data` implementations (app-private `File`, SAF tree), unified behind a
  `DocumentFile`-shaped type exposing `.uri`. Storage is partitioned as
  `MyDataBase/{engineType}/queries/`.
- Settings preference: nullable SAF-tree URI string (`null` → app-private default),
  plus a Settings row ("Query storage location" + "Change folder" → `OpenDocumentTree`).
- Rewire BOTH entry points (`MyDataBasesNavHost.kt:141-147`, `:225-231`) to open the
  list screen instead of the selector flag.
- **Converge the QueryEditor Save shortcut** (`QueryEditorScreen.kt:457-499`, currently
  MediaStore/legacy) onto `QueryFileStore` so saved queries appear in the list.
- New ADR under `.atl/architecture/decisions/` for the storage abstraction.
- i18n for all new strings (es+en minimum; full locale set per android-dev skill).
- Add `androidx.documentfile:documentfile` dependency.

### Out of Scope (explicit)
- **Any other app-managed folder** (backups, exports, SSH keys). Build only `Queries`
  now; keep the resolver generic (engine-keyed) but ship a single folder type. Future
  folder types will reuse the same `MyDataBase/{engineType}/` partitioning without an
  API change.
- New Room/DataStore table tracking individual files (decision #5 — folder is truth).
- Changing `NewQueryOptionsSheet` internals or the open/run SAF `GetContent` pickers'
  behavior beyond the interplay decision below.
- Cross-connection query indexing/search, tagging, or rename/versioning of files.

## Capabilities

### New Capabilities
- `query-files-list`: Browsable list screen of `.sql` files backed by the managed
  folder, with FAB re-opening the existing selector; empty/unavailable states.
- `app-file-storage`: Reusable storage-location abstraction (`AppFolder` resolver +
  `QueryFileStore`) unifying app-private `File` and SAF-tree sources behind one type;
  Settings-driven location with persisted SAF permission and graceful fallback.

### Modified Capabilities
- None at the spec level yet (no prior `openspec/specs/` for query storage or the
  editor save flow). The QueryEditor save-destination convergence is delivered as part
  of `app-file-storage` rather than as a delta to an existing spec.

## Approach

1. **Storage abstraction** (`app-file-storage`): `domain/repositories/QueryFileStore`
   exposes `list(engine): List<QueryFileRef>`, `read(ref): String`, `write(engine, name,
   content): QueryFileRef`, `delete(ref)` — every path-resolving op takes the
   `DatabaseType` engine key. `QueryFileRef` carries `.uri`, `name`, `lastModified`.
   A generic `AppFolder` (sealed/enum, only `Queries` now) resolves via
   `AppFolder.resolve(engine, folder)` to the base
   `MyDataBase/{engine.name.lowercase()}/queries/` `DocumentFile` —
   `DocumentFile.fromFile(privateSubdir)` OR `DocumentFile.fromTreeUri(savedUri)` under
   the same `{engineType}/queries/` subpath — chosen by the Settings preference. One
   code path; consumers keep using `contentResolver.openInputStream(ref.uri)` (already
   compatible with both `file://` and `content://`).
2. **List screen** (`query-files-list`): ViewModel exposes `StateFlow<UiState>` (Idle/
   Loading/Success(list)/Error). FAB triggers the existing selector overlay.
3. **Entry-point rewiring**: both entry points navigate to the list screen; the FAB is
   the sole new trigger of `NewQueryOptionsSheet`.
4. **Settings**: extend `SettingsRepository` with `observe/setQueryStoragePath` (nullable
   URI string) + `takePersistableUriPermission` on pick.
5. **Save convergence**: replace the inline MediaStore/legacy write in the editor with a
   `QueryFileStore.write(...)` call; the explicit "Save As" `CreateDocument` picker
   remains for exporting elsewhere.

## Resolutions to the 8 Open Questions

**Q1 — Route vs overlay (list screen).** **Position: real navigable route**
(`Routes.QueryFiles = "connection/{connectionId}/query_files"`, contextual per
convention). Rationale: `RunScript` already sets the precedent for a full route that
passes opaque data via hoisted NavHost state rather than route args, and the list is a
full screen with a FAB — an overlay is the wrong shape. **The `2026-06-30` guarantee is
preserved because the selector itself stays a sibling overlay** triggered only by the
list's FAB; the two entry points now navigate to a route that is NOT the selector, so
the no-double-sheet / no-route-regression rule is not touched. *(Confirm)*

**Q2 — Abstraction shape & layer.** **Position:** interface `QueryFileStore` in
`domain/repositories/`; two impls in `data/repositories/` (`PrivateFileQueryStore`,
`SafTreeQueryStore`) selected by a `data`-level resolver reading the Settings pref;
`AppFolder` sealed type in `domain/models/`. `DocumentFile` is a `data`-layer detail —
`domain` sees only `QueryFileRef` (with `.uri`). Fits Clean Architecture and the
existing repository pattern.
**Engine-scoping amendment:** every path-resolving operation on `QueryFileStore` and
the underlying `AppFolder` resolver takes a `DatabaseType` parameter **from day one**
(e.g. `list(engine)`, `write(engine, name, content)`, and `AppFolder.resolve(engine,
folder)`). This is deliberate so that adding future folder types (e.g. `Backups`) or
new engines never forces a breaking API change — the engine dimension is baked into
the resolver signature, not bolted on later. `AppFolder` stays a sealed type (only
`Queries` now); the engine key is orthogonal and always required. *(Confirm)*

**Q3 — Do "New Query" saves auto-target this folder? (biggest decision.)**
**Position: YES.** The QueryEditor Save shortcut (and Save button) default-target
`QueryFileStore.write(...)`, i.e. the managed folder. The existing explicit **"Save As"
`CreateDocument` picker remains** for exporting to arbitrary locations. Rationale:
without convergence the list is perpetually empty and the feature is pointless; the
exploration flagged this as the single biggest risk. The legacy
`Environment.getExternalStorageDirectory()` path is **removed**, not extended.
*(Confirm — this changes existing save behavior; MediaStore-visible copies in
`Documents/MyDatabase/query` will no longer be produced by the default Save.)*

**Q4 — Open/Run file interplay (copy into folder?).** **Position: do NOT auto-copy.**
"Open Query File" / "Run Script" continue to pick arbitrary files via `GetContent` and
operate in place; they do NOT appear in the list unless the user explicitly saves them
into the managed folder (which the editor Save now does). Rationale: silently copying
every opened file into the managed folder is surprising and pollutes the list;
the source of truth stays "files the app wrote/created here." *(Confirm)*

**Q5 — Migration on location change.** **Position: offer to copy (not move), else start
fresh.** When the user picks a new location and `.sql` files exist in the old one, show a
one-time prompt: "Copy existing query files to the new location?" If declined, the new
location starts empty and old files remain orphaned but recoverable by reverting the
pref. Copy (not move) avoids destructive data loss if the new SAF grant is later lost.
Tradeoff: transient duplication vs. safety — we choose safety. *(Confirm)*

**Q6 — Scoping (per database engine type).** **Position: scope per `DatabaseType`,
not global and not per-connection.** Storage is partitioned by engine into
`MyDataBase/{engineType}/queries/` (see Q7). This is the **middle ground**: multiple
connections of the *same* engine share one folder (e.g. all MySQL connections →
`mysql/queries/`), while *different* engines are kept separate. Rationale: SQL dialects
are not portable across engines anyway (a Postgres query rarely runs unchanged on
SQLite), so partitioning by engine matches how users actually reuse queries and keeps
each engine's library clean. Per-connection scoping was rejected as too granular
(fragments the same-dialect library and multiplies SAF subfolders); pure-global was
rejected because it mixes non-portable dialects in one flat list. The scoping key is
the existing `DatabaseType` enum (`core/database/engine/DatabaseType.kt`:
`MYSQL`/`MARIADB`/`POSTGRESQL`/`SQLITE`). `connectionId` still flows into
`openQueryCard(connectionId, ...)` for execution context and is mapped to its
`DatabaseType` for storage scoping; the individual connection does not scope storage.
*(Confirm — this supersedes the earlier "global" answer.)*

**Q7 — Path layout, extension filter, listing.** **Position:** path is
**`MyDataBase/{engineType}/queries/`**, where `{engineType}` is
`DatabaseType.name.lowercase()` → `mysql` / `mariadb` / `postgresql` / `sqlite`
(lowercase recommended for filesystem-friendliness and cross-provider consistency; see
new decision #9). App-private default base:
`getExternalFilesDir(null)/MyDataBase/{engineType}/queries/`. The `queries` leaf stays
constant; the engine segment sits **above** it so future folder types share the same
`{engineType}/` parent (e.g. a hypothetical `MyDataBase/mysql/backups/`). List filters
to `.sql` (case-insensitive extension match); no MIME reliance for the private `File`
path. SAF path filters by name suffix.
**SAF-tree (user-redirected) case:** when the user redirects storage to a SAF tree, they
grant **one** tree root and the `{engineType}/queries/` subfolder structure is created
**under that single chosen root** (`root/mysql/queries/`, `root/postgresql/queries/`,
…). The user does **not** pick a separate tree per engine — one permission grant covers
all engines, which is simpler and keeps the migration/permission-loss model identical
to the app-private case. Missing per-engine subfolders are created lazily on first
write. *(Confirm single-root layout and `queries` leaf.)*

**Q8 — Sorting / metadata.** **Position:** since there is no DB, list shows file **name +
`lastModified`** (from `DocumentFile.lastModified()` / `File.lastModified()`), sorted by
recency (most-recent first) with name as tiebreaker. No "created vs loaded"
distinction is possible or attempted. *(Confirm.)*

**Bonus — SAF permission loss (exploration risk #3):** on list load and app start, if the
saved tree URI is unavailable (revoked, SD removed, data cleared), **fall back to the
app-private default**, surface a one-time notice, and keep the pref so the user can
re-pick. No crash, no empty-looking error state.

## Decisions Needing User Confirmation Before Design/Spec

1. **Q3 (save convergence)** — confirm default Save targets the managed folder and the
   legacy MediaStore `Documents/MyDatabase/query` behavior is removed (queries will no
   longer appear in the device Documents provider by default; "Save As" still exports).
2. **Q1** — confirm list screen is a real route (selector stays overlay), not itself an
   overlay.
3. **Q5** — confirm "copy on location change" prompt vs. simpler "start fresh, no prompt."
4. **Q6** — confirm **per-`DatabaseType`** folder scoping (same-engine connections
   share one folder; engines kept separate), superseding the earlier "global" answer.
5. **Q4** — confirm opened/run files are NOT auto-copied into the managed folder.
6. **Q7** — confirm the `MyDataBase/{engineType}/queries/` path layout (engine segment
   above a constant `queries` leaf), single SAF tree root, and `.sql`-only filter.
7. **Q2** — confirm layer placement (`domain` interface + `data` impls, `DocumentFile`
   as a `data` detail) **and** that path-resolving APIs take a `DatabaseType` parameter
   from day one (no breaking change when future folders/engines are added).
8. **Model A vs B default** — confirm app-private external (`getExternalFilesDir`, wiped
   on uninstall) is the acceptable default, with SAF as opt-in redirect.
9. **Engine-folder segment casing (NEW)** — confirm the exact naming of the
   `{engineType}` path segment: **lowercase** `mysql` / `mariadb` / `postgresql` /
   `sqlite` (from `DatabaseType.name.lowercase()`), recommended over the enum's
   upper-case `MYSQL`/… for filesystem-friendliness and cross-provider consistency. If
   you prefer the raw enum name or the `displayName` (`MySQL`, `MariaDB`, …), say so —
   this locks the on-disk folder names and must be settled before spec/design.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `ui/navigation/Routes.kt` | Modified | Add `QueryFiles` contextual route |
| `ui/navigation/MyDataBasesNavHost.kt` | Modified | Rewire 2 entry points; register route; selector overlay now FAB-triggered |
| `ui/screens/queryfiles/` (new) | New | List screen + ViewModel |
| `domain/repositories/QueryFileStore.kt` (new) | New | Storage interface |
| `domain/models/AppFolder.kt`, `QueryFileRef.kt` (new) | New | Engine-keyed folder resolver (`resolve(DatabaseType, folder)`) + file ref |
| `data/repositories/*QueryStore.kt` (new) | New | Two impls (private File, SAF tree) |
| `domain/repositories/SettingsRepository.kt` + Impl | Modified | `observe/setQueryStoragePath` |
| `ui/screens/settings/SettingsScreen.kt` + VM | Modified | Storage-location row + `OpenDocumentTree` |
| `core/di/` | Modified/New | Provide store impls + resolver |
| `ui/screens/queryeditor/QueryEditorScreen.kt` | Modified | Save converges to `QueryFileStore`; drop legacy path |
| `.atl/architecture/decisions/` | New | Storage-abstraction ADR |
| `res/values*/strings.xml` | Modified | New localized strings |
| `app/build.gradle(.kts)` | Modified | Add `androidx.documentfile` |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Overlay/route regression (`2026-06-30` guarantee) | Med | List is a route; selector stays overlay, FAB-triggered only; regression test |
| Save-convergence changes user-visible behavior (Q3) | High | Explicit user confirmation before design; keep "Save As" export |
| SAF persisted-permission loss | Med | Detect on load/start → fall back to private default + one-time notice |
| Uninstall wipes private-default files | Med | UX messaging; SAF redirect available for durable storage |
| `DocumentFile` tree listing slower than `File` | Low | Acceptable for small `.sql` lists; note for large dirs |
| i18n breadth (10 locales) | Med | Add strings across full locale set before commit (android-dev audit) |
| Engine-folder casing locked wrong on disk (Q9) | Low | Settle lowercase segment before spec/design; existing folders can't be auto-renamed after ship without a migration |
| SAF single-root × per-engine subfolders (permission/creation) | Low | Create `{engineType}/queries/` lazily under the one granted tree root; one grant covers all engines |

## Rollback Plan

The feature is additive and greenfield. To revert: (1) restore the two entry points to
set `showNewQueryOptionsSheet = true` directly; (2) remove the `QueryFiles` route,
list screen, store abstraction, and Settings row; (3) restore the original QueryEditor
Save shortcut (MediaStore/legacy) from git history; (4) drop the `androidx.documentfile`
dependency and the DataStore key. No schema/migration to unwind (no metadata table).
Because saved files are plain `.sql` on disk, no user data format is locked in.

## Dependencies

- `androidx.documentfile:documentfile` (new Gradle dependency).
- `activity-compose` already provides `OpenDocumentTree`/`GetContent` contracts.
- New storage ADR (per `openspec/config.yaml` design rules).

## Success Criteria

- [ ] Both entry points open the Query Files list screen (not the selector directly).
- [ ] FAB opens the unchanged `NewQueryOptionsSheet`; no double-sheet / route regression.
- [ ] A query saved from the editor appears in the list without app restart.
- [ ] Saving/listing is partitioned per engine: a MySQL-context query lands in
      `MyDataBase/mysql/queries/`, a Postgres one in `MyDataBase/postgresql/queries/`,
      and the list scoped to one engine does not show another engine's files.
- [ ] Path-resolving `QueryFileStore`/`AppFolder` APIs take a `DatabaseType` parameter
      (verified by signature; future folder types add no breaking change).
- [ ] Changing the Settings location redirects listing/saving to the chosen folder
      (incl. SD card) with `{engineType}/queries/` subfolders created under the single
      granted tree root, surviving process death via persisted SAF permission.
- [ ] SAF grant loss falls back to the app-private default without crashing.
- [ ] One `QueryFileStore` code path serves both storage models via `DocumentFile.uri`.
- [ ] All new user-facing strings localized (es+en minimum; full set per skill).
- [ ] `./gradlew test` and `./gradlew assembleDebug` pass (TDD per config).
