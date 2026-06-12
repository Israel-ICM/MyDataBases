package com.sphynxs.mydatabases.core.database.engine

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Tipos de motores de bases de datos soportados.
 *
 * @property displayName Nombre para mostrar en la UI
 * @property defaultPort Puerto por defecto del motor
 * @author israel-icm
 * @date 2026-06-11
 */
@Parcelize
enum class DatabaseType(
    val displayName: String,
    val defaultPort: Int
) : Parcelable {
    MYSQL("MySQL", 3306),
    MARIADB("MariaDB", 3306),
    POSTGRESQL("PostgreSQL", 5432),
    SQLITE("SQLite", 0)
}
