package com.sphynxs.mydatabases.domain.usecases.queryfiles

import androidx.documentfile.provider.DocumentFile
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TDD tests for `MigrateQueryFilesUseCase` (change `query-files-storage`, Phase 7).
 *
 * Walks the [DocumentFile] tree of an explicit "old" root directly (rather than through
 * [QueryFileStore], which only ever resolves ONE current root per call by design — see PR-2) and
 * delegates the actual byte I/O to caller-supplied `readContent`/`writeContent` functions. This
 * keeps the use case itself a pure, dependency-free tree walker: the caller (Settings ViewModel,
 * Phase 8) decides what "write" means — typically `QueryFileStore.write(...)` called AFTER the
 * storage preference has already been switched to the new location, so `writeContent` naturally
 * targets the new root without this use case needing to know about it directly.
 *
 * Scans all 4 [DatabaseType] engine subfolders, copies via read-then-write, never deletes
 * originals.
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
class MigrateQueryFilesUseCaseTest {

    private val useCase = MigrateQueryFilesUseCase()

    private fun folder(vararg children: Pair<String, DocumentFile>): DocumentFile {
        val childMap = children.toMap()
        return mockk(relaxed = true) {
            every { findFile(any()) } answers { childMap[firstArg()] }
            every { createDirectory(any()) } answers { childMap[firstArg()] }
        }
    }

    private fun sqlFile(name: String, content: String, contentByUri: MutableMap<Any, String>): DocumentFile {
        val uri = mockk<android.net.Uri>(relaxed = true)
        contentByUri[uri] = content
        return mockk(relaxed = true) {
            every { isFile } returns true
            every { this@mockk.name } returns name
            every { this@mockk.uri } returns uri
        }
    }

    @Test
    fun `no files in any engine folder returns hadFilesToMigrate false`() = runTest {
        val emptyRoot = mockk<DocumentFile>(relaxed = true) {
            every { findFile(any()) } returns null
        }

        val result = useCase(oldRoot = emptyRoot, readContent = { "" }, writeContent = { _, _, _ -> true })

        assertFalse(result.hadFilesToMigrate)
        assertEquals(0, result.filesCopied)
        assertEquals(0, result.filesFailed)
    }

    @Test
    fun `copies each found file via read then write, never touching the original`() = runTest {
        val contentByUri = mutableMapOf<Any, String>()
        val mysqlFile = sqlFile("a.sql", "SELECT 1;", contentByUri)
        val mysqlQueries = folder("a.sql" to mysqlFile).also {
            every { it.listFiles() } returns arrayOf(mysqlFile)
        }
        val mysqlEngine = folder("queries" to mysqlQueries)
        val oldRoot = folder("mysql" to mysqlEngine)

        var writtenName: String? = null
        var writtenContent: String? = null
        val result = useCase(
            oldRoot = oldRoot,
            readContent = { uri -> contentByUri[uri] },
            writeContent = { _, name, content -> writtenName = name; writtenContent = content; true }
        )

        assertTrue(result.hadFilesToMigrate)
        assertEquals(1, result.filesCopied)
        assertEquals(0, result.filesFailed)
        assertEquals("a.sql", writtenName)
        assertEquals("SELECT 1;", writtenContent)
    }

    @Test
    fun `a failed write is counted as failed without aborting remaining files`() = runTest {
        val contentByUri = mutableMapOf<Any, String>()
        val file1 = sqlFile("one.sql", "SELECT 1;", contentByUri)
        val file2 = sqlFile("two.sql", "SELECT 2;", contentByUri)
        val queries = folder().also { every { it.listFiles() } returns arrayOf(file1, file2) }
        val engine = folder("queries" to queries)
        val oldRoot = folder("sqlite" to engine)

        val result = useCase(
            oldRoot = oldRoot,
            readContent = { uri -> contentByUri[uri] },
            writeContent = { _, name, _ -> name != "one.sql" } // "one.sql" fails, "two.sql" succeeds
        )

        assertEquals(1, result.filesCopied)
        assertEquals(1, result.filesFailed)
    }
}
