package com.sphynxs.mydatabases.core.database.engine.mysql

import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

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
    fun `pool creates valid connection with minimal config`() = runTest {
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
        assertNotNull(connection, "Connection should not be null")
        assertTrue(connection.isValid(5), "Connection should be valid within 5 seconds")
        assertFalse(connection.isClosed, "Connection should not be closed")
    }
    
    @Test
    fun `pool creates connection with SSL enabled`() = runTest {
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
        assertNotNull(connection, "Connection with SSL should not be null")
        assertTrue(connection.isValid(5), "SSL connection should be valid")
    }
    
    @Test
    fun `pool reuses connections from pool`() = runTest {
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
        assertNotNull(conn2, "Reused connection should not be null")
        assertTrue(conn2.isValid(5), "Reused connection should be valid")
    }
    
    @Test
    fun `pool closes successfully and releases resources`() = runTest {
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
        assertTrue(connection.isClosed, "Connection should be closed after pool.close()")
    }
}
