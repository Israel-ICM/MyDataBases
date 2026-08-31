package com.sphynxs.mydatabases.domain.models

import androidx.documentfile.provider.DocumentFile

/**
 * Outcome of resolving the active storage root for [AppFolder]-scoped files (change
 * `query-files-storage`). Distinguishes a normal resolution from a permission-loss fallback so
 * callers can surface the fallback notice — shown on EVERY resolve where the condition is
 * detected, never suppressed after the first showing (confirmed decision).
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
sealed class RootResolution {

    /** The resolved storage root — always present, regardless of which variant this is. */
    abstract val root: DocumentFile

    /** Normal resolution — either the app-private default or a valid, granted SAF tree. */
    data class Resolved(override val root: DocumentFile) : RootResolution()

    /**
     * The configured SAF tree could not be used (permission lost, tree removed, grant revoked) —
     * [root] is the app-private default used as a fallback. [reason] is a short, non-localized
     * diagnostic string (the UI layer maps this to a localized notice, not this raw text).
     */
    data class Fallback(override val root: DocumentFile, val reason: String) : RootResolution()
}
