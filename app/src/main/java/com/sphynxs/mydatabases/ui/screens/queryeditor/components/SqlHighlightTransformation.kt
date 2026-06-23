package com.sphynxs.mydatabases.ui.screens.queryeditor.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Visual transformation para syntax highlighting de SQL.
 *
 * Aplica colores y estilos a tokens SQL según el esquema de color activo
 * (MaterialTheme.colorScheme). Se integra con BasicTextField via `visualTransformation`.
 *
 * Debounced re-tokenization se maneja FUERA de este componente (300ms en el caller).
 *
 * @param tokens Lista de tokens SQL (producidos por SqlTokenizer)
 * @param keywordColor Color para keywords (SELECT, FROM, etc.)
 * @param stringColor Color para strings ('foo', "bar")
 * @param commentColor Color para comments (-- ..., /* ... */)
 * @param numberColor Color para números (123, 45.67)
 * @param operatorColor Color para operadores (=, !=, <>, etc.)
 *
 * @author israel-icm
 * @date 2026-06-23
 */
class SqlHighlightTransformation(
    private val tokens: List<SqlToken>,
    private val keywordColor: Color,
    private val stringColor: Color,
    private val commentColor: Color,
    private val numberColor: Color,
    private val operatorColor: Color
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)

        tokens.forEach { token ->
            // Validar que el token range no exceda el tamaño del texto actual
            // (puede pasar si tokens se calcularon con texto anterior y el usuario está borrando)
            val start = token.range.first.coerceAtMost(text.text.length)
            val end = (token.range.last + 1).coerceAtMost(text.text.length)
            
            if (start >= end) return@forEach // Token inválido, skip

            val style = when (token.kind) {
                TokenKind.KEYWORD -> SpanStyle(
                    color = keywordColor,
                    fontWeight = FontWeight.Bold
                )
                TokenKind.STRING -> SpanStyle(
                    color = stringColor
                )
                TokenKind.COMMENT -> SpanStyle(
                    color = commentColor,
                    fontStyle = FontStyle.Italic
                )
                TokenKind.NUMBER -> SpanStyle(
                    color = numberColor
                )
                TokenKind.OPERATOR -> SpanStyle(
                    color = operatorColor
                )
                // IDENTIFIER, WHITESPACE, PUNCTUATION → default (sin color custom)
                else -> null
            }

            style?.let {
                builder.addStyle(it, start, end)
            }
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

/**
 * Composable helper para crear SqlHighlightTransformation con theme colors.
 *
 * Usa MaterialTheme.colorScheme para colores adaptados al theme activo.
 *
 * @param tokens Lista de tokens SQL (actualizar con debounce 300ms)
 * @return VisualTransformation lista para aplicar a BasicTextField
 */
@Composable
fun rememberSqlHighlightTransformation(tokens: List<SqlToken>): VisualTransformation {
    val colorScheme = MaterialTheme.colorScheme

    return remember(tokens, colorScheme) {
        SqlHighlightTransformation(
            tokens = tokens,
            keywordColor = colorScheme.primary,
            stringColor = colorScheme.tertiary,
            commentColor = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            numberColor = colorScheme.secondary,
            operatorColor = colorScheme.onSurface
        )
    }
}
