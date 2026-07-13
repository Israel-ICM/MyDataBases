package com.sphynxs.mydatabases.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.core.database.models.ConnectionFolder
import com.sphynxs.mydatabases.ui.components.ios.IOSCard
import com.sphynxs.mydatabases.ui.components.ios.IOSDropdownMenu
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens
import compose.icons.TablerIcons
import compose.icons.tablericons.ChevronRight
import compose.icons.tablericons.Folder

/**
 * Card de folder con diseño iOS unificado.
 *
 * Muestra un folder con su nombre, cantidad de conexiones, y controles
 * de expand/collapse, editar y eliminar.
 *
 * @param folder El folder a mostrar
 * @param connectionCount Cantidad de conexiones dentro del folder
 * @param isExpanded Si el folder está expandido (mostrando conexiones)
 * @param onToggleExpand Callback cuando se toca el botón de expand/collapse
 * @param onEditClick Callback cuando se toca el botón editar
 * @param onDeleteClick Callback cuando se toca el botón eliminar
 * @param isReorderMode Si está en modo de reordenamiento
 * @param modifier Modificador opcional
 *
 * @author israel-icm
 * @date 2026-07-01
 */
@Composable
fun FolderCard(
    folder: ConnectionFolder,
    connectionCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isReorderMode: Boolean = false,
    onDragHandleTouch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val folderColor = MaterialTheme.colorScheme.primary
    
    // Animación del chevron (0° = collapsed, 90° = expanded)
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        label = "chevron_rotation"
    )

    IOSCard(
        onClick = onToggleExpand,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LocalDesignTokens.current.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalDesignTokens.current.innerSpacing)
        ) {
            // Ícono de folder con gradiente
            Box(
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    folderColor.copy(alpha = 0.25f),
                                    folderColor.copy(alpha = 0.12f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = TablerIcons.Folder,
                        contentDescription = null,
                        tint = folderColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Nombre del folder + connection count
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = stringResource(R.string.folder_connections_count, connectionCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Chevron de expand/collapse
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = TablerIcons.ChevronRight,
                    contentDescription = if (isExpanded) {
                        stringResource(R.string.folder_collapse)
                    } else {
                        stringResource(R.string.folder_expand)
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronRotation)
                )
            }

            // More menu o drag handle
            if (isReorderMode) {
                IconButton(
                    onClick = onDragHandleTouch,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = PhosphorAppIcons.Action.dragHandle,
                        contentDescription = stringResource(R.string.action_reorder),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorAppIcons.Action.more,
                            contentDescription = stringResource(R.string.action_more),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IOSDropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.folder_edit)) },
                            onClick = {
                                showMenu = false
                                onEditClick()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = PhosphorAppIcons.Action.edit,
                                    contentDescription = null
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.folder_delete)) },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = PhosphorAppIcons.Action.delete,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
