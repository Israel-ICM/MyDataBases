package com.sphynxs.mydatabases.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.core.database.models.Table
import com.sphynxs.mydatabases.core.database.models.TableType
import com.sphynxs.mydatabases.ui.theme.MyDataBasesTheme
import com.sphynxs.mydatabases.ui.theme.pressAnimation
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppShapes
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppSpacing

/**
 * Tarjeta reutilizable para mostrar una tabla en la lista.
 *
 * Muestra: nombre de la tabla, tipo (TABLE/VIEW), engine, row count.
 *
 * @param table La tabla a mostrar
 * @param onCardClick Callback cuando se toca la tarjeta (navegar a visor de tabla)
 * @param modifier Modificador opcional para la tarjeta
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@Composable
fun TableCard(
    table: Table,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalAppSpacing.current
    val shapes = LocalAppShapes.current
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        onClick = onCardClick,
        modifier = modifier
            .fillMaxWidth()
            .pressAnimation(interactionSource),
        shape = shapes.large,
        colors = CardDefaults.cardColors(),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg)
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon 40dp con container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(AppIcons.Nav.Tables),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            // Contenido principal
            Column(modifier = Modifier.weight(1f)) {
                // Nombre de la tabla
                Text(
                    text = table.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(spacing.xs))

                // Tipo y engine (sin row count)
                val details = buildString {
                    append(when (table.type) {
                        TableType.TABLE -> "Tabla"
                        TableType.VIEW -> "Vista"
                        TableType.SYSTEM_TABLE -> "Sistema"
                    })
                    table.engine?.let { append(" • $it") }
                }
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Badge rowCount prominente
            Badge(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text("${table.rowCount ?: 0} filas")
            }
        }
    }
}

/**
 * Preview para TableCard.
 */
@Preview(showBackground = true)
@Composable
private fun TableCardPreview() {
    MyDataBasesTheme {
        TableCard(
            table = Table(
                name = "users",
                database = "production",
                type = TableType.TABLE,
                engine = "InnoDB",
                rowCount = 1523
            ),
            onCardClick = {}
        )
    }
}
