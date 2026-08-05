package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import com.sphynxs.mydatabases.domain.sql.ScriptError
import com.sphynxs.mydatabases.domain.sql.ScriptExecutionProgress
import com.sphynxs.mydatabases.domain.sql.ScriptExecutionSummary
import com.sphynxs.mydatabases.domain.sql.ScriptStatement
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.StringReader

/**
 * TDD tests for `ExecuteScriptUseCase` (change `large-sql-script-execution`, Phase 8).
 *
 * Repository is mocked; the splitter itself is exercised for real (via a genuine `StringReader`)
 * to prove `ScriptError`s from the re-split propagate correctly through the use case.
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-04
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExecuteScriptUseCaseTest {

    private lateinit var repository: DatabaseRepository
    private lateinit var useCase: ExecuteScriptUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = ExecuteScriptUseCase(repository)
    }

    @Test
    fun `progress events are emitted in statement order and Completed follows`() = runTest {
        coEvery { repository.executeScript(any(), any()) } coAnswers {
            val statements = firstArg<Flow<ScriptStatement>>()
            val onProgress = secondArg<suspend (ScriptExecutionProgress) -> Unit>()
            var index = 0
            statements.collect { stmt ->
                onProgress(ScriptExecutionProgress(index, stmt.lineNumber, null))
                index++
            }
            Result.success(ScriptExecutionSummary(index, null, 0L))
        }

        val sql = "INSERT INTO t VALUES (1);\nINSERT INTO t VALUES (2);"
        val events = useCase(StringReader(sql)).toList()

        val progressEvents = events.filterIsInstance<ExecutionEvent.Progress>()
        assertEquals(2, progressEvents.size)
        assertEquals(0, progressEvents[0].progress.statementIndex)
        assertEquals(1, progressEvents[1].progress.statementIndex)

        val completed = events.filterIsInstance<ExecutionEvent.Completed>().single()
        assertEquals(2, completed.summary.statementsExecuted)
        assertTrue(events.last() is ExecutionEvent.Completed)
    }

    @Test
    fun `repository failure surfaces as ExecutionEvent Error`() = runTest {
        val failure = com.sphynxs.mydatabases.core.database.models.DatabaseError.QueryExecutionFailed(
            query = "statement #1",
            reason = "Stopped at statement 1 (line 1): syntax error"
        )
        coEvery { repository.executeScript(any(), any()) } returns Result.failure(failure)

        val events = useCase(StringReader("SELECT 1;")).toList()

        val error = events.filterIsInstance<ExecutionEvent.Error>().single()
        assertEquals(failure, error.error)
        assertTrue(events.none { it is ExecutionEvent.Completed })
    }

    @Test
    fun `ScriptError from the re-split propagates as Error`() = runTest {
        coEvery { repository.executeScript(any(), any()) } coAnswers {
            val statements = firstArg<Flow<ScriptStatement>>()
            statements.collect { }
            Result.success(ScriptExecutionSummary(0, null, 0L))
        }

        val sql = "SELECT 1;\nDELIMITER\nSELECT 2;"
        val events = useCase(StringReader(sql)).toList()

        val error = events.filterIsInstance<ExecutionEvent.Error>().single()
        assertTrue(error.error is ScriptError.MalformedDelimiterDirective)
        assertTrue(events.none { it is ExecutionEvent.Completed })
    }

    @Test
    fun `cancellation during collection does not emit a false Completed`() = runTest {
        coEvery { repository.executeScript(any(), any()) } coAnswers {
            val statements = firstArg<Flow<ScriptStatement>>()
            statements.collect { throw CancellationException("cancelled") }
            Result.success(ScriptExecutionSummary(0, null, 0L))
        }

        val collected = mutableListOf<ExecutionEvent>()
        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking {
                useCase(StringReader("SELECT 1;")).collect { collected.add(it) }
            }
        }

        assertTrue(collected.none { it is ExecutionEvent.Completed })
    }
}
