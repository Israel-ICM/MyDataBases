# Especificación: ui-reduced-motion

## Propósito

Detectar la preferencia del sistema de "animaciones reducidas" (a11y) y propagarla globalmente vía `CompositionLocal` para que todos los componentes y el token `AppMotion` respeten esa preferencia automáticamente.

## Requirements

### Requirement: Detección desde Settings.Global

El sistema MUST detectar la preferencia leyendo `Settings.Global.ANIMATOR_DURATION_SCALE` mediante `Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)`.

Reglas de interpretación:

- `0f` → reduced motion ACTIVO (usuario desactivó animaciones).
- `> 0f` → reduced motion INACTIVO (animaciones normales o aceleradas).

Cualquier excepción al leer la setting MUST asumir `false` (animaciones normales) — fail safe hacia la mejor experiencia visual.

#### Scenario: Setting en 0

- GIVEN `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`
- WHEN el sistema lee la preferencia
- THEN devuelve `true` (reduced motion activo)

#### Scenario: Setting en 1

- GIVEN `Settings.Global.ANIMATOR_DURATION_SCALE == 1f`
- WHEN el sistema lee la preferencia
- THEN devuelve `false` (animaciones normales)

#### Scenario: Excepción al leer

- GIVEN un `SecurityException` al consultar `Settings.Global`
- WHEN el sistema intenta leer
- THEN captura la excepción y devuelve `false`
- AND la app NO crashea

### Requirement: CompositionLocal LocalReducedMotion

El sistema MUST exponer:

```kotlin
val LocalReducedMotion: ProvidableCompositionLocal<Boolean> =
    compositionLocalOf { false }
```

`AppTheme` MUST proveer este `CompositionLocal` calculando el valor inicial desde `Settings.Global` y manteniéndolo actualizado durante el ciclo de vida del Composable.

#### Scenario: Composable consume reduced motion

- GIVEN un Composable dentro de `AppTheme`
- WHEN solicita `LocalReducedMotion.current`
- THEN obtiene el valor actual (`true` o `false`) sincronizado con el sistema

### Requirement: Observación de cambios en runtime

El sistema MUST observar cambios de `Settings.Global.ANIMATOR_DURATION_SCALE` mientras la app está en foreground.

Implementación recomendada: `ContentObserver` registrado en `onStart` y desregistrado en `onStop` del lifecycle owner (`Activity` o `Composable` que provee el `CompositionLocal`).

Cuando el valor cambia, el `State<Boolean>` interno MUST actualizarse y disparar recomposición.

#### Scenario: Usuario activa reduced motion mientras la app corre

- GIVEN app abierta con `LocalReducedMotion.current == false`
- WHEN el usuario va a Settings del sistema → Developer options → "Animator duration scale" → "Animation off"
- AND vuelve a la app
- THEN `LocalReducedMotion.current` pasa a `true`
- AND las animaciones en curso o futuras se vuelven instantáneas
- AND los `SkeletonLoader` no inician su (futura) animación shimmer

#### Scenario: Usuario desactiva reduced motion mientras la app corre

- GIVEN app abierta con `LocalReducedMotion.current == true`
- WHEN el usuario restaura "Animator duration scale" a 1x
- AND vuelve a la app
- THEN `LocalReducedMotion.current` pasa a `false`
- AND animaciones futuras vuelven a sus durations normales

### Requirement: Integración con AppMotion

`AppMotion.durationOrInstant(base: Int, reduced: Boolean): Int` (definido en `ui-design-tokens`) MUST ser la única fuente de verdad para calcular durations efectivas.

Los Composables que animen MUST consumir el valor así:

```kotlin
val reduced = LocalReducedMotion.current
val duration = AppMotion.durationOrInstant(AppMotion.medium, reduced)
val animatedValue by animateFloatAsState(
    targetValue = if (visible) 1f else 0f,
    animationSpec = tween(durationMillis = duration, easing = AppMotion.standard)
)
```

Cuando `reduced == true`, `duration == 0` y `tween(0)` aplica el cambio en un frame (sin interpolación).

#### Scenario: AnimatedVisibility respeta reduced motion

- GIVEN `LocalReducedMotion.current == true`
- AND una `AnimatedVisibility` que aparece con `fadeIn(tween(AppMotion.medium))`
- WHEN se calcula la duration efectiva con `durationOrInstant`
- THEN devuelve `0` ms
- AND el contenido aparece sin fade (instantáneo)

### Requirement: Provisión en MainActivity

`MainActivity` MUST envolver el contenido con `CompositionLocalProvider(LocalReducedMotion provides isReducedMotion)` antes de `AppTheme`, donde `isReducedMotion` es un `State<Boolean>` calculado por la función de detección + observer.

Alternativamente, `AppTheme` mismo puede proveer el `CompositionLocal` internamente, simplificando el `MainActivity`. **Decisión normativa**: el provisor es responsabilidad de `AppTheme`, para mantener `MainActivity` mínimo.

#### Scenario: AppTheme provee el CompositionLocal

- GIVEN `setContent { AppTheme { Pantalla() } }`
- WHEN `Pantalla` consume `LocalReducedMotion.current`
- THEN obtiene el valor actual sin que `MainActivity` haya tenido que proveerlo manualmente

## Non-Functional Requirements

- **Performance**: La lectura inicial de `Settings.Global` MUST ocurrir en `< 5 ms`. No bloquea el primer frame.
- **Battery**: El `ContentObserver` MUST desregistrarse en `onStop` para no mantener referencias ni callbacks innecesarios.
- **Compatibility**: API 29+ (`Settings.Global.ANIMATOR_DURATION_SCALE` está disponible desde API 17).

## Edge Cases

#### Scenario: App en background no recibe updates

- GIVEN app en background con `LocalReducedMotion.current == false`
- WHEN el usuario cambia el setting del sistema
- AND vuelve a la app
- THEN al re-suscribir el `ContentObserver` en `onStart`, el valor se re-lee
- AND `LocalReducedMotion.current` refleja el valor actual

#### Scenario: Permisos restringidos

- GIVEN un dispositivo con políticas que restringen `Settings.Global` lectura para apps no privilegiadas
- WHEN la app intenta leer
- THEN captura la excepción
- AND `LocalReducedMotion.current` queda en `false` (animaciones normales)
- AND la app NO crashea

#### Scenario: Composable fuera de AppTheme

- GIVEN un Composable consumido en un `@Preview` sin `AppTheme`
- WHEN solicita `LocalReducedMotion.current`
- THEN obtiene el default del `compositionLocalOf` (`false`)
- AND no crashea

## Constraints

- El sistema MUST NO requerir permisos especiales (`Settings.Global.ANIMATOR_DURATION_SCALE` es lectura libre).
- El sistema MUST NO usar `Settings.System` ni `Settings.Secure` para esta detección.
- El sistema MUST NO exponer una preferencia "override" en Settings de la app — la preferencia es del SISTEMA, no de la app. (Si en el futuro se requiere override propio, será un cambio separado.)
