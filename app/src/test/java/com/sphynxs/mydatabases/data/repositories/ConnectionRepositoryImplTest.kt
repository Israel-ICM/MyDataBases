package com.sphynxs.mydatabases.data.repositories

import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.core.security.CredentialEncryption
import com.sphynxs.mydatabases.data.local.dao.ConnectionDao
import com.sphynxs.mydatabases.data.local.entities.ConnectionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests para [ConnectionRepositoryImpl].
 *
 * Verifica que el repositorio:
 * 1. Encripta passwords antes de guardar en Room
 * 2. Desencripta passwords al leer de Room
 * 3. Mapea correctamente entre ConnectionConfig y ConnectionEntity
 * 4. Delega correctamente al DAO
 *
 * @author israel-icm
 * @date 2026-06-12
 */
class ConnectionRepositoryImplTest {

    private lateinit var connectionDao: ConnectionDao
    private lateinit var credentialEncryption: CredentialEncryption
    private lateinit var repository: ConnectionRepositoryImpl

    @Before
    fun setUp() {
        connectionDao = mockk(relaxed = true)
        credentialEncryption = mockk()
        repository = ConnectionRepositoryImpl(connectionDao, credentialEncryption)
    }

    @Test
    fun `save encrypts password before inserting to Room`() = runTest {
        // Given
        val config = ConnectionConfig(
            id = "conn-1",
            name = "Production MySQL",
            type = DatabaseType.MYSQL,
            host = "localhost",
            port = 3306,
            database = "mydb",
            username = "admin",
            password = "secretPassword123"
        )

        val encryptedKey = "encrypted-uuid-12345"
        every { credentialEncryption.encrypt("secretPassword123") } returns encryptedKey

        val capturedEntity = slot<ConnectionEntity>()
        coEvery { connectionDao.insert(capture(capturedEntity)) } returns Unit

        // When
        repository.save(config)

        // Then
        coVerify(exactly = 1) { credentialEncryption.encrypt("secretPassword123") }
        coVerify(exactly = 1) { connectionDao.insert(any()) }

        assertEquals(encryptedKey, capturedEntity.captured.encryptedPassword)
        assertEquals("conn-1", capturedEntity.captured.id)
        assertEquals("Production MySQL", capturedEntity.captured.name)
    }

    @Test
    fun `getAll decrypts passwords from Room entities`() = runTest {
        // Given
        val entity1 = ConnectionEntity(
            id = "conn-1",
            name = "MySQL Prod",
            type = DatabaseType.MYSQL,
            host = "192.168.1.10",
            port = 3306,
            database = "proddb",
            username = "admin",
            encryptedPassword = "encrypted-key-1",
            useSSL = true,
            sshTunnelConfig = null,
            connectionTimeout = 10_000L,
            readTimeout = 30_000L,
            maxPoolSize = 10,
            createdAt = 1000L,
            lastUsedAt = 2000L
        )

        every { connectionDao.getAll() } returns flowOf(listOf(entity1))
        every { credentialEncryption.decrypt("encrypted-key-1") } returns "plainPassword1"

        // When
        val result = repository.getAll().first()

        // Then
        assertEquals(1, result.size)
        val config = result[0]
        assertEquals("conn-1", config.id)
        assertEquals("MySQL Prod", config.name)
        assertEquals("plainPassword1", config.password)
    }

    @Test
    fun `getById returns null when connection not found`() = runTest {
        // Given
        coEvery { connectionDao.getById("nonexistent") } returns null

        // When
        val result = repository.getById("nonexistent")

        // Then
        assertNull(result)
    }

    @Test
    fun `getById decrypts password when connection exists`() = runTest {
        // Given
        val entity = ConnectionEntity(
            id = "conn-42",
            name = "Test DB",
            type = DatabaseType.MARIADB,
            host = "localhost",
            port = 3307,
            database = "testdb",
            username = "tester",
            encryptedPassword = "encrypted-key-42",
            useSSL = false,
            sshTunnelConfig = null,
            connectionTimeout = 5000L,
            readTimeout = 15000L,
            maxPoolSize = 5,
            createdAt = 500L,
            lastUsedAt = null
        )

        coEvery { connectionDao.getById("conn-42") } returns entity
        every { credentialEncryption.decrypt("encrypted-key-42") } returns "decryptedPass42"

        // When
        val result = repository.getById("conn-42")

        // Then
        assertNotNull(result)
        assertEquals("conn-42", result!!.id)
        assertEquals("Test DB", result.name)
        assertEquals("decryptedPass42", result.password)
    }

    @Test
    fun `delete delegates to DAO`() = runTest {
        // Given
        val connectionId = "conn-to-delete"
        coEvery { connectionDao.deleteById(connectionId) } returns Unit

        // When
        repository.delete(connectionId)

        // Then
        coVerify(exactly = 1) { connectionDao.deleteById(connectionId) }
    }

    @Test
    fun `testConnection returns success when connection succeeds`() = runTest {
        // Given
        val config = ConnectionConfig(
            id = "test-conn",
            name = "Test Connection",
            type = DatabaseType.MYSQL,
            host = "localhost",
            port = 3306,
            database = "test",
            username = "root",
            password = "password"
        )

        // When
        val result = repository.testConnection(config)

        // Then
        // For MVP, testConnection always returns success (real connection test deferred)
        assertTrue(result.isSuccess)
    }
}
