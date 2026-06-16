package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.models.Database
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios para GetDatabasesUseCase.
 */
class GetDatabasesUseCaseTest {

    @Test
    fun `invoke calls repository getDatabases`() = runTest {
        // Arrange
        val mockRepository = mockk<DatabaseRepository>()
        val useCase = GetDatabasesUseCase(mockRepository)
        
        val expectedDatabases = listOf(
            Database(name = "db1", charset = "utf8mb4", collation = "utf8mb4_general_ci"),
            Database(name = "db2", charset = "utf8mb4", collation = "utf8mb4_general_ci")
        )
        
        coEvery { mockRepository.getDatabases() } returns Result.success(expectedDatabases)
        
        // Act
        val result = useCase()
        
        // Assert
        assertTrue("Result should be success", result.isSuccess)
        assertEquals("Databases should match", expectedDatabases, result.getOrNull())
        coVerify(exactly = 1) { mockRepository.getDatabases() }
    }

    @Test
    fun `invoke returns empty list when no databases`() = runTest {
        // Arrange
        val mockRepository = mockk<DatabaseRepository>()
        val useCase = GetDatabasesUseCase(mockRepository)
        
        coEvery { mockRepository.getDatabases() } returns Result.success(emptyList())
        
        // Act
        val result = useCase()
        
        // Assert
        assertTrue("Result should be success", result.isSuccess)
        assertTrue("List should be empty", result.getOrNull()?.isEmpty() == true)
    }
}
