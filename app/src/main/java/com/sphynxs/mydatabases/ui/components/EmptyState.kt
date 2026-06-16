package com.sphynxs.mydatabases.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.ui.theme.MyDataBasesTheme
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppSpacing

/**
 * Componente reutilizable para estados vacíos ilustrados.
 *
 * Muestra un ícono grande, título, descripción opcional, y acción opcional.
 * Todo centrado vertical y horizontalmente.
 *
 * Anatomía:
 * 1. Ícono central (96 dp), color outline
 * 2. Título (titleMedium, onSurface)
 * 3. Descripción opcional (bodyMedium, outline, max 2 líneas)
 * 4. Acción opcional (Button)
 *
 * @param icon Painter del ícono central (ej: AppIcons.State.EmptyConnections)
 * @param title Título principal del estado vacío
 * @param description Descripción opcional (null para omitir)
 * @param action Composable opcional para botón de acción (null para omitir)
 * @param modifier Modificador opcional
 *
 * @author israel-icm
 * @date 2026-06-15
 */
@Composable
fun EmptyState(
    icon: Painter,
    title: String,
    description: String? = null,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val spacing = LocalAppSpacing.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ícono central
            Icon(
                painter = icon,
                contentDescription = null,  // Decorativo
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            // Título
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            // Descripción opcional
            description?.let {
                Spacer(modifier = Modifier.height(spacing.sm))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Acción opcional
            action?.let {
                Spacer(modifier = Modifier.height(spacing.xl))
                it()
            }
        }
    }
}

/**
 * Preview para EmptyState con acción.
 */
@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    MyDataBasesTheme {
        EmptyState(
            icon = painterResource(R.drawable.ic_state_empty_connections),
            title = "Sin conexiones",
            description = "Agregá una conexión para empezar a trabajar con bases de datos",
            action = {
                Button(onClick = {}) {
                    Text("Nueva conexión")
                }
            }
        )
    }
}

/**
 * Preview para EmptyState sin acción.
 */
@Preview(showBackground = true)
@Composable
private fun EmptyStateNoActionPreview() {
    MyDataBasesTheme {
        EmptyState(
            icon = painterResource(R.drawable.ic_state_empty_tables),
            title = "Sin tablas",
            description = "Esta base de datos no tiene tablas visibles"
        )
    }
}
