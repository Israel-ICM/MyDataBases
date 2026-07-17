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

    // --- ENUM/SET value-list types (change `create-table`, ENUM/SET support) ---

    @Test
    fun `invoke emits ENUM DDL with multiple values`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "users",
            columns = listOf(
                ColumnDefinition(
                    name = "status",
                    type = SqlColumnType.Enum,
                    values = listOf("active", "inactive", "pending"),
                    nullable = false,
                )
            )
        )

        assertEquals(
            "CREATE TABLE `users` (`status` ENUM('active','inactive','pending') NOT NULL)",
            sqlSlot.captured
        )
    }

    @Test
    fun `invoke emits SET DDL with multiple values`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "users",
            columns = listOf(
                ColumnDefinition(
                    name = "roles",
                    type = SqlColumnType.Set,
                    values = listOf("admin", "editor", "viewer"),
                    nullable = true,
                )
            )
        )

        assertEquals(
            "CREATE TABLE `users` (`roles` SET('admin','editor','viewer'))",
            sqlSlot.captured
        )
    }

    @Test
    fun `invoke escapes single quotes inside ENUM values`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "products",
            columns = listOf(
                ColumnDefinition(
                    name = "size",
                    type = SqlColumnType.Enum,
                    values = listOf("kid's", "adult"),
                    nullable = true,
                )
            )
        )

        assertTrue(sqlSlot.captured.contains("ENUM('kid''s','adult')"))
    }

    // --- Extended field attributes (change `create-table`, extended field attributes addendum) ---
    // Test-first per user instruction: written before/alongside the implementation, NOT executed
    // by sdd-apply (project HARD RULE — no ./gradlew runs).

    @Test
    fun `invoke emits UNSIGNED ZEROFILL for a zero-filled integer column`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "counters",
            columns = listOf(ColumnDefinition(name = "id", type = SqlColumnType.Int, nullable = false, zeroFill = true))
        )

        assertEquals("CREATE TABLE `counters` (`id` INT UNSIGNED ZEROFILL NOT NULL)", sqlSlot.captured)
    }

    @Test
    fun `invoke omits UNSIGNED ZEROFILL when zeroFill is false`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "counters",
            columns = listOf(ColumnDefinition(name = "id", type = SqlColumnType.Int, nullable = false, zeroFill = false))
        )

        assertFalse(sqlSlot.captured.contains("ZEROFILL"))
    }

    @Test
    fun `invoke emits CHARACTER SET clause when only charset is set`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "users",
            columns = listOf(
                ColumnDefinition(
                    name = "name",
                    type = SqlColumnType.VarChar,
                    length = 100,
                    nullable = false,
                    characterSet = "utf8mb4",
                )
            )
        )

        assertEquals(
            "CREATE TABLE `users` (`name` VARCHAR(100) CHARACTER SET utf8mb4 NOT NULL)",
            sqlSlot.captured
        )
    }

    @Test
    fun `invoke emits COLLATE clause when only collation is set`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "users",
            columns = listOf(
                ColumnDefinition(
                    name = "name",
                    type = SqlColumnType.VarChar,
                    length = 100,
                    nullable = false,
                    collation = "utf8mb4_general_ci",
                )
            )
        )

        assertEquals(
            "CREATE TABLE `users` (`name` VARCHAR(100) COLLATE utf8mb4_general_ci NOT NULL)",
            sqlSlot.captured
        )
    }

    @Test
    fun `invoke emits CHARACTER SET and COLLATE together when both are set`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "users",
            columns = listOf(
                ColumnDefinition(
                    name = "name",
                    type = SqlColumnType.VarChar,
                    length = 100,
                    nullable = false,
                    characterSet = "utf8mb4",
                    collation = "utf8mb4_general_ci",
                )
            )
        )

        assertEquals(
            "CREATE TABLE `users` (`name` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL)",
            sqlSlot.captured
        )
    }

    @Test
    fun `invoke ignores charset and collation for a non-charset type`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "counters",
            columns = listOf(
                ColumnDefinition(
                    name = "id",
                    type = SqlColumnType.Int,
                    nullable = false,
                    characterSet = "utf8mb4",
                    collation = "utf8mb4_general_ci",
                )
            )
        )

        assertFalse(sqlSlot.captured.contains("CHARACTER SET"))
        assertFalse(sqlSlot.captured.contains("COLLATE"))
    }

    @Test
    fun `invoke emits DEFAULT clause with a raw unquoted value`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "logs",
            columns = listOf(
                ColumnDefinition(
                    name = "created_at",
                    type = SqlColumnType.Timestamp,
                    nullable = false,
                    defaultValue = "CURRENT_TIMESTAMP",
                )
            )
        )

        assertEquals(
            "CREATE TABLE `logs` (`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)",
            sqlSlot.captured
        )
    }

    @Test
    fun `invoke omits DEFAULT clause when defaultValue is blank`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "logs",
            columns = listOf(
                ColumnDefinition(name = "created_at", type = SqlColumnType.Timestamp, nullable = false, defaultValue = "  ")
            )
        )

        assertFalse(sqlSlot.captured.contains("DEFAULT"))
    }

    @Test
    fun `invoke emits ON UPDATE CURRENT_TIMESTAMP when autoUpdateTimestamp is true`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "logs",
            columns = listOf(
                ColumnDefinition(name = "updated_at", type = SqlColumnType.DateTime, nullable = false, autoUpdateTimestamp = true)
            )
        )

        assertEquals(
            "CREATE TABLE `logs` (`updated_at` DATETIME NOT NULL ON UPDATE CURRENT_TIMESTAMP)",
            sqlSlot.captured
        )
    }

    @Test
    fun `invoke emits AUTO_INCREMENT when autoIncrement is true`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "users",
            columns = listOf(
                ColumnDefinition(
                    name = "id",
                    type = SqlColumnType.Int,
                    nullable = false,
                    isPrimaryKey = true,
                    autoIncrement = true,
                )
            )
        )

        assertEquals(
            "CREATE TABLE `users` (`id` INT NOT NULL AUTO_INCREMENT, PRIMARY KEY (`id`))",
            sqlSlot.captured
        )
    }

    @Test
    fun `invoke emits NOT NULL DEFAULT ON UPDATE and AUTO_INCREMENT in that exact order`() = runTest {
        val sqlSlot = slot<String>()
        coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

        useCase(
            name = "orders",
            columns = listOf(
                ColumnDefinition(
                    name = "id",
                    type = SqlColumnType.Int,
                    nullable = false,
                    defaultValue = "0",
                    autoUpdateTimestamp = true,
                    autoIncrement = true,
                )
            )
        )

        assertEquals(
            "CREATE TABLE `orders` (`id` INT NOT NULL DEFAULT 0 ON UPDATE CURRENT_TIMESTAMP AUTO_INCREMENT)",
            sqlSlot.captured
        )
    }

    @Test
    fun `invoke never emits DEFAULT ON UPDATE or AUTO_INCREMENT for a generated column even when set`() = runTest {
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
                    defaultValue = "0",
                    autoUpdateTimestamp = true,
                    autoIncrement = true,
                )
            )
        )

        assertFalse(sqlSlot.captured.contains("DEFAULT"))
        assertFalse(sqlSlot.captured.contains("ON UPDATE"))
        assertFalse(sqlSlot.captured.contains("AUTO_INCREMENT"))
    }

    @Test
    fun `invoke emits the full extended clause order UNSIGNED ZEROFILL, CHARACTER SET COLLATE, NOT NULL, DEFAULT, ON UPDATE, AUTO_INCREMENT, COMMENT`() =
        runTest {
            val sqlSlot = slot<String>()
            coEvery { repository.executeUpdate(capture(sqlSlot), any()) } returns Result.success(1)

            // NOTE: combining zeroFill with a charset-bearing type is not a realistic MySQL
            // column (ZEROFILL is numeric-only) — this test intentionally exercises
            // buildColumnClause's ordering logic in isolation, since applicability gating
            // (which type/state combos are reachable from the UI) lives in
            // ColumnDefinitionValidation/FieldDefinitionDialog, not in CreateTableUseCase.
            useCase(
                name = "orders",
                columns = listOf(
                    ColumnDefinition(
                        name = "code",
                        type = SqlColumnType.VarChar,
                        length = 10,
                        nullable = false,
                        zeroFill = true,
                        characterSet = "utf8mb4",
                        collation = "utf8mb4_general_ci",
                        defaultValue = "0",
                        autoUpdateTimestamp = true,
                        autoIncrement = true,
                        comment = "x",
                    )
                )
            )

            assertEquals(
                "CREATE TABLE `orders` (`code` VARCHAR(10) UNSIGNED ZEROFILL CHARACTER SET utf8mb4 " +
                    "COLLATE utf8mb4_general_ci NOT NULL DEFAULT 0 ON UPDATE CURRENT_TIMESTAMP " +
                    "AUTO_INCREMENT COMMENT 'x')",
                sqlSlot.captured
            )
        }
}
