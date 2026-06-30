package com.sphynxs.mydatabases.core.database.models

import android.os.Parcelable
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Configuración de conexión a una base de datos.
 *
 * Contiene todos los parámetros necesarios para establecer una conexión:
 * credenciales, timeouts, pool settings, SSL, SSH, etc.
 *
 * @property id Identificador único de la configuración
 * @property name Nombre descriptivo para el usuario
 * @property type Tipo de motor de base de datos
 * @property host Dirección del servidor (hostname o IP)
 * @property port Puerto del servidor
 * @property database Nombre de la base de datos
 * @property username Usuario para autenticación
 * @property password Contraseña (debe estar encriptada antes de persistir)
 * @property useSSL Si se debe usar SSL/TLS para la conexión
 * @property sslConfig Configuración SSL/TLS detallada (certificados, modo)
 * @property sshTunnelConfig Configuración de túnel SSH (opcional)
 * @property connectionString Connection string completa (sobreescribe host/port/user/pass si se proporciona)
 * @property connectionTimeout Timeout para establecer la conexión (ms)
 * @property readTimeout Timeout para ejecutar queries (ms)
 * @property maxPoolSize Número máximo de conexiones en el pool
 * @property createdAt Timestamp de creación de la configuración
 * @property lastUsedAt Timestamp del último uso (null si nunca se usó)
 * @author israel-icm
 * @date 2026-06-11 (updated 2026-06-30 for advanced connection options)
 */
@Parcelize
data class ConnectionConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: DatabaseType,
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    val password: String,
    val useSSL: Boolean = false,
    val sslConfig: SSLConfig? = null,
    val sshTunnelConfig: SSHTunnelConfig? = null,
    val connectionString: String? = null,
    val connectionTimeout: Long = 10_000L,
    val readTimeout: Long = 30_000L,
    val maxPoolSize: Int = 10,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null
) : Parcelable {
    
    /**
     * Indica si se debe usar connection string en vez de parámetros individuales.
     */
    val usesConnectionString: Boolean
        get() = !connectionString.isNullOrBlank()
    
    /**
     * Indica si tiene configuración avanzada (SSL con certificados, SSH, o connection string).
     */
    val hasAdvancedConfig: Boolean
        get() = sslConfig != null || sshTunnelConfig != null || usesConnectionString
}
