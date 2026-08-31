package com.sphynxs.mydatabases.ui.screens.settings

import app.cash.turbine.test
import com.sphynxs.mydatabases.data.storage.QueryStorageResolver
import com.sphynxs.mydatabases.domain.models.ThemeMode
import com.sphynxs.mydatabases.domain.repositories.QueryFileStore
import com.sphynxs.mydatabases.domain.repositories.SettingsRepository
import com.sphynxs.mydatabases.domain.usecases.queryfiles.MigrateQueryFilesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests para `SettingsViewModel.themeMode` (StateFlow) y `setThemeMode()`.
 *
 * Repositorio mockeado con Mockk; `themeMode` se colecciona con Turbine porque usa
 * `stateIn(SharingStarted.WhileSubscribed)`, que solo colecciona el upstream mientras
 * haya un subscriptor activo.
 *
 * @author gentle-ai (TDD RED)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SettingsRepository
    private lateinit var queryStorageResolver: QueryStorageResolver
    private lateinit var queryFileStore: QueryFileStore
    private lateinit var migrateQueryFilesUseCase: MigrateQueryFilesUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        every { repository.observeBrandedPaletteEnabled() } returns flowOf(false)
        every { repository.observeQueryStorageTreeUri() } returns flowOf(null)
        queryStorageResolver = mockk(relaxed = true)
        queryFileStore = mockk(relaxed = true)
        migrateQueryFilesUseCase = mockk(relaxed = true)
    }

    private fun viewModel(): SettingsViewModel =
        SettingsViewModel(repository, queryStorageResolver, queryFileStore, migrateQueryFilesUseCase)

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `themeMode exposes SYSTEM when repository reports SYSTEM`() = runTest {
        every { repository.observeThemeMode() } returns MutableStateFlow(ThemeMode.SYSTEM)
        val viewModel = viewModel()

        viewModel.themeMode.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem())
        }
    }

    @Test
    fun `themeMode exposes DARK when repository reports DARK`() = runTest {
        every { repository.observeThemeMode() } returns MutableStateFlow(ThemeMode.DARK)
        val viewModel = viewModel()

        viewModel.themeMode.test {
            // `stateIn(WhileSubscribed)` emits `initialValue` (SYSTEM) as soon as this
            // collector subscribes, before the upstream coroutine has a chance to run
            // on the StandardTestDispatcher. Skip that first emission and assert on the
            // value that arrives once the upstream flow is actually collected.
            skipItems(1)
            assertEquals(ThemeMode.DARK, awaitItem())
        }
    }

    @Test
    fun `setThemeMode delegates to repository setThemeMode with LIGHT`() = runTest {
        every { repository.observeThemeMode() } returns MutableStateFlow(ThemeMode.SYSTEM)
        coEvery { repository.setThemeMode(any()) } returns Unit
        val viewModel = viewModel()

        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        coVerify { repository.setThemeMode(ThemeMode.LIGHT) }
    }
}
