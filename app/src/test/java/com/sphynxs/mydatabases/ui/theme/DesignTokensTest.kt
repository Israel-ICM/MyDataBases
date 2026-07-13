package com.sphynxs.mydatabases.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests para `buildDesignTokens(scheme)`, la función pura que deriva un
 * [DesignTokens] theme-aware a partir de un `ColorScheme` resuelto (light o dark).
 * Sin dependencias de Compose runtime más allá de `ColorScheme`/`Color` — ver
 * `openspec/changes/dark-mode/design.md` (Testing Strategy, Interfaces/Contracts).
 *
 * Cubre específicamente el gap de contraste señalado en design.md: `textSecondary`
 * NO debe derivar de `scheme.outline` (falla WCAG AA: 2.37:1 contra `brand_surface`),
 * debe derivar de `scheme.secondary` (5.81:1, AA-compliant).
 *
 * @author gentle-ai (TDD RED)
 */
class DesignTokensTest {

    @Test
    fun `buildDesignTokens with light scheme returns light-appropriate structural values`() {
        val tokens = buildDesignTokens(BrandedLightColorScheme)

        assertEquals(BrandedLightColorScheme.background, tokens.backgroundPrimary)
        assertEquals(BrandedLightColorScheme.surface, tokens.surfacePrimary)
        assertEquals(BrandedLightColorScheme.onBackground, tokens.textPrimary)
    }

    @Test
    fun `buildDesignTokens with dark scheme returns dark-appropriate structural values`() {
        val tokens = buildDesignTokens(BrandedDarkColorScheme)

        assertEquals(BrandedDarkColorScheme.background, tokens.backgroundPrimary)
        assertEquals(BrandedDarkColorScheme.surface, tokens.surfacePrimary)
        assertEquals(BrandedDarkColorScheme.onBackground, tokens.textPrimary)
    }

    @Test
    fun `light and dark instances differ on theme-varying structural fields`() {
        val light = buildDesignTokens(BrandedLightColorScheme)
        val dark = buildDesignTokens(BrandedDarkColorScheme)

        assertNotEquals(light.backgroundPrimary, dark.backgroundPrimary)
        assertNotEquals(light.surfacePrimary, dark.surfacePrimary)
        assertNotEquals(light.textPrimary, dark.textPrimary)
        assertNotEquals(light.textSecondary, dark.textSecondary)
    }

    @Test
    fun `light and dark instances share theme-invariant typography and spacing`() {
        val light = buildDesignTokens(BrandedLightColorScheme)
        val dark = buildDesignTokens(BrandedDarkColorScheme)

        assertEquals(light.cardTitleSize, dark.cardTitleSize)
        assertEquals(light.cardPadding, dark.cardPadding)
        assertEquals(light.iconSmall, dark.iconSmall)
        assertEquals(light.cardCornerRadius, dark.cardCornerRadius)
    }

    @Test
    fun `textSecondary derives from scheme secondary, not scheme outline (WCAG AA regression guard)`() {
        // scheme.outline against BrandedDarkColorScheme's own surface is only 2.37:1,
        // well below the 4.5:1 WCAG AA threshold for text. scheme.secondary (9DA1C0)
        // gives 5.81:1. See design.md Open Questions + apply-progress contrast log.
        val dark = buildDesignTokens(BrandedDarkColorScheme)

        assertEquals(BrandedDarkColorScheme.secondary, dark.textSecondary)
        assertNotEquals(BrandedDarkColorScheme.outline, dark.textSecondary)
    }

    @Test
    fun `dependent color roles compose from the same resolved text locals`() {
        val light = buildDesignTokens(BrandedLightColorScheme)

        assertEquals(light.textPrimary, light.largeTitleColor)
        assertEquals(light.textPrimary, light.cardTitleColor)
        assertEquals(light.textSecondary, light.cardSubtitleColor)
        assertEquals(light.textTertiary, light.captionColor)
    }
}
