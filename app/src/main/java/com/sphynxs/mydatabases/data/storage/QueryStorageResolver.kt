package com.sphynxs.mydatabases.data.storage

import com.sphynxs.mydatabases.domain.models.RootResolution
import com.sphynxs.mydatabases.domain.repositories.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Decides which storage root is active — app-private default, or the user-configured SAF tree —
 * and detects permission loss, falling back to the private root (change `query-files-storage`).
 *
 * Re-checked on EVERY call, never cached: a SAF grant can be lost mid-session (SD card removed,
 * grant revoked), so [resolveRoot] must re-verify each time, not just once at startup.
 *
 * @param settingsRepository Source of the persisted SAF tree Uri preference (`null` = private default)
 * @param provider Resolves the two candidate roots without touching `Context` directly
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
class QueryStorageResolver @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val provider: QueryStorageRootProvider
) {

    suspend fun resolveRoot(): RootResolution {
        val treeUri = settingsRepository.observeQueryStorageTreeUri().first()

        if (treeUri == null) {
            val privateRoot = requireNotNull(provider.privateRoot()) {
                "App-private external storage is unavailable"
            }
            return RootResolution.Resolved(privateRoot)
        }

        val safRoot = provider.safRoot(treeUri)
        return if (safRoot != null && safRoot.exists() && safRoot.canWrite()) {
            RootResolution.Resolved(safRoot)
        } else {
            val privateRoot = requireNotNull(provider.privateRoot()) {
                "App-private external storage is unavailable"
            }
            RootResolution.Fallback(
                root = privateRoot,
                reason = "SAF tree unavailable or not writable: $treeUri"
            )
        }
    }
}
