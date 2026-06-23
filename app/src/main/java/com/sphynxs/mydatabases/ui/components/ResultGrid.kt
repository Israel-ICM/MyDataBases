package com.sphynxs.mydatabases.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Componente reutilizable de grid de resultados.
 *
 * Extraído de TableViewerScreen.RowsTab para reuso en TableViewer, QueryEditor
 * y cualquier superficie futura que renderice resultados tabulares.
 *
 * Renderiza una tabla con:
 * - Header row con nombres de columnas (bold, distinct styling)
 * - Data rows alineadas con las columnas
 * - Scroll horizontal sincronizado (header + rows juntos)
 * - Display de NULL como "NULL" con color muted (no string vacío)
 * - Soporte para result sets vacíos (muestra header, sin crashes)
 *
 * @param columns Lista de nombres de columnas en orden
 * @param rows Lista de filas (cada fila = Map<String, Any?> con claves = columnas)
 * @param modifier Modificador opcional para el grid container
 *
 * @author israel-icm
 * @date 2026-06-23
 */
@Composable
fun ResultGrid(
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
                    val value = row[column]
                    Text(
                        text = value?.toString() ?: "NULL",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (value == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
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
