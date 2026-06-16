package com.sphynxs.mydatabases.core.database.models

import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Tests para ConnectionConfig.
 *
 * @author israel-icm
 * @date 2026-06-11
 */
class ConnectionConfigTest {

    @Test
    fun `ConnectionConfig creation with all fields works`() {
        // GIVEN: Una configuración de conexión válida
        val config = ConnectionConfig(
            id = "test-id-123",
            name = "Test DB",
            type = DatabaseType.MYSQL,
            host = "localhost",
            port = 3306,
            database = "test_db",
            username = "test_user",
            password = "test_password",
            useSSL = true,
            sshTunnelConfig = null,
            connectionTimeout = 10_000L,
            readTimeout = 30_000L,
            maxPoolSize = 10,
            createdAt = 1234567890L,
            lastUsedAt = 9876543210L
        )

        // THEN: Todos los campos se asignan correctamente
        assertEquals("test-id-123", config.id)
        assertEquals("Test DB", config.name)
        assertEquals(DatabaseType.MYSQL, config.type)
        assertEquals("localhost", config.host)
        assertEquals(3306, config.port)
        assertEquals("test_db", config.database)
        assertEquals("test_user", config.username)
        assertEquals("test_password", config.password)
        assertEquals(true, config.useSSL)
        assertEquals(null, config.sshTunnelConfig)
        assertEquals(10_000L, config.connectionTimeout)
        assertEquals(30_000L, config.readTimeout)
        assertEquals(10, config.maxPoolSize)
        assertEquals(1234567890L, config.createdAt)
        assertEquals(9876543210L, config.lastUsedAt)
    }

    @Test
    fun `ConnectionConfig handles different DatabaseType values`() {
        // GIVEN: Configuración con MariaDB y valores diferentes
        val config = ConnectionConfig(
            id = "mariadb-config",
            name = "Production DB",
            type = DatabaseType.MARIADB,
            host = "db.example.com",
            port = 5432,
            database = "prod_db",
            username = "admin",
            password = "encrypted_pwd",
            useSSL = false,
            sshTunnelConfig = null,
            connectionTimeout = 5_000L,
            readTimeout = 60_000L,
            maxPoolSize = 20,
            createdAt = 1111111111L,
            lastUsedAt = null
        )

        // THEN: Los valores se asignan correctamente
        assertEquals("mariadb-config", config.id)
        assertEquals(DatabaseType.MARIADB, config.type)
        assertEquals(5432, config.port)
        assertEquals(false, config.useSSL)
        assertEquals(null, config.lastUsedAt)
    }
}
