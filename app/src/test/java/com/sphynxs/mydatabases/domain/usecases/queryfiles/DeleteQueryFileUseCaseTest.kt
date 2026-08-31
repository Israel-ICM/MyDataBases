package com.sphynxs.mydatabases.domain.usecases.queryfiles

import android.net.Uri
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
 * TDD tests for `DeleteQueryFileUseCase` (change `query-files-storage`, Phase 2).
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
class DeleteQueryFileUseCaseTest {

    private lateinit var store: QueryFileStore
    private lateinit var useCase: DeleteQueryFileUseCase

    @Before
    fun setup() {
        store = mockk()
        useCase = DeleteQueryFileUseCase(store)
    }

    @Test
    fun `invoke delegates to store delete with the given uri`() = runTest {
        val uri = mockk<Uri>()
        coEvery { store.delete(uri) } returns Result.success(true)

        val result = useCase(uri)

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
        coVerify(exactly = 1) { store.delete(uri) }
    }

    @Test
    fun `invoke propagates failure from the store`() = runTest {
        val uri = mockk<Uri>()
        val error = RuntimeException("not found")
        coEvery { store.delete(uri) } returns Result.failure(error)

        val result = useCase(uri)

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
