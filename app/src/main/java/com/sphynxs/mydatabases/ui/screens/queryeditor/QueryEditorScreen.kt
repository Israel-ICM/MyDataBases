package com.sphynxs.mydatabases.ui.screens.queryeditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
@Composable
fun QueryEditorScreen(
    connectionId: String,
    initialSql: String? = null,
    viewModel: QueryEditorViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var sqlText by remember { mutableStateOf(TextFieldValue(initialSql ?: "")) }
    val uiState by viewModel.uiState.collectAsState()
    val cursorPositions = remember { mutableStateListOf<Int>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
            // SQL Editor (estilo VS Code - modo claro)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                SqlCodeEditor(
                    value = sqlText,
                    onValueChange = { sqlText = it },
                    placeholder = "-- Enter SQL query...",
                    cursorPositions = cursorPositions,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Execute/Cancel button (toggle based on running state)
                if (uiState is QueryEditorUiState.Running) {
                    // Cancel button (red, stop icon)
                    Button(
                        onClick = { viewModel.cancel() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336), // Rojo
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = stringResource(R.string.cancel_button)
                        )
                    }
                } else {
                    // Execute button (green, play icon)
                    Button(
                        onClick = {
                            viewModel.executeStatements(sql = sqlText.text)
                        },
                        enabled = sqlText.text.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50), // Verde
                            contentColor = Color.White
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "Execute query"
                        }
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.execute_button)
                        )
                    }
                }

                // Save button (icon only, no action yet)
                OutlinedButton(
                    onClick = { 
                        // TODO: Implementar guardar query
                        android.util.Log.d("QueryEditorScreen", "Save clicked (not implemented yet)")
                    },
                    enabled = sqlText.text.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "Save query"
                    )
                }
                
                // Clear button (adaptive: clear cursors or clear text)
                OutlinedButton(
                    onClick = { 
                        if (cursorPositions.isNotEmpty()) {
                            // Si hay cursores (badge >= 1), limpiar solo cursores
                            cursorPositions.clear()
                            android.util.Log.d("QueryEditorScreen", "Cursors cleared")
                        } else {
                            // Si no hay cursores (badge vacío), limpiar texto
                            sqlText = TextFieldValue("")
                            android.util.Log.d("QueryEditorScreen", "Text cleared")
                        }
                    }
                ) {
                    if (cursorPositions.size >= 1) {
                        // Mostrar "|×" cuando hay 1+ cursores (badge visible)
                        Text(
                            text = "|×",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    } else {
                        // Mostrar ícono X cuando badge está vacío
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear_button)
                        )
                    }
                }
                
                // Add cursor button with badge
                BadgedBox(
                    badge = {
                        if (cursorPositions.isNotEmpty()) {
                            Badge {
                                Text(
                                    text = "${cursorPositions.size}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                ) {
                    OutlinedButton(
                        onClick = {
                            val currentPos = sqlText.selection.start
                            android.util.Log.d("QueryEditorScreen", "Click |+ at position $currentPos, cursorPositions: $cursorPositions, contains: ${cursorPositions.contains(currentPos)}")
                            if (!cursorPositions.contains(currentPos)) {
                                cursorPositions.add(currentPos)
                                android.util.Log.d("QueryEditorScreen", "✅ Cursor added at $currentPos")
                            } else {
                                cursorPositions.remove(currentPos)
                                android.util.Log.d("QueryEditorScreen", "❌ Cursor removed from $currentPos")
                            }
                        }
                    ) {
                        Text(
                            text = "|+",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
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

                        is QueryEditorUiState.Success -> {
                            SuccessDisplay(
                                message = state.message,
                                executionTimeMs = state.executionTimeMs
                            )
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
 * Display de éxito con mensaje verde.
 */
@Composable
private fun SuccessDisplay(
    message: String,
    executionTimeMs: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "✓ Success",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Execution time: ${executionTimeMs}ms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
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
