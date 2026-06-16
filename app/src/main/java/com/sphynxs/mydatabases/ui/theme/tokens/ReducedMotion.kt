package com.sphynxs.mydatabases.ui.theme.tokens

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Detecta y observa cambios en la configuración de reduced motion del sistema.
 *
 * Lee `Settings.Global.ANIMATOR_DURATION_SCALE` y registra un ContentObserver
 * para detectar cambios en runtime.
 *
 * Un valor de 0.0 indica que el usuario tiene animaciones desactivadas
 * (Configuración de desarrollador → "Escala de duración de animador" → OFF).
 *
 * @param context Contexto Android para acceder a Settings.Global
 * @return State<Boolean> — true si reduced motion está activo, false si no
 *
 * @author israel-icm
 * @date 2026-06-15
 */
@Composable
fun rememberReducedMotion(context: Context): State<Boolean> {
    val isReducedMotion = remember {
        mutableStateOf(isReducedMotionEnabled(context))
    }
    
    DisposableEffect(context) {
        val contentResolver = context.contentResolver
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                isReducedMotion.value = isReducedMotionEnabled(context)
            }
        }
        
        contentResolver.registerContentObserver(uri, false, observer)
        
        onDispose {
            contentResolver.unregisterContentObserver(observer)
        }
    }
    
    return isReducedMotion
}

/**
 * Lee el valor actual de ANIMATOR_DURATION_SCALE desde Settings.Global.
 *
 * @param context Contexto Android
 * @return true si reduced motion está activo (scale == 0.0), false si no
 */
private fun isReducedMotionEnabled(context: Context): Boolean {
    return try {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
        scale == 0.0f
    } catch (e: Settings.SettingNotFoundException) {
        false // Default: animaciones activadas
    }
}

/**
 * CompositionLocal para exponer el estado de reduced motion a toda la jerarquía.
 *
 * Provisto por AppTheme automáticamente.
 *
 * Uso:
 * ```kotlin
 * val reduced = LocalReducedMotion.current
 * val duration = if (reduced) 0 else 300
 * ```
 *
 * @author israel-icm
 * @date 2026-06-15
 */
val LocalReducedMotion = staticCompositionLocalOf { false }
