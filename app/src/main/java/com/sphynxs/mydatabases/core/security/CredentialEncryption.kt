package com.sphynxs.mydatabases.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encripta y desencripta credenciales usando EncryptedSharedPreferences.
 *
 * Usa una master key generada en el Android Keystore y AES256-GCM para encriptar
 * passwords y otros datos sensibles antes de guardarlos en Room.
 *
 * **Uso**:
 * ```kotlin
 * val encryption = CredentialEncryption(context)
 * val encrypted = encryption.encrypt("myPassword")
 * val decrypted = encryption.decrypt(encrypted) // "myPassword"
 * ```
 *
 * @param context El contexto de la app para acceder a SharedPreferences
 * @author israel-icm
 * @date 2026-06-12
 */
class CredentialEncryption(private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val sharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Encripta un plaintext password.
     *
     * @param plaintext El password en texto plano
     * @return El password encriptado como string (base64 internamente por EncryptedSharedPrefs)
     */
    fun encrypt(plaintext: String): String {
        val key = generateUniqueKey()
        sharedPreferences.edit().putString(key, plaintext).apply()
        return key
    }

    /**
     * Desencripta un password previamente encriptado.
     *
     * @param encrypted El key retornado por [encrypt]
     * @return El password en texto plano original
     */
    fun decrypt(encrypted: String): String {
        return sharedPreferences.getString(encrypted, null)
            ?: throw IllegalArgumentException("Encrypted key not found: $encrypted")
    }

    /**
     * Genera una key única para guardar cada password encriptado.
     *
     * @return Un UUID como string
     */
    private fun generateUniqueKey(): String {
        return java.util.UUID.randomUUID().toString()
    }

    companion object {
        private const val PREFS_FILE_NAME = "mydatabases_secure_credentials"
    }
}
