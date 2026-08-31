package com.sphynxs.mydatabases.domain.usecases.queryfiles

import android.net.Uri
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.domain.repositories.QueryFileStore
import javax.inject.Inject

/**
 * Use case for saving a `.sql` query file into [engineType]'s managed queries folder (change
 * `query-files-storage`).
 *
 * @param store Storage abstraction for `.sql` query files
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
class SaveQueryFileUseCase @Inject constructor(
    private val store: QueryFileStore
) {
    suspend operator fun invoke(engineType: DatabaseType, fileName: String, content: String): Result<Uri> {
        return store.write(engineType, fileName, content)
    }
}
