package com.sphynxs.mydatabases.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.SSLConfig
import com.sphynxs.mydatabases.core.database.models.SSHTunnelConfig

/**
 * Entidad de Room para persistir conexiones de base de datos.
 *
 * Mapea [com.sphynxs.mydatabases.core.database.models.ConnectionConfig] a Room,
 * con el password encriptado usando [com.sphynxs.mydatabases.core.security.CredentialEncryption].
 *
 * **IMPORTANTE**: El campo [encryptedPassword] NO contiene el password en texto plano,
 * sino la key retornada por CredentialEncryption.encrypt(). Para obtener el plaintext,
 * hay que llamar a CredentialEncryption.decrypt(encryptedPassword).
 *
 * **Advanced Options**: sslConfig, sshTunnelConfig, and connectionString are serialized to JSON.
 * SSH passwords are also encrypted in the repository layer before being serialized.
 *
 * @property id ID único de la conexión
 * @property name Nombre descriptivo para el usuario
 * @property type Tipo de motor de base de datos
 * @property host Dirección del servidor
 * @property port Puerto del servidor
 * @property database Nombre de la base de datos
 * @property username Usuario para autenticación
 * @property encryptedPassword Key de EncryptedSharedPreferences (NO el password en plaintext)
 * @property useSSL Si se debe usar SSL/TLS
 * @property sslConfig Configuración SSL/TLS detallada (JSON serializado, null si no aplica)
 * @property sshTunnelConfig Configuración de túnel SSH (JSON serializado con password encriptado, null si no aplica)
 * @property connectionString Connection string opcional (sobreescribe host/port/user/pass si se proporciona)
 * @property connectionTimeout Timeout para establecer conexión (ms)
 * @property readTimeout Timeout para queries (ms)
 * @property maxPoolSize Tamaño máximo del connection pool
 * @property createdAt Timestamp de creación
 * @property lastUsedAt Timestamp del último uso (null si nunca se usó)
 * @property folderId ID del folder que contiene esta conexión (null = nivel root)
 * @property order Posición en la lista dentro del folder o en root (menor = arriba)
 * @author israel-icm
 * @date 2026-06-12 (updated 2026-06-30 for folders & ordering)
 */
@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: DatabaseType,
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    @ColumnInfo(name = "encrypted_password") val encryptedPassword: String,
    @ColumnInfo(name = "use_ssl") val useSSL: Boolean,
    @ColumnInfo(name = "ssl_config") val sslConfig: SSLConfig? = null,
    @ColumnInfo(name = "ssh_tunnel_config") val sshTunnelConfig: SSHTunnelConfig? = null,
    @ColumnInfo(name = "connection_string") val connectionString: String? = null,
    @ColumnInfo(name = "connection_timeout") val connectionTimeout: Long,
    @ColumnInfo(name = "read_timeout") val readTimeout: Long,
    @ColumnInfo(name = "max_pool_size") val maxPoolSize: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "last_used_at") val lastUsedAt: Long?,
    @ColumnInfo(name = "folder_id") val folderId: String? = null,
    val order: Int = 0
)
