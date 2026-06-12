package com.sphynxs.mydatabases.core.database.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for DatabaseError sealed class.
 *
 * Tests verify:
 * - All error cases are instantiable
 * - Error messages are preserved
 * - Sealed class hierarchy works correctly
 *
 * @author israel-icm
 * @date 2026-06-11
 */
class DatabaseErrorTest {

    @Test
    fun `ConnectionFailed error preserves reason`() {
        // Given: A ConnectionFailed error with a specific reason
        val reason = "Host 'db.example.com' is unreachable"
        val error = DatabaseError.ConnectionFailed(reason)

        // Then: Reason is preserved
        assertEquals(reason, error.reason)
        assertTrue(error is DatabaseError)
        assertTrue(error is Throwable)
    }

    @Test
    fun `AuthenticationFailed error preserves reason`() {
        // Given: An AuthenticationFailed error
        val reason = "Access denied for user 'admin'"
        val error = DatabaseError.AuthenticationFailed(reason)

        // Then: Reason is preserved
        assertEquals(reason, error.reason)
        assertTrue(error is DatabaseError)
    }

    @Test
    fun `QueryExecutionFailed error preserves query and reason`() {
        // Given: A QueryExecutionFailed error
        val query = "SELECT * FROM users WHERE id = ?"
        val reason = "Table 'myapp.users' doesn't exist"
        val error = DatabaseError.QueryExecutionFailed(query, reason)

        // Then: Both query and reason are preserved
        assertEquals(query, error.query)
        assertEquals(reason, error.reason)
        assertTrue(error is DatabaseError)
    }

    @Test
    fun `TimeoutError error preserves operation`() {
        // Given: A TimeoutError
        val operation = "executeQuery"
        val error = DatabaseError.TimeoutError(operation)

        // Then: Operation is preserved
        assertEquals(operation, error.operation)
        assertTrue(error is DatabaseError)
    }

    @Test
    fun `InvalidConfiguration error preserves field and reason`() {
        // Given: An InvalidConfiguration error
        val field = "port"
        val reason = "Port must be between 1 and 65535"
        val error = DatabaseError.InvalidConfiguration(field, reason)

        // Then: Both field and reason are preserved
        assertEquals(field, error.field)
        assertEquals(reason, error.reason)
        assertTrue(error is DatabaseError)
    }

    @Test
    fun `UnsupportedFeature error preserves feature name`() {
        // Given: An UnsupportedFeature error
        val feature = "SEQUENCES"
        val error = DatabaseError.UnsupportedFeature(feature)

        // Then: Feature is preserved
        assertEquals(feature, error.feature)
        assertTrue(error is DatabaseError)
    }

    @Test
    fun `UnknownError wraps original throwable`() {
        // Given: An UnknownError wrapping another exception
        val original = RuntimeException("Something went wrong")
        val error = DatabaseError.UnknownError(original)

        // Then: Original throwable is preserved
        assertEquals(original, error.throwable)
        assertTrue(error is DatabaseError)
    }

    @Test
    fun `sealed class allows exhaustive when expression`() {
        // Given: Different DatabaseError instances
        val errors = listOf(
            DatabaseError.ConnectionFailed("test"),
            DatabaseError.AuthenticationFailed("test"),
            DatabaseError.QueryExecutionFailed("SELECT 1", "test"),
            DatabaseError.TimeoutError("test"),
            DatabaseError.InvalidConfiguration("field", "test"),
            DatabaseError.UnsupportedFeature("test"),
            DatabaseError.UnknownError(RuntimeException())
        )

        // When: Exhaustive when expression
        val messages = errors.map { error ->
            when (error) {
                is DatabaseError.ConnectionFailed -> "Connection failed: ${error.reason}"
                is DatabaseError.AuthenticationFailed -> "Auth failed: ${error.reason}"
                is DatabaseError.QueryExecutionFailed -> "Query failed: ${error.reason}"
                is DatabaseError.TimeoutError -> "Timeout: ${error.operation}"
                is DatabaseError.InvalidConfiguration -> "Invalid ${error.field}: ${error.reason}"
                is DatabaseError.UnsupportedFeature -> "Unsupported: ${error.feature}"
                is DatabaseError.UnknownError -> "Unknown: ${error.throwable.message}"
            }
        }

        // Then: All cases are handled
        assertEquals(7, messages.size)
        assertTrue(messages.all { it.isNotBlank() })
    }
}
