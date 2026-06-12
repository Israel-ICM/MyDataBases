package com.sphynxs.mydatabases.core.database.models

import com.sphynxs.mydatabases.core.database.engine.DatabaseType

/**
 * Representa una conexión activa a una base de datos.
 *
 * @property id Identificador único de la conexión
 * @property type Tipo de motor de base de datos
 * @property database Nombre de la base de datos conectada
 * @property host Host del servidor
 * @property port Puerto del servidor
 * @property username Usuario autenticado
 * @property version Versión del motor de base de datos
 * @property connectedAt Timestamp de cuándo se estableció la conexión
 * @author israel-icm
 * @date 2026-06-11
 */
data class Connection(
    val id: String,
    val type: DatabaseType,
    val database: String,
    val host: String,
    val port: Int,
    val username: String,
    val version: String,
    val connectedAt: Long
)
