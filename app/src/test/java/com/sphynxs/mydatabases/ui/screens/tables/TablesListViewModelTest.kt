package com.sphynxs.mydatabases.ui.screens.tables

import app.cash.turbine.test
import com.sphynxs.mydatabases.core.database.models.Table
import com.sphynxs.mydatabases.core.database.models.TableType
import com.sphynxs.mydatabases.domain.usecases.GetTablesUseCase
import io.mockk.coEvery
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
 * Tests para TablesListViewModel.
 *
 * Verifica las transiciones de UiState en respuesta a invocaciones
 * del GetTablesUseCase.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TablesListViewModelTest {

    private lateinit var getTablesUseCase: GetTablesUseCase
    private lateinit var viewModel: TablesListViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getTablesUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * RED Test #1: Cuando GetTablesUseCase retorna éxito con tablas,
     * el ViewModel debe emitir Success.
     */
    @Test
    fun `loadTables emite Success cuando el use case retorna lista no vacia`() = runTest {
        // GIVEN: use case retorna 2 tables
        val table1 = Table(name = "users", database = "production", type = TableType.TABLE, engine = "InnoDB")
        val table2 = Table(name = "orders", database = "production", type = TableType.TABLE, engine = "InnoDB")
        coEvery { getTablesUseCase("production") } returns Result.success(listOf(table1, table2))

        // WHEN: se crea el ViewModel y se dispara loadTables
        viewModel = TablesListViewModel(getTablesUseCase)
        viewModel.loadTables("production")
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN: estado debe ser Success con las 2 tables
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is TablesUiState.Success)
            assertEquals(2, (state as TablesUiState.Success).tables.size)
            assertEquals("users", state.tables[0].name)
            assertEquals("orders", state.tables[1].name)
        }
    }

    /**
     * RED Test #2 (TRIANGULATE): Cuando GetTablesUseCase retorna error,
     * el ViewModel debe emitir Error.
     */
    @Test
    fun `loadTables emite Error cuando el use case falla`() = runTest {
        // GIVEN: use case retorna fallo
        val errorMessage = "Permission denied"
        coEvery { getTablesUseCase("production") } returns Result.failure(Exception(errorMessage))

        // WHEN: se crea el ViewModel y se dispara loadTables
        viewModel = TablesListViewModel(getTablesUseCase)
        viewModel.loadTables("production")
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN: estado debe ser Error
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is TablesUiState.Error)
            assertEquals(errorMessage, (state as TablesUiState.Error).message)
        }
    }

    /**
     * RED Test #3 (TRIANGULATE): Estado inicial debe ser Loading.
     */
    @Test
    fun `estado inicial es Loading`() = runTest {
        // GIVEN: un ViewModel recién creado
        viewModel = TablesListViewModel(getTablesUseCase)

        // WHEN: se observa el estado inicial
        viewModel.uiState.test {
            // THEN: debe ser Loading
            val state = awaitItem()
            assertTrue(state is TablesUiState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * RED Test #4 (TRIANGULATE): Lista vacía debe emitir Empty.
     */
    @Test
    fun `loadTables emite Empty cuando no hay tables`() = runTest {
        // GIVEN: use case retorna lista vacía
        coEvery { getTablesUseCase("empty_db") } returns Result.success(emptyList())

        // WHEN: se crea el ViewModel y se dispara loadTables
        viewModel = TablesListViewModel(getTablesUseCase)
        viewModel.loadTables("empty_db")
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN: estado debe ser Empty
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is TablesUiState.Empty)
        }
    }
}
