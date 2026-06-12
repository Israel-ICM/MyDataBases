package com.sphynxs.mydatabases.core.database.models

import android.os.Parcel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests para ConnectionConfig con soporte Parcelable.
 *
 * @author israel-icm
 * @date 2026-06-11
 */
@RunWith(RobolectricTestRunner::class)
class ConnectionConfigTest {

    @Test
    fun `ConnectionConfig parcelable roundtrip preserves all fields`() {
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

        // WHEN: Serializamos y deserializamos a través de Parcel
        val parcel = Parcel.obtain()
        config.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val fromParcel = ConnectionConfig.createFromParcel(parcel)
        parcel.recycle()

        // THEN: El objeto deserializado es idéntico al original
        assertEquals(config.id, fromParcel.id)
        assertEquals(config.name, fromParcel.name)
        assertEquals(config.type, fromParcel.type)
        assertEquals(config.host, fromParcel.host)
        assertEquals(config.port, fromParcel.port)
        assertEquals(config.database, fromParcel.database)
        assertEquals(config.username, fromParcel.username)
        assertEquals(config.password, fromParcel.password)
        assertEquals(config.useSSL, fromParcel.useSSL)
        assertEquals(config.sshTunnelConfig, fromParcel.sshTunnelConfig)
        assertEquals(config.connectionTimeout, fromParcel.connectionTimeout)
        assertEquals(config.readTimeout, fromParcel.readTimeout)
        assertEquals(config.maxPoolSize, fromParcel.maxPoolSize)
        assertEquals(config.createdAt, fromParcel.createdAt)
        assertEquals(config.lastUsedAt, fromParcel.lastUsedAt)
    }

    @Test
    fun `ConnectionConfig parcelable works with different values`() {
        // GIVEN: Una configuración diferente para triangulación
        val config = ConnectionConfig(
            id = "another-id-456",
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

        // WHEN: Serializamos y deserializamos
        val parcel = Parcel.obtain()
        config.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val fromParcel = ConnectionConfig.createFromParcel(parcel)
        parcel.recycle()

        // THEN: Los valores se preservan correctamente
        assertEquals(config.id, fromParcel.id)
        assertEquals(config.type, fromParcel.type)
        assertEquals(config.port, fromParcel.port)
        assertEquals(config.useSSL, fromParcel.useSSL)
        assertEquals(config.lastUsedAt, fromParcel.lastUsedAt)
    }
}
