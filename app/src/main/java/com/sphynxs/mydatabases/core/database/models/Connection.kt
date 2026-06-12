package com.sphynxs.mydatabases.core.database.models

import com.sphynxs.mydatabases.core.database.engine.DatabaseType

/**
 * Representa una conexión activa a una base de datos.
 *
 * Esta clase contiene la información de estado de una conexión exitosa:
 * - Identificador único de la configuración usada
 * - Tipo de motor conectado
 * - Información del servidor (host, port, database, version)
 * - Timestamp de conexión
 *
 * No contiene la conexión JDBC real (esa se maneja internamente en el engine).
 *
 * @property id ID de la ConnectionConfig usada para conectar
 * @property type Tipo de motor de base de datos
 * @property database Nombre de la base de datos conectada
 * @property host Hostname del servidor
 * @property port Puerto del servidor
 * @property username Usuario autenticado
 * @property version Versión del motor (ej: "8.0.33", "10.11.2-MariaDB")
 * @property connectedAt Timestamp de conexión exitosa (milisegundos desde epoch)
 *
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
