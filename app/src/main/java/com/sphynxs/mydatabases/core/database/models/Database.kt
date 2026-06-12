package com.sphynxs.mydatabases.core.database.models

/**
 * Representa una base de datos (schema) en el servidor.
 *
 * Contiene:
 * - Nombre del schema
 * - Character set por defecto
 * - Collation por defecto
 *
 * @property name Nombre de la base de datos
 * @property charset Character set por defecto (ej: "utf8mb4", "latin1")
 * @property collation Collation por defecto (ej: "utf8mb4_unicode_ci", "utf8mb4_general_ci")
 *
 * @author israel-icm
 * @date 2026-06-11
 */
data class Database(
    val name: String,
    val charset: String,
    val collation: String
)
