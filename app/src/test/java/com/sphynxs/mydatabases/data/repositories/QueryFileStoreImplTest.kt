package com.sphynxs.mydatabases.data.repositories

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.data.storage.QueryStorageResolver
import com.sphynxs.mydatabases.domain.models.RootResolution
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * TDD tests for `QueryFileStoreImpl` (change `query-files-storage`, Phase 4).
 *
 * `QueryStorageResolver` and the `DocumentFile` tree are mocked — this covers the
 * folder-navigation/creation logic and the `.sql` filter/per-engine isolation, which is what's
 * genuinely unit-testable without real `Context`/`ContentResolver` I/O (that part is covered by
 * the populated-but-not-executed instrumented test, Phase 4.7 — no device available).
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
class QueryFileStoreImplTest {

    private lateinit var resolver: QueryStorageResolver
    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var store: QueryFileStoreImpl
    private lateinit var root: DocumentFile

    @Before
    fun setup() {
        resolver = mockk()
        context = mockk()
        contentResolver = mockk()
        every { context.contentResolver } returns contentResolver
        root = mockk(relaxed = true)
        store = QueryFileStoreImpl(resolver, context)
    }

    private fun sqlFile(name: String, lastModified: Long = 0L): DocumentFile {
        val uri = mockk<Uri>(relaxed = true)
        return mockk(relaxed = true) {
            every { isFile } returns true
            every { this@mockk.name } returns name
            every { this@mockk.uri } returns uri
            every { lastModified() } returns lastModified
        }
    }

    // --- list() ---

    @Test
    fun `list filters to sql files only, case-insensitive`() = runTest {
        val queriesFolder = mockk<DocumentFile>(relaxed = true) {
            every { listFiles() } returns arrayOf(
                sqlFile("a.sql"),
                sqlFile("B.SQL"),
                sqlFile("notes.txt"),
                sqlFile("readme")
            )
        }
        val engineFolder = mockk<DocumentFile>(relaxed = true) {
            every { findFile("queries") } returns queriesFolder
        }
        every { root.findFile("mysql") } returns engineFolder
        coEveryResolveRoot(RootResolution.Resolved(root))

        val result = store.list(DatabaseType.MYSQL)

        assertTrue(result.isSuccess)
        val names = result.getOrNull()!!.map { it.name }
        assertEquals(listOf("a.sql", "B.SQL"), names)
    }

    @Test
    fun `list returns empty when the engine subfolder does not exist yet`() = runTest {
        every { root.findFile("postgresql") } returns null
        coEveryResolveRoot(RootResolution.Resolved(root))

        val result = store.list(DatabaseType.POSTGRESQL)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun `list under a Fallback resolution still returns the fallback folder contents, not an error`() = runTest {
        val queriesFolder = mockk<DocumentFile>(relaxed = true) {
            every { listFiles() } returns arrayOf(sqlFile("x.sql"))
        }
        val engineFolder = mockk<DocumentFile>(relaxed = true) {
            every { findFile("queries") } returns queriesFolder
        }
        every { root.findFile("sqlite") } returns engineFolder
        coEveryResolveRoot(RootResolution.Fallback(root, "SAF tree unavailable"))

        val result = store.list(DatabaseType.SQLITE)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()!!.size)
    }

    @Test
    fun `per-engine isolation - listing one engine never returns another engine's files`() = runTest {
        val mysqlQueries = mockk<DocumentFile>(relaxed = true) {
            every { listFiles() } returns arrayOf(sqlFile("mysql_only.sql"))
        }
        val mysqlEngine = mockk<DocumentFile>(relaxed = true) {
            every { findFile("queries") } returns mysqlQueries
        }
        every { root.findFile("mysql") } returns mysqlEngine
        every { root.findFile("postgresql") } returns null
        coEveryResolveRoot(RootResolution.Resolved(root))

        val mysqlResult = store.list(DatabaseType.MYSQL)
        val postgresResult = store.list(DatabaseType.POSTGRESQL)

        assertEquals(listOf("mysql_only.sql"), mysqlResult.getOrNull()!!.map { it.name })
        assertTrue(postgresResult.getOrNull()!!.isEmpty())
    }

    // --- write() ---

    @Test
    fun `write creates the engine and queries subfolders lazily when missing`() = runTest {
        val newFile = mockk<DocumentFile>(relaxed = true) {
            every { uri } returns mockk(relaxed = true)
        }
        val queriesFolder = mockk<DocumentFile>(relaxed = true) {
            every { findFile("new_query.sql") } returns null
            every { createFile(any(), "new_query.sql") } returns newFile
        }
        val engineFolder = mockk<DocumentFile>(relaxed = true) {
            every { findFile("queries") } returns null
            every { createDirectory("queries") } returns queriesFolder
        }
        every { root.findFile("mariadb") } returns null
        every { root.createDirectory("mariadb") } returns engineFolder
        coEveryResolveRoot(RootResolution.Resolved(root))
        every { contentResolver.openOutputStream(any(), any()) } returns ByteArrayOutputStream()

        val result = store.write(DatabaseType.MARIADB, "new_query.sql", "SELECT 1;")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `write overwrites an existing file with the same name instead of duplicating`() = runTest {
        val existingFile = mockk<DocumentFile>(relaxed = true) {
            every { uri } returns mockk(relaxed = true)
        }
        val queriesFolder = mockk<DocumentFile>(relaxed = true) {
            every { findFile("existing.sql") } returns existingFile
        }
        val engineFolder = mockk<DocumentFile>(relaxed = true) {
            every { findFile("queries") } returns queriesFolder
        }
        every { root.findFile("mysql") } returns engineFolder
        coEveryResolveRoot(RootResolution.Resolved(root))
        every { contentResolver.openOutputStream(any(), any()) } returns ByteArrayOutputStream()

        val result = store.write(DatabaseType.MYSQL, "existing.sql", "SELECT 2;")

        assertTrue(result.isSuccess)
        assertEquals(existingFile.uri, result.getOrNull())
    }

    private fun coEveryResolveRoot(resolution: RootResolution) {
        io.mockk.coEvery { resolver.resolveRoot() } returns resolution
    }
}
