package com.sphynxs.mydatabases.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sphynxs.mydatabases.core.database.models.ConnectionFolder
import com.sphynxs.mydatabases.data.local.converters.DatabaseTypeConverter
import com.sphynxs.mydatabases.data.local.converters.SSLConfigConverter
import com.sphynxs.mydatabases.data.local.converters.SSHTunnelConfigConverter
import com.sphynxs.mydatabases.data.local.dao.ConnectionDao
import com.sphynxs.mydatabases.data.local.dao.FolderDao
import com.sphynxs.mydatabases.data.local.entities.ConnectionEntity

/**
 * Base de datos Room de la aplicación.
 *
 * Contiene todas las entidades y DAOs de MyDataBases.
 *
 * @author israel-icm
 * @date 2026-06-12 (updated 2026-06-30 for folders & reordering)
 */
@Database(
    entities = [
        ConnectionEntity::class,
        ConnectionFolder::class
    ],
    version = 3,  // Bumped for folders: new table + folder_id/order columns
    exportSchema = false
)
@TypeConverters(
    DatabaseTypeConverter::class,
    SSHTunnelConfigConverter::class,
    SSLConfigConverter::class
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * DAO para operaciones sobre conexiones.
     *
     * @return El DAO de conexiones
     */
    abstract fun connectionDao(): ConnectionDao
    
    /**
     * DAO para operaciones sobre folders.
     *
     * @return El DAO de folders
     */
    abstract fun folderDao(): FolderDao
}
