# app-settings Specification

## Purpose

User-facing preferences screen for theme mode, dynamic color toggle (Android 12+), and locale, persisted in DataStore and applied immediately.

## Requirements

### Requirement: Settings Entry Point

The system MUST expose a Settings entry point from the Connections screen top app bar.

#### Scenario: Open from Connections

- GIVEN the user is on the Connections screen
- WHEN the user taps the Settings icon
- THEN the Settings screen is pushed onto the back stack

### Requirement: Theme Mode Selector

The screen MUST offer a single-selection control for `LIGHT | DARK | SYSTEM`. The current selection MUST reflect the persisted value.

#### Scenario: Change to dark

- GIVEN the current mode is `SYSTEM`
- WHEN the user selects `DARK`
- THEN the app re-renders in dark theme immediately AND DataStore is updated

#### Scenario: Persisted reflection

- GIVEN the persisted mode is `DARK`
- WHEN Settings opens
- THEN `DARK` is selected in the UI

### Requirement: Dynamic Color Toggle

On API 31+ the screen MUST expose a "Use system colors" switch. On API 30 and below the switch MUST be hidden.

#### Scenario: Toggle visible on Android 12+

- GIVEN the device runs API 31+
- WHEN Settings renders
- THEN the dynamic color switch is visible

#### Scenario: Toggle hidden on older

- GIVEN the device runs API 30
- WHEN Settings renders
- THEN the dynamic color switch is NOT visible AND only the brand palette applies

#### Scenario: Enable dynamic color

- GIVEN the user enables the dynamic color switch on API 31+
- WHEN the change is committed
- THEN the theme rebuilds its `ColorScheme` from `dynamic*ColorScheme(context)`

### Requirement: Locale Selector

The screen MUST offer a single-selection control for `Español | English`. The current selection MUST reflect the persisted locale.

#### Scenario: Change to English

- GIVEN the current locale is `es`
- WHEN the user selects "English"
- THEN `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))` is called AND DataStore stores `en`

#### Scenario: Persisted reflection

- GIVEN the persisted locale is `en`
- WHEN Settings opens
- THEN "English" is selected in the UI

### Requirement: Immediate Application

Theme and locale changes MUST take effect immediately. The user MUST NOT need to restart the app to see the new theme or language.

#### Scenario: Theme applied immediately

- GIVEN the user changes the theme mode
- WHEN the new value is persisted
- THEN the next composition frame renders with the new `ColorScheme`

#### Scenario: Locale applied immediately

- GIVEN the user changes the locale
- WHEN the new value is persisted
- THEN within 1 second all visible strings render in the new language

### Requirement: No Restart Required

The system MUST handle activity recreation triggered by locale change without losing user position (e.g., user remains on Settings, not bumped to start destination).

#### Scenario: Survive recreation

- GIVEN the user is on Settings and changes locale
- WHEN the activity recreates
- THEN the user is still on Settings with the new locale applied

## Non-Functional

- **Performance**: Settings screen MUST render within 200ms. Preference reads MUST use cached `StateFlow` (no UI-thread DataStore reads).
- **Security**: Settings store no sensitive data; plain DataStore Preferences acceptable.
- **Testability**: `SettingsRepository` MUST be unit-testable with a fake DataStore. ViewModel MUST emit a single `SettingsUiState`.
- **Accessibility**: Each preference row MUST be a single focusable element with role and state (selected / unselected) exposed to TalkBack in both languages.
