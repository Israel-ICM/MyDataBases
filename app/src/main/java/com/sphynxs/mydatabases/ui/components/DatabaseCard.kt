package com.sphynxs.mydatabases.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.core.database.models.Database
import com.sphynxs.mydatabases.ui.components.ios.IOSCard
import com.sphynxs.mydatabases.ui.components.ios.IOSDropdownMenu
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens
import com.sphynxs.mydatabases.ui.theme.AppTheme

/**
 * Card de base de datos con diseño iOS unificado.
 *
 * Al mantener presionado (long-press) muestra feedback háptico y un popup menu
 * con las acciones disponibles sobre la base de datos (change `database-row-actions-menu`).
 * Los callbacks de acción son no-op por default: por ahora solo se expone el menú,
 * la lógica de negocio de cada acción se implementa en cambios posteriores.
 *
 * @param database La base de datos a mostrar
 * @param onCardClick Callback cuando se toca la tarjeta (navegar a tablas)
 * @param onEditClick Callback al seleccionar "Editar" en el popup menu
 * @param onNewQueryClick Callback al seleccionar "Nuevo query" en el popup menu
 * @param onOpenConsoleClick Callback al seleccionar "Abrir consola" en el popup menu
 * @param onSearchClick Callback al seleccionar "Buscar en base de datos" en el popup menu
 * @param onBackupClick Callback al seleccionar "Backup" en el popup menu
 * @param onShareClick Callback al seleccionar "Compartir" en el popup menu
 * @param onDeleteClick Callback al seleccionar "Eliminar" en el popup menu
 * @param modifier Modificador opcional
 *
 * @author israel-icm
 * @date 2026-06-17 (updated 2026-07-21 con long-press + popup menu, change `database-row-actions-menu`;
 *   agregado "Backup" el mismo día)
 */
@Composable
fun DatabaseCard(
    database: Database,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit = {},
    onNewQueryClick: () -> Unit = {},
    onOpenConsoleClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onBackupClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    IOSCard(
        onClick = onCardClick,
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            showMenu = true
        },
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LocalDesignTokens.current.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalDesignTokens.current.innerSpacing)
        ) {
            // Ícono de database con gradiente turquesa
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        // accentSecondary = brand_tertiary (0xFF8EE3D3); accentSuccess = el
                        // mismo turquesa oscuro (0xFF006B63) — ambos exact-match a los
                        // literales previos, ahora vía token theme-invariant (DesignTokens.kt).
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                LocalDesignTokens.current.accentSecondary.copy(alpha = 0.20f),
                                LocalDesignTokens.current.accentSuccess.copy(alpha = 0.12f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PhosphorAppIcons.Db.mysql,  // Usa Database genérico
                    contentDescription = null,
                    tint = LocalDesignTokens.current.accentSuccess,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Contenido principal
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Nombre de la base de datos
                Text(
                    text = database.name,
                    fontSize = LocalDesignTokens.current.cardTitleSize,
                    fontWeight = LocalDesignTokens.current.cardTitleWeight,
                    color = LocalDesignTokens.current.cardTitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Charset
                Text(
                    text = database.charset,
                    fontSize = LocalDesignTokens.current.cardSubtitleSize,
                    fontWeight = LocalDesignTokens.current.cardSubtitleWeight,
                    color = LocalDesignTokens.current.cardSubtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IOSDropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            // Opción: Editar — modifica la base de datos
            DropdownMenuItem(
                text = { Text(stringResource(R.string.database_row_action_edit)) },
                leadingIcon = {
                    Icon(
                        imageVector = PhosphorAppIcons.Action.edit,
                        contentDescription = null,
                        tint = LocalDesignTokens.current.iconNormal
                    )
                },
                onClick = {
                    showMenu = false
                    onEditClick()
                }
            )

            // Opción: Nuevo query — herramienta para trabajar con esta base de datos
            DropdownMenuItem(
                text = { Text(stringResource(R.string.database_row_action_new_query)) },
                leadingIcon = {
                    Icon(
                        imageVector = PhosphorAppIcons.Nav.newQuery,
                        contentDescription = null,
                        tint = LocalDesignTokens.current.iconNormal
                    )
                },
                onClick = {
                    showMenu = false
                    onNewQueryClick()
                }
            )

            // Opción: Abrir consola
            DropdownMenuItem(
                text = { Text(stringResource(R.string.database_row_action_open_console)) },
                leadingIcon = {
                    Icon(
                        imageVector = PhosphorAppIcons.Nav.console,
                        contentDescription = null,
                        tint = LocalDesignTokens.current.iconNormal
                    )
                },
                onClick = {
                    showMenu = false
                    onOpenConsoleClick()
                }
            )

            // Opción: Buscar en base de datos
            DropdownMenuItem(
                text = { Text(stringResource(R.string.database_row_action_search)) },
                leadingIcon = {
                    Icon(
                        imageVector = PhosphorAppIcons.Action.search,
                        contentDescription = null,
                        tint = LocalDesignTokens.current.iconNormal
                    )
                },
                onClick = {
                    showMenu = false
                    onSearchClick()
                }
            )

            // Opción: Backup — preservar/extraer una copia completa
            DropdownMenuItem(
                text = { Text(stringResource(R.string.database_row_action_backup)) },
                leadingIcon = {
                    Icon(
                        imageVector = PhosphorAppIcons.Nav.backup,
                        contentDescription = null,
                        tint = LocalDesignTokens.current.iconNormal
                    )
                },
                onClick = {
                    showMenu = false
                    onBackupClick()
                }
            )

            // Opción: Compartir — extraer/enviar
            DropdownMenuItem(
                text = { Text(stringResource(R.string.database_row_action_share)) },
                leadingIcon = {
                    Icon(
                        imageVector = PhosphorAppIcons.Action.share,
                        contentDescription = null,
                        tint = LocalDesignTokens.current.iconNormal
                    )
                },
                onClick = {
                    showMenu = false
                    onShareClick()
                }
            )

            // Opción: Eliminar (destructiva — la más severa, va al final del menú)
            DropdownMenuItem(
                text = { Text(stringResource(R.string.database_row_action_delete)) },
                leadingIcon = {
                    Icon(
                        imageVector = PhosphorAppIcons.Action.delete,
                        contentDescription = null,
                        tint = LocalDesignTokens.current.destructiveAction
                    )
                },
                onClick = {
                    showMenu = false
                    onDeleteClick()
                }
            )
        }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F7)
@Composable
private fun DatabaseCardPreview() {
    AppTheme {
        DatabaseCard(
            database = Database(
                name = "my_database",
                charset = "utf8mb4",
                collation = "utf8mb4_unicode_ci"
            ),
            onCardClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
