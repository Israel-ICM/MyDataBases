package com.sphynxs.mydatabases.ui.screens.queryeditor.components

import com.sphynxs.mydatabases.domain.editor.SqlKeywords

/**
 * Tokenizer puro para SQL (MySQL/MariaDB).
 *
 * Función pura, 100% testable en JVM unit tests sin Compose/Robolectric.
 * Basado en regex para detectar keywords, strings, comments, números, etc.
 *
 * Uses SqlKeywords.KEYWORDS as single source of truth (prevents drift with formatter/completion).
 *
 * @author israel-icm
 * @date 2026-06-23
 */
object SqlTokenizer {

    /**
     * Tokeniza un string SQL en tokens clasificados.
     *
     * Orden de precedencia (greedy-first para evitar matches incorrectos):
     * 1. Comments (line `--`, block `/* ... */`)
     * 2. Strings (single `'...'`, double `"..."`, backtick `` `...` ``)
     * 3. Numbers (`123`, `45.67`)
     * 4. Keywords (SELECT, FROM, WHERE, etc. — case-insensitive)
     * 5. Operators (`=`, `!=`, `<>`, `<=`, `>=`, etc.)
     * 6. Punctuation (`(`, `)`, `,`, `;`, `.`)
     * 7. Identifiers (nombres de tablas/columnas)
     * 8. Whitespace
     *
     * @param sql String SQL de entrada
     * @return Lista de [SqlToken] con rangos y clasificaciones
     */
    fun tokenize(sql: String): List<SqlToken> {
        if (sql.isEmpty()) return emptyList()

        val tokens = mutableListOf<SqlToken>()
        var position = 0

        // Regex patterns (orden importa — más específicos primero)
        val patterns = listOf(
            // 1. Block comment: /* ... */ (incluso multi-línea)
            Regex("""/\*[\s\S]*?\*/""") to TokenKind.COMMENT,

            // 2. Line comment: -- ...
            Regex("""--[^\r\n]*""") to TokenKind.COMMENT,

            // 3. Single-quoted string: 'value' (con escape '' para MySQL)
            Regex("""'(?:''|[^'])*'""") to TokenKind.STRING,

            // 4. Double-quoted string: "value"
            Regex(""""[^"]*"""") to TokenKind.STRING,

            // 5. Backtick-quoted identifier: `table-name`
            Regex("""`[^`]*`""") to TokenKind.IDENTIFIER,

            // 6. Numbers: int o decimal (45.67, 123)
            Regex("""\d+(\.\d+)?""") to TokenKind.NUMBER,

            // 7. Keywords (case-insensitive, from SqlKeywords single source of truth)
            Regex(
                """\b(?:${SqlKeywords.KEYWORDS.joinToString("|")})\b""",
                RegexOption.IGNORE_CASE
            ) to TokenKind.KEYWORD,

            // 8. Operators (multi-char primero, luego single-char)
            Regex("""<>|!=|<=|>=|&&|\|\||<<|>>|<|>|=|\+|-|\*|/|%""") to TokenKind.OPERATOR,

            // 9. Punctuation
            Regex("""[(),;.]""") to TokenKind.PUNCTUATION,

            // 10. Identifiers (nombres de tablas/columnas: letras, números, underscore)
            Regex("""[a-zA-Z_][a-zA-Z0-9_]*""") to TokenKind.IDENTIFIER,

            // 11. Whitespace (espacios, tabs, newlines)
            Regex("""\s+""") to TokenKind.WHITESPACE
        )

        while (position < sql.length) {
            var matched = false

            for ((pattern, kind) in patterns) {
                val matchResult = pattern.find(sql, position)
                if (matchResult != null && matchResult.range.first == position) {
                    tokens.add(
                        SqlToken(
                            range = matchResult.range,
                            kind = kind
                        )
                    )
                    position = matchResult.range.last + 1
                    matched = true
                    break
                }
            }

            // Si ningún pattern matchea, avanzar un carácter (fallback — no debería pasar en SQL válido)
            if (!matched) {
                position++
            }
        }

        return tokens
    }
}

/**
 * Token SQL producido por el tokenizer.
 *
 * @property range Rango de caracteres en el string de entrada (IntRange inclusivo)
 * @property kind Clasificación del token
 */
data class SqlToken(
    val range: IntRange,
    val kind: TokenKind
)

/**
 * Tipos de tokens SQL reconocidos.
 */
enum class TokenKind {
    /** SELECT, FROM, WHERE, INSERT, etc. */
    KEYWORD,

    /** 'foo', "bar" */
    STRING,

    /** -- line comment, /* block comment */ */
    COMMENT,

    /** 123, 45.67 */
    NUMBER,

    /** table_name, column_name, `backtick-id` */
    IDENTIFIER,

    /** =, !=, <>, <, >, <=, >=, +, -, *, /, % */
    OPERATOR,

    /** Espacios, tabs, newlines */
    WHITESPACE,

    /** ( ) , ; . */
    PUNCTUATION
}
