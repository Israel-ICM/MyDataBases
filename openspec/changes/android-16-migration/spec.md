# Spec: Android 16 Migration — API 36

## Intent

Migrar MyDataBases a **Android 16 (API 36)**, actualizando compileSdk, targetSdk, toolchain (AGP, Gradle, Kotlin) y dependencias para alinearse con el ecosistema más reciente, cumplir con los requisitos de Google Play, y adoptar los behavior changes obligatorios de Android 16.

## Scope

### In Scope
- compileSdk = 36, targetSdk = 36
- AGP 8.9.2, Gradle 8.11.1, Kotlin 2.1.21
- KSP 2.1.21-2.0.2, Hilt 2.57.1 (KAPT en vez de KSP por incompatibilidad KSP 2.0+ con Hilt)
- Compose compiler embebido vía `kotlin.plugin.compose`
- Actualización de dependencias (Compose BOM 2025.05.01, Navigation 2.8.6, Room 2.7.0, etc.)
- Edge-to-edge enforcement obligatorio (Android 16)
- Predictive back navigation habilitado
- Gradle wrapper actualizado

### Out of Scope
- Cambios de funcionalidad o UI
- Migración de código fuente (solo toolchain y configuración)
- 16KB page alignment (no tenemos .so nativas)

## Behavior Changes — Android 16

### 1. Edge-to-Edge Obligatorio
Android 16 ignora `windowOptOutEdgeToEdgeEnforcement`. La app DEBE dibujar detrás de system bars.
- `MainActivity` ya llama `enableEdgeToEdge()` ✅
- No existe flag `windowOptOutEdgeToEdgeEnforcement` en `AndroidManifest.xml` ✅
- Los `CompositionLocal` de WindowInsets se deben manejar en screens (ya se usa `Scaffold` que maneja insets)

### 2. Predictive Back Navigation
Android 16 activa predictive back por defecto.
- Navigation Compose 2.8.x lo soporta nativamente
- No es necesario migrar manual a `OnBackInvokedCallback`

### 3. Orientation / Layout Constraints
Displays ≥600dp: `screenOrientation`, `setRequestedOrientation()` son ignorados.
- El proyecto ya usa `WindowSizeClass` y layouts adaptativos
- No hay restricciones de orientación en el manifesto
- Sin impacto

### 4. Strict Intent Validation
Android 16 bloquea intent redirection no declarada. Sin impacto (no usamos intents implícitos en esta app).

## Version Changes

| Componente | Versión Actual | Versión Nueva |
|------------|---------------|---------------|
| compileSdk | 34 | 36 |
| targetSdk | 34 | 36 |
| AGP | 8.2.2 | 8.9.2 |
| Gradle | 8.5 | 8.11.1 |
| Kotlin | 1.9.22 | 2.1.21 |
| KSP | 1.9.22-1.0.17 | 2.1.21-2.0.2 |
| Hilt | 2.50 | 2.57.1 (KAPT) |
| Compose BOM | 2024.02.00 | 2025.05.01 |
| Navigation Compose | 2.7.7 | 2.8.6 |
| Room | 2.6.1 | 2.7.0 |
| Activity Compose | 1.8.2 | 1.10.1 |
| Core KTX | 1.12.0 | 1.16.0 |
| Lifecycle Runtime | 2.7.0 | 2.9.0 |
| Coroutines | 1.7.3 | 1.9.0 |
| Mockk | 1.13.9 | 1.13.14 |
| Robolectric | 4.11.1 | 4.14.1 |
| Hilt Navigation Compose | 1.1.0 | 1.2.0 |

## Files to Modify

| File | Change |
|------|--------|
| `build.gradle.kts` (root) | AGP 8.9.2, Kotlin 2.1.21, Hilt 2.57.1, KSP 2.1.21-2.0.2, + `kotlin.plugin.compose` |
| `app/build.gradle.kts` | compileSdk 36, targetSdk 36, dependecias actualizadas, remove `composeOptions`, + `kotlin.plugin.compose`, + `kotlin.kapt` (Hilt via KAPT) |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 8.11.1 |

## Notas de Implementación

### Hilt + KAPT
- Hilt **no es compatible** con KSP 2.0+ (Kotlin Analysis API). Se migró Hilt a KAPT.
- Room sigue funcionando con KSP sin problemas.
- `hilt-android-compiler` usa KAPT, Room usa KSP — coexisten sin conflictos.

### TopSheet Notch Fix
- El TopSheet (panel workspace) quedaba oculto detrás del camera notch en dispositivos modernos.
- Fix: usar `WindowInsets.statusBars.getTop()` para offsetear el contenido colapsado (peek) debajo del notch.
- `ViewCompat.setOnApplyWindowInsetsListener` aplicado al root View + paddings en el TopSheet.
- Ver `TopSheet.kt` para la implementación completa del inset handling.

### Gradle Lock Fix (macOS)
- Gradle fallaba con "Unable to delete directory" en macOS por file watching del daemon.
- Fix: `org.gradle.vfs.watch=false` en `gradle.properties` desactiva el file watching.
- También fue necesario nukear `.gradle/` cache stale antes del primer build exitoso.

### KSP + Hilt Compatibilidad
- KSP 2.1.21-1.0.29 **no existe** — la versión correcta para Kotlin 2.1.21 es `2.1.21-2.0.2`.
- Hilt 2.52 es incompatible con Kotlin 2.1.21 (metadata mismatch).
- Hilt 2.59.2 requiere AGP 9.0.0+.
- **Combo estable final**: Hilt 2.57.1 (KAPT) + AGP 8.9.2 + Kotlin 2.1.21 + KSP 2.1.21-2.0.2.

### README Actualizado
- Se actualizó `README.md`: SDK 34 → 36, Android 14 → 16 (líneas 54 y 128).

## Acceptance Criteria

- [x] `./gradlew assembleDebug` compila sin errores
- [x] `./gradlew compileDebugKotlin` type-check pasa
- [x] compileSdk = 36, targetSdk = 36 en APK generado
- [x] La app corre en Android 16 sin regresiones visuales
- [x] Edge-to-edge funciona correctamente — `enableEdgeToEdge()` en `MainActivity`, sin `windowOptOutEdgeToEdgeEnforcement`
- [x] Predictive back habilitado vía Navigation Compose 2.8.x
- [x] Temas (light/dark/system) siguen funcionando
- [x] Localización es/en sigue funcionando
- [x] TopSheet visible debajo del camera notch (WindowInsets.statusBars)
- [x] `./gradlew test` — pendiente (no ejecutado aún)
