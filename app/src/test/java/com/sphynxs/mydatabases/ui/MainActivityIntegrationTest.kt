package com.sphynxs.mydatabases.ui

import com.sphynxs.mydatabases.domain.models.ThemeMode
import org.junit.Test
import org.junit.Assert.assertNotNull

/**
 * Tests de smoke para MainActivity.
 *
 * MainActivity es una actividad de infraestructura que renderiza NavHost.
 * Los tests de comportamiento real (navegación, scaffold adaptativo) se harán
 * en Compose UI Tests (instrumentation) en fases posteriores.
 *
 * Este test verifica que el enum ThemeMode existe y es utilizable.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
class MainActivityIntegrationTest {
    
    /**
     * Smoke test: verifica que ThemeMode enum está disponible.
     *
     * MainActivity usa ThemeMode.SYSTEM por defecto.
     * Este test confirma que el enum se puede instanciar.
     */
    @Test
    fun themeMode_system_isAvailable() {
        // GIVEN: ThemeMode.SYSTEM
        val mode = ThemeMode.SYSTEM
        
        // THEN: debe existir
        assertNotNull(mode)
    }
}
