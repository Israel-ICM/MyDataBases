package com.sphynxs.mydatabases.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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
        val tokens = buildDesignTokens(BrandedLightColorScheme, darkTheme = false)

        assertEquals(BrandedLightColorScheme.background, tokens.backgroundPrimary)
        assertEquals(BrandedLightColorScheme.surface, tokens.surfacePrimary)
        assertEquals(BrandedLightColorScheme.onBackground, tokens.textPrimary)
    }

    @Test
    fun `buildDesignTokens with dark scheme returns dark-appropriate structural values`() {
        val tokens = buildDesignTokens(BrandedDarkColorScheme, darkTheme = true)

        assertEquals(BrandedDarkColorScheme.background, tokens.backgroundPrimary)
        assertEquals(BrandedDarkColorScheme.surface, tokens.surfacePrimary)
        assertEquals(BrandedDarkColorScheme.onBackground, tokens.textPrimary)
    }

    @Test
    fun `light and dark instances differ on theme-varying structural fields`() {
        val light = buildDesignTokens(BrandedLightColorScheme, darkTheme = false)
        val dark = buildDesignTokens(BrandedDarkColorScheme, darkTheme = true)

        assertNotEquals(light.backgroundPrimary, dark.backgroundPrimary)
        assertNotEquals(light.surfacePrimary, dark.surfacePrimary)
        assertNotEquals(light.textPrimary, dark.textPrimary)
        assertNotEquals(light.textSecondary, dark.textSecondary)
    }

    @Test
    fun `light and dark instances share theme-invariant typography and spacing`() {
        val light = buildDesignTokens(BrandedLightColorScheme, darkTheme = false)
        val dark = buildDesignTokens(BrandedDarkColorScheme, darkTheme = true)

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
        val dark = buildDesignTokens(BrandedDarkColorScheme, darkTheme = true)

        assertEquals(BrandedDarkColorScheme.secondary, dark.textSecondary)
        assertNotEquals(BrandedDarkColorScheme.outline, dark.textSecondary)
    }

    @Test
    fun `dependent color roles compose from the same resolved text locals`() {
        val light = buildDesignTokens(BrandedLightColorScheme, darkTheme = false)

        assertEquals(light.textPrimary, light.largeTitleColor)
        assertEquals(light.textPrimary, light.cardTitleColor)
        assertEquals(light.textSecondary, light.cardSubtitleColor)
        assertEquals(light.textTertiary, light.captionColor)
    }

    // ============================================================================
    // WCAG contrast regression guards — verify-report.md WARNING #3 (accentSuccess)
    // and WARNING #4 (textTertiary). Computed via `contrastRatio()` (ContrastUtils.kt,
    // sanity-checked in ContrastUtilsTest), not eyeballed or hardcoded as a comment.
    // ============================================================================

    @Test
    fun `accentSuccess clears the WCAG 1_4_11 non-text 3-to-1 minimum against surface in both themes`() {
        val light = buildDesignTokens(BrandedLightColorScheme, darkTheme = false)
        val dark = buildDesignTokens(BrandedDarkColorScheme, darkTheme = true)

        val lightRatio = contrastRatio(light.accentSuccess, light.surfacePrimary)
        val darkRatio = contrastRatio(dark.accentSuccess, dark.surfacePrimary)

        assertTrue(
            "light accentSuccess/surface contrast was $lightRatio, expected >= 3.0",
            lightRatio >= 3.0
        )
        assertTrue(
            "dark accentSuccess/surface contrast was $darkRatio, expected >= 3.0 " +
                "(regression guard: was 2.30:1 before this fix — verify-report.md WARNING #3)",
            darkRatio >= 3.0
        )
    }

    @Test
    fun `accentSuccess dark value is theme-aware, not the reused light literal (triangulation)`() {
        val light = buildDesignTokens(BrandedLightColorScheme, darkTheme = false)
        val dark = buildDesignTokens(BrandedDarkColorScheme, darkTheme = true)

        assertNotEquals(light.accentSuccess, dark.accentSuccess)
    }

    @Test
    fun `textTertiary dark value clears the WCAG AA 4_5-to-1 minimum for normal text`() {
        val dark = buildDesignTokens(BrandedDarkColorScheme, darkTheme = true)

        val ratio = contrastRatio(dark.textTertiary, dark.surfacePrimary)

        assertTrue(
            "dark textTertiary/surface contrast was $ratio, expected >= 4.5 " +
                "(regression guard: was 2.37:1 before this fix — verify-report.md WARNING #4)",
            ratio >= 4.5
        )
    }

    @Test
    fun `textTertiary dark value no longer reuses scheme outline (triangulation)`() {
        val dark = buildDesignTokens(BrandedDarkColorScheme, darkTheme = true)

        assertNotEquals(BrandedDarkColorScheme.outline, dark.textTertiary)
    }
}
