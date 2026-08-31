package com.sphynxs.mydatabases.domain.usecases.queryfiles

import android.net.Uri
import com.sphynxs.mydatabases.domain.repositories.QueryFileStore
import javax.inject.Inject

/**
 * Use case for deleting a `.sql` query file (change `query-files-storage`).
 *
 * @param store Storage abstraction for `.sql` query files
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
class DeleteQueryFileUseCase @Inject constructor(
    private val store: QueryFileStore
) {
    suspend operator fun invoke(uri: Uri): Result<Boolean> {
        return store.delete(uri)
    }
}
