package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import com.sphynxs.mydatabases.domain.sql.ScriptError
import com.sphynxs.mydatabases.domain.sql.ScriptExecutionProgress
import com.sphynxs.mydatabases.domain.sql.ScriptExecutionSummary
import com.sphynxs.mydatabases.domain.sql.SqlStatementStreamSplitter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import java.io.Reader
import javax.inject.Inject

/**
 * Phase B (execute) of the "run script" flow (change `large-sql-script-execution`).
 *
 * Re-splits [reader] with a FRESH pass (deliberate — see `design.md` "Splitter re-run per phase":
 * caching Phase A's statement list would reintroduce the exact "buffer the whole script" failure
 * mode this feature exists to avoid) and delegates sequential execution to
 * [DatabaseRepository.executeScript], bridging its suspend `onProgress` callback into this
 * use case's own [Flow] via `channelFlow`.
 *
 * Takes a caller-supplied [Reader] for the same reason as [PreScanScriptUseCase] — stays testable
 * without Android `Uri`/`ContentResolver` mocking.
 *
 * @param repository Repository for database access
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-04
 */
class ExecuteScriptUseCase @Inject constructor(
    private val repository: DatabaseRepository
) {

    /**
     * Runs Phase B execution over [reader], emitting progress per statement and a final
     * [ExecutionEvent.Completed] with the [ScriptExecutionSummary], or [ExecutionEvent.Error] if
     * the re-split fails (malformed script) or the repository/engine reports a failure.
     *
     * @param reader Source to re-split and execute; caller owns its lifecycle
     * @return Cold [Flow] of [ExecutionEvent]
     */
    operator fun invoke(reader: Reader): Flow<ExecutionEvent> = channelFlow {
        try {
            val statements = SqlStatementStreamSplitter.split(reader)
            val result = repository.executeScript(statements) { progress ->
                send(ExecutionEvent.Progress(progress))
            }
            result.fold(
                onSuccess = { summary -> send(ExecutionEvent.Completed(summary)) },
                onFailure = { error -> send(ExecutionEvent.Error(error)) }
            )
        } catch (e: ScriptError) {
            send(ExecutionEvent.Error(e))
        }
    }
}

/** Events emitted by [ExecuteScriptUseCase] during the Phase B execution pass. */
sealed class ExecutionEvent {
    data class Progress(val progress: ScriptExecutionProgress) : ExecutionEvent()
    data class Completed(val summary: ScriptExecutionSummary) : ExecutionEvent()
    data class Error(val error: Throwable) : ExecutionEvent()
}
