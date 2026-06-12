package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.models.Table
import com.sphynxs.mydatabases.core.database.models.TableType
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios para GetTablesUseCase.
 */
class GetTablesUseCaseTest {

    @Test
    fun `invoke calls repository getTables with database name`() = runTest {
        // Arrange
        val mockRepository = mockk<DatabaseRepository>()
        val useCase = GetTablesUseCase(mockRepository)
        
        val databaseName = "test_db"
        val expectedTables = listOf(
            Table(
                name = "users",
                database = databaseName,
                type = TableType.TABLE,
                engine = "InnoDB",
                rowCount = 100,
                dataLength = 16384,
                createdAt = System.currentTimeMillis(),
                comment = "Users table"
            ),
            Table(
                name = "posts",
                database = databaseName,
                type = TableType.TABLE,
                engine = "InnoDB",
                rowCount = 50,
                dataLength = 8192,
                createdAt = System.currentTimeMillis(),
                comment = "Posts table"
            )
        )
        
        coEvery { mockRepository.getTables(any()) } returns Result.success(expectedTables)
        
        // Act
        val result = useCase(databaseName)
        
        // Assert
        assertTrue("Result should be success", result.isSuccess)
        assertEquals("Tables should match", expectedTables, result.getOrNull())
        coVerify(exactly = 1) { mockRepository.getTables(databaseName) }
    }

    @Test
    fun `invoke returns empty list for database with no tables`() = runTest {
        // Arrange
        val mockRepository = mockk<DatabaseRepository>()
        val useCase = GetTablesUseCase(mockRepository)
        
        val databaseName = "empty_db"
        
        coEvery { mockRepository.getTables(any()) } returns Result.success(emptyList())
        
        // Act
        val result = useCase(databaseName)
        
        // Assert
        assertTrue("Result should be success", result.isSuccess)
        assertTrue("List should be empty", result.getOrNull()?.isEmpty() == true)
    }
}
