package com.sphynxs.mydatabases.domain.sql

/**
 * Statement individual extraído por [SqlStatementStreamSplitter] de un script SQL.
 *
 * @property sql Texto completo del statement (sin el terminador).
 * @property lineNumber Línea 1-based donde comienza el statement en el archivo fuente.
 * @property hasTopLevelWhere True si el statement contiene una cláusula `WHERE` a nivel
 *   top-level (fuera de subqueries y de cualquier literal de cadena).
 *
 * @author sdd-apply
 * @date 2026-08-04
 */
data class ScriptStatement(
    val sql: String,
    val lineNumber: Int,
    val hasTopLevelWhere: Boolean
)

/**
 * Categorías de riesgo que [StatementRiskClassifier] puede asignar a un [ScriptStatement].
 *
 * Todas requieren confirmación explícita del usuario antes de ejecutarse (ver
 * `large-sql-script-execution/spec.md`, Requirement "Statement Risk Classification").
 */
enum class RiskCategory {
    DDL,
    DELETE,
    UPDATE_NO_WHERE
}

/**
 * Reporte agregado de riesgo producido por la fase de pre-scan (Phase A).
 *
 * @property totalStatements Cantidad total de statements en el script.
 * @property counts Cantidad de statements por [RiskCategory].
 * @property lineNumbers Líneas 1-based de cada statement riesgoso, agrupadas por [RiskCategory].
 * @property isRisky True si al menos una categoría tiene un conteo mayor a cero.
 *
 * @author sdd-apply
 * @date 2026-08-04
 */
data class RiskReport(
    val totalStatements: Int,
    val counts: Map<RiskCategory, Int>,
    val lineNumbers: Map<RiskCategory, List<Int>>
) {
    val isRisky: Boolean get() = counts.values.any { it > 0 }
}

/**
 * Progreso emitido durante la ejecución (Phase B) de un script SQL.
 *
 * @property statementIndex Índice 0-based del statement en curso.
 * @property lineNumber Línea 1-based donde comienza el statement en curso.
 * @property totalStatements Cantidad total de statements, si se conoce.
 */
data class ScriptExecutionProgress(
    val statementIndex: Int,
    val lineNumber: Int,
    val totalStatements: Int?
)

/**
 * Resumen final de una ejecución de script, exitosa o interrumpida.
 *
 * @property statementsExecuted Cantidad de statements ejecutados exitosamente.
 * @property stoppedAtStatement Índice 1-based del statement donde se detuvo la ejecución
 *   por error o cancelación, null si terminó exitosamente.
 * @property selectRowsDiscarded Cantidad total de filas de SELECT descartadas (streamed, no bufferizadas).
 */
data class ScriptExecutionSummary(
    val statementsExecuted: Int,
    val stoppedAtStatement: Int?,
    val selectRowsDiscarded: Long
)

/**
 * Errores estructurales producidos por [SqlStatementStreamSplitter] al parsear un script.
 *
 * Son distintos de `DatabaseError`: nunca representan una falla del servidor, sino un
 * script mal formado.
 *
 * @author sdd-apply
 * @date 2026-08-04
 */
sealed class ScriptError(message: String) : Throwable(message) {

    /**
     * La directiva `DELIMITER` no tiene un token de terminador después de la palabra clave.
     *
     * @property lineNumber Línea 1-based donde ocurrió la directiva inválida.
     */
    data class MalformedDelimiterDirective(val lineNumber: Int) :
        ScriptError("Malformed DELIMITER directive at line $lineNumber")

    /**
     * Un token (comentario de bloque o literal) no se cerró antes de llegar a EOF.
     *
     * @property lineNumber Línea 1-based donde comenzó el token sin terminar.
     * @property kind Descripción del tipo de token (ej: "block comment", "string literal").
     */
    data class UnterminatedToken(val lineNumber: Int, val kind: String) :
        ScriptError("Unterminated $kind starting at line $lineNumber")
}
