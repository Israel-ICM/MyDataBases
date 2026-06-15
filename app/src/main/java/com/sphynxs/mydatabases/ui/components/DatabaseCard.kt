package com.sphynxs.mydatabases.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.core.database.models.Database
import com.sphynxs.mydatabases.ui.theme.MyDataBasesTheme
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppElevation
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
    val elevation = LocalAppElevation.current

    Card(
        onClick = onCardClick,
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = elevation.cardResting, shape = shapes.medium),
        shape = shapes.medium,
        colors = CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg)
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contenido principal
            Column(modifier = Modifier.weight(1f)) {
                // Nombre de la base de datos
                Text(
                    text = database.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(spacing.xxs))

                // Charset y collation
                Text(
                    text = "${database.charset} • ${database.collation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Icono de navegación
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
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
