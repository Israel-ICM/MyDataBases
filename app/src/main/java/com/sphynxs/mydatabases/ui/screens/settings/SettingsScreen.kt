package com.sphynxs.mydatabases.ui.screens.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.domain.models.ThemeMode
import com.sphynxs.mydatabases.ui.components.PhosphorAppIcons

/**
 * Pantalla de Settings — branded palette + selector de theme mode (System/Light/Dark).
 *
 * La configuración de language, etc. se implementará en el cambio #6 del roadmap.
 *
 * @param viewModel SettingsViewModel inyectado por Hilt
 *
 * @author israel-icm
 * @date 2026-06-15
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val brandedPaletteEnabled by viewModel.brandedPaletteEnabled.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val queryStorageTreeUri by viewModel.queryStorageTreeUri.collectAsState()
    val migrationPrompt by viewModel.migrationPrompt.collectAsState()
    val lastMigrationFailureCount by viewModel.lastMigrationFailureCount.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val failureMessage = stringResource(R.string.query_storage_migration_partial_failure)

    val openTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.onStorageTreeSelected(uri)
        }
    }

    LaunchedEffect(lastMigrationFailureCount) {
        val count = lastMigrationFailureCount
        if (count != null) {
            snackbarHostState.showSnackbar("$failureMessage: $count")
            viewModel.consumeLastMigrationFailureCount()
        }
    }

    if (migrationPrompt is MigrationPromptState.Shown) {
        AlertDialog(
            onDismissRequest = { viewModel.declineMigration() },
            title = { Text(stringResource(R.string.query_storage_migration_prompt_title)) },
            text = { Text(stringResource(R.string.query_storage_migration_prompt_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmMigration() }) {
                    Text(stringResource(R.string.query_storage_migration_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.declineMigration() }) {
                    Text(stringResource(R.string.query_storage_migration_decline))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = PhosphorAppIcons.Action.back,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Branded Palette Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_branded_palette_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.settings_branded_palette_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                
                Switch(
                    checked = brandedPaletteEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.setBrandedPaletteEnabled(enabled)
                    }
                )
            }

            Spacer(modifier = Modifier.padding(vertical = 12.dp))

            // Theme Mode Selector
            Text(
                text = stringResource(R.string.theme_mode_label),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.padding(vertical = 4.dp))

            ThemeModeSelector(
                selected = themeMode,
                onSelect = { mode -> viewModel.setThemeMode(mode) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.padding(vertical = 12.dp))

            // Query storage location (change `query-files-storage`)
            Text(
                text = stringResource(R.string.query_storage_location_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = if (queryStorageTreeUri == null) {
                    stringResource(R.string.query_storage_location_default_summary)
                } else {
                    stringResource(R.string.query_storage_location_custom_summary)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.padding(vertical = 4.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { openTreeLauncher.launch(null) }) {
                    Text(stringResource(R.string.query_storage_change_folder))
                }
                if (queryStorageTreeUri != null) {
                    TextButton(onClick = { viewModel.onStorageTreeSelected(null) }) {
                        Text(stringResource(R.string.query_storage_reset_default))
                    }
                }
            }
        }
    }
}

/**
 * Selector segmentado System/Light/Dark para `theme_mode`.
 *
 * Composable interno sin ViewModel — testeable directamente sin depender de Hilt,
 * siguiendo la convención del proyecto (ver `WorkspaceCarouselTest`).
 *
 * @param selected Modo de tema actualmente activo
 * @param onSelect Callback invocado con el modo elegido al tocar una opción
 * @param modifier Modifier del layout raíz
 *
 * @author gentle-ai (TDD GREEN)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)

    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(text = stringResource(mode.labelRes()))
            }
        }
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_mode_system
    ThemeMode.LIGHT -> R.string.theme_mode_light
    ThemeMode.DARK -> R.string.theme_mode_dark
}
