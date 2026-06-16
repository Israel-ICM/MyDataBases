package com.sphynxs.mydatabases.data.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.sphynxs.mydatabases.domain.repositories.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de SettingsRepository usando DataStore.
 *
 * @param dataStore DataStore<Preferences> inyectado por Hilt
 *
 * @author israel-icm (TDD GREEN)
 * @date 2026-06-15
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {
    
    companion object {
        private val BRANDED_PALETTE_KEY = booleanPreferencesKey("branded_palette_enabled")
    }
    
    override fun observeBrandedPaletteEnabled(): Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[BRANDED_PALETTE_KEY] ?: true  // Default: true (branded colors)
        }
    
    override suspend fun setBrandedPaletteEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[BRANDED_PALETTE_KEY] = enabled
        }
    }
}
