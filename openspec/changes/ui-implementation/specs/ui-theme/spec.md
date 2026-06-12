# ui-theme Specification

## Purpose

Material 3 theming with light/dark/system mode, optional dynamic color on Android 12+, and DataStore-backed persistence of user preference.

## Requirements

### Requirement: Theme Mode Selection

The system MUST support three theme modes: `LIGHT`, `DARK`, `SYSTEM`. The default SHALL be `SYSTEM`.

#### Scenario: Default is system

- GIVEN a fresh install
- WHEN the user opens the app
- THEN the theme follows the OS dark/light setting

#### Scenario: Explicit dark mode

- GIVEN the user selected `DARK` in Settings
- WHEN any screen renders
- THEN it uses the dark `ColorScheme` regardless of OS setting

### Requirement: Persistence Across Restart

The selected theme mode MUST be persisted in DataStore Preferences and applied before the first frame on next launch.

#### Scenario: Persisted across cold start

- GIVEN the user set theme to `DARK` and closed the app
- WHEN the user reopens the app
- THEN the first composition renders the dark `ColorScheme` (no light-to-dark flash)

### Requirement: Dynamic Color Support

On Android 12+ (API 31+), the system SHOULD offer a "Use system colors" toggle. When enabled, the theme SHALL derive its `ColorScheme` from `dynamicLightColorScheme`/`dynamicDarkColorScheme`.

#### Scenario: Dynamic color on Android 12+

- GIVEN the device runs API 31+ AND dynamic color is enabled
- WHEN the theme builds the `ColorScheme`
- THEN it uses `dynamicLightColorScheme(context)` or `dynamicDarkColorScheme(context)` per mode

#### Scenario: Dynamic color unavailable

- GIVEN the device runs API 30 or below
- WHEN the theme builds the `ColorScheme`
- THEN the dynamic color toggle MUST be hidden in Settings AND the brand fallback palette is used

### Requirement: Brand Fallback Palette

When dynamic color is disabled or unavailable, the system MUST use the brand palette defined in `ui/theme/Color.kt`. Color tokens MUST be defined only once.

#### Scenario: Brand palette consistency

- GIVEN dynamic color is off
- WHEN light or dark theme renders
- THEN all surfaces, primary, secondary, and error tokens come from the brand palette

### Requirement: Single Theme Composable

All screens MUST be wrapped in a single `MyDataBasesTheme` composable. Screens MUST NOT call `MaterialTheme { ... }` directly.

#### Scenario: Theme wrapping

- GIVEN any screen
- WHEN it renders
- THEN it inherits `MaterialTheme.colorScheme` from `MyDataBasesTheme`

## Non-Functional

- **Performance**: Theme switch SHOULD complete within one frame (16ms target). Recomposition scope SHOULD be limited to the `NavHost` subtree.
- **Accessibility**: Color contrast MUST meet WCAG AA (4.5:1 for text) in both light and dark schemes.
- **Security**: Theme preference contains no sensitive data — plain DataStore Preferences is acceptable.
- **Testability**: `ThemeMode` resolution from preference SHALL be unit-testable without Compose.
