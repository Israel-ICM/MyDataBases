package com.sphynxs.mydatabases.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
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
 * Tema principal de MyDataBases con design system completo.
 *
 * Integra:
 * - Design tokens (Spacing, Shapes, Elevation, Motion) vía CompositionLocals
 * - Branded color palette con toggle dinámico
 * - Dynamic color support (Android 12+)
 * - Reduced motion detection
 * - Edge-to-edge status bar appearance
 *
 * Lógica de selección de ColorScheme:
 * 1. Si `userPrefersBranded == true` → BrandedDark/LightColorScheme
 * 2. Si `userPrefersBranded == false` && Android 12+ → dynamicDark/LightColorScheme
 * 3. Fallback (Android < 12 o dynamic no disponible) → BrandedDark/LightColorScheme
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
    
    // Determinar si dark theme está activo
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemInDarkTheme
    }
    
    // Observar preferencia de branded palette desde SettingsViewModel
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val userPrefersBranded by settingsViewModel.brandedPaletteEnabled.collectAsState()
    
    // Detectar reduced motion
    val isReducedMotion by rememberReducedMotion(context)
    
    // Selección de ColorScheme
    val colorScheme = when {
        // Usuario eligió branded explícitamente
        userPrefersBranded -> {
            if (darkTheme) BrandedDarkColorScheme else BrandedLightColorScheme
        }
        
        // Dynamic color disponible (Android 12+)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        
        // Fallback a branded para dispositivos < Android 12
        else -> {
            if (darkTheme) BrandedDarkColorScheme else BrandedLightColorScheme
        }
    }
    
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
        LocalReducedMotion provides isReducedMotion
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
