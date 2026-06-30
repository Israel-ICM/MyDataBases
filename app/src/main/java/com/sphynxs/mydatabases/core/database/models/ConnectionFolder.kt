package com.sphynxs.mydatabases.core.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entidad de Room para folders/grupos de conexiones.
 *
 * Permite organizar conexiones en grupos expandibles/colapsables.
 * Las conexiones sin folder (folderId = null) aparecen en el nivel root.
 *
 * @property id Identificador único del folder (UUID)
 * @property name Nombre del folder mostrado al usuario
 * @property icon Identificador de ícono personalizado (opcional, futuro)
 * @property color Color hex personalizado (opcional, futuro)
 * @property isExpanded Estado UI: true = expandido, false = colapsado
 * @property order Posición en la lista (menor = arriba)
 * @property createdAt Timestamp de creación en milisegundos
 *
 * @author israel-icm
 * @date 2026-06-30
 */
@Entity(tableName = "connection_folders")
data class ConnectionFolder(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val name: String,
    
    val icon: String? = null,
    
    val color: String? = null,
    
    @ColumnInfo(name = "is_expanded")
    val isExpanded: Boolean = true,
    
    val order: Int = 0,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
