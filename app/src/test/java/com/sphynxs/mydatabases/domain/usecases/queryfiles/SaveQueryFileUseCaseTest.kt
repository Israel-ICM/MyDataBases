package com.sphynxs.mydatabases.domain.usecases.queryfiles

import android.net.Uri
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.domain.repositories.QueryFileStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * TDD tests for `SaveQueryFileUseCase` (change `query-files-storage`, Phase 2).
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
class SaveQueryFileUseCaseTest {

    private lateinit var store: QueryFileStore
    private lateinit var useCase: SaveQueryFileUseCase

    @Before
    fun setup() {
        store = mockk()
        useCase = SaveQueryFileUseCase(store)
    }

    @Test
    fun `invoke delegates to store write with engine, name and content`() = runTest {
        val uri = mockk<Uri>()
        coEvery { store.write(DatabaseType.MARIADB, "my_query.sql", "SELECT 1;") } returns Result.success(uri)

        val result = useCase(DatabaseType.MARIADB, "my_query.sql", "SELECT 1;")

        assertTrue(result.isSuccess)
        assertEquals(uri, result.getOrNull())
        coVerify(exactly = 1) { store.write(DatabaseType.MARIADB, "my_query.sql", "SELECT 1;") }
    }

    @Test
    fun `invoke propagates failure from the store`() = runTest {
        val error = RuntimeException("disk full")
        coEvery { store.write(any(), any(), any()) } returns Result.failure(error)

        val result = useCase(DatabaseType.SQLITE, "x.sql", "")

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
