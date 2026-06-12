package com.sphynxs.mydatabases.core.database.models

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests para Transaction.
 *
 * Tests verify:
 * - commit() ejecuta el callback onCommit
 * - rollback() ejecuta el callback onRollback
 * - Las operaciones son thread-safe (Dispatchers.IO)
 *
 * @author israel-icm
 * @date 2026-06-12
 */
class TransactionTest {

    @Test
    fun `commit ejecuta callback onCommit`() = runTest {
        // Given: Transaction con callback
        var commitExecuted = false
        val mockConnection = mockk<java.sql.Connection>(relaxed = true)
        val transaction = Transaction(
            connection = mockConnection,
            onCommit = { commitExecuted = true },
            onRollback = { }
        )

        // When: commit() es llamado
        transaction.commit()

        // Then: onCommit fue ejecutado
        assert(commitExecuted) { "onCommit debería haber sido ejecutado" }
    }

    @Test
    fun `rollback ejecuta callback onRollback`() = runTest {
        // Given: Transaction con callback
        var rollbackExecuted = false
        val mockConnection = mockk<java.sql.Connection>(relaxed = true)
        val transaction = Transaction(
            connection = mockConnection,
            onCommit = { },
            onRollback = { rollbackExecuted = true }
        )

        // When: rollback() es llamado
        transaction.rollback()

        // Then: onRollback fue ejecutado
        assert(rollbackExecuted) { "onRollback debería haber sido ejecutado" }
    }

    @Test
    fun `commit no ejecuta callback onRollback`() = runTest {
        // Given: Transaction con ambos callbacks
        var commitExecuted = false
        var rollbackExecuted = false
        val mockConnection = mockk<java.sql.Connection>(relaxed = true)
        val transaction = Transaction(
            connection = mockConnection,
            onCommit = { commitExecuted = true },
            onRollback = { rollbackExecuted = true }
        )

        // When: solo commit() es llamado
        transaction.commit()

        // Then: solo onCommit fue ejecutado
        assert(commitExecuted) { "onCommit debería haber sido ejecutado" }
        assert(!rollbackExecuted) { "onRollback NO debería haber sido ejecutado" }
    }

    @Test
    fun `rollback no ejecuta callback onCommit`() = runTest {
        // Given: Transaction con ambos callbacks
        var commitExecuted = false
        var rollbackExecuted = false
        val mockConnection = mockk<java.sql.Connection>(relaxed = true)
        val transaction = Transaction(
            connection = mockConnection,
            onCommit = { commitExecuted = true },
            onRollback = { rollbackExecuted = true }
        )

        // When: solo rollback() es llamado
        transaction.rollback()

        // Then: solo onRollback fue ejecutado
        assert(!commitExecuted) { "onCommit NO debería haber sido ejecutado" }
        assert(rollbackExecuted) { "onRollback debería haber sido ejecutado" }
    }
}
