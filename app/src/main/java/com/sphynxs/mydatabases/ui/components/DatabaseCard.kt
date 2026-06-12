package com.sphynxs.mydatabases.ui.components

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.core.database.models.Database
import com.sphynxs.mydatabases.ui.theme.MyDataBasesTheme

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
    Card(
        onClick = onCardClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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

                Spacer(modifier = Modifier.height(4.dp))

                // Charset y collation
                Text(
                    text = "${database.charset} • ${database.collation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Icono de navegación
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
