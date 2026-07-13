package com.sphynxs.mydatabases.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.sphynxs.mydatabases.domain.models.ThemeMode
import com.sphynxs.mydatabases.ui.screens.settings.SettingsViewModel
import com.sphynxs.mydatabases.ui.theme.tokens.AppElevation
import com.sphynxs.mydatabases.ui.theme.tokens.AppMotion
import com.sphynxs.mydatabases.ui.theme.tokens.AppShapes
import com.sphynxs.mydatabases.ui.theme.tokens.AppSpacing
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppElevation
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppMotion
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppShapes
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppSpacing
import com.sphynxs.mydatabases.ui.theme.tokens.LocalReducedMotion
import com.sphynxs.mydatabases.ui.theme.tokens.rememberReducedMotion

/**
 * Tema principal de MyDataBases con design system completo. Único punto de entrada de
 * tema de la app — el tema legacy fue removido (ver `openspec/changes/dark-mode`).
 *
 * Integra:
 * - Design tokens (Spacing, Shapes, Elevation, Motion, `LocalDesignTokens`) vía
 *   CompositionLocals — `LocalDesignTokens` sigue anclado a la paleta branded
 *   (Light/DarkDesignTokens) independientemente del eje dynamic color (ver
 *   `DesignTokens.kt` — `buildDesignTokens`)
 * - Branded color palette con toggle dinámico (leído de `SettingsViewModel`)
 * - Dynamic color support (Android 12+)
 * - Reduced motion detection
 * - Edge-to-edge status bar appearance
 *
 * Ambos ejes son independientes (`theme_mode` × `branded_palette`):
 * 1. Si `brandedPaletteEnabled == true` → BrandedDark/LightColorScheme
 * 2. Si `brandedPaletteEnabled == false` && Android 12+ → dynamicDark/LightColorScheme
 * 3. Fallback (Android < 12 o dynamic no disponible) → BrandedDark/LightColorScheme
 *
 * En `@Preview`/inspection (`LocalInspectionMode`) no hay grafo de Hilt disponible, así
 * que `brandedPaletteEnabled` cae a `true` (mismo look que el hack anterior en preview).
 *
 * @param themeMode Modo de tema (LIGHT, DARK, SYSTEM). Default: SYSTEM
 * @param content Contenido de la app envuelto con este tema
 *
 * @author israel-icm
 * @date 2026-06-15
 */
@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDarkTheme = isSystemInDarkTheme()
    val darkTheme = resolveDarkTheme(themeMode, systemInDarkTheme)

    val brandedPaletteEnabled = if (LocalInspectionMode.current) {
        true
    } else {
        val settingsViewModel: SettingsViewModel = hiltViewModel()
        val brandedState by settingsViewModel.brandedPaletteEnabled.collectAsState()
        brandedState
    }

    val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme: ColorScheme = when {
        brandedPaletteEnabled -> if (darkTheme) BrandedDarkColorScheme else BrandedLightColorScheme
        dynamicColorAvailable -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> if (darkTheme) BrandedDarkColorScheme else BrandedLightColorScheme
    }
    
    // Detectar reduced motion
    val isReducedMotion by rememberReducedMotion(context)
    
    // Edge-to-edge: configurar appearance de status bar
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    
    // Proveer design tokens vía CompositionLocals
    CompositionLocalProvider(
        LocalAppSpacing provides AppSpacing(),
        LocalAppShapes provides AppShapes(),
        LocalAppElevation provides AppElevation(),
        LocalAppMotion provides AppMotion(),
        LocalReducedMotion provides isReducedMotion,
        LocalDesignTokens provides if (darkTheme) DarkDesignTokens else LightDesignTokens
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = androidx.compose.material3.Shapes(
                small = LocalAppShapes.current.small,
                medium = LocalAppShapes.current.medium,
                large = LocalAppShapes.current.large,
                extraLarge = LocalAppShapes.current.extraLarge
            ),
            content = content
        )
    }
}

/**
 * Resuelve si el tema oscuro debe estar activo a partir del `ThemeMode` elegido por el
 * usuario y el estado actual del sistema operativo. Función pura — sin dependencias de
 * Compose/Android — extraída para test unitario directo (ver `AppThemeTest`).
 *
 * @param themeMode Modo de tema seleccionado (LIGHT, DARK o SYSTEM)
 * @param systemInDarkTheme true si el sistema operativo está en modo oscuro
 * @return true si el tema oscuro debe estar activo
 *
 * @author gentle-ai (TDD GREEN)
 */
internal fun resolveDarkTheme(themeMode: ThemeMode, systemInDarkTheme: Boolean): Boolean =
    when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemInDarkTheme
    }
