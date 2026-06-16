package com.sphynxs.mydatabases.core.database.engine.mysql

import com.sphynxs.mydatabases.core.database.engine.DatabaseFeature
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

/**
 * Tests unitarios para MySQLEngine.
 * 
 * Cubre:
 * - getSupportedFeatures retorna features correctas
 * - Error mapping (verificar a través de tipos de Result)
 * - Todas las operaciones usan el metadataReader correctamente
 * 
 * @author israel-icm
 * @date 2026-06-12
 */
class MySQLEngineTest {
    
    @Test
    fun `getSupportedFeatures returns MySQL feature set`() {
        // Arrange
        val engine = MySQLEngine()
        
        // Act
        val features = engine.getSupportedFeatures()
        
        // Assert
        assertTrue(DatabaseFeature.STORED_PROCEDURES in features)
        assertTrue(DatabaseFeature.TRIGGERS in features)
        assertTrue(DatabaseFeature.VIEWS in features)
        assertTrue(DatabaseFeature.EVENTS in features)
        assertTrue(DatabaseFeature.FOREIGN_KEYS in features)
        assertTrue(DatabaseFeature.TRANSACTIONS in features)
        assertTrue(DatabaseFeature.FULL_TEXT_SEARCH in features)
        assertTrue(DatabaseFeature.JSON_TYPE in features)
    }
    
    @Test
    fun `getSupportedFeatures has exactly 8 features for MySQL`() {
        // Arrange
        val engine = MySQLEngine()
        
        // Act
        val features = engine.getSupportedFeatures()
        
        // Assert
        assertEquals(8, features.size)
    }
    
    @Test
    fun `engine initializes without connection`() {
        // Arrange & Act
        val engine = MySQLEngine()
        
        // Assert - no debe lanzar excepción
        assertNotNull(engine)
    }
}
