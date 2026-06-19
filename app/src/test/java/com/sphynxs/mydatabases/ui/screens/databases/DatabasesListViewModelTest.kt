package com.sphynxs.mydatabases.ui.screens.databases

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.sphynxs.mydatabases.core.database.models.Database
import com.sphynxs.mydatabases.domain.usecases.GetDatabasesUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
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
 * Tests para DatabasesListViewModel.
 *
 * Verifica las transiciones de UiState en respuesta a invocaciones
 * del GetDatabasesUseCase.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DatabasesListViewModelTest {

    private lateinit var getDatabasesUseCase: GetDatabasesUseCase
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: DatabasesListViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getDatabasesUseCase = mockk()
        savedStateHandle = mockk()
        // Default: provide valid connectionId for all tests
        every { savedStateHandle.get<String>("connectionId") } returns "test-connection-id"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * RED Test #1: Cuando GetDatabasesUseCase retorna éxito,
     * el ViewModel debe emitir Success con la lista.
     */
    @Test
    fun `loadDatabases emite Success cuando el use case retorna lista no vacia`() = runTest {
        // GIVEN: use case retorna 2 databases
        val database1 = Database(name = "production", charset = "utf8mb4", collation = "utf8mb4_unicode_ci")
        val database2 = Database(name = "testing", charset = "utf8mb4", collation = "utf8mb4_unicode_ci")
        coEvery { getDatabasesUseCase() } returns Result.success(listOf(database1, database2))

        // WHEN: se crea el ViewModel y se dispara loadDatabases
        viewModel = DatabasesListViewModel(getDatabasesUseCase, savedStateHandle)
        viewModel.loadDatabases()
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN: estado debe ser Success con las 2 databases
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is DatabasesUiState.Success)
            assertEquals(2, (state as DatabasesUiState.Success).databases.size)
            assertEquals("production", state.databases[0].name)
            assertEquals("testing", state.databases[1].name)
        }
    }

    /**
     * RED Test #2 (TRIANGULATE): Cuando GetDatabasesUseCase retorna error,
     * el ViewModel debe emitir Error con el mensaje.
     */
    @Test
    fun `loadDatabases emite Error cuando el use case falla`() = runTest {
        // GIVEN: use case retorna fallo
        val errorMessage = "Connection lost"
        coEvery { getDatabasesUseCase() } returns Result.failure(Exception(errorMessage))

        // WHEN: se crea el ViewModel y se dispara loadDatabases
        viewModel = DatabasesListViewModel(getDatabasesUseCase, savedStateHandle)
        viewModel.loadDatabases()
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN: estado debe ser Error con el mensaje
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is DatabasesUiState.Error)
            assertEquals(errorMessage, (state as DatabasesUiState.Error).message)
        }
    }

    /**
     * RED Test #3 (TRIANGULATE): Estado inicial debe ser Loading.
     */
    @Test
    fun `estado inicial es Loading`() = runTest {
        // GIVEN: un ViewModel recién creado
        viewModel = DatabasesListViewModel(getDatabasesUseCase, savedStateHandle)

        // WHEN: se observa el estado inicial
        viewModel.uiState.test {
            // THEN: debe ser Loading
            val state = awaitItem()
            assertTrue(state is DatabasesUiState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * RED Test #4 (TRIANGULATE): Lista vacía debe emitir Success con lista vacía.
     */
    @Test
    fun `loadDatabases emite Success con lista vacia cuando no hay databases`() = runTest {
        // GIVEN: use case retorna lista vacía
        coEvery { getDatabasesUseCase() } returns Result.success(emptyList())

        // WHEN: se crea el ViewModel y se dispara loadDatabases
        viewModel = DatabasesListViewModel(getDatabasesUseCase, savedStateHandle)
        viewModel.loadDatabases()
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN: estado debe ser Success con lista vacía
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is DatabasesUiState.Success)
            assertEquals(0, (state as DatabasesUiState.Success).databases.size)
        }
    }

    /**
     * RED Test #5: ViewModel debe leer connectionId desde SavedStateHandle.
     * Spec: database-list-navigation "ViewModel loads databases for the navArg connectionId"
     */
    @Test
    fun `ViewModel lee connectionId desde SavedStateHandle`() = runTest {
        // GIVEN: SavedStateHandle con connectionId = "c-42"
        val savedStateHandle = mockk<SavedStateHandle>()
        every { savedStateHandle.get<String>("connectionId") } returns "c-42"
        coEvery { getDatabasesUseCase() } returns Result.success(emptyList())

        // WHEN: se crea el ViewModel con SavedStateHandle
        viewModel = DatabasesListViewModel(getDatabasesUseCase, savedStateHandle)

        // THEN: el ViewModel se inicializa sin error
        // (la implementación debe leer connectionId y no lanzar excepción)
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is DatabasesUiState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * RED Test #6 (TRIANGULATE): Missing connectionId debe fallar con IllegalStateException.
     * Spec: database-list-navigation "Missing connectionId fails loudly"
     */
    @Test(expected = IllegalStateException::class)
    fun `Missing connectionId en SavedStateHandle lanza IllegalStateException`() {
        // GIVEN: SavedStateHandle sin connectionId
        val savedStateHandle = mockk<SavedStateHandle>()
        every { savedStateHandle.get<String>("connectionId") } returns null

        // WHEN: se crea el ViewModel
        // THEN: debe lanzar IllegalStateException
        DatabasesListViewModel(getDatabasesUseCase, savedStateHandle)
    }
}
