package com.sphynxs.mydatabases.core.database.engine.mariadb

import com.sphynxs.mydatabases.core.database.engine.DatabaseFeature
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests para MariaDBEngine.
 * 
 * MariaDB es un fork de MySQL que comparte la mayoría de la funcionalidad.
 * La diferencia principal es el soporte para SEQUENCES.
 * 
 * @author israel-icm
 * @date 2026-06-12
 */
class MariaDBEngineTest {
    
    /**
     * RED: Test que verifica que MariaDB soporta SEQUENCES.
     * MariaDB tiene SEQUENCES nativas (CREATE SEQUENCE), mientras que MySQL no.
     */
    @Test
    fun `getSupportedFeatures includes SEQUENCES for MariaDB`() {
        // Arrange
        val engine = MariaDBEngine()
        
        // Act
        val features = engine.getSupportedFeatures()
        
        // Assert
        assertTrue(
            "MariaDB debe soportar SEQUENCES (feature nativa de MariaDB)",
            DatabaseFeature.SEQUENCES in features
        )
    }
    
    /**
     * TRIANGULATE: Verificar que MariaDB soporta todas las features de MySQL.
     * MariaDB es un superset de MySQL, por lo que debe heredar todas sus features.
     */
    @Test
    fun `getSupportedFeatures includes all MySQL features`() {
        // Arrange
        val engine = MariaDBEngine()
        
        // Act
        val features = engine.getSupportedFeatures()
        
        // Assert - MariaDB debe tener todas estas features de MySQL
        val expectedFeatures = setOf(
            DatabaseFeature.STORED_PROCEDURES,
            DatabaseFeature.TRIGGERS,
            DatabaseFeature.VIEWS,
            DatabaseFeature.EVENTS,
            DatabaseFeature.FOREIGN_KEYS,
            DatabaseFeature.TRANSACTIONS,
            DatabaseFeature.FULL_TEXT_SEARCH,
            DatabaseFeature.JSON_TYPE
        )
        
        assertTrue(
            "MariaDB debe soportar todas las features de MySQL",
            features.containsAll(expectedFeatures)
        )
    }
    
    /**
     * TRIANGULATE: Verificar que el total de features es correcto.
     * MariaDB = MySQL features + SEQUENCES = 9 features total.
     */
    @Test
    fun `getSupportedFeatures returns exactly 9 features`() {
        // Arrange
        val engine = MariaDBEngine()
        
        // Act
        val features = engine.getSupportedFeatures()
        
        // Assert
        assertEquals(
            "MariaDB debe tener 9 features (8 de MySQL + SEQUENCES)",
            9,
            features.size
        )
    }
}
