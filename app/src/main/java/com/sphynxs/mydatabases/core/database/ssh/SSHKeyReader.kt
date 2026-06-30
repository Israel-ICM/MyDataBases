package com.sphynxs.mydatabases.core.database.ssh

import android.content.Context
import android.net.Uri

/**
 * Utility for reading and validating SSH private keys from Android URIs.
 *
 * Supports standard SSH key formats:
 * - PEM format: `-----BEGIN RSA PRIVATE KEY-----` or `-----BEGIN PRIVATE KEY-----`
 * - OpenSSH format: `-----BEGIN OPENSSH PRIVATE KEY-----`
 *
 * **Limitations**:
 * - Encrypted keys (with passphrase) are NOT supported in v1.0
 * - Keys must be unencrypted or use password authentication instead
 *
 * Usage:
 * ```kotlin
 * val keyBytes = SSHKeyReader.readPrivateKey(context, keyUri)
 * if (SSHKeyReader.isEncrypted(keyContent)) {
 *     // Show error: encrypted keys not supported
 * }
 * ```
 *
 * @author israel-icm
 * @date 2026-06-30
 */
object SSHKeyReader {
    
    /**
     * Reads a private key from an Android URI and returns raw bytes for JSch.
     *
     * The URI typically comes from Android Storage Access Framework (SAF)
     * file picker (content:// scheme).
     *
     * @param context Android context for accessing ContentResolver
     * @param uri URI to the private key file
     * @return Private key content as byte array
     * @throws IllegalArgumentException if URI cannot be read or key format is invalid
     * @throws IllegalStateException if key is encrypted (has passphrase)
     */
    fun readPrivateKey(context: Context, uri: Uri): ByteArray {
        val keyContent = try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: throw IllegalArgumentException("Cannot open private key from URI: $uri")
        } catch (e: Exception) {
            throw IllegalArgumentException("Error reading private key from URI: ${e.message}", e)
        }
        
        // Validate key format
        if (!isValidPrivateKey(keyContent)) {
            throw IllegalArgumentException(
                "Invalid private key format. Expected PEM or OpenSSH format with " +
                "-----BEGIN ... PRIVATE KEY----- header."
            )
        }
        
        // Check if encrypted
        if (isEncrypted(keyContent)) {
            throw IllegalStateException(
                "Encrypted SSH keys (with passphrase) are not supported in v1.0. " +
                "Please use an unencrypted private key or password authentication."
            )
        }
        
        return keyContent.toByteArray(Charsets.UTF_8)
    }
    
    /**
     * Checks if a private key is encrypted (requires passphrase).
     *
     * Detects common encryption markers in PEM format:
     * - "ENCRYPTED" keyword
     * - "Proc-Type: 4,ENCRYPTED" header (traditional PEM)
     *
     * @param keyContent Private key content as String
     * @return true if the key appears to be encrypted
     */
    fun isEncrypted(keyContent: String): Boolean {
        return keyContent.contains("ENCRYPTED", ignoreCase = true) ||
               keyContent.contains("Proc-Type: 4,ENCRYPTED", ignoreCase = true)
    }
    
    /**
     * Validates that the content appears to be a valid SSH private key.
     *
     * Checks for standard PEM/OpenSSH headers:
     * - `-----BEGIN RSA PRIVATE KEY-----` (PEM RSA)
     * - `-----BEGIN PRIVATE KEY-----` (PEM PKCS#8)
     * - `-----BEGIN OPENSSH PRIVATE KEY-----` (OpenSSH format)
     *
     * @param keyContent Private key content as String
     * @return true if the content has a valid private key header
     */
    fun isValidPrivateKey(keyContent: String): Boolean {
        return keyContent.contains("-----BEGIN RSA PRIVATE KEY-----") ||
               keyContent.contains("-----BEGIN PRIVATE KEY-----") ||
               keyContent.contains("-----BEGIN OPENSSH PRIVATE KEY-----") ||
               keyContent.contains("-----BEGIN EC PRIVATE KEY-----") ||  // Elliptic Curve keys
               keyContent.contains("-----BEGIN DSA PRIVATE KEY-----")    // DSA keys
    }
}
