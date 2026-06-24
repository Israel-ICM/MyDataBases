package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.domain.completion.ColumnInfo
import com.sphynxs.mydatabases.domain.completion.SchemaSnapshot
import javax.inject.Inject

/**
 * Load database schema snapshot for SQL completion.
 *
 * Wraps existing GetTablesUseCase + GetColumnsUseCase to create a lightweight
 * snapshot suitable for completion provider.
 *
 * Initial version: loads tables eagerly, columns map empty (lazy on-demand later).
 *
 * Design: ADR 5 — Schema snapshot is lazy-loaded
 * Spec: scenario 19 (valid DB), scenario 20 (invalid DB graceful degradation)
 *
 * @param getTablesUseCase Existing use case to fetch table names
 * @param getColumnsUseCase Existing use case to fetch column metadata
 *
 * @author israel-icm
 * @date 2026-06-24
 */
class LoadSchemaSnapshotUseCase @Inject constructor(
    private val getTablesUseCase: GetTablesUseCase,
    private val getColumnsUseCase: GetColumnsUseCase
) {
    /**
     * Load schema snapshot for a database.
     *
     * @param databaseName The database name to load schema for
     * @return Result<SchemaSnapshot> — Success with snapshot, or Failure if DB invalid/unreachable
     */
    suspend operator fun invoke(databaseName: String): Result<SchemaSnapshot> {
        return try {
            // Fetch tables (eager)
            val tablesResult = getTablesUseCase(databaseName)
            
            if (tablesResult.isFailure) {
                return Result.failure(
                    tablesResult.exceptionOrNull() 
                        ?: Exception("Failed to load tables for database: $databaseName")
                )
            }
            
            val tables = tablesResult.getOrNull() ?: emptyList()
            
            // For now, columns map empty (lazy loading in future PR)
            // In PR #3, we'll load columns on-demand per table
            val snapshot = SchemaSnapshot(
                databaseName = databaseName,
                tables = tables.map { it.name },
                columns = emptyMap() // TODO: lazy-load columns per table on first access
            )
            
            Result.success(snapshot)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
