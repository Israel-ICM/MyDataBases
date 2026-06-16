package com.sphynxs.mydatabases.ui.screens.settings

import app.cash.turbine.test
import com.sphynxs.mydatabases.data.repositories.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests unitarios para SettingsViewModel.
 *
 * Verifica que el StateFlow refleja correctamente los cambios del repository.
 *
 * @author israel-icm (TDD RED → TRIANGULATE)
 * @date 2026-06-15
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockRepository: SettingsRepository = mockk()
    private lateinit var viewModel: SettingsViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `brandedPaletteEnabled refleja el Flow del repository con valor false`() = runTest {
        // GIVEN: repository emite false
        every { mockRepository.observeBrandedPaletteEnabled() } returns flowOf(false)
        viewModel = SettingsViewModel(mockRepository)
        
        // WHEN: observamos el StateFlow
        viewModel.brandedPaletteEnabled.test {
            // THEN: emite false
            val emission = awaitItem()
            assertFalse(emission)
        }
    }
    
    @Test
    fun `brandedPaletteEnabled refleja el Flow del repository con valor true`() = runTest {
        // GIVEN: repository emite true
        every { mockRepository.observeBrandedPaletteEnabled() } returns flowOf(true)
        viewModel = SettingsViewModel(mockRepository)
        
        // WHEN: observamos el StateFlow
        viewModel.brandedPaletteEnabled.test {
            // THEN: emite true
            val emission = awaitItem()
            assertTrue(emission)
        }
    }
    
    @Test
    fun `setBrandedPaletteEnabled llama al repository`() = runTest {
        // GIVEN: repository configurado
        every { mockRepository.observeBrandedPaletteEnabled() } returns flowOf(false)
        coEvery { mockRepository.setBrandedPaletteEnabled(any()) } returns Unit
        viewModel = SettingsViewModel(mockRepository)
        
        // WHEN: llamamos setBrandedPaletteEnabled(true)
        viewModel.setBrandedPaletteEnabled(true)
        
        // THEN: repository.setBrandedPaletteEnabled fue llamado
        coVerify { mockRepository.setBrandedPaletteEnabled(true) }
    }
}
