package com.sphynxs.mydatabases.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sphynxs.mydatabases.data.local.converters.DatabaseTypeConverter
import com.sphynxs.mydatabases.data.local.converters.SSHTunnelConfigConverter
import com.sphynxs.mydatabases.data.local.dao.ConnectionDao
import com.sphynxs.mydatabases.data.local.entities.ConnectionEntity

/**
 * Base de datos Room de la aplicación.
 *
 * Contiene todas las entidades y DAOs de MyDataBases.
 * Actualmente solo tiene la tabla de conexiones.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@Database(
    entities = [ConnectionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(
    DatabaseTypeConverter::class,
    SSHTunnelConfigConverter::class
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * DAO para operaciones sobre conexiones.
     *
     * @return El DAO de conexiones
     */
    abstract fun connectionDao(): ConnectionDao
}
