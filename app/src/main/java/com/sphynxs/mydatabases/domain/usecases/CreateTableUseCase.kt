package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.models.ColumnDefinition
import com.sphynxs.mydatabases.core.database.models.DatabaseError
import com.sphynxs.mydatabases.core.database.models.GeneratedStorageMode
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import javax.inject.Inject

/**
 * Use case para crear una nueva tabla en el servidor MySQL/MariaDB conectado (change `create-table`).
 *
 * Construye un statement `CREATE TABLE` a partir de una lista ordenada de [ColumnDefinition],
 * incluyendo el branch de columnas generadas (`GENERATED ALWAYS AS (<expresión>) [VIRTUAL|STORED]`).
 * Valida todos los identificadores (nombre de tabla y de cada columna) contra
 * `^[A-Za-z0-9_]{1,64}$` antes de ejecutar. Mirrors `CreateDatabaseUseCase`'s
 * validate → `buildString` DDL → `executeUpdate` shape.
 *
 * @param repository Repositorio de acceso a base de datos
 * @author sdd-apply (Strict TDD)
 * @date 2026-07-15
 */
class CreateTableUseCase @Inject constructor(
    private val repository: DatabaseRepository
) {

    companion object {
        private val IDENTIFIER_REGEX = Regex("^[A-Za-z0-9_]{1,64}$")
    }

    /**
     * Crea una tabla con el nombre y las columnas especificadas.
     *
     * @param name Nombre de la tabla (trimmed, validado contra el regex de identificador)
     * @param columns Lista ordenada de definiciones de columna (al menos una requerida)
     * @return Result.success(Unit) si se creó, Result.failure(DatabaseError) en caso contrario
     */
    suspend operator fun invoke(name: String, columns: List<ColumnDefinition>): Result<Unit> {
        val trimmedName = name.trim()

        if (!IDENTIFIER_REGEX.matches(trimmedName)) {
            return Result.failure(
                DatabaseError.InvalidConfiguration(
                    field = "table_name",
                    reason = "Must match ^[A-Za-z0-9_]{1,64}$: '$name'"
                )
            )
        }

        if (columns.isEmpty()) {
            return Result.failure(
                DatabaseError.InvalidConfiguration(
                    field = "columns",
                    reason = "At least one column is required"
                )
            )
        }

        for (column in columns) {
            val trimmedColumnName = column.name.trim()
            if (!IDENTIFIER_REGEX.matches(trimmedColumnName)) {
                return Result.failure(
                    DatabaseError.InvalidConfiguration(
                        field = "column_name",
                        reason = "Must match ^[A-Za-z0-9_]{1,64}$: '${column.name}'"
                    )
                )
            }
        }

        val sql = buildCreateTableSql(trimmedName, columns)

        return repository.executeUpdate(sql, emptyList()).map { Unit }
    }

    /**
     * Construye el DDL `CREATE TABLE` completo: columnas (comma-joined) + PRIMARY KEY final.
     */
    private fun buildCreateTableSql(tableName: String, columns: List<ColumnDefinition>): String =
        buildString {
            append("CREATE TABLE `$tableName` (")
            append(columns.joinToString(", ") { buildColumnClause(it) })

            val primaryKeyColumns = columns.filter { it.isPrimaryKey }
            if (primaryKeyColumns.isNotEmpty()) {
                append(", PRIMARY KEY (")
                append(primaryKeyColumns.joinToString(", ") { "`${it.name.trim()}`" })
                append(")")
            }

            append(")")
        }

    /**
     * Construye la cláusula de una columna siguiendo el orden de design.md:
     * 1. `` `name` `` 2. tipo + `(length[,decimals])` 3. branch `isVirtual`
     * (`GENERATED ALWAYS AS (...) VIRTUAL|STORED` sin NULL/DEFAULT, o `NOT NULL`/omitido)
     * 4. `COMMENT '...'` si corresponde.
     */
    private fun buildColumnClause(column: ColumnDefinition): String {
        val parts = mutableListOf<String>()
        parts += "`${column.name.trim()}`"
        parts += column.type.sqlName + buildLengthAndDecimalsSuffix(column)

        if (column.isVirtual) {
            parts += "GENERATED ALWAYS AS (${column.expression})"
            parts += (column.generatedStorageMode ?: GeneratedStorageMode.VIRTUAL).sqlKeyword
        } else if (!column.nullable) {
            parts += "NOT NULL"
        }

        if (!column.comment.isNullOrBlank()) {
            parts += "COMMENT '${column.comment.trim().replace("'", "''")}'"
        }

        return parts.joinToString(" ")
    }

    /** `(length[,decimals])` suffix, only for length/decimal-bearing base types. */
    private fun buildLengthAndDecimalsSuffix(column: ColumnDefinition): String {
        if (!column.type.supportsLength || column.length == null) return ""
        return if (column.type.supportsDecimals && column.decimals != null) {
            "(${column.length},${column.decimals})"
        } else {
            "(${column.length})"
        }
    }
}
