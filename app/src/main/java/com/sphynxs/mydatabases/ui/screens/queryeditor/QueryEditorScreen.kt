package com.sphynxs.mydatabases.ui.screens.queryeditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.ui.components.ResultGrid
import com.sphynxs.mydatabases.ui.screens.queryeditor.components.SqlCodeEditor

/**
 * Pantalla del editor de queries SQL.
 *
 * Features:
 * - Editor con syntax highlighting (SqlCodeEditor)
 * - Toolbar con botones Execute/Cancel/Clear
 * - Result pane con ResultGrid (SELECT) o UpdateSummary (INSERT/UPDATE/DELETE)
 * - Error display
 * - Loading state
 *
 * Spec: openspec/changes/sql-editor/specs/query-editor/spec.md
 *
 * @param connectionId ID de la conexión activa
 * @param initialSql SQL inicial (null = editor vacío)
 * @param viewModel ViewModel (inyectado por Hilt)
 *
 * @author israel-icm
 * @date 2026-06-23
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueryEditorScreen(
    connectionId: String,
    initialSql: String? = null,
    viewModel: QueryEditorViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var sqlText by remember { mutableStateOf(TextFieldValue(initialSql ?: "")) }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SQL Editor") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // SQL Editor
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                SqlCodeEditor(
                    value = sqlText,
                    onValueChange = { sqlText = it },
                    placeholder = "Enter SQL query...",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Execute button
                Button(
                    onClick = {
                        viewModel.executeStatements(sql = sqlText.text)
                    },
                    enabled = sqlText.text.isNotBlank() && uiState !is QueryEditorUiState.Running,
                    modifier = Modifier.semantics {
                        contentDescription = "Execute query"
                    }
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.execute_button))
                }

                // Cancel button (visible solo cuando está Running)
                if (uiState is QueryEditorUiState.Running) {
                    OutlinedButton(
                        onClick = { viewModel.cancel() }
                    ) {
                        Text(stringResource(R.string.cancel_button))
                    }
                }

                // Clear button
                OutlinedButton(
                    onClick = { sqlText = TextFieldValue("") }
                ) {
                    Text(stringResource(R.string.clear_button))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Result pane
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (val state = uiState) {
                        is QueryEditorUiState.Idle -> {
                            Text(
                                text = stringResource(R.string.query_editor_empty_state),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        is QueryEditorUiState.Running -> {
                            CircularProgressIndicator()
                        }

                        is QueryEditorUiState.SelectResult -> {
                            ResultGrid(
                                columns = state.result.columns,
                                rows = state.result.rows,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is QueryEditorUiState.UpdateSummary -> {
                            UpdateSummaryTable(results = state.results)
                        }

                        is QueryEditorUiState.Error -> {
                            ErrorDisplay(
                                message = state.message,
                                failedStatement = state.failedStatement
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tabla de resumen para statements UPDATE/INSERT/DELETE.
 *
 * Muestra: statement SQL, rows affected, execution time.
 */
@Composable
private fun UpdateSummaryTable(results: List<com.sphynxs.mydatabases.domain.models.StatementResult>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Update Summary",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        results.forEach { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = result.sql,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Rows affected: ${result.affectedRows ?: 0}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Time: ${result.executionTimeMs}ms",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * Display de error con mensaje y statement que falló.
 */
@Composable
private fun ErrorDisplay(
    message: String,
    failedStatement: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.query_editor_error_prefix),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            failedStatement?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Failed statement:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
