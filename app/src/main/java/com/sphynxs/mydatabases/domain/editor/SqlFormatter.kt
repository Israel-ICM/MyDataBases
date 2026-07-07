package com.sphynxs.mydatabases.domain.editor

import com.sphynxs.mydatabases.ui.screens.queryeditor.components.SqlToken
import com.sphynxs.mydatabases.ui.screens.queryeditor.components.SqlTokenizer
import com.sphynxs.mydatabases.ui.screens.queryeditor.components.TokenKind

/**
 * Pure SQL formatter for MySQL/MariaDB.
 *
 * Transformation rules (see spec.md Capability: sql-formatter, and design.md ADR 7 for the
 * depth-tracked state machine this implements):
 * - UPPERCASE all KEYWORD tokens.
 * - Split multiple top-level statements on `;` (outside strings/comments/parens); format each
 *   independently; rejoin with `;\n`; preserve a trailing `;`.
 * - Insert newline before major clauses: FROM, WHERE, JOIN (INNER/LEFT/RIGHT/OUTER/CROSS/FULL),
 *   GROUP BY, ORDER BY, HAVING, LIMIT, UNION.
 * - `SELECT` sits alone on its clause line; the projection list breaks one column per line
 *   (2-space indent), even for a single column.
 * - `FROM`/`WHERE` clause bodies break onto their own indented line (2 spaces), unconditionally.
 * - A top-level parenthesized comma list following `INSERT INTO <table>` or `VALUES` breaks one
 *   item per line (2-space indent), including single-item lists.
 * - Parenthesized nesting beyond depth 1 (e.g. a subquery inside `VALUES`, or a function-call
 *   argument list) is left flat — only keyword-case normalization applies there.
 * - Insert 2-space indent after newline for ON/AND/OR subclauses following a JOIN predicate.
 * - Preserve STRING/COMMENT tokens byte-for-byte.
 * - Trim trailing whitespace per line.
 * - Collapse 3+ blank lines to 1.
 * - Idempotent: `format(format(x)) == format(x)`. All structural decisions derive ONLY from
 *   token KIND + keyword/punctuation identity, never from WHITESPACE content, so re-tokenizing
 *   the output reproduces the same non-whitespace token sequence and thus the same decisions.
 *
 * Pure function, 100% JVM-testable (no Android dependencies).
 *
 * Spec: openspec/changes/editor-completion-and-format/spec.md (scenarios 1-8f)
 * Design: ADR 2 (pure function) + ADR 7 (statement + list breaking state machine — see that
 * ADR's "[CORRECTED during sdd-apply]" note for 3 ordering fixes found while implementing this)
 *
 * @author israel-icm
 * @date 2026-07-07
 */
object SqlFormatter {

    /**
     * Keywords that force their own clause line at `parenDepth == 0`.
     *
     * `VALUES` was added here during implementation (not present in the original ADR 7
     * pseudocode) — without it, `VALUES` stays glued to the preceding `)` instead of landing
     * on its own line as the golden fixture requires. See design.md ADR 7 correction note.
     */
    private val CLAUSE_NEWLINE_KEYWORDS = setOf(
        "FROM", "WHERE", "HAVING", "LIMIT", "UNION",
        "INNER", "LEFT", "RIGHT", "OUTER", "CROSS", "FULL",
        "GROUP", "ORDER", "VALUES"
    )

    /** Of the clause-newline keywords, which ALSO force their body onto its own indented line. */
    private val BODY_INDENT_KEYWORDS = setOf("FROM", "WHERE")

    /** JOIN-family keywords that make a following ON/AND/OR indent underneath them. */
    private val JOIN_KEYWORDS = setOf("JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "CROSS", "FULL")

    /** ON/AND/OR — subclause keywords that indent when they directly follow a JOIN predicate. */
    private val SUBCLAUSE_KEYWORDS = setOf("ON", "AND", "OR")

    private enum class ListMode { NONE, PROJECTION, PAREN_LIST }

    /** True when [kw] must force its own clause line at the current [parenDepth]. */
    private fun shouldInsertNewlineBefore(kw: String, parenDepth: Int): Boolean =
        kw in CLAUSE_NEWLINE_KEYWORDS && parenDepth == 0

    /** True when [kw] is an ON/AND/OR subclause directly following a JOIN-family keyword. */
    private fun shouldIndentSubclause(kw: String, previousKeyword: String?): Boolean =
        kw in SUBCLAUSE_KEYWORDS && previousKeyword in JOIN_KEYWORDS

    /**
     * Format SQL text to normalized form.
     *
     * @param sql Input SQL string (any case, any whitespace, possibly multiple `;`-separated
     *   statements)
     * @return Formatted SQL — see class doc for the full rule set
     */
    fun format(sql: String): String {
        if (sql.isBlank()) return ""

        val tokens = SqlTokenizer.tokenize(sql)
        if (tokens.isEmpty()) return ""

        val (segments, hadTrailingSemicolon) = splitTopLevelStatements(sql, tokens)
        val formattedStatements = segments
            .map { formatStatement(sql, it) }
            .filter { it.isNotBlank() }

        if (formattedStatements.isEmpty()) return ""

        val joined = formattedStatements.joinToString(";\n")
        return if (hadTrailingSemicolon) "$joined;" else joined
    }

    /**
     * Splits [tokens] into independent top-level statements at `;` boundaries sitting at
     * paren-depth 0. The `;` token itself is dropped from both segments it separates. A `;`
     * inside a string/comment can never trigger a split because the tokenizer already consumed
     * it as part of a single STRING/COMMENT token range, never as its own PUNCTUATION token.
     *
     * @return the per-statement token lists, plus whether the input had a trailing top-level
     *   `;` (i.e. the final segment produced by the split is empty or whitespace-only).
     */
    private fun splitTopLevelStatements(
        sql: String,
        tokens: List<SqlToken>
    ): Pair<List<List<SqlToken>>, Boolean> {
        var depth = 0
        val segments = mutableListOf(mutableListOf<SqlToken>())

        for (token in tokens) {
            if (token.kind == TokenKind.PUNCTUATION) {
                when (sql.substring(token.range)) {
                    "(" -> depth++
                    ")" -> depth = maxOf(0, depth - 1)
                    ";" -> if (depth == 0) {
                        segments.add(mutableListOf())
                        continue
                    }
                }
            }
            segments.last().add(token)
        }

        val trailingSemicolon = segments.last().all { it.kind == TokenKind.WHITESPACE }
        if (trailingSemicolon) segments.removeAt(segments.lastIndex)

        return segments to trailingSemicolon
    }

    /**
     * Formats a single statement's token list per the ADR 7 depth-tracked state machine.
     */
    private fun formatStatement(sql: String, tokens: List<SqlToken>): String {
        if (tokens.isEmpty()) return ""

        val result = StringBuilder()

        var parenDepth = 0
        var activeListDepth: Int? = null
        var listMode = ListMode.NONE
        var indentLevel = 0
        var atLineStart = false
        var pendingListTrigger = false
        var pendingTableCapture = false
        var previousKeyword: String? = null

        fun appendText(text: String) {
            result.append(text)
            atLineStart = false
        }

        fun breakLine() {
            result.append("\n").append("  ".repeat(indentLevel))
            atLineStart = true
        }

        for (token in tokens) {
            val text = sql.substring(token.range)

            when (token.kind) {
                TokenKind.KEYWORD -> {
                    val kw = text.uppercase()

                    if (shouldInsertNewlineBefore(kw, parenDepth)) {
                        // Reset indent/list state BEFORE breaking so the clause keyword lands
                        // at BASE indent (not whatever indent the previous clause body used).
                        indentLevel = 0
                        listMode = ListMode.NONE
                        breakLine()
                    } else if (shouldIndentSubclause(kw, previousKeyword)) {
                        // Existing ON/AND/OR-after-JOIN indent, routed through atLineStart.
                        indentLevel = 1
                        breakLine()
                    }

                    appendText(kw)

                    when {
                        kw == "SELECT" && parenDepth == 0 -> {
                            listMode = ListMode.PROJECTION
                            indentLevel = 1
                            breakLine()
                        }
                        kw in BODY_INDENT_KEYWORDS && parenDepth == 0 -> {
                            indentLevel = 1
                            breakLine()
                        }
                        kw == "INTO" -> pendingTableCapture = true
                        kw == "VALUES" && parenDepth == 0 -> pendingListTrigger = true
                    }

                    previousKeyword = kw
                }

                TokenKind.IDENTIFIER -> {
                    appendText(text)
                    if (pendingTableCapture) {
                        pendingListTrigger = true
                        pendingTableCapture = false
                    }
                }

                TokenKind.WHITESPACE -> {
                    if (!atLineStart && result.isNotEmpty()) {
                        result.append(" ")
                    }
                }

                TokenKind.STRING, TokenKind.COMMENT -> appendText(text)

                TokenKind.PUNCTUATION -> {
                    when (text) {
                        "(" -> {
                            val isListOpenParen = pendingListTrigger &&
                                parenDepth == 0 &&
                                activeListDepth == null
                            if (isListOpenParen) {
                                parenDepth++
                                activeListDepth = parenDepth
                                listMode = ListMode.PAREN_LIST
                                // Break BEFORE appending "(" so it sits alone on its own line
                                // at base indent; only then bump indent for the list items.
                                breakLine()
                                appendText("(")
                                indentLevel = 1
                                breakLine()
                                pendingListTrigger = false
                            } else {
                                parenDepth++
                                appendText("(")
                            }
                        }

                        ")" -> {
                            if (activeListDepth != null && parenDepth == activeListDepth) {
                                indentLevel = 0
                                breakLine()
                                appendText(")")
                                parenDepth--
                                activeListDepth = null
                                listMode = ListMode.NONE
                            } else {
                                appendText(")")
                                parenDepth = maxOf(0, parenDepth - 1)
                            }
                        }

                        "," -> {
                            val isListBreakComma =
                                (listMode == ListMode.PAREN_LIST && parenDepth == activeListDepth) ||
                                    (listMode == ListMode.PROJECTION && parenDepth == 0)
                            appendText(",")
                            if (isListBreakComma) {
                                breakLine()
                            }
                        }

                        else -> appendText(text)
                    }
                }

                else -> appendText(text)
            }
        }

        return result.toString()
            .lines()
            .joinToString("\n") { it.trimEnd() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}
