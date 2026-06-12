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
 * Tests para [SaveConnectionUseCase].
 *
 * @author israel-icm
 * @date 2026-06-12
 */
class SaveConnectionUseCaseTest {

    private lateinit var repository: ConnectionRepository
    private lateinit var useCase: SaveConnectionUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = SaveConnectionUseCase(repository)
    }

    @Test
    fun `invoke delegates save to repository`() = runTest {
        // Given
        val connection = ConnectionConfig(
            id = "new-conn",
            name = "New Connection",
            type = DatabaseType.MYSQL,
            host = "localhost",
            port = 3306,
            database = "newdb",
            username = "user",
            password = "password"
        )

        coEvery { repository.save(connection) } returns Unit

        // When
        useCase(connection)

        // Then
        coVerify(exactly = 1) { repository.save(connection) }
    }
}
