package com.sphynxs.mydatabases.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Indicador de carga centrado en pantalla.
 *
 * Muestra un CircularProgressIndicator de Material 3 centrado vertical y
 * horizontalmente en el contenedor padre.
 *
 * Usá este componente para estados de carga mientras esperás datos.
 *
 * @param modifier Modifier para aplicar al contenedor Box (por defecto fillMaxSize)
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
