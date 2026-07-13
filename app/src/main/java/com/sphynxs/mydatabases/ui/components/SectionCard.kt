package com.sphynxs.mydatabases.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.ui.theme.AppTheme
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppShapes
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppSpacing

/**
 * Section card — agrupa visualmente contenido relacionado con un título.
 *
 * Diseñado para formularios y configuraciones, separa secciones lógicas
 * (ej: "Conexión", "Autenticación", "Avanzado").
 *
 * Características:
 * - Background: surfaceContainer
 * - Shape: large (16dp)
 * - Padding interno: 20dp
 * - Title: labelLarge, color onSurfaceVariant
 * - Spacer 8dp entre title y content
 *
 * @param title Título de la sección (ej: "Configuración de Conexión")
 * @param modifier Modificador opcional
 * @param content Contenido de la sección (composable lambda con ColumnScope)
 *
 * @author israel-icm
 * @date 2026-06-15
 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shapes = LocalAppShapes.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // Section title
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Section content
            content()
        }
    }
}

/**
 * Preview con sample content.
 */
@Preview(name = "Section Card", showBackground = true)
@Composable
private fun SectionCardPreview() {
    AppTheme {
        SectionCard(
            title = "Configuración de Conexión"
        ) {
            Text(
                text = "Host: db.example.com",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Port: 3306",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Database: mydb",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
