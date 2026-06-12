package com.sphynxs.mydatabases.domain.usecases.connections

import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.domain.repositories.ConnectionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests para [TestConnectionUseCase].
 *
 * @author israel-icm
 * @date 2026-06-12
 */
class TestConnectionUseCaseTest {

    private lateinit var repository: ConnectionRepository
    private lateinit var useCase: TestConnectionUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = TestConnectionUseCase(repository)
    }

    @Test
    fun `invoke delegates test to repository`() = runTest {
        // Given
        val connection = ConnectionConfig(
            id = "test-conn",
            name = "Test Connection",
            type = DatabaseType.MYSQL,
            host = "localhost",
            port = 3306,
            database = "test",
            username = "admin",
            password = "pass"
        )

        coEvery { repository.testConnection(connection) } returns Result.success(Unit)

        // When
        val result = useCase(connection)

        // Then
        coVerify(exactly = 1) { repository.testConnection(connection) }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `invoke returns failure when test fails`() = runTest {
        // Given
        val connection = ConnectionConfig(
            id = "bad-conn",
            name = "Bad Connection",
            type = DatabaseType.MYSQL,
            host = "invalid-host",
            port = 3306,
            database = "test",
            username = "admin",
            password = "pass"
        )

        val error = Exception("Connection refused")
        coEvery { repository.testConnection(connection) } returns Result.failure(error)

        // When
        val result = useCase(connection)

        // Then
        assertTrue(result.isFailure)
    }
}
