package com.sphynxs.mydatabases.domain.usecases.connections

import com.sphynxs.mydatabases.domain.repositories.ConnectionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Tests para [DeleteConnectionUseCase].
 *
 * @author israel-icm
 * @date 2026-06-12
 */
class DeleteConnectionUseCaseTest {

    private lateinit var repository: ConnectionRepository
    private lateinit var useCase: DeleteConnectionUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = DeleteConnectionUseCase(repository)
    }

    @Test
    fun `invoke delegates delete to repository`() = runTest {
        // Given
        val connectionId = "conn-to-delete"
        coEvery { repository.delete(connectionId) } returns Unit

        // When
        useCase(connectionId)

        // Then
        coVerify(exactly = 1) { repository.delete(connectionId) }
    }
}
