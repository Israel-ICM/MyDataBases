package com.sphynxs.mydatabases.domain.usecases.queryfiles

import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.domain.models.QueryFileInfo
import com.sphynxs.mydatabases.domain.repositories.QueryFileStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import android.net.Uri

/**
 * TDD tests for `ListQueryFilesUseCase` (change `query-files-storage`, Phase 2).
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
class ListQueryFilesUseCaseTest {

    private lateinit var store: QueryFileStore
    private lateinit var useCase: ListQueryFilesUseCase

    @Before
    fun setup() {
        store = mockk()
        useCase = ListQueryFilesUseCase(store)
    }

    @Test
    fun `invoke delegates to store list with the given engine`() = runTest {
        val uri = mockk<Uri>()
        val files = listOf(QueryFileInfo("a.sql", uri, 100L))
        coEvery { store.list(DatabaseType.MYSQL) } returns Result.success(files)

        val result = useCase(DatabaseType.MYSQL)

        assertTrue(result.isSuccess)
        assertEquals(files, result.getOrNull())
        coVerify(exactly = 1) { store.list(DatabaseType.MYSQL) }
    }

    @Test
    fun `invoke propagates failure from the store`() = runTest {
        val error = RuntimeException("boom")
        coEvery { store.list(DatabaseType.POSTGRESQL) } returns Result.failure(error)

        val result = useCase(DatabaseType.POSTGRESQL)

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
