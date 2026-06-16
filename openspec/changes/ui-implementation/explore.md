# Exploration: UI Implementation

**Fecha**: 2026-06-12
**Autor**: israel-icm (vía orquestador SDD)
**Change**: ui-implementation
**Artifact store**: openspec
**Status**: Draft — listo para Proposal

---

## 1. Objetivo

Definir cómo construir la capa de presentación (UI) de MyDataBases sobre el `core-database-engine` ya implementado (MySQL + MariaDB, Result<T>, HikariCP, Hilt, Use Cases).

Esta exploración:
- Audita qué UI existe ya en el repo.
- Propone el set mínimo de pantallas para v1.0 alineado con el roadmap.
- Compara opciones de navegación, theming y modularización.
- NO escribe código todavía.

---

## 2. Current State (qué hay hoy)

### 2.1 Entry points

- `MainActivity.kt` — `ComponentActivity` con `@AndroidEntryPoint`, `enableEdgeToEdge()` y un `Scaffold` que muestra un placeholder `Greeting("MyDataBases")`. NO hay `NavHost`, NO hay `WindowSizeClass`, NO hay TopBar/BottomBar reales.
- `MyDataBasesApplication.kt` — `@HiltAndroidApp`, vacío.

### 2.2 Theming (parcial)

- `ui/theme/Theme.kt` — `MyDataBasesTheme` con `dynamicColor: Boolean = true`, fallback a `DarkColorScheme` / `LightColorScheme` para < Android 12, `isSystemInDarkTheme()` por default, status bar tintado con `colorScheme.primary`.
- `ui/theme/Color.kt` — Solo 8 colores de fallback (primary + onPrimary + container + onContainer para light/dark). Faltan secondary, tertiary, error, surface, background, outline, etc.
- `ui/theme/Type.kt` — Tipografía mínima (`bodyLarge`, `titleLarge`, `labelSmall`) con `FontFamily.Default`. Falta el resto de la escala Material 3.

### 2.3 Domain ya disponible (para consumir)

- `DatabaseRepository` (interface) y `DatabaseRepositoryImpl` con: `connect`, `disconnect`, `executeQuery`, `executeUpdate`, `getDatabases`, `getTables`, `getColumns`, `getIndexes`, `getForeignKeys`, `beginTransaction`, `getVersion`, `getSupportedFeatures`.
- Use Cases: `ConnectToDatabaseUseCase`, `ExecuteQueryUseCase`, `ExecuteUpdateUseCase`, `GetColumnsUseCase`, `GetDatabasesUseCase`, `GetTablesUseCase`.
- Models con KDoc en español: `ConnectionConfig` (Parcelable, con SSL/SSH/timeouts/pool), `Connection`, `QueryResult`, `Database`, `Table`, `Column`, `Index`, `ForeignKey`, `Transaction`, `DatabaseError` (sealed), `DatabaseType`, `DatabaseFeature`.

### 2.4 Persistencia / Settings

- `DataStore Preferences 1.0.0` ya está en deps. NO hay aún `Room` entities para `ConnectionConfig` ni Repository de persistencia local. Hoy el repo del engine maneja solo la conexión “en uso”, no el catálogo guardado.

### 2.5 Recursos / i18n

- `res/values/strings.xml` solo contiene `app_name`. NO existe `values-es/strings.xml`. El proyecto declara soporte es/en pero el catálogo de strings está vacío.

### 2.6 Navegación

- `androidx.navigation:navigation-compose:2.7.7` y `androidx.hilt:hilt-navigation-compose:1.1.0` ya están en `build.gradle.kts`, pero NO se usan.

### 2.7 Tests UI

- `app/src/androidTest/java/com/sphynxs/mydatabases/` existe vacío. `androidx.compose.ui:ui-test-junit4` y `ui-test-manifest` ya están configurados.

### 2.8 Estructura de paquetes actual

```
com.sphynxs.mydatabases/
├── MainActivity.kt
├── MyDataBasesApplication.kt
├── core/database/...     ← engine completo
├── domain/usecases/...   ← use cases
└── ui/theme/             ← theme básico
```

**Gap crítico**: NO existe `ui/` (más allá de theme), NO existe `presentation/`, NO existe `navigation/`.

---

## 3. Affected Areas

Archivos que cambiarán o se crearán cuando se implemente:

- `MainActivity.kt` — agregar `WindowSizeClass`, `NavHost`, `Scaffold` adaptativo (reemplazar `Greeting`).
- `ui/theme/Color.kt` — completar paleta Material 3 (primary/secondary/tertiary/error/surface/background + variants light/dark).
- `ui/theme/Type.kt` — completar escala tipográfica Material 3.
- `ui/theme/Theme.kt` — seguramente OK; podría exponer toggle manual (system/light/dark) para Settings.
- `res/values/strings.xml` — poblar todos los strings de UI (en).
- `res/values-es/strings.xml` — nuevo, espejo en español.
- `MyDataBasesApplication.kt` — sin cambios estructurales.
- `ui/navigation/` — nuevo (NavHost + rutas type-safe).
- `ui/screens/connections/` — nuevo (list, form, detail, test).
- `ui/screens/explorer/` — nuevo (databases → tables → columns).
- `ui/screens/query/` — nuevo (editor + results table).
- `ui/screens/settings/` — nuevo (tema, idioma, sobre).
- `ui/components/` — nuevo (TopBar, EmptyState, LoadingState, ErrorState, ResultTable, AdaptiveScaffold, DataGrid).
- `core/persistence/` (nombre tentativo) — nuevo (Room + DAO para `ConnectionConfig`, integración con `core-security` cuando exista; por ahora password encriptado vía `EncryptedSharedPreferences` o stub para entregar en otra change).
- `core/di/` — nuevo módulo Hilt para presentation y persistence.

---

## 4. Pantallas necesarias para v1.0

Mapeando el roadmap v1.0 (MySQL + MariaDB) y el spec heredado de `core-database-engine` (UI simple, dark/light, KDoc en español):

### 4.1 Inventario mínimo (MUST en v1.0)

| # | Pantalla | Rol | Use Case que consume |
|---|----------|-----|-----------------------|
| 1 | **Connections List** | Lista de `ConnectionConfig` guardadas; FAB para nueva; swipe/long-press para editar/eliminar; tap para abrir | DAO (persistencia local, pendiente) |
| 2 | **Connection Form** | Crear/editar config: nombre, tipo (MySQL/MariaDB), host, port, database, user, password, SSL, timeouts | DAO + `ConnectToDatabaseUseCase` (botón "Probar") |
| 3 | **Connection Test/Status** | Modal o estado dentro del Form; muestra éxito (versión + features) o `DatabaseError` mapeado a string localizada | `ConnectToDatabaseUseCase`, `getVersion`, `getSupportedFeatures` |
| 4 | **Explorer — Databases** | Lista de databases del servidor conectado | `GetDatabasesUseCase` |
| 5 | **Explorer — Tables** | Lista de tablas de la database seleccionada (tipo, engine, rowCount) | `GetTablesUseCase` |
| 6 | **Explorer — Table Detail** | Tabs: Columns / Indexes / Foreign Keys | `GetColumnsUseCase`, `getIndexes`, `getForeignKeys` |
| 7 | **Query Editor** | Multiline SQL input + botón ejecutar; abajo `QueryResult` paginado (columns + rows) o `affectedRows` para UPDATE | `ExecuteQueryUseCase`, `ExecuteUpdateUseCase` |
| 8 | **Settings** | Tema (system/light/dark), idioma (es/en), versión de la app, link a licencias | DataStore |

### 4.2 SHOULD (sliceables a chained PRs siguientes si exceden budget)

- Confirmación destructiva (delete conexión, DELETE/UPDATE sin WHERE detectado heurísticamente).
- Indicador de conexión activa global (TopBar chip).
- Historial básico de queries (DataStore o Room — empuja a v1.2 si se complica).

### 4.3 OUT OF SCOPE en esta change

- Autenticación (login Google/Apple/Email) — feature aparte (roadmap v1.0 separado).
- Edición visual de filas (insertar/actualizar/eliminar desde grid) — empuja a `data-editor` change.
- Export CSV/JSON/SQL — change separado.
- SSH tunneling UI — change `ssh-tunneling`.
- Backup & Restore UI — change `backup-restore`.
- PostgreSQL/SQLite UI — bloqueado hasta v1.1 (engine no implementado).

### 4.4 Mapa de navegación propuesto

```
NavHost (start = connections_list)
├── connections_list
│   ├── connection_form?id={configId?}
│   └── explorer/databases?connectionId={id}
│       └── explorer/tables?database={name}
│           └── explorer/table?database={name}&table={name}
│               └── tabs: columns | indexes | foreign_keys
├── query?database={name}
└── settings
```

Profundidad máxima: 3 niveles (cumple con la guía “máximo 3 niveles” del spec heredado).

---

## 5. Comparación de enfoques

### 5.1 Navegación: Navigation Compose vs manual state

| Criterio | Navigation Compose (2.7.7) | Manual state holder |
|----------|---------------------------|---------------------|
| Back stack / system back | Nativo, correcto | Hay que reimplementarlo |
| Deep links | Soportado out-of-the-box | Manual |
| Type safety | Mejorable; con `2.7.x` se usa string routes + args (typesafe llega estable en 2.8.x) | Total con sealed classes |
| Integración con Hilt | `hiltViewModel()` + `hilt-navigation-compose` | Manual |
| Costo | Curva baja, ya en deps | Curva alta, propenso a bugs |
| Adaptive (rail/drawer/bottom) | Compatible vía `NavBackStackEntry` + `currentDestination` | Igual de fácil/difícil |
| Test | `ComposeTestRule` + `NavController` mockeable | Más fácil unitariamente |
| Recomendación equipo Android | Estándar de facto | Solo en apps muy pequeñas |

**Conclusión**: **Navigation Compose 2.7.7** con string routes + sealed class de destinos para encapsular `route` + `args`. Migrar a typesafe (2.8.x) cuando sea estable es trivial porque el sealed class central absorbe el cambio.

Riesgo: en 2.7.7 los argumentos van como string; envolver en un wrapper `object Routes { fun connectionForm(id: String?) = ... }` evita strings sueltos en composables.

### 5.2 Theming: Material 3 dynamic color vs custom theme

| Criterio | Dynamic Color (Material You) | Custom palette fija |
|----------|------------------------------|---------------------|
| Coherencia de marca | Cambia por wallpaper del usuario | Marca consistente |
| Identidad MyDataBases | Diluida | Fuerte |
| Soporte | Android 12+ (S+). Fallback obligatorio en API 29–30 (~25–30% del parque base aún) | Funciona en TODO Min SDK 29 |
| Trabajo extra | Requiere paleta de fallback igual | Una sola paleta bien definida |
| Accesibilidad / contraste | Dinámico → impredecible; auditar es difícil | Auditable contra WCAG fácilmente |
| Dark mode | Soportado en ambos | Soportado en ambos |
| Estado actual del repo | Ya activado en `Theme.kt` (`dynamicColor = true`) con paleta de fallback **incompleta** | — |

**Trade-off real**: el spec heredado dice “simple y fácil de usar” y “estándar de apps modernas”, pero también define la identidad de marca como herramienta profesional. Una app de DBA quiere reconocimiento visual estable (verde Sphynxs).

**Conclusión propuesta**: **paleta custom completa Material 3 como default**, con dynamic color como **opción** controlada desde Settings (off por default). Esto:
- Garantiza branding consistente.
- Cumple Min SDK 29 sin código condicional en la UI.
- Mantiene accesibilidad auditable.
- Respeta a usuarios que sí quieren Material You (opt-in).

Acción concreta: completar `Color.kt` con la paleta full (16+ tokens light + 16+ tokens dark), y cambiar `dynamicColor` de `true` a `false` por default (leído desde DataStore).

### 5.3 Modularización: single-module vs multi-module por features

`.atl/architecture/modules.md` propone explícitamente módulos Gradle (`feature-connections`, `feature-explorer`, `feature-editor`, `core-ui`, `core-designsystem`, etc.). Hoy el proyecto es **single-module** (`:app`).

| Criterio | Mantener single-module por ahora | Migrar ya a multi-module |
|----------|----------------------------------|--------------------------|
| Tiempo de implementación v1.0 | Bajo | Alto (config Gradle, KSP por módulo, Hilt across modules) |
| Build incremental | Recompila todo | Compila solo el módulo tocado |
| Aislamiento de features | Por package (convención) | Forzado por Gradle |
| Riesgo de “feature-a depende de feature-b” | Convención, fácil violar | Imposible por compilador |
| Setup de Hilt | Trivial | Hay que mover el `@HiltAndroidApp` y `EntryPoint`s con cuidado |
| ProGuard / R8 | Una sola config | Reglas por módulo, más superficie |
| Onboarding nuevos devs | Más simple | Más curvas |
| Encaja con engine actual | ✅ (ya está en `core/database` por package) | Requiere mover engine a `:core-database` |

**Trade-off**: el roadmap es ambicioso (v2.0+ con designer visual, IA, múltiples motores). En ese horizonte, multi-module paga. Pero introducir 8 módulos AHORA para entregar solo MySQL/MariaDB con UI básica es over-engineering antes de validar.

**Conclusión recomendada**: **single-module con package-based feature split por ahora**, con una regla de disciplina: cada feature vive en `presentation/{feature}/` y NO importa de otro feature. Programar la migración a multi-module como **change separada** cuando aparezca el segundo feature pesado (probablemente al introducir editor SQL avanzado en v1.2 o PostgreSQL en v1.1).

Estructura de packages propuesta (single-module):

```
com.sphynxs.mydatabases/
├── core/
│   ├── database/        (existente)
│   ├── persistence/     (nuevo — Room + ConnectionConfigDao)
│   ├── security/        (nuevo — encriptación passwords)
│   └── di/              (nuevo — módulos Hilt)
├── domain/
│   └── usecases/        (existente; agregar GetSavedConnections, SaveConnection, DeleteConnection)
├── ui/
│   ├── theme/           (existente; completar)
│   ├── navigation/      (nuevo)
│   ├── components/      (nuevo — AdaptiveScaffold, ResultTable, EmptyState, ...)
│   ├── connections/     (nuevo — list/form + ViewModel)
│   ├── explorer/        (nuevo — databases/tables/detail + ViewModel)
│   ├── query/           (nuevo — editor + ViewModel)
│   └── settings/        (nuevo — settings + ViewModel)
├── MainActivity.kt
└── MyDataBasesApplication.kt
```

Esto deja el camino libre para mover cada `ui/{feature}/` a `:feature-{name}` cuando llegue el momento, sin reescribir lógica.

---

## 6. Decisiones técnicas adicionales que entran en este scope

- **State management**: MVVM con `ViewModel` + `StateFlow<UiState>` (sealed: `Idle | Loading | Success(data) | Error(message)`). Inyectado vía `hiltViewModel()`. Nada de `LiveData` (proyecto 100% Kotlin/Compose/Flow).
- **Adaptive layout**: `calculateWindowSizeClass(activity)` en `MainActivity`, pasado por composition local o como parámetro al `NavHost`. Compact → BottomNav. Medium → NavigationRail. Expanded → PermanentDrawer + list-detail multi-pane en Connections y Explorer.
- **i18n**: poblar `values/strings.xml` (en) y crear `values-es/strings.xml`. Ninguna string hardcoded en composables.
- **Error mapping**: una función central `DatabaseError → @StringRes` (lives en `ui/components/error` o en `presentation/util`).
- **Async**: `viewModelScope.launch { repository.x().onSuccess/onFailure }`. Cancelación de queries largas vía `Job` cancelable expuesto desde el ViewModel.
- **Testing**: cada ViewModel con unit tests (Mockk + `kotlinx-coroutines-test`); pantallas críticas (Form, Editor) con Compose UI tests en `androidTest`.
- **TDD**: dado `strict_tdd: true`, los tasks DEBEN escribir tests primero — esto se materializa en la fase tasks/apply, NO aquí.

---

## 7. Recommendation

**Propuesta para la fase Proposal/Spec/Design**:

1. **Scope v1.0 UI** = pantallas 1–8 de la sección 4.1 (Connections List, Form, Test, Explorer Databases/Tables/Detail, Query, Settings).
2. **Navegación** = Navigation Compose 2.7.7 con sealed class `Routes`. Sin migración a typesafe (2.8.x) en esta change.
3. **Theming** = paleta Material 3 custom completa light + dark; `dynamicColor` configurable desde Settings (off por default). Completar `Color.kt` y `Type.kt`.
4. **Arquitectura** = single-module con package split por feature; preparar la migración a multi-module como change futura.
5. **Adaptive** = WindowSizeClass desde `MainActivity`; BottomNav / NavigationRail / PermanentDrawer; list-detail en Compact vs Medium/Expanded.
6. **Persistencia local de conexiones** = Room + `ConnectionConfigEntity` + mapper + DAO. Password encriptado via Android Keystore (delegar la criptografía concreta a una sub-change `core-security` si excede el budget, y usar EncryptedSharedPreferences interino como placeholder con TODO).
7. **i18n** = `values/strings.xml` (en) y `values-es/strings.xml`, ambos completos desde el día 1.

### Forecast de tamaño y budget

Review budget de la sesión: **800 líneas**. La UI completa (8 pantallas + theme + nav + persistencia + i18n + tests) **excede claramente** ese budget. Estimación gruesa:

- Theme completo + strings es/en: ~150 líneas
- Navigation + AdaptiveScaffold + componentes comunes: ~300 líneas
- Persistencia local (Room + DAO + mapper + DI): ~250 líneas + tests ~200
- Connections (list + form + test) + ViewModels + tests: ~500 líneas
- Explorer (3 pantallas) + ViewModels + tests: ~450 líneas
- Query (editor + result table) + ViewModel + tests: ~400 líneas
- Settings + ViewModel + tests: ~150 líneas

**Total estimado**: ~2.400 líneas. Triplica el budget.

**Recomendación al orquestador**: planificar **chained PRs** (orden sugerido):
1. PR-1: Theme completo + strings es/en + AdaptiveScaffold + Navigation skeleton (Settings stub) → ~600 líneas.
2. PR-2: Persistencia local + Connections (list/form/test) → ~750 líneas.
3. PR-3: Explorer (databases/tables/detail) → ~700 líneas.
4. PR-4: Query Editor + Result Table → ~600 líneas.
5. PR-5: Settings real (tema/idioma toggles) + pulido → ~300 líneas.

Esto se decide formalmente en `sdd-tasks`, pero el orquestador YA debería avisar al usuario que esta change va a chained PRs (porque la PR strategy es `ask-always`).

### Ready for Proposal

**Yes** — con la siguiente acción inmediata para el orquestador:

> Avisar al usuario que el UI completo excede el budget de 800 líneas en una sola PR y proponer estrategia chained PRs (5 PRs sugeridos). Cuando el usuario confirme estrategia, lanzar `sdd-propose` para `ui-implementation`.

---

## 8. Risks

1. **Password encryption no implementado todavía**. El engine ya espera passwords en `ConnectionConfig`, pero el spec heredado dice “Passwords encriptados con Android Keystore” y eso aún no existe. Mitigación: empezar con EncryptedSharedPreferences (más simple, soportado desde API 23) o `androidx.security:security-crypto`. Documentar como deuda en el proposal.

2. **`mysql-connector-java` en Android**. El driver oficial de MySQL puede traer issues de DEX size / multidex / clases no soportadas en Android runtime. El engine ya está mergeado (presumiblemente funciona en device), pero conviene verificar APK size una vez que se sumen Compose + Room + Hilt en pantallas reales. NFR-006 fija budget de < 15MB.

3. **`dynamicColor = true` actual** introduce variabilidad visual no auditable. Si lo dejamos así, las screenshots de Play Store cambiarán según el dispositivo. Recomendación: cambiar default a `false` antes de release.

4. **`mysql-connector-java:8.0.33`** está deprecado a favor de `com.mysql:mysql-connector-j`. No es bloqueante para esta change, pero anotar como deuda.

5. **Adaptive layout en Connection Form**. El form tiene ~10 campos; en Compact requiere scroll, en Medium/Expanded conviene dual-column (el estándar `adaptive-layouts.md` ya muestra patrón). Estimar bien el esfuerzo en `sdd-design`.

6. **Result table de queries arbitrarias** puede tener cientos de columnas o filas enormes. Necesitamos paginación + horizontal scroll + virtualización (`LazyVerticalGrid` no encaja bien aquí; mejor `LazyColumn` con `Row` interno y `LazyRow` o un `HorizontalPager`). Esto es una decisión de Design, NO bloqueante para Proposal.

7. **Hilt + Compose + Multi-module en el futuro**. Si se difiere multi-module, la migración futura tiene costo de re-cablear `@HiltAndroidApp`, `EntryPoint`s y assemblies. Recomendación: estructurar la DI en módulos Hilt separados (`PresentationModule`, `PersistenceModule`, `DatabaseModule` ya existe) para que la migración sea movimiento de carpetas, no rewrite.

8. **`composeOptions.kotlinCompilerExtensionVersion = "1.5.8"`** está atado a Kotlin 1.9.22. Cualquier bump de Kotlin requiere actualizar el extension version en sincronía. No es de esta change, pero advertir.

---

## 9. Notas para fases siguientes

- **sdd-propose**: tomar este scope (pantallas 1–8) y enmarcarlo. Incluir rollback plan: dado que es solo presentación + persistencia local nueva, el rollback es revertir las PRs; los datos guardados en Room/DataStore se pierden, lo cual es aceptable porque son configuraciones de usuario fáciles de reingresar.
- **sdd-spec**: usar Given/When/Then con RFC 2119. Cubrir adaptive behavior por WindowSizeClass para cada pantalla y locales es/en en TODOS los strings.
- **sdd-design**: ADR sugerido: ADR-002 Navigation Compose + sealed Routes, ADR-003 Theming (custom paleta + dynamic opt-in), ADR-004 Single-module + package split (con plan de migración).
- **sdd-tasks**: dividir en 5 slices según la sección 7 “Forecast de tamaño”. Forzar las guard lines de review budget.

---

**Status**: Listo para Proposal.
