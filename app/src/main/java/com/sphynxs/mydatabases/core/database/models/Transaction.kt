package com.sphynxs.mydatabases.core.database.models

/**
 * Representa una transacción activa en la base de datos.
 *
 * Una transacción permite agrupar múltiples operaciones (INSERT/UPDATE/DELETE)
 * en una unidad atómica que puede ser confirmada (commit) o revertida (rollback).
 *
 * Lifecycle:
 * 1. beginTransaction() → Transaction creada, auto-commit deshabilitado
 * 2. executeUpdate() múltiples veces
 * 3. commit() → cambios confirmados, auto-commit habilitado
 *    OR rollback() → cambios revertidos, auto-commit habilitado
 *
 * @property id Identificador único de la transacción
 * @property startedAt Timestamp de inicio de la transacción (milisegundos desde epoch)
 *
 * @author israel-icm
 * @date 2026-06-11
 */
data class Transaction(
    val id: String,
    val startedAt: Long
)
