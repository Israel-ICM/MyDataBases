# theme-mode Specification

## Purpose

Defines user-selectable application appearance (System / Light / Dark) as an
independent axis layered on top of the existing `branded_palette` boolean. The
selection is persisted across launches and applied app-wide through the canonical
`AppTheme` entry point, including `DesignTokens`-driven surfaces and custom-drawn
components.

Cross-cutting rule (applies to every requirement below): `theme_mode`
(SYSTEM/LIGHT/DARK) and `branded_palette` (existing boolean) are INDEPENDENT axes.
`theme_mode` selects the base Material 3 `ColorScheme` (light vs dark);
`branded_palette` layers the branded WCAG AA tint on top in BOTH light and dark.
Neither axis disables or mutually excludes the other.

## ADDED Requirements

### Requirement: Theme Mode Selector in Settings

The Settings screen SHALL present a control letting the user choose one of three
theme modes: System, Light, Dark. The control SHALL reflect the currently
persisted `theme_mode` on entry.

#### Scenario: User selects Light

- GIVEN the Settings screen is open with persisted mode SYSTEM
- WHEN the user selects "Light"
- THEN the app renders the light `ColorScheme` immediately
- AND the selector shows "Light" as active

#### Scenario: User selects Dark

- GIVEN the Settings screen is open
- WHEN the user selects "Dark"
- THEN the app renders the dark `ColorScheme` immediately
- AND the selector shows "Dark" as active

#### Scenario: User selects System

- GIVEN the current mode is Dark
- WHEN the user selects "System"
- THEN the app resolves appearance from the OS setting
- AND follows subsequent OS light/dark changes while mode remains System

#### Scenario: Selection survives restart

- GIVEN the user has selected "Dark"
- WHEN the app is fully closed and relaunched
- THEN the app starts in dark appearance without user interaction

### Requirement: Theme Mode Persistence

The system SHALL persist the chosen theme mode under a new DataStore key
`theme_mode`, stored as the `ThemeMode` enum name. The default value, when no
preference has been written, SHALL be `SYSTEM`.

#### Scenario: Default on first launch

- GIVEN a fresh install with no `theme_mode` value written
- WHEN the app reads the theme mode
- THEN it returns SYSTEM

#### Scenario: Write then observe

- GIVEN the user sets mode to DARK
- WHEN the persisted preference stream emits
- THEN observers receive DARK
- AND the value read after process death is DARK

### Requirement: Effective ColorScheme Resolution

`AppTheme` SHALL compute the effective `ColorScheme` from BOTH axes:
`theme_mode` (resolved to light or dark, with SYSTEM deferring to the OS) selects
the base scheme, and `branded_palette` selects branded vs non-branded. No
`TEMPORAL` hack SHALL force branded colors.

Rule table (base × branded → scheme):

| Effective base | branded_palette | Resulting scheme |
|----------------|-----------------|------------------|
| Light | false | Non-branded light |
| Light | true | `BrandedLightColorScheme` |
| Dark | false | Non-branded dark |
| Dark | true | `BrandedDarkColorScheme` |

(SYSTEM resolves to Light or Dark via the OS, then applies the same table.)

#### Scenario: Dark + branded

- GIVEN theme_mode DARK and branded_palette true
- WHEN AppTheme resolves the scheme
- THEN it uses `BrandedDarkColorScheme`

#### Scenario: Light + branded

- GIVEN theme_mode LIGHT and branded_palette true
- WHEN AppTheme resolves the scheme
- THEN it uses `BrandedLightColorScheme`

#### Scenario: Dark + non-branded

- GIVEN theme_mode DARK and branded_palette false
- WHEN AppTheme resolves the scheme
- THEN it uses the non-branded dark scheme

#### Scenario: System defers to OS then applies branded axis

- GIVEN theme_mode SYSTEM, OS in dark, branded_palette true
- WHEN AppTheme resolves the scheme
- THEN it uses `BrandedDarkColorScheme`

#### Scenario: Axes are independent

- GIVEN any theme_mode value
- WHEN branded_palette is toggled
- THEN only the branded tint changes; the light/dark base is unchanged
- AND toggling theme_mode never changes the branded_palette value

### Requirement: Canonical Theme Entry Point Migration

`AppTheme` SHALL be the sole theme entry point. `MyDataBasesTheme` SHALL be
removed, and every call site — including `@Preview` composables — SHALL be
migrated to `AppTheme`.

#### Scenario: No remaining references

- GIVEN the change is complete
- WHEN the source tree is searched for `MyDataBasesTheme`
- THEN no references remain (definition, call sites, or previews)

#### Scenario: Previews render via AppTheme

- GIVEN a `@Preview` that previously wrapped content in `MyDataBasesTheme`
- WHEN the preview is rendered
- THEN it is wrapped in `AppTheme` and renders without error

### Requirement: Theme-Aware Design Tokens

`DesignTokens` SHALL resolve per active theme rather than exposing static
light-only values. When a screen reads a design token color, the returned value
SHALL correspond to the currently active theme (light or dark).

#### Scenario: Token reads dark value

- GIVEN the dark theme is active
- WHEN a screen reads a `DesignTokens` color
- THEN the returned color is the dark-mode value

#### Scenario: Token reads light value

- GIVEN the light theme is active
- WHEN a screen reads the same token
- THEN the returned color is the light-mode value

#### Scenario: Token tracks live theme change

- GIVEN a screen is displaying token-driven colors
- WHEN the effective theme switches from light to dark
- THEN the screen recomposes with the dark token values

### Requirement: Dark-Safe Custom-Drawn Effects

Custom-drawn shadows, scrims, and gradients SHALL remain visible with
appropriate contrast in dark mode. `WorkspaceCarousel` (currently a pure-black
`BlurMaskFilter` shadow) and other custom-draw components identified in
exploration (IOS component shadows/scrims, backdrop scrim, background gradients,
card shadow color) SHALL NOT render as invisible or muddy on dark surfaces.

#### Scenario: Carousel shadow visible on dark

- GIVEN the dark theme is active
- WHEN `WorkspaceCarousel` draws its shadow
- THEN the shadow is perceptible against the dark surface (not pure black on near-black)

#### Scenario: Scrims and gradients adapt

- GIVEN the dark theme is active
- WHEN a scrim or background gradient is drawn
- THEN its color/alpha is tuned so contrast remains appropriate for dark

#### Scenario: Light unaffected

- GIVEN the light theme is active
- WHEN the same custom-draw components render
- THEN their appearance is unchanged from prior behavior

### Requirement: Localized Theme Mode Strings

All user-facing strings for the theme mode selector (its label and the
System/Light/Dark option labels) SHALL exist in string resources and be present
in all 10 supported locale `strings.xml` files. No hardcoded UI text SHALL be
introduced.

Supported locales: `values`, `values-es`, `values-ar`, `values-de`,
`values-fr`, `values-hi`, `values-ja`, `values-pt-rBR`, `values-ru`,
`values-zh-rCN`.

#### Scenario: Strings present in every locale

- GIVEN the theme mode selector strings are added
- WHEN each of the 10 `strings.xml` files is inspected
- THEN every new string key is present in each locale

#### Scenario: No hardcoded selector text

- GIVEN the Settings theme selector UI
- WHEN its composables are inspected
- THEN all displayed text comes from `stringResource`, none inline
