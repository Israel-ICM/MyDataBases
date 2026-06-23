# result-grid Specification

## Purpose

Reusable Compose component that displays tabular query results (column headers + data rows) with horizontal scrolling. Extracted from the inlined private `RowsTab` in `TableViewerScreen` so it can be reused by `TableViewer`, `QueryEditor`, and any future result-rendering surface.

## Requirements

### Requirement: Public Reusable API

The system MUST expose a single reusable Composable that accepts a list of column names, a list of row data, and a `Modifier`, and renders the result grid. It MUST NOT be marked `private` or `internal` to a single screen.

#### Scenario: Reused across screens

- GIVEN the shared result-grid Composable is defined
- WHEN `TableViewerScreen` and `QueryEditorScreen` both reference it
- THEN both screens compile against the same Composable
- AND no copy-pasted grid renderer remains in either screen

### Requirement: Column Headers

The result grid MUST render a header row above the data rows. Header cells MUST be visually distinct from data cells (for example via weight, divider, or background).

#### Scenario: Headers visible

- GIVEN columns `id`, `name`, `email`
- WHEN the grid renders
- THEN a header row shows `id`, `name`, `email` in order
- AND the header row is visually distinguished from data rows

### Requirement: Data Rows

The result grid MUST render each row's cells in the same column order as the headers. Empty result sets MUST render the header row alone with no data rows and MUST NOT crash.

#### Scenario: Rows align with headers

- GIVEN columns `id`, `name` and rows `[(1, "Ada"), (2, "Linus")]`
- WHEN the grid renders
- THEN row 1 shows `1` under `id` and `Ada` under `name`
- AND row 2 shows `2` under `id` and `Linus` under `name`

#### Scenario: Empty result

- GIVEN columns `id`, `name` and zero rows
- WHEN the grid renders
- THEN the header row is shown
- AND no data rows are shown
- AND no crash or error occurs

### Requirement: Horizontal Scroll

When the total column width exceeds the available width, both the header row and the data rows MUST scroll horizontally in sync.

#### Scenario: Wide result scrolls

- GIVEN a result with 20 columns and a narrow screen
- WHEN the user scrolls horizontally
- THEN the header row and data rows scroll together so columns stay aligned

### Requirement: NULL Display

A cell whose value is SQL `NULL` MUST render a visually distinct `NULL` indicator (not an empty string and not the literal text `null`).

#### Scenario: Null cell

- GIVEN a row contains a `NULL` value in the `email` column
- WHEN the grid renders that row
- THEN the cell shows a distinct `NULL` marker (such as italic "NULL" or muted styling)
- AND the cell is not blank

### Requirement: Refactor From TableViewer

The existing private `RowsTab` implementation in `TableViewerScreen` MUST be replaced by the shared component. `TableViewerScreen` MUST continue to render rows with the same visual behaviour after the extraction.

#### Scenario: TableViewer parity

- GIVEN `TableViewerScreen` previously rendered rows via the inlined `RowsTab`
- WHEN the refactor is applied and the shared component is used instead
- THEN `TableViewerScreen` still renders columns, rows, horizontal scroll, and NULL display with parity behaviour
- AND no inlined private grid renderer remains in `TableViewerScreen`

## Non-Functional

- **Testability**: The shared component MUST be exercised by at least one Compose UI test covering header + rows + horizontal scroll + NULL display.
- **Performance**: The component MUST render large results lazily (column-wise horizontal scroll, row-wise lazy column) so that result sets up to the row-cap defined by `query-runner` do not block the UI thread.
- **Accessibility**: Cells SHOULD expose row and column position via content description.
