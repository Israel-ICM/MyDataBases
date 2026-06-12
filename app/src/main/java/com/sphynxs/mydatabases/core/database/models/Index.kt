package com.sphynxs.mydatabases.core.database.models

/**
 * Representa un índice de una tabla.
 *
 * Soporta:
 * - Índices simples (1 columna)
 * - Índices compuestos (múltiples columnas)
 * - Unique vs non-unique
 * - Diferentes tipos (BTREE, HASH, FULLTEXT, SPATIAL)
 *
 * @property name Nombre del índice (ej: "idx_email", "PRIMARY")
 * @property columns Lista de columnas en el índice (orden importa para compuestos)
 * @property unique Si el índice es UNIQUE
 * @property type Tipo de índice (BTREE, HASH, FULLTEXT, SPATIAL)
 *
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
 *
 * @property BTREE B-Tree (por defecto en MySQL/MariaDB)
 * @property HASH Hash index (solo en MEMORY engine)
 * @property FULLTEXT Full-text search index
 * @property SPATIAL Spatial index (para geometrías)
 *
 * @author israel-icm
 * @date 2026-06-11
 */
enum class IndexType {
    BTREE,
    HASH,
    FULLTEXT,
    SPATIAL
}
