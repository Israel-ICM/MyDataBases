# Design: Query Files List Screen + App-Managed Storage Location

## Technical Approach

One `QueryFileStore` code path serves both storage models. A `QueryStorageResolver`
(data layer) resolves a root `DocumentFile` — either `DocumentFile.fromFile(getExternalFilesDir)`
or `DocumentFile.fromTreeUri(savedUri)` — based on the nullable SAF-tree preference in
`SettingsRepository`. `QueryFileStoreImpl` then does all list/read/write/delete purely
via `DocumentFile` APIs against `{root}/{engineType}/queries/`, so private vs. SAF is a
resolver-time decision, not two duplicated store implementations.

## Architecture Decisions

| Decision | Option chosen | Alternative rejected | Rationale |
|---|---|---|---|
| Store impl count | **One** `QueryFileStoreImpl` parameterized by `QueryStorageRootProvider` | Two full impls (`PrivateFileQueryStore`/`SafTreeQueryStore`) | Both eventually operate on a `DocumentFile` root; duplicating list/copy/sort logic violates the proposal's "one code path" success criterion |
| Root selection | `QueryStorageResolver.resolveRoot(): RootResolution` sealed result (`Resolved`/`Fallback`) | Plain `DocumentFile?` return | Callers need to distinguish "using SAF normally" from "fell back to private" to surface the one-time notice |
| Engine-segment logic | Pure function `AppFolder.segmentFor(DatabaseType): String` in `domain/models/` | Inline `.name.lowercase()` in the store impl | Keeps casing rule (decision #6) JVM-unit-testable without touching `DocumentFile` |
| Permission-loss check timing | Inside `resolveRoot()`, called on every list-screen load + app start (`MainActivity` init) | Only on app start | SAF revocation can happen mid-session (SD card removed); checking on load is cheap (`DocumentFile.exists()`) |
| Notice persistence | In-memory, one-shot per resolver instance (not persisted to DataStore) | Persisted "seen" flag | Simplest; re-shown after process death is acceptable UX, avoids a new pref (flag as open question) |

## Data Flow

    QueryFilesViewModel ──resolve(connectionId)──▶ ConnectionRepository.getById()
           │                                              │ .type: DatabaseType
           ▼                                              ▼
    QueryFileStore.list(engineType) ──▶ QueryStorageResolver.resolveRoot()
           │                                  │
           │                     private ◀────┴────▶ SAF tree (Settings pref)
           ▼
    DocumentFile.listFiles() filtered ".sql" ▶ sort by lastModified desc ▶ UiState.Content

Editor save: `QueryEditorViewModel` resolves `DatabaseType` from `connectionId` (already
needed for query execution) → `SaveQueryFileUseCase(engineType, fileName, content)` →
`QueryFileStore.write(...)`.

## File Changes

| File | Action | Description |
|---|---|---|
| `domain/repositories/QueryFileStore.kt` | Create | `list/read/write/delete` interface |
| `domain/models/QueryFileInfo.kt` | Create | `name`, `uri`, `lastModified` |
| `domain/models/AppFolder.kt` | Create | Sealed (`Queries` only) + `segmentFor(DatabaseType)` pure fn |
| `domain/usecases/queryfiles/*UseCase.kt` | Create | `ListQueryFilesUseCase`, `SaveQueryFileUseCase`, `DeleteQueryFileUseCase` |
| `data/repositories/QueryFileStoreImpl.kt` | Create | Single impl over resolved root `DocumentFile` |
| `data/storage/QueryStorageResolver.kt` | Create | Root selection + permission-loss fallback |
| `data/storage/QueryStorageRootProvider.kt` | Create | `Private`/`Saf` provider variants |
| `domain/repositories/SettingsRepository.kt` + Impl | Modify | Add `observe/setQueryStorageTreeUri(Uri?)` |
| `ui/screens/settings/SettingsScreen.kt` + VM | Modify | Storage row, `OpenDocumentTree`, migration prompt |
| `ui/screens/queryfiles/QueryFilesScreen.kt` + VM | Create | List route + FAB → `NewQueryOptionsSheet` |
| `ui/navigation/Routes.kt` | Modify | Add `QueryFiles` (`connection/{connectionId}/query_files`) |
| `ui/navigation/MyDataBasesNavHost.kt` | Modify | Both entry points navigate; register route; sheet trigger moves |
| `ui/screens/queryeditor/QueryEditorScreen.kt` | Modify | Ctrl+S calls `SaveQueryFileUseCase`; drop `ContentValues`/`MediaStore` |
| `core/di/RepositoryModule.kt` | Modify | `@Binds bindQueryFileStore` |
| `core/di/QueryStorageModule.kt` | Create | `@Provides` root providers |
| `.atl/architecture/decisions/ADR-003-*.md` | Create | Storage abstraction ADR |

## Interfaces

```kotlin
interface QueryFileStore {
    suspend fun list(engineType: DatabaseType): Result<List<QueryFileInfo>>
    suspend fun read(uri: Uri): Result<String>
    suspend fun write(engineType: DatabaseType, fileName: String, content: String): Result<Uri>
    suspend fun delete(uri: Uri): Result<Boolean>
}

sealed class RootResolution {
    data class Resolved(val root: DocumentFile) : RootResolution()
    data class Fallback(val root: DocumentFile, val reason: String) : RootResolution()
}
```

## Testing Strategy

| Layer | What | Approach |
|---|---|---|
| Unit (JVM) | `AppFolder.segmentFor`, use cases, `QueryFilesViewModel` sorting/state | Mockk `QueryFileStore`, same pattern as `CreateFolderUseCaseTest` |
| Unit (JVM) | `SettingsRepositoryImpl` new pref | `FakeDataStore`, per existing `SettingsRepositoryImplTest` |
| Instrumented | `QueryFileStoreImpl` (`DocumentFile`/`ContentResolver` I/O), migration copy | `app/src/androidTest/` — **same limitation as prior changes**: populated but not executed by the agent (no `./gradlew connectedAndroidTest` per HARD RULE) |

**Honesty check**: root resolution and permission-loss detection unavoidably touch
`DocumentFile`/`ContentResolver` and stay in `data/` — but engine-segment naming, sort
order, and UI state derivation are extracted as pure functions specifically so they
don't require instrumentation. No unnecessary logic pushed into `data/` beyond
unavoidable `DocumentFile` calls.

## Migration / Rollout

Settings location change (or revert) triggers: (1) `OpenDocumentTree` + persist
`takePersistableUriPermission`; (2) scan old root's 4 engine subfolders for `.sql`
files; (3) if any exist, prompt "Copy existing query files to the new location?";
(4) on confirm, copy (not move) each file via `read(oldUri)` → `write(newEngine, name,
content)`; (5) persist new pref regardless of the copy choice; (6) partial-failure
files are reported by count in a snackbar, succeeded files remain, old files stay
untouched (recoverable by reverting the pref).

## Open Questions

- [ ] Should the SAF-fallback one-time notice be persisted (survive process death) or is in-memory-per-session acceptable?
- [ ] Toolbar "Save"/"Open" buttons in `QueryEditorScreen` are currently dead stubs — wire them to the same `SaveQueryFileUseCase`/`QueryFileStore` in this change, or defer (out of stated scope)?
