package com.sphynxs.mydatabases.ui.screens.databases

/**
 * Validation result for form fields.
 *
 * Pure data class representing validation state.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

/**
 * Validates a database name.
 *
 * Rules (from spec add-database-form "Name Validation"):
 * - Must not be empty after trimming whitespace
 * - Must match ^[A-Za-z0-9_]{1,64}$ (alphanumeric and underscore, 1-64 chars)
 *
 * @param name The database name to validate
 * @return ValidationResult with isValid and optional errorMessage
 */
fun validateDatabaseName(name: String): ValidationResult {
    val trimmed = name.trim()
    
    return when {
        trimmed.isEmpty() -> ValidationResult(
            isValid = false,
            errorMessage = "Name is required"
        )
        trimmed.length > 64 -> ValidationResult(
            isValid = false,
            errorMessage = "Max 64 characters"
        )
        !trimmed.matches(Regex("^[A-Za-z0-9_]{1,64}$")) -> ValidationResult(
            isValid = false,
            errorMessage = "Only letters, numbers and underscore"
        )
        else -> ValidationResult(isValid = true)
    }
}

/**
 * Validates an optional field (charset or collation).
 *
 * Rules (from spec add-database-form "Optional Charset and Collation"):
 * - Empty is valid (optional)
 * - If non-empty, must match ^[A-Za-z0-9_]{1,64}$
 *
 * @param value The optional field value to validate
 * @return ValidationResult with isValid and optional errorMessage
 */
fun validateOptionalField(value: String): ValidationResult {
    val trimmed = value.trim()
    
    return when {
        trimmed.isEmpty() -> ValidationResult(isValid = true)
        trimmed.length > 64 -> ValidationResult(
            isValid = false,
            errorMessage = "Max 64 characters"
        )
        !trimmed.matches(Regex("^[A-Za-z0-9_]{1,64}$")) -> ValidationResult(
            isValid = false,
            errorMessage = "Only letters, numbers and underscore"
        )
        else -> ValidationResult(isValid = true)
    }
}
