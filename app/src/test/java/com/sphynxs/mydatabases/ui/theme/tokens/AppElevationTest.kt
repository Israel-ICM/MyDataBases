package com.sphynxs.mydatabases.ui.theme.tokens

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests unitarios para AppElevation.
 *
 * Verifica que los niveles de elevación estén correctamente definidos.
 *
 * @author israel-icm (TDD RED)
 * @date 2026-06-15
 */
class AppElevationTest {

    @Test
    fun `AppElevation provee los 5 niveles definidos en spec`() {
        val elevation = AppElevation()
        
        assertEquals(0.dp, elevation.none)
        assertEquals(1.dp, elevation.cardResting)
        assertEquals(3.dp, elevation.cardHover)
        assertEquals(6.dp, elevation.cardPressed)
        assertEquals(8.dp, elevation.modal)
    }
}
