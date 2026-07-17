package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.models.ColumnDefinition
import com.sphynxs.mydatabases.core.database.models.DatabaseError
import com.sphynxs.mydatabases.core.database.models.SqlColumnType
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * TDD tests for `CreateTableUseCase` DDL building and execution (change `create-table`).
 *
 * Covers:
 * - Valid simple-table DDL and identifier rejection (table + column names)
 * - Per-type length/decimal formatting
 * - PRIMARY KEY clause placement
 * - Generated-column DDL branch (`GENERATED ALWAYS AS (...) VIRTUAL|STORED`), base-type
 *   length/decimals retained, no `NULL`/`DEFAULT` emitted, `COMMENT` still applied
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-07-15
 */
class CreateTableUseCaseTest {

    private lateinit var repository: DatabaseRepository
    private lateinit var useCase: CreateTableUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = CreateTableUseCase(repository)
    }

    // --- Task 1.4: valid simple-table DDL ---

    @Test
    fun `invoke builds and executes simple CREATE TABLE DDL`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        val result = useCase(
            name = "users",
            columns = listOf(
                ColumnDefinition(name = "id", type = SqlColumnType.Int, nullable = false, isPrimaryKey = true),
                ColumnDefinition(name = "email", type = SqlColumnType.VarChar, length = 255, nullable = false),
            )
        )

        assertTrue(result.isSuccess)
        assertEquals(
            "CREATE TABLE `users` (`id` INT NOT NULL, `email` VARCHAR(255) NOT NULL, PRIMARY KEY (`id`))",
            sqlSlot.captured
        )
        coVerify(exactly = 1) { repository.executeUpdate(any(), any()) }
    }

    @Test
    fun `invoke omits NOT NULL clause when column is nullable`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "users",
            columns = listOf(ColumnDefinition(name = "nickname", type = SqlColumnType.VarChar, length = 32, nullable = true))
        )

        assertEquals("CREATE TABLE `users` (`nickname` VARCHAR(32))", sqlSlot.captured)
    }

    // --- Task 1.4: identifier rejection ---

    @Test
    fun `invoke rejects invalid table name with a space`() = runTest {
        val result = useCase(
            name = "user table",
            columns = listOf(ColumnDefinition(name = "id", type = SqlColumnType.Int))
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DatabaseError.InvalidConfiguration)
        coVerify(exactly = 0) { repository.executeUpdate(any(), any()) }
    }

    @Test
    fun `invoke rejects invalid column name with a space`() = runTest {
        val result = useCase(
            name = "users",
            columns = listOf(ColumnDefinition(name = "user name", type = SqlColumnType.VarChar, length = 32))
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DatabaseError.InvalidConfiguration)
        coVerify(exactly = 0) { repository.executeUpdate(any(), any()) }
    }

    @Test
    fun `invoke rejects empty columns list`() = runTest {
        val result = useCase(name = "users", columns = emptyList())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DatabaseError.InvalidConfiguration)
        coVerify(exactly = 0) { repository.executeUpdate(any(), any()) }
    }

    // --- Task 1.4: per-type length/decimal formatting ---

    @Test
    fun `invoke formats DECIMAL column with length and decimals`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "products",
            columns = listOf(ColumnDefinition(name = "price", type = SqlColumnType.Decimal, length = 10, decimals = 2, nullable = false))
        )

        assertTrue(sqlSlot.captured.contains("`price` DECIMAL(10,2) NOT NULL"))
    }

    @Test
    fun `invoke ignores length for non length-bearing type`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "logs",
            columns = listOf(ColumnDefinition(name = "created_at", type = SqlColumnType.DateTime, length = 50, nullable = false))
        )

        assertTrue(sqlSlot.captured.contains("`created_at` DATETIME NOT NULL"))
        assertFalse(sqlSlot.captured.contains("DATETIME("))
    }

    // --- Task 1.4: PRIMARY KEY clause placement ---

    @Test
    fun `invoke appends PRIMARY KEY clause after all columns`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "orders",
            columns = listOf(
                ColumnDefinition(name = "id", type = SqlColumnType.Int, nullable = false, isPrimaryKey = true),
                ColumnDefinition(name = "total", type = SqlColumnType.Decimal, length = 10, decimals = 2, nullable = false),
            )
        )

        assertEquals(
            "CREATE TABLE `orders` (`id` INT NOT NULL, `total` DECIMAL(10,2) NOT NULL, PRIMARY KEY (`id`))",
            sqlSlot.captured
        )
    }

    @Test
    fun `invoke omits PRIMARY KEY clause when no column is a key`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "logs",
            columns = listOf(ColumnDefinition(name = "message", type = SqlColumnType.Text, nullable = true))
        )

        assertFalse(sqlSlot.captured.contains("PRIMARY KEY"))
    }

    // --- Task 1.6: generated-column DDL branch ---

    @Test
    fun `invoke emits GENERATED ALWAYS AS VIRTUAL for non-key virtual column`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "orders",
            columns = listOf(
                ColumnDefinition(
                    name = "total_with_tax",
                    type = SqlColumnType.Decimal,
                    length = 10,
                    decimals = 2,
                    isVirtual = true,
                    expression = "price * 1.16",
                    isPrimaryKey = false,
                )
            )
        )

        assertEquals(
            "CREATE TABLE `orders` (`total_with_tax` DECIMAL(10,2) GENERATED ALWAYS AS (price * 1.16) VIRTUAL)",
            sqlSlot.captured
        )
    }

    @Test
    fun `invoke forces STORED for a generated key column`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "orders",
            columns = listOf(
                ColumnDefinition(
                    name = "id",
                    type = SqlColumnType.Int,
                    isVirtual = true,
                    expression = "1",
                    isPrimaryKey = true,
                )
            )
        )

        assertEquals(
            "CREATE TABLE `orders` (`id` INT GENERATED ALWAYS AS (1) STORED, PRIMARY KEY (`id`))",
            sqlSlot.captured
        )
    }

    @Test
    fun `invoke never emits NULL or NOT NULL for a generated column`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "orders",
            columns = listOf(
                ColumnDefinition(
                    name = "total_with_tax",
                    type = SqlColumnType.Decimal,
                    length = 10,
                    decimals = 2,
                    nullable = false,
                    isVirtual = true,
                    expression = "price * 1.16",
                )
            )
        )

        assertFalse(sqlSlot.captured.contains("NULL"))
        assertFalse(sqlSlot.captured.contains("DEFAULT"))
    }

    @Test
    fun `invoke applies COMMENT to a generated column`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "orders",
            columns = listOf(
                ColumnDefinition(
                    name = "total_with_tax",
                    type = SqlColumnType.Decimal,
                    length = 10,
                    decimals = 2,
                    isVirtual = true,
                    expression = "price * 1.16",
                    comment = "Computed total including tax",
                )
            )
        )

        assertTrue(sqlSlot.captured.contains("COMMENT 'Computed total including tax'"))
    }
}
