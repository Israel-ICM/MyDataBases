package com.sphynxs.mydatabases.ui.screens.databases

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.R

/**
 * Pantalla placeholder para agregar una nueva base de datos.
 * 
 * Placeholder para PR #1 — implementación completa en PR #2.
 *
 * @param connectionId ID de la conexión activa
 *
 * @author israel-icm
 * @date 2026-06-19
 */
@Composable
fun AddDatabaseScreen(
    connectionId: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.add_database_coming_soon),
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
