package com.sphynxs.mydatabases.ui.screens.queryeditor.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Visual transformation to highlight find/replace matches (FR-14).
 *
 * Phase 5.6 — Overlay for match highlighting.
 *
 * Adds background color to all match ranges.
 * Current match gets a distinct color.
 *
 * @param matches List of match ranges
 * @param currentMatchIndex Index of current match (highlighted differently)
 *
 * @author israel-icm (SDD apply phase)
 * @date 2026-06-29
 */
class MatchHighlightTransformation(
    private val matches: List<TextRange>,
    private val currentMatchIndex: Int
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        if (matches.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val highlighted = buildAnnotatedString {
            append(text.text)

            // Add match highlights
            matches.forEachIndexed { index, range ->
                val isCurrent = index == currentMatchIndex
                val backgroundColor = if (isCurrent) {
                    Color(0xFFFFEB3B) // Yellow for current match
                } else {
                    Color(0xFFFFEB3B).copy(alpha = 0.4f) // Lighter yellow for other matches
                }

                addStyle(
                    // Fuerza texto oscuro sobre el highlight amarillo: sin esto, en dark
                    // mode el color de texto del editor (theme-aware, casi blanco) queda
                    // ilegible sobre fondo amarillo — bug estructural, no solo cosmético
                    // (ver design.md R6 heuristic: fondo literal + texto que sí cambia con
                    // el tema = contraste roto). El amarillo en sí queda decorativo/deferred.
                    style = SpanStyle(background = backgroundColor, color = Color(0xFF1A1A1A)),
                    start = range.start,
                    end = range.end
                )
            }
        }

        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}
