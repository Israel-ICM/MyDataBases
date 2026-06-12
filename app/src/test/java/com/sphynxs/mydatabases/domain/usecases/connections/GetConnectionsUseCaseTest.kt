package com.sphynxs.mydatabases.domain.usecases.connections

import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.domain.repositories.ConnectionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Tests para [GetConnectionsUseCase].
 *
 * Verifica que el caso de uso:
 * 1. Delega correctamente al repositorio
 * 2. Retorna el Flow sin transformar
 *
 * @author israel-icm
 * @date 2026-06-12
 */
class GetConnectionsUseCaseTest {

    private lateinit var repository: ConnectionRepository
    private lateinit var useCase: GetConnectionsUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetConnectionsUseCase(repository)
    }

    @Test
    fun `invoke returns all connections from repository`() = runTest {
        // Given
        val connections = listOf(
            ConnectionConfig(
                id = "1",
                name = "MySQL Prod",
                type = DatabaseType.MYSQL,
                host = "localhost",
                port = 3306,
                database = "prod",
                username = "admin",
                password = "pass"
            ),
            ConnectionConfig(
                id = "2",
                name = "MariaDB Dev",
                type = DatabaseType.MARIADB,
                host = "127.0.0.1",
                port = 3307,
                database = "dev",
                username = "dev",
                password = "devpass"
            )
        )

        every { repository.getAll() } returns flowOf(connections)

        // When
        val result = useCase().first()

        // Then
        verify(exactly = 1) { repository.getAll() }
        assertEquals(2, result.size)
        assertEquals("MySQL Prod", result[0].name)
        assertEquals("MariaDB Dev", result[1].name)
    }

    @Test
    fun `invoke returns empty list when no connections exist`() = runTest {
        // Given
        every { repository.getAll() } returns flowOf(emptyList())

        // When
        val result = useCase().first()

        // Then
        assertEquals(0, result.size)
    }
}
