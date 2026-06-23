# workspace-cards Specification

## Purpose

Multi-instance workspace where users open and manage independent "cards" stacked at the top of the screen (the existing PlayStation-style top sheet). Each card is a self-contained editor or viewer with its own state. This spec adds a new `WorkspaceCard.Query` type to host SQL editor instances alongside existing card types.

## Requirements

### Requirement: Card Type Taxonomy

The system MUST model workspace cards as a sealed type hierarchy. The hierarchy MUST include at least the following variants:

| Variant | Purpose |
|---|---|
| `Table` | Shows a table viewer for a chosen `(database, table)` |
| `Query` | Hosts a SQL editor instance (this change) |
| `View` | Reserved for future view-of-data cards |

A new variant `WorkspaceCard.Query` MUST be added in this change.

#### Scenario: Query variant exists

- GIVEN the workspace card sealed type is defined
- WHEN code references `WorkspaceCard.Query`
- THEN the type resolves and compiles
- AND it carries the data needed to identify a query editor instance (unique id, optional initial SQL, target connection id)

### Requirement: Independent Card Instances

Each card open in the workspace MUST own its own state. Opening, focusing, or closing one card MUST NOT mutate the state of any other card.

#### Scenario: Two query cards coexist

- GIVEN the user opens two `WorkspaceCard.Query` cards in sequence
- WHEN both cards exist in the workspace
- THEN each card has its own unique id
- AND each card stores its own SQL contents, caret position, and execution state

#### Scenario: Mixed cards coexist

- GIVEN the user opens one `Table` card and one `Query` card
- WHEN both cards exist in the workspace
- THEN both render in the workspace stack
- AND switching between them preserves each one's internal state

### Requirement: Opening a Query Card

The system MUST provide an entry point to open a new `WorkspaceCard.Query` for the active connection. Opening a query card MUST add it to the active card list and bring it to the focused/expanded state.

#### Scenario: Open new query card

- GIVEN the user is on a connection-scoped screen
- WHEN the user invokes "New query" from the workspace controls
- THEN a new `WorkspaceCard.Query` is appended to the active cards list
- AND the new card is shown in the expanded state with an empty editor

#### Scenario: Open additional query card

- GIVEN one `WorkspaceCard.Query` is already open
- WHEN the user invokes "New query" again
- THEN a second `WorkspaceCard.Query` is appended with its own unique id
- AND the existing first query card retains its contents and state

### Requirement: Card Identity

Every workspace card instance MUST carry a stable unique identifier for the lifetime of the workspace session. Identifiers MUST be used to address cards for focus, close, and state lookup.

#### Scenario: Stable id across re-renders

- GIVEN a `WorkspaceCard.Query` card with id `q1` is open
- WHEN the workspace is re-rendered (configuration change or recomposition)
- THEN the card still resolves by id `q1`
- AND its state is recovered

### Requirement: Closing a Card

The system MUST allow closing an individual card. Closing a card MUST remove it from the active list and MUST release its state.

#### Scenario: Close one of many

- GIVEN three cards `q1`, `q2`, `t1` are open
- WHEN the user closes `q2`
- THEN the active card list contains only `q1` and `t1`
- AND closing `q2` does not affect `q1` or `t1`

## Non-Functional

- **State isolation**: Card state MUST be keyed by card id at the workspace manager level so no card can read or write another card's state.
- **Testability**: The workspace manager MUST be unit-testable without Compose. Adding, focusing, and closing cards MUST be exercised by tests.
