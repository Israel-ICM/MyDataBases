package com.sphynxs.mydatabases.domain.sql

/**
 * Clasificador puro de riesgo por statement, según la tabla de reglas fija de
 * `large-sql-script-execution/spec.md` (Requirement "Statement Risk Classification").
 *
 * No ejecuta ni se conecta a nada — es una función pura del texto del statement más el
 * flag `hasTopLevelWhere` calculado por `SqlStatementStreamSplitter`. Reglas
 * runtime-configurables están fuera de scope.
 *
 * @author sdd-apply
 * @date 2026-08-04
 */
object StatementRiskClassifier {

    private val DDL_KEYWORDS = setOf("CREATE", "ALTER", "DROP", "TRUNCATE", "RENAME")

    /**
     * Clasifica [statement] según la tabla de reglas fija (case-insensitive sobre la
     * palabra clave inicial). Retorna null si el statement es "clean" y no requiere
     * confirmación.
     *
     * Un `CREATE PROCEDURE ...` (cuerpo delimitado por `DELIMITER`) clasifica igual que
     * cualquier otro `CREATE`: por su palabra clave inicial.
     */
    fun classify(statement: ScriptStatement): RiskCategory? {
        val leadingKeyword = leadingKeyword(statement.sql) ?: return null

        return when {
            leadingKeyword in DDL_KEYWORDS -> RiskCategory.DDL
            leadingKeyword == "DELETE" -> RiskCategory.DELETE
            leadingKeyword == "UPDATE" && !statement.hasTopLevelWhere -> RiskCategory.UPDATE_NO_WHERE
            else -> null
        }
    }

    private fun leadingKeyword(sql: String): String? =
        sql.trim().takeWhile { !it.isWhitespace() }.uppercase().ifEmpty { null }
}
