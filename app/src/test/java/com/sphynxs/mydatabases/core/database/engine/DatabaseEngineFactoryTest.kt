package com.sphynxs.mydatabases.core.database.engine

import com.sphynxs.mydatabases.core.database.engine.mariadb.MariaDBEngine
import com.sphynxs.mydatabases.core.database.engine.mysql.MySQLEngine
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests para DatabaseEngineFactory.
 * 
 * Verifica que el factory crea las instancias correctas de DatabaseEngine
 * según el tipo especificado.
 * 
 * @author israel-icm
 * @date 2026-06-12
 */
class DatabaseEngineFactoryTest {
    
    /**
     * RED: Test que verifica que el factory crea MySQLEngine para tipo MYSQL.
     */
    @Test
    fun `create returns MySQLEngine when type is MYSQL`() {
        // Arrange
        val type = DatabaseType.MYSQL
        
        // Act
        val engine = DatabaseEngineFactory.create(type)
        
        // Assert
        assertTrue(
            "Factory debe crear MySQLEngine para tipo MYSQL",
            engine is MySQLEngine
        )
    }
    
    /**
     * TRIANGULATE: Verificar que el factory crea MariaDBEngine para tipo MARIADB.
     */
    @Test
    fun `create returns MariaDBEngine when type is MARIADB`() {
        // Arrange
        val type = DatabaseType.MARIADB
        
        // Act
        val engine = DatabaseEngineFactory.create(type)
        
        // Assert
        assertTrue(
            "Factory debe crear MariaDBEngine para tipo MARIADB",
            engine is MariaDBEngine
        )
    }
    
    /**
     * TRIANGULATE: Verificar que el factory lanza NotImplementedError para PostgreSQL.
     * PostgreSQL será implementado en v1.1.
     */
    @Test(expected = NotImplementedError::class)
    fun `create throws NotImplementedError when type is POSTGRESQL`() {
        // Arrange
        val type = DatabaseType.POSTGRESQL
        
        // Act
        DatabaseEngineFactory.create(type)
        
        // Assert - espera NotImplementedError
    }
    
    /**
     * TRIANGULATE: Verificar que el factory lanza NotImplementedError para SQLite.
     * SQLite será implementado en v1.1.
     */
    @Test(expected = NotImplementedError::class)
    fun `create throws NotImplementedError when type is SQLITE`() {
        // Arrange
        val type = DatabaseType.SQLITE
        
        // Act
        DatabaseEngineFactory.create(type)
        
        // Assert - espera NotImplementedError
    }
    
    /**
     * TRIANGULATE: Verificar el mensaje de error para PostgreSQL.
     * El mensaje debe indicar claramente que será implementado en v1.1.
     */
    @Test
    fun `create throws NotImplementedError with correct message for POSTGRESQL`() {
        // Arrange
        val type = DatabaseType.POSTGRESQL
        
        // Act & Assert
        try {
            DatabaseEngineFactory.create(type)
            fail("Debería lanzar NotImplementedError")
        } catch (e: NotImplementedError) {
            assertTrue(
                "Mensaje debe mencionar v1.1 para PostgreSQL",
                e.message?.contains("PostgreSQL") == true && e.message?.contains("v1.1") == true
            )
        }
    }
    
    /**
     * TRIANGULATE: Verificar el mensaje de error para SQLite.
     * El mensaje debe indicar claramente que será implementado en v1.1.
     */
    @Test
    fun `create throws NotImplementedError with correct message for SQLITE`() {
        // Arrange
        val type = DatabaseType.SQLITE
        
        // Act & Assert
        try {
            DatabaseEngineFactory.create(type)
            fail("Debería lanzar NotImplementedError")
        } catch (e: NotImplementedError) {
            assertTrue(
                "Mensaje debe mencionar v1.1 para SQLite",
                e.message?.contains("SQLite") == true && e.message?.contains("v1.1") == true
            )
        }
    }
}
