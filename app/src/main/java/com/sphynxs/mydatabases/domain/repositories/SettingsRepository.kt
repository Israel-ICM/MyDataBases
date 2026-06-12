package com.sphynxs.mydatabases.domain.repositories

import com.sphynxs.mydatabases.domain.models.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para gestionar la configuración del usuario.
 *
 * Provee acceso a las preferencias guardadas en DataStore (modo de tema, idioma, etc).
 *
 * @author israel-icm
 * @date 2026-06-12
 */
interface SettingsRepository {

    /**
     * Obtiene el modo de tema actual del usuario.
     *
     * @return Flow con el modo de tema (LIGHT, DARK, SYSTEM). Por defecto SYSTEM si no hay datos guardados.
     */
    fun getThemeMode(): Flow<ThemeMode>

    /**
     * Guarda el modo de tema elegido por el usuario.
     *
     * @param mode El modo de tema a guardar
     */
    suspend fun setThemeMode(mode: ThemeMode)
}
