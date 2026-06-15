package com.sphynxs.mydatabases.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.ui.theme.DbAccents
import com.sphynxs.mydatabases.ui.theme.MyDataBasesTheme

/**
 * Database type selector — SegmentedButton M3 para elegir tipo de DB.
 *
 * Características:
 * - 4 opciones: MySQL, PostgreSQL, MariaDB, SQLite
 * - Ícono con accent color cuando está selected
 * - Layout: SingleChoiceSegmentedButtonRow (M3)
 *
 * @param selected Tipo de base de datos actualmente seleccionado
 * @param onSelect Callback cuando se selecciona un tipo diferente
 * @param modifier Modificador opcional
 * @param enabled Si el selector está habilitado (default: true)
 *
 * @author israel-icm
 * @date 2026-06-15
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseTypeSelector(
    selected: DatabaseType,
    onSelect: (DatabaseType) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val options = listOf(
        DatabaseType.MYSQL,
        DatabaseType.POSTGRESQL,
        DatabaseType.MARIADB,
        DatabaseType.SQLITE
    )

    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, type ->
            SegmentedButton(
                selected = selected == type,
                onClick = { onSelect(type) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                ),
                enabled = enabled,
                icon = {
                    Icon(
                        painter = painterResource(
                            when (type) {
                                DatabaseType.MYSQL -> AppIcons.Db.MySql
                                DatabaseType.POSTGRESQL -> AppIcons.Db.Postgres
                                DatabaseType.MARIADB -> AppIcons.Db.MariaDb
                                DatabaseType.SQLITE -> AppIcons.Db.Sqlite
                            }
                        ),
                        contentDescription = type.displayName,
                        tint = if (selected == type) {
                            DbAccents.accentFor(type)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                label = {
                    Text(
                        text = type.displayName,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}

/**
 * Preview con MySQL selected.
 */
@Preview(name = "MySQL selected", showBackground = true)
@Composable
private fun DatabaseTypeSelectorPreview_MySQL() {
    MyDataBasesTheme {
        DatabaseTypeSelector(
            selected = DatabaseType.MYSQL,
            onSelect = {}
        )
    }
}

/**
 * Preview con PostgreSQL selected.
 */
@Preview(name = "PostgreSQL selected", showBackground = true)
@Composable
private fun DatabaseTypeSelectorPreview_Postgres() {
    MyDataBasesTheme {
        DatabaseTypeSelector(
            selected = DatabaseType.POSTGRESQL,
            onSelect = {}
        )
    }
}

/**
 * Preview con MariaDB selected.
 */
@Preview(name = "MariaDB selected", showBackground = true)
@Composable
private fun DatabaseTypeSelectorPreview_MariaDB() {
    MyDataBasesTheme {
        DatabaseTypeSelector(
            selected = DatabaseType.MARIADB,
            onSelect = {}
        )
    }
}

/**
 * Preview con SQLite selected.
 */
@Preview(name = "SQLite selected", showBackground = true)
@Composable
private fun DatabaseTypeSelectorPreview_SQLite() {
    MyDataBasesTheme {
        DatabaseTypeSelector(
            selected = DatabaseType.SQLITE,
            onSelect = {}
        )
    }
}
