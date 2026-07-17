package com.sphynxs.mydatabases.core.database.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TDD tests for `ColumnDefinition` cross-field validation rules (change `create-table`).
 *
 * Covers:
 * - Length/Decimals applicability per [SqlColumnType]
 * - Llave (Key) forces Nulo=false for non-virtual columns
 * - Nulo control hidden/disabled when Virtual=true
 * - Expresión required-non-blank only when Virtual=true
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-07-15
 */
class ColumnDefinitionValidationTest {

    // --- Task 1.1: Length applicability ---

    @Test
    fun `isLengthApplicable returns true for VARCHAR`() {
        assertTrue(ColumnDefinitionValidation.isLengthApplicable(SqlColumnType.VarChar))
    }

    @Test
    fun `isLengthApplicable returns true for CHAR`() {
        assertTrue(ColumnDefinitionValidation.isLengthApplicable(SqlColumnType.Char))
    }

    @Test
    fun `isLengthApplicable returns true for DECIMAL`() {
        assertTrue(ColumnDefinitionValidation.isLengthApplicable(SqlColumnType.Decimal))
    }

    @Test
    fun `isLengthApplicable returns true for NUMERIC`() {
        assertTrue(ColumnDefinitionValidation.isLengthApplicable(SqlColumnType.Numeric))
    }

    @Test
    fun `isLengthApplicable returns false for INT`() {
        assertFalse(ColumnDefinitionValidation.isLengthApplicable(SqlColumnType.Int))
    }

    @Test
    fun `isLengthApplicable returns false for TEXT`() {
        assertFalse(ColumnDefinitionValidation.isLengthApplicable(SqlColumnType.Text))
    }

    @Test
    fun `isLengthApplicable returns false for DATETIME`() {
        assertFalse(ColumnDefinitionValidation.isLengthApplicable(SqlColumnType.DateTime))
    }

    // --- Task 1.1: Decimals applicability ---

    @Test
    fun `isDecimalsApplicable returns true for DECIMAL`() {
        assertTrue(ColumnDefinitionValidation.isDecimalsApplicable(SqlColumnType.Decimal))
    }

    @Test
    fun `isDecimalsApplicable returns true for NUMERIC`() {
        assertTrue(ColumnDefinitionValidation.isDecimalsApplicable(SqlColumnType.Numeric))
    }

    @Test
    fun `isDecimalsApplicable returns true for FLOAT`() {
        assertTrue(ColumnDefinitionValidation.isDecimalsApplicable(SqlColumnType.Float))
    }

    @Test
    fun `isDecimalsApplicable returns true for DOUBLE`() {
        assertTrue(ColumnDefinitionValidation.isDecimalsApplicable(SqlColumnType.Double))
    }

    @Test
    fun `isDecimalsApplicable returns false for VARCHAR`() {
        assertFalse(ColumnDefinitionValidation.isDecimalsApplicable(SqlColumnType.VarChar))
    }

    @Test
    fun `isDecimalsApplicable returns false for INT`() {
        assertFalse(ColumnDefinitionValidation.isDecimalsApplicable(SqlColumnType.Int))
    }

    // --- Task 1.1: Nombre identifier validation ---

    @Test
    fun `isValidName accepts alphanumeric and underscore up to 64 chars`() {
        assertTrue(ColumnDefinitionValidation.isValidName("user_name_1"))
    }

    @Test
    fun `isValidName rejects blank name`() {
        assertFalse(ColumnDefinitionValidation.isValidName(""))
    }

    @Test
    fun `isValidName rejects name with space`() {
        assertFalse(ColumnDefinitionValidation.isValidName("user name"))
    }

    @Test
    fun `isValidName rejects name over 64 chars`() {
        val tooLong = "a".repeat(65)
        assertFalse(ColumnDefinitionValidation.isValidName(tooLong))
    }

    // --- Task 1.1: Llave -> Nulo lock (non-virtual) ---

    @Test
    fun `resolveNullable forces false when Llave true and non-virtual`() {
        val result = ColumnDefinitionValidation.resolveNullable(
            requestedNullable = true,
            isPrimaryKey = true,
            isVirtual = false
        )
        assertFalse(result)
    }

    @Test
    fun `resolveNullable keeps requested value when Llave false and non-virtual`() {
        val result = ColumnDefinitionValidation.resolveNullable(
            requestedNullable = true,
            isPrimaryKey = false,
            isVirtual = false
        )
        assertTrue(result)
    }

    @Test
    fun `resolveNullable does not force false for virtual columns even when Llave true`() {
        // Nulo control is hidden/disabled for virtual columns regardless of Llave;
        // nullability is derived from the expression, not user-settable.
        val result = ColumnDefinitionValidation.resolveNullable(
            requestedNullable = true,
            isPrimaryKey = true,
            isVirtual = true
        )
        assertTrue(result)
    }

    @Test
    fun `isNuloEditable returns false when Virtual is true`() {
        assertFalse(ColumnDefinitionValidation.isNuloEditable(isVirtual = true))
    }

    @Test
    fun `isNuloEditable returns true when Virtual is false and Llave is false`() {
        assertTrue(ColumnDefinitionValidation.isNuloEditable(isVirtual = false, isPrimaryKey = false))
    }

    @Test
    fun `isNuloEditable returns false when Virtual is false and Llave is true`() {
        assertFalse(ColumnDefinitionValidation.isNuloEditable(isVirtual = false, isPrimaryKey = true))
    }

    // --- Task 1.1: Expresión required-non-blank only when Virtual=true ---

    @Test
    fun `isExpressionRequired returns true when Virtual is true`() {
        assertTrue(ColumnDefinitionValidation.isExpressionRequired(isVirtual = true))
    }

    @Test
    fun `isExpressionRequired returns false when Virtual is false`() {
        assertFalse(ColumnDefinitionValidation.isExpressionRequired(isVirtual = false))
    }

    @Test
    fun `isExpressionValid rejects blank expression when Virtual is true`() {
        assertFalse(ColumnDefinitionValidation.isExpressionValid(expression = "", isVirtual = true))
    }

    @Test
    fun `isExpressionValid rejects null expression when Virtual is true`() {
        assertFalse(ColumnDefinitionValidation.isExpressionValid(expression = null, isVirtual = true))
    }

    @Test
    fun `isExpressionValid accepts non-blank expression when Virtual is true`() {
        assertTrue(ColumnDefinitionValidation.isExpressionValid(expression = "price * 1.16", isVirtual = true))
    }

    @Test
    fun `isExpressionValid accepts blank expression when Virtual is false`() {
        // Expresión is hidden/ignored when Virtual = false, so blank/null must not fail.
        assertTrue(ColumnDefinitionValidation.isExpressionValid(expression = null, isVirtual = false))
    }

    @Test
    fun `isExpressionValid does not parse or semantically validate expression content`() {
        // Spec: client MUST NOT parse/semantically validate the expression - only non-blank check.
        assertTrue(
            ColumnDefinitionValidation.isExpressionValid(
                expression = "this is not ) valid ( sql (((",
                isVirtual = true
            )
        )
    }

    // --- generatedStorageMode (ColumnDefinition computed property) ---

    @Test
    fun `generatedStorageMode is null for non-virtual column`() {
        val column = ColumnDefinition(name = "id", type = SqlColumnType.Int, isVirtual = false)
        assertEquals(null, column.generatedStorageMode)
    }

    @Test
    fun `generatedStorageMode is VIRTUAL for virtual non-key column`() {
        val column = ColumnDefinition(
            name = "total",
            type = SqlColumnType.Decimal,
            isVirtual = true,
            expression = "price * qty",
            isPrimaryKey = false
        )
        assertEquals(GeneratedStorageMode.VIRTUAL, column.generatedStorageMode)
    }

    @Test
    fun `generatedStorageMode is STORED for virtual key column`() {
        val column = ColumnDefinition(
            name = "total",
            type = SqlColumnType.Decimal,
            isVirtual = true,
            expression = "price * qty",
            isPrimaryKey = true
        )
        assertEquals(GeneratedStorageMode.STORED, column.generatedStorageMode)
    }
}
