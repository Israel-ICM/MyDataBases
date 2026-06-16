package com.sphynxs.mydatabases.core.database.models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Representa una transacción activa de base de datos.
 *
 * Permite hacer commit o rollback de cambios.
 *
 * @property connection Conexión JDBC subyacente (privada)
 * @property onCommit Callback para confirmar la transacción
 * @property onRollback Callback para revertir la transacción
 * @author israel-icm
 * @date 2026-06-11
 */
data class Transaction(
    private val connection: java.sql.Connection,
    private val onCommit: () -> Unit,
    private val onRollback: () -> Unit
) {
    /**
     * Confirma la transacción (COMMIT).
     */
    suspend fun commit() = withContext(Dispatchers.IO) {
        onCommit()
    }

    /**
     * Revierte la transacción (ROLLBACK).
     */
    suspend fun rollback() = withContext(Dispatchers.IO) {
        onRollback()
    }
}
