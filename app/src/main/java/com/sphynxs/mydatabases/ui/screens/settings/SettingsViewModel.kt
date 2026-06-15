package com.sphynxs.mydatabases.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 *
 * Future: theme mode, language, etc. (cambio #6 del roadmap)
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
}
