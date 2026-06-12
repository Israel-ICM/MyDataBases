package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.Connection
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Tests unitarios para ConnectToDatabaseUseCase.
 */
class ConnectToDatabaseUseCaseTest {

    @Test
    fun `invoke calls repository connect with correct config`() = runTest {
        // Arrange
        val mockRepository = mockk<DatabaseRepository>()
        val useCase = ConnectToDatabaseUseCase(mockRepository)
        
        val testConfig = ConnectionConfig(
            id = UUID.randomUUID().toString(),
            name = "Test DB",
            type = DatabaseType.MYSQL,
            host = "localhost",
            port = 3306,
            database = "test_db",
            username = "test_user",
            password = "test_pass"
        )
        
        val expectedConnection = Connection(
            id = testConfig.id,
            type = DatabaseType.MYSQL,
            database = "test_db",
            host = "localhost",
            port = 3306,
            username = "test_user",
            version = "8.0.33",
            connectedAt = System.currentTimeMillis()
        )
        
        coEvery { mockRepository.connect(any()) } returns Result.success(expectedConnection)
        
        // Act
        val result = useCase(testConfig)
        
        // Assert
        assertTrue("Result should be success", result.isSuccess)
        assertEquals("Connection should match", expectedConnection, result.getOrNull())
        coVerify(exactly = 1) { mockRepository.connect(testConfig) }
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        // Arrange
        val mockRepository = mockk<DatabaseRepository>()
        val useCase = ConnectToDatabaseUseCase(mockRepository)
        
        val testConfig = ConnectionConfig(
            id = UUID.randomUUID().toString(),
            name = "Test DB",
            type = DatabaseType.MYSQL,
            host = "invalid-host",
            port = 3306,
            database = "test_db",
            username = "test_user",
            password = "test_pass"
        )
        
        val expectedException = Exception("Connection failed")
        coEvery { mockRepository.connect(any()) } returns Result.failure(expectedException)
        
        // Act
        val result = useCase(testConfig)
        
        // Assert
        assertTrue("Result should be failure", result.isFailure)
        assertEquals("Exception should match", expectedException, result.exceptionOrNull())
    }
}
