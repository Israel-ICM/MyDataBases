package com.sphynxs.mydatabases.ui.screens.settings

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.sphynxs.mydatabases.data.storage.QueryStorageResolver
import com.sphynxs.mydatabases.domain.models.RootResolution
import com.sphynxs.mydatabases.domain.repositories.QueryFileStore
import com.sphynxs.mydatabases.domain.repositories.SettingsRepository
import com.sphynxs.mydatabases.domain.usecases.queryfiles.MigrateQueryFilesUseCase
import com.sphynxs.mydatabases.domain.usecases.queryfiles.MigrationResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * TDD tests for `SettingsViewModel`'s query-storage-location behavior (change
 * `query-files-storage`, Phase 8).
 *
 * The "does the old root already have files?" check is a cheap structural scan (no content
 * read), separate from [MigrateQueryFilesUseCase] which does the actual read-then-write copy —
 * only invoked once, on confirm, never during the initial has-files check.
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelQueryStorageTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var resolver: QueryStorageResolver
    private lateinit var store: QueryFileStore
    private lateinit var migrateUseCase: MigrateQueryFilesUseCase
    private lateinit var viewModel: SettingsViewModel
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
        settingsRepository = mockk(relaxed = true)
        resolver = mockk()
        store = mockk()
        migrateUseCase = mockk()
        every { settingsRepository.observeBrandedPaletteEnabled() } returns flowOf(true)
        every { settingsRepository.observeThemeMode() } returns flowOf(com.sphynxs.mydatabases.domain.models.ThemeMode.SYSTEM)
        every { settingsRepository.observeQueryStorageTreeUri() } returns flowOf(null)
        viewModel = SettingsViewModel(settingsRepository, resolver, store, migrateUseCase)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    private fun rootWithNoFiles(): DocumentFile = mockk(relaxed = true) {
        every { findFile(any()) } returns null
    }

    private fun rootWithOneFile(): DocumentFile {
        val sqlFile = mockk<DocumentFile>(relaxed = true) {
            every { isFile } returns true
            every { name } returns "a.sql"
        }
        val queriesFolder = mockk<DocumentFile>(relaxed = true) {
            every { listFiles() } returns arrayOf(sqlFile)
        }
        val engineFolder = mockk<DocumentFile>(relaxed = true) {
            every { findFile("queries") } returns queriesFolder
        }
        return mockk(relaxed = true) {
            every { findFile("mysql") } returns engineFolder
            every { findFile(neq("mysql")) } returns null
        }
    }

    @Test
    fun `selecting a new folder with no existing files skips the prompt and persists directly`() = runTest(dispatcher) {
        coEvery { resolver.resolveRoot() } returns RootResolution.Resolved(rootWithNoFiles())
        val newUri = mockk<Uri>(relaxed = true)

        viewModel.onStorageTreeSelected(newUri)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.migrationPrompt.value is MigrationPromptState.Hidden)
        coVerify { settingsRepository.setQueryStorageTreeUri(newUri) }
        coVerify(exactly = 0) { migrateUseCase(any(), any(), any()) }
    }

    @Test
    fun `selecting a new folder with existing files shows the migration prompt without persisting yet`() = runTest(dispatcher) {
        coEvery { resolver.resolveRoot() } returns RootResolution.Resolved(rootWithOneFile())
        val newUri = mockk<Uri>(relaxed = true)

        viewModel.onStorageTreeSelected(newUri)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.migrationPrompt.value is MigrationPromptState.Shown)
        coVerify(exactly = 0) { settingsRepository.setQueryStorageTreeUri(newUri) }
    }

    @Test
    fun `confirming migration persists the new location then copies files`() = runTest(dispatcher) {
        coEvery { resolver.resolveRoot() } returns RootResolution.Resolved(rootWithOneFile())
        val newUri = mockk<Uri>(relaxed = true)
        coEvery { migrateUseCase(any(), any(), any()) } returns MigrationResult(true, 1, 0)

        viewModel.onStorageTreeSelected(newUri)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.confirmMigration()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { settingsRepository.setQueryStorageTreeUri(newUri) }
        coVerify(exactly = 1) { migrateUseCase(any(), any(), any()) }
        assertTrue(viewModel.migrationPrompt.value is MigrationPromptState.Hidden)
    }

    @Test
    fun `declining migration persists the new location but never copies files`() = runTest(dispatcher) {
        coEvery { resolver.resolveRoot() } returns RootResolution.Resolved(rootWithOneFile())
        val newUri = mockk<Uri>(relaxed = true)

        viewModel.onStorageTreeSelected(newUri)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.declineMigration()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { settingsRepository.setQueryStorageTreeUri(newUri) }
        coVerify(exactly = 0) { migrateUseCase(any(), any(), any()) }
        assertTrue(viewModel.migrationPrompt.value is MigrationPromptState.Hidden)
    }
}
