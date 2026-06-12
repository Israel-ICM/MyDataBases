package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.models.QueryResult
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios para ExecuteQueryUseCase.
 */
class ExecuteQueryUseCaseTest {

    @Test
    fun `invoke calls repository executeQuery with query and params`() = runTest {
        // Arrange
        val mockRepository = mockk<DatabaseRepository>()
        val useCase = ExecuteQueryUseCase(mockRepository)
        
        val query = "SELECT * FROM users WHERE id = ?"
        val params = listOf(1)
        
        val expectedResult = QueryResult(
            columns = listOf("id", "name"),
            rows = listOf(mapOf("id" to 1, "name" to "Test User")),
            rowCount = 1,
            executionTimeMs = 50
        )
        
        coEvery { mockRepository.executeQuery(any(), any()) } returns Result.success(expectedResult)
        
        // Act
        val result = useCase(query, params)
        
        // Assert
        assertTrue("Result should be success", result.isSuccess)
        assertEquals("QueryResult should match", expectedResult, result.getOrNull())
        coVerify(exactly = 1) { mockRepository.executeQuery(query, params) }
    }

    @Test
    fun `invoke calls repository executeQuery with empty params by default`() = runTest {
        // Arrange
        val mockRepository = mockk<DatabaseRepository>()
        val useCase = ExecuteQueryUseCase(mockRepository)
        
        val query = "SELECT * FROM users"
        
        val expectedResult = QueryResult(
            columns = listOf("id", "name"),
            rows = emptyList(),
            rowCount = 0,
            executionTimeMs = 30
        )
        
        coEvery { mockRepository.executeQuery(any(), any()) } returns Result.success(expectedResult)
        
        // Act
        val result = useCase(query)
        
        // Assert
        assertTrue("Result should be success", result.isSuccess)
        coVerify(exactly = 1) { mockRepository.executeQuery(query, emptyList()) }
    }
}
