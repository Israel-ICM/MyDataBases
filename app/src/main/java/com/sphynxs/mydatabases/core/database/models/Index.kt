package com.sphynxs.mydatabases.core.database.models

/**
 * Representa un índice en una tabla.
 *
 * @property name Nombre del índice
 * @property columns Lista de columnas que forman el índice (en orden)
 * @property unique Si es un índice único
 * @property type Tipo de índice (BTREE, HASH, FULLTEXT, SPATIAL)
 * @author israel-icm
 * @date 2026-06-11
 */
data class Index(
    val name: String,
    val columns: List<String>,
    val unique: Boolean,
    val type: IndexType
)

/**
 * Tipo de índice.
 */
enum class IndexType {
    /** B-Tree (default) */
    BTREE,

    /** Hash */
    HASH,

    /** Full-text search */
    FULLTEXT,

    /** Spatial (geometría) */
    SPATIAL
}
