package com.sphynxs.mydatabases.ui.components.folders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.core.database.models.ConnectionFolder
import com.sphynxs.mydatabases.ui.components.PhosphorAppIcons
import compose.icons.TablerIcons
import compose.icons.tablericons.Folder
import compose.icons.tablericons.Home

/**
 * Bottom sheet para seleccionar un folder de destino al mover una conexión.
 *
 * Muestra una lista de folders disponibles más la opción "Root level" (sin folder).
 * Incluye botón para crear un nuevo folder.
 *
 * @param folders Lista de folders disponibles
 * @param currentFolderId ID del folder actual de la conexión (null si está en root)
 * @param onSelectFolder Callback cuando se selecciona un folder (null = root)
 * @param onCreateNew Callback cuando se toca "New folder"
 * @param onDismiss Callback cuando se cierra el sheet
 *
 * @author israel-icm
 * @date 2026-07-01
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToFolderSheet(
    folders: List<ConnectionFolder>,
    currentFolderId: String?,
    onSelectFolder: (String?) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Título
            Text(
                text = stringResource(R.string.move_to_folder),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            
            LazyColumn {
                // Opción: Root level (sin folder)
                item {
                    FolderOption(
                        name = stringResource(R.string.move_to_root),
                        icon = { 
                            Icon(
                                imageVector = TablerIcons.Home,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        isSelected = currentFolderId == null,
                        onClick = {
                            onSelectFolder(null)
                            onDismiss()
                        }
                    )
                }
                
                if (folders.isNotEmpty()) {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
                
                // Lista de folders
                items(folders, key = { it.id }) { folder ->
                    FolderOption(
                        name = folder.name,
                        icon = {
                            Icon(
                                imageVector = TablerIcons.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        isSelected = currentFolderId == folder.id,
                        onClick = {
                            onSelectFolder(folder.id)
                            onDismiss()
                        }
                    )
                }
                
                // Botón: New folder
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onCreateNew)
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = PhosphorAppIcons.Action.add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.size(16.dp))
                        
                        Text(
                            text = stringResource(R.string.folder_create),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Spacer final
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Opción de folder en la lista.
 */
@Composable
private fun FolderOption(
    name: String,
    icon: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        
        Spacer(modifier = Modifier.size(16.dp))
        
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
    }
}
