package com.sphynxs.mydatabases.core.database.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests para DatabaseError sealed class.
 *
 * @author israel-icm
 * @date 2026-06-11
 */
class DatabaseErrorTest {

    @Test
    fun `DatabaseError ConnectionFailed contains reason`() {
        // GIVEN: Un error de conexión fallida
        val error = DatabaseError.ConnectionFailed("Host unreachable: timeout exceeded")

        // THEN: El error tiene el mensaje correcto
        assertEquals("Host unreachable: timeout exceeded", error.reason)
        assertTrue(error is DatabaseError)
    }

    @Test
    fun `DatabaseError AuthenticationFailed contains reason`() {
        // GIVEN: Un error de autenticación fallida
        val error = DatabaseError.AuthenticationFailed("Invalid credentials")

        // THEN: El error tiene el mensaje correcto
        assertEquals("Invalid credentials", error.reason)
        assertTrue(error is DatabaseError)
    }

    @Test
    fun `DatabaseError QueryExecutionFailed contains query and reason`() {
        // GIVEN: Un error de ejecución de query
        val query = "SELECT * FROM non_existent_table"
        val reason = "Table 'test.non_existent_table' doesn't exist"
        val error = DatabaseError.QueryExecutionFailed(query, reason)

        // THEN: El error contiene query y razón
        assertEquals(query, error.query)
        assertEquals(reason, error.reason)
        assertTrue(error is DatabaseError)
    }

    @Test
    fun `DatabaseError TimeoutError contains operation`() {
        // GIVEN: Un error de timeout
        val error = DatabaseError.TimeoutError("Connecting to database")

        // THEN: El error indica la operación que falló
        assertEquals("Connecting to database", error.operation)
        assertTrue(error is DatabaseError)
    }

    @Test
    fun `DatabaseError InvalidConfiguration contains field and reason`() {
        // GIVEN: Un error de configuración inválida
        val error = DatabaseError.InvalidConfiguration("port", "Port must be between 1 and 65535")

        // THEN: El error indica el campo y la razón
        assertEquals("port", error.field)
        assertEquals("Port must be between 1 and 65535", error.reason)
        assertTrue(error is DatabaseError)
    }

    @Test
    fun `DatabaseError UnknownError wraps throwable`() {
        // GIVEN: Un error desconocido que envuelve una excepción
        val cause = RuntimeException("Unexpected error")
        val error = DatabaseError.UnknownError(cause)

        // THEN: El error contiene el throwable original
        assertEquals(cause, error.throwable)
        assertEquals("Unexpected error", error.throwable.message)
        assertTrue(error is DatabaseError)
    }
}
