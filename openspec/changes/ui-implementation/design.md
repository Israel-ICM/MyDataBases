# Design: UI Implementation for MyDataBases

## Technical Approach

This change implements a complete Compose UI layer on top of the existing database engine (MySQL/MariaDB). The approach follows Clean Architecture with MVVM presentation, using Jetpack Compose for declarative UI, Hilt for dependency injection, Room for connection persistence with encrypted passwords, and DataStore for user preferences (theme, locale).

Navigation uses Navigation Compose with typed sealed Routes. State management follows ViewModel + StateFlow with a sealed UiState hierarchy (`Loading | Success | Error`) per screen. Material 3 theming supports light/dark/system modes plus dynamic color on Android 12+. Localization supports es (default) and en with runtime switching via `AppCompatDelegate`. Adaptive layouts respond to `WindowSizeClass` (Compact/Medium/Expanded).

**Maps to proposal approach**: Single-activity architecture, typed navigation, DataStore + Room persistence, Material 3 theming, es/en localization with runtime switching, MVVM + StateFlow state management.

**References specs**: (No specs exist yet — proposal defines requirements inline.)

## Architecture Decisions

### Decision: Navigation Architecture

**Choice**: Navigation Compose with sealed `Route` class hierarchy  
**Alternatives considered**: Manual back stack management, Jetpack Fragments, Compose Destinations library  
**Rationale**: Navigation Compose is first-party, stable, and integrates cleanly with Compose. Sealed Routes provide compile-time safety for navigation arguments. Manual back stack is error-prone; Fragments violate 100% Compose requirement; Compose Destinations adds code generation complexity without enough ROI for this scale.

### Decision: Credential Encryption Strategy

**Choice**: `androidx.security.crypto.EncryptedSharedPreferences` with MasterKey for Room password field  
**Alternatives considered**: Android Keystore direct use, plaintext (REJECTED), custom AES encryption  
**Rationale**: `EncryptedSharedPreferences` uses AES256-GCM backed by Android Keystore. It's battle-tested and handles key rotation. Direct Keystore usage requires more boilerplate and error handling. Plaintext violates security-by-default principle. Custom crypto introduces risk of implementation bugs.

### Decision: Theme Persistence

**Choice**: DataStore Preferences for `ThemeMode` enum (LIGHT/DARK/SYSTEM)  
**Alternatives considered**: SharedPreferences, Room, in-memory only  
**Rationale**: DataStore is type-safe, async-first, and integrates with coroutines. SharedPreferences is sync and deprecated for new code. Room is overkill for a single enum. In-memory loses user preference on restart.

### Decision: Locale Switching Mechanism

**Choice**: `AppCompatDelegate.setApplicationLocales` + DataStore persistence  
**Alternatives considered**: Manual context wrapper, runtime Locale.setDefault, res switching  
**Rationale**: `setApplicationLocales` (AndroidX 1.6+) handles activity recreation cleanly and respects system locale fallback. Manual context wrapper is fragile across configuration changes. `Locale.setDefault` doesn't persist and affects the whole process. Res switching requires manual rebuild of Composables.

### Decision: State Management Pattern

**Choice**: Sealed `UiState` per screen (`Loading | Success<T> | Error(message)`) + ViewModel with `StateFlow`  
**Alternatives considered**: LiveData, SharedFlow, mutableStateOf in ViewModel, Channel  
**Rationale**: Sealed UiState makes state exhaustive and forces UI to handle all cases. StateFlow is lifecycle-aware and caches the last value. LiveData is older and less Kotlin-idiomatic. SharedFlow doesn't cache. mutableStateOf in ViewModel works but StateFlow integrates better with collectAsStateWithLifecycle. Channel is for events, not state.

### Decision: Room Schema for Connections

**Choice**: Single `ConnectionEntity` table with encrypted password, no migration yet  
**Alternatives considered**: Proto DataStore, Firebase, separate tables for SSH config  
**Rationale**: Room provides typed queries, compile-time safety, and SQLite reliability. Proto DataStore is overkill for structured relational data (connections have foreign key potential for SSH tunnels). Firebase violates offline-first requirement. Separate SSH table over-engineers for v1; denormalized SSHTunnelConfig JSON column is simpler and sufficient until proven otherwise.

### Decision: WindowSizeClass Adaptation Strategy

**Choice**: Compact-first, Medium/Expanded add horizontal split for list+detail screens  
**Alternatives considered**: Tablet-only separate layouts, responsive grid only, ignore adaptation  
**Rationale**: Most users are on phones (Compact). Medium/Expanded warrant better UX for foldables/tablets but shouldn't block v1. Horizontal split (Navigation Rail + detail pane) is Material 3 standard and low-effort. Separate tablet layouts duplicate code. Responsive grid alone doesn't solve navigation depth. Ignoring adaptation violates proposal scope.

## Data Flow

### Connection CRUD Flow

    User → ConnectionForm (Composable)
              ↓
         ConnectionFormViewModel
              ↓ (SaveConnectionUseCase)
         ConnectionRepository
              ↓
         Room ConnectionDao → ConnectionEntity (password encrypted via EncryptedSharedPreferences)

### Database Browsing Flow

    User → ConnectionsListScreen → select connection
              ↓ (TestConnectionUseCase)
         DatabaseRepository.connect()
              ↓
         MySQL/MariaDB Engine (existing)
              ↓ (on success)
         Navigate to DatabasesListScreen
              ↓ (GetDatabasesUseCase)
         DatabaseRepository.getDatabases()
              ↓
         Render database list → User selects DB → Navigate to TablesListScreen
              ↓ (GetTablesUseCase)
         DatabaseRepository.getTables(dbName)
              ↓
         Render table list → User selects table → Navigate to TableViewerScreen
              ↓ (GetColumnsUseCase + ExecuteQueryUseCase)
         DatabaseRepository.getColumns() + executeQuery("SELECT * FROM table LIMIT 1000")
              ↓
         Render rows in LazyColumn with schema tab

### Query Execution Flow

    User → QueryEditorScreen (TextField + Button)
              ↓
         QueryEditorViewModel
              ↓ (ExecuteQueryUseCase or ExecuteUpdateUseCase)
         DatabaseRepository.executeQuery() or .executeUpdate()
              ↓
         MySQL/MariaDB Engine (existing)
              ↓
         Result<QueryResult> or Result<Int> → UiState.Success(data) or UiState.Error(message)
              ↓
         Render result grid (LazyColumn + LazyRow) or affected rows count or error message

### Theme/Locale Flow

    User → SettingsScreen → toggle ThemeMode or Locale
              ↓
         SettingsViewModel
              ↓ (SetThemeModeUseCase or SetLocaleUseCase)
         SettingsRepository
              ↓
         DataStore Preferences → persist enum
              ↓ (for locale)
         AppCompatDelegate.setApplicationLocales() → Activity recreates
              ↓ (for theme)
         MyDataBasesTheme Composable observes StateFlow → recomposes with new ColorScheme

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `app/src/main/java/.../ui/navigation/Route.kt` | Create | Sealed class hierarchy for typed routes (Connections, ConnectionForm, Databases, Tables, TableViewer, QueryEditor, Settings) |
| `app/src/main/java/.../ui/navigation/MyDataBasesNavHost.kt` | Create | NavHost composable with all screen destinations and route parsing |
| `app/src/main/java/.../ui/screens/connections/ConnectionsListScreen.kt` | Create | Screen: list of saved connections with FAB, edit, delete, test, connect actions |
| `app/src/main/java/.../ui/screens/connections/ConnectionsListViewModel.kt` | Create | ViewModel: loads connections from Room, handles delete/test/connect |
| `app/src/main/java/.../ui/screens/connections/ConnectionFormScreen.kt` | Create | Screen: form for create/edit connection (host, port, user, password, DB, SSH toggle) |
| `app/src/main/java/.../ui/screens/connections/ConnectionFormViewModel.kt` | Create | ViewModel: saves ConnectionConfig to Room with encrypted password |
| `app/src/main/java/.../ui/screens/connections/ConnectionsUiState.kt` | Create | Sealed UiState for connections list/form |
| `app/src/main/java/.../ui/screens/databases/DatabasesListScreen.kt` | Create | Screen: list of databases post-connection |
| `app/src/main/java/.../ui/screens/databases/DatabasesListViewModel.kt` | Create | ViewModel: calls GetDatabasesUseCase |
| `app/src/main/java/.../ui/screens/databases/DatabasesUiState.kt` | Create | Sealed UiState for databases list |
| `app/src/main/java/.../ui/screens/tables/TablesListScreen.kt` | Create | Screen: list of tables for selected database |
| `app/src/main/java/.../ui/screens/tables/TablesListViewModel.kt` | Create | ViewModel: calls GetTablesUseCase |
| `app/src/main/java/.../ui/screens/tables/TablesUiState.kt` | Create | Sealed UiState for tables list |
| `app/src/main/java/.../ui/screens/tableviewer/TableViewerScreen.kt` | Create | Screen: rows grid + schema/columns tabs |
| `app/src/main/java/.../ui/screens/tableviewer/TableViewerViewModel.kt` | Create | ViewModel: calls GetColumnsUseCase + ExecuteQueryUseCase (SELECT * LIMIT 1000) |
| `app/src/main/java/.../ui/screens/tableviewer/TableViewerUiState.kt` | Create | Sealed UiState for table viewer (rows + columns) |
| `app/src/main/java/.../ui/screens/query/QueryEditorScreen.kt` | Create | Screen: SQL input TextField + result grid/error display |
| `app/src/main/java/.../ui/screens/query/QueryEditorViewModel.kt` | Create | ViewModel: calls ExecuteQueryUseCase or ExecuteUpdateUseCase |
| `app/src/main/java/.../ui/screens/query/QueryEditorUiState.kt` | Create | Sealed UiState for query editor (idle, loading, success, error) |
| `app/src/main/java/.../ui/screens/settings/SettingsScreen.kt` | Create | Screen: theme mode picker, locale picker |
| `app/src/main/java/.../ui/screens/settings/SettingsViewModel.kt` | Create | ViewModel: reads/writes theme + locale to DataStore |
| `app/src/main/java/.../ui/screens/settings/SettingsUiState.kt` | Create | Data class for settings state (themeMode, locale) |
| `app/src/main/java/.../ui/components/ConnectionCard.kt` | Create | Reusable Composable: card for connection list item |
| `app/src/main/java/.../ui/components/DatabaseCard.kt` | Create | Reusable Composable: card for database list item |
| `app/src/main/java/.../ui/components/TableCard.kt` | Create | Reusable Composable: card for table list item |
| `app/src/main/java/.../ui/components/QueryResultGrid.kt` | Create | Reusable Composable: LazyColumn + LazyRow for query results |
| `app/src/main/java/.../ui/components/ErrorCard.kt` | Create | Reusable Composable: error display with retry button |
| `app/src/main/java/.../ui/components/LoadingIndicator.kt` | Create | Reusable Composable: centered CircularProgressIndicator |
| `app/src/main/java/.../ui/theme/Theme.kt` | Modify | Add ThemeMode parameter, read from SettingsRepository StateFlow, apply dynamic color |
| `app/src/main/java/.../ui/theme/Color.kt` | Modify | Complete Material 3 color tokens (add secondary, tertiary, error, surface, etc.) |
| `app/src/main/java/.../data/local/AppDatabase.kt` | Create | Room database with ConnectionEntity table |
| `app/src/main/java/.../data/local/dao/ConnectionDao.kt` | Create | Room DAO for CRUD on ConnectionEntity |
| `app/src/main/java/.../data/local/entities/ConnectionEntity.kt` | Create | Room entity mapped from ConnectionConfig (password encrypted) |
| `app/src/main/java/.../data/local/converters/DatabaseTypeConverter.kt` | Create | Room TypeConverter for DatabaseType enum |
| `app/src/main/java/.../data/local/converters/SSHTunnelConfigConverter.kt` | Create | Room TypeConverter for SSHTunnelConfig (JSON serialization) |
| `app/src/main/java/.../data/local/security/CredentialEncryption.kt` | Create | Utility for encrypt/decrypt password using EncryptedSharedPreferences |
| `app/src/main/java/.../data/repository/ConnectionRepository.kt` | Create | Interface for save/load/delete ConnectionConfig |
| `app/src/main/java/.../data/repository/ConnectionRepositoryImpl.kt` | Create | Implementation using Room + CredentialEncryption |
| `app/src/main/java/.../data/repository/SettingsRepository.kt` | Create | Interface for get/set ThemeMode and Locale |
| `app/src/main/java/.../data/repository/SettingsRepositoryImpl.kt` | Create | Implementation using DataStore Preferences |
| `app/src/main/java/.../data/di/LocalModule.kt` | Create | Hilt module providing Room database, DAOs, DataStore, repositories |
| `app/src/main/java/.../domain/usecases/SaveConnectionUseCase.kt` | Create | UseCase: save ConnectionConfig via ConnectionRepository |
| `app/src/main/java/.../domain/usecases/LoadConnectionsUseCase.kt` | Create | UseCase: load all ConnectionConfig from ConnectionRepository |
| `app/src/main/java/.../domain/usecases/DeleteConnectionUseCase.kt` | Create | UseCase: delete ConnectionConfig by ID |
| `app/src/main/java/.../domain/usecases/TestConnectionUseCase.kt` | Create | UseCase: connect + disconnect to validate credentials (no side effects) |
| `app/src/main/java/.../domain/usecases/SetThemeModeUseCase.kt` | Create | UseCase: persist ThemeMode to SettingsRepository |
| `app/src/main/java/.../domain/usecases/GetThemeModeUseCase.kt` | Create | UseCase: observe ThemeMode from SettingsRepository (StateFlow) |
| `app/src/main/java/.../domain/usecases/SetLocaleUseCase.kt` | Create | UseCase: persist Locale + call AppCompatDelegate.setApplicationLocales |
| `app/src/main/java/.../domain/usecases/GetLocaleUseCase.kt` | Create | UseCase: observe Locale from SettingsRepository (StateFlow) |
| `app/src/main/java/.../domain/models/ThemeMode.kt` | Create | Enum: LIGHT, DARK, SYSTEM |
| `app/src/main/java/.../domain/models/AppLocale.kt` | Create | Enum: SPANISH, ENGLISH (wraps locale tags) |
| `app/src/main/java/.../MainActivity.kt` | Modify | Replace Greeting with MyDataBasesNavHost, provide WindowSizeClass via CompositionLocal, observe ThemeMode |
| `app/src/main/res/values/strings.xml` | Modify | Add all en strings for screens, labels, errors |
| `app/src/main/res/values-es/strings.xml` | Create | Add all es strings (mirrored from values/strings.xml) |
| `app/src/main/AndroidManifest.xml` | Modify | Add locale config, remove theme references if conflicting |
| `app/build.gradle.kts` | Modify | Add `androidx.security:security-crypto:1.1.0-alpha06` dependency |

## Interfaces / Contracts

### Navigation Routes

```kotlin
package com.sphynxs.mydatabases.ui.navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class Route(val path: String) {
    object Connections : Route("connections")
    
    @Parcelize
    data class ConnectionForm(val connectionId: String? = null) : Route("connection_form"), Parcelable
    
    object Databases : Route("databases")
    
    @Parcelize
    data class Tables(val databaseName: String) : Route("tables"), Parcelable
    
    @Parcelize
    data class TableViewer(
        val databaseName: String,
        val tableName: String
    ) : Route("table_viewer"), Parcelable
    
    @Parcelize
    data class QueryEditor(val databaseName: String? = null) : Route("query_editor"), Parcelable
    
    object Settings : Route("settings")
}
```

### UiState Sealed Hierarchies

```kotlin
package com.sphynxs.mydatabases.ui.screens.connections

import com.sphynxs.mydatabases.core.database.models.ConnectionConfig

sealed class ConnectionsListUiState {
    object Loading : ConnectionsListUiState()
    data class Success(val connections: List<ConnectionConfig>) : ConnectionsListUiState()
    data class Error(val message: String) : ConnectionsListUiState()
}

sealed class ConnectionFormUiState {
    object Idle : ConnectionFormUiState()
    object Saving : ConnectionFormUiState()
    object Saved : ConnectionFormUiState()
    data class Error(val message: String) : ConnectionFormUiState()
}

sealed class ConnectionTestUiState {
    object Idle : ConnectionTestUiState()
    object Testing : ConnectionTestUiState()
    object Success : ConnectionTestUiState()
    data class Error(val message: String) : ConnectionTestUiState()
}
```

### Repository Interfaces

```kotlin
package com.sphynxs.mydatabases.data.repository

import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import kotlinx.coroutines.flow.Flow

interface ConnectionRepository {
    suspend fun save(config: ConnectionConfig)
    suspend fun delete(id: String)
    suspend fun getById(id: String): ConnectionConfig?
    fun getAll(): Flow<List<ConnectionConfig>>
    suspend fun updateLastUsed(id: String, timestamp: Long)
}
```

```kotlin
package com.sphynxs.mydatabases.data.repository

import com.sphynxs.mydatabases.domain.models.AppLocale
import com.sphynxs.mydatabases.domain.models.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    fun getLocale(): Flow<AppLocale>
    suspend fun setLocale(locale: AppLocale)
    fun getLastUsedConnectionId(): Flow<String?>
    suspend fun setLastUsedConnectionId(id: String?)
}
```

### Room Entity

```kotlin
package com.sphynxs.mydatabases.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sphynxs.mydatabases.core.database.engine.DatabaseType

@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: DatabaseType,
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    @ColumnInfo(name = "encrypted_password") val encryptedPassword: String, // Encrypted via EncryptedSharedPreferences
    @ColumnInfo(name = "use_ssl") val useSSL: Boolean,
    @ColumnInfo(name = "ssh_tunnel_config_json") val sshTunnelConfigJson: String?, // JSON serialized SSHTunnelConfig
    @ColumnInfo(name = "connection_timeout") val connectionTimeout: Long,
    @ColumnInfo(name = "read_timeout") val readTimeout: Long,
    @ColumnInfo(name = "max_pool_size") val maxPoolSize: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "last_used_at") val lastUsedAt: Long?
)
```

### DAO

```kotlin
package com.sphynxs.mydatabases.data.local.dao

import androidx.room.*
import com.sphynxs.mydatabases.data.local.entities.ConnectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connection: ConnectionEntity)
    
    @Delete
    suspend fun delete(connection: ConnectionEntity)
    
    @Query("DELETE FROM connections WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("SELECT * FROM connections WHERE id = :id")
    suspend fun getById(id: String): ConnectionEntity?
    
    @Query("SELECT * FROM connections ORDER BY last_used_at DESC, created_at DESC")
    fun getAll(): Flow<List<ConnectionEntity>>
    
    @Query("UPDATE connections SET last_used_at = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: String, timestamp: Long)
}
```

### Domain Models

```kotlin
package com.sphynxs.mydatabases.domain.models

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

enum class AppLocale(val tag: String) {
    SPANISH("es"),
    ENGLISH("en")
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit (ViewModel) | UiState transitions, UseCase calls with mocked repos | JUnit + Mockk + kotlinx-coroutines-test with `runTest`, verify StateFlow emissions |
| Unit (Repository) | ConnectionRepository CRUD with fake DAO, SettingsRepository with fake DataStore | JUnit + Mockk, use test doubles instead of real Room/DataStore |
| Unit (UseCase) | Each UseCase invokes correct repository method with correct params | JUnit + Mockk, simple verify calls |
| Unit (Encryption) | CredentialEncryption round-trip encrypt/decrypt | JUnit, verify plaintext → encrypted → plaintext identity |
| Integration (Room) | DAO queries with in-memory Room database | JUnit + Robolectric + Room.inMemoryDatabaseBuilder, verify actual SQL behavior |
| Integration (DataStore) | SettingsRepository with real DataStore in test directory | JUnit + kotlinx-coroutines-test, verify actual file I/O |
| UI (Compose) | Critical flows: create connection → test → browse databases → run query | Compose UI Test + ComposeTestRule, verify navigation, text inputs, result rendering |
| UI (Compose) | Settings screen theme/locale toggle | Compose UI Test, verify DataStore updates and theme change |
| UI (Compose) | Error states render correctly | Compose UI Test, inject error UiState, verify error message displayed |

**TDD Workflow**:
1. Write ViewModel test FIRST (expected UiState emissions for given UseCase results)
2. Implement ViewModel to pass test
3. Write Repository test FIRST (expected DAO/DataStore calls)
4. Implement Repository to pass test
5. Write DAO test FIRST (expected SQL query results)
6. Implement DAO to pass test
7. Write Compose UI test for critical user flow AFTER implementation (high cost, use sparingly)

**Coverage Target**: 80% per project standards (not enforced by tooling yet, but manual code review required).

**Test Execution**:
- Unit + Integration: `./gradlew test`
- UI: `./gradlew connectedAndroidTest`

## Migration / Rollout

### Room Schema Migration
**Version 1**: `ConnectionEntity` table as defined above. No migration needed for initial release.

**Future migrations**: When schema changes (e.g., adding columns, changing types):
1. Increment Room database version
2. Provide `Migration` object with SQL ALTER TABLE statements
3. Write integration test verifying migration from v(n) to v(n+1) preserves data
4. Never use `fallbackToDestructiveMigration` in release builds (data loss risk)

### Feature Rollout
**Phase 1 (MVP)**: All screens shipped together as atomic change. No feature flags.

**Phase 2 (Post-release)**: If a screen has critical bugs, add `BuildConfig.FEATURE_*` flag to gate screen in NavHost. Revert to Connections screen with message "Feature temporarily disabled".

**Rollback Plan**: Revert commit range on `main`. No data loss (Room schema v1 is additive). Encrypted passwords remain safe in Room DB.

### Locale Rollout
**es** is default (proposal scope). **en** strings provided at launch. Future locales (pt, fr, etc.) require:
1. Add `values-{lang}/strings.xml`
2. Add enum to `AppLocale`
3. Add option to Settings screen picker
4. No code changes to logic

### Dynamic Color Opt-Out
**Default**: Dynamic color ON for Android 12+.  
**Settings screen**: Add "Use dynamic color" toggle persisted in DataStore. If OFF, force branded palette regardless of OS version.

## Open Questions

- [ ] **SSH Tunnel Implementation**: Proposal mentions SSH tunnel toggle in ConnectionForm, but no SSH library dependency exists yet. Should we use JSch, SSHJ, or defer SSH to a follow-up change? **Recommendation**: Defer SSH to v2 — add `sshTunnelConfig` field to Room schema but gray out toggle in UI with "Coming soon" message. Avoids adding JSch dependency and testing complexity for MVP.
- [ ] **Query Result Pagination**: Proposal mentions capping at 1000 rows with "Load more". Should this be implemented as LIMIT/OFFSET at engine layer, or as a LazyColumn windowing strategy? **Recommendation**: LIMIT 1000 for MVP, no "Load more" button yet. Add in v2 if users report needing it.
- [ ] **Password Visibility Toggle**: Should ConnectionForm password field have a visibility toggle (show/hide plaintext)? **Recommendation**: Yes, Material 3 standard pattern. Low effort, high UX value.
- [ ] **Connection Test Timeout**: TestConnectionUseCase should timeout if connection hangs. Should we use `withTimeout` from coroutines, or rely on `connectionTimeout` from ConnectionConfig? **Recommendation**: Both — `withTimeout(connectionTimeout + 2000)` to give engine a grace period before coroutine cancellation.
- [ ] **Error Message Localization**: Should DatabaseError messages (from engine layer) be localized, or are they always English (since they come from JDBC drivers)? **Recommendation**: Wrap engine errors in user-facing localized strings in ViewModel (e.g., `getString(R.string.connection_failed, error.message)`). Keep raw error in logs.
