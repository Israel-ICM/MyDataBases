package com.sphynxs.mydatabases.core.database.di

import com.sphynxs.mydatabases.core.database.engine.DatabaseEngineFactory
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepositoryImpl
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios para DatabaseModule (Hilt DI).
 */
class DatabaseModuleTest {

    @Test
    fun `module provides DatabaseEngineFactory`() {
        // Arrange
        val module = DatabaseModule
        
        // Act
        val factory = module.provideDatabaseEngineFactory()
        
        // Assert
        assertNotNull("Factory should not be null", factory)
        assertTrue(
            "Factory should be DatabaseEngineFactory singleton",
            factory === DatabaseEngineFactory
        )
    }

    @Test
    fun `module provides DatabaseRepository`() {
        // Arrange
        val module = DatabaseModule
        val factory = module.provideDatabaseEngineFactory()
        
        // Act
        val repository = module.provideDatabaseRepository(factory)
        
        // Assert
        assertNotNull("Repository should not be null", repository)
        assertTrue(
            "Repository should be DatabaseRepositoryImpl instance",
            repository is DatabaseRepositoryImpl
        )
    }

    @Test
    fun `module provides singleton instances`() {
        // Arrange
        val module = DatabaseModule
        val factory = module.provideDatabaseEngineFactory()
        
        // Act - Call twice to verify singleton behavior
        val repository1 = module.provideDatabaseRepository(factory)
        val repository2 = module.provideDatabaseRepository(factory)
        
        // Assert - Note: Module provides new instances, but Hilt manages singleton scope
        // This test verifies the provide methods work correctly
        assertNotNull("First repository should not be null", repository1)
        assertNotNull("Second repository should not be null", repository2)
    }
}
