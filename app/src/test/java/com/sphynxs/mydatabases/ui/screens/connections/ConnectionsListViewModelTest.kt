package com.sphynxs.mydatabases.ui.screens.connections

import app.cash.turbine.test
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.domain.usecases.connections.DeleteConnectionUseCase
import com.sphynxs.mydatabases.domain.usecases.connections.GetConnectionsUseCase
import com.sphynxs.mydatabases.domain.usecases.connections.TestConnectionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
 * Tests para ConnectionsListViewModel.
 *
 * Verifica las emisiones de UiState en respuesta a eventos
 * del usuario y cambios en el repositorio.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionsListViewModelTest {

    private lateinit var getConnectionsUseCase: GetConnectionsUseCase
    private lateinit var deleteConnectionUseCase: DeleteConnectionUseCase
    private lateinit var testConnectionUseCase: TestConnectionUseCase
    private lateinit var viewModel: ConnectionsListViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getConnectionsUseCase = mockk()
        deleteConnectionUseCase = mockk(relaxed = true)
        testConnectionUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * RED Test: Cuando el repositorio emite una lista de conexiones,
     * el ViewModel debe emitir Success con esa lista.
     */
    @Test
    fun `getConnections emite Success cuando el repositorio devuelve lista no vacia`() = runTest {
        // GIVEN: repositorio con 2 conexiones
        val connection1 = ConnectionConfig(
            id = "1",
            name = "Producción",
            type = DatabaseType.MYSQL,
            host = "db.example.com",
            port = 3306,
            database = "mydb",
            username = "admin",
            password = "secret"
        )
        val connection2 = ConnectionConfig(
            id = "2",
            name = "Dev",
            type = DatabaseType.MARIADB,
            host = "localhost",
            port = 3307,
            database = "test",
            username = "dev",
            password = "dev123"
        )
        every { getConnectionsUseCase() } returns flowOf(listOf(connection1, connection2))

        // WHEN: se crea el ViewModel
        viewModel = ConnectionsListViewModel(
            getConnectionsUseCase,
            deleteConnectionUseCase,
            testConnectionUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN: estado debe ser Success con las 2 conexiones
        viewModel.uiState.test {
            // Skip Loading initial state
            val loading = awaitItem()
            assertTrue(loading is ConnectionsUiState.Loading)
            
            val state = awaitItem() as ConnectionsUiState.Success
            assertEquals(2, state.connections.size)
            assertEquals("Producción", state.connections[0].name)
            assertEquals("Dev", state.connections[1].name)
        }
    }

    /**
     * TRIANGULATE: Caso con lista vacía también debe emitir Success.
     */
    @Test
    fun `getConnections emite Success con lista vacia cuando no hay conexiones`() = runTest {
        // GIVEN: repositorio vacío
        every { getConnectionsUseCase() } returns flowOf(emptyList())

        // WHEN
        viewModel = ConnectionsListViewModel(
            getConnectionsUseCase,
            deleteConnectionUseCase,
            testConnectionUseCase
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN: Success con lista vacía
        viewModel.uiState.test {
            // Skip Loading initial state
            val loading = awaitItem()
            assertTrue(loading is ConnectionsUiState.Loading)
            
            val state = awaitItem() as ConnectionsUiState.Success
            assertEquals(0, state.connections.size)
        }
    }

    /**
     * TRIANGULATE: Caso delete — debe llamar al use case correcto.
     */
    @Test
    fun `deleteConnection llama al DeleteConnectionUseCase con el id correcto`() = runTest {
        // GIVEN
        every { getConnectionsUseCase() } returns flowOf(emptyList())
        viewModel = ConnectionsListViewModel(
            getConnectionsUseCase,
            deleteConnectionUseCase,
            testConnectionUseCase
        )

        // WHEN
        viewModel.deleteConnection("42")
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN
        coVerify(exactly = 1) { deleteConnectionUseCase("42") }
    }

    /**
     * TRIANGULATE: Test connection exitoso debe devolver Result.success.
     */
    @Test
    fun `testConnection devuelve success cuando el use case retorna Result success`() = runTest {
        // GIVEN
        val config = ConnectionConfig(
            id = "99",
            name = "Test",
            type = DatabaseType.MYSQL,
            host = "test.local",
            port = 3306,
            database = "db",
            username = "user",
            password = "pass"
        )
        coEvery { testConnectionUseCase(config) } returns Result.success(Unit)
        every { getConnectionsUseCase() } returns flowOf(emptyList())

        viewModel = ConnectionsListViewModel(
            getConnectionsUseCase,
            deleteConnectionUseCase,
            testConnectionUseCase
        )

        // WHEN
        val result = viewModel.testConnection(config)

        // THEN
        assertTrue(result.isSuccess)
    }

    /**
     * TRIANGULATE: Test connection fallido debe devolver Result.failure.
     */
    @Test
    fun `testConnection devuelve failure cuando el use case retorna Result failure`() = runTest {
        // GIVEN
        val config = ConnectionConfig(
            id = "98",
            name = "BadHost",
            type = DatabaseType.MYSQL,
            host = "invalid.host",
            port = 9999,
            database = "db",
            username = "user",
            password = "pass"
        )
        val error = RuntimeException("Connection refused")
        coEvery { testConnectionUseCase(config) } returns Result.failure(error)
        every { getConnectionsUseCase() } returns flowOf(emptyList())

        viewModel = ConnectionsListViewModel(
            getConnectionsUseCase,
            deleteConnectionUseCase,
            testConnectionUseCase
        )

        // WHEN
        val result = viewModel.testConnection(config)

        // THEN
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
