package com.sphynxs.mydatabases.core.database.ssh

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

/**
 * Unit tests para SSHKeyReader.
 *
 * Prueba lectura, validación y detección de claves privadas SSH.
 *
 * @author israel-icm
 * @date 2026-06-30
 */
class SSHKeyReaderTest {

    private val context: Context = mockk()
    private val contentResolver: ContentResolver = mockk()

    @Test
    fun `readPrivateKey with valid PEM RSA key returns bytes`() {
        // Given
        val validPemKey = """
            -----BEGIN RSA PRIVATE KEY-----
            MIIEpAIBAAKCAQEAw7eP8v3QZ5k...
            -----END RSA PRIVATE KEY-----
        """.trimIndent()
        
        val uri = Uri.parse("content://com.android.providers.downloads/12345")
        
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(validPemKey.toByteArray())
        
        // When
        val result = SSHKeyReader.readPrivateKey(context, uri)
        
        // Then
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertEquals(validPemKey, String(result, Charsets.UTF_8))
    }

    @Test
    fun `readPrivateKey with valid OpenSSH key returns bytes`() {
        // Given
        val validOpenSshKey = """
            -----BEGIN OPENSSH PRIVATE KEY-----
            b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAABlwAAAAdzc2gtcn
            -----END OPENSSH PRIVATE KEY-----
        """.trimIndent()
        
        val uri = Uri.parse("content://com.android.providers.downloads/12346")
        
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(validOpenSshKey.toByteArray())
        
        // When
        val result = SSHKeyReader.readPrivateKey(context, uri)
        
        // Then
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertEquals(validOpenSshKey, String(result, Charsets.UTF_8))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `readPrivateKey with invalid format throws exception`() {
        // Given
        val invalidKey = "This is not a valid SSH key"
        val uri = Uri.parse("content://com.android.providers.downloads/12347")
        
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(invalidKey.toByteArray())
        
        // When
        SSHKeyReader.readPrivateKey(context, uri)
        
        // Then - exception thrown
    }

    @Test(expected = IllegalStateException::class)
    fun `readPrivateKey with encrypted key throws exception`() {
        // Given
        val encryptedKey = """
            -----BEGIN RSA PRIVATE KEY-----
            Proc-Type: 4,ENCRYPTED
            DEK-Info: AES-128-CBC,1234567890ABCDEF
            
            MIIEpAIBAAKCAQEAw7eP8v3QZ5k...
            -----END RSA PRIVATE KEY-----
        """.trimIndent()
        
        val uri = Uri.parse("content://com.android.providers.downloads/12348")
        
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(encryptedKey.toByteArray())
        
        // When
        SSHKeyReader.readPrivateKey(context, uri)
        
        // Then - exception thrown
    }

    @Test(expected = IllegalArgumentException::class)
    fun `readPrivateKey with null input stream throws exception`() {
        // Given
        val uri = Uri.parse("content://com.android.providers.downloads/99999")
        
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns null
        
        // When
        SSHKeyReader.readPrivateKey(context, uri)
        
        // Then - exception thrown
    }

    @Test
    fun `isEncrypted with encrypted PEM returns true`() {
        // Given
        val encryptedKey = """
            -----BEGIN RSA PRIVATE KEY-----
            Proc-Type: 4,ENCRYPTED
            DEK-Info: AES-128-CBC,1234567890ABCDEF
        """.trimIndent()
        
        // When
        val result = SSHKeyReader.isEncrypted(encryptedKey)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `isEncrypted with ENCRYPTED keyword returns true`() {
        // Given
        val encryptedKey = """
            -----BEGIN ENCRYPTED PRIVATE KEY-----
            MIIFHDBOBgkqhkiG9w0BBQ0wQTApBgkqhkiG9w0BBQwwHAQI...
        """.trimIndent()
        
        // When
        val result = SSHKeyReader.isEncrypted(encryptedKey)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `isEncrypted with unencrypted key returns false`() {
        // Given
        val unencryptedKey = """
            -----BEGIN RSA PRIVATE KEY-----
            MIIEpAIBAAKCAQEAw7eP8v3QZ5k...
            -----END RSA PRIVATE KEY-----
        """.trimIndent()
        
        // When
        val result = SSHKeyReader.isEncrypted(unencryptedKey)
        
        // Then
        assertFalse(result)
    }

    @Test
    fun `isValidPrivateKey with RSA PEM returns true`() {
        // Given
        val rsaKey = "-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEA..."
        
        // When
        val result = SSHKeyReader.isValidPrivateKey(rsaKey)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `isValidPrivateKey with PKCS8 PEM returns true`() {
        // Given
        val pkcs8Key = "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG..."
        
        // When
        val result = SSHKeyReader.isValidPrivateKey(pkcs8Key)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `isValidPrivateKey with OpenSSH format returns true`() {
        // Given
        val opensshKey = "-----BEGIN OPENSSH PRIVATE KEY-----\nb3BlbnNzaC1rZXktdjE..."
        
        // When
        val result = SSHKeyReader.isValidPrivateKey(opensshKey)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `isValidPrivateKey with EC key returns true`() {
        // Given
        val ecKey = "-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIIGlRkwrN..."
        
        // When
        val result = SSHKeyReader.isValidPrivateKey(ecKey)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `isValidPrivateKey with DSA key returns true`() {
        // Given
        val dsaKey = "-----BEGIN DSA PRIVATE KEY-----\nMIIBuwIBAAKBgQD9f1OB..."
        
        // When
        val result = SSHKeyReader.isValidPrivateKey(dsaKey)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `isValidPrivateKey with invalid content returns false`() {
        // Given
        val invalidKey = "This is just a random text file"
        
        // When
        val result = SSHKeyReader.isValidPrivateKey(invalidKey)
        
        // Then
        assertFalse(result)
    }

    @Test
    fun `isValidPrivateKey with public key returns false`() {
        // Given
        val publicKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC..."
        
        // When
        val result = SSHKeyReader.isValidPrivateKey(publicKey)
        
        // Then
        assertFalse(result)
    }

    @Test
    fun `isValidPrivateKey with empty string returns false`() {
        // When
        val result = SSHKeyReader.isValidPrivateKey("")
        
        // Then
        assertFalse(result)
    }
}
