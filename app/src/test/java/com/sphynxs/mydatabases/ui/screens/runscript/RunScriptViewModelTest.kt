package com.sphynxs.mydatabases.ui.screens.runscript

import com.sphynxs.mydatabases.core.database.models.DatabaseError
import com.sphynxs.mydatabases.domain.sql.RiskCategory
import com.sphynxs.mydatabases.domain.sql.RiskReport
import com.sphynxs.mydatabases.domain.sql.ScriptError
import com.sphynxs.mydatabases.domain.sql.ScriptExecutionProgress
import com.sphynxs.mydatabases.domain.sql.ScriptExecutionSummary
import com.sphynxs.mydatabases.domain.usecases.ExecuteScriptUseCase
import com.sphynxs.mydatabases.domain.usecases.ExecutionEvent
import com.sphynxs.mydatabases.domain.usecases.PreScanEvent
import com.sphynxs.mydatabases.domain.usecases.PreScanScriptUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
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
import java.io.StringReader

/**
 * TDD tests for `RunScriptViewModel` state machine (change `large-sql-script-execution`, Phase 9).
 *
 * Both use cases are mocked to control exactly which `PreScanEvent`/`ExecutionEvent` sequence
 * the ViewModel observes, isolating the state-machine transition logic under test.
 *
 * Assertions read `viewModel.state.value` directly at checkpoints rather than collecting into a
 * list — `StateFlow` only guarantees observers see the LATEST value, not every intermediate one,
 * so a separate "spy" collector on the same `StandardTestDispatcher` can miss fast, non-suspending
 * transitions entirely (observed directly: a first draft of this test asserted on every
 * micro-transition and failed intermittently for exactly this reason).
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-04
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RunScriptViewModelTest {

    private lateinit var preScanScriptUseCase: PreScanScriptUseCase
    private lateinit var executeScriptUseCase: ExecuteScriptUseCase
    private lateinit var viewModel: RunScriptViewModel
    private val dispatcher = StandardTestDispatcher()

    private fun dummyReader() = StringReader("SELECT 1;")

    @Before
    fun setup() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
        preScanScriptUseCase = mockk()
        executeScriptUseCase = mockk()
        viewModel = RunScriptViewModel(preScanScriptUseCase, executeScriptUseCase)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    private fun riskyReport() = RiskReport(
        totalStatements = 2,
        counts = mapOf(RiskCategory.DDL to 1, RiskCategory.DELETE to 0, RiskCategory.UPDATE_NO_WHERE to 0),
        lineNumbers = mapOf(RiskCategory.DDL to listOf(1))
    )

    private fun cleanReport() = RiskReport(
        totalStatements = 2,
        counts = mapOf(RiskCategory.DDL to 0, RiskCategory.DELETE to 0, RiskCategory.UPDATE_NO_WHERE to 0),
        lineNumbers = emptyMap()
    )

    @Test
    fun `full state sequence for a risky script goes through AwaitingConfirmation then Success`() = runTest(dispatcher) {
        every { preScanScriptUseCase(any()) } returns flowOf(
            PreScanEvent.Progress(1, 1),
            PreScanEvent.Completed(riskyReport())
        )
        every { executeScriptUseCase(any()) } returns flowOf(
            ExecutionEvent.Progress(ScriptExecutionProgress(0, 1, null)),
            ExecutionEvent.Completed(ScriptExecutionSummary(2, null, 0L))
        )

        assertTrue(viewModel.state.value is RunScriptState.Idle)

        viewModel.runScript { dummyReader() }
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value is RunScriptState.AwaitingConfirmation)

        viewModel.confirm()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value is RunScriptState.Success)
        assertEquals(2, (viewModel.state.value as RunScriptState.Success).summary.statementsExecuted)
    }

    @Test
    fun `clean script auto-skips AwaitingConfirmation`() = runTest(dispatcher) {
        every { preScanScriptUseCase(any()) } returns flowOf(PreScanEvent.Completed(cleanReport()))
        every { executeScriptUseCase(any()) } returns flowOf(
            ExecutionEvent.Completed(ScriptExecutionSummary(2, null, 0L))
        )

        viewModel.runScript { dummyReader() }
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value is RunScriptState.Success)
    }

    @Test
    fun `decline returns to Idle with zero execution`() = runTest(dispatcher) {
        every { preScanScriptUseCase(any()) } returns flowOf(PreScanEvent.Completed(riskyReport()))

        viewModel.runScript { dummyReader() }
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value is RunScriptState.AwaitingConfirmation)

        viewModel.decline()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value is RunScriptState.Idle)
        // executeScriptUseCase must never have been called — no `every {}` was registered for
        // it in this test, so MockK would throw on any unexpected invocation attempt.
    }

    @Test
    fun `cancel during execution yields Cancelled`() = runTest(dispatcher) {
        every { preScanScriptUseCase(any()) } returns flowOf(PreScanEvent.Completed(cleanReport()))
        every { executeScriptUseCase(any()) } returns flow {
            emit(ExecutionEvent.Progress(ScriptExecutionProgress(0, 1, null)))
            delay(10_000) // never resumes before cancel() fires
            emit(ExecutionEvent.Completed(ScriptExecutionSummary(1, null, 0L)))
        }

        viewModel.runScript { dummyReader() }
        dispatcher.scheduler.runCurrent()

        viewModel.cancel()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value is RunScriptState.Cancelled)
    }

    @Test
    fun `ConnectionFailed maps to a descriptive Error`() = runTest(dispatcher) {
        every { preScanScriptUseCase(any()) } returns flowOf(PreScanEvent.Completed(cleanReport()))
        every { executeScriptUseCase(any()) } returns flowOf(
            ExecutionEvent.Error(DatabaseError.ConnectionFailed("No conectado"))
        )

        viewModel.runScript { dummyReader() }
        dispatcher.scheduler.advanceUntilIdle()

        val error = viewModel.state.value as RunScriptState.Error
        assertTrue(error.message.contains("No conectado"))
    }

    @Test
    fun `QueryExecutionFailed with stopped-at context maps to a descriptive Error`() = runTest(dispatcher) {
        every { preScanScriptUseCase(any()) } returns flowOf(PreScanEvent.Completed(cleanReport()))
        every { executeScriptUseCase(any()) } returns flowOf(
            ExecutionEvent.Error(
                DatabaseError.QueryExecutionFailed(
                    query = "statement #3",
                    reason = "Stopped at statement 3 (line 12): syntax error"
                )
            )
        )

        viewModel.runScript { dummyReader() }
        dispatcher.scheduler.advanceUntilIdle()

        val error = viewModel.state.value as RunScriptState.Error
        assertTrue(error.message.contains("Stopped at statement 3"))
    }

    @Test
    fun `malformed DELIMITER ScriptError maps to a descriptive Error during pre-scan`() = runTest(dispatcher) {
        every { preScanScriptUseCase(any()) } returns flowOf(
            PreScanEvent.Error(ScriptError.MalformedDelimiterDirective(7))
        )

        viewModel.runScript { dummyReader() }
        dispatcher.scheduler.advanceUntilIdle()

        val error = viewModel.state.value as RunScriptState.Error
        assertTrue(error.message.contains("7"))
    }

    // Note: "no active connection" produces the exact same `DatabaseError.ConnectionFailed`
    // as any other connection-lost case (see `DatabaseRepositoryImpl`'s
    // `?: Result.failure(DatabaseError.ConnectionFailed("No conectado"))` fallback) — the
    // `ConnectionFailed maps to a descriptive Error` test above already covers this exact path;
    // a separate test would just duplicate it.
}
