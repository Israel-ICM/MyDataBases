# Specs Index — ui-implementation

This change introduces 7 NEW capabilities. No existing specs were modified.

Each capability is defined as a full spec under `specs/{capability}/spec.md` in this change folder. They will be merged into `openspec/specs/{capability}/spec.md` at archive time.

## Capabilities

| Capability | Spec | Requirements | Focus |
|------------|------|--------------|-------|
| `ui-navigation` | [specs/ui-navigation/spec.md](specs/ui-navigation/spec.md) | 5 | Typed routes, single-activity, back stack, adaptive scaffold |
| `ui-theme` | [specs/ui-theme/spec.md](specs/ui-theme/spec.md) | 5 | Material 3 light/dark/system, dynamic color (API 31+), brand fallback |
| `ui-localization` | [specs/ui-localization/spec.md](specs/ui-localization/spec.md) | 5 | es (default) + en, runtime switch via `AppCompatDelegate`, no hardcoded strings |
| `connection-management` | [specs/connection-management/spec.md](specs/connection-management/spec.md) | 7 | CRUD, test connection, encrypted credentials via `androidx.security.crypto` |
| `database-browser` | [specs/database-browser/spec.md](specs/database-browser/spec.md) | 6 | Databases → tables → rows, schema tab, 1000-row pagination |
| `query-runner` | [specs/query-runner/spec.md](specs/query-runner/spec.md) | 7 | SQL editor, result grid, cancellation, 1000-row cap |
| `app-settings` | [specs/app-settings/spec.md](specs/app-settings/spec.md) | 6 | Theme / dynamic color / locale selectors, immediate application |

## Cross-cutting Non-Functional Themes

- **Security**: No plaintext credentials in Room, DataStore, or logs. Master key in Android Keystore, `AES256_GCM`.
- **Localization**: All user-facing strings in both `values/strings.xml` and `values-es/strings.xml`. Parity enforced by test.
- **Adaptive UI**: `WindowSizeClass` resolved at activity, exposed via `CompositionLocal`. Compact = single pane; Medium/Expanded MAY render list+detail.
- **Accessibility**: WCAG AA contrast, content descriptions for icons, single focusable rows for settings preferences, all localized in es and en.
- **State**: MVVM with Hilt-injected ViewModels exposing `StateFlow<UiState>`; sealed `UiState` with `Loading | Success | Empty | Error` semantics.
- **Performance**: Navigation transitions < 100ms; initial result render < 1.5s on LAN; theme switch ≤ 1 frame.

## Coverage Summary

- Happy paths: covered for all 7 capabilities
- Edge cases: covered (empty states, invalid input, cancelled actions, version-gated features)
- Error states: covered (network loss, permission denied, syntax errors, validation failures)

## Next Step

Ready for `sdd-design`.
