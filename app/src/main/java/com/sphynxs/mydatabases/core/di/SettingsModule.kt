package com.sphynxs.mydatabases.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt para configuración y settings de la aplicación.
 *
 * Provee:
 * - DataStore<Preferences> para persistencia de settings
 * - SettingsRepository binding
 *
 * @author israel-icm
 * @date 2026-06-15
 */
@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    
    /**
     * Provee DataStore<Preferences> como singleton.
     *
     * - Archivo: "settings.preferences_pb" en dataDir de la app
     * - Corruption handler: emptyPreferences (reset en caso de corrupción)
     * - Migrations: ninguna (primera versión)
     *
     * @param context Contexto de la aplicación
     * @return DataStore<Preferences> singleton
     */
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            produceFile = { context.preferencesDataStoreFile("settings") }
        )
    }
}
