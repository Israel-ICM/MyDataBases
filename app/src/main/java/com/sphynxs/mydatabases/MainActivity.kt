package com.sphynxs.mydatabases

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.tooling.preview.Preview
import com.sphynxs.mydatabases.domain.models.ThemeMode
import com.sphynxs.mydatabases.ui.navigation.MyDataBasesNavHost
import com.sphynxs.mydatabases.ui.theme.MyDataBasesTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * CompositionLocal para exponer el WindowSizeClass a toda la jerarquía de Composables.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
val LocalWindowSizeClass = staticCompositionLocalOf<WindowSizeClass?> {
    null
}

/**
 * Actividad principal de la aplicación.
 *
 * Configura el tema adaptativo, WindowSizeClass y el NavHost principal.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Calcular WindowSizeClass para adaptación de UI
            val windowSizeClass = calculateWindowSizeClass(this)
            
            // ThemeMode hardcodeado a SYSTEM por ahora (será dinámico en PR #2)
            val themeMode = remember { mutableStateOf(ThemeMode.SYSTEM) }
            
            CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
                MyDataBasesTheme(themeMode = themeMode.value) {
                    MyDataBasesNavHost()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    MyDataBasesTheme {
        MyDataBasesNavHost()
    }
}
