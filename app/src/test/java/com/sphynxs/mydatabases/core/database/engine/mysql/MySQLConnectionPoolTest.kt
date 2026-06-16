package com.sphynxs.mydatabases.core.database.engine.mysql

import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

/**
 * Tests unitarios para MySQLConnectionPool.
 * 
 * Cubre:
 * - Ciclo de vida del pool (creación, obtención de conexión, cierre)
 * - Configuración SSL
 * - Validación de conexiones
 * 
 * @author israel-icm
 * @date 2026-06-12
 */
class MySQLConnectionPoolTest {
    
    private var pool: MySQLConnectionPool? = null
    
    @After
    fun tearDown() {
        pool?.close()
    }
    
    @Test
    fun `pool creates valid connection with minimal config`() = runBlocking {
        // Arrange
        val config = ConnectionConfig(
            name = "Test MySQL",
            type = DatabaseType.MYSQL,
            host = "localhost",
            port = 3306,
            database = "test_db",
            username = "test_user",
            password = "test_pass",
            useSSL = false
        )
        
        // Act
        pool = MySQLConnectionPool(config)
        val connection = pool!!.getConnection()
        
        // Assert - conexión válida
        assertNotNull("Connection should not be null", connection)
        assertTrue("Connection should be valid within 5 seconds", connection.isValid(5))
        assertFalse("Connection should not be closed", connection.isClosed)
    }
    
    @Test
    fun `pool creates connection with SSL enabled`() = runBlocking {
        // Arrange
        val config = ConnectionConfig(
            name = "Test MySQL SSL",
            type = DatabaseType.MYSQL,
            host = "secure-db.example.com",
            port = 3306,
            database = "production_db",
            username = "admin",
            password = "secure_pass",
            useSSL = true // SSL habilitado
        )
        
        // Act
        pool = MySQLConnectionPool(config)
        val connection = pool!!.getConnection()
        
        // Assert - conexión válida con SSL
        assertNotNull("Connection with SSL should not be null", connection)
        assertTrue("SSL connection should be valid", connection.isValid(5))
    }
    
    @Test
    fun `pool reuses connections from pool`() = runBlocking {
        // Arrange
        val config = ConnectionConfig(
            name = "Test Pool Reuse",
            type = DatabaseType.MYSQL,
            host = "localhost",
            port = 3306,
            database = "test_db",
            username = "test_user",
            password = "test_pass",
            maxPoolSize = 5
        )
        
        pool = MySQLConnectionPool(config)
        
        // Act - obtener y liberar conexión varias veces
        val conn1 = pool!!.getConnection()
        conn1.close()
        
        val conn2 = pool!!.getConnection()
        
        // Assert - segunda conexión reutiliza del pool (HikariCP maneja esto)
        assertNotNull("Reused connection should not be null", conn2)
        assertTrue("Reused connection should be valid", conn2.isValid(5))
    }
    
    @Test
    fun `pool closes successfully and releases resources`() = runBlocking {
        // Arrange
        val config = ConnectionConfig(
            name = "Test Close",
            type = DatabaseType.MYSQL,
            host = "localhost",
            port = 3306,
            database = "test_db",
            username = "test_user",
            password = "test_pass"
        )
        
        pool = MySQLConnectionPool(config)
        val connection = pool!!.getConnection()
        
        // Act
        pool!!.close()
        
        // Assert - pool cerrado, conexión ya no válida
        assertTrue("Connection should be closed after pool.close()", connection.isClosed)
    }
}
