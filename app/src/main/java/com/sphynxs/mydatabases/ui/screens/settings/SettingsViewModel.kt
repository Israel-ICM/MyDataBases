package com.sphynxs.mydatabases.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphynxs.mydatabases.domain.models.ThemeMode
import com.sphynxs.mydatabases.domain.repositories.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla de Settings.
 *
 * Expone preferencias del usuario como StateFlows y métodos para modificarlas.
 * Actualmente gestiona:
 * - Branded palette toggle
 * - Theme mode (light/dark/system)
 *
 * Future: language, etc. (cambio #6 del roadmap)
 *
 * @author israel-icm (TDD GREEN)
 * @date 2026-06-15
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    /**
     * Estado de la preferencia de paleta branded.
     *
     * - true: usuario prefiere branded colors
     * - false: usuario prefiere dynamic color (o no disponible → branded fallback)
     */
    val brandedPaletteEnabled: StateFlow<Boolean> =
        settingsRepository.observeBrandedPaletteEnabled()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )
    
    /**
     * Persiste la preferencia de paleta branded.
     *
     * @param enabled true para activar branded, false para dynamic
     */
    fun setBrandedPaletteEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBrandedPaletteEnabled(enabled)
        }
    }

    /**
     * Estado del modo de tema (SYSTEM, LIGHT o DARK).
     *
     * Default `SYSTEM` mientras no llega el primer valor persistido.
     */
    val themeMode: StateFlow<ThemeMode> =
        settingsRepository.observeThemeMode()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ThemeMode.SYSTEM
            )

    /**
     * Persiste el modo de tema elegido por el usuario.
     *
     * @param mode Modo de tema a persistir (LIGHT, DARK o SYSTEM)
     */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }
}
