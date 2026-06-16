package com.sphynxs.mydatabases.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sphynxs.mydatabases.data.repositories.SettingsRepositoryImpl
import com.sphynxs.mydatabases.domain.models.ThemeMode
import com.sphynxs.mydatabases.domain.repositories.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Tests para SettingsRepositoryImpl con DataStore mockeado.
 *
 * Verifica que el repositorio lee correctamente los valores de DataStore.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryImplTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepository

    @Before
    fun setup() {
        dataStore = mockk(relaxed = true)
        repository = SettingsRepositoryImpl(dataStore)
    }

    /**
     * Verifica que getThemeMode retorna SYSTEM por defecto cuando no hay datos guardados.
     */
    @Test
    fun getThemeMode_cuandoNoHayDatos_retornaSYSTEM() = runTest {
        // Given
        every { dataStore.data } returns flowOf(preferencesOf())

        // When
        val themeMode = repository.getThemeMode().first()

        // Then
        assertEquals(ThemeMode.SYSTEM, themeMode)
    }

    /**
     * Verifica que getThemeMode retorna DARK cuando está guardado en DataStore.
     */
    @Test
    fun getThemeMode_cuandoHayDARK_retornaDARK() = runTest {
        // Given
        val key = stringPreferencesKey("theme_mode")
        every { dataStore.data } returns flowOf(preferencesOf(key to "DARK"))

        // When
        val themeMode = repository.getThemeMode().first()

        // Then
        assertEquals(ThemeMode.DARK, themeMode)
    }

    /**
     * Verifica que getThemeMode retorna LIGHT cuando está guardado en DataStore.
     */
    @Test
    fun getThemeMode_cuandoHayLIGHT_retornaLIGHT() = runTest {
        // Given
        val key = stringPreferencesKey("theme_mode")
        every { dataStore.data } returns flowOf(preferencesOf(key to "LIGHT"))

        // When
        val themeMode = repository.getThemeMode().first()

        // Then
        assertEquals(ThemeMode.LIGHT, themeMode)
    }

    /**
     * Verifica que getThemeMode retorna SYSTEM cuando está guardado explícitamente.
     */
    @Test
    fun getThemeMode_cuandoHaySYSTEM_retornaSYSTEM() = runTest {
        // Given
        val key = stringPreferencesKey("theme_mode")
        every { dataStore.data } returns flowOf(preferencesOf(key to "SYSTEM"))

        // When
        val themeMode = repository.getThemeMode().first()

        // Then
        assertEquals(ThemeMode.SYSTEM, themeMode)
    }
}
