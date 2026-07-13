package com.sphynxs.mydatabases.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * Utilidades de contraste WCAG 2.0 para tokens de color — extraídas para permitir que
 * el contraste de `DesignTokens` se verifique con un test EJECUTABLE (no un cálculo
 * manual documentado en un comentario, que es como se venían verificando los valores
 * de `textSecondary`/`textTertiary`/`accentSuccess` hasta este fix-round).
 *
 * Implementa la fórmula oficial de luminancia relativa sRGB (WCAG 2.0, sección 1.4.3) a
 * mano — sin dependencias de Compose runtime/Context más allá de `Color` — para poder
 * correr en tests JVM puros. Sanity-checkeada contra ratios matemáticamente conocidos
 * (negro/blanco = 21.00:1 exacto, mismo color = 1.00:1 exacto) en `ContrastUtilsTest`
 * antes de usarse para validar tokens reales.
 *
 * Ver `openspec/changes/dark-mode/verify-report.md` (WARNING #3 `accentSuccess`,
 * WARNING #4 `textTertiary`) y `DesignTokensTest` (regresión de contraste).
 *
 * @author gentle-ai (TDD GREEN, verify fix-round)
 */

private fun linearizeChannel(component: Float): Double {
    val c = component.toDouble()
    return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
}

private fun relativeLuminance(color: Color): Double {
    val r = linearizeChannel(color.red)
    val g = linearizeChannel(color.green)
    val b = linearizeChannel(color.blue)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

/**
 * Computa el ratio de contraste WCAG 2.0 entre dos colores.
 *
 * @return un valor en `[1.0, 21.0]`. `>= 3.0` cumple el mínimo no-textual de WCAG
 *   1.4.11 (íconos, bordes, controles). `>= 4.5` cumple AA de WCAG 1.4.3 para texto
 *   normal.
 */
internal fun contrastRatio(a: Color, b: Color): Double {
    val luminanceA = relativeLuminance(a)
    val luminanceB = relativeLuminance(b)
    val lighter = maxOf(luminanceA, luminanceB)
    val darker = minOf(luminanceA, luminanceB)
    return (lighter + 0.05) / (darker + 0.05)
}
