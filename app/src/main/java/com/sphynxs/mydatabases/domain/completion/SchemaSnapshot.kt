package com.sphynxs.mydatabases.domain.completion

/**
 * Lightweight snapshot of database schema for SQL completion.
 *
 * Loaded once on editor mount (if databaseName != null), cached in ViewModel StateFlow.
 * Columns loaded lazily per-table on first access (not included in initial snapshot).
 *
 * Design: ADR 5 — Schema snapshot is lazy-loaded
 * Spec: scenario 19 (schema available), scenario 20 (schema unavailable)
 *
 * @property databaseName The active database name (null if no database context)
 * @property tables List of table names in the database (eager-loaded)
 * @property columns Map of table name → list of (column name, type) pairs (lazy-loaded on demand)
 *
 * @author israel-icm
 * @date 2026-06-24
 */
data class SchemaSnapshot(
    val databaseName: String,
    val tables: List<String>,
    val columns: Map<String, List<ColumnInfo>> = emptyMap()
)

/**
 * Column metadata for completion suggestions.
 *
 * @property name Column name (e.g., "id", "email")
 * @property type MySQL/MariaDB type (e.g., "INT", "VARCHAR(255)", "DATETIME")
 */
data class ColumnInfo(
    val name: String,
    val type: String
)
