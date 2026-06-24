package com.sphynxs.mydatabases.domain.usecases

import com.google.common.truth.Truth.assertThat
import com.sphynxs.mydatabases.core.database.models.Table
import com.sphynxs.mydatabases.core.database.models.TableType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LoadSchemaSnapshotUseCase.
 *
 * TDD: RED → GREEN → REFACTOR
 * Spec: openspec/changes/editor-completion-and-format/spec.md (scenarios 19-20)
 *
 * @author israel-icm
 * @date 2026-06-24
 */
class LoadSchemaSnapshotUseCaseTest {

    private lateinit var getTablesUseCase: GetTablesUseCase
    private lateinit var getColumnsUseCase: GetColumnsUseCase
    private lateinit var useCase: LoadSchemaSnapshotUseCase

    @Before
    fun setup() {
        getTablesUseCase = mockk()
        getColumnsUseCase = mockk()
        useCase = LoadSchemaSnapshotUseCase(getTablesUseCase, getColumnsUseCase)
    }

    /**
     * Scenario 19a: Load schema for valid database
     * GIVEN a valid database name
     * WHEN LoadSchemaSnapshotUseCase is invoked
     * THEN snapshot contains table names
     */
    @Test
    fun invoke_validDatabase_returnsSnapshotWithTables() = runTest {
        // GIVEN
        val databaseName = "test_db"
        val tables = listOf(
            Table(name = "users", database = databaseName, type = TableType.TABLE, rowCount = 100),
            Table(name = "orders", database = databaseName, type = TableType.TABLE, rowCount = 50)
        )
        coEvery { getTablesUseCase(databaseName) } returns Result.success(tables)

        // WHEN
        val result = useCase(databaseName)

        // THEN
        assertThat(result.isSuccess).isTrue()
        val snapshot = result.getOrNull()
        assertThat(snapshot).isNotNull()
        assertThat(snapshot?.databaseName).isEqualTo(databaseName)
        assertThat(snapshot?.tables).containsExactly("users", "orders")
        assertThat(snapshot?.columns).isEmpty() // Lazy-loaded later
    }

    /**
     * Scenario 19b: Load schema for empty database
     * GIVEN a database with no tables
     * WHEN LoadSchemaSnapshotUseCase is invoked
     * THEN snapshot contains empty table list
     */
    @Test
    fun invoke_emptyDatabase_returnsSnapshotWithEmptyTables() = runTest {
        // GIVEN
        val databaseName = "empty_db"
        coEvery { getTablesUseCase(databaseName) } returns Result.success(emptyList())

        // WHEN
        val result = useCase(databaseName)

        // THEN
        assertThat(result.isSuccess).isTrue()
        val snapshot = result.getOrNull()
        assertThat(snapshot).isNotNull()
        assertThat(snapshot?.tables).isEmpty()
    }

    /**
     * Scenario 20: Load schema for invalid database
     * GIVEN an invalid database name
     * WHEN LoadSchemaSnapshotUseCase is invoked
     * THEN Result.failure is returned
     */
    @Test
    fun invoke_invalidDatabase_returnsFailure() = runTest {
        // GIVEN
        val databaseName = "nonexistent_db"
        val error = Exception("Database not found: $databaseName")
        coEvery { getTablesUseCase(databaseName) } returns Result.failure(error)

        // WHEN
        val result = useCase(databaseName)

        // THEN
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isNotNull()
    }
}
