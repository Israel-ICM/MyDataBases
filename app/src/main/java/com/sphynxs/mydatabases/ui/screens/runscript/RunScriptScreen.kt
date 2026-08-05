package com.sphynxs.mydatabases.ui.screens.runscript

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sphynxs.mydatabases.LocalWindowSizeClass
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.domain.sql.RiskCategory
import com.sphynxs.mydatabases.domain.sql.RiskReport
import com.sphynxs.mydatabases.domain.sql.ScriptExecutionSummary
import com.sphynxs.mydatabases.ui.adaptive.adaptivePadding
import java.io.BufferedReader

/**
 * Screen for the "run script" flow — executes a large `.sql` file without ever opening it in the
 * visual editor (change `large-sql-script-execution`). Renders [RunScriptViewModel]'s sealed
 * [RunScriptState] directly; owns the one `Uri` -> `Reader` resolution point (via
 * `ContentResolver`) so the ViewModel itself stays `Context`-free.
 *
 * @param uri SAF content `Uri` of the `.sql` file to run
 * @param connectionId Active connection identifier (currently informational only — execution
 *   always targets whichever connection is active, per the locked DB-context-inheritance rule)
 * @param onFinished Called when the flow reaches a terminal state (`Success`, `Error`,
 *   `Cancelled`) and the caller should navigate away
 * @param viewModel Hilt-provided [RunScriptViewModel]
 * @param modifier Optional modifier
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-04
 */
@Composable
fun RunScriptScreen(
    uri: Uri,
    connectionId: String,
    onFinished: () -> Unit = {},
    viewModel: RunScriptViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val windowSizeClass = LocalWindowSizeClass.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(uri) {
        viewModel.runScript {
            val stream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open input stream for $uri")
            BufferedReader(stream.reader())
        }
    }

    val padding = windowSizeClass?.let { adaptivePadding(it) } ?: PaddingValues16dp

    Box(
        modifier = modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        when (val current = state) {
            is RunScriptState.Idle -> Unit
            is RunScriptState.PreScanning -> PreScanningContent(current)
            is RunScriptState.AwaitingConfirmation -> RiskConfirmationDialog(
                report = current.report,
                onConfirm = viewModel::confirm,
                onDecline = viewModel::decline
            )
            is RunScriptState.Executing -> ExecutingContent(
                statementIndex = current.progress.statementIndex,
                lineNumber = current.progress.lineNumber,
                onCancel = viewModel::cancel
            )
            is RunScriptState.Success -> SuccessContent(current.summary, onFinished)
            is RunScriptState.Error -> OutcomeContent(
                titleRes = R.string.run_script_error_title,
                detail = current.message,
                showPartialUpdateWarning = true,
                onDismiss = onFinished
            )
            is RunScriptState.Cancelled -> OutcomeContent(
                titleRes = R.string.run_script_cancelled_title,
                detail = null,
                showPartialUpdateWarning = true,
                onDismiss = onFinished
            )
        }
    }
}

private val PaddingValues16dp = PaddingValues(16.dp)

@Composable
private fun PreScanningContent(state: RunScriptState.PreScanning) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text(
            text = stringResource(
                R.string.run_script_prescanning_label,
                state.statementsScanned,
                state.lineNumber
            ),
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun ExecutingContent(statementIndex: Int, lineNumber: Int, onCancel: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.run_script_executing_label, statementIndex + 1, lineNumber),
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
        )
        OutlinedButton(onClick = onCancel) {
            Text(stringResource(R.string.action_cancel))
        }
    }
}

@Composable
private fun SuccessContent(summary: ScriptExecutionSummary, onDismiss: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.run_script_success_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.run_script_success_summary, summary.statementsExecuted),
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )
        Button(onClick = onDismiss) {
            Text(stringResource(R.string.action_ok))
        }
    }
}

@Composable
private fun OutcomeContent(
    titleRes: Int,
    detail: String?,
    showPartialUpdateWarning: Boolean,
    onDismiss: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
        if (detail != null) {
            Text(text = detail, modifier = Modifier.padding(top = 8.dp))
        }
        if (showPartialUpdateWarning) {
            Text(
                text = stringResource(R.string.run_script_partial_update_warning),
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Button(onClick = onDismiss) {
            Text(stringResource(R.string.action_ok))
        }
    }
}

/**
 * A single, aggregated confirmation dialog listing risky-statement counts and line numbers per
 * [RiskCategory] — shown exactly once per run, never per-statement (per the locked two-phase
 * design decision).
 */
@Composable
private fun RiskConfirmationDialog(
    report: RiskReport,
    onConfirm: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text(stringResource(R.string.run_script_confirm_title)) },
        text = {
            Column {
                Text(stringResource(R.string.run_script_confirm_intro))
                RiskCategory.entries.forEach { category ->
                    val count = report.counts[category] ?: 0
                    if (count > 0) {
                        val lines = report.lineNumbers[category].orEmpty().joinToString(", ")
                        Text(
                            text = stringResource(
                                R.string.run_script_confirm_category_row,
                                categoryLabel(category),
                                count,
                                lines
                            ),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.run_script_confirm_button)) }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text(stringResource(R.string.run_script_decline_button)) }
        }
    )
}

@Composable
private fun categoryLabel(category: RiskCategory): String = when (category) {
    RiskCategory.DDL -> stringResource(R.string.run_script_risk_category_ddl)
    RiskCategory.DELETE -> stringResource(R.string.run_script_risk_category_delete)
    RiskCategory.UPDATE_NO_WHERE -> stringResource(R.string.run_script_risk_category_update_no_where)
}
