package com.sphynxs.mydatabases.domain.usecases.queryfiles

import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.domain.models.QueryFileInfo
import com.sphynxs.mydatabases.domain.repositories.QueryFileStore
import javax.inject.Inject

/**
 * Use case for listing `.sql` files in [engineType]'s managed queries folder (change
 * `query-files-storage`).
 *
 * @param store Storage abstraction for `.sql` query files
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
class ListQueryFilesUseCase @Inject constructor(
    private val store: QueryFileStore
) {
    suspend operator fun invoke(engineType: DatabaseType): Result<List<QueryFileInfo>> {
        return store.list(engineType)
    }
}
