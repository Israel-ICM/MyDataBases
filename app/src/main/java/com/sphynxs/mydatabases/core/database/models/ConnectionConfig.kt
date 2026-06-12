package com.sphynxs.mydatabases.core.database.models

import android.os.Parcelable
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Configuración de conexión a una base de datos.
 *
 * Contiene toda la información necesaria para establecer una conexión:
 * - Credenciales (host, port, database, username, password)
 * - Opciones de seguridad (SSL/TLS, SSH tunneling)
 * - Timeouts y pool settings
 * - Metadata (createdAt, lastUsedAt)
 *
 * Esta clase es Parcelable para poder pasarla entre Activities/Fragments.
 *
 * @property id Identificador único de la conexión (UUID auto-generado)
 * @property name Nombre descriptivo para mostrar en UI
 * @property type Tipo de motor de base de datos (MySQL, MariaDB, etc.)
 * @property host Hostname o IP del servidor
 * @property port Puerto del servidor
 * @property database Nombre de la base de datos
 * @property username Usuario para autenticación
 * @property password Contraseña encriptada (Android Keystore)
 * @property useSSL Habilitar conexión SSL/TLS
 * @property sshTunnelConfig Configuración de túnel SSH (opcional)
 * @property connectionTimeout Timeout en milisegundos para establecer conexión
 * @property readTimeout Timeout en milisegundos para queries
 * @property maxPoolSize Número máximo de conexiones en el pool
 * @property createdAt Timestamp de creación (milisegundos desde epoch)
 * @property lastUsedAt Timestamp del último uso (null si nunca se usó)
 *
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

/**
 * Configuración de túnel SSH para conexiones remotas.
 *
 * Permite conectar a bases de datos que solo son accesibles a través de un servidor SSH.
 *
 * @property sshHost Hostname del servidor SSH
 * @property sshPort Puerto del servidor SSH
 * @property sshUsername Usuario SSH
 * @property sshPassword Contraseña SSH (opcional si se usa key)
 * @property sshKeyPath Path a la clave privada SSH (opcional)
 * @property localPort Puerto local para el túnel
 * @property remoteHost Host remoto de la base de datos
 * @property remotePort Puerto remoto de la base de datos
 *
 * @author israel-icm
 * @date 2026-06-11
 */
@Parcelize
data class SSHTunnelConfig(
    val sshHost: String,
    val sshPort: Int = 22,
    val sshUsername: String,
    val sshPassword: String? = null,
    val sshKeyPath: String? = null,
    val localPort: Int,
    val remoteHost: String,
    val remotePort: Int
) : Parcelable
