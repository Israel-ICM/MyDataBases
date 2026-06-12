package com.sphynxs.mydatabases.domain.usecases.connections

import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.domain.repositories.ConnectionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Tests para [UpdateConnectionUseCase].
 *
 * @author israel-icm
 * @date 2026-06-12
 */
class UpdateConnectionUseCaseTest {

    private lateinit var repository: ConnectionRepository
    private lateinit var useCase: UpdateConnectionUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = UpdateConnectionUseCase(repository)
    }

    @Test
    fun `invoke delegates save to repository with existing connection`() = runTest {
        // Given
        val connection = ConnectionConfig(
            id = "existing-conn",
            name = "Updated Connection",
            type = DatabaseType.MYSQL,
            host = "localhost",
            port = 3306,
            database = "updated_db",
            username = "admin",
            password = "newpass"
        )

        coEvery { repository.save(connection) } returns Unit

        // When
        useCase(connection)

        // Then
        coVerify(exactly = 1) { repository.save(connection) }
    }
}
