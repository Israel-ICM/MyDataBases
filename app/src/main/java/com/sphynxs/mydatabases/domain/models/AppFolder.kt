package com.sphynxs.mydatabases.domain.models

import com.sphynxs.mydatabases.core.database.engine.DatabaseType

/**
 * App-managed storage folders, scoped per [DatabaseType] (change `query-files-storage`).
 *
 * Path layout: `MyDataBase/{engineSegment}/{folder}/` — the engine segment sits above the leaf
 * folder so future [AppFolder] variants (only [Queries] exists today) reuse the same per-engine
 * partitioning without any breaking change to this contract.
 *
 * Pure domain logic: zero Android/`Context`/`DocumentFile` dependency, fully JVM-unit-testable.
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
sealed class AppFolder(private val leafName: String) {

    /** The `.sql` query files folder. */
    data object Queries : AppFolder("queries")

    companion object {
        private const val ROOT = "MyDataBase"

        /**
         * Lowercase, filesystem-friendly path segment for [engine] — e.g. `MYSQL` -> `"mysql"`.
         *
         * Deliberately NOT [DatabaseType.displayName] (mixed case, e.g. `"MariaDB"`) — the
         * segment must be a stable, lowercase, filesystem-safe identifier.
         */
        fun segmentFor(engine: DatabaseType): String = engine.name.lowercase()

        /**
         * Resolves the relative path for [folder] scoped to [engine], e.g.
         * `resolve(MYSQL, Queries) == "MyDataBase/mysql/queries/"`.
         */
        fun resolve(engine: DatabaseType, folder: AppFolder): String {
            return "$ROOT/${segmentFor(engine)}/${folder.leafName}/"
        }
    }
}
