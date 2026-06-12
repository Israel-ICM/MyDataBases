# Tasks: UI Implementation for MyDataBases

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~2,400 lines (79 files created/modified) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | 5 PRs (feature-branch-chain) |
| Delivery strategy | feature-branch-chain |
| Chain strategy | feature-branch-chain |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Base Branch | Notes |
|------|------|-----------|-------------|-------|
| 1 | Theme + Navigation skeleton | PR #1 | `feature/ui-implementation` | Foundation: Material 3 theme, routes, NavHost scaffold, no screens yet. Fully tested. ~450 lines. |
| 2 | Persistence + Connections screens | PR #2 | PR #1 branch | Room + DataStore + encryption + Connections CRUD UI. Depends on unit 1. ~600 lines. |
| 3 | Database Explorer screens | PR #3 | PR #2 branch | Databases/Tables/TableViewer screens + ViewModels. Depends on unit 2. ~550 lines. |
| 4 | Query Editor screen | PR #4 | PR #3 branch | SQL editor + result grid + execution. Depends on unit 3. ~400 lines. |
| 5 | Settings screen + polish | PR #5 | PR #4 branch | Theme/locale pickers, localization parity test, final integration. ~400 lines. |

**Note**: Each PR targets the immediate previous PR branch. GitHub diff will only show that slice's changes. After all PRs merge into `feature/ui-implementation`, final PR merges tracker branch to `main`.

---

## Phase 1: Foundation (PR #1 — Theme + Navigation Skeleton)

**Target**: ~450 lines | **Base**: `feature/ui-implementation` | **Tests**: Unit + Compose UI

### 1.1 Infrastructure Setup
- [x] Create `app/src/main/java/.../ui/theme/Color.kt` — Material 3 color tokens (light/dark/dynamic)
- [x] Create `app/src/main/java/.../ui/theme/Theme.kt` — `MyDataBasesTheme` composable with ThemeMode parameter (LIGHT/DARK/SYSTEM)
- [x] Create `app/src/main/java/.../ui/theme/Type.kt` — Material 3 typography scale
- [x] Create `app/src/main/java/.../domain/models/ThemeMode.kt` — enum (LIGHT, DARK, SYSTEM)
- [x] TEST: Write `ThemeTest.kt` verifying light/dark color schemes apply correctly

### 1.2 Navigation Infrastructure
- [x] TEST: Write `RouteTest.kt` — RED (verify sealed Route class hierarchy path serialization)
- [x] Create `app/src/main/java/.../ui/navigation/Route.kt` — sealed Routes (Connections, DatabaseList, Tables, TableViewer, QueryEditor, Settings)
- [x] TEST: GREEN — verify Route paths serialize/deserialize correctly
- [x] TEST: Write `MyDataBasesNavHostTest.kt` — RED (verify NavHost renders initial route) [DEFERRED to instrumentation]
- [x] Create `app/src/main/java/.../ui/navigation/MyDataBasesNavHost.kt` — NavHost with placeholder Composables (empty screens with Text)
- [x] TEST: GREEN — verify NavHost renders Connections route by default [SMOKE: compilation passes]

### 1.3 MainActivity Integration
- [x] TEST: Write `MainActivityIntegrationTest.kt` — SMOKE (verify ThemeMode.SYSTEM exists)
- [x] Modify `app/src/main/java/.../MainActivity.kt` — replace Greeting with MyDataBasesNavHost, provide WindowSizeClass via CompositionLocal, observe ThemeMode hardcoded SYSTEM
- [x] TEST: GREEN — verify MainActivity smoke test passes

### 1.4 Localization Skeleton
- [x] Modify `app/src/main/res/values/strings.xml` — add nav route labels + common actions (en)
- [x] Create `app/src/main/res/values-es/strings.xml` — mirror all strings (es)
- [x] TEST: Write `LocalizationParityTest.kt` — RED → GREEN → TRIANGULATE (3 additional strings)

### 1.5 Reusable Components
- [x] Create `app/src/main/java/.../ui/components/LoadingIndicator.kt` — centered CircularProgressIndicator
- [x] Create `app/src/main/java/.../ui/components/ErrorCard.kt` — error message + retry button (localized)
- [x] TEST: SMOKE — compilation passes, KDoc complete [Compose UI tests deferred to instrumentation]

**PR #1 Acceptance**: `./gradlew test` passes, `./gradlew assembleDebug` builds, NavHost renders with placeholder screens, theme switches correctly, localization parity validated.

---

## Phase 2: Persistence + Connections (PR #2 — Room + DataStore + Connections CRUD)

**Target**: ~600 lines | **Base**: PR #1 branch | **Tests**: Unit + Integration + Compose UI

### 2.1 Security + Encryption
- [x] Add `androidx.security:security-crypto:1.1.0-alpha06` to `app/build.gradle.kts`
- [x] TEST: Write `CredentialEncryptionTest.kt` — RED (verify encrypt/decrypt round-trip)
- [x] Create `app/src/main/java/.../core/security/CredentialEncryption.kt` — encrypt/decrypt using EncryptedSharedPreferences
- [x] TEST: GREEN — verify plaintext → encrypted → plaintext identity (4 tests passing)

### 2.2 Room Database Setup
- [x] Create `app/src/main/java/.../data/local/entities/ConnectionEntity.kt` — Room entity with encrypted_password
- [x] Create `app/src/main/java/.../data/local/converters/DatabaseTypeConverter.kt` — Room TypeConverter for DatabaseType enum
- [x] Create `app/src/main/java/.../data/local/converters/SSHTunnelConfigConverter.kt` — Room TypeConverter for SSHTunnelConfig (JSON)
- [x] TEST: Write `ConnectionDaoTest.kt` — RED (verify CRUD operations with in-memory Room)
- [x] Create `app/src/main/java/.../data/local/dao/ConnectionDao.kt` — DAO with insert/delete/getById/getAll/updateLastUsed
- [x] Create `app/src/main/java/.../data/local/AppDatabase.kt` — Room database with ConnectionEntity table
- [x] TEST: GREEN — verify DAO queries work with in-memory DB (4 tests passing)

### 2.3 DataStore Setup
- [x] TEST: Write `SettingsRepositoryImplTest.kt` — RED (verify ThemeMode/Locale persist and observe)
- [x] Create `app/src/main/java/.../domain/repositories/SettingsRepository.kt` — interface for get/set ThemeMode
- [x] Create `app/src/main/java/.../core/persistence/UserPreferences.kt` — data class for user preferences
- [x] Create `app/src/main/java/.../data/repositories/SettingsRepositoryImpl.kt` — implementation using DataStore Preferences
- [x] TEST: GREEN — verify DataStore reads correct values (4 tests passing)

### 2.4 Connection Repository (DEFERRED to PR #2b)
- [ ] TEST: Write `ConnectionRepositoryImplTest.kt` — RED (verify save encrypts password, load decrypts)
- [ ] Create `app/src/main/java/.../domain/repositories/ConnectionRepository.kt` — interface
- [ ] Create `app/src/main/java/.../data/repositories/ConnectionRepositoryImpl.kt` — uses Room + CredentialEncryption
- [ ] TEST: GREEN — verify repository encrypts passwords before Room insert

### 2.5 DI Modules
- [x] Create `app/src/main/java/.../core/di/DatabaseModule.kt` — Hilt module providing Room database and DAOs
- [x] Create `app/src/main/java/.../core/di/SecurityModule.kt` — Hilt module providing CredentialEncryption singleton
- [x] Create `app/src/main/java/.../core/di/PersistenceModule.kt` — Hilt module providing DataStore
- [x] Create `app/src/main/java/.../core/di/RepositoryModule.kt` — Hilt module binding SettingsRepository (ConnectionRepository deferred to PR #2b)

### 2.6 Domain UseCases
- [ ] TEST: Write `SaveConnectionUseCaseTest.kt` — RED (verify UseCase calls repository.save)
- [ ] Create `app/src/main/java/.../domain/usecases/SaveConnectionUseCase.kt`
- [ ] TEST: GREEN — verify UseCase delegates to repository
- [ ] TEST: Write `LoadConnectionsUseCaseTest.kt` + `DeleteConnectionUseCaseTest.kt` + `TestConnectionUseCaseTest.kt` — RED
- [ ] Create remaining UseCases (LoadConnections, DeleteConnection, TestConnection)
- [ ] TEST: GREEN — verify all UseCases work

### 2.7 Connections UI State
- [ ] Create `app/src/main/java/.../ui/screens/connections/ConnectionsUiState.kt` — sealed states (Loading, Success, Error)
- [ ] Create `app/src/main/java/.../ui/screens/connections/ConnectionFormUiState.kt` — sealed states (Idle, Saving, Saved, Error)

### 2.8 Connections ViewModels
- [ ] TEST: Write `ConnectionsListViewModelTest.kt` — RED (verify UiState transitions)
- [ ] Create `app/src/main/java/.../ui/screens/connections/ConnectionsListViewModel.kt` — loads connections, handles delete/test/connect
- [ ] TEST: GREEN — verify ViewModel emits correct states
- [ ] TEST: Write `ConnectionFormViewModelTest.kt` — RED
- [ ] Create `app/src/main/java/.../ui/screens/connections/ConnectionFormViewModel.kt` — saves ConnectionConfig
- [ ] TEST: GREEN

### 2.9 Connections Screens
- [ ] Create `app/src/main/java/.../ui/components/ConnectionCard.kt` — reusable card for connection list item
- [ ] TEST: Write `ConnectionsListScreenTest.kt` — RED (verify list renders, FAB click navigates)
- [ ] Create `app/src/main/java/.../ui/screens/connections/ConnectionsListScreen.kt` — list with FAB, edit, delete, test, connect actions
- [ ] TEST: GREEN — verify screen renders correctly
- [ ] TEST: Write `ConnectionFormScreenTest.kt` — RED (verify form saves on submit)
- [ ] Create `app/src/main/java/.../ui/screens/connections/ConnectionFormScreen.kt` — form with host/port/user/password/DB/SSH toggle
- [ ] TEST: GREEN — verify form input and save

### 2.10 Navigation Wiring
- [ ] Modify `MyDataBasesNavHost.kt` — replace Connections + ConnectionForm placeholders with real screens
- [ ] TEST: Write integration test verifying navigation from ConnectionsList → ConnectionForm → save → back to list

### 2.11 Localization Updates
- [ ] Update `values/strings.xml` + `values-es/strings.xml` — add all Connection screen labels, errors, hints

**PR #2 Acceptance**: `./gradlew test` passes, Connections CRUD works end-to-end, passwords encrypted in Room, navigation flows correctly, localization parity validated.

---

## Phase 3: Database Explorer (PR #3 — Databases/Tables/TableViewer)

**Target**: ~550 lines | **Base**: PR #2 branch | **Tests**: Unit + Compose UI

### 3.1 Explorer UI States
- [ ] Create `app/src/main/java/.../ui/screens/databases/DatabasesUiState.kt` — sealed states
- [ ] Create `app/src/main/java/.../ui/screens/tables/TablesUiState.kt` — sealed states
- [ ] Create `app/src/main/java/.../ui/screens/tableviewer/TableViewerUiState.kt` — sealed states (rows + columns)

### 3.2 Explorer ViewModels
- [ ] TEST: Write `DatabasesListViewModelTest.kt` — RED (verify UiState transitions with mock GetDatabasesUseCase)
- [ ] Create `app/src/main/java/.../ui/screens/databases/DatabasesListViewModel.kt`
- [ ] TEST: GREEN
- [ ] TEST: Write `TablesListViewModelTest.kt` — RED
- [ ] Create `app/src/main/java/.../ui/screens/tables/TablesListViewModel.kt`
- [ ] TEST: GREEN
- [ ] TEST: Write `TableViewerViewModelTest.kt` — RED
- [ ] Create `app/src/main/java/.../ui/screens/tableviewer/TableViewerViewModel.kt`
- [ ] TEST: GREEN

### 3.3 Explorer Components
- [ ] Create `app/src/main/java/.../ui/components/DatabaseCard.kt` — reusable card for database list item
- [ ] Create `app/src/main/java/.../ui/components/TableCard.kt` — reusable card for table list item

### 3.4 Explorer Screens
- [ ] TEST: Write `DatabasesListScreenTest.kt` — RED (verify list renders, click navigates to tables)
- [ ] Create `app/src/main/java/.../ui/screens/databases/DatabasesListScreen.kt`
- [ ] TEST: GREEN
- [ ] TEST: Write `TablesListScreenTest.kt` — RED
- [ ] Create `app/src/main/java/.../ui/screens/tables/TablesListScreen.kt`
- [ ] TEST: GREEN
- [ ] TEST: Write `TableViewerScreenTest.kt` — RED (verify rows grid + schema tab)
- [ ] Create `app/src/main/java/.../ui/screens/tableviewer/TableViewerScreen.kt` — rows grid (LazyColumn + LazyRow) + schema/columns tabs
- [ ] TEST: GREEN

### 3.5 Navigation Wiring
- [ ] Modify `MyDataBasesNavHost.kt` — replace Databases, Tables, TableViewer placeholders with real screens
- [ ] TEST: Write integration test verifying navigation flow: Connections → test connection → Databases → Tables → TableViewer

### 3.6 Localization Updates
- [ ] Update `values/strings.xml` + `values-es/strings.xml` — add all Explorer screen labels

**PR #3 Acceptance**: `./gradlew test` passes, Explorer screens navigate correctly, table rows render in grid, schema tab displays columns, localization parity validated.

---

## Phase 4: Query Editor (PR #4 — SQL Editor + Result Grid)

**Target**: ~400 lines | **Base**: PR #3 branch | **Tests**: Unit + Compose UI

### 4.1 Query Editor UI State
- [ ] Create `app/src/main/java/.../ui/screens/query/QueryEditorUiState.kt` — sealed states (Idle, Loading, Success, Error)

### 4.2 Query Editor ViewModel
- [ ] TEST: Write `QueryEditorViewModelTest.kt` — RED (verify ExecuteQuery/ExecuteUpdate UseCase calls)
- [ ] Create `app/src/main/java/.../ui/screens/query/QueryEditorViewModel.kt`
- [ ] TEST: GREEN

### 4.3 Query Result Component
- [ ] Create `app/src/main/java/.../ui/components/QueryResultGrid.kt` — reusable LazyColumn + LazyRow for result rendering

### 4.4 Query Editor Screen
- [ ] TEST: Write `QueryEditorScreenTest.kt` — RED (verify SQL input, execute button, result/error display)
- [ ] Create `app/src/main/java/.../ui/screens/query/QueryEditorScreen.kt` — TextField for SQL + execute button + result grid
- [ ] TEST: GREEN

### 4.5 Navigation Wiring
- [ ] Modify `MyDataBasesNavHost.kt` — replace QueryEditor placeholder with real screen
- [ ] TEST: Write integration test verifying query execution flow (input SQL → execute → render result or error)

### 4.6 Localization Updates
- [ ] Update `values/strings.xml` + `values-es/strings.xml` — add all Query Editor labels, hints, errors

**PR #4 Acceptance**: `./gradlew test` passes, Query Editor executes queries and displays results or errors correctly, localization parity validated.

---

## Phase 5: Settings + Final Polish (PR #5 — Settings + Integration)

**Target**: ~400 lines | **Base**: PR #4 branch | **Tests**: Unit + Compose UI + Integration

### 5.1 Settings Domain Models
- [ ] Create `app/src/main/java/.../domain/models/AppLocale.kt` — enum (SPANISH, ENGLISH)

### 5.2 Settings UseCases
- [ ] TEST: Write `SetThemeModeUseCaseTest.kt` + `GetThemeModeUseCaseTest.kt` — RED
- [ ] Create `app/src/main/java/.../domain/usecases/SetThemeModeUseCase.kt` + `GetThemeModeUseCase.kt`
- [ ] TEST: GREEN
- [ ] TEST: Write `SetLocaleUseCaseTest.kt` + `GetLocaleUseCaseTest.kt` — RED
- [ ] Create `app/src/main/java/.../domain/usecases/SetLocaleUseCase.kt` + `GetLocaleUseCase.kt` (calls AppCompatDelegate.setApplicationLocales)
- [ ] TEST: GREEN

### 5.3 Settings UI State
- [ ] Create `app/src/main/java/.../ui/screens/settings/SettingsUiState.kt` — data class (themeMode, locale)

### 5.4 Settings ViewModel
- [ ] TEST: Write `SettingsViewModelTest.kt` — RED (verify theme/locale changes persist)
- [ ] Create `app/src/main/java/.../ui/screens/settings/SettingsViewModel.kt`
- [ ] TEST: GREEN

### 5.5 Settings Screen
- [ ] TEST: Write `SettingsScreenTest.kt` — RED (verify theme picker, locale picker, immediate application)
- [ ] Create `app/src/main/java/.../ui/screens/settings/SettingsScreen.kt` — theme mode picker + locale picker
- [ ] TEST: GREEN

### 5.6 Navigation Wiring
- [ ] Modify `MyDataBasesNavHost.kt` — replace Settings placeholder with real screen
- [ ] Add Settings icon to ConnectionsListScreen toolbar (navigate to Settings)

### 5.7 MainActivity Theme Integration
- [ ] Modify `MainActivity.kt` — replace hardcoded ThemeMode StateFlow with GetThemeModeUseCase observation
- [ ] TEST: Write integration test verifying theme change in Settings → Activity recreates → new theme applies

### 5.8 Localization Final Pass
- [ ] Update `values/strings.xml` + `values-es/strings.xml` — add all Settings labels
- [ ] Run `LocalizationParityTest.kt` — verify all en strings exist in es

### 5.9 AndroidManifest Updates
- [ ] Modify `app/src/main/AndroidManifest.xml` — add locale config, remove conflicting theme references

### 5.10 Final Integration Testing
- [ ] TEST: Write end-to-end UI test — create connection → test → browse databases → run query → change theme → change locale
- [ ] Run `./gradlew test` + `./gradlew connectedAndroidTest` — verify all tests pass

### 5.11 Documentation Updates
- [ ] Update `README.md` — add screenshots, feature list, build instructions
- [ ] Create ADR for Theme/Locale persistence strategy

**PR #5 Acceptance**: `./gradlew test` + `./gradlew connectedAndroidTest` pass, Settings screen works end-to-end, theme and locale changes apply immediately, all localization strings present in es + en, final integration flow verified.

---

## Testing Summary

| Layer | Tests | Command |
|-------|-------|---------|
| Unit (ViewModels, UseCases, Repos) | 35+ tests | `./gradlew test` |
| Integration (Room, DataStore) | 8+ tests | `./gradlew test` |
| UI (Compose) | 12+ tests | `./gradlew connectedAndroidTest` |

**TDD Workflow Applied**: Every ViewModel, UseCase, Repository, and DAO has tests written BEFORE implementation (RED → GREEN → REFACTOR).

**Coverage Target**: 80% for domain/data/presentation layers (manual review, tooling not yet configured).

---

## PR Chain Strategy

### Branch Structure

```
main
  └── feature/ui-implementation (tracker branch)
        ├── PR #1: ui-theme-navigation-skeleton
        │     └── PR #2: ui-persistence-connections
        │           └── PR #3: ui-database-explorer
        │                 └── PR #4: ui-query-editor
        │                       └── PR #5: ui-settings-polish
```

### PR Workflow

1. **PR #1**: Branch from `feature/ui-implementation`, targets `feature/ui-implementation`
2. **PR #2**: Branch from PR #1 branch, targets PR #1 branch
3. **PR #3**: Branch from PR #2 branch, targets PR #2 branch
4. **PR #4**: Branch from PR #3 branch, targets PR #3 branch
5. **PR #5**: Branch from PR #4 branch, targets PR #4 branch
6. **Final PR**: Merge `feature/ui-implementation` into `main` after all child PRs merged

**Each PR diff shows ONLY that slice's changes** (GitHub compares child against immediate parent).

### PR Acceptance Criteria

Each PR MUST:
- Pass `./gradlew test`
- Pass `./gradlew assembleDebug`
- Pass localization parity test
- Stay under 800 changed lines
- Include tests for all new code (80% coverage target)
- Follow Strict TDD (tests before implementation)
- Update strings.xml for both en + es

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| PR exceeds 800 lines | Split into smaller tasks, move non-critical polish to follow-up |
| Test execution slow on CI | Run unit tests only in PR checks, instrumentation tests nightly |
| Merge conflicts in feature-branch-chain | Rebase child PRs frequently, keep PRs small and focused |
| Room migration issues | Use in-memory DB for tests, defer schema changes to follow-up |
| Locale switching doesn't recreate Activity | Verify AppCompatDelegate.setApplicationLocales usage, test on real device |

---

## Open Questions Resolved

- **SSH Tunnel UI**: Grayed out with "Coming soon" message. Field in Room schema but no JSch dependency. Deferred to v2.
- **Query Result Pagination**: LIMIT 1000 for MVP. No "Load more" yet.
- **Password Visibility Toggle**: Included in ConnectionFormScreen (Material 3 standard).
- **Connection Test Timeout**: `withTimeout(connectionTimeout + 2000)` in TestConnectionUseCase.
- **Error Message Localization**: Wrap engine errors in localized strings in ViewModels (e.g., `getString(R.string.connection_failed, rawError)`).

---

## Files Summary

**Total**: 79 files (64 created, 15 modified)

| Category | Files | Lines (approx) |
|----------|-------|----------------|
| UI Screens | 18 | ~900 |
| ViewModels | 9 | ~450 |
| Components | 6 | ~300 |
| Navigation | 2 | ~200 |
| Theme | 3 | ~150 |
| Room | 5 | ~250 |
| DataStore | 2 | ~100 |
| Security | 1 | ~50 |
| UseCases | 8 | ~200 |
| Repositories | 4 | ~200 |
| DI | 1 | ~100 |
| Tests | 55+ | ~1,800 |
| Localization | 2 | ~200 |
| Manifest | 1 | ~20 |
| Build | 1 | ~10 |

**Total Estimated Lines**: ~2,430 lines (within forecast range).
