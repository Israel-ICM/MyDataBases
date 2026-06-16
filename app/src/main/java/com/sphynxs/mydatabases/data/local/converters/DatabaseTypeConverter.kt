package com.sphynxs.mydatabases.data.local.converters

import androidx.room.TypeConverter
import com.sphynxs.mydatabases.core.database.engine.DatabaseType

/**
 * Converter para persistir [DatabaseType] enum en Room.
 *
 * Room no sabe cómo guardar enums directamente, así que este converter
 * los transforma a String y viceversa.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
class DatabaseTypeConverter {

    /**
     * Convierte un [DatabaseType] a String para guardarlo en la DB.
     *
     * @param type El enum a convertir
     * @return El nombre del enum como string
     */
    @TypeConverter
    fun fromDatabaseType(type: DatabaseType): String {
        return type.name
    }

    /**
     * Convierte un String de la DB de vuelta a [DatabaseType].
     *
     * @param value El string guardado en la DB
     * @return El enum correspondiente
     */
    @TypeConverter
    fun toDatabaseType(value: String): DatabaseType {
        return DatabaseType.valueOf(value)
    }
}
