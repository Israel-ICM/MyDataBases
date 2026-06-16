package com.sphynxs.mydatabases.ui.screens.tableviewer

import app.cash.turbine.test
import com.sphynxs.mydatabases.core.database.models.Column
import com.sphynxs.mydatabases.core.database.models.ColumnKey
import com.sphynxs.mydatabases.core.database.models.QueryResult
import com.sphynxs.mydatabases.domain.usecases.ExecuteQueryUseCase
import com.sphynxs.mydatabases.domain.usecases.GetColumnsUseCase
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
 * Tests para TableViewerViewModel.
 *
 * Verifica las transiciones de UiState en respuesta a invocaciones
 * de ExecuteQueryUseCase (rows) + GetColumnsUseCase (schema).
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TableViewerViewModelTest {

    private lateinit var executeQueryUseCase: ExecuteQueryUseCase
    private lateinit var getColumnsUseCase: GetColumnsUseCase
    private lateinit var viewModel: TableViewerViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        executeQueryUseCase = mockk()
        getColumnsUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * RED Test #1: Cuando ambos use cases retornan éxito con datos,
     * el ViewModel debe emitir Success con rows y columns.
     */
    @Test
    fun `loadTable emite Success cuando hay rows y columns`() = runTest {
        // GIVEN: use cases retornan datos
        val columns = listOf(
            Column(name = "id", type = "int", nullable = false, key = ColumnKey.PRIMARY),
            Column(name = "name", type = "varchar(255)", nullable = false, key = ColumnKey.NONE)
        )
        val rows = QueryResult(
            columns = listOf("id", "name"),
            rows = listOf(
                mapOf("id" to 1, "name" to "Alice"),
                mapOf("id" to 2, "name" to "Bob")
            ),
            rowCount = 2,
            executionTimeMs = 50
        )
        coEvery { getColumnsUseCase("production.users") } returns Result.success(columns)
        coEvery { executeQueryUseCase("SELECT * FROM production.users LIMIT 1000", emptyList()) } returns Result.success(rows)

        // WHEN: se crea el ViewModel y se dispara loadTable
        viewModel = TableViewerViewModel(executeQueryUseCase, getColumnsUseCase)
        viewModel.loadTable("production", "users")
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN: estado debe ser Success con rows y columns
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is TableViewerUiState.Success)
            assertEquals(2, (state as TableViewerUiState.Success).rows.rowCount)
            assertEquals(2, state.columns.size)
            assertEquals("id", state.columns[0].name)
        }
    }

    /**
     * RED Test #2 (TRIANGULATE): Cuando no hay rows pero sí columns,
     * el ViewModel debe emitir Empty con columns.
     */
    @Test
    fun `loadTable emite Empty cuando no hay rows pero si columns`() = runTest {
        // GIVEN: tabla vacía con schema
        val columns = listOf(
            Column(name = "id", type = "int", nullable = false, key = ColumnKey.PRIMARY)
        )
        val emptyRows = QueryResult(
            columns = listOf("id"),
            rows = emptyList(),
            rowCount = 0,
            executionTimeMs = 20
        )
        coEvery { getColumnsUseCase("test.empty_table") } returns Result.success(columns)
        coEvery { executeQueryUseCase("SELECT * FROM test.empty_table LIMIT 1000", emptyList()) } returns Result.success(emptyRows)

        // WHEN: se carga la tabla
        viewModel = TableViewerViewModel(executeQueryUseCase, getColumnsUseCase)
        viewModel.loadTable("test", "empty_table")
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN: estado debe ser Empty con columns
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is TableViewerUiState.Empty)
            assertEquals(1, (state as TableViewerUiState.Empty).columns.size)
        }
    }

    /**
     * RED Test #3 (TRIANGULATE): Cuando falla ExecuteQueryUseCase,
     * el ViewModel debe emitir Error.
     */
    @Test
    fun `loadTable emite Error cuando executeQuery falla`() = runTest {
        // GIVEN: query falla
        val errorMessage = "Table doesn't exist"
        coEvery { getColumnsUseCase("test.nonexistent") } returns Result.success(emptyList())
        coEvery { executeQueryUseCase("SELECT * FROM test.nonexistent LIMIT 1000", emptyList()) } returns Result.failure(Exception(errorMessage))

        // WHEN: se carga la tabla
        viewModel = TableViewerViewModel(executeQueryUseCase, getColumnsUseCase)
        viewModel.loadTable("test", "nonexistent")
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN: estado debe ser Error
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is TableViewerUiState.Error)
            assertEquals(errorMessage, (state as TableViewerUiState.Error).message)
        }
    }

    /**
     * RED Test #4 (TRIANGULATE): Estado inicial debe ser Loading.
     */
    @Test
    fun `estado inicial es Loading`() = runTest {
        // GIVEN: un ViewModel recién creado
        viewModel = TableViewerViewModel(executeQueryUseCase, getColumnsUseCase)

        // WHEN: se observa el estado inicial
        viewModel.uiState.test {
            // THEN: debe ser Loading
            val state = awaitItem()
            assertTrue(state is TableViewerUiState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
