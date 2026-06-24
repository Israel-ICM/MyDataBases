package com.sphynxs.mydatabases.domain.completion

/**
 * A single code completion suggestion for SQL editor.
 *
 * Displayed in completion popup with text + optional metadata (type for columns).
 *
 * Design: openspec/changes/editor-completion-and-format/design.md
 * Spec: scenarios 13-28
 *
 * @property text The text to insert when accepted (e.g., "SELECT", "users", "id")
 * @property kind Classification: KEYWORD, TABLE, COLUMN
 * @property detail Optional metadata (e.g., "INT" for column type, null for keywords/tables)
 * @property score Ranking score for sorting (higher = better match)
 *
 * @author israel-icm
 * @date 2026-06-24
 */
data class CompletionSuggestion(
    val text: String,
    val kind: CompletionKind,
    val detail: String? = null,
    val score: Double = 0.0
)

/**
 * Classification of completion suggestion.
 *
 * Used for context-aware ranking and icon display in popup.
 */
enum class CompletionKind {
    KEYWORD,  // SQL reserved words (SELECT, FROM, WHERE, etc.)
    TABLE,    // Database table names
    COLUMN    // Table column names (with optional type in detail)
}
