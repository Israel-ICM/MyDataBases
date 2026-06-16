package com.sphynxs.mydatabases.domain.usecases.connections

import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.domain.repositories.ConnectionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Tests para [GetConnectionByIdUseCase].
 *
 * @author israel-icm
 * @date 2026-06-12
 */
class GetConnectionByIdUseCaseTest {

    private lateinit var repository: ConnectionRepository
    private lateinit var useCase: GetConnectionByIdUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetConnectionByIdUseCase(repository)
    }

    @Test
    fun `invoke returns connection when exists`() = runTest {
        // Given
        val connection = ConnectionConfig(
            id = "conn-42",
            name = "Test DB",
            type = DatabaseType.MYSQL,
            host = "localhost",
            port = 3306,
            database = "test",
            username = "admin",
            password = "pass"
        )

        coEvery { repository.getById("conn-42") } returns connection

        // When
        val result = useCase("conn-42")

        // Then
        coVerify(exactly = 1) { repository.getById("conn-42") }
        assertEquals("Test DB", result!!.name)
    }

    @Test
    fun `invoke returns null when connection does not exist`() = runTest {
        // Given
        coEvery { repository.getById("nonexistent") } returns null

        // When
        val result = useCase("nonexistent")

        // Then
        assertNull(result)
    }
}
