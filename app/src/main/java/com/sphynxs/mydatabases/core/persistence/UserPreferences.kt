package com.sphynxs.mydatabases.core.persistence

import com.sphynxs.mydatabases.domain.models.ThemeMode

/**
 * Preferencias del usuario guardadas en DataStore.
 *
 * Contiene configuraciones como el modo de tema y el idioma de la interfaz.
 *
 * @property themeMode Modo de tema elegido por el usuario (LIGHT, DARK, SYSTEM)
 * @author israel-icm
 * @date 2026-06-12
 */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
