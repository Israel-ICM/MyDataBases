package com.sphynxs.mydatabases.core.di

import com.sphynxs.mydatabases.data.repositories.ConnectionRepositoryImpl
import com.sphynxs.mydatabases.data.repositories.FolderRepositoryImpl
import com.sphynxs.mydatabases.data.repositories.QueryFileStoreImpl
import com.sphynxs.mydatabases.data.repositories.SettingsRepositoryImpl
import com.sphynxs.mydatabases.domain.repositories.ConnectionRepository
import com.sphynxs.mydatabases.domain.repositories.FolderRepository
import com.sphynxs.mydatabases.domain.repositories.QueryFileStore
import com.sphynxs.mydatabases.domain.repositories.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt que vincula interfaces de repositorios con sus implementaciones.
 *
 * Usa @Binds para mapear contratos de dominio a implementaciones de datos.
 *
 * @author israel-icm
 * @date 2026-06-12 (updated 2026-06-30 for folders)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Vincula SettingsRepository con su implementación usando DataStore.
     *
     * @param impl Implementación del repositorio
     * @return Interfaz del repositorio
     */
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    /**
     * Vincula ConnectionRepository con su implementación usando Room.
     *
     * @param impl Implementación del repositorio
     * @return Interfaz del repositorio
     */
    @Binds
    @Singleton
    abstract fun bindConnectionRepository(
        impl: ConnectionRepositoryImpl
    ): ConnectionRepository
    
    /**
     * Vincula FolderRepository con su implementación usando Room.
     *
     * @param impl Implementación del repositorio
     * @return Interfaz del repositorio
     */
    @Binds
    @Singleton
    abstract fun bindFolderRepository(
        impl: FolderRepositoryImpl
    ): FolderRepository

    /**
     * Vincula QueryFileStore con su implementación única sobre la raíz resuelta por
     * QueryStorageResolver (privada o SAF) — change `query-files-storage`.
     *
     * @param impl Implementación del store
     * @return Interfaz del store
     */
    @Binds
    @Singleton
    abstract fun bindQueryFileStore(
        impl: QueryFileStoreImpl
    ): QueryFileStore
}
