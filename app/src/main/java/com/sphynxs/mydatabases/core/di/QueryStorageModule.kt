package com.sphynxs.mydatabases.core.di

import com.sphynxs.mydatabases.data.storage.DefaultQueryStorageRootProvider
import com.sphynxs.mydatabases.data.storage.QueryStorageRootProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt para el almacenamiento de query files (change `query-files-storage`).
 *
 * Vincula QueryStorageRootProvider con su implementación real basada en Context/DocumentFile.
 * `QueryStorageResolver` y `QueryFileStoreImpl` no necesitan binding propio — Hilt los
 * construye directamente vía sus constructores `@Inject`.
 *
 * @author sdd-apply
 * @date 2026-08-05
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class QueryStorageModule {

    @Binds
    @Singleton
    abstract fun bindQueryStorageRootProvider(
        impl: DefaultQueryStorageRootProvider
    ): QueryStorageRootProvider
}
