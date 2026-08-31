package com.sphynxs.mydatabases.domain.repositories

import android.net.Uri
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.domain.models.QueryFileInfo

/**
 * Storage abstraction for `.sql` query files, scoped per [DatabaseType] (change
 * `query-files-storage`).
 *
 * Every path-resolving operation takes [DatabaseType] explicitly from day one — no default engine
 * parameter anywhere — so future [com.sphynxs.mydatabases.domain.models.AppFolder] variants never
 * force a breaking change to this contract. A single implementation resolves either an app-private
 * or a user-chosen SAF-tree root transparently; callers never know or care which is active.
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
interface QueryFileStore {

    /**
     * Lists all `.sql` files currently in [engineType]'s queries folder, most-recent-first is
     * NOT guaranteed here — sorting is a presentation concern, applied by the caller (ViewModel).
     */
    suspend fun list(engineType: DatabaseType): Result<List<QueryFileInfo>>

    /** Reads the full text content of the file at [uri]. */
    suspend fun read(uri: Uri): Result<String>

    /**
     * Writes [content] as a new (or overwritten) file named [fileName] under [engineType]'s
     * queries folder, creating the folder lazily if it doesn't exist yet.
     *
     * @return The resulting file's [Uri] on success.
     */
    suspend fun write(engineType: DatabaseType, fileName: String, content: String): Result<Uri>

    /** Deletes the file at [uri]. Returns `true` if a file was actually deleted. */
    suspend fun delete(uri: Uri): Result<Boolean>
}
