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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.sphynxs.mydatabases.ui.navigation.MyDataBasesNavHost
import com.sphynxs.mydatabases.ui.screens.settings.SettingsViewModel
import com.sphynxs.mydatabases.ui.theme.AppTheme
import com.sphynxs.mydatabases.ui.workspace.WorkspaceManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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
    
    @Inject
    lateinit var workspaceManager: WorkspaceManager
    
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Calcular WindowSizeClass para adaptación de UI
            val windowSizeClass = calculateWindowSizeClass(this)
            
            // themeMode leído del SettingsViewModel (persistido en DataStore)
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()
            
            CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
                AppTheme(themeMode = themeMode) {
                    MyDataBasesNavHost(workspaceManager = workspaceManager)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    AppTheme {
        MyDataBasesNavHost(workspaceManager = WorkspaceManager())
    }
}
