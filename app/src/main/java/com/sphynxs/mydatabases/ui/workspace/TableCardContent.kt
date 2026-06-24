package com.sphynxs.mydatabases.ui.workspace

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

    LaunchedEffect(databaseName, tableName) {
        viewModel.loadTable(databaseName, tableName)
    }

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
 * Rows content — tabla estilo Excel 2021 con edición.
 */
@Composable
private fun RowsContent(
    columns: List<String>,
    rows: List<Map<String, Any?>>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    // Estado de celda seleccionada (rowIndex, columnName, valor)
    var selectedCell by remember { mutableStateOf<Triple<Int, String, String?>?>(null) }
    
    // Estado de zoom (escala visual) y offset
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    
    // Calcular límite de zoom out
    // Ancho total tabla = columnas * 150dp
    val tableWidthPx = with(density) { (150.dp * columns.size).toPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    
    // minScale = cuando la tabla escalada cabe exactamente en la pantalla
    val minScale = (screenWidthPx / tableWidthPx).coerceIn(0.3f, 1f)
    
    // Calcular límites de offset según el zoom actual
    // Cuando scale < 1, la tabla es más pequeña que su tamaño original
    val scaledTableWidth = tableWidthPx * scale
    val maxOffsetX = if (scaledTableWidth < screenWidthPx) {
        // Si la tabla escalada cabe completa, centrarla
        (screenWidthPx - scaledTableWidth) / 2f
    } else {
        0f // Borde izquierdo pegado a la pantalla
    }
    val minOffsetX = if (scaledTableWidth < screenWidthPx) {
        maxOffsetX // Centrada
    } else {
        screenWidthPx - scaledTableWidth // Borde derecho pegado a la pantalla
    }
    
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .pointerInput(minScale) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            
                            if (zoom != 1f) {
                                scale = (scale * zoom).coerceIn(minScale, 2.5f)
                                event.changes.forEach { it.consume() }
                            }
                            
                            if (pan.x != 0f) {
                                offsetX = (offsetX + pan.x).coerceIn(minOffsetX, maxOffsetX)
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                }
        ) {
        // Header row estilo Excel
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.border(width = 1.dp, color = borderColor)
            ) {
                columns.forEach { column ->
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .border(
                                width = 0.5.dp,
                                color = borderColor
                            )
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = column,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Data rows con bordes estilo Excel
        LazyColumn {
            items(rows.size) { index ->
                val row = rows[index]
                val backgroundColor = if (index % 2 == 0) 
                    MaterialTheme.colorScheme.surface
                else 
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                
                Row(
                    modifier = Modifier
                        .background(backgroundColor)
                        .border(width = 0.5.dp, color = borderColor)
                ) {
                    columns.forEach { column ->
                        val value = row[column]?.toString() ?: ""
                        val isNull = row[column] == null
                        val isSelected = selectedCell?.let { it.first == index && it.second == column } == true
                        
                        Box(
                            modifier = Modifier
                                .width(150.dp)
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp, 
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else borderColor
                                )
                                .background(
                                    if (isSelected) 
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                                    else 
                                        backgroundColor
                                )
                                .clickable {
                                    selectedCell = Triple(index, column, if (isNull) null else value)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = if (isNull) "NULL" else value,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isNull) 
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                else 
                                    MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
            }
        }
        
        // Editor flotante (aparece cuando hay celda seleccionada)
        selectedCell?.let { (rowIndex, columnName, currentValue) ->
            CellEditor(
                columnName = columnName,
                value = currentValue ?: "",
                onSave = { newValue ->
                    // TODO: Guardar cambio en la base de datos
                    selectedCell = null
                },
                onCancel = {
                    selectedCell = null
                }
            )
        }
    }


/**
 * Editor flotante para editar una celda.
 */
@Composable
private fun CellEditor(
    columnName: String,
    value: String,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editedValue by remember { mutableStateOf(value) }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        tonalElevation = 8.dp,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = columnName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = editedValue,
                onValueChange = { editedValue = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_value)) },
                singleLine = false,
                minLines = 3,
                maxLines = 6
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
            ) {
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.action_cancel))
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(onClick = { onSave(editedValue) }) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}
