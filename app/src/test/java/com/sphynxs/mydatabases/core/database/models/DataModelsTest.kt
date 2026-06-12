package com.sphynxs.mydatabases.core.database.models

import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for core data models.
 *
 * Tests verify:
 * - Data classes are instantiable with correct properties
 * - Enums have all expected values
 * - Collections and maps work correctly
 *
 * @author israel-icm
 * @date 2026-06-11
 */
class DataModelsTest {

    @Test
    fun `Connection model holds all properties`() {
        // Given: A Connection instance
        val connection = Connection(
            id = "test-id",
            type = DatabaseType.MYSQL,
            database = "myapp",
            host = "localhost",
            port = 3306,
            username = "admin",
            version = "8.0.33",
            connectedAt = 1702345678000L
        )

        // Then: All properties are accessible
        assertEquals("test-id", connection.id)
        assertEquals(DatabaseType.MYSQL, connection.type)
        assertEquals("myapp", connection.database)
        assertEquals("localhost", connection.host)
        assertEquals(3306, connection.port)
        assertEquals("admin", connection.username)
        assertEquals("8.0.33", connection.version)
        assertEquals(1702345678000L, connection.connectedAt)
    }

    @Test
    fun `QueryResult model holds columns and rows`() {
        // Given: A QueryResult with 2 columns and 3 rows
        val columns = listOf("id", "name")
        val rows = listOf(
            mapOf("id" to 1, "name" to "Alice"),
            mapOf("id" to 2, "name" to "Bob"),
            mapOf("id" to 3, "name" to "Charlie")
        )
        val result = QueryResult(
            columns = columns,
            rows = rows,
            rowCount = 3,
            executionTimeMs = 150L,
            warnings = emptyList()
        )

        // Then: All properties are accessible
        assertEquals(columns, result.columns)
        assertEquals(rows, result.rows)
        assertEquals(3, result.rowCount)
        assertEquals(150L, result.executionTimeMs)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `QueryResult supports empty result`() {
        // Given: An empty QueryResult
        val result = QueryResult(
            columns = listOf("id", "name"),
            rows = emptyList(),
            rowCount = 0,
            executionTimeMs = 50L
        )

        // Then: Properties reflect empty state
        assertEquals(2, result.columns.size)
        assertTrue(result.rows.isEmpty())
        assertEquals(0, result.rowCount)
    }

    @Test
    fun `Table model holds metadata`() {
        // Given: A Table instance
        val table = Table(
            name = "users",
            database = "myapp",
            type = TableType.TABLE,
            engine = "InnoDB",
            rowCount = 1000L,
            dataLength = 65536L,
            createdAt = 1702345678000L,
            comment = "User accounts"
        )

        // Then: All properties are accessible
        assertEquals("users", table.name)
        assertEquals("myapp", table.database)
        assertEquals(TableType.TABLE, table.type)
        assertEquals("InnoDB", table.engine)
        assertEquals(1000L, table.rowCount)
        assertEquals(65536L, table.dataLength)
        assertEquals(1702345678000L, table.createdAt)
        assertEquals("User accounts", table.comment)
    }

    @Test
    fun `TableType enum has expected values`() {
        // Then: All table types are present
        val types = TableType.values()
        assertTrue(types.contains(TableType.TABLE))
        assertTrue(types.contains(TableType.VIEW))
        assertTrue(types.contains(TableType.SYSTEM_TABLE))
        assertEquals(3, types.size)
    }

    @Test
    fun `Column model holds type information`() {
        // Given: A Column instance
        val column = Column(
            name = "id",
            type = "int(11)",
            nullable = false,
            key = ColumnKey.PRIMARY,
            default = null,
            extra = "auto_increment",
            comment = "Primary key"
        )

        // Then: All properties are accessible
        assertEquals("id", column.name)
        assertEquals("int(11)", column.type)
        assertEquals(false, column.nullable)
        assertEquals(ColumnKey.PRIMARY, column.key)
        assertEquals(null, column.default)
        assertEquals("auto_increment", column.extra)
        assertEquals("Primary key", column.comment)
    }

    @Test
    fun `ColumnKey enum has expected values`() {
        // Then: All column key types are present
        val keys = ColumnKey.values()
        assertTrue(keys.contains(ColumnKey.PRIMARY))
        assertTrue(keys.contains(ColumnKey.UNIQUE))
        assertTrue(keys.contains(ColumnKey.MULTIPLE))
        assertTrue(keys.contains(ColumnKey.NONE))
        assertEquals(4, keys.size)
    }

    @Test
    fun `Database model holds charset and collation`() {
        // Given: A Database instance
        val database = Database(
            name = "myapp",
            charset = "utf8mb4",
            collation = "utf8mb4_unicode_ci"
        )

        // Then: All properties are accessible
        assertEquals("myapp", database.name)
        assertEquals("utf8mb4", database.charset)
        assertEquals("utf8mb4_unicode_ci", database.collation)
    }

    @Test
    fun `Index model holds column list and metadata`() {
        // Given: An Index instance
        val index = Index(
            name = "idx_email",
            columns = listOf("email"),
            unique = true,
            type = IndexType.BTREE
        )

        // Then: All properties are accessible
        assertEquals("idx_email", index.name)
        assertEquals(listOf("email"), index.columns)
        assertEquals(true, index.unique)
        assertEquals(IndexType.BTREE, index.type)
    }

    @Test
    fun `Index supports composite indexes`() {
        // Given: A composite index
        val index = Index(
            name = "idx_name_email",
            columns = listOf("name", "email"),
            unique = false,
            type = IndexType.BTREE
        )

        // Then: Multiple columns are preserved
        assertEquals(2, index.columns.size)
        assertTrue(index.columns.contains("name"))
        assertTrue(index.columns.contains("email"))
    }

    @Test
    fun `IndexType enum has expected values`() {
        // Then: All index types are present
        val types = IndexType.values()
        assertTrue(types.contains(IndexType.BTREE))
        assertTrue(types.contains(IndexType.HASH))
        assertTrue(types.contains(IndexType.FULLTEXT))
        assertTrue(types.contains(IndexType.SPATIAL))
        assertEquals(4, types.size)
    }

    @Test
    fun `ForeignKey model holds references and actions`() {
        // Given: A ForeignKey instance
        val fk = ForeignKey(
            name = "fk_user_id",
            column = "user_id",
            referencedTable = "users",
            referencedColumn = "id",
            onDelete = ReferentialAction.CASCADE,
            onUpdate = ReferentialAction.RESTRICT
        )

        // Then: All properties are accessible
        assertEquals("fk_user_id", fk.name)
        assertEquals("user_id", fk.column)
        assertEquals("users", fk.referencedTable)
        assertEquals("id", fk.referencedColumn)
        assertEquals(ReferentialAction.CASCADE, fk.onDelete)
        assertEquals(ReferentialAction.RESTRICT, fk.onUpdate)
    }

    @Test
    fun `ReferentialAction enum has expected values`() {
        // Then: All referential actions are present
        val actions = ReferentialAction.values()
        assertTrue(actions.contains(ReferentialAction.CASCADE))
        assertTrue(actions.contains(ReferentialAction.SET_NULL))
        assertTrue(actions.contains(ReferentialAction.RESTRICT))
        assertTrue(actions.contains(ReferentialAction.NO_ACTION))
        assertEquals(4, actions.size)
    }

    @Test
    fun `Transaction can be created with callbacks`() {
        // Given: Mock callbacks
        var commitCalled = false
        var rollbackCalled = false
        val mockConnection = mockk<java.sql.Connection>(relaxed = true)
        
        val transaction = Transaction(
            connection = mockConnection,
            onCommit = { commitCalled = true },
            onRollback = { rollbackCalled = true }
        )

        // Then: Transaction is created
        assertNotNull(transaction)
    }
}
