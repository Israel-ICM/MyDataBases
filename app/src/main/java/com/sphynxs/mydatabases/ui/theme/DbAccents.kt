package com.sphynxs.mydatabases.ui.theme

import androidx.compose.ui.graphics.Color
import com.sphynxs.mydatabases.core.database.engine.DatabaseType

/**
 * Paleta de colores de acento por tipo de base de datos.
 *
 * Colores inspirados en las marcas oficiales de cada motor:
 * - MySQL: azul cian característico del logo MySQL/Dolphin
 * - PostgreSQL: azul oscuro institucional de Postgres
 * - MariaDB: dorado/bronce del logo MariaDB seal
 * - SQLite: azul marino profundo del logo SQLite
 *
 * Uso:
 * ```kotlin
 * val accent = DbAccents.accentFor(connection.type)
 * Box(
 *     modifier = Modifier.background(accent.copy(alpha = 0.1f))
 * )
 * ```
 *
 * decorative, deferred: dark-mode — colores de identidad de marca de cada motor de DB
 * (logos), intencionalmente theme-INVARIANTES. Mismo tratamiento que `accentPrimary`/
 * `accentSecondary`/`destructiveAction` en `DesignTokens.kt` (PR-2): son identidad visual
 * reconocible, no roles semánticos de superficie/texto — no deben derivar del ColorScheme.
 *
 * @author israel-icm
 * @date 2026-06-15
 */
object DbAccents {
    /**
     * MySQL accent — azul cian (#00758F).
     */
    val MySQL = Color(0xFF00758F)

    /**
     * PostgreSQL accent — azul institucional (#336791).
     */
    val Postgres = Color(0xFF336791)

    /**
     * MariaDB accent — dorado/bronce (#C49A6C).
     */
    val MariaDB = Color(0xFFC49A6C)

    /**
     * SQLite accent — azul marino (#003B57).
     */
    val SQLite = Color(0xFF003B57)

    /**
     * Retorna el color de acento para un tipo de base de datos.
     *
     * @param type Tipo de motor de base de datos
     * @return Color de acento correspondiente
     */
    fun accentFor(type: DatabaseType): Color = when (type) {
        DatabaseType.MYSQL -> MySQL
        DatabaseType.POSTGRESQL -> Postgres
        DatabaseType.MARIADB -> MariaDB
        DatabaseType.SQLITE -> SQLite
    }
}
