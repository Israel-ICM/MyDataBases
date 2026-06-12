package com.sphynxs.mydatabases.core.database.models

/**
 * Representa una base de datos (schema) en el servidor.
 *
 * @property name Nombre de la base de datos
 * @property charset Charset por defecto
 * @property collation Collation por defecto
 * @author israel-icm
 * @date 2026-06-11
 */
data class Database(
    val name: String,
    val charset: String,
    val collation: String
)
