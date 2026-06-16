package com.sphynxs.mydatabases.core.di

import android.content.Context
import androidx.room.Room
import com.sphynxs.mydatabases.data.local.AppDatabase
import com.sphynxs.mydatabases.data.local.dao.ConnectionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt que provee la base de datos Room y sus DAOs.
 *
 * Configura la base de datos local de la app con conexiones encriptadas.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provee la instancia singleton de la base de datos Room.
     *
     * @param context Contexto de la aplicación
     * @return Instancia de AppDatabase
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mydatabases.db"
        )
            .fallbackToDestructiveMigration() // TODO: Cambiar a migraciones reales en producción
            .build()
    }

    /**
     * Provee el DAO de conexiones desde la base de datos.
     *
     * @param database Instancia de AppDatabase
     * @return DAO para operaciones CRUD en conexiones
     */
    @Provides
    fun provideConnectionDao(database: AppDatabase): ConnectionDao {
        return database.connectionDao()
    }
}
