package com.sphynxs.mydatabases.data.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import app.cash.turbine.test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk

/**
 * Tests unitarios para SettingsRepository.
 *
 * Verifica que la persistencia de branded palette funciona correctamente
 * usando DataStore.
 *
 * @author israel-icm (TDD RED → TRIANGULATE)
 * @date 2026-06-15
 */
class SettingsRepositoryTest {

    private val mockDataStore: DataStore<Preferences> = mockk()
    private val repository = SettingsRepositoryImpl(mockDataStore)
    
    private val BRANDED_PALETTE_KEY = booleanPreferencesKey("branded_palette_enabled")

    @Test
    fun `observeBrandedPaletteEnabled emite false por defecto`() = runTest {
        // GIVEN: DataStore sin preferencias (empty)
        val emptyPrefs = preferencesOf()
        every { mockDataStore.data } returns flowOf(emptyPrefs)
        
        // WHEN: observamos el Flow
        repository.observeBrandedPaletteEnabled().test {
            // THEN: emite false (default)
            val emission = awaitItem()
            assertFalse(emission)
            awaitComplete()
        }
    }
    
    @Test
    fun `observeBrandedPaletteEnabled emite true cuando está guardado`() = runTest {
        // GIVEN: DataStore con branded_palette_enabled = true
        val prefs = preferencesOf(BRANDED_PALETTE_KEY to true)
        every { mockDataStore.data } returns flowOf(prefs)
        
        // WHEN: observamos el Flow
        repository.observeBrandedPaletteEnabled().test {
            // THEN: emite true
            val emission = awaitItem()
            assertTrue(emission)
            awaitComplete()
        }
    }
    
    @Test
    fun `setBrandedPaletteEnabled persiste el valor en DataStore`() = runTest {
        // GIVEN: DataStore mockead para esperar un edit
        coEvery { mockDataStore.updateData(any()) } returns preferencesOf(BRANDED_PALETTE_KEY to true)
        
        // WHEN: guardamos true
        repository.setBrandedPaletteEnabled(true)
        
        // THEN: DataStore.edit fue llamado
        coVerify { mockDataStore.updateData(any()) }
    }
}
