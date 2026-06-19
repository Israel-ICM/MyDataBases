package com.sphynxs.mydatabases.core.database.models

/**
 * Representa un character set disponible en el servidor MySQL/MariaDB.
 *
 * Obtenido desde `SHOW CHARACTER SET`.
 *
 * @property name Nombre del charset (ej: utf8mb4, latin1)
 * @property description Descripción legible (ej: "UTF-8 Unicode")
 * @property defaultCollation Collation por defecto para este charset
 * @property maxLength Longitud máxima en bytes por caracter
 *
 * @author israel-icm
 * @date 2026-06-19
 */
data class CharacterSet(
    val name: String,
    val description: String,
    val defaultCollation: String,
    val maxLength: Int
)

/**
 * Representa una collation disponible para un character set.
 *
 * Obtenido desde `SHOW COLLATION WHERE Charset = 'X'`.
 *
 * @property name Nombre de la collation (ej: utf8mb4_general_ci)
 * @property charset Character set asociado
 * @property id ID numérico de la collation
 * @property isDefault Si es la collation por defecto del charset
 *
 * @author israel-icm
 * @date 2026-06-19
 */
data class Collation(
    val name: String,
    val charset: String,
    val id: Int,
    val isDefault: Boolean
)
