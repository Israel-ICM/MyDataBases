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
     * Construye la cláusula de una columna siguiendo el orden de design.md (extendido por el
     * addendum "extended field attributes"):
     * 1. `` `name` ``
     * 2. tipo + `(length[,decimals])`, o `tipo('v1','v2',...)` cuando `type.supportsValues` es
     *    true (ENUM/SET, change `create-table` ENUM/SET support — estos tipos no tienen
     *    longitud/decimales, así que la cláusula de valores los reemplaza)
     * 3. `UNSIGNED ZEROFILL` si `zeroFill == true` (antes de charset, atributo numérico
     *    inmediatamente después del tipo)
     * 4. `CHARACTER SET x COLLATE y` si `type.supportsCharset` y `characterSet`/`collation`
     *    no son ambos null
     * 5. branch `isVirtual`: (`GENERATED ALWAYS AS (...) VIRTUAL|STORED` — nunca emite
     *    NULL/DEFAULT/AUTO_INCREMENT/ON UPDATE para columnas generadas) o, si no es virtual:
     *    `NOT NULL` (si corresponde) → `DEFAULT <valor>` (crudo, no citado) → `ON UPDATE
     *    CURRENT_TIMESTAMP` → `AUTO_INCREMENT`
     * 6. `COMMENT '...'` si corresponde (siempre al final)
     */
    private fun buildColumnClause(column: ColumnDefinition): String {
        val parts = mutableListOf<String>()
        parts += "`${column.name.trim()}`"
        parts += if (column.type.supportsValues) {
            buildValuesTypeClause(column)
        } else {
            column.type.sqlName + buildLengthAndDecimalsSuffix(column)
        }

        if (column.zeroFill) {
            parts += "UNSIGNED ZEROFILL"
        }

        buildCharsetClause(column)?.let { parts += it }

        if (column.isVirtual) {
            parts += "GENERATED ALWAYS AS (${column.expression})"
            parts += (column.generatedStorageMode ?: GeneratedStorageMode.VIRTUAL).sqlKeyword
        } else {
            if (!column.nullable) {
                parts += "NOT NULL"
            }
            if (!column.defaultValue.isNullOrBlank()) {
                parts += "DEFAULT ${column.defaultValue.trim()}"
            }
            if (column.autoUpdateTimestamp) {
                parts += "ON UPDATE CURRENT_TIMESTAMP"
            }
            if (column.autoIncrement) {
                parts += "AUTO_INCREMENT"
            }
        }

        if (!column.comment.isNullOrBlank()) {
            parts += "COMMENT '${column.comment.trim().replace("'", "''")}'"
        }

        return parts.joinToString(" ")
    }

    /**
     * `CHARACTER SET x COLLATE y` clause, solo para tipos con `type.supportsCharset` (change
     * `create-table`, extended field attributes addendum). Emite `CHARACTER SET x` solo,
     * `COLLATE y` solo, o ambos separados por espacio, según cuál de
     * [ColumnDefinition.characterSet]/[ColumnDefinition.collation] esté seteado. `null` si el
     * tipo no soporta charset o si ninguno de los dos está seteado.
     */
    private fun buildCharsetClause(column: ColumnDefinition): String? {
        if (!column.type.supportsCharset) return null
        val charsetPart = column.characterSet?.let { "CHARACTER SET $it" }
        val collationPart = column.collation?.let { "COLLATE $it" }
        return listOfNotNull(charsetPart, collationPart).takeIf { it.isNotEmpty() }?.joinToString(" ")
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

    /**
     * `TYPE('v1','v2',...)` clause for value-list types (ENUM/SET). Each value is trimmed and
     * SQL-escaped the same way `comment` is (single quotes doubled), replacing the normal
     * `sqlName + buildLengthAndDecimalsSuffix(...)` path for these types.
     */
    private fun buildValuesTypeClause(column: ColumnDefinition): String {
        val escapedValues = column.values.joinToString(",") { "'${it.trim().replace("'", "''")}'" }
        return "${column.type.sqlName}($escapedValues)"
    }
}
