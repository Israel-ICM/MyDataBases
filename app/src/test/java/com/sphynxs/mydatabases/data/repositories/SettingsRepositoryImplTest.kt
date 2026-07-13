package com.sphynxs.mydatabases.data.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.sphynxs.mydatabases.domain.models.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Fake en memoria de `DataStore<Preferences>` para tests unitarios.
 *
 * Replica el contrato mínimo (`data` + `updateData`) que usa la extensión
 * `DataStore<Preferences>.edit { }`, sin depender de I/O real ni de Robolectric.
 */
private class FakeDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)
    override val data: Flow<Preferences> = state.asStateFlow()

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

/**
 * Unit tests para el round-trip de `theme_mode` en `SettingsRepositoryImpl`.
 *
 * @author gentle-ai (TDD RED)
 */
class SettingsRepositoryImplTest {

    @Test
    fun `observeThemeMode returns SYSTEM by default when unset`() = runTest {
        val repository = SettingsRepositoryImpl(FakeDataStore())

        val mode = repository.observeThemeMode().first()

        assertEquals(ThemeMode.SYSTEM, mode)
    }

    @Test
    fun `setThemeMode with DARK then observeThemeMode returns DARK`() = runTest {
        val repository = SettingsRepositoryImpl(FakeDataStore())

        repository.setThemeMode(ThemeMode.DARK)
        val mode = repository.observeThemeMode().first()

        assertEquals(ThemeMode.DARK, mode)
    }

    @Test
    fun `setThemeMode with LIGHT then observeThemeMode returns LIGHT`() = runTest {
        val repository = SettingsRepositoryImpl(FakeDataStore())

        repository.setThemeMode(ThemeMode.LIGHT)
        val mode = repository.observeThemeMode().first()

        assertEquals(ThemeMode.LIGHT, mode)
    }
}
