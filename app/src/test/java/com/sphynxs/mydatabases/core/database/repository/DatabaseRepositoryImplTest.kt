package com.sphynxs.mydatabases.core.database.repository

import com.sphynxs.mydatabases.core.database.engine.DatabaseEngineFactory
import com.sphynxs.mydatabases.core.database.models.DatabaseError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios para DatabaseRepositoryImpl.
 */
class DatabaseRepositoryImplTest {

    @Test
    fun `executeQuery returns failure when no engine connected`() = runTest {
        // Arrange
        val repository = DatabaseRepositoryImpl(DatabaseEngineFactory)
        
        // Act
        val result = repository.executeQuery("SELECT 1")
        
        // Assert
        assertTrue("Result should be failure", result.isFailure)
        assertTrue(
            "Error should be ConnectionFailed",
            result.exceptionOrNull() is DatabaseError.ConnectionFailed
        )
    }

    @Test
    fun `executeUpdate returns failure when no engine connected`() = runTest {
        // Arrange
        val repository = DatabaseRepositoryImpl(DatabaseEngineFactory)
        
        // Act
        val result = repository.executeUpdate("UPDATE users SET name = 'test'")
        
        // Assert
        assertTrue("Result should be failure", result.isFailure)
        assertTrue(
            "Error should be ConnectionFailed",
            result.exceptionOrNull() is DatabaseError.ConnectionFailed
        )
    }

    @Test
    fun `getDatabases returns failure when no engine connected`() = runTest {
        // Arrange
        val repository = DatabaseRepositoryImpl(DatabaseEngineFactory)
        
        // Act
        val result = repository.getDatabases()
        
        // Assert
        assertTrue("Result should be failure", result.isFailure)
        assertTrue(
            "Error should be ConnectionFailed",
            result.exceptionOrNull() is DatabaseError.ConnectionFailed
        )
    }

    @Test
    fun `getTables returns failure when no engine connected`() = runTest {
        // Arrange
        val repository = DatabaseRepositoryImpl(DatabaseEngineFactory)
        
        // Act
        val result = repository.getTables("test_db")
        
        // Assert
        assertTrue("Result should be failure", result.isFailure)
        assertTrue(
            "Error should be ConnectionFailed",
            result.exceptionOrNull() is DatabaseError.ConnectionFailed
        )
    }

    @Test
    fun `getColumns returns failure when no engine connected`() = runTest {
        // Arrange
        val repository = DatabaseRepositoryImpl(DatabaseEngineFactory)
        
        // Act
        val result = repository.getColumns("users")
        
        // Assert
        assertTrue("Result should be failure", result.isFailure)
        assertTrue(
            "Error should be ConnectionFailed",
            result.exceptionOrNull() is DatabaseError.ConnectionFailed
        )
    }
}
