package com.sphynxs.mydatabases.data.repositories

import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.core.security.CredentialEncryption
import com.sphynxs.mydatabases.data.local.dao.ConnectionDao
import com.sphynxs.mydatabases.data.local.entities.ConnectionEntity
import com.sphynxs.mydatabases.domain.repositories.ConnectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementación del repositorio de conexiones usando Room y CredentialEncryption.
 *
 * Encripta passwords automáticamente al guardar y los desencripta al leer.
 * Usa [ConnectionDao] para persistir y [CredentialEncryption] para seguridad.
 *
 * @property connectionDao DAO de Room para acceso a la tabla connections
 * @property credentialEncryption Servicio de encriptación/desencriptación
 * @author israel-icm
 * @date 2026-06-12
 */
class ConnectionRepositoryImpl @Inject constructor(
    private val connectionDao: ConnectionDao,
    private val credentialEncryption: CredentialEncryption
) : ConnectionRepository {

    override fun getAll(): Flow<List<ConnectionConfig>> {
        return connectionDao.getAll().map { entities ->
            entities.map { it.toDomain(credentialEncryption) }
        }
    }

    override suspend fun getById(id: String): ConnectionConfig? {
        return connectionDao.getById(id)?.toDomain(credentialEncryption)
    }

    override suspend fun save(config: ConnectionConfig) {
        val entity = config.toEntity(credentialEncryption)
        connectionDao.insert(entity)
    }

    override suspend fun delete(id: String) {
        connectionDao.deleteById(id)
    }

    override suspend fun testConnection(config: ConnectionConfig): Result<Unit> {
        // TODO: Implement real connection test using DatabaseEngine
        // For MVP, we just return success (real test deferred to integration with engine)
        return Result.success(Unit)
    }
    
    override suspend fun updateOrder(connectionId: String, order: Int) {
        connectionDao.updateOrder(connectionId, order)
    }
}

/**
 * Convierte un ConnectionConfig del dominio a ConnectionEntity de Room.
 *
 * Encripta passwords (database y SSH) usando [credentialEncryption].
 *
 * @param credentialEncryption El servicio de encriptación
 * @return La entidad de Room con passwords encriptados
 */
private fun ConnectionConfig.toEntity(
    credentialEncryption: CredentialEncryption
): ConnectionEntity {
    val encryptedPassword = credentialEncryption.encrypt(this.password)
    
    // Encrypt SSH password if SSH tunnel is configured
    val encryptedSshConfig = this.sshTunnelConfig?.let { ssh ->
        ssh.copy(
            password = credentialEncryption.encrypt(ssh.password)
        )
    }

    return ConnectionEntity(
        id = this.id,
        name = this.name,
        type = this.type,
        host = this.host,
        port = this.port,
        database = this.database,
        username = this.username,
        encryptedPassword = encryptedPassword,
        useSSL = this.useSSL,
        sslConfig = this.sslConfig,
        sshTunnelConfig = encryptedSshConfig,
        connectionString = this.connectionString,
        connectionTimeout = this.connectionTimeout,
        readTimeout = this.readTimeout,
        maxPoolSize = this.maxPoolSize,
        createdAt = this.createdAt,
        lastUsedAt = this.lastUsedAt,
        folderId = this.folderId,
        order = this.order
    )
}

/**
 * Convierte un ConnectionEntity de Room a ConnectionConfig del dominio.
 *
 * Desencripta passwords (database y SSH) usando [credentialEncryption].
 *
 * @param credentialEncryption El servicio de desencriptación
 * @return El modelo de dominio con passwords en plaintext
 */
private fun ConnectionEntity.toDomain(
    credentialEncryption: CredentialEncryption
): ConnectionConfig {
    val decryptedPassword = credentialEncryption.decrypt(this.encryptedPassword)
    
    // Decrypt SSH password if SSH tunnel is configured
    val decryptedSshConfig = this.sshTunnelConfig?.let { ssh ->
        ssh.copy(
            password = credentialEncryption.decrypt(ssh.password)
        )
    }

    return ConnectionConfig(
        id = this.id,
        name = this.name,
        type = this.type,
        host = this.host,
        port = this.port,
        database = this.database,
        username = this.username,
        password = decryptedPassword,
        useSSL = this.useSSL,
        sslConfig = this.sslConfig,
        sshTunnelConfig = decryptedSshConfig,
        connectionString = this.connectionString,
        connectionTimeout = this.connectionTimeout,
        readTimeout = this.readTimeout,
        maxPoolSize = this.maxPoolSize,
        createdAt = this.createdAt,
        lastUsedAt = this.lastUsedAt,
        folderId = this.folderId,
        order = this.order
    )
}
