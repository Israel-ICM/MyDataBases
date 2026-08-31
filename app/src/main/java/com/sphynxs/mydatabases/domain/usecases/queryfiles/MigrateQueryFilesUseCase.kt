package com.sphynxs.mydatabases.domain.usecases.queryfiles

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.domain.models.AppFolder
import javax.inject.Inject

/** Outcome of a [MigrateQueryFilesUseCase] run. */
data class MigrationResult(
    val hadFilesToMigrate: Boolean,
    val filesCopied: Int,
    val filesFailed: Int
)

/**
 * Copies existing `.sql` query files from [oldRoot] into wherever [writeContent] targets — never
 * deletes originals (change `query-files-storage`).
 *
 * A pure [DocumentFile] tree walker: it never touches [QueryFileStore], which only ever resolves
 * ONE current root per call by design, and never touches `Context`/`ContentResolver` directly —
 * the caller supplies [readContent]/[writeContent], typically backed by `QueryFileStore.read`/
 * `.write` called AFTER the storage preference has already been switched, so `writeContent`
 * naturally lands in the new location without this use case needing to know about it explicitly.
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
class MigrateQueryFilesUseCase @Inject constructor() {

    suspend operator fun invoke(
        oldRoot: DocumentFile,
        readContent: suspend (Uri) -> String?,
        writeContent: suspend (engineType: DatabaseType, fileName: String, content: String) -> Boolean
    ): MigrationResult {
        var hadFiles = false
        var copied = 0
        var failed = 0

        for (engine in DatabaseType.entries) {
            val segment = AppFolder.segmentFor(engine)
            val queriesFolder = oldRoot.findFile(segment)?.findFile("queries") ?: continue

            val files = queriesFolder.listFiles()
                .filter { it.isFile && it.name.orEmpty().endsWith(".sql", ignoreCase = true) }

            for (file in files) {
                hadFiles = true
                val content = readContent(file.uri)
                if (content == null) {
                    failed++
                    continue
                }
                val ok = writeContent(engine, file.name.orEmpty(), content)
                if (ok) copied++ else failed++
            }
        }

        return MigrationResult(hadFilesToMigrate = hadFiles, filesCopied = copied, filesFailed = failed)
    }
}
