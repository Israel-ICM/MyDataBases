package com.sphynxs.mydatabases.domain.models

/**
 * Resultado de ejecutar un statement SQL individual en modo multi-statement.
 *
 * Usado para renderizar summary table en el query editor cuando se ejecutan
 * múltiples statements (UPDATE, DELETE, INSERT, etc.).
 *
 * @property sql Statement SQL ejecutado (para display en tabla de resultados)
 * @property affectedRows Filas afectadas (para INSERT/UPDATE/DELETE), null para queries
 * @property executionTimeMs Tiempo de ejecución en milisegundos
 * @property isQuery True si SELECT/SHOW/etc, false si INSERT/UPDATE/DELETE/DDL
 *
 * @author israel-icm
 * @date 2026-06-23
 */
data class StatementResult(
    val sql: String,
    val affectedRows: Int?,
    val executionTimeMs: Long,
    val isQuery: Boolean
)
