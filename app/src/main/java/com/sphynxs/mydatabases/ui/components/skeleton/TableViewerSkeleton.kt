package com.sphynxs.mydatabases.ui.components.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.ui.theme.MyDataBasesTheme
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppSpacing

/**
 * Skeleton de carga para TableViewerScreen.
 *
 * Reproduce la silueta de un grid de tabla con 10 filas × 4 columnas.
 * Simula headers de columna + filas de datos.
 *
 * @param modifier Modificador opcional
 *
 * @author israel-icm
 * @date 2026-06-15
 */
@Composable
fun TableViewerSkeleton(
    modifier: Modifier = Modifier
) {
    val spacing = LocalAppSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.lg)
            .semantics { contentDescription = "Cargando datos de tabla" },
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        // Headers de columna
        TableRowPlaceholder(isHeader = true)

        Spacer(modifier = Modifier.height(spacing.xs))

        // Filas de datos
        repeat(10) {
            TableRowPlaceholder(isHeader = false)
        }
    }
}

/**
 * Placeholder de una fila de tabla (header o datos).
 */
@Composable
private fun TableRowPlaceholder(isHeader: Boolean) {
    val spacing = LocalAppSpacing.current
    val height = if (isHeader) 32.dp else 24.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        // 4 columnas con anchos variados
        SkeletonBox(width = 80.dp, height = height, modifier = Modifier.weight(1f))
        SkeletonBox(width = 120.dp, height = height, modifier = Modifier.weight(1.5f))
        SkeletonBox(width = 100.dp, height = height, modifier = Modifier.weight(1.2f))
        SkeletonBox(width = 60.dp, height = height, modifier = Modifier.weight(0.8f))
    }
}

/**
 * Preview para TableViewerSkeleton.
 */
@Preview(showBackground = true)
@Composable
private fun TableViewerSkeletonPreview() {
    MyDataBasesTheme {
        TableViewerSkeleton()
    }
}
