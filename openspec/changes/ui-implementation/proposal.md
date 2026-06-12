# Proposal: UI Implementation for MyDataBases

## Intent

The database engine (MySQL/MariaDB) is implemented but has no user-facing surface. Users cannot create connections, browse databases, or run queries. This change delivers the complete Compose UI so the app becomes usable end-to-end, with dark/light theming, es/en localization, and a navigation skeleton that future engines (PostgreSQL, SQLite) can plug into.

## Scope

### In Scope
- Navigation skeleton (Navigation Compose) with typed routes
- Connections list screen (CRUD: create, edit, delete, test, connect)
- Connection form screen (host, port, user, password, db, SSH tunnel toggle)
- Databases list screen (post-connection)
- Tables list screen (per database)
- Table viewer screen (rows + schema/columns tabs)
- SQL query editor screen (input + result grid + error display)
- Settings screen (theme mode, language)
- Theme system: Material 3 dynamic color + light/dark/system follow + persistence
- Locale system: es (default) + en, runtime switch + persistence
- Adaptive layouts for Compact/Medium/Expanded `WindowSizeClass`
- DataStore-backed preferences (theme, locale, last-used connection)
- Room-backed persistence for saved connections (encrypted password field)

### Out of Scope
- Additional engines (PostgreSQL, SQLite, MongoDB) — engine layer ready, UI parameterized but only MySQL/MariaDB selectable
- Query history and saved queries
- Schema diff / migration tools
- Data export (CSV/JSON)
- Biometric unlock for saved connections
- Tablet-specific master-detail panes beyond basic adaptive scaffolding
- Cloud sync of connections
- In-app SQL syntax highlighting (plain `TextField` for now)

## Capabilities

### New Capabilities
- `ui-navigation`: Typed Navigation Compose routes, single-activity scaffold, deep-link safe back stack
- `ui-theme`: Material 3 theming with light/dark/system mode, dynamic color (Android 12+), DataStore persistence
- `ui-localization`: es/en string resources, runtime locale switch via `AppCompatDelegate`, DataStore persistence
- `connection-management`: List, create, edit, delete, test, and persist database connections (Room + encrypted credentials)
- `database-browser`: Browse databases, tables, columns, and rows for a live connection
- `query-runner`: Execute ad-hoc SQL with result grid and error surfacing
- `app-settings`: User-facing preferences screen (theme, language)

### Modified Capabilities
- None (no prior UI specs exist)

## Approach

**Navigation**: Single `MainActivity` hosting a `NavHost`. Routes defined as a sealed `Route` hierarchy for type safety. Navigation graph: `Connections → ConnectionForm`, `Connections → Databases → Tables → TableViewer`, `Tables → QueryEditor`, `Settings` (top-level).

**Theming**: `MyDataBasesTheme` composable reads `ThemeMode` (LIGHT/DARK/SYSTEM) from `SettingsRepository` (DataStore). Supports Material 3 dynamic color on Android 12+, falls back to brand palette. Color tokens defined once in `ui/theme/Color.kt`.

**Localization**: All user-facing strings in `res/values/strings.xml` (en) and `res/values-es/strings.xml` (es). Default locale: es. Runtime switch persisted in DataStore and applied via `AppCompatDelegate.setApplicationLocales`. KDoc on all public APIs in Spanish per project standard.

**State management**: MVVM with Hilt-injected `ViewModel`s exposing `StateFlow<UiState>`. Each screen owns its `UiState` sealed class (`Loading | Success | Error`). Compose collects via `collectAsStateWithLifecycle`. No `LiveData`.

**Adaptive UI**: `calculateWindowSizeClass` at activity level passed down via `CompositionLocal`. Compact = single pane, Medium/Expanded = list+detail where applicable.

**Persistence**:
- `DataStore Preferences` → theme mode, locale, last opened connection id
- `Room` → `ConnectionEntity` table with encrypted password (Android Keystore + `EncryptedSharedPreferences` strategy or `androidx.security.crypto`)

**Architecture layers** (per Clean Architecture):
- `presentation/` — Composables, ViewModels, UiState
- `domain/` — Use cases (existing) + new ones for connection persistence and settings
- `data/` — Repositories (existing engine + new `ConnectionRepository`, `SettingsRepository`)

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `app/src/main/java/.../ui/` | New | All Compose screens, navigation, theme system |
| `app/src/main/java/.../ui/theme/` | Modified | Extend with theme mode logic + dynamic color |
| `app/src/main/java/.../data/local/` | New | Room DB for connections, DataStore for settings |
| `app/src/main/java/.../domain/usecases/` | New | Save/load/delete connection, get/set theme, get/set locale |
| `app/src/main/java/.../MainActivity.kt` | Modified | Host `NavHost`, apply theme + window size class |
| `app/src/main/res/values/strings.xml` | Modified | All en strings |
| `app/src/main/res/values-es/strings.xml` | New | All es strings |
| `app/src/main/AndroidManifest.xml` | Modified | Locale config, theme references |
| `app/build.gradle.kts` | Modified | Add `androidx.security.crypto`, `material3-window-size-class`, Room runtime if absent |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Encrypted credentials at rest implementation bugs leak passwords | Medium | Use `androidx.security.crypto` MasterKey + `EncryptedFile`; never log credentials; integration test for round-trip |
| Runtime locale switch causes activity recreation flicker | Low | Use `AppCompatDelegate.setApplicationLocales` (handles recreation cleanly); test on Android 13+ and 12- |
| Dynamic color clashes with brand identity on Android 12+ | Low | Allow user opt-out in Settings; provide branded fallback palette |
| Query result grid OOM on huge result sets | Medium | Page results at engine layer (LIMIT/OFFSET); `LazyColumn` with stable keys; cap initial fetch at 1000 rows with "Load more" |
| Adaptive layouts add complexity beyond review budget | Medium | Ship Compact-first; Medium/Expanded behind a `WindowSizeClass` branch but minimal until validated |
| Strict TDD overhead delays UI delivery | Medium | ViewModel + Repository tested on JVM; Compose UI tests only for critical flows (connect, query, settings) |
| Room schema migrations after first release | Low | Define `ConnectionEntity` v1 carefully; document migration policy in design phase |

## Rollback Plan

If a critical defect ships:
1. Revert the feature commit range on `main` (`git revert <range>`)
2. Database engine layer is untouched, so revert leaves a working but UI-less app — no data loss
3. Saved connections in Room are forward-compatible only; rollback before first user release carries zero migration risk
4. After release, schema rollback requires either a migration or `fallbackToDestructiveMigration` on debug builds only

Per-screen rollback: each screen is delivered as an independent work unit and can be feature-flagged off via a simple `BuildConfig` flag if needed.

## Dependencies

- Existing `DatabaseRepository` + use cases (already implemented)
- `androidx.security.crypto:crypto` (to be added)
- `androidx.compose.material3:material3-window-size-class` (to be added if missing)
- `androidx.room:room-runtime` + KSP processor (verify in `build.gradle.kts`)
- `androidx.datastore:datastore-preferences` (already in stack)

## Success Criteria

- [ ] User can create, save, test, edit, and delete a MySQL/MariaDB connection
- [ ] User can connect, browse databases → tables → rows
- [ ] User can run an arbitrary SQL query and see results or a clear error
- [ ] User can switch theme (light/dark/system) and locale (es/en); choice persists across restart
- [ ] All user-facing text exists in both `values/` and `values-es/`
- [ ] App passes `./gradlew test` and `./gradlew connectedAndroidTest`
- [ ] Build passes `./gradlew assembleDebug` with no new warnings
- [ ] Compact layout verified on a phone; Medium verified on a foldable/tablet emulator
- [ ] No plaintext credentials in Room, logs, or DataStore
