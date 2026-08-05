package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.domain.sql.RiskCategory
import com.sphynxs.mydatabases.domain.sql.RiskReport
import com.sphynxs.mydatabases.domain.sql.ScriptError
import com.sphynxs.mydatabases.domain.sql.SqlStatementStreamSplitter
import com.sphynxs.mydatabases.domain.sql.StatementRiskClassifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import java.io.Reader
import javax.inject.Inject

/**
 * Phase A (pre-scan) of the "run script" flow (change `large-sql-script-execution`).
 *
 * Streams a `.sql` script once via [SqlStatementStreamSplitter], classifies every statement
 * via [StatementRiskClassifier], and aggregates counts + exact line numbers per [RiskCategory]
 * into a final [RiskReport]. Performs ZERO database interaction — this is pure domain logic,
 * read-only, cancelable via plain `Flow` cancellation (no extra hook needed).
 *
 * Takes a caller-supplied [Reader] rather than an Android `Uri` so this use case stays a pure
 * JVM component, fully unit-testable without `ContentResolver`/`Context` mocking — the caller
 * (the ViewModel, which has Hilt-injected Android context) is responsible for opening the
 * stream, consistent with the splitter's "Reader-in / Flow-out, source-agnostic" contract.
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-04
 */
class PreScanScriptUseCase @Inject constructor() {

    /**
     * Runs the pre-scan pass over [reader], emitting progress as statements are scanned and a
     * final [PreScanEvent.Completed] with the aggregated [RiskReport], or a [PreScanEvent.Error]
     * if the script is malformed (e.g. an unparseable `DELIMITER` directive).
     *
     * @param reader Source to scan; caller owns its lifecycle (this use case does not close it)
     * @return Cold [Flow] of [PreScanEvent]
     */
    operator fun invoke(reader: Reader): Flow<PreScanEvent> = channelFlow {
        val counts = mutableMapOf<RiskCategory, Int>()
        val lineNumbers = mutableMapOf<RiskCategory, MutableList<Int>>()
        var total = 0

        try {
            SqlStatementStreamSplitter.split(reader).collect { statement ->
                total++
                val category = StatementRiskClassifier.classify(statement)
                if (category != null) {
                    counts[category] = (counts[category] ?: 0) + 1
                    lineNumbers.getOrPut(category) { mutableListOf() }.add(statement.lineNumber)
                }
                send(PreScanEvent.Progress(total, statement.lineNumber))
            }
            send(
                PreScanEvent.Completed(
                    RiskReport(
                        totalStatements = total,
                        counts = RiskCategory.entries.associateWith { counts[it] ?: 0 },
                        lineNumbers = lineNumbers
                    )
                )
            )
        } catch (e: ScriptError) {
            send(PreScanEvent.Error(e))
        }
    }
}

/** Events emitted by [PreScanScriptUseCase] during the Phase A pre-scan pass. */
sealed class PreScanEvent {
    data class Progress(val statementsScanned: Int, val lineNumber: Int) : PreScanEvent()
    data class Completed(val report: RiskReport) : PreScanEvent()
    data class Error(val error: ScriptError) : PreScanEvent()
}
