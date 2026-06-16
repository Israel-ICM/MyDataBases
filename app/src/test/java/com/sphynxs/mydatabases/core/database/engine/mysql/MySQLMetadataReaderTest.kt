package com.sphynxs.mydatabases.core.database.engine.mysql

import com.sphynxs.mydatabases.core.database.models.ColumnKey
import com.sphynxs.mydatabases.core.database.models.IndexType
import com.sphynxs.mydatabases.core.database.models.ReferentialAction
import com.sphynxs.mydatabases.core.database.models.TableType
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

/**
 * Tests unitarios para MySQLMetadataReader.
 * 
 * Cubre:
 * - readDatabases: parseo de información de schemas
 * - readTables: parseo de tablas con metadata
 * - readColumns: parseo de columnas con tipos y constraints
 * - readIndexes: parseo de índices compuestos
 * - readForeignKeys: parseo de relaciones
 * 
 * @author israel-icm
 * @date 2026-06-12
 */
class MySQLMetadataReaderTest {
    
    private lateinit var reader: MySQLMetadataReader
    private lateinit var mockConnection: Connection
    private lateinit var mockStatement: Statement
    private lateinit var mockResultSet: ResultSet
    
    @Before
    fun setup() {
        reader = MySQLMetadataReader()
        mockConnection = mockk(relaxed = true)
        mockStatement = mockk(relaxed = true)
        mockResultSet = mockk(relaxed = true)
    }
    
    @Test
    fun `readDatabases parses schema metadata correctly`() {
        // Arrange
        val query = "SELECT SCHEMA_NAME as name, DEFAULT_CHARACTER_SET_NAME as charset, DEFAULT_COLLATION_NAME as collation FROM information_schema.SCHEMATA"
        
        every { mockConnection.createStatement() } returns mockStatement
        every { mockStatement.executeQuery(query) } returns mockResultSet
        
        // Mock ResultSet con 2 databases
        every { mockResultSet.next() } returnsMany listOf(true, true, false)
        every { mockResultSet.getString("name") } returnsMany listOf("app_db", "test_db")
        every { mockResultSet.getString("charset") } returnsMany listOf("utf8mb4", "utf8mb4")
        every { mockResultSet.getString("collation") } returnsMany listOf("utf8mb4_unicode_ci", "utf8mb4_general_ci")
        
        // Act
        val databases = reader.readDatabases(mockConnection, query)
        
        // Assert
        assertEquals(2, databases.size)
        assertEquals("app_db", databases[0].name)
        assertEquals("utf8mb4", databases[0].charset)
        assertEquals("utf8mb4_unicode_ci", databases[0].collation)
        assertEquals("test_db", databases[1].name)
    }
    
    @Test
    fun `readDatabases returns empty list when no databases exist`() {
        // Arrange
        val query = "SELECT SCHEMA_NAME as name FROM information_schema.SCHEMATA"
        
        every { mockConnection.createStatement() } returns mockStatement
        every { mockStatement.executeQuery(query) } returns mockResultSet
        every { mockResultSet.next() } returns false // Sin resultados
        
        // Act
        val databases = reader.readDatabases(mockConnection, query)
        
        // Assert
        assertTrue(databases.isEmpty())
    }
    
    @Test
    fun `readTables parses table metadata with row counts and engines`() {
        // Arrange
        val query = "SELECT TABLE_NAME as name, TABLE_TYPE as type, ENGINE as engine, TABLE_ROWS as rowCount, DATA_LENGTH as dataLength FROM information_schema.TABLES WHERE TABLE_SCHEMA = ?"
        val database = "mydb"
        
        val mockPreparedStatement = mockk<java.sql.PreparedStatement>(relaxed = true)
        every { mockConnection.prepareStatement(query) } returns mockPreparedStatement
        every { mockPreparedStatement.executeQuery() } returns mockResultSet
        
        // Mock ResultSet con 2 tablas
        every { mockResultSet.next() } returnsMany listOf(true, true, false)
        every { mockResultSet.getString("name") } returnsMany listOf("users", "orders")
        every { mockResultSet.getString("type") } returnsMany listOf("BASE TABLE", "BASE TABLE")
        every { mockResultSet.getString("engine") } returnsMany listOf("InnoDB", "InnoDB")
        every { mockResultSet.getLong("rowCount") } returnsMany listOf(1500L, 3200L)
        every { mockResultSet.getLong("dataLength") } returnsMany listOf(49152L, 81920L)
        every { mockResultSet.getLong("createdAt") } returnsMany listOf(1672531200000L, 1672617600000L)
        every { mockResultSet.getString("comment") } returnsMany listOf("User accounts", "Customer orders")
        
        // Act
        val tables = reader.readTables(mockConnection, query, database)
        
        // Assert
        assertEquals(2, tables.size)
        assertEquals("users", tables[0].name)
        assertEquals(TableType.TABLE, tables[0].type)
        assertEquals("InnoDB", tables[0].engine)
        assertEquals(1500L, tables[0].rowCount)
    }
    
    @Test
    fun `readColumns parses column metadata with primary key identification`() {
        // Arrange
        val query = "SELECT COLUMN_NAME as name, COLUMN_TYPE as type, IS_NULLABLE as nullable, COLUMN_KEY as key, COLUMN_DEFAULT as default_value, EXTRA as extra FROM information_schema.COLUMNS WHERE TABLE_NAME = ?"
        val table = "users"
        
        val mockPreparedStatement = mockk<java.sql.PreparedStatement>(relaxed = true)
        every { mockConnection.prepareStatement(query) } returns mockPreparedStatement
        every { mockPreparedStatement.executeQuery() } returns mockResultSet
        
        // Mock ResultSet con 3 columnas
        every { mockResultSet.next() } returnsMany listOf(true, true, true, false)
        every { mockResultSet.getString("name") } returnsMany listOf("id", "name", "email")
        every { mockResultSet.getString("type") } returnsMany listOf("int(11)", "varchar(255)", "varchar(255)")
        every { mockResultSet.getString("nullable") } returnsMany listOf("NO", "NO", "YES")
        every { mockResultSet.getString("key") } returnsMany listOf("PRI", "", "UNI")
        every { mockResultSet.getString("default_value") } returnsMany listOf(null, null, null)
        every { mockResultSet.getString("extra") } returnsMany listOf("auto_increment", "", "")
        every { mockResultSet.getString("comment") } returnsMany listOf("", "", "")
        
        // Act
        val columns = reader.readColumns(mockConnection, query, table)
        
        // Assert
        assertEquals(3, columns.size)
        
        // Column 1: id (PRI KEY)
        assertEquals("id", columns[0].name)
        assertEquals("int(11)", columns[0].type)
        assertEquals(false, columns[0].nullable)
        assertEquals(ColumnKey.PRIMARY, columns[0].key)
        assertEquals("auto_increment", columns[0].extra)
        
        // Column 2: name
        assertEquals("name", columns[1].name)
        assertEquals(ColumnKey.NONE, columns[1].key)
        
        // Column 3: email (UNIQUE)
        assertEquals("email", columns[2].name)
        assertEquals(true, columns[2].nullable)
        assertEquals(ColumnKey.UNIQUE, columns[2].key)
    }
    
    @Test
    fun `readIndexes groups compound indexes correctly`() {
        // Arrange
        val query = "SELECT INDEX_NAME as name, COLUMN_NAME as column, NON_UNIQUE as nonUnique, INDEX_TYPE as type FROM information_schema.STATISTICS WHERE TABLE_NAME = ?"
        val table = "users"
        
        val mockPreparedStatement = mockk<java.sql.PreparedStatement>(relaxed = true)
        every { mockConnection.prepareStatement(query) } returns mockPreparedStatement
        every { mockPreparedStatement.executeQuery() } returns mockResultSet
        
        // Mock índice compuesto (name, email)
        every { mockResultSet.next() } returnsMany listOf(true, true, false)
        every { mockResultSet.getString("name") } returnsMany listOf("idx_name_email", "idx_name_email")
        every { mockResultSet.getString("column") } returnsMany listOf("name", "email")
        every { mockResultSet.getInt("nonUnique") } returnsMany listOf(1, 1)
        every { mockResultSet.getString("type") } returnsMany listOf("BTREE", "BTREE")
        
        // Act
        val indexes = reader.readIndexes(mockConnection, query, table)
        
        // Assert
        assertEquals(1, indexes.size) // 1 índice compuesto
        assertEquals("idx_name_email", indexes[0].name)
        assertEquals(2, indexes[0].columns.size) // 2 columnas
        assertEquals(listOf("name", "email"), indexes[0].columns)
        assertEquals(false, indexes[0].unique) // NON_UNIQUE = 1
        assertEquals(IndexType.BTREE, indexes[0].type)
    }
    
    @Test
    fun `readForeignKeys parses referential actions correctly`() {
        // Arrange
        val query = "SELECT CONSTRAINT_NAME as name, COLUMN_NAME as column, REFERENCED_TABLE_NAME as referencedTable, REFERENCED_COLUMN_NAME as referencedColumn, DELETE_RULE as onDelete, UPDATE_RULE as onUpdate FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_NAME = ?"
        val table = "orders"
        
        val mockPreparedStatement = mockk<java.sql.PreparedStatement>(relaxed = true)
        every { mockConnection.prepareStatement(query) } returns mockPreparedStatement
        every { mockPreparedStatement.executeQuery() } returns mockResultSet
        
        // Mock FK con CASCADE
        every { mockResultSet.next() } returnsMany listOf(true, false)
        every { mockResultSet.getString("name") } returns "fk_orders_user_id"
        every { mockResultSet.getString("column") } returns "user_id"
        every { mockResultSet.getString("referencedTable") } returns "users"
        every { mockResultSet.getString("referencedColumn") } returns "id"
        every { mockResultSet.getString("onDelete") } returns "CASCADE"
        every { mockResultSet.getString("onUpdate") } returns "RESTRICT"
        
        // Act
        val foreignKeys = reader.readForeignKeys(mockConnection, query, table)
        
        // Assert
        assertEquals(1, foreignKeys.size)
        assertEquals("fk_orders_user_id", foreignKeys[0].name)
        assertEquals("user_id", foreignKeys[0].column)
        assertEquals("users", foreignKeys[0].referencedTable)
        assertEquals("id", foreignKeys[0].referencedColumn)
        assertEquals(ReferentialAction.CASCADE, foreignKeys[0].onDelete)
        assertEquals(ReferentialAction.RESTRICT, foreignKeys[0].onUpdate)
    }
}
