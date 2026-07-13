package com.sphynxs.mydatabases.ui.theme

import com.sphynxs.mydatabases.domain.models.ThemeMode
import org.junit.Assert.assertFalse
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
}
