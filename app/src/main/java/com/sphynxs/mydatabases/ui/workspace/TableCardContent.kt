package com.sphynxs.mydatabases.ui.workspace

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.ui.components.ErrorCard
import com.sphynxs.mydatabases.ui.components.skeleton.TableViewerSkeleton
import com.sphynxs.mydatabases.ui.screens.tableviewer.TableViewerUiState
import com.sphynxs.mydatabases.ui.screens.tableviewer.TableViewerViewModel

/**
 * Table Card Content — contenido de una card de tabla en el workspace.
 *
 * Extraído de TableViewerScreen para reutilización en el workspace multi-tab.
 * Muestra SOLO la tab de "Filas" (rows table) sin Scaffold ni TabRow.
 *
 * El workspace manejará la navegación entre cards (tabs), por lo que este
 * composable se enfoca solo en renderizar el contenido de la tabla.
 *
 * Estados:
 * - Loading: Skeleton mientras carga la data
 * - Success: Grid de rows con scroll horizontal
 * - Empty: Mensaje "tabla vacía" (pero schema existe)
 * - Error: Card de error con retry
 *
 * @param databaseName Nombre de la base de datos
 * @param tableName Nombre de la tabla
 * @param modifier Modificador opcional
 * @param viewModel ViewModel con la lógica de carga (Hilt-injected)
 *
 * @author israel-icm
 * @date 2026-06-15
 */
@Composable
fun TableCardContent(
    databaseName: String,
    tableName: String,
    modifier: Modifier = Modifier,
    viewModel: TableViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // NO cargar aquí - TableViewerScreen ya lo hace con su LaunchedEffect
    // Este composable solo renderiza el estado actual del ViewModel

    when (val state = uiState) {
        is TableViewerUiState.Loading -> {
            TableViewerSkeleton(modifier = modifier)
        }

        is TableViewerUiState.Success -> {
            RowsContent(
                columns = state.rows.columns,
                rows = state.rows.rows,
                modifier = modifier
            )
        }

        is TableViewerUiState.Empty -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.table_viewer_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        is TableViewerUiState.Error -> {
            ErrorCard(
                message = state.message,
                onRetry = { viewModel.loadTable(databaseName, tableName) },
                modifier = modifier.fillMaxSize()
            )
        }

        else -> {
            // Idle state (no debería ocurrir por el LaunchedEffect)
            Box(modifier = modifier.fillMaxSize())
        }
    }
}

/**
 * Rows content — grid de datos con scroll horizontal.
 *
 * Layout:
 * - Header row: Nombres de columnas (bold)
 * - Data rows: Valores de cada row
 * - Cada columna tiene width fijo de 150dp
 */
@Composable
private fun RowsContent(
    columns: List<String>,
    rows: List<Map<String, Any?>>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        // Header row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                columns.forEach { column ->
                    Text(
                        text = column,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .width(150.dp)
                            .padding(horizontal = 4.dp)
                    )
                }
            }
            HorizontalDivider()
        }

        // Data rows
        items(rows) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                columns.forEach { column ->
                    Text(
                        text = row[column]?.toString() ?: "NULL",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .width(150.dp)
                            .padding(horizontal = 4.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
