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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.ui.theme.AppTheme
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppSpacing

/**
 * Skeleton de carga para DatabaseListScreen.
 *
 * Reproduce la silueta de 6 DatabaseCard placeholders.
 * Cada placeholder simula: nombre DB + charset/collation + icono flecha.
 *
 * @param modifier Modificador opcional
 *
 * @author israel-icm
 * @date 2026-06-15
 */
@Composable
fun DatabaseListSkeleton(
    modifier: Modifier = Modifier
) {
    val spacing = LocalAppSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.lg)
            .semantics { contentDescription = "Cargando bases de datos" },
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Spacer(modifier = Modifier.height(spacing.sm))
        repeat(6) {
            DatabaseCardPlaceholder()
        }
    }
}

/**
 * Placeholder individual que simula un DatabaseCard.
 */
@Composable
private fun DatabaseCardPlaceholder() {
    val spacing = LocalAppSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Contenido principal (nombre + metadata)
        Column(modifier = Modifier.weight(1f)) {
            SkeletonText(width = 120.dp, lines = 1)
            Spacer(modifier = Modifier.height(spacing.xs))
            SkeletonText(width = 180.dp, lines = 1)
        }

        Spacer(modifier = Modifier.width(spacing.md))

        // Ícono de navegación
        SkeletonCircle(size = 24.dp)
    }
}

/**
 * Preview para DatabaseListSkeleton.
 */
@Preview(showBackground = true)
@Composable
private fun DatabaseListSkeletonPreview() {
    AppTheme {
        DatabaseListSkeleton()
    }
}
