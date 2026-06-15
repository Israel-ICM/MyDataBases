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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.core.database.models.Database
import com.sphynxs.mydatabases.ui.theme.MyDataBasesTheme
import com.sphynxs.mydatabases.ui.theme.pressAnimation
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppShapes
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppSpacing

/**
 * Tarjeta reutilizable para mostrar una base de datos en la lista.
 *
 * Muestra: nombre de la DB, charset, collation.
 *
 * @param database La base de datos a mostrar
 * @param onCardClick Callback cuando se toca la tarjeta (navegar a tablas)
 * @param modifier Modificador opcional para la tarjeta
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@Composable
fun DatabaseCard(
    database: Database,
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
            // Icon 48dp con container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.large
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_nav_tables),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Contenido principal
            Column(modifier = Modifier.weight(1f)) {
                // Nombre de la base de datos
                Text(
                    text = database.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(spacing.xs))

                // Charset chip
                AssistChip(
                    onClick = {},
                    label = { Text("${database.charset} • ${database.collation}") },
                    enabled = false,
                    colors = AssistChipDefaults.assistChipColors(
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

/**
 * Preview para DatabaseCard.
 */
@Preview(showBackground = true)
@Composable
private fun DatabaseCardPreview() {
    MyDataBasesTheme {
        DatabaseCard(
            database = Database(
                name = "production",
                charset = "utf8mb4",
                collation = "utf8mb4_unicode_ci"
            ),
            onCardClick = {}
        )
    }
}
