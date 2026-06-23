package com.sphynxs.mydatabases.ui.screens.databases

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.sphynxs.mydatabases.ui.workspace.WorkspaceManager

/**
 * Pantalla que lanza una workspace Query card.
 *
 * Esta pantalla no renderiza UI — solo invoca WorkspaceManager.openQueryCard()
 * y el workspace se abre automáticamente en el overlay superior.
 *
 * Spec: workspace-cards (Requirement: Opening a Query Card)
 * Phase: 7 (WorkspaceManager Integration)
 *
 * @param connectionId ID de la conexión activa
 * @param workspaceManager Workspace manager inyectado desde NavHost
 * @param modifier Modificador opcional (no usado)
 *
 * @author sdd-apply
 * @date 2026-06-23
 */
@Composable
fun NewQueryScreen(
    connectionId: String,
    workspaceManager: WorkspaceManager,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(connectionId) {
        workspaceManager.openQueryCard(
            connectionId = connectionId,
            initialSql = null
        )
    }
}
