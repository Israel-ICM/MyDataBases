package com.sphynxs.mydatabases.ui.theme.tokens

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests unitarios para AppSpacing.
 *
 * Verifica que todos los valores de spacing definidos en el spec
 * estén correctamente implementados.
 *
 * @author israel-icm (TDD RED)
 * @date 2026-06-15
 */
class AppSpacingTest {

    @Test
    fun `AppSpacing provee todos los valores definidos en spec`() {
        val spacing = AppSpacing()
        
        assertEquals(0.dp, spacing.none)
        assertEquals(2.dp, spacing.xxs)
        assertEquals(4.dp, spacing.xs)
        assertEquals(8.dp, spacing.sm)
        assertEquals(12.dp, spacing.md)
        assertEquals(16.dp, spacing.lg)
        assertEquals(24.dp, spacing.xl)
        assertEquals(32.dp, spacing.xxl)
        assertEquals(48.dp, spacing.xxxl)
    }
}
