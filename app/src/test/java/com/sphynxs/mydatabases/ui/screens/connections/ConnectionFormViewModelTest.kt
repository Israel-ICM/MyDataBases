package com.sphynxs.mydatabases.ui.screens.connections

import app.cash.turbine.test
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.domain.usecases.connections.GetConnectionByIdUseCase
import com.sphynxs.mydatabases.domain.usecases.connections.SaveConnectionUseCase
import com.sphynxs.mydatabases.domain.usecases.connections.TestConnectionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
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
 * Tests para ConnectionFormViewModel.
 *
 * Verifica las emisiones de UiState en respuesta a eventos
 * del formulario (guardar, probar conexión, editar).
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionFormViewModelTest {

    private lateinit var saveConnectionUseCase: SaveConnectionUseCase
    private lateinit var getConnectionByIdUseCase: GetConnectionByIdUseCase
    private lateinit var testConnectionUseCase: TestConnectionUseCase
    private lateinit var viewModel: ConnectionFormViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        saveConnectionUseCase = mockk(relaxed = true)
        getConnectionByIdUseCase = mockk()
        testConnectionUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * RED Test: saveConnection debe emitir Saving → Saved cuando el use case completa.
     */
    @Test
    fun `saveConnection emite Saving y luego Saved cuando se guarda exitosamente`() = runTest {
        // GIVEN
        val config = ConnectionConfig(
            id = "new-123",
            name = "Nueva conexión",
            type = DatabaseType.MYSQL,
            host = "localhost",
            port = 3306,
            database = "testdb",
            username = "user",
            password = "pass"
        )
        coEvery { saveConnectionUseCase(config) } returns Unit

        viewModel = ConnectionFormViewModel(
            saveConnectionUseCase,
            getConnectionByIdUseCase,
            testConnectionUseCase
        )

        // WHEN
        viewModel.saveConnection(config)
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN: debe emitir Saving → Saved
        viewModel.formState.test {
            val state = awaitItem()
            assertTrue(state is ConnectionFormUiState.Saved)
        }
        coVerify(exactly = 1) { saveConnectionUseCase(config) }
    }

    /**
     * TRIANGULATE: Caso con error — debe emitir Error.
     */
    @Test
    fun `saveConnection emite Error cuando el use case falla`() = runTest {
        // GIVEN
        val config = ConnectionConfig(
            id = "bad-123",
            name = "Mala conexión",
            type = DatabaseType.MARIADB,
            host = "badhost",
            port = 9999,
            database = "db",
            username = "user",
            password = "pass"
        )
        val error = RuntimeException("Database write failed")
        coEvery { saveConnectionUseCase(config) } throws error

        viewModel = ConnectionFormViewModel(
            saveConnectionUseCase,
            getConnectionByIdUseCase,
            testConnectionUseCase
        )

        // WHEN
        viewModel.saveConnection(config)
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN: debe emitir Error
        viewModel.formState.test {
            val state = awaitItem() as ConnectionFormUiState.Error
            assertEquals("Database write failed", state.message)
        }
    }

    /**
     * TRIANGULATE: testConnection success debe emitir Testing → Success.
     */
    @Test
    fun `testConnection emite Testing y luego Success cuando la conexion es valida`() = runTest {
        // GIVEN
        val config = ConnectionConfig(
            id = "test-123",
            name = "Test",
            type = DatabaseType.MYSQL,
            host = "valid.host",
            port = 3306,
            database = "db",
            username = "user",
            password = "pass"
        )
        coEvery { testConnectionUseCase(config) } returns Result.success(Unit)

        viewModel = ConnectionFormViewModel(
            saveConnectionUseCase,
            getConnectionByIdUseCase,
            testConnectionUseCase
        )

        // WHEN
        viewModel.testConnection(config)
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN: debe emitir Testing → Success
        viewModel.testState.test {
            val state = awaitItem()
            assertTrue(state is ConnectionTestUiState.Success)
        }
    }

    /**
     * TRIANGULATE: testConnection failure debe emitir Testing → Error.
     */
    @Test
    fun `testConnection emite Testing y luego Error cuando la conexion falla`() = runTest {
        // GIVEN
        val config = ConnectionConfig(
            id = "test-fail",
            name = "Test",
            type = DatabaseType.MYSQL,
            host = "invalid.host",
            port = 8888,
            database = "db",
            username = "user",
            password = "pass"
        )
        val error = RuntimeException("Connection refused")
        coEvery { testConnectionUseCase(config) } returns Result.failure(error)

        viewModel = ConnectionFormViewModel(
            saveConnectionUseCase,
            getConnectionByIdUseCase,
            testConnectionUseCase
        )

        // WHEN
        viewModel.testConnection(config)
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN: debe emitir Testing → Error
        viewModel.testState.test {
            val state = awaitItem() as ConnectionTestUiState.Error
            assertEquals("Connection refused", state.message)
        }
    }

    /**
     * TRIANGULATE: loadConnection con ID existente debe retornar el config.
     */
    @Test
    fun `loadConnection devuelve la configuracion cuando existe el ID`() = runTest {
        // GIVEN
        val existingConfig = ConnectionConfig(
            id = "42",
            name = "Producción",
            type = DatabaseType.MYSQL,
            host = "prod.db",
            port = 3306,
            database = "proddb",
            username = "admin",
            password = "secret"
        )
        coEvery { getConnectionByIdUseCase("42") } returns existingConfig

        viewModel = ConnectionFormViewModel(
            saveConnectionUseCase,
            getConnectionByIdUseCase,
            testConnectionUseCase
        )

        // WHEN
        val result = viewModel.loadConnection("42")

        // THEN
        assertEquals(existingConfig, result)
        coVerify(exactly = 1) { getConnectionByIdUseCase("42") }
    }

    /**
     * TRIANGULATE: loadConnection con ID inexistente debe retornar null.
     */
    @Test
    fun `loadConnection devuelve null cuando no existe el ID`() = runTest {
        // GIVEN
        coEvery { getConnectionByIdUseCase("999") } returns null

        viewModel = ConnectionFormViewModel(
            saveConnectionUseCase,
            getConnectionByIdUseCase,
            testConnectionUseCase
        )

        // WHEN
        val result = viewModel.loadConnection("999")

        // THEN
        assertEquals(null, result)
        coVerify(exactly = 1) { getConnectionByIdUseCase("999") }
    }
}
