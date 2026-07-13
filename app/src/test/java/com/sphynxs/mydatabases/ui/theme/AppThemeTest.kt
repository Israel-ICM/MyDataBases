package com.sphynxs.mydatabases.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.sphynxs.mydatabases.domain.models.ThemeMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests para `resolveDarkTheme(themeMode, systemInDarkTheme)`, la función pura que
 * resuelve si el tema oscuro debe estar activo. Sin dependencias de Compose/Android —
 * ver `openspec/changes/dark-mode/design.md` (Testing Strategy).
 *
 * @author gentle-ai (TDD RED)
 */
class AppThemeTest {

    @Test
    fun `resolveDarkTheme returns false for LIGHT regardless of system state`() {
        assertFalse(resolveDarkTheme(ThemeMode.LIGHT, systemInDarkTheme = true))
        assertFalse(resolveDarkTheme(ThemeMode.LIGHT, systemInDarkTheme = false))
    }

    @Test
    fun `resolveDarkTheme returns true for DARK regardless of system state`() {
        assertTrue(resolveDarkTheme(ThemeMode.DARK, systemInDarkTheme = false))
        assertTrue(resolveDarkTheme(ThemeMode.DARK, systemInDarkTheme = true))
    }

    @Test
    fun `resolveDarkTheme follows system when SYSTEM and system is dark`() {
        assertTrue(resolveDarkTheme(ThemeMode.SYSTEM, systemInDarkTheme = true))
    }

    @Test
    fun `resolveDarkTheme follows system when SYSTEM and system is light`() {
        assertFalse(resolveDarkTheme(ThemeMode.SYSTEM, systemInDarkTheme = false))
    }

    // ============================================================================
    // resolveColorScheme — Requirement "Effective ColorScheme Resolution" (R3)
    // Fix-round for verify-report.md CRITICAL #1: this branching previously lived
    // inline in `AppTheme`'s `@Composable` body and had ZERO test coverage. Extracted
    // to a pure function (no Compose/Context dependency) so all 5 spec scenarios are
    // directly, deterministically testable — mirrors `resolveDarkTheme`'s pattern.
    // ============================================================================

    @Test
    fun `Dark + branded resolves to BrandedDarkColorScheme (R3 scenario 1)`() {
        val result = resolveColorScheme(
            darkTheme = true,
            brandedPaletteEnabled = true,
            dynamicColorAvailable = true,
            dynamicScheme = lightColorScheme()
        )

        assertSame(BrandedDarkColorScheme, result)
    }

    @Test
    fun `Light + branded resolves to BrandedLightColorScheme (R3 scenario 2)`() {
        val result = resolveColorScheme(
            darkTheme = false,
            brandedPaletteEnabled = true,
            dynamicColorAvailable = true,
            dynamicScheme = lightColorScheme()
        )

        assertSame(BrandedLightColorScheme, result)
    }

    @Test
    fun `Dark + non-branded resolves to the dynamic dark scheme when available (R3 scenario 3)`() {
        val fakeDynamicDark = lightColorScheme() // stand-in for dynamicDarkColorScheme(context)

        val result = resolveColorScheme(
            darkTheme = true,
            brandedPaletteEnabled = false,
            dynamicColorAvailable = true,
            dynamicScheme = fakeDynamicDark
        )

        assertSame(fakeDynamicDark, result)
    }

    @Test
    fun `Dark + non-branded falls back to BrandedDarkColorScheme when dynamic color is unavailable`() {
        // Triangulation: proves the branch is NOT hardcoded to always return the fake —
        // when dynamic color can't be resolved (pre-existing, documented fallback, see
        // verify-report.md Coherence table), branded is used even with branded disabled.
        val result = resolveColorScheme(
            darkTheme = true,
            brandedPaletteEnabled = false,
            dynamicColorAvailable = false,
            dynamicScheme = null
        )

        assertSame(BrandedDarkColorScheme, result)
    }

    @Test
    fun `System defers to OS-dark then applies branded axis (R3 scenario 4, composed with resolveDarkTheme)`() {
        val darkTheme = resolveDarkTheme(ThemeMode.SYSTEM, systemInDarkTheme = true)

        val result = resolveColorScheme(
            darkTheme = darkTheme,
            brandedPaletteEnabled = true,
            dynamicColorAvailable = true,
            dynamicScheme = lightColorScheme()
        )

        assertSame(BrandedDarkColorScheme, result)
    }

    @Test
    fun `Axes are independent - toggling branded_palette never changes the resolved dark-vs-light base (R3 scenario 5)`() {
        val fakeDynamicDark = lightColorScheme()
        val fakeDynamicLight = darkColorScheme() // distinct instance; identity is what's asserted

        // Fix darkTheme = true, toggle branded: base stays "dark side", only the tint moves
        assertSame(
            BrandedDarkColorScheme,
            resolveColorScheme(true, brandedPaletteEnabled = true, dynamicColorAvailable = true, dynamicScheme = fakeDynamicDark)
        )
        assertSame(
            fakeDynamicDark,
            resolveColorScheme(true, brandedPaletteEnabled = false, dynamicColorAvailable = true, dynamicScheme = fakeDynamicDark)
        )

        // Fix darkTheme = false, toggle branded: base stays "light side"
        assertSame(
            BrandedLightColorScheme,
            resolveColorScheme(false, brandedPaletteEnabled = true, dynamicColorAvailable = true, dynamicScheme = fakeDynamicLight)
        )
        assertSame(
            fakeDynamicLight,
            resolveColorScheme(false, brandedPaletteEnabled = false, dynamicColorAvailable = true, dynamicScheme = fakeDynamicLight)
        )

        // Fix brandedPaletteEnabled = true, toggle darkTheme: branded axis alone must pick
        // the matching branded scheme, never crossing into the other base
        assertSame(
            BrandedDarkColorScheme,
            resolveColorScheme(true, brandedPaletteEnabled = true, dynamicColorAvailable = true, dynamicScheme = fakeDynamicDark)
        )
        assertSame(
            BrandedLightColorScheme,
            resolveColorScheme(false, brandedPaletteEnabled = true, dynamicColorAvailable = true, dynamicScheme = fakeDynamicLight)
        )
    }
}
