package com.sphynxs.mydatabases.data.local.converters

import com.sphynxs.mydatabases.core.database.models.SSHAuthMethod
import com.sphynxs.mydatabases.core.database.models.SSHTunnelConfig
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests para SSHTunnelConfigConverter.
 *
 * Prueba serialización/deserialización de SSH config a JSON.
 *
 * @author israel-icm
 * @date 2026-06-30
 */
class SSHTunnelConfigConverterTest {

    private lateinit var converter: SSHTunnelConfigConverter

    @Before
    fun setup() {
        converter = SSHTunnelConfigConverter()
    }

    @Test
    fun `fromSSHTunnelConfig with complete config returns JSON string`() {
        // Given
        val config = SSHTunnelConfig(
            enabled = true,
            host = "bastion.example.com",
            port = 22,
            username = "sshuser",
            authMethod = SSHAuthMethod.PASSWORD,
            password = "encrypted-password-123",
            privateKeyUri = null
        )

        // When
        val json = converter.fromSSHTunnelConfig(config)

        // Then
        assertNotNull(json)
        assertTrue(json!!.contains("\"enabled\":true"))
        assertTrue(json.contains("\"host\":\"bastion.example.com\""))
        assertTrue(json.contains("\"port\":22"))
        assertTrue(json.contains("\"username\":\"sshuser\""))
        assertTrue(json.contains("\"authMethod\":\"PASSWORD\""))
        assertTrue(json.contains("\"password\":\"encrypted-password-123\""))
    }

    @Test
    fun `fromSSHTunnelConfig with private key auth includes privateKeyUri`() {
        // Given
        val config = SSHTunnelConfig(
            enabled = true,
            host = "bastion.example.com",
            port = 22,
            username = "sshuser",
            authMethod = SSHAuthMethod.PRIVATE_KEY,
            password = "",
            privateKeyUri = "content://com.android.providers.downloads/key.pem"
        )

        // When
        val json = converter.fromSSHTunnelConfig(config)

        // Then
        assertNotNull(json)
        assertTrue(json!!.contains("\"authMethod\":\"PRIVATE_KEY\""))
        assertTrue(json.contains("\"privateKeyUri\":\"content://com.android.providers.downloads/key.pem\""))
    }

    @Test
    fun `fromSSHTunnelConfig with null config returns null`() {
        // When
        val json = converter.fromSSHTunnelConfig(null)

        // Then
        assertNull(json)
    }

    @Test
    fun `toSSHTunnelConfig with valid JSON returns config`() {
        // Given
        val json = """
            {
                "enabled": true,
                "host": "bastion.example.com",
                "port": 22,
                "username": "sshuser",
                "authMethod": "PASSWORD",
                "password": "encrypted-password-123"
            }
        """.trimIndent()

        // When
        val config = converter.toSSHTunnelConfig(json)

        // Then
        assertNotNull(config)
        assertEquals(true, config!!.enabled)
        assertEquals("bastion.example.com", config.host)
        assertEquals(22, config.port)
        assertEquals("sshuser", config.username)
        assertEquals(SSHAuthMethod.PASSWORD, config.authMethod)
        assertEquals("encrypted-password-123", config.password)
        assertNull(config.privateKeyUri)
    }

    @Test
    fun `toSSHTunnelConfig with private key JSON returns config with URI`() {
        // Given
        val json = """
            {
                "enabled": true,
                "host": "bastion.example.com",
                "port": 22,
                "username": "sshuser",
                "authMethod": "PRIVATE_KEY",
                "password": "",
                "privateKeyUri": "content://com.android.providers.downloads/key.pem"
            }
        """.trimIndent()

        // When
        val config = converter.toSSHTunnelConfig(json)

        // Then
        assertNotNull(config)
        assertEquals(SSHAuthMethod.PRIVATE_KEY, config!!.authMethod)
        assertEquals("content://com.android.providers.downloads/key.pem", config.privateKeyUri)
    }

    @Test
    fun `toSSHTunnelConfig with null JSON returns null`() {
        // When
        val config = converter.toSSHTunnelConfig(null)

        // Then
        assertNull(config)
    }

    @Test
    fun `toSSHTunnelConfig with invalid JSON returns null`() {
        // Given
        val invalidJson = "{ this is not valid JSON }"

        // When
        val config = converter.toSSHTunnelConfig(invalidJson)

        // Then
        assertNull(config)
    }

    @Test
    fun `toSSHTunnelConfig with missing enabled defaults to false`() {
        // Given - JSON sin campo enabled
        val json = """
            {
                "host": "bastion.example.com",
                "port": 22,
                "username": "sshuser",
                "authMethod": "PASSWORD",
                "password": "encrypted-password-123"
            }
        """.trimIndent()

        // When
        val config = converter.toSSHTunnelConfig(json)

        // Then
        assertNotNull(config)
        assertEquals(false, config!!.enabled)
    }

    @Test
    fun `toSSHTunnelConfig with missing authMethod defaults to PASSWORD`() {
        // Given - JSON sin authMethod
        val json = """
            {
                "enabled": true,
                "host": "bastion.example.com",
                "port": 22,
                "username": "sshuser",
                "password": "encrypted-password-123"
            }
        """.trimIndent()

        // When
        val config = converter.toSSHTunnelConfig(json)

        // Then
        assertNotNull(config)
        assertEquals(SSHAuthMethod.PASSWORD, config!!.authMethod)
    }

    @Test
    fun `roundtrip serialization preserves all fields`() {
        // Given
        val original = SSHTunnelConfig(
            enabled = true,
            host = "bastion.example.com",
            port = 2222,
            username = "sshuser",
            authMethod = SSHAuthMethod.PRIVATE_KEY,
            password = "encrypted-password",
            privateKeyUri = "content://com.android.providers.downloads/key.pem"
        )

        // When
        val json = converter.fromSSHTunnelConfig(original)
        val restored = converter.toSSHTunnelConfig(json)

        // Then
        assertNotNull(restored)
        assertEquals(original.enabled, restored!!.enabled)
        assertEquals(original.host, restored.host)
        assertEquals(original.port, restored.port)
        assertEquals(original.username, restored.username)
        assertEquals(original.authMethod, restored.authMethod)
        assertEquals(original.password, restored.password)
        assertEquals(original.privateKeyUri, restored.privateKeyUri)
    }
}
