package com.sphynxs.mydatabases.domain.models

import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * TDD tests for `AppFolder` (change `query-files-storage`, Phase 1).
 *
 * Covers the engine-scoped path resolution rule: `segmentFor` must be lowercase (never the raw
 * enum name or `displayName`), and `resolve` must always produce `MyDataBase/{segment}/queries/`.
 * Pure JVM — zero Android/`DocumentFile` dependency.
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
class AppFolderTest {

    @Test
    fun `segmentFor MYSQL is lowercase mysql`() {
        assertEquals("mysql", AppFolder.segmentFor(DatabaseType.MYSQL))
    }

    @Test
    fun `segmentFor MARIADB is lowercase mariadb`() {
        assertEquals("mariadb", AppFolder.segmentFor(DatabaseType.MARIADB))
    }

    @Test
    fun `segmentFor POSTGRESQL is lowercase postgresql`() {
        assertEquals("postgresql", AppFolder.segmentFor(DatabaseType.POSTGRESQL))
    }

    @Test
    fun `segmentFor SQLITE is lowercase sqlite`() {
        assertEquals("sqlite", AppFolder.segmentFor(DatabaseType.SQLITE))
    }

    @Test
    fun `segmentFor is never the raw enum name or displayName for a mixed-case engine`() {
        // MariaDB's displayName is "MariaDB" (mixed case) — guards against accidentally
        // using displayName instead of the lowercase name-based segment.
        val segment = AppFolder.segmentFor(DatabaseType.MARIADB)
        assertEquals(false, segment == DatabaseType.MARIADB.displayName)
        assertEquals(false, segment == DatabaseType.MARIADB.name)
    }

    @Test
    fun `resolve Queries for MYSQL ends with MyDataBase mysql queries`() {
        val path = AppFolder.resolve(DatabaseType.MYSQL, AppFolder.Queries)
        assertEquals("MyDataBase/mysql/queries/", path)
    }

    @Test
    fun `resolve Queries for MARIADB ends with MyDataBase mariadb queries`() {
        val path = AppFolder.resolve(DatabaseType.MARIADB, AppFolder.Queries)
        assertEquals("MyDataBase/mariadb/queries/", path)
    }

    @Test
    fun `resolve Queries for POSTGRESQL ends with MyDataBase postgresql queries`() {
        val path = AppFolder.resolve(DatabaseType.POSTGRESQL, AppFolder.Queries)
        assertEquals("MyDataBase/postgresql/queries/", path)
    }

    @Test
    fun `resolve Queries for SQLITE ends with MyDataBase sqlite queries`() {
        val path = AppFolder.resolve(DatabaseType.SQLITE, AppFolder.Queries)
        assertEquals("MyDataBase/sqlite/queries/", path)
    }
}
