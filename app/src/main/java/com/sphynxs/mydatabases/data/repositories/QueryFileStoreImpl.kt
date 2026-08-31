package com.sphynxs.mydatabases.data.repositories

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.data.storage.QueryStorageResolver
import com.sphynxs.mydatabases.domain.models.QueryFileInfo
import com.sphynxs.mydatabases.domain.repositories.QueryFileStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single [QueryFileStore] implementation over whichever root [QueryStorageResolver] resolves —
 * app-private or SAF tree — so callers never need to know or care which is active (change
 * `query-files-storage`).
 *
 * Engine partitioning (`{engineType}/queries/`) is resolved lazily: [list] never creates missing
 * subfolders (an absent folder just means an empty list), [write] creates them on first use.
 *
 * @param resolver Decides the active storage root, with permission-loss fallback
 * @param context Only used for `ContentResolver` byte I/O — all path navigation goes through
 *   [DocumentFile] methods, which don't need `Context` once a root is already resolved
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
@Singleton
class QueryFileStoreImpl @Inject constructor(
    private val resolver: QueryStorageResolver,
    @ApplicationContext private val context: Context
) : QueryFileStore {

    override suspend fun list(engineType: DatabaseType): Result<List<QueryFileInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val root = resolver.resolveRoot().root
                val queriesFolder = engineQueriesFolder(root, engineType, createIfMissing = false)
                    ?: return@runCatching emptyList()

                queriesFolder.listFiles()
                    .filter { it.isFile && it.name.orEmpty().endsWith(".sql", ignoreCase = true) }
                    .map { QueryFileInfo(name = it.name.orEmpty(), uri = it.uri, lastModified = it.lastModified()) }
            }
        }

    override suspend fun read(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalStateException("Cannot open input stream for $uri")
        }
    }

    override suspend fun write(engineType: DatabaseType, fileName: String, content: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            runCatching {
                val root = resolver.resolveRoot().root
                val queriesFolder = requireNotNull(engineQueriesFolder(root, engineType, createIfMissing = true)) {
                    "Cannot create the $engineType queries folder"
                }

                val target = queriesFolder.findFile(fileName)
                    ?: queriesFolder.createFile("application/sql", fileName)
                    ?: throw IllegalStateException("Cannot create file '$fileName'")

                context.contentResolver.openOutputStream(target.uri, "wt")?.use { out ->
                    out.write(content.toByteArray())
                } ?: throw IllegalStateException("Cannot open output stream for ${target.uri}")

                target.uri
            }
        }

    override suspend fun delete(uri: Uri): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            DocumentFile.fromSingleUri(context, uri)?.delete() ?: false
        }
    }

    /**
     * Resolves `{root}/{engineSegment}/queries/`, creating the two path segments lazily when
     * [createIfMissing] is true. Returns `null` (never creates) when [createIfMissing] is false
     * and either segment doesn't exist yet — an absent folder means an empty list, not an error.
     */
    private fun engineQueriesFolder(
        root: DocumentFile,
        engineType: DatabaseType,
        createIfMissing: Boolean
    ): DocumentFile? {
        val segment = com.sphynxs.mydatabases.domain.models.AppFolder.segmentFor(engineType)

        val engineFolder = root.findFile(segment)
            ?: if (createIfMissing) root.createDirectory(segment) else null
        engineFolder ?: return null

        return engineFolder.findFile("queries")
            ?: if (createIfMissing) engineFolder.createDirectory("queries") else null
    }
}
