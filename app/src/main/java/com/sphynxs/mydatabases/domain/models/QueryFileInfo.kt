package com.sphynxs.mydatabases.domain.models

import android.net.Uri

/**
 * Metadata for a single `.sql` file listed from an [AppFolder.Queries] location (change
 * `query-files-storage`). No database-backed tracking — this is a direct projection of what
 * [com.sphynxs.mydatabases.domain.repositories.QueryFileStore.list] found on disk/SAF.
 *
 * @property name File display name (including `.sql` extension)
 * @property uri Content or file `Uri`, directly usable with `ContentResolver.openInputStream`
 * @property lastModified Epoch millis, used for most-recent-first sorting
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
data class QueryFileInfo(
    val name: String,
    val uri: Uri,
    val lastModified: Long
)
