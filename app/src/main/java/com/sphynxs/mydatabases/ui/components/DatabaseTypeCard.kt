package com.sphynxs.mydatabases.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.ui.components.ios.IOSGroupedCard
import com.sphynxs.mydatabases.ui.components.ios.IOSListItem
import com.sphynxs.mydatabases.ui.theme.DbAccents

/**
 * Card expandible para un tipo de base de datos.
 * Muestra icono, descripción, lista de conexiones y botón para agregar.
 */
@Composable
fun DatabaseTypeCard(
    type: DatabaseType,
    connections: List<ConnectionConfig>,
    onConnectionClick: (String) -> Unit,
    onEditConnection: (String) -> Unit,
    onAddConnection: (DatabaseType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    IOSGroupedCard(modifier = modifier) {
        // Header con título y chevron
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${type.name} Connections",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp 
                             else Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Colapsar" else "Expandir",
                tint = Color(0xFF8E8E93)
            )
        }
        
        // Contenido expandible
        AnimatedVisibility(visible = expanded) {
            Column {
                HorizontalDivider(color = Color(0xFFC6C6C8), thickness = 0.5.dp)
                
                // Header con icono + descripción
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(AppIcons.Db.icon(type)),
                        contentDescription = null,
                        tint = DbAccents.accentFor(type),
                        modifier = Modifier.size(100.dp)
                    )
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Text(
                        text = getDescription(type),
                        fontSize = 15.sp,
                        color = Color(0xFF8E8E93),
                        lineHeight = 20.sp
                    )
                }
                
                HorizontalDivider(color = Color(0xFFC6C6C8), thickness = 0.5.dp)
                
                // Lista de conexiones
                if (connections.isEmpty()) {
                    Text(
                        "No hay conexiones ${type.name}",
                        fontSize = 15.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    connections.forEachIndexed { index, connection ->
                        IOSListItem(
                            title = connection.name,
                            subtitle = "${connection.host}:${connection.port}",
                            onClick = { onConnectionClick(connection.id) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { onEditConnection(connection.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = PhosphorAppIcons.Action.edit,
                                        contentDescription = "Editar",
                                        tint = Color(0xFF007AFF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            showDivider = index < connections.size - 1
                        )
                    }
                    
                    HorizontalDivider(color = Color(0xFFC6C6C8), thickness = 0.5.dp)
                }
                
                // Botón agregar conexión de este tipo
                TextButton(
                    onClick = { onAddConnection(type) },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 16.dp, bottom = 8.dp, top = 8.dp)
                ) {
                    Icon(PhosphorAppIcons.Action.add, contentDescription = null, tint = Color(0xFF007AFF))
                    Spacer(Modifier.width(4.dp))
                    Text("Agregar ${type.name}", color = Color(0xFF007AFF))
                }
            }
        }
    }
}

private fun getDescription(type: DatabaseType): String = when (type) {
    DatabaseType.MYSQL -> "Sistema de gestión de bases de datos relacional de código abierto"
    DatabaseType.POSTGRESQL -> "Sistema de gestión de bases de datos objeto-relacional avanzado"
    DatabaseType.SQLITE -> "Motor de base de datos SQL embebido, ligero y autónomo"
    DatabaseType.MARIADB -> "Fork de MySQL con mejoras de rendimiento y características"
}
