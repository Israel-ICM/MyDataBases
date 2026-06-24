package com.sphynxs.mydatabases.domain.completion

import com.sphynxs.mydatabases.domain.editor.SqlKeywords

/**
 * Pure SQL completion provider.
 *
 * Generates context-aware suggestions from keywords + schema snapshot.
 * Pure function, 100% JVM-testable (no Android dependencies).
 *
 * Design: ADR 3 — Completion provider is pure function
 * Spec: openspec/changes/editor-completion-and-format/spec.md (scenarios 13-28)
 *
 * Context-aware ranking:
 * - After FROM/JOIN/UPDATE/INTO → rank TABLE first
 * - After SELECT/WHERE/ON/, → rank COLUMN first
 * - Otherwise → KEYWORD first
 *
 * @author israel-icm
 * @date 2026-06-24
 */
object SqlCompletionProvider {

    /**
     * Generate completion suggestions for a given prefix and context.
     *
     * @param prefix The text prefix to match (e.g., "SEL", "use", "id")
     * @param context The SQL text before the cursor (last 50 chars for context detection)
     * @param schema Optional schema snapshot (null = keywords only)
     * @return List of ranked suggestions (highest score first)
     */
    fun getSuggestions(
        prefix: String,
        context: String = "",
        schema: SchemaSnapshot? = null
    ): List<CompletionSuggestion> {
        if (prefix.isBlank()) return emptyList()

        val suggestions = mutableListOf<CompletionSuggestion>()
        val normalizedPrefix = prefix.lowercase()
        val contextUpper = context.uppercase()

        // Determine context bias
        val preferTables = contextUpper.matches(Regex(".*\\b(FROM|JOIN|UPDATE|INTO)\\s*$"))
        val preferColumns = contextUpper.matches(Regex(".*\\b(SELECT|WHERE|ON|,)\\s*$"))

        // 1. Add keyword suggestions
        SqlKeywords.KEYWORDS.forEach { keyword ->
            if (keyword.lowercase().startsWith(normalizedPrefix)) {
                val score = when {
                    !preferTables && !preferColumns -> 100.0 // Default: keywords first
                    preferTables || preferColumns -> 50.0   // Lower when context prefers schema
                    else -> 100.0
                }
                suggestions.add(
                    CompletionSuggestion(
                        text = keyword,
                        kind = CompletionKind.KEYWORD,
                        detail = null,
                        score = score
                    )
                )
            }
        }

        // 2. Add table suggestions (if schema available)
        schema?.tables?.forEach { tableName ->
            if (tableName.lowercase().startsWith(normalizedPrefix)) {
                val score = when {
                    preferTables -> 100.0  // Prefer tables after FROM/JOIN
                    preferColumns -> 30.0  // Lower when columns preferred
                    else -> 70.0           // Default middle
                }
                suggestions.add(
                    CompletionSuggestion(
                        text = tableName,
                        kind = CompletionKind.TABLE,
                        detail = null,
                        score = score
                    )
                )
            }
        }

        // 3. Add column suggestions (if schema available)
        schema?.columns?.values?.flatten()?.forEach { columnInfo ->
            if (columnInfo.name.lowercase().startsWith(normalizedPrefix)) {
                val score = when {
                    preferColumns -> 100.0  // Prefer columns after SELECT/WHERE
                    preferTables -> 30.0    // Lower when tables preferred
                    else -> 60.0            // Default lower than keywords
                }
                suggestions.add(
                    CompletionSuggestion(
                        text = columnInfo.name,
                        kind = CompletionKind.COLUMN,
                        detail = columnInfo.type, // Show type (e.g., "INT", "VARCHAR")
                        score = score
                    )
                )
            }
        }

        // Sort by score descending, then alphabetically
        return suggestions
            .sortedWith(compareByDescending<CompletionSuggestion> { it.score }.thenBy { it.text })
            .take(20) // Limit to top 20 suggestions
    }
}
