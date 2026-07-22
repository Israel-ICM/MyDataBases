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
import com.sphynxs.mydatabases.core.database.models.Table as TableModel
import com.sphynxs.mydatabases.ui.components.ios.IOSCard
import com.sphynxs.mydatabases.ui.components.ios.IOSDropdownMenu
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens
import com.sphynxs.mydatabases.ui.theme.AppTheme

/**
 * Card de tabla con diseño iOS unificado.
 *
 * Al mantener presionado (long-press) muestra feedback háptico y un popup menu
 * con las acciones disponibles sobre la tabla (change `table-row-actions-menu`).
 * Los callbacks de acción son no-op por default: por ahora solo se expone el menú,
 * la lógica de negocio de cada acción se implementa en cambios posteriores.
 *
 * @param table La tabla a mostrar
 * @param onCardClick Callback cuando se toca la tarjeta (navegar a visor de tabla)
 * @param onEditClick Callback al seleccionar "Editar" en el popup menu
 * @param onRenameClick Callback al seleccionar "Renombrar" en el popup menu
 * @param onDuplicateClick Callback al seleccionar "Duplicar" en el popup menu
 * @param onCopyClick Callback al seleccionar "Copiar" en el popup menu
 * @param onExportClick Callback al seleccionar "Exportar" en el popup menu
 * @param onShareClick Callback al seleccionar "Compartir" en el popup menu
 * @param onAddShortcutClick Callback al seleccionar "Agregar a accesos directos" en el popup menu
 * @param onTruncateClick Callback al seleccionar "Truncar" en el popup menu
 * @param onDeleteClick Callback al seleccionar "Eliminar" en el popup menu
 * @param modifier Modificador opcional
 *
 * @author israel-icm
 * @date 2026-06-17 (updated 2026-07-21 con long-press + popup menu, change `table-row-actions-menu`;
 *   reordenado el mismo día: modificar tabla -> crear/extraer datos -> organizar -> destructivas
 *   al final en orden de severidad creciente)
 */
@Composable
fun TableCard(
    table: TableModel,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onDuplicateClick: () -> Unit = {},
    onCopyClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onAddShortcutClick: () -> Unit = {},
    onTruncateClick: () -> Unit = {},
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
            // Ícono de tabla con gradiente turquesa
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        // accentSuccessLight (0xFFA3F2E6) / accentSuccess (0xFF006B63) —
                        // exact-match a los literales previos, ahora vía token
                        // theme-invariant (DesignTokens.kt).
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                LocalDesignTokens.current.accentSuccessLight.copy(alpha = 0.20f),
                                LocalDesignTokens.current.accentSuccess.copy(alpha = 0.12f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PhosphorAppIcons.Nav.tables,
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
                // Nombre de la tabla
                Text(
                    text = table.name,
                    fontSize = LocalDesignTokens.current.cardTitleSize,
                    fontWeight = LocalDesignTokens.current.cardTitleWeight,
                    color = LocalDesignTokens.current.cardTitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Engine y row count
                val subtitle = buildString {
                    table.engine?.let { append(it) }
                    if (table.rowCount != null) {
                        if (isNotEmpty()) append(" • ")
                        append("${table.rowCount} rows")
                    }
                }
                
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        fontSize = LocalDesignTokens.current.cardSubtitleSize,
                        fontWeight = LocalDesignTokens.current.cardSubtitleWeight,
                        color = LocalDesignTokens.current.cardSubtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        IOSDropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            // Opción: Editar — modifica la estructura de la tabla
            DropdownMenuItem(
                text = { Text(stringResource(R.string.table_action_edit)) },
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

            // Opción: Renombrar — modifica solo el nombre de la tabla
            DropdownMenuItem(
                text = { Text(stringResource(R.string.table_action_rename)) },
                leadingIcon = {
                    Icon(
                        imageVector = PhosphorAppIcons.Action.rename,
                        contentDescription = null,
                        tint = LocalDesignTokens.current.iconNormal
                    )
                },
                onClick = {
                    showMenu = false
                    onRenameClick()
                }
            )

            // Opción: Duplicar — crea una tabla nueva a partir de esta
            DropdownMenuItem(
                text = { Text(stringResource(R.string.table_action_duplicate)) },
                leadingIcon = {
                    Icon(
                        imageVector = PhosphorAppIcons.Action.duplicate,
                        contentDescription = null,
                        tint = LocalDesignTokens.current.iconNormal
                    )
                },
                onClick = {
                    showMenu = false
                    onDuplicateClick()
                }
            )

            // Opción: Copiar — extraer datos, de más liviano (portapapeles)...
            DropdownMenuItem(
                text = { Text(stringResource(R.string.table_action_copy)) },
                leadingIcon = {
                    Icon(
                        imageVector = PhosphorAppIcons.Action.copy,
                        contentDescription = null,
                        tint = LocalDesignTokens.current.iconNormal
                    )
                },
                onClick = {
                    showMenu = false
                    onCopyClick()
                }
            )

            // Opción: Exportar — ...a más pesado (archivo)...
            DropdownMenuItem(
                text = { Text(stringResource(R.string.table_action_export)) },
                leadingIcon = {
                    Icon(
                        imageVector = PhosphorAppIcons.Action.export,
                        contentDescription = null,
                        tint = LocalDesignTokens.current.iconNormal
                    )
                },
                onClick = {
                    showMenu = false
                    onExportClick()
                }
            )

            // Opción: Compartir — ...hasta compartir el resultado
            DropdownMenuItem(
                text = { Text(stringResource(R.string.table_action_share)) },
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

            // Opción: Agregar a accesos directos — organización, no modifica la tabla
            DropdownMenuItem(
                text = { Text(stringResource(R.string.table_action_add_shortcut)) },
                leadingIcon = {
                    Icon(
                        imageVector = PhosphorAppIcons.Action.addShortcut,
                        contentDescription = null,
                        tint = LocalDesignTokens.current.iconNormal
                    )
                },
                onClick = {
                    showMenu = false
                    onAddShortcutClick()
                }
            )

            // Opción: Truncar (destructiva — borra todas las filas, mantiene la estructura;
            // menos severa que eliminar, por eso va antes)
            DropdownMenuItem(
                text = { Text(stringResource(R.string.table_action_truncate)) },
                leadingIcon = {
                    Icon(
                        imageVector = PhosphorAppIcons.Action.truncate,
                        contentDescription = null,
                        tint = LocalDesignTokens.current.destructiveAction
                    )
                },
                onClick = {
                    showMenu = false
                    onTruncateClick()
                }
            )

            // Opción: Eliminar (destructiva — la más severa, va al final del menú)
            DropdownMenuItem(
                text = { Text(stringResource(R.string.table_action_delete)) },
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
private fun TableCardPreview() {
    AppTheme {
        TableCard(
            table = TableModel(
                name = "users",
                database = "mydb",
                type = com.sphynxs.mydatabases.core.database.models.TableType.TABLE,
                engine = "InnoDB",
                rowCount = 1234
            ),
            onCardClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
