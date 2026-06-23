package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.models.DatabaseError
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import javax.inject.Inject

/**
 * Use case for creating a new database on the connected MySQL/MariaDB server.
 *
 * Composes a CREATE DATABASE statement with optional CHARACTER SET and COLLATE clauses.
 * Validates all identifiers against ^[A-Za-z0-9_]{1,64}$ before executing.
 *
 * @param repository Repository for database access
 * @author sdd-apply (Strict TDD)
 * @date 2026-06-23
 */
class CreateDatabaseUseCase @Inject constructor(
    private val repository: DatabaseRepository
) {
    
    companion object {
        private val IDENTIFIER_REGEX = Regex("^[A-Za-z0-9_]{1,64}$")
    }
    
    /**
     * Creates a new database with the specified name and optional charset/collation.
     *
     * @param name Database name (trimmed, validated against identifier regex)
     * @param charset Character set (optional, trimmed, validated if non-blank)
     * @param collation Collation (optional, trimmed, validated if non-blank)
     * @return Result.success(Unit) if created, Result.failure(DatabaseError) otherwise
     */
    suspend operator fun invoke(
        name: String,
        charset: String? = null,
        collation: String? = null
    ): Result<Unit> {
        // Trim all inputs
        val trimmedName = name.trim()
        val trimmedCharset = charset?.trim()?.takeIf { it.isNotBlank() }
        val trimmedCollation = collation?.trim()?.takeIf { it.isNotBlank() }
        
        // Validate name (required)
        if (!IDENTIFIER_REGEX.matches(trimmedName)) {
            return Result.failure(
                DatabaseError.InvalidConfiguration(
                    field = "database_name",
                    reason = "Must match ^[A-Za-z0-9_]{1,64}$: '$name'"
                )
            )
        }
        
        // Validate charset (if provided)
        if (trimmedCharset != null && !IDENTIFIER_REGEX.matches(trimmedCharset)) {
            return Result.failure(
                DatabaseError.InvalidConfiguration(
                    field = "charset",
                    reason = "Must match ^[A-Za-z0-9_]{1,64}$: '$charset'"
                )
            )
        }
        
        // Validate collation (if provided)
        if (trimmedCollation != null && !IDENTIFIER_REGEX.matches(trimmedCollation)) {
            return Result.failure(
                DatabaseError.InvalidConfiguration(
                    field = "collation",
                    reason = "Must match ^[A-Za-z0-9_]{1,64}$: '$collation'"
                )
            )
        }
        
        // Compose SQL
        val sql = buildString {
            append("CREATE DATABASE `$trimmedName`")
            
            if (trimmedCharset != null) {
                append(" CHARACTER SET `$trimmedCharset`")
            }
            
            if (trimmedCollation != null) {
                append(" COLLATE `$trimmedCollation`")
            }
        }
        
        // Execute and map Result<Int> to Result<Unit>
        return repository.executeUpdate(sql, emptyList()).map { Unit }
    }
}
