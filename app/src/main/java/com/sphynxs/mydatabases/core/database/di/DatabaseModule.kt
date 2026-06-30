package com.sphynxs.mydatabases.core.database.di

import android.content.Context
import com.sphynxs.mydatabases.core.database.engine.DatabaseEngineFactory
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module para inyección de dependencias del módulo database.
 * 
 * Provee instancias singleton de:
 * - DatabaseEngineFactory
 * - DatabaseRepository
 * 
 * @author israel-icm
 * @date 2026-06-12 (updated 2026-06-30 for Context injection)
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Provee la instancia singleton de DatabaseEngineFactory.
     * 
     * @return DatabaseEngineFactory object singleton
     */
    @Provides
    @Singleton
    fun provideDatabaseEngineFactory(): DatabaseEngineFactory {
        return DatabaseEngineFactory
    }
    
    /**
     * Provee la instancia singleton de DatabaseRepository.
     * 
     * @param context Contexto de aplicación para leer certificados SSL
     * @param factory Factory para crear DatabaseEngine instances
     * @return DatabaseRepository implementación concreta (DatabaseRepositoryImpl)
     */
    @Provides
    @Singleton
    fun provideDatabaseRepository(
        @ApplicationContext context: Context,
        factory: DatabaseEngineFactory
    ): DatabaseRepository {
        return DatabaseRepositoryImpl(context, factory)
    }
}
