package com.sphynxs.mydatabases.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.ui.theme.MyDataBasesTheme
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppElevation
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppShapes
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppSpacing

/**
 * Tarjeta de error premium con ícono custom, mensaje y botón de reintentar opcional.
 *
 * Refactorizada para usar design tokens (spacing, shapes, elevation) y soportar
 * errores sin acción de retry (ej: errores no recuperables).
 *
 * Layout: Card con background errorContainer, ícono Error a la izquierda, texto + botón opcional.
 *
 * @param message Mensaje de error a mostrar (max 4 líneas con ellipsis)
 * @param onRetry Callback opcional cuando el usuario presiona "Reintentar" (null para omitir botón)
 * @param modifier Modifier para aplicar al contenedor (por defecto fillMaxSize)
 *
 * @author israel-icm
 * @date 2026-06-15
 */
@Composable
fun ErrorCard(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val spacing = LocalAppSpacing.current
    val shapes = LocalAppShapes.current
    val elevation = LocalAppElevation.current

    Column(
        modifier = modifier.padding(spacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = elevation.cardResting, shape = shapes.medium),
            shape = shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ícono de error
                Icon(
                    painter = painterResource(R.drawable.ic_state_error),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.width(spacing.lg))

                // Contenido (título + descripción + botón)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.error_generic),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(spacing.xs))

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Botón de retry opcional
                    onRetry?.let {
                        Spacer(modifier = Modifier.height(spacing.lg))
                        OutlinedButton(
                            onClick = it,
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(text = stringResource(R.string.action_retry))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Preview para ErrorCard con retry.
 */
@Preview(showBackground = true)
@Composable
private fun ErrorCardWithRetryPreview() {
    MyDataBasesTheme {
        ErrorCard(
            message = "No se pudo conectar al servidor. Verificá tu conexión a internet.",
            onRetry = {}
        )
    }
}

/**
 * Preview para ErrorCard sin retry.
 */
@Preview(showBackground = true)
@Composable
private fun ErrorCardNoRetryPreview() {
    MyDataBasesTheme {
        ErrorCard(
            message = "El archivo de base de datos está corrupto y no se puede recuperar.",
            onRetry = null
        )
    }
}
