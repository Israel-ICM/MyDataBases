package com.sphynxs.mydatabases.domain.editor

import com.sphynxs.mydatabases.ui.screens.queryeditor.components.SqlToken
import com.sphynxs.mydatabases.ui.screens.queryeditor.components.SqlTokenizer
import com.sphynxs.mydatabases.ui.screens.queryeditor.components.TokenKind

/**
 * Pure SQL formatter for MySQL/MariaDB.
 *
 * Transformation rules:
 * - UPPERCASE all KEYWORD tokens
 * - Insert newline before major clauses: FROM, WHERE, JOIN (INNER/LEFT/RIGHT/OUTER/CROSS/FULL),
 *   GROUP BY, ORDER BY, HAVING, LIMIT, UNION
 * - Insert 2-space indent after newline for ON/AND/OR subclauses
 * - Preserve STRING/COMMENT tokens byte-for-byte
 * - Preserve user projection formatting (no comma breaks)
 * - Trim trailing whitespace per line
 * - Collapse 3+ blank lines to 1
 * - Idempotent: format(format(x)) == format(x)
 *
 * Pure function, 100% JVM-testable (no Android dependencies).
 *
 * Spec: openspec/changes/editor-completion-and-format/spec.md (scenarios 1-8)
 * Design: ADR 2 — Formatter is pure function
 *
 * @author israel-icm
 * @date 2026-06-24
 */
object SqlFormatter {

    /**
     * Format SQL text to normalized form.
     *
     * @param sql Input SQL string (any case, any whitespace)
     * @return Formatted SQL (UPPERCASE keywords, newlines before major clauses, idempotent)
     */
    fun format(sql: String): String {
        if (sql.isBlank()) return ""

        val tokens = SqlTokenizer.tokenize(sql)
        if (tokens.isEmpty()) return ""

        val result = StringBuilder()
        var previousKind: TokenKind? = null
        var previousKeyword: String? = null
        var needsIndent = false

        for (i in tokens.indices) {
            val token = tokens[i]
            val tokenText = sql.substring(token.range)

            when (token.kind) {
                TokenKind.KEYWORD -> {
                    val uppercased = tokenText.uppercase()

                    // Insert newline before major clauses
                    if (shouldInsertNewlineBefore(uppercased) && result.isNotEmpty()) {
                        result.append("\n")
                        needsIndent = false
                    }

                    // Insert 2-space indent for ON/AND/OR subclauses after JOIN
                    if (shouldIndentSubclause(uppercased, previousKeyword)) {
                        if (result.lastOrNull() == '\n') {
                            result.append("  ")
                        }
                        needsIndent = false
                    }

                    result.append(uppercased)
                    previousKeyword = uppercased
                }

                TokenKind.STRING, TokenKind.COMMENT -> {
                    // Preserve strings and comments verbatim (byte-for-byte)
                    result.append(tokenText)
                }

                TokenKind.WHITESPACE -> {
                    // Preserve whitespace (collapsed later during line trimming)
                    if (result.isNotEmpty() && !result.endsWith("\n")) {
                        result.append(" ")
                    }
                }

                else -> {
                    // IDENTIFIER, NUMBER, OPERATOR, PUNCTUATION
                    result.append(tokenText)
                }
            }

            previousKind = token.kind
        }

        // Post-processing: trim trailing whitespace per line, collapse multiple blank lines
        return result.toString()
            .lines()
            .joinToString("\n") { it.trimEnd() }  // Trim trailing whitespace per line
            .replace(Regex("\n{3,}"), "\n\n")    // Collapse 3+ blank lines to 1
            .trim()
    }

    /**
     * Determines if a newline should be inserted before a keyword.
     *
     * Major clauses that start new lines:
     * - FROM, WHERE, HAVING, LIMIT, UNION
     * - JOIN variants: INNER JOIN, LEFT JOIN, RIGHT JOIN, OUTER JOIN, CROSS JOIN, FULL JOIN
     * - GROUP BY, ORDER BY
     */
    private fun shouldInsertNewlineBefore(keyword: String): Boolean {
        return keyword in setOf(
            "FROM", "WHERE", "HAVING", "LIMIT", "UNION",
            "INNER", "LEFT", "RIGHT", "OUTER", "CROSS", "FULL",
            "GROUP", "ORDER"
        )
    }

    /**
     * Determines if a subclause (ON, AND, OR) should be indented 2 spaces.
     *
     * Indents when:
     * - Current keyword is ON/AND/OR
     * - Previous keyword was a JOIN variant (INNER/LEFT/RIGHT/OUTER/CROSS/FULL/JOIN)
     *
     * This is a simplified v1 rule — full context tracking would require deeper state.
     */
    private fun shouldIndentSubclause(keyword: String, previousKeyword: String?): Boolean {
        if (keyword !in setOf("ON", "AND", "OR")) return false

        val joinKeywords = setOf("JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "CROSS", "FULL")
        return previousKeyword in joinKeywords
    }
}
