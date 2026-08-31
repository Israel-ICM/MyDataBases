package com.sphynxs.mydatabases.data.repositories

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.sphynxs.mydatabases.domain.models.ThemeMode
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
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

    // --- query storage tree uri (change `query-files-storage`) ---

    @Before
    fun mockUriStatics() {
        // Uri.parse is an Android framework stub under plain JUnit — mock the one static
        // method this repository actually calls; returned mocks just echo back their string
        // via toString(), which is all SettingsRepositoryImpl round-trips through DataStore.
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } answers {
            val value = firstArg<String>()
            mockk<Uri>(relaxed = true).also { every { it.toString() } returns value }
        }
    }

    @After
    fun unmockUriStatics() {
        io.mockk.unmockkStatic(Uri::class)
    }

    @Test
    fun `observeQueryStorageTreeUri returns null by default when unset`() = runTest {
        val repository = SettingsRepositoryImpl(FakeDataStore())

        val uri = repository.observeQueryStorageTreeUri().first()

        assertNull(uri)
    }

    @Test
    fun `setQueryStorageTreeUri then observeQueryStorageTreeUri round-trips the value`() = runTest {
        val repository = SettingsRepositoryImpl(FakeDataStore())
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://com.android.externalstorage.documents/tree/abc"

        repository.setQueryStorageTreeUri(uri)
        val observed = repository.observeQueryStorageTreeUri().first()

        assertEquals(uri.toString(), observed.toString())
    }

    @Test
    fun `setQueryStorageTreeUri with null clears the preference back to default`() = runTest {
        val repository = SettingsRepositoryImpl(FakeDataStore())
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://x/tree/y"
        repository.setQueryStorageTreeUri(uri)

        repository.setQueryStorageTreeUri(null)
        val observed = repository.observeQueryStorageTreeUri().first()

        assertNull(observed)
    }
}
