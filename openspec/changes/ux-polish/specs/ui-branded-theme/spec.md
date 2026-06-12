# Especificación: ui-branded-theme

## Propósito

Definir una paleta cromática branded **dark-mode-first** que dé al app identidad propia (futurista, premium, alineada con Navicat + PlayStation + Arc + Linear), respetando la convivencia con Material You (dynamic color en API 31+) y permitiendo al usuario elegir cuál aplicar.

## Requirements

### Requirement: Tokens de color branded

El sistema MUST definir los siguientes tokens cromáticos en `Color.kt`:

| Token              | Valor      | Rol Material 3            | Razón                                |
|--------------------|------------|---------------------------|--------------------------------------|
| `brand_bg`         | `#1A1F2E`  | `background` (dark)       | Azul-grafito profundo, no negro puro |
| `brand_surface`    | `#222837`  | `surface` (dark)          | Surface elevada sobre background     |
| `brand_surface_variant` | `#2E3447` | `surfaceVariant` (dark) | Cards y containers internos          |
| `brand_outline`    | `#5B5F7D`  | `outline` / `onSurfaceVariant` | Bordes, divisores, texto secundario |
| `brand_primary`    | `#7C80E8`  | `primary`                 | Azul-índigo eléctrico (PlayStation feel) |
| `brand_on_primary` | `#0F1119`  | `onPrimary`               | Contraste WCAG AA sobre primary      |
| `brand_on_bg`      | `#E6E8F0`  | `onBackground` / `onSurface` | Texto principal sobre dark         |
| `brand_secondary`  | `#8EE3D3`  | `tertiary`                | Acento mint para success / highlights |
| `brand_on_secondary`| `#0F1119` | `onTertiary`              | Contraste sobre secondary            |
| `brand_error`      | `#FF6B7A`  | `error`                   | Rojo cálido, no Material default     |

**Justificación del mapeo**:
- `#5B5F7D` se mapea a `outline` (no a `surfaceVariant`) porque su luminancia (~36%) es apropiada para bordes y texto secundario, no para fondos.
- `#8EE3D3` se mapea a `tertiary` (no a `secondary`) porque Material 3 reserva `secondary` para acciones complementarias del primary; `tertiary` es el rol natural para un acento de soporte.
- `#E6E8F0` se mapea simultáneamente a `onBackground` y `onSurface` porque ambos rolees suelen compartir el mismo color de texto en dark mode.

### Requirement: Esquema branded dark (canónico)

El sistema MUST exponer `BrandedDarkColorScheme: ColorScheme` construido con los tokens anteriores, completado con valores derivados consistentes para los roles Material 3 no listados explícitamente (`primaryContainer`, `onPrimaryContainer`, `inverseSurface`, etc.).

Los roles derivados MUST cumplir contraste mínimo **WCAG AA (4.5:1)** entre cualquier par `xxx` / `onXxx`.

#### Scenario: Contraste primary cumple WCAG AA

- GIVEN `BrandedDarkColorScheme.primary` (`#7C80E8`) y `onPrimary` (`#0F1119`)
- WHEN se calcula el ratio de contraste
- THEN el ratio es ≥ 4.5:1

### Requirement: Esquema branded light (derivado)

El sistema MUST exponer `BrandedLightColorScheme: ColorScheme` derivado del esquema dark mediante inversión de luminancia para roles de superficie y texto, manteniendo `primary` (`#7C80E8`) prácticamente igual (solo ajuste leve de saturación si fuese necesario para contraste).

| Token light            | Valor aprox. | Derivación                       |
|------------------------|--------------|----------------------------------|
| `background` (light)   | `#F5F6FA`    | Invertir luminancia de `brand_bg` |
| `surface` (light)      | `#FFFFFF`    | Surface puro                     |
| `surfaceVariant` (light)| `#E6E8F0`   | Reuso de `brand_on_bg`           |
| `onBackground` (light) | `#1A1F2E`    | Reuso de `brand_bg`              |
| `outline` (light)      | `#8B90AE`    | `brand_outline` aclarado ~25%    |

#### Scenario: Tema light se ve coherente

- GIVEN el usuario activa light mode
- WHEN se aplica `BrandedLightColorScheme`
- THEN los textos son legibles (contraste ≥ 4.5:1)
- AND la identidad branded (primary `#7C80E8`) sigue siendo reconocible

### Requirement: Lógica de selección de tema

El sistema MUST aplicar el siguiente algoritmo de selección en `AppTheme`:

```
si userPrefersBranded == true:
    usar BrandedDark / BrandedLight según darkMode
sino si supportsDynamic (API 31+) && dynamicColorEnabled:
    usar dynamicDarkColorScheme(context) / dynamicLightColorScheme(context)
sino:
    usar BrandedDark / BrandedLight según darkMode (fallback branded, NO Material default)
```

La preferencia `userPrefersBranded: Boolean` MUST persistirse en `DataStore` y exponerse como `Flow<Boolean>`. Default: `false` (el primer arranque prefiere dynamic si está disponible).

#### Scenario: Usuario activa branded sobre dynamic

- GIVEN dispositivo API 33 con dynamic color activo
- AND `userPrefersBranded == true`
- WHEN se aplica `AppTheme`
- THEN se usa `BrandedDarkColorScheme`
- AND NO se usa `dynamicDarkColorScheme()`

#### Scenario: Fallback en dispositivo viejo

- GIVEN dispositivo API 29 (sin soporte dynamic)
- AND `userPrefersBranded == false`
- WHEN se aplica `AppTheme`
- THEN se usa `BrandedDarkColorScheme` (no Material default)

### Requirement: Edge-to-edge correcto

El sistema MUST eliminar `window.statusBarColor = colorScheme.primary.toArgb()` del `Theme`.

El sistema MUST aplicar:

```kotlin
WindowCompat.setDecorFitsSystemWindows(window, false)
WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkMode
```

`MainActivity` ya invoca `enableEdgeToEdge()`; el theme NO debe sobreescribir esto.

Los Composables raíz MUST consumir `WindowInsets` (`statusBarsPadding()`, `navigationBarsPadding()`) donde corresponda para evitar clipping del contenido.

#### Scenario: Status bar transparente en dark

- GIVEN dark mode activo
- WHEN la app inicia
- THEN status bar es transparente
- AND íconos del sistema (hora, batería) son blancos
- AND el contenido NO se solapa con la status bar

#### Scenario: Status bar transparente en light

- GIVEN light mode activo
- WHEN la app inicia
- THEN status bar es transparente
- AND íconos del sistema son oscuros

## Non-Functional Requirements

- **Performance**: Cambio de tema (branded ↔ dynamic) MUST completarse en < 16 ms (un frame a 60 fps).
- **Accessibility**: Todo par `xxx`/`onXxx` MUST cumplir WCAG AA (4.5:1) en branded.
- **Persistence**: La preferencia `userPrefersBranded` MUST sobrevivir reinicios y reinstalaciones desde backup.

## Edge Cases

#### Scenario: Dynamic color OFF en sistema

- GIVEN sistema sin dynamic color habilitado por el usuario
- AND `userPrefersBranded == false`
- WHEN se aplica `AppTheme`
- THEN se usa `BrandedDarkColorScheme` (fallback)
- AND NO se usa Material default

#### Scenario: Usuario alterna branded mientras app corre

- GIVEN app abierta con branded `OFF`
- WHEN el usuario activa "Branded palette" en Settings
- THEN el `Flow<Boolean>` emite `true`
- AND `AppTheme` re-renderiza con `BrandedDarkColorScheme`
- AND el cambio ocurre en < 16 ms

#### Scenario: Sistema cambia dynamic color mientras app corre

- GIVEN app abierta con `userPrefersBranded == false` y dynamic activo
- WHEN el usuario cambia el wallpaper (API 31+) y vuelve a la app
- THEN el `ColorScheme` re-calcula con los nuevos colores dynamic
- AND el cambio es visible sin reiniciar la app

## Constraints

- Los strings de la pantalla de Settings (toggle "Branded palette") MUST estar en `strings.xml` (es + en).
- No se usan `@SuppressLint`; el código MUST ser compatible con API 29 nativamente.
- El sistema MUST NOT depender de Compose Material (Material 2); solo Material 3.
