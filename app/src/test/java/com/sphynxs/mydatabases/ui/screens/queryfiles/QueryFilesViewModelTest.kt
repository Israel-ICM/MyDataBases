package com.sphynxs.mydatabases.ui.screens.queryfiles

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.data.storage.QueryStorageResolver
import com.sphynxs.mydatabases.domain.models.QueryFileInfo
import com.sphynxs.mydatabases.domain.models.RootResolution
import com.sphynxs.mydatabases.domain.repositories.ConnectionRepository
import com.sphynxs.mydatabases.domain.usecases.queryfiles.ListQueryFilesUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * TDD tests for `QueryFilesViewModel` (change `query-files-storage`, Phase 10).
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QueryFilesViewModelTest {

    private lateinit var listQueryFilesUseCase: ListQueryFilesUseCase
    private lateinit var connectionRepository: ConnectionRepository
    private lateinit var queryStorageResolver: QueryStorageResolver
    private lateinit var viewModel: QueryFilesViewModel
    private val dispatcher = StandardTestDispatcher()

    private fun connectionConfig(type: DatabaseType) = mockk<ConnectionConfig>(relaxed = true) {
        every { this@mockk.type } returns type
    }

    @Before
    fun setup() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
        listQueryFilesUseCase = mockk()
        connectionRepository = mockk()
        queryStorageResolver = mockk()
        io.mockk.coEvery { queryStorageResolver.resolveRoot() } returns RootResolution.Resolved(mockk<DocumentFile>(relaxed = true))
        viewModel = QueryFilesViewModel(listQueryFilesUseCase, connectionRepository, queryStorageResolver)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `load with a populated folder sorts most-recent-first with name tiebreaker`() = runTest(dispatcher) {
        coEvery { connectionRepository.getById("c1") } returns connectionConfig(DatabaseType.MYSQL)
        val uri = mockk<Uri>(relaxed = true)
        val files = listOf(
            QueryFileInfo("old.sql", uri, 100L),
            QueryFileInfo("newer_b.sql", uri, 200L),
            QueryFileInfo("newer_a.sql", uri, 200L)
        )
        coEvery { listQueryFilesUseCase(DatabaseType.MYSQL) } returns Result.success(files)

        viewModel.load("c1")
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value as QueryFilesUiState.Success
        assertEquals(listOf("newer_a.sql", "newer_b.sql", "old.sql"), state.files.map { it.name })
    }

    @Test
    fun `load with an empty but available folder maps to a distinct Empty state, not Error`() = runTest(dispatcher) {
        coEvery { connectionRepository.getById("c1") } returns connectionConfig(DatabaseType.POSTGRESQL)
        coEvery { listQueryFilesUseCase(DatabaseType.POSTGRESQL) } returns Result.success(emptyList())

        viewModel.load("c1")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value is QueryFilesUiState.Empty)
    }

    @Test
    fun `store failure maps to Error with a message`() = runTest(dispatcher) {
        coEvery { connectionRepository.getById("c1") } returns connectionConfig(DatabaseType.SQLITE)
        coEvery { listQueryFilesUseCase(DatabaseType.SQLITE) } returns Result.failure(RuntimeException("disk error"))

        viewModel.load("c1")
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value as QueryFilesUiState.Error
        assertTrue(state.message.contains("disk error"))
    }

    @Test
    fun `engine-in-context scoping resolves DatabaseType via the connection and lists that engine`() = runTest(dispatcher) {
        coEvery { connectionRepository.getById("c2") } returns connectionConfig(DatabaseType.MARIADB)
        coEvery { listQueryFilesUseCase(DatabaseType.MARIADB) } returns Result.success(emptyList())

        viewModel.load("c2")
        dispatcher.scheduler.advanceUntilIdle()

        io.mockk.coVerify { listQueryFilesUseCase(DatabaseType.MARIADB) }
    }

    @Test
    fun `a Fallback resolution sets showFallbackNotice true on every load, not suppressed`() = runTest(dispatcher) {
        coEvery { connectionRepository.getById("c1") } returns connectionConfig(DatabaseType.MYSQL)
        coEvery { listQueryFilesUseCase(DatabaseType.MYSQL) } returns Result.success(emptyList())
        io.mockk.coEvery { queryStorageResolver.resolveRoot() } returns
            RootResolution.Fallback(mockk<DocumentFile>(relaxed = true), "SAF tree unavailable")

        viewModel.load("c1")
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.showFallbackNotice.value)

        viewModel.load("c1")
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.showFallbackNotice.value)
    }

    @Test
    fun `unknown connection id maps to Error without calling the use case`() = runTest(dispatcher) {
        coEvery { connectionRepository.getById("missing") } returns null

        viewModel.load("missing")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value is QueryFilesUiState.Error)
        io.mockk.coVerify(exactly = 0) { listQueryFilesUseCase(any()) }
    }
}
