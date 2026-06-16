package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.models.Column
import com.sphynxs.mydatabases.core.database.models.ColumnKey
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios para GetColumnsUseCase.
 */
class GetColumnsUseCaseTest {

    @Test
    fun `invoke calls repository getColumns with table name`() = runTest {
        // Arrange
        val mockRepository = mockk<DatabaseRepository>()
        val useCase = GetColumnsUseCase(mockRepository)
        
        val tableName = "users"
        val expectedColumns = listOf(
            Column(
                name = "id",
                type = "int(11)",
                nullable = false,
                key = ColumnKey.PRIMARY,
                default = null,
                extra = "auto_increment",
                comment = "Primary key"
            ),
            Column(
                name = "name",
                type = "varchar(255)",
                nullable = false,
                key = ColumnKey.NONE,
                default = null,
                extra = "",
                comment = "User name"
            )
        )
        
        coEvery { mockRepository.getColumns(any()) } returns Result.success(expectedColumns)
        
        // Act
        val result = useCase(tableName)
        
        // Assert
        assertTrue("Result should be success", result.isSuccess)
        assertEquals("Columns should match", expectedColumns, result.getOrNull())
        coVerify(exactly = 1) { mockRepository.getColumns(tableName) }
    }

    @Test
    fun `invoke returns empty list for table with no columns`() = runTest {
        // Arrange
        val mockRepository = mockk<DatabaseRepository>()
        val useCase = GetColumnsUseCase(mockRepository)
        
        val tableName = "empty_table"
        
        coEvery { mockRepository.getColumns(any()) } returns Result.success(emptyList())
        
        // Act
        val result = useCase(tableName)
        
        // Assert
        assertTrue("Result should be success", result.isSuccess)
        assertTrue("List should be empty", result.getOrNull()?.isEmpty() == true)
    }
}
