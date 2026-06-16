# ui-localization Specification

## Purpose

Full Spanish/English localization with runtime locale switching via `AppCompatDelegate`, DataStore-backed persistence, and Spanish as the default locale.

## Requirements

### Requirement: Supported Locales

The system MUST support `es` (default) and `en`. All user-facing strings MUST exist in both `res/values-es/strings.xml` and `res/values/strings.xml`.

#### Scenario: Default locale is Spanish

- GIVEN a fresh install with no user locale preference
- WHEN the app starts
- THEN all user-facing strings render in Spanish regardless of OS locale

#### Scenario: No missing string

- GIVEN any string id used in a Composable via `stringResource(...)`
- WHEN the app builds
- THEN the id MUST resolve in BOTH `values/strings.xml` AND `values-es/strings.xml`

### Requirement: Runtime Locale Switch

The user MUST be able to switch the locale at runtime from the Settings screen. The switch MUST be applied via `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))`.

#### Scenario: Switch from es to en

- GIVEN the app is running in Spanish
- WHEN the user selects "English" in Settings
- THEN within one second all visible strings render in English

#### Scenario: Switch persisted across restart

- GIVEN the user switched to English
- WHEN the user closes and reopens the app
- THEN the app starts in English

### Requirement: Locale Persistence

The selected locale MUST be persisted in DataStore Preferences as an IETF BCP 47 language tag (`es`, `en`).

#### Scenario: Persistence write

- GIVEN the user changes locale
- WHEN the change is committed
- THEN DataStore stores the tag AND `AppCompatDelegate.getApplicationLocales()` returns the same tag

### Requirement: Hardcoded Strings Forbidden

No user-facing string SHALL be hardcoded in Kotlin source. All text MUST come from `stringResource(R.string.*)`.

#### Scenario: No hardcoded text

- GIVEN any Composable in `presentation/`
- WHEN it renders a `Text(...)` or `Button(...)` with label
- THEN the value MUST come from `stringResource` (the linter or code review SHALL reject hardcoded literals)

### Requirement: KDoc Language

Public APIs MUST be documented in Spanish per project standard. Inline code comments MAY be Spanish or English at the author's choice.

#### Scenario: Public API KDoc

- GIVEN a public function, class, or property
- WHEN it has KDoc
- THEN the prose is in Spanish

## Non-Functional

- **Performance**: Locale change MUST complete within 1 second on a mid-range device including activity recreation.
- **Accessibility**: All strings MUST also serve as content descriptions where appropriate; both languages MUST be reviewed for clarity by a fluent speaker before release.
- **Testability**: String coverage parity (es vs en) SHALL be enforceable by a unit test that diffs the two `strings.xml` files.
- **Compatibility**: Locale switch MUST work on Android 12 (API 32) and below via `AppCompatDelegate` backport.
