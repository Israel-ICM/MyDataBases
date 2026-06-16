package com.sphynxs.mydatabases.core.di

import android.content.Context
import com.sphynxs.mydatabases.core.security.CredentialEncryption
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt que provee servicios de seguridad.
 *
 * Provee la encriptación de credenciales para passwords y datos sensibles.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    /**
     * Provee el servicio de encriptación de credenciales como singleton.
     *
     * Usa Android Keystore + EncryptedSharedPreferences para guardar passwords.
     *
     * @param context Contexto de la aplicación
     * @return Instancia única de CredentialEncryption
     */
    @Provides
    @Singleton
    fun provideCredentialEncryption(
        @ApplicationContext context: Context
    ): CredentialEncryption {
        return CredentialEncryption(context)
    }
}
