package com.sphynxs.mydatabases.di

import com.sphynxs.mydatabases.ui.workspace.WorkspaceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Workspace Module — módulo Hilt para dependency injection del workspace.
 *
 * Provee el WorkspaceManager singleton que gestiona el estado del workspace multi-tab.
 *
 * @author israel-icm
 * @date 2026-06-15
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkspaceModule {
    @Provides
    @Singleton
    fun provideWorkspaceManager(): WorkspaceManager = WorkspaceManager()
}
