package com.sphynxs.mydatabases.ui.screens.queryeditor.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests para SqlTokenizer.
 *
 * Valida tokenización correcta de SQL (keywords, strings, comments, numbers, etc.)
 * con cobertura 100% (pure function, highly testable).
 *
 * @author israel-icm
 * @date 2026-06-23
 */
class SqlTokenizerTest {

    @Test
    fun `tokenize empty string returns empty list`() {
        // GIVEN: SQL vacío
        val sql = ""

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Lista vacía
        assertTrue(tokens.isEmpty())
    }

    @Test
    fun `tokenize whitespace only returns whitespace token`() {
        // GIVEN: Solo espacios
        val sql = "   \n\t  "

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Un token WHITESPACE
        assertEquals(1, tokens.size)
        assertEquals(TokenKind.WHITESPACE, tokens[0].kind)
        assertEquals(0..8, tokens[0].range)
    }

    @Test
    fun `tokenize SELECT keyword uppercase`() {
        // GIVEN: Keyword en mayúsculas
        val sql = "SELECT"

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Un token KEYWORD
        val keywords = tokens.filter { it.kind == TokenKind.KEYWORD }
        assertEquals(1, keywords.size)
        assertEquals(0..5, keywords[0].range)
    }

    @Test
    fun `tokenize select keyword lowercase`() {
        // GIVEN: Keyword en minúsculas
        val sql = "select"

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Un token KEYWORD (case-insensitive)
        val keywords = tokens.filter { it.kind == TokenKind.KEYWORD }
        assertEquals(1, keywords.size)
    }

    @Test
    fun `tokenize SeLeCt keyword mixed case`() {
        // GIVEN: Keyword en mixed case
        val sql = "SeLeCt"

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Un token KEYWORD
        val keywords = tokens.filter { it.kind == TokenKind.KEYWORD }
        assertEquals(1, keywords.size)
    }

    @Test
    fun `tokenize multiple keywords separated by whitespace`() {
        // GIVEN: Query simple
        val sql = "SELECT * FROM users"

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Tokens incluyen SELECT y FROM como KEYWORD
        val keywords = tokens.filter { it.kind == TokenKind.KEYWORD }
        assertTrue(keywords.size >= 2)
        assertTrue(keywords.any { sql.substring(it.range).equals("SELECT", ignoreCase = true) })
        assertTrue(keywords.any { sql.substring(it.range).equals("FROM", ignoreCase = true) })
    }

    @Test
    fun `tokenize single-quoted string`() {
        // GIVEN: String con comillas simples
        val sql = "'Ada Lovelace'"

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Un token STRING
        val strings = tokens.filter { it.kind == TokenKind.STRING }
        assertEquals(1, strings.size)
        assertEquals(0..13, strings[0].range)
    }

    @Test
    fun `tokenize double-quoted string`() {
        // GIVEN: String con comillas dobles
        val sql = "\"value\""

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Un token STRING
        val strings = tokens.filter { it.kind == TokenKind.STRING }
        assertEquals(1, strings.size)
    }

    @Test
    fun `tokenize string with escaped single quote`() {
        // GIVEN: String con escape '' (MySQL style)
        val sql = "'don''t'"

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Un token STRING que abarca toda la cadena
        val strings = tokens.filter { it.kind == TokenKind.STRING }
        assertEquals(1, strings.size)
        assertEquals(0..8, strings[0].range)
    }

    @Test
    fun `tokenize line comment`() {
        // GIVEN: Comentario de línea
        val sql = "-- old query\nSELECT 1"

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Un token COMMENT + tokens de SELECT 1
        val comments = tokens.filter { it.kind == TokenKind.COMMENT }
        assertEquals(1, comments.size)
        assertTrue(sql.substring(comments[0].range).startsWith("--"))
    }

    @Test
    fun `tokenize block comment single line`() {
        // GIVEN: Comentario de bloque en una línea
        val sql = "/* comment */ SELECT"

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Un token COMMENT
        val comments = tokens.filter { it.kind == TokenKind.COMMENT }
        assertEquals(1, comments.size)
        assertEquals("/* comment */", sql.substring(comments[0].range))
    }

    @Test
    fun `tokenize block comment multi line`() {
        // GIVEN: Comentario multi-línea
        val sql = "/* multi\n   line */ SELECT 1"

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Un token COMMENT que abarca todo el bloque
        val comments = tokens.filter { it.kind == TokenKind.COMMENT }
        assertEquals(1, comments.size)
        assertTrue(sql.substring(comments[0].range).contains("multi"))
        assertTrue(sql.substring(comments[0].range).contains("line"))
    }

    @Test
    fun `tokenize integer number`() {
        // GIVEN: Número entero
        val sql = "123"

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Un token NUMBER
        val numbers = tokens.filter { it.kind == TokenKind.NUMBER }
        assertEquals(1, numbers.size)
        assertEquals("123", sql.substring(numbers[0].range))
    }

    @Test
    fun `tokenize decimal number`() {
        // GIVEN: Número decimal
        val sql = "45.67"

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Un token NUMBER
        val numbers = tokens.filter { it.kind == TokenKind.NUMBER }
        assertEquals(1, numbers.size)
        assertEquals("45.67", sql.substring(numbers[0].range))
    }

    @Test
    fun `tokenize WHERE clause with number and comparison`() {
        // GIVEN: Condición con número
        val sql = "WHERE price > 45.67 AND qty = 3"

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Números 45.67 y 3 son NUMBER
        val numbers = tokens.filter { it.kind == TokenKind.NUMBER }
        assertEquals(2, numbers.size)
        assertTrue(numbers.any { sql.substring(it.range) == "45.67" })
        assertTrue(numbers.any { sql.substring(it.range) == "3" })
    }

    @Test
    fun `tokenize identifier plain`() {
        // GIVEN: Identificador simple
        val sql = "users"

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Un token IDENTIFIER
        val identifiers = tokens.filter { it.kind == TokenKind.IDENTIFIER }
        assertEquals(1, identifiers.size)
    }

    @Test
    fun `tokenize identifier with underscore`() {
        // GIVEN: Identificador con underscore
        val sql = "user_id"

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Un token IDENTIFIER
        val identifiers = tokens.filter { it.kind == TokenKind.IDENTIFIER }
        assertEquals(1, identifiers.size)
        assertEquals("user_id", sql.substring(identifiers[0].range))
    }

    @Test
    fun `tokenize identifier backtick quoted`() {
        // GIVEN: Identificador con backticks
        val sql = "`table-name`"

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Un token IDENTIFIER
        val identifiers = tokens.filter { it.kind == TokenKind.IDENTIFIER }
        assertEquals(1, identifiers.size)
        assertEquals("`table-name`", sql.substring(identifiers[0].range))
    }

    @Test
    fun `tokenize operators`() {
        // GIVEN: Operadores varios
        val sql = "= != <> < > <= >="

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Múltiples tokens OPERATOR
        val operators = tokens.filter { it.kind == TokenKind.OPERATOR }
        assertTrue(operators.size >= 7)
    }

    @Test
    fun `tokenize punctuation`() {
        // GIVEN: Puntuación
        val sql = "( ) , ; ."

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Tokens PUNCTUATION
        val punctuation = tokens.filter { it.kind == TokenKind.PUNCTUATION }
        assertTrue(punctuation.size >= 5)
    }

    @Test
    fun `tokenize semicolon inside string is not separator`() {
        // GIVEN: String con semicolon dentro
        val sql = "SELECT 'a;b' FROM dual"

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: El semicolon es parte del token STRING, no PUNCTUATION
        val strings = tokens.filter { it.kind == TokenKind.STRING }
        assertEquals(1, strings.size)
        assertTrue(sql.substring(strings[0].range).contains(";"))
    }

    @Test
    fun `tokenize complex query with all token types`() {
        // GIVEN: Query complejo
        val sql = """
            -- Get active users
            SELECT id, name FROM users WHERE active = 1 AND price > 45.67;
        """.trimIndent()

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: Contiene todos los tipos
        assertTrue(tokens.any { it.kind == TokenKind.COMMENT })
        assertTrue(tokens.any { it.kind == TokenKind.KEYWORD })
        assertTrue(tokens.any { it.kind == TokenKind.IDENTIFIER })
        assertTrue(tokens.any { it.kind == TokenKind.NUMBER })
        assertTrue(tokens.any { it.kind == TokenKind.OPERATOR })
        assertTrue(tokens.any { it.kind == TokenKind.PUNCTUATION })
        assertTrue(tokens.any { it.kind == TokenKind.WHITESPACE })
    }

    @Test
    fun `tokenize INSERT with VALUES keyword`() {
        // GIVEN: INSERT statement
        val sql = "INSERT INTO logs (msg) VALUES ('hi')"

        // WHEN: Tokenizamos
        val tokens = SqlTokenizer.tokenize(sql)

        // THEN: INSERT, INTO, VALUES son keywords
        val keywords = tokens.filter { it.kind == TokenKind.KEYWORD }
        assertTrue(keywords.any { sql.substring(it.range).equals("INSERT", ignoreCase = true) })
        assertTrue(keywords.any { sql.substring(it.range).equals("INTO", ignoreCase = true) })
        assertTrue(keywords.any { sql.substring(it.range).equals("VALUES", ignoreCase = true) })
    }
}
