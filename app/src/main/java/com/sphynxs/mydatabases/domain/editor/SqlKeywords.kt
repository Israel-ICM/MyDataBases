package com.sphynxs.mydatabases.domain.editor

/**
 * Single source of truth for SQL keywords (MySQL/MariaDB).
 *
 * Consumed by:
 * - SqlTokenizer (syntax highlighting regex)
 * - SqlFormatter (UPPERCASE transformation)
 * - SqlCompletionProvider (keyword suggestions)
 *
 * All keywords MUST be UPPERCASE (normalized form).
 * Adding a keyword here automatically propagates to all consumers.
 *
 * Spec: openspec/changes/editor-completion-and-format/spec.md
 * Design: ADR 1 — SqlKeywords as single source of truth
 *
 * @author israel-icm
 * @date 2026-06-24
 */
object SqlKeywords {
    
    /**
     * Canonical set of ~75 MySQL/MariaDB keywords.
     *
     * Sorted alphabetically for maintainability.
     * Coverage: DML, DDL, DQL, TCL, and common functions.
     */
    val KEYWORDS: Set<String> = setOf(
        "ALL",
        "ALTER",
        "AND",
        "AS",
        "ASC",
        "AUTO_INCREMENT",
        "AVG",
        "BEGIN",
        "BETWEEN",
        "BY",
        "CASCADE",
        "CASE",
        "CHECK",
        "COMMIT",
        "CONSTRAINT",
        "COUNT",
        "CREATE",
        "CROSS",
        "DATABASE",
        "DEFAULT",
        "DELETE",
        "DESC",
        "DESCRIBE",
        "DISTINCT",
        "DROP",
        "ELSE",
        "END",
        "EXISTS",
        "EXPLAIN",
        "FOREIGN",
        "FROM",
        "FULL",
        "GROUP",
        "HAVING",
        "IF",
        "IN",
        "INDEX",
        "INNER",
        "INSERT",
        "INTO",
        "IS",
        "JOIN",
        "KEY",
        "LEFT",
        "LIKE",
        "LIMIT",
        "MAX",
        "MIN",
        "NOT",
        "NULL",
        "OFFSET",
        "ON",
        "OR",
        "ORDER",
        "OUTER",
        "PRIMARY",
        "REFERENCES",
        "RESTRICT",
        "RIGHT",
        "ROLLBACK",
        "SCHEMA",
        "SELECT",
        "SET",
        "SHOW",
        "START",
        "SUM",
        "TABLE",
        "THEN",
        "TRANSACTION",
        "UNION",
        "UNIQUE",
        "UPDATE",
        "USE",
        "VALUES",
        "VIEW",
        "WHEN",
        "WHERE",
        "WITH"
    )
}
