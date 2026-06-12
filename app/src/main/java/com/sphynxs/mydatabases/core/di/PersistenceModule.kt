package com.sphynxs.mydatabases.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Extension para crear el DataStore de preferencias de forma lazy.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Módulo de Hilt que provee DataStore para persistencia de preferencias.
 *
 * Configura DataStore Preferences para guardar configuraciones del usuario (tema, idioma, etc).
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {

    /**
     * Provee el DataStore de preferencias de usuario.
     *
     * @param context Contexto de la aplicación
     * @return Instancia de DataStore para preferencias
     */
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.dataStore
    }
}
