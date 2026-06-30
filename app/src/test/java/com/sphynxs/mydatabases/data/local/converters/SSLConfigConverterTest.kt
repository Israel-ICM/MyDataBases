package com.sphynxs.mydatabases.data.local.converters

import com.sphynxs.mydatabases.core.database.models.SSLConfig
import com.sphynxs.mydatabases.core.database.models.SSLMode
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests para SSLConfigConverter.
 *
 * Prueba serialización/deserialización de SSL config a JSON.
 *
 * @author israel-icm
 * @date 2026-06-30
 */
class SSLConfigConverterTest {

    private lateinit var converter: SSLConfigConverter

    @Before
    fun setup() {
        converter = SSLConfigConverter()
    }

    @Test
    fun `fromSSLConfig with complete config returns JSON string`() {
        // Given
        val config = SSLConfig(
            mode = SSLMode.VERIFY_CA,
            caCertificateUri = "content://com.android.providers.downloads/ca-cert.pem",
            clientCertificateUri = "content://com.android.providers.downloads/client-cert.pem",
            clientKeyUri = "content://com.android.providers.downloads/client-key.pem"
        )

        // When
        val json = converter.fromSSLConfig(config)

        // Then
        assertNotNull(json)
        assertTrue(json!!.contains("\"mode\":\"VERIFY_CA\""))
        assertTrue(json.contains("\"caCertificateUri\":\"content://com.android.providers.downloads/ca-cert.pem\""))
        assertTrue(json.contains("\"clientCertificateUri\":\"content://com.android.providers.downloads/client-cert.pem\""))
        assertTrue(json.contains("\"clientKeyUri\":\"content://com.android.providers.downloads/client-key.pem\""))
    }

    @Test
    fun `fromSSLConfig with null config returns null`() {
        // When
        val json = converter.fromSSLConfig(null)

        // Then
        assertNull(json)
    }

    @Test
    fun `fromSSLConfig with only CA certificate includes only caCertificateUri`() {
        // Given
        val config = SSLConfig(
            mode = SSLMode.VERIFY_CA,
            caCertificateUri = "content://com.android.providers.downloads/ca-cert.pem",
            clientCertificateUri = null,
            clientKeyUri = null
        )

        // When
        val json = converter.fromSSLConfig(config)

        // Then
        assertNotNull(json)
        assertTrue(json!!.contains("\"caCertificateUri\""))
        assertFalse(json.contains("\"clientCertificateUri\""))
        assertFalse(json.contains("\"clientKeyUri\""))
    }

    @Test
    fun `toSSLConfig with valid JSON returns config`() {
        // Given
        val json = """
            {
                "mode": "VERIFY_IDENTITY",
                "caCertificateUri": "content://com.android.providers.downloads/ca-cert.pem",
                "clientCertificateUri": "content://com.android.providers.downloads/client-cert.pem",
                "clientKeyUri": "content://com.android.providers.downloads/client-key.pem"
            }
        """.trimIndent()

        // When
        val config = converter.toSSLConfig(json)

        // Then
        assertNotNull(config)
        assertEquals(SSLMode.VERIFY_IDENTITY, config!!.mode)
        assertEquals("content://com.android.providers.downloads/ca-cert.pem", config.caCertificateUri)
        assertEquals("content://com.android.providers.downloads/client-cert.pem", config.clientCertificateUri)
        assertEquals("content://com.android.providers.downloads/client-key.pem", config.clientKeyUri)
    }

    @Test
    fun `toSSLConfig with null JSON returns null`() {
        // When
        val config = converter.toSSLConfig(null)

        // Then
        assertNull(config)
    }

    @Test
    fun `toSSLConfig with invalid JSON returns null`() {
        // Given
        val invalidJson = "{ this is not valid JSON }"

        // When
        val config = converter.toSSLConfig(invalidJson)

        // Then
        assertNull(config)
    }

    @Test
    fun `toSSLConfig with missing optional fields returns config with nulls`() {
        // Given - Solo mode, sin certificados
        val json = """
            {
                "mode": "REQUIRED"
            }
        """.trimIndent()

        // When
        val config = converter.toSSLConfig(json)

        // Then
        assertNotNull(config)
        assertEquals(SSLMode.REQUIRED, config!!.mode)
        assertNull(config.caCertificateUri)
        assertNull(config.clientCertificateUri)
        assertNull(config.clientKeyUri)
    }

    @Test
    fun `roundtrip serialization preserves all fields`() {
        // Given
        val original = SSLConfig(
            mode = SSLMode.VERIFY_CA,
            caCertificateUri = "content://com.android.providers.downloads/ca-cert.pem",
            clientCertificateUri = "content://com.android.providers.downloads/client-cert.pem",
            clientKeyUri = "content://com.android.providers.downloads/client-key.pem"
        )

        // When
        val json = converter.fromSSLConfig(original)
        val restored = converter.toSSLConfig(json)

        // Then
        assertNotNull(restored)
        assertEquals(original.mode, restored!!.mode)
        assertEquals(original.caCertificateUri, restored.caCertificateUri)
        assertEquals(original.clientCertificateUri, restored.clientCertificateUri)
        assertEquals(original.clientKeyUri, restored.clientKeyUri)
    }

    @Test
    fun `fromSSLConfig with all SSL modes serializes correctly`() {
        // Test all SSL modes
        val modes = listOf(
            SSLMode.DISABLED,
            SSLMode.PREFERRED,
            SSLMode.REQUIRED,
            SSLMode.VERIFY_CA,
            SSLMode.VERIFY_IDENTITY
        )

        modes.forEach { mode ->
            // Given
            val config = SSLConfig(mode = mode)

            // When
            val json = converter.fromSSLConfig(config)

            // Then
            assertNotNull("JSON should not be null for mode $mode", json)
            assertTrue("JSON should contain mode $mode", json!!.contains("\"mode\":\"${mode.name}\""))
        }
    }
}
