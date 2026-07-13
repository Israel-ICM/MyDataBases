package com.sphynxs.mydatabases.ui.screens.databases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.ui.components.BreathingBackground
import com.sphynxs.mydatabases.ui.components.ScreenTitle
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens

/**
 * Pantalla para monitoreo del servidor.
 * 
 * TabRow con 3 tabs (Metrics, Queries, Health) con placeholders.
 * Implementación completa con métricas reales en follow-up.
 *
 * Spec: server-monitor-shell
 * Phase: 6 (MonitorScreen Placeholder)
 *
 * @param connectionId ID de la conexión activa
 * @param modifier Modificador opcional
 *
 * @author sdd-apply
 * @date 2026-06-19
 */
@Composable
fun MonitorScreen(
    connectionId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    Scaffold(
        modifier = modifier,
        containerColor = LocalDesignTokens.current.backgroundPrimary
    ) { paddingValues ->
        BreathingBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Título grande estilo iOS con botón de retroceso
                ScreenTitle(
                    title = stringResource(R.string.nav_monitor),
                    onBackClick = onNavigateBack
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // TabRow con 3 tabs
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.semantics {
                contentDescription = "Monitor tabs"
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text(stringResource(R.string.monitor_tab_metrics)) },
                modifier = Modifier.semantics {
                    contentDescription = "Metrics tab"
                }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text(stringResource(R.string.monitor_tab_queries)) },
                modifier = Modifier.semantics {
                    contentDescription = "Queries tab"
                }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { Text(stringResource(R.string.monitor_tab_health)) },
                modifier = Modifier.semantics {
                    contentDescription = "Health tab"
                }
            )
        }
        
        // Contenido placeholder según tab seleccionado
        when (selectedTabIndex) {
            0 -> PlaceholderContent(
                icon = Icons.Default.BarChart,
                title = stringResource(R.string.monitor_metrics_title),
                message = stringResource(R.string.monitor_placeholder)
            )
            1 -> PlaceholderContent(
                icon = Icons.Default.List,
                title = stringResource(R.string.monitor_queries_title),
                message = stringResource(R.string.monitor_placeholder)
            )
            2 -> PlaceholderContent(
                icon = Icons.Default.Favorite,
                title = stringResource(R.string.monitor_health_title),
                message = stringResource(R.string.monitor_placeholder)
            )
        }
            }
        }
    }
}

/**
 * Componente reutilizable para placeholder de cada tab.
 *
 * @param icon Icono representativo del tab
 * @param title Título del placeholder
 * @param message Mensaje "Coming soon"
 */
@Composable
private fun PlaceholderContent(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.height(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
