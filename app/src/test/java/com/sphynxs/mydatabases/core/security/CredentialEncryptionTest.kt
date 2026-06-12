package com.sphynxs.mydatabases.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests para [CredentialEncryption] — ciclo RED → GREEN → TRIANGULATE.
 *
 * Valida que las contraseñas se encripten correctamente usando EncryptedSharedPreferences
 * y que se puedan desencriptar de vuelta al texto original.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@RunWith(RobolectricTestRunner::class)
class CredentialEncryptionTest {

    private lateinit var context: Context
    private lateinit var credentialEncryption: CredentialEncryption

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        credentialEncryption = CredentialEncryption(context)
    }

    /**
     * RED: Este test va a fallar porque CredentialEncryption aún no existe.
     *
     * Escenario: Encriptar una contraseña plaintext debe producir texto cifrado
     * diferente al original.
     */
    @Test
    fun `encrypt() debe retornar texto diferente al plaintext`() {
        // Given
        val plainPassword = "mySecurePassword123"

        // When
        val encrypted = credentialEncryption.encrypt(plainPassword)

        // Then
        assertNotNull("El texto encriptado no debe ser null", encrypted)
        assertNotEquals(
            "El texto encriptado debe ser diferente al plaintext",
            plainPassword,
            encrypted
        )
    }

    /**
     * GREEN + TRIANGULATE: Round-trip con un segundo password diferente.
     *
     * Escenario: Encriptar → Desencriptar debe recuperar el plaintext original.
     */
    @Test
    fun `encrypt() y decrypt() deben hacer round-trip correctamente`() {
        // Given
        val plainPassword = "anotherPassword456"

        // When
        val encrypted = credentialEncryption.encrypt(plainPassword)
        val decrypted = credentialEncryption.decrypt(encrypted)

        // Then
        assertEquals(
            "El password desencriptado debe coincidir con el original",
            plainPassword,
            decrypted
        )
    }

    /**
     * TRIANGULATE: Tercer caso con password vacío.
     *
     * Escenario: Encriptar y desencriptar un string vacío debe funcionar.
     */
    @Test
    fun `encrypt() y decrypt() deben manejar password vacío`() {
        // Given
        val emptyPassword = ""

        // When
        val encrypted = credentialEncryption.encrypt(emptyPassword)
        val decrypted = credentialEncryption.decrypt(encrypted)

        // Then
        assertEquals("El password vacío desencriptado debe ser vacío", emptyPassword, decrypted)
    }

    /**
     * TRIANGULATE: Cuarto caso con caracteres especiales.
     *
     * Escenario: Passwords con caracteres especiales deben encriptarse/desencriptarse correctamente.
     */
    @Test
    fun `encrypt() y decrypt() deben manejar caracteres especiales`() {
        // Given
        val specialPassword = "p@ssw0rd!#\$%^&*()_+-=[]{}|;:',.<>?/~`"

        // When
        val encrypted = credentialEncryption.encrypt(specialPassword)
        val decrypted = credentialEncryption.decrypt(encrypted)

        // Then
        assertEquals(
            "El password con caracteres especiales debe round-trip correctamente",
            specialPassword,
            decrypted
        )
    }
}
