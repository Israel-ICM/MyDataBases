package com.sphynxs.mydatabases.ui.screens.databases

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TDD tests for AddDatabase form validation logic.
 *
 * Spec: add-database-form "Name Validation"
 *
 * @author sdd-apply
 * @date 2026-06-19
 */
class AddDatabaseValidationTest {

    /**
     * RED Test #1: Empty name is invalid.
     * Spec: "Empty name disables Create button"
     */
    @Test
    fun `validateDatabaseName returns error for empty string`() {
        // GIVEN: empty name
        val name = ""

        // WHEN: validating
        val result = validateDatabaseName(name)

        // THEN: validation fails
        assertFalse(result.isValid)
        assertEquals("Name is required", result.errorMessage)
    }

    /**
     * RED Test #2 (TRIANGULATE): Name with invalid characters fails.
     * Spec: "Invalid characters in name"
     */
    @Test
    fun `validateDatabaseName returns error for name with invalid characters`() {
        // GIVEN: name with dash and exclamation
        val name = "my-db!"

        // WHEN: validating
        val result = validateDatabaseName(name)

        // THEN: validation fails
        assertFalse(result.isValid)
        assertEquals("Only letters, numbers and underscore", result.errorMessage)
    }

    /**
     * RED Test #3 (TRIANGULATE): Name exceeding 64 chars fails.
     * Spec: "Name exceeds 64 characters"
     */
    @Test
    fun `validateDatabaseName returns error for name exceeding 64 characters`() {
        // GIVEN: 65-character name
        val name = "a".repeat(65)

        // WHEN: validating
        val result = validateDatabaseName(name)

        // THEN: validation fails
        assertFalse(result.isValid)
        assertEquals("Max 64 characters", result.errorMessage)
    }

    /**
     * RED Test #4 (TRIANGULATE): Valid name passes.
     * Spec: "Valid name enables Create"
     */
    @Test
    fun `validateDatabaseName returns valid for correct name`() {
        // GIVEN: valid name
        val name = "analytics_2026"

        // WHEN: validating
        val result = validateDatabaseName(name)

        // THEN: validation succeeds
        assertTrue(result.isValid)
        assertEquals(null, result.errorMessage)
    }

    /**
     * RED Test #5 (TRIANGULATE): Whitespace-only name fails.
     */
    @Test
    fun `validateDatabaseName returns error for whitespace-only string`() {
        // GIVEN: whitespace-only
        val name = "   "

        // WHEN: validating
        val result = validateDatabaseName(name)

        // THEN: validation fails
        assertFalse(result.isValid)
        assertEquals("Name is required", result.errorMessage)
    }

    /**
     * RED Test #6: Empty optional charset is valid.
     * Spec: "Empty optional fields do not block submit"
     */
    @Test
    fun `validateOptionalField returns valid for empty string`() {
        // GIVEN: empty charset
        val charset = ""

        // WHEN: validating
        val result = validateOptionalField(charset)

        // THEN: validation succeeds (optional fields can be empty)
        assertTrue(result.isValid)
        assertEquals(null, result.errorMessage)
    }

    /**
     * RED Test #7 (TRIANGULATE): Invalid optional charset fails.
     * Spec: "Invalid charset shows inline error"
     */
    @Test
    fun `validateOptionalField returns error for invalid characters`() {
        // GIVEN: charset with space
        val charset = "utf8 mb4"

        // WHEN: validating
        val result = validateOptionalField(charset)

        // THEN: validation fails
        assertFalse(result.isValid)
        assertEquals("Only letters, numbers and underscore", result.errorMessage)
    }

    /**
     * RED Test #8 (TRIANGULATE): Valid optional charset passes.
     */
    @Test
    fun `validateOptionalField returns valid for correct charset`() {
        // GIVEN: valid charset
        val charset = "utf8mb4"

        // WHEN: validating
        val result = validateOptionalField(charset)

        // THEN: validation succeeds
        assertTrue(result.isValid)
        assertEquals(null, result.errorMessage)
    }
}
