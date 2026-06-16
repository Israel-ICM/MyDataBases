package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios para ExecuteUpdateUseCase.
 */
class ExecuteUpdateUseCaseTest {

    @Test
    fun `invoke calls repository executeUpdate with query and params`() = runTest {
        // Arrange
        val mockRepository = mockk<DatabaseRepository>()
        val useCase = ExecuteUpdateUseCase(mockRepository)
        
        val query = "UPDATE users SET name = ? WHERE id = ?"
        val params = listOf("Updated Name", 1)
        val expectedAffectedRows = 1
        
        coEvery { mockRepository.executeUpdate(any(), any()) } returns Result.success(expectedAffectedRows)
        
        // Act
        val result = useCase(query, params)
        
        // Assert
        assertTrue("Result should be success", result.isSuccess)
        assertEquals("Affected rows should match", expectedAffectedRows, result.getOrNull())
        coVerify(exactly = 1) { mockRepository.executeUpdate(query, params) }
    }

    @Test
    fun `invoke returns zero when no rows affected`() = runTest {
        // Arrange
        val mockRepository = mockk<DatabaseRepository>()
        val useCase = ExecuteUpdateUseCase(mockRepository)
        
        val query = "UPDATE users SET name = 'test' WHERE id = 999"
        val expectedAffectedRows = 0
        
        coEvery { mockRepository.executeUpdate(any(), any()) } returns Result.success(expectedAffectedRows)
        
        // Act
        val result = useCase(query)
        
        // Assert
        assertTrue("Result should be success", result.isSuccess)
        assertEquals("Affected rows should be 0", 0, result.getOrNull())
    }
}
