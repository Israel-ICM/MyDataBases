package com.sphynxs.mydatabases.core.database.models

import android.os.Parcelable
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Configuración de conexión a una base de datos.
 *
 * Contiene todos los parámetros necesarios para establecer una conexión:
 * credenciales, timeouts, pool settings, etc.
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
 * @property sshTunnelConfig Configuración de túnel SSH (opcional)
 * @property connectionTimeout Timeout para establecer la conexión (ms)
 * @property readTimeout Timeout para ejecutar queries (ms)
 * @property maxPoolSize Número máximo de conexiones en el pool
 * @property createdAt Timestamp de creación de la configuración
 * @property lastUsedAt Timestamp del último uso (null si nunca se usó)
 * @author israel-icm
 * @date 2026-06-11
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
    val useSSL: Boolean = true,
    val sshTunnelConfig: SSHTunnelConfig? = null,
    val connectionTimeout: Long = 10_000L,
    val readTimeout: Long = 30_000L,
    val maxPoolSize: Int = 10,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null
) : Parcelable
