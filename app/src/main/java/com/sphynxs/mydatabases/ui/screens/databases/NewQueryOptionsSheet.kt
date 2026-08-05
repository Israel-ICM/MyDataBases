package com.sphynxs.mydatabases.ui.screens.databases

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.ui.components.PhosphorAppIcons

/**
 * "What do you want to do?" entry-point selector for the `new_query` modal action (change
 * `large-sql-script-execution`, amendment). Replaces the previous single-action behavior
 * (`workspaceManager.openQueryCard(...)` called directly) with three explicit choices.
 *
 * Mirrors [com.sphynxs.mydatabases.ui.components.folders.MoveToFolderSheet]'s stateless
 * `ModalBottomSheet` + clickable-row pattern exactly — a static `Column` of 3 rows is used
 * instead of a `LazyColumn` since the option count is fixed, never data-driven.
 *
 * @param onNewQuery Opens a blank query editor (unchanged prior behavior)
 * @param onOpenQueryFile Opens the SAF file picker; oversized files (`> 50,000` lines) redirect
 *   to the Run Script flow instead of opening in the editor (see the caller's guard logic)
 * @param onRunScript Opens the SAF file picker and goes straight to Phase A pre-scan, any size
 * @param onDismiss Closes the sheet
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-04
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewQueryOptionsSheet(
    onNewQuery: () -> Unit,
    onOpenQueryFile: () -> Unit,
    onRunScript: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text(
                text = stringResource(R.string.new_query_options_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            NewQueryOption(
                label = stringResource(R.string.new_query_option_new),
                icon = PhosphorAppIcons.Nav.newQuery,
                onClick = { onNewQuery(); onDismiss() }
            )
            NewQueryOption(
                label = stringResource(R.string.new_query_option_open_file),
                icon = PhosphorAppIcons.Nav.openQueryFile,
                onClick = { onOpenQueryFile(); onDismiss() }
            )
            NewQueryOption(
                label = stringResource(R.string.new_query_option_run_script),
                icon = PhosphorAppIcons.Nav.runScript,
                onClick = { onRunScript(); onDismiss() }
            )
        }
    }
}

@Composable
private fun NewQueryOption(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
