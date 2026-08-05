package com.sphynxs.mydatabases.ui.screens.runscript

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphynxs.mydatabases.core.database.models.DatabaseError
import com.sphynxs.mydatabases.domain.sql.RiskReport
import com.sphynxs.mydatabases.domain.sql.ScriptError
import com.sphynxs.mydatabases.domain.sql.ScriptExecutionProgress
import com.sphynxs.mydatabases.domain.sql.ScriptExecutionSummary
import com.sphynxs.mydatabases.domain.usecases.ExecuteScriptUseCase
import com.sphynxs.mydatabases.domain.usecases.ExecutionEvent
import com.sphynxs.mydatabases.domain.usecases.PreScanEvent
import com.sphynxs.mydatabases.domain.usecases.PreScanScriptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.Reader
import javax.inject.Inject

/**
 * Sealed state machine for the "run script" flow (change `large-sql-script-execution`).
 *
 * `Idle -> PreScanning -> AwaitingConfirmation -> Executing -> Success | Error | Cancelled`,
 * with `AwaitingConfirmation` skipped entirely when the pre-scan reports a clean script
 * (`RiskReport.isRisky == false`).
 */
sealed class RunScriptState {
    data object Idle : RunScriptState()
    data class PreScanning(val statementsScanned: Int, val lineNumber: Int) : RunScriptState()
    data class AwaitingConfirmation(val report: RiskReport) : RunScriptState()
    data class Executing(val progress: ScriptExecutionProgress) : RunScriptState()
    data class Success(val summary: ScriptExecutionSummary) : RunScriptState()
    data class Error(val message: String) : RunScriptState()
    data object Cancelled : RunScriptState()
}

/**
 * Drives the two-phase "run script" flow: Phase A pre-scan (risk classification) followed by
 * Phase B execution, with a single aggregated confirmation step in between when risky statements
 * are found (change `large-sql-script-execution`).
 *
 * Stays `Context`-free: takes an `openReader: () -> Reader` factory rather than an Android `Uri`,
 * mirroring the existing `QueryEditorScreen` convention where file I/O (`ContentResolver`,
 * `openInputStream`) lives in the Composable/Screen layer, not the ViewModel. The factory is
 * invoked twice — once for Phase A, once again (fresh `Reader`) for Phase B — matching the
 * "splitter re-run per phase" design decision (never cache Phase A's statement list).
 *
 * @param preScanScriptUseCase Phase A: pre-scan and classify risk
 * @param executeScriptUseCase Phase B: re-split and execute
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-04
 */
@HiltViewModel
class RunScriptViewModel @Inject constructor(
    private val preScanScriptUseCase: PreScanScriptUseCase,
    private val executeScriptUseCase: ExecuteScriptUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<RunScriptState>(RunScriptState.Idle)
    val state: StateFlow<RunScriptState> = _state.asStateFlow()

    private var openReader: (() -> Reader)? = null
    private var job: Job? = null

    /**
     * Starts Phase A (pre-scan) over a `Reader` produced by [openReader]. If the report comes
     * back clean, automatically continues into Phase B without waiting for confirmation.
     *
     * @param openReader Factory producing a fresh [Reader] each time it's invoked (called once
     *   now for Phase A, and again later — either immediately if clean, or after [confirm] — for
     *   Phase B)
     */
    fun runScript(openReader: () -> Reader) {
        this.openReader = openReader
        job = viewModelScope.launch {
            _state.value = RunScriptState.PreScanning(0, 0)
            runCatching {
                preScanScriptUseCase(openReader()).collect { event ->
                    when (event) {
                        is PreScanEvent.Progress ->
                            _state.value = RunScriptState.PreScanning(event.statementsScanned, event.lineNumber)

                        is PreScanEvent.Completed ->
                            if (event.report.isRisky) {
                                _state.value = RunScriptState.AwaitingConfirmation(event.report)
                            } else {
                                executePhaseB()
                            }

                        is PreScanEvent.Error ->
                            _state.value = RunScriptState.Error(mapErrorToMessage(event.error))
                    }
                }
            }.onFailure { handleJobFailure(it) }
        }
    }

    /** Confirms execution of a risky script from [RunScriptState.AwaitingConfirmation]. */
    fun confirm() {
        if (_state.value !is RunScriptState.AwaitingConfirmation) return
        job = viewModelScope.launch {
            runCatching { executePhaseB() }.onFailure { handleJobFailure(it) }
        }
    }

    /** Declines execution, returning to [RunScriptState.Idle] without running anything. */
    fun decline() {
        job?.cancel()
        _state.value = RunScriptState.Idle
    }

    /** Cancels the in-flight pre-scan or execution; mirrors the `QueryEditorViewModel` pattern. */
    fun cancel() {
        job?.cancel()
        _state.value = RunScriptState.Cancelled
    }

    private suspend fun executePhaseB() {
        val reader = openReader?.invoke() ?: return
        _state.value = RunScriptState.Executing(ScriptExecutionProgress(0, 0, null))
        executeScriptUseCase(reader).collect { event ->
            when (event) {
                is ExecutionEvent.Progress -> _state.value = RunScriptState.Executing(event.progress)
                is ExecutionEvent.Completed -> _state.value = RunScriptState.Success(event.summary)
                is ExecutionEvent.Error -> _state.value = RunScriptState.Error(mapErrorToMessage(event.error))
            }
        }
    }

    private fun handleJobFailure(cause: Throwable) {
        if (cause is CancellationException) {
            _state.value = RunScriptState.Cancelled
        } else {
            _state.value = RunScriptState.Error(mapErrorToMessage(cause))
        }
    }

    /**
     * Maps a failure to a descriptive message for [RunScriptState.Error].
     *
     * `DatabaseError.QueryExecutionFailed.reason` already embeds "stopped at statement N
     * (line L)" context (see `MySQLEngine.executeScript`), so it is used as-is. Final resource-key
     * based en/es localization is Phase 12's concern (deferred to the PR that adds `strings.xml`
     * wiring) — this returns plain descriptive English text for now, consistent with
     * `DatabaseError`'s own `message`/`reason` fields, which are not localized either.
     */
    private fun mapErrorToMessage(error: Throwable): String = when (error) {
        is DatabaseError.ConnectionFailed -> error.message
        is DatabaseError.QueryExecutionFailed -> error.reason
        // ScriptError's constructor param is a plain `message: String` (not `override val`),
        // so it does not narrow inherited `Throwable.message: String?` to non-null — rebuild
        // the text from the exception's own fields instead of relying on `.message`.
        is ScriptError.MalformedDelimiterDirective -> "Malformed DELIMITER directive at line ${error.lineNumber}"
        is ScriptError.UnterminatedToken -> "Unterminated ${error.kind} starting at line ${error.lineNumber}"
        is DatabaseError -> error.message
        else -> error.message ?: "Unknown error"
    }
}
