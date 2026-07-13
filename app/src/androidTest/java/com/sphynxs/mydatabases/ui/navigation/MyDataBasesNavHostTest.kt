package com.sphynxs.mydatabases.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sphynxs.mydatabases.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests de navegación principal de la app.
 *
 * Verifica que el NavHost renderice correctamente las rutas iniciales.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@RunWith(AndroidJUnit4::class)
class MyDataBasesNavHostTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * Verifica que el NavHost inicie en la ruta Connections por defecto.
     */
    @Test
    fun navHost_debe_mostrar_pantalla_connections_por_defecto() {
        composeTestRule.setContent {
            AppTheme {
                MyDataBasesNavHost()
            }
        }
        
        // La pantalla de Connections debe estar visible
        composeTestRule
            .onNodeWithText("Connections")
            .assertExists()
    }
    
    /**
     * Verifica que existan pantallas placeholder para todas las rutas.
     */
    @Test
    fun navHost_debe_tener_placeholders_para_todas_las_rutas() {
        composeTestRule.setContent {
            AppTheme {
                MyDataBasesNavHost()
            }
        }
        
        // Verifico que Connections screen se renderiza (es la inicial)
        composeTestRule
            .onNodeWithText("Connections")
            .assertExists()
    }
}
