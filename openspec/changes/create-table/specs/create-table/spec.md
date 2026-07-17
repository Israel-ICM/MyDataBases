# Create Table Specification

## Purpose

Enables a user inside a database's Tables screen to create a new table by entering a
table name, defining ordered column definitions through a nested field-definition form,
and executing real `CREATE TABLE` DDL against the active MySQL/MariaDB connection.

## Requirements

### Requirement: Open Create Table Sheet

The system MUST open a "Crear tabla" `ModalBottomSheet` when the user triggers the
existing `new_table` bottom-nav modal action while the `TablesListScreen` is active.

#### Scenario: Trigger opens the sheet

- GIVEN the user is on `TablesListScreen` for a connected MySQL/MariaDB database
- WHEN the user taps the `new_table` bottom-nav modal action
- THEN the "Crear tabla" `ModalBottomSheet` MUST be presented over the current screen
- AND the sheet MUST open with an empty table-name field and an empty fields list

#### Scenario: Sheet opens fresh after prior dismiss

- GIVEN the sheet was previously opened and dismissed with entered data
- WHEN the user taps `new_table` again
- THEN the sheet MUST present cleared state (no residual name or fields)

### Requirement: Sheet Layout Order

The parent sheet MUST render controls top-to-bottom in this exact order: (1) table-name
field, (2) fields/columns list, (3) "+ Agregar campo" button, (4) OK/Cancel actions.

#### Scenario: Vertical ordering is enforced

- GIVEN the "Crear tabla" sheet is open
- WHEN the sheet content is rendered
- THEN the table-name field MUST appear first, then the fields list, then the
  "+ Agregar campo" button, then the OK and Cancel actions in that order

### Requirement: Open Field Definition Dialog

<!-- MODIFIED in change `create-table`: added the Expresión (Expression) input; input
count changed from eight to nine. Previously the form exposed eight inputs and no
Expresión field. -->

Tapping "+ Agregar campo" MUST open a sheet-styled `Dialog` containing the field-definition
form with inputs: Nombre, Tipo, Longitud, Decimales, Nulo, Virtual, Expresión, Llave,
Comentario. The Expresión input MUST be conditionally shown per the "Expression Field
Applicability" requirement.

#### Scenario: Dialog opens with empty form

- GIVEN the "Crear tabla" sheet is open
- WHEN the user taps "+ Agregar campo"
- THEN a sheet-styled `Dialog` MUST appear showing the field-definition inputs
- AND all inputs MUST start empty/default with the parent sheet remaining composed behind it
- AND the Expresión input MUST be hidden while Virtual = false (its default)

### Requirement: Field Name Validation

Nombre MUST be required, non-blank, and a valid SQL identifier matching
`^[A-Za-z0-9_]{1,64}$` (no spaces, no reserved/special characters).

#### Scenario: Blank name is rejected

- GIVEN the field-definition dialog is open with Nombre blank
- WHEN the user taps OK
- THEN a localized validation error MUST be shown on Nombre
- AND the dialog MUST stay open and MUST NOT append a field

#### Scenario: Invalid identifier is rejected

- GIVEN Nombre contains a space or reserved character (e.g. `user name`)
- WHEN the user taps OK
- THEN a localized "invalid identifier" error MUST be shown and no field appended

### Requirement: Field Type Required

Tipo MUST be required and selected from the supported MySQL/MariaDB column-type list.

#### Scenario: Missing type is rejected

- GIVEN Nombre is valid but Tipo is unselected
- WHEN the user taps OK
- THEN a localized validation error MUST be shown on Tipo and no field appended

### Requirement: Length Field Applicability

Longitud MUST be enabled only for length-bearing types (VARCHAR, CHAR, DECIMAL, NUMERIC);
for all other types it MUST be disabled/hidden and its value ignored.

#### Scenario: Length enabled for VARCHAR

- GIVEN Tipo is set to VARCHAR
- THEN Longitud MUST be enabled and editable

#### Scenario: Length disabled for non-length type

- GIVEN Tipo is set to a non-length type (e.g. INT, TEXT, DATETIME)
- THEN Longitud MUST be disabled/hidden and excluded from the field definition

### Requirement: Decimals Field Applicability

Decimales MUST be enabled only for numeric/decimal types (DECIMAL, NUMERIC, FLOAT, DOUBLE);
for all other types it MUST be disabled/hidden and its value ignored.

#### Scenario: Decimals enabled for DECIMAL

- GIVEN Tipo is set to DECIMAL
- THEN Decimales MUST be enabled and editable

#### Scenario: Decimals disabled for non-numeric type

- GIVEN Tipo is set to VARCHAR or INT
- THEN Decimales MUST be disabled/hidden and excluded from the field definition

### Requirement: Key Forces Not Null

<!-- MODIFIED in change `create-table` (generated-column support): scoped to non-generated
columns. For generated columns the Nulo control is hidden/disabled regardless of Llave (see
"Virtual Column Semantics"). Previously this rule was unconditional. -->

When Virtual = false AND Llave (Key) is true, the system MUST force Nulo (Nullable) to false
and disable the Nulo control, reflecting that a key column cannot be nullable. When Virtual =
true this rule does not apply, because the Nulo control is already hidden/disabled for
generated columns and nullability is derived from the expression.

#### Scenario: Enabling Key forces Not Null

- GIVEN the dialog has Nulo = true
- WHEN the user sets Llave = true
- THEN Nulo MUST be forced to false AND the Nulo control MUST be disabled

#### Scenario: Disabling Key re-enables Nullable

- GIVEN Llave = true with Nulo disabled at false
- WHEN the user sets Llave = false
- THEN the Nulo control MUST be re-enabled (retaining false until changed)

### Requirement: Expression Field Applicability

<!-- ADDED in change `create-table` (generated-column support). -->

The Expresión (Expression) input MUST be shown and MUST be required (non-blank) only when
Virtual = true; when Virtual = false it MUST be hidden and its value ignored. Expresión is
free-text and MUST be treated as an opaque SQL expression: the client MUST NOT attempt to
parse or semantically validate the expression. Invalid SQL expressions are an accepted
limitation and MUST surface only through the DDL execution failure path (see "Parent OK
Executes DDL"). The only client-side validation MUST be the non-blank requirement.

#### Scenario: Expression shown and required when Virtual is true

- GIVEN the field-definition dialog with Virtual = false and Expresión hidden
- WHEN the user sets Virtual = true
- THEN the Expresión input MUST become visible AND MUST be marked required

#### Scenario: Blank expression rejected when Virtual is true

- GIVEN Virtual = true and Expresión is blank
- WHEN the user taps OK in the dialog
- THEN a localized validation error MUST be shown on Expresión and no field appended

#### Scenario: Expression hidden and ignored when Virtual is false

- GIVEN Virtual = true with an entered Expresión
- WHEN the user sets Virtual = false
- THEN the Expresión input MUST be hidden AND its value MUST be excluded from the field definition

#### Scenario: Expression is not parsed client-side

- GIVEN Virtual = true and Expresión contains a non-blank but syntactically invalid SQL fragment
- WHEN the user taps OK in the dialog
- THEN the field MUST be appended without client-side SQL validation
- AND any error MUST surface later only when the parent OK executes the DDL

### Requirement: Virtual Column Semantics

<!-- MODIFIED in change `create-table` (generated-column support). Previously the Virtual
flag only suppressed default handling with NO DDL effect. Now Virtual = true emits real
MySQL/MariaDB generated-column DDL. Previously Nulo behavior for Virtual columns was
unspecified; now the Nulo control is hidden/disabled for generated columns. -->

When Virtual (generated column) is true, the system MUST emit MySQL/MariaDB generated-column
DDL of the form `<type>[(length[,decimals])] GENERATED ALWAYS AS (<expresión>)
[VIRTUAL|STORED]`. The system MUST NOT emit a user-supplied default value (generated columns
cannot have DEFAULT). Length/Decimals MUST still apply to the base type where the type is
length- or decimal-bearing (e.g. `VARCHAR(50) GENERATED ALWAYS AS (...)`). Nulo MUST NOT be
user-settable for generated columns: the Nulo control MUST be hidden/disabled while Virtual =
true, because MySQL derives a generated column's nullability from its expression rather than
an explicit NULL/NOT NULL clause. Comentario MUST remain allowed (MySQL supports COMMENT on
generated columns). Non-generated semantics apply when Virtual is false.

#### Scenario: Virtual emits generated-column DDL

- GIVEN a field with Virtual = true, a valid Tipo, and a non-blank Expresión
- WHEN the table DDL is built
- THEN the column MUST render as `<type> GENERATED ALWAYS AS (<expresión>) VIRTUAL`
- AND the base-type length/decimals MUST be included when the type is length/decimal-bearing

#### Scenario: Virtual omits default handling

- GIVEN the field-definition dialog with Virtual = false
- WHEN the user sets Virtual = true
- THEN any default-value handling MUST be omitted from the resulting field definition
- AND the field MUST be marked as a generated/virtual column

#### Scenario: Nulo hidden for generated columns

- GIVEN the field-definition dialog with Virtual = false and the Nulo control visible
- WHEN the user sets Virtual = true
- THEN the Nulo control MUST be hidden/disabled AND MUST NOT emit a NULL/NOT NULL clause in the DDL

#### Scenario: Comentario allowed on generated column

- GIVEN Virtual = true with a non-blank Comentario
- WHEN the table DDL is built
- THEN the generated-column definition MUST include the `COMMENT '<comentario>'` clause

### Requirement: Generated Key Column Forces STORED

<!-- ADDED in change `create-table` (generated-column support). Interacts with the
existing "Key Forces Not Null" requirement. -->

Generated columns MUST default to the `VIRTUAL` storage form. When Virtual = true AND Llave
(Key) = true, the system MUST instead emit the `STORED` storage form, because MySQL requires
a generated column that participates in a `PRIMARY KEY` to be `STORED`. This rule MUST be
applied automatically based on cross-field state and MUST NOT require a separate user input.

#### Scenario: Virtual non-key column uses VIRTUAL

- GIVEN a field with Virtual = true and Llave = false
- WHEN the column DDL is built
- THEN the storage form MUST be `VIRTUAL`

#### Scenario: Virtual key column forced to STORED

- GIVEN a field with Virtual = true and Llave = true
- WHEN the column DDL is built
- THEN the storage form MUST be `STORED`
- AND the column MUST be eligible for inclusion in the table PRIMARY KEY

#### Scenario: Toggling Key updates storage form

- GIVEN a field with Virtual = true and Llave = true rendering as `STORED`
- WHEN the user sets Llave = false
- THEN the storage form MUST revert to `VIRTUAL`

### Requirement: Field Dialog OK Appends Without SQL

The field-definition dialog OK button MUST validate the form; on success it MUST append a
new field entry to the parent sheet's list, close the dialog, and reveal the parent sheet
with the new field visible. It MUST NOT execute any SQL.

#### Scenario: Valid field is appended

- GIVEN a valid field (valid Nombre, selected Tipo, consistent cross-field state)
- WHEN the user taps OK in the dialog
- THEN the field MUST be appended to the parent sheet's list
- AND the dialog MUST close and the parent sheet MUST show the new field
- AND NO SQL MUST be executed

### Requirement: Field Dialog Cancel Discards

Cancelling or dismissing the field-definition dialog MUST discard all in-progress field
input and leave the parent sheet's list unchanged.

#### Scenario: Cancel discards in-progress field

- GIVEN the dialog has partially entered field data
- WHEN the user taps Cancel or dismisses the dialog
- THEN the dialog MUST close with no field appended and the parent list unchanged

### Requirement: Parent OK Enablement

The parent sheet's OK button MUST be enabled only when the table-name is non-blank AND at
least one field exists in the list; otherwise it MUST be disabled.

#### Scenario: OK disabled without name or fields

- GIVEN the table-name is blank OR the fields list is empty
- THEN the parent OK button MUST be disabled

#### Scenario: OK enabled with name and one field

- GIVEN the table-name is non-blank AND at least one field exists
- THEN the parent OK button MUST be enabled

### Requirement: Parent OK Executes DDL

The parent sheet's OK button MUST execute `CREATE TABLE` DDL via `CreateTableUseCase`
against the active connection. On success the sheet MUST close and `TablesListScreen` MUST
refresh to show the new table. On failure the DDL error MUST be surfaced as a localized
message, the sheet MUST stay open, and entered fields MUST NOT be lost.

#### Scenario: Successful creation

- GIVEN a valid table name and at least one valid field
- WHEN the user taps parent OK and the DDL succeeds
- THEN the table MUST be created, the sheet MUST close, and `TablesListScreen` MUST
  refresh and display the new table

#### Scenario: DDL failure keeps data

- GIVEN a valid table name and fields
- WHEN the user taps OK and the DDL execution fails (error or connection issue)
- THEN a localized error message MUST be surfaced
- AND the sheet MUST stay open with the entered name and fields intact

### Requirement: Parent Cancel Dismisses

Cancelling or dismissing the parent "Crear tabla" sheet MUST close it and discard the
in-progress table name and fields list; no SQL MUST be executed.

#### Scenario: Cancel discards the draft

- GIVEN the parent sheet has an entered name and fields
- WHEN the user taps Cancel or dismisses the sheet
- THEN the sheet MUST close, all draft data MUST be discarded, and no SQL executed

### Requirement: Localized Strings In All Locales

All new user-facing strings MUST have entries in all 10 supported locales (en, es, fr, de,
pt-rBR, ru, zh-rCN, ja, hi, ar). No user-facing text MUST be hardcoded. This is BLOCKING —
the change MUST NOT ship with any locale missing a new key.

#### Scenario: Every new key exists in all 10 locales

- GIVEN new `create_table_*` and `field_def_*` string keys are introduced (including the
  Expresión label, its required-validation error, and any generated-column helper text)
- WHEN the strings are audited before commit
- THEN each new key MUST exist in all 10 `strings.xml` locale files
- AND no `Text(...)` call in the feature MUST use a hardcoded literal
