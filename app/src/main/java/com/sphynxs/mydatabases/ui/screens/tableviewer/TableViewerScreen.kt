package com.sphynxs.mydatabases.ui.screens.tableviewer

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.core.database.models.Column
import com.sphynxs.mydatabases.core.database.models.ColumnKey
import com.sphynxs.mydatabases.ui.components.ErrorCard
import com.sphynxs.mydatabases.ui.components.LoadingIndicator
import com.sphynxs.mydatabases.ui.components.skeleton.TableViewerSkeleton
import com.sphynxs.mydatabases.ui.theme.MyDataBasesTheme

/**
 * Pantalla de visor de tabla.
 *
 * Muestra dos tabs:
 * - Rows: Grid de datos (LazyColumn + horizontal scroll)
 * - Schema: Metadata de columnas
 *
 * @param databaseName Nombre de la base de datos
 * @param tableName Nombre de la tabla
 * @param viewModel El ViewModel con la lógica de estado
 * @param modifier Modificador opcional
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableViewerScreen(
    databaseName: String,
    tableName: String,
    viewModel: TableViewerViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Cargar tabla al montar la pantalla
    LaunchedEffect(databaseName, tableName) {
        viewModel.loadTable(databaseName, tableName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.table_viewer_title, tableName)) }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        when (val state = uiState) {
            is TableViewerUiState.Loading -> {
                TableViewerSkeleton(modifier = Modifier.padding(paddingValues))
            }

            is TableViewerUiState.Success -> {
                Column(modifier = Modifier.padding(paddingValues)) {
                    // Tabs
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(stringResource(R.string.table_viewer_tab_rows)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(stringResource(R.string.table_viewer_tab_schema)) }
                        )
                    }

                    // Contenido del tab seleccionado
                    when (selectedTab) {
                        0 -> RowsTab(
                            columns = state.rows.columns,
                            rows = state.rows.rows,
                            modifier = Modifier.fillMaxSize()
                        )
                        1 -> SchemaTab(
                            columns = state.columns,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            is TableViewerUiState.Empty -> {
                Column(modifier = Modifier.padding(paddingValues)) {
                    // Tabs (schema sigue disponible aunque no haya rows)
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(stringResource(R.string.table_viewer_tab_rows)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(stringResource(R.string.table_viewer_tab_schema)) }
                        )
                    }

                    when (selectedTab) {
                        0 -> Box(
                            modifier = Modifier.fillMaxSize(),
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
                        1 -> SchemaTab(
                            columns = state.columns,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            is TableViewerUiState.Error -> {
                ErrorCard(
                    message = state.message,
                    onRetry = { viewModel.loadTable(databaseName, tableName) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

/**
 * Tab de rows (grid de datos con scroll horizontal).
 */
@Composable
private fun RowsTab(
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

/**
 * Tab de schema (metadata de columnas).
 */
@Composable
private fun SchemaTab(
    columns: List<Column>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.padding(16.dp)) {
        items(columns, key = { it.name }) { column ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = column.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tipo: ${column.type}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Nullable: ${if (column.nullable) "Sí" else "No"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (column.key != ColumnKey.NONE) {
                        Text(
                            text = "Clave: ${column.key}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    column.default?.let {
                        Text(
                            text = "Default: $it",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    column.extra?.let {
                        Text(
                            text = "Extra: $it",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * Preview para TableViewerScreen.
 */
@Preview(showBackground = true)
@Composable
private fun TableViewerScreenPreview() {
    MyDataBasesTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Preview: Visor de tabla")
        }
    }
}
