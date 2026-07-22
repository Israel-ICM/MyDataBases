package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.models.DatabaseError
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import javax.inject.Inject

/**
 * Use case for updating an existing database's charset/collation on the connected
 * MySQL/MariaDB server.
 *
 * Composes an ALTER DATABASE statement with CHARACTER SET and/or COLLATE clauses.
 * The database name itself is NOT renamable — MySQL/MariaDB have no `RENAME DATABASE`
 * equivalent via ALTER, so `name` only identifies which database to alter.
 * Validates all identifiers against ^[A-Za-z0-9_]{1,64}$ before executing, mirroring
 * [CreateDatabaseUseCase].
 *
 * @param repository Repository for database access
 * @author gentle-ai
 * @date 2026-07-21
 */
class UpdateDatabaseUseCase @Inject constructor(
    private val repository: DatabaseRepository
) {

    companion object {
        private val IDENTIFIER_REGEX = Regex("^[A-Za-z0-9_]{1,64}$")
    }

    /**
     * Updates the charset and/or collation of an existing database.
     *
     * @param name Existing database name (trimmed, validated against identifier regex)
     * @param charset New character set (optional, trimmed, validated if non-blank)
     * @param collation New collation (optional, trimmed, validated if non-blank)
     * @return Result.success(Unit) if updated, Result.failure(DatabaseError) otherwise.
     *   Fails with [DatabaseError.InvalidConfiguration] if neither charset nor collation
     *   is provided — ALTER DATABASE requires at least one clause.
     */
    suspend operator fun invoke(
        name: String,
        charset: String? = null,
        collation: String? = null
    ): Result<Unit> {
        val trimmedName = name.trim()
        val trimmedCharset = charset?.trim()?.takeIf { it.isNotBlank() }
        val trimmedCollation = collation?.trim()?.takeIf { it.isNotBlank() }

        if (!IDENTIFIER_REGEX.matches(trimmedName)) {
            return Result.failure(
                DatabaseError.InvalidConfiguration(
                    field = "database_name",
                    reason = "Must match ^[A-Za-z0-9_]{1,64}$: '$name'"
                )
            )
        }

        if (trimmedCharset != null && !IDENTIFIER_REGEX.matches(trimmedCharset)) {
            return Result.failure(
                DatabaseError.InvalidConfiguration(
                    field = "charset",
                    reason = "Must match ^[A-Za-z0-9_]{1,64}$: '$charset'"
                )
            )
        }

        if (trimmedCollation != null && !IDENTIFIER_REGEX.matches(trimmedCollation)) {
            return Result.failure(
                DatabaseError.InvalidConfiguration(
                    field = "collation",
                    reason = "Must match ^[A-Za-z0-9_]{1,64}$: '$collation'"
                )
            )
        }

        if (trimmedCharset == null && trimmedCollation == null) {
            return Result.failure(
                DatabaseError.InvalidConfiguration(
                    field = "charset_collation",
                    reason = "At least one of charset or collation must be provided for ALTER DATABASE"
                )
            )
        }

        val sql = buildString {
            append("ALTER DATABASE `$trimmedName`")

            if (trimmedCharset != null) {
                append(" CHARACTER SET `$trimmedCharset`")
            }

            if (trimmedCollation != null) {
                append(" COLLATE `$trimmedCollation`")
            }
        }

        return repository.executeUpdate(sql, emptyList()).map { Unit }
    }
}
