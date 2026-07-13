package com.sphynxs.mydatabases.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        }
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
