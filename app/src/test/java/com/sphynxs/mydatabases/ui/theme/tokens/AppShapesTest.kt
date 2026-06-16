package com.sphynxs.mydatabases.ui.theme.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests unitarios para AppShapes.
 *
 * Verifica que los valores de corner radius estén correctamente definidos.
 *
 * @author israel-icm (TDD RED)
 * @date 2026-06-15
 */
class AppShapesTest {

    @Test
    fun `AppShapes provee las 5 formas definidas en spec`() {
        val shapes = AppShapes()
        
        assertEquals(RoundedCornerShape(0.dp), shapes.none)
        assertEquals(RoundedCornerShape(8.dp), shapes.small)
        assertEquals(RoundedCornerShape(12.dp), shapes.medium)
        assertEquals(RoundedCornerShape(20.dp), shapes.large)
        assertEquals(RoundedCornerShape(28.dp), shapes.extraLarge)
    }
}
