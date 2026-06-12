package com.sphynxs.mydatabases.core.database.models

import android.os.Parcel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for ConnectionConfig Parcelable implementation.
 *
 * Tests verify:
 * - Parcelable roundtrip preserves all fields
 * - Default values are applied correctly
 * - Generated UUIDs are unique
 *
 * @author israel-icm
 * @date 2026-06-11
 */
@RunWith(RobolectricTestRunner::class)
class ConnectionConfigTest {

    @Test
    fun `parcelable roundtrip preserves all fields`() {
        // Given: A ConnectionConfig with all fields populated
        val original = ConnectionConfig(
            id = "test-uuid-123",
            name = "Production DB",
            type = DatabaseType.MYSQL,
            host = "db.example.com",
            port = 3306,
            database = "myapp",
            username = "admin",
            password = "encrypted_password",
            useSSL = true,
            sshTunnelConfig = null,
            connectionTimeout = 10_000L,
            readTimeout = 30_000L,
            maxPoolSize = 10,
            createdAt = 1702345678000L,
            lastUsedAt = 1702345679000L
        )

        // When: Parcelable roundtrip
        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val restored = ConnectionConfig.CREATOR.createFromParcel(parcel)
        parcel.recycle()

        // Then: All fields are preserved
        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.type, restored.type)
        assertEquals(original.host, restored.host)
        assertEquals(original.port, restored.port)
        assertEquals(original.database, restored.database)
        assertEquals(original.username, restored.username)
        assertEquals(original.password, restored.password)
        assertEquals(original.useSSL, restored.useSSL)
        assertEquals(original.sshTunnelConfig, restored.sshTunnelConfig)
        assertEquals(original.connectionTimeout, restored.connectionTimeout)
        assertEquals(original.readTimeout, restored.readTimeout)
        assertEquals(original.maxPoolSize, restored.maxPoolSize)
        assertEquals(original.createdAt, restored.createdAt)
        assertEquals(original.lastUsedAt, restored.lastUsedAt)
    }

    @Test
    fun `default values are applied correctly`() {
        // Given: A minimal ConnectionConfig using defaults
        val config = ConnectionConfig(
            name = "Test DB",
            type = DatabaseType.MYSQL,
            host = "localhost",
            port = 3306,
            database = "test",
            username = "root",
            password = "secret"
        )

        // Then: Default values are present
        assertEquals(true, config.useSSL)
        assertEquals(null, config.sshTunnelConfig)
        assertEquals(10_000L, config.connectionTimeout)
        assertEquals(30_000L, config.readTimeout)
        assertEquals(10, config.maxPoolSize)
        assert(config.id.isNotBlank()) // UUID generated
        assert(config.createdAt > 0) // Timestamp generated
        assertEquals(null, config.lastUsedAt)
    }

    @Test
    fun `generated UUIDs are unique`() {
        // Given: Two ConnectionConfigs created without explicit IDs
        val config1 = ConnectionConfig(
            name = "DB1",
            type = DatabaseType.MYSQL,
            host = "host1",
            port = 3306,
            database = "db1",
            username = "user1",
            password = "pass1"
        )

        val config2 = ConnectionConfig(
            name = "DB2",
            type = DatabaseType.MARIADB,
            host = "host2",
            port = 3307,
            database = "db2",
            username = "user2",
            password = "pass2"
        )

        // Then: IDs are unique
        assert(config1.id != config2.id)
    }
}
