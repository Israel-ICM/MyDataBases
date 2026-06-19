package com.sphynxs.mydatabases.ui.screens.databases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.R

/**
 * Pantalla placeholder para editor de consultas SQL.
 * 
 * Placeholder con icon + title + "Coming soon" message.
 * Implementación completa del editor SQL en follow-up.
 *
 * Spec: query-editor-placeholder
 * Phase: 7 (NewQueryScreen Placeholder)
 *
 * @param connectionId ID de la conexión activa
 * @param modifier Modificador opcional
 *
 * @author sdd-apply
 * @date 2026-06-19
 */
@Composable
fun NewQueryScreen(
    connectionId: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = stringResource(R.string.query_editor_title),
                modifier = Modifier
                    .height(64.dp)
                    .semantics {
                        contentDescription = "Query editor icon"
                    },
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.query_editor_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.monitor_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
