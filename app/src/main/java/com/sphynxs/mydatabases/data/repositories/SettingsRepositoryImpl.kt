package com.sphynxs.mydatabases.data.repositories

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sphynxs.mydatabases.domain.models.ThemeMode
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
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val QUERY_STORAGE_TREE_URI_KEY = stringPreferencesKey("query_storage_tree_uri")
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

    override fun observeThemeMode(): Flow<ThemeMode> =
        dataStore.data.map { prefs ->
            prefs[THEME_MODE_KEY]?.let { storedName ->
                runCatching { ThemeMode.valueOf(storedName) }.getOrDefault(ThemeMode.SYSTEM)
            } ?: ThemeMode.SYSTEM  // Default: SYSTEM cuando no hay preferencia guardada
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }

    override fun observeQueryStorageTreeUri(): Flow<Uri?> =
        dataStore.data.map { prefs ->
            prefs[QUERY_STORAGE_TREE_URI_KEY]?.let { Uri.parse(it) }
        }

    override suspend fun setQueryStorageTreeUri(uri: Uri?) {
        dataStore.edit { prefs ->
            if (uri == null) {
                prefs.remove(QUERY_STORAGE_TREE_URI_KEY)
            } else {
                prefs[QUERY_STORAGE_TREE_URI_KEY] = uri.toString()
            }
        }
    }
}
