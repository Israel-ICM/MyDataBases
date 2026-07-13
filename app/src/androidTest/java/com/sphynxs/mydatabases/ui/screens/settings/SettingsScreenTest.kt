package com.sphynxs.mydatabases.ui.screens.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sphynxs.mydatabases.domain.models.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests para `ThemeModeSelector` (control System/Light/Dark de la pantalla de
 * Settings). Ver `openspec/changes/dark-mode/specs/theme-mode/spec.md` — escenarios
 * "User selects Light/Dark/System".
 *
 * Testea el composable interno `ThemeModeSelector` directamente (no `SettingsScreen`)
 * para evitar `hiltViewModel()`, siguiendo la convención ya establecida en
 * `WorkspaceCarouselTest` ("All fixtures ... to avoid QueryEditorScreen's Hilt
 * `hiltViewModel()`").
 *
 * TDD note (`strict_tdd: true`): escrito PRIMERO, en orden RED->GREEN por tasks.md
 * 1.10/1.11. Ejecución (`./gradlew connectedAndroidTest`) DIFERIDA — no hay
 * dispositivo/emulador disponible en esta sesión (mismo precedente que
 * `WorkspaceCarouselTest`/`StringsResourceTest`). GREEN NO está confirmado por ejecución
 * en esta sesión; ver tabla de evidencia TDD en apply-progress.
 *
 * @author gentle-ai (TDD RED)
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tappingLight_invokesOnSelectWithLight() {
        var selectedMode: ThemeMode? = null
        composeTestRule.setContent {
            ThemeModeSelector(
                selected = ThemeMode.SYSTEM,
                onSelect = { selectedMode = it }
            )
        }

        composeTestRule.onNodeWithText("Light").performClick()

        assertEquals(ThemeMode.LIGHT, selectedMode)
    }

    @Test
    fun tappingDark_invokesOnSelectWithDark() {
        var selectedMode: ThemeMode? = null
        composeTestRule.setContent {
            ThemeModeSelector(
                selected = ThemeMode.SYSTEM,
                onSelect = { selectedMode = it }
            )
        }

        composeTestRule.onNodeWithText("Dark").performClick()

        assertEquals(ThemeMode.DARK, selectedMode)
    }

    @Test
    fun tappingSystem_invokesOnSelectWithSystem() {
        var selectedMode: ThemeMode? = null
        composeTestRule.setContent {
            ThemeModeSelector(
                selected = ThemeMode.DARK,
                onSelect = { selectedMode = it }
            )
        }

        composeTestRule.onNodeWithText("System").performClick()

        assertEquals(ThemeMode.SYSTEM, selectedMode)
    }
}
