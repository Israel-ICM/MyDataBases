# Especificación: ui-design-tokens

## Propósito

Centralizar todos los valores visuales del sistema de diseño (espaciado, formas, elevación, motion) en tokens accesibles desde cualquier Composable mediante `CompositionLocal`, eliminando literales `*.dp` dispersos y garantizando coherencia visual.

## Requirements

### Requirement: Token de Spacing

El sistema MUST exponer una escala de espaciado en `AppSpacing` basada en múltiplos de **4 dp**.

La escala MUST contener los siguientes valores nombrados (todos `Dp`):

| Token        | Valor  | Uso típico                          |
|--------------|--------|-------------------------------------|
| `none`       | 0 dp   | Cero explícito                      |
| `xxs`        | 2 dp   | Separación entre íconos y texto inline |
| `xs`         | 4 dp   | Padding interno mínimo              |
| `sm`         | 8 dp   | Padding entre elementos de lista    |
| `md`         | 12 dp  | Padding interno de cards            |
| `lg`         | 16 dp  | Padding estándar de pantalla        |
| `xl`         | 24 dp  | Padding adaptativo Medium           |
| `xxl`        | 32 dp  | Padding adaptativo Expanded         |
| `xxxl`       | 48 dp  | Separación entre secciones grandes  |

El sistema MUST exponer `AppSpacing` mediante `LocalAppSpacing: ProvidableCompositionLocal<AppSpacing>` consumible como `MaterialTheme.appSpacing` o `LocalAppSpacing.current`.

#### Scenario: Consumo de spacing desde un Composable

- GIVEN un Composable dentro de `AppTheme`
- WHEN solicita `MaterialTheme.appSpacing.lg`
- THEN obtiene `16.dp` sin ambigüedad
- AND no necesita hardcodear el literal `16.dp`

#### Scenario: Consumo fuera de AppTheme

- GIVEN un Composable que no está envuelto en `AppTheme` (ej.: `@Preview` mal configurado)
- WHEN solicita `LocalAppSpacing.current.lg`
- THEN obtiene el valor por defecto `16.dp` (no lanza excepción)

### Requirement: Token de Shapes

El sistema MUST exponer `AppShapes` con cuatro niveles de corner radius:

| Token       | Valor   | Uso                                  |
|-------------|---------|--------------------------------------|
| `none`      | 0 dp    | Bordes rectos (status bar, divisores) |
| `small`     | 8 dp    | Chips, botones pequeños              |
| `medium`    | 12 dp   | Cards estándar, text fields          |
| `large`     | 20 dp   | Bottom sheets, dialogs               |
| `extraLarge`| 28 dp   | Containers destacados, hero cards    |

Cada token MUST ser un `RoundedCornerShape`. El sistema MUST sobreescribir `MaterialTheme.shapes` con estos valores para que componentes Material 3 los hereden automáticamente.

#### Scenario: Card respeta shape token

- GIVEN una `Card` dentro de `AppTheme`
- WHEN no se pasa parámetro `shape`
- THEN renderiza con `AppShapes.medium` (12 dp radius)

### Requirement: Token de Elevation

El sistema MUST exponer `AppElevation` con cinco niveles de elevación (`Dp`):

| Token         | Valor  | Uso                                    |
|---------------|--------|----------------------------------------|
| `none`        | 0 dp   | Surfaces planas (background)           |
| `cardResting` | 1 dp   | Card en estado normal                  |
| `cardHover`   | 3 dp   | Card en hover (Medium/Expanded)        |
| `cardPressed` | 6 dp   | Card en pressed                        |
| `modal`       | 8 dp   | Bottom sheets, dialogs                 |

#### Scenario: Card resting usa elevación baja

- GIVEN una `ConnectionCard` en estado normal
- WHEN se renderiza
- THEN aplica `Modifier.shadow(AppElevation.cardResting)` (1 dp)
- AND no satura visualmente la lista

### Requirement: Token de Motion

El sistema MUST exponer `AppMotion` con durations en `Int` (ms) y easings en `Easing`:

**Durations**:
- `instant`: 0 ms
- `fast`: 150 ms
- `medium`: 300 ms
- `slow`: 500 ms

**Easings**:
- `standard`: `FastOutSlowInEasing`
- `decelerate`: `LinearOutSlowInEasing`
- `accelerate`: `FastOutLinearInEasing`
- `emphasized`: `CubicBezierEasing(0.2f, 0f, 0f, 1f)` (Material 3 emphasized)

`AppMotion` MUST exponer `durationOrInstant(base: Int, reduced: Boolean): Int` que devuelve `0` cuando `reduced == true` y `base` en caso contrario. Esto integra la capability `ui-reduced-motion` sin que cada Composable repita la lógica.

#### Scenario: Duration respeta reduced motion

- GIVEN `LocalReducedMotion.current == true`
- WHEN un Composable solicita `AppMotion.durationOrInstant(AppMotion.medium, reduced = true)`
- THEN obtiene `0`
- AND la animación es instantánea

### Requirement: Wrapper AppTheme

El sistema MUST exponer `AppTheme(content)` que envuelve `MaterialTheme` y provee los cuatro `CompositionLocal` (`LocalAppSpacing`, `LocalAppShapes`, `LocalAppElevation`, `LocalAppMotion`).

`AppTheme` MUST integrar la lógica de `ui-branded-theme` (selección dynamic vs branded) y de `ui-reduced-motion` (lectura del setting del sistema), pero esos comportamientos están especificados en sus respectivas capabilities.

#### Scenario: AppTheme provee todos los tokens

- GIVEN `setContent { AppTheme { Pantalla() } }`
- WHEN `Pantalla` consume `MaterialTheme.appSpacing`, `MaterialTheme.appShapes`, `MaterialTheme.appElevation`, `MaterialTheme.appMotion`
- THEN todos resuelven sin error
- AND ningún valor es `null` ni el default fallback

## Non-Functional Requirements

- **Performance**: La lectura de un token MUST ser O(1) (read directo de `CompositionLocal`).
- **Backward compatibility**: Los Composables que aún usen literales `*.dp` MUST seguir funcionando durante la migración; no se rompe API existente.
- **API 29+**: No se usan APIs posteriores a API 29.

## Edge Cases

- **Composable sin `AppTheme` envolvente**: usa defaults razonables, no crashea.
- **Cambio de tema en runtime**: los tokens se re-componen automáticamente vía `CompositionLocal`.
- **Preview de Compose**: `AppTheme` MUST funcionar dentro de `@Preview` sin requerir Activity.

## Constraints

- Los tokens MUST NOT exponer valores mutables (`var`); solo `val`.
- `AppMotion` durations MUST ser `Int` (no `Duration`) para interop directa con `tween()` y `animateFloatAsState()`.
