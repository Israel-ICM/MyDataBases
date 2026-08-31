package com.sphynxs.mydatabases.data.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Resolves the two candidate [DocumentFile] storage roots — app-private and SAF-tree — behind a
 * mockable interface, so [QueryStorageResolver] never touches `Context`/static `DocumentFile`
 * factories directly (change `query-files-storage`).
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
interface QueryStorageRootProvider {

    /** App-private external storage root (`getExternalFilesDir`-based), wiped on uninstall. */
    fun privateRoot(): DocumentFile?

    /** SAF tree root for a previously granted [treeUri], or `null` if it can't be resolved. */
    fun safRoot(treeUri: Uri): DocumentFile?
}

class DefaultQueryStorageRootProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : QueryStorageRootProvider {

    override fun privateRoot(): DocumentFile? {
        val dir = context.getExternalFilesDir(null) ?: return null
        return DocumentFile.fromFile(dir)
    }

    override fun safRoot(treeUri: Uri): DocumentFile? {
        return DocumentFile.fromTreeUri(context, treeUri)
    }
}
