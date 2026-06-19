# Add Database Form Specification

## Purpose

Defines the `AddDatabaseScreen` reached at `connection/{connectionId}/add-database`. This change introduces ONLY the form UI for creating a database/schema: a required name input, an optional charset input, and an optional collation input. SQL execution and driver integration are out of scope; the form's "Create" action is wired but its backend invocation is deferred to a follow-up change.

## Requirements

### Requirement: Form Fields and Defaults

The screen MUST present three input fields: `name` (required), `charset` (optional), `collation` (optional). The screen MUST display field labels in the device locale and MUST mark `name` as required.

#### Scenario: Initial render shows three empty fields

- GIVEN the user navigates to `connection/c-42/add-database`
- WHEN `AddDatabaseScreen` renders for the first time
- THEN three input fields are visible in this order: name, charset, collation
- AND all fields are empty
- AND the name field is marked as required (visual `*` or "Required" indicator)

#### Scenario: Fields have localized labels

- GIVEN the device locale is `en`
- WHEN the screen renders
- THEN labels read: "Name", "Charset (optional)", "Collation (optional)"

- GIVEN the device locale is `es`
- WHEN the screen renders
- THEN labels read: "Nombre", "Charset (opcional)", "Collation (opcional)"

#### Scenario: Charset and collation default to placeholder hints

- GIVEN the charset and collation inputs are empty
- WHEN the user focuses each field
- THEN a placeholder hint suggests common values (e.g. `utf8mb4`, `utf8mb4_unicode_ci`)
- AND the placeholder MUST NOT be submitted as a real value

### Requirement: Name Validation

The system MUST validate the `name` field on every change and on submit. The `name` MUST be non-empty after trimming whitespace. The `name` MUST match `^[A-Za-z0-9_]{1,64}$` (alphanumeric and underscore, 1-64 chars).

#### Scenario: Empty name disables Create button

- GIVEN the name field is empty
- WHEN the user looks at the Create button
- THEN the button is disabled
- AND no validation error is shown until the user attempts to submit or blurs the field

#### Scenario: Invalid characters in name

- GIVEN the user types `my-db!` into the name field
- WHEN the field loses focus or the user taps Create
- THEN an inline error appears under the name field
- AND the error message is localized: "Only letters, numbers and underscore" (en) / "Solo letras, números y guion bajo" (es)
- AND the Create button remains disabled

#### Scenario: Name exceeds 64 characters

- GIVEN the user types a 65-character name
- WHEN the field is evaluated
- THEN an inline error appears: "Max 64 characters" (en) / "Máximo 64 caracteres" (es)
- AND the Create button is disabled

#### Scenario: Valid name enables Create

- GIVEN the user types `analytics_2026` into the name field
- WHEN the name passes validation
- THEN no inline error is shown
- AND the Create button is enabled (assuming charset/collation are empty or also valid)

### Requirement: Optional Charset and Collation

`charset` and `collation` MUST be optional. If empty, the form MUST treat them as "not specified". If non-empty, each MUST match `^[A-Za-z0-9_]{1,64}$`.

#### Scenario: Empty optional fields do not block submit

- GIVEN name is valid and charset and collation are empty
- WHEN the user looks at the Create button
- THEN the button is enabled

#### Scenario: Invalid charset shows inline error

- GIVEN the user types `utf8 mb4` (with a space) into the charset field
- WHEN the field is evaluated
- THEN an inline error appears under charset
- AND the Create button is disabled until corrected

#### Scenario: Invalid collation shows inline error

- GIVEN the user types `utf8mb4;DROP` into the collation field
- WHEN the field is evaluated
- THEN an inline error appears under collation
- AND the Create button is disabled

### Requirement: Submission Wiring (No SQL Execution in This Change)

Tapping Create MUST collect the form values and invoke an `onSubmit(name, charset?, collation?)` callback on the ViewModel. In this change, the ViewModel MUST log the submission and surface a "Not yet implemented" notice; it MUST NOT execute `CREATE DATABASE` against the server.

#### Scenario: Create dispatches to ViewModel

- GIVEN name is `analytics_2026`, charset is empty, collation is `utf8mb4_unicode_ci`
- WHEN the user taps Create
- THEN the ViewModel's `onSubmit` receives `("analytics_2026", null, "utf8mb4_unicode_ci")`

#### Scenario: ViewModel surfaces deferred-execution notice

- GIVEN `onSubmit` is invoked with valid inputs
- WHEN the ViewModel processes the call
- THEN the UI shows a snackbar/toast: "Coming soon — DB creation will be wired in a follow-up" (en) / "Próximamente — la creación se conectará en un cambio futuro" (es)
- AND no network or driver call is made
- AND `MySQLConnectionPool` is not touched

#### Scenario: No persistence on submit

- GIVEN the user submits a valid form
- WHEN the submission completes
- THEN no row is written to Room
- AND no DataStore entry is mutated

### Requirement: Cancel and Back Behavior

The screen MUST provide a way to return to `connection/{connectionId}/databases` without submitting. The system back action MUST pop to the database list.

#### Scenario: System back returns to database list

- GIVEN the user is on `connection/c-42/add-database`
- WHEN the user presses the system back button
- THEN the NavController pops to `connection/c-42/databases`
- AND no submission is performed

#### Scenario: Cancel button (if present) returns to database list

- GIVEN a Cancel/Close affordance is present in the top bar
- WHEN the user taps it
- THEN the NavController pops to `connection/c-42/databases`

### Requirement: Adaptive Layout

The form MUST be usable in `Compact`, `Medium`, and `Expanded` WindowSizeClass.

#### Scenario: Compact stacks fields vertically

- GIVEN WindowSizeClass is `Compact`
- WHEN the screen renders
- THEN the three fields stack vertically with the Create button below

#### Scenario: Medium/Expanded constrains form width

- GIVEN WindowSizeClass is `Medium` or `Expanded`
- WHEN the screen renders
- THEN the form is constrained to a readable max width (e.g. 480dp)
- AND it is horizontally centered or aligned to the leading edge consistent with the rest of the app

### Requirement: Accessibility

All fields, the Create button, and any inline errors MUST be accessible.

#### Scenario: Field labels are announced by screen readers

- GIVEN TalkBack is on
- WHEN the user focuses each field
- THEN the localized label is announced
- AND any inline error message is announced when present

#### Scenario: Create button state is announced

- GIVEN the Create button is disabled
- WHEN a screen reader focuses it
- THEN it is announced as disabled
