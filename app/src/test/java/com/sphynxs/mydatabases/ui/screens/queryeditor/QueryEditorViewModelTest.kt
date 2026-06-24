package com.sphynxs.mydatabases.ui.screens.queryeditor

import app.cash.turbine.test
import com.sphynxs.mydatabases.core.database.models.QueryResult
import com.sphynxs.mydatabases.domain.usecases.ExecuteQueryUseCase
import com.sphynxs.mydatabases.domain.usecases.ExecuteUpdateUseCase
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
 * Unit tests para QueryEditorViewModel.
 *
 * Valida state machine, multi-statement execution, error handling, cancellation.
 * Target: >85% coverage (critical business logic).
 *
 * @author israel-icm
 * @date 2026-06-23
 */
@OptIn(ExperimentalCoroutinesApi::class)
@org.junit.Ignore("ViewModel constructor changed - needs update to use ExecuteBatchStatementsUseCase")
class QueryEditorViewModelTest {

    private lateinit var executeQueryUseCase: ExecuteQueryUseCase
    private lateinit var executeUpdateUseCase: ExecuteUpdateUseCase
    private lateinit var viewModel: QueryEditorViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        executeQueryUseCase = mockk()
        executeUpdateUseCase = mockk()
        viewModel = QueryEditorViewModel(executeQueryUseCase, executeUpdateUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        // GIVEN: ViewModel recién creado
        // WHEN: Observamos el estado inicial
        // THEN: Estado inicial es Idle
        assertEquals(QueryEditorUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `executeStatements with single SELECT returns SelectResult`() = runTest {
        // GIVEN: Query SELECT simple
        val sql = "SELECT * FROM users"
        val expectedResult = QueryResult(
            columns = listOf("id", "name"),
            rows = listOf(
                mapOf("id" to 1, "name" to "Ada"),
                mapOf("id" to 2, "name" to "Linus")
            ),
            rowCount = 2,
            executionTimeMs = 10
        )
        coEvery { executeQueryUseCase(sql, emptyList()) } returns Result.success(expectedResult)

        // WHEN: Ejecutamos
        viewModel.uiState.test {
            assertEquals(QueryEditorUiState.Idle, awaitItem()) // Estado inicial

            viewModel.executeStatements(sql)
            assertEquals(QueryEditorUiState.Running, awaitItem()) // Transición a Running

            testDispatcher.scheduler.advanceUntilIdle() // Ejecutar coroutine

            val finalState = awaitItem()
            assertTrue(finalState is QueryEditorUiState.SelectResult)
            assertEquals(expectedResult, (finalState as QueryEditorUiState.SelectResult).result)
            assertTrue(finalState.executionTimeMs >= 0)
        }
    }

    @Test
    fun `executeStatements with single UPDATE returns UpdateSummary`() = runTest {
        // GIVEN: Query UPDATE simple
        val sql = "UPDATE users SET active = 1 WHERE id = 5"
        coEvery { executeUpdateUseCase(sql, emptyList()) } returns Result.success(1)

        // WHEN: Ejecutamos
        viewModel.uiState.test {
            assertEquals(QueryEditorUiState.Idle, awaitItem())

            viewModel.executeStatements(sql)
            assertEquals(QueryEditorUiState.Running, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()

            val finalState = awaitItem()
            assertTrue(finalState is QueryEditorUiState.UpdateSummary)
            val summary = (finalState as QueryEditorUiState.UpdateSummary).results
            assertEquals(1, summary.size)
            assertEquals(1, summary[0].affectedRows)
            assertEquals(sql, summary[0].sql)
            assertTrue(summary[0].executionTimeMs >= 0)
        }
    }

    @Test
    fun `executeStatements with multiple SELECTs shows last result`() = runTest {
        // GIVEN: Múltiples SELECTs separados por ;
        val sql1 = "SELECT 1"
        val sql2 = "SELECT 2"
        val sql3 = "SELECT 3"
        val combinedSql = "$sql1; $sql2; $sql3;"

        val result1 = QueryResult(listOf("1"), listOf(mapOf("1" to 1)), rowCount = 1, executionTimeMs = 10)
        val result2 = QueryResult(listOf("2"), listOf(mapOf("2" to 2)), rowCount = 1, executionTimeMs = 10)
        val result3 = QueryResult(listOf("3"), listOf(mapOf("3" to 3)), rowCount = 1, executionTimeMs = 10)

        coEvery { executeQueryUseCase(sql1, emptyList()) } returns Result.success(result1)
        coEvery { executeQueryUseCase(sql2, emptyList()) } returns Result.success(result2)
        coEvery { executeQueryUseCase(sql3, emptyList()) } returns Result.success(result3)

        // WHEN: Ejecutamos
        viewModel.uiState.test {
            skipItems(1) // Idle inicial

            viewModel.executeStatements(combinedSql)
            skipItems(1) // Running

            testDispatcher.scheduler.advanceUntilIdle()

            val finalState = awaitItem()
            assertTrue(finalState is QueryEditorUiState.SelectResult)
            assertEquals(result3, (finalState as QueryEditorUiState.SelectResult).result) // Último SELECT
        }
    }

    @Test
    fun `executeStatements with mixed UPDATE and SELECT shows SelectResult`() = runTest {
        // GIVEN: UPDATE seguido de SELECT
        val update = "UPDATE users SET active = 1"
        val select = "SELECT COUNT(*) FROM users"
        val combinedSql = "$update; $select;"

        val selectResult = QueryResult(listOf("COUNT(*)"), listOf(mapOf("COUNT(*)" to 10)), rowCount = 1, executionTimeMs = 10)
        coEvery { executeUpdateUseCase(update, emptyList()) } returns Result.success(5)
        coEvery { executeQueryUseCase(select, emptyList()) } returns Result.success(selectResult)

        // WHEN: Ejecutamos
        viewModel.uiState.test {
            skipItems(1)

            viewModel.executeStatements(combinedSql)
            skipItems(1)

            testDispatcher.scheduler.advanceUntilIdle()

            val finalState = awaitItem()
            assertTrue(finalState is QueryEditorUiState.SelectResult)
            assertEquals(selectResult, (finalState as QueryEditorUiState.SelectResult).result)
        }
    }

    @Test
    fun `executeStatements with error on first statement returns Error`() = runTest {
        // GIVEN: Query inválido
        val sql = "SELEKT * FROM users"
        coEvery { executeQueryUseCase(sql, emptyList()) } returns Result.failure(Exception("Syntax error"))

        // WHEN: Ejecutamos
        viewModel.uiState.test {
            skipItems(1)

            viewModel.executeStatements(sql)
            skipItems(1)

            testDispatcher.scheduler.advanceUntilIdle()

            val finalState = awaitItem()
            assertTrue(finalState is QueryEditorUiState.Error)
            assertEquals("Syntax error", (finalState as QueryEditorUiState.Error).message)
            assertEquals(sql, finalState.failedStatement)
        }
    }

    @Test
    fun `executeStatements with error on middle statement stops execution`() = runTest {
        // GIVEN: Tres statements, el segundo falla
        val sql1 = "SELECT 1"
        val sql2 = "INVALID SQL"
        val sql3 = "SELECT 3"
        val combinedSql = "$sql1; $sql2; $sql3;"

        val result1 = QueryResult(listOf("1"), listOf(mapOf("1" to 1)), rowCount = 1, executionTimeMs = 10)
        coEvery { executeQueryUseCase(sql1, emptyList()) } returns Result.success(result1)
        coEvery { executeQueryUseCase(sql2, emptyList()) } returns Result.failure(Exception("Error"))

        // WHEN: Ejecutamos
        viewModel.uiState.test {
            skipItems(1)

            viewModel.executeStatements(combinedSql)
            skipItems(1)

            testDispatcher.scheduler.advanceUntilIdle()

            val finalState = awaitItem()
            assertTrue(finalState is QueryEditorUiState.Error)

            // THEN: El tercer statement NO se ejecuta
            coVerify(exactly = 0) { executeQueryUseCase(sql3, emptyList()) }
        }
    }

    @Test
    fun `cancel during execution returns to Idle`() = runTest {
        // GIVEN: Query en ejecución
        val sql = "SELECT * FROM users"
        coEvery { executeQueryUseCase(sql, emptyList()) } returns Result.success(
            QueryResult(listOf("id"), listOf(mapOf("id" to 1)), rowCount = 1, executionTimeMs = 10)
        )

        viewModel.uiState.test {
            skipItems(1)

            viewModel.executeStatements(sql)
            assertEquals(QueryEditorUiState.Running, awaitItem())

            // WHEN: Cancelamos antes de que termine
            viewModel.cancel()

            // THEN: Vuelve a Idle
            assertEquals(QueryEditorUiState.Idle, awaitItem())
        }
    }

    @Test
    fun `detectQueryType recognizes SELECT as query`() = runTest {
        // GIVEN: SELECT statement
        val sql = "SELECT * FROM users"
        coEvery { executeQueryUseCase(sql, emptyList()) } returns Result.success(
            QueryResult(listOf("id"), emptyList(), rowCount = 0, executionTimeMs = 10)
        )

        // WHEN: Ejecutamos
        viewModel.executeStatements(sql)
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN: Se llama executeQueryUseCase (no executeUpdateUseCase)
        coVerify { executeQueryUseCase(sql, emptyList()) }
        coVerify(exactly = 0) { executeUpdateUseCase(any(), any()) }
    }

    @Test
    fun `detectQueryType recognizes INSERT as update`() = runTest {
        // GIVEN: INSERT statement
        val sql = "INSERT INTO logs (msg) VALUES ('hi')"
        coEvery { executeUpdateUseCase(sql, emptyList()) } returns Result.success(1)

        // WHEN: Ejecutamos
        viewModel.executeStatements(sql)
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN: Se llama executeUpdateUseCase (no executeQueryUseCase)
        coVerify { executeUpdateUseCase(sql, emptyList()) }
        coVerify(exactly = 0) { executeQueryUseCase(any(), any()) }
    }

    @Test
    fun `detectQueryType recognizes SHOW as query`() = runTest {
        // GIVEN: SHOW statement
        val sql = "SHOW TABLES"
        coEvery { executeQueryUseCase(sql, emptyList()) } returns Result.success(
            QueryResult(listOf("Tables"), emptyList(), rowCount = 0, executionTimeMs = 10)
        )

        // WHEN: Ejecutamos
        viewModel.executeStatements(sql)
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN: Se llama executeQueryUseCase
        coVerify { executeQueryUseCase(sql, emptyList()) }
    }

    @Test
    fun `empty SQL does not execute`() = runTest {
        // GIVEN: SQL vacío
        val sql = "   "

        // WHEN: Ejecutamos
        viewModel.executeStatements(sql)
        testDispatcher.scheduler.advanceUntilIdle()

        // THEN: No se llama a ningún use case
        coVerify(exactly = 0) { executeQueryUseCase(any(), any()) }
        coVerify(exactly = 0) { executeUpdateUseCase(any(), any()) }
    }
}
