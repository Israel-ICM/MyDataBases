package com.sphynxs.mydatabases.core.database.di

import com.sphynxs.mydatabases.core.database.engine.DatabaseEngineFactory
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
 * @date 2026-06-12
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
     * @param factory Factory para crear DatabaseEngine instances
     * @return DatabaseRepository implementación concreta (DatabaseRepositoryImpl)
     */
    @Provides
    @Singleton
    fun provideDatabaseRepository(
        factory: DatabaseEngineFactory
    ): DatabaseRepository {
        return DatabaseRepositoryImpl(factory)
    }
}
