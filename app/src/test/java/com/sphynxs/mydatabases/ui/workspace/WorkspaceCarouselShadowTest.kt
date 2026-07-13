package com.sphynxs.mydatabases.ui.workspace

import androidx.compose.ui.graphics.toArgb
import com.sphynxs.mydatabases.ui.theme.BrandedDarkColorScheme
import com.sphynxs.mydatabases.ui.theme.BrandedLightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Golden/smoke test para `carouselShadowColorArgb(onSurfaceColor)`, la función pura que
 * deriva el color de la sombra dibujada a mano de [WorkspaceCarouselItem] a partir del
 * `onSurface` del `ColorScheme` activo.
 *
 * Cubre el bug que motivó la Fase 3 (R6 — Dark-Safe Custom-Drawn Effects): la sombra
 * usaba `android.graphics.Color.BLACK` fijo, invisible sobre `BrandedDarkColorScheme`
 * (fondo ya oscuro). La derivación debe producir un tono que SÍ contraste sobre dark
 * (técnica "lighter-overlay" de Material) — ver design.md Architecture Decisions.
 *
 * @author gentle-ai (TDD RED, PR-3)
 */
class WorkspaceCarouselShadowTest {

    @Test
    fun `shadow color derives from the given onSurface color, round-trips to ARGB`() {
        val onSurface = BrandedLightColorScheme.onSurface

        val result = carouselShadowColorArgb(onSurface)

        assertEquals(onSurface.toArgb(), result)
    }

    @Test
    fun `dark theme shadow is NOT pure black — visible on dark surface (R6 regression guard)`() {
        // BrandedDarkColorScheme.onSurface = brand_on_bg, a near-white tone by design
        // (design.md: "onSurface contrasts in both, matching Material's lighter-overlay
        // dark-elevation technique"). Pure black here would reproduce the exact bug R6
        // exists to prevent: a shadow invisible against WorkspaceCarousel's dark backdrop.
        val darkOnSurface = BrandedDarkColorScheme.onSurface

        val result = carouselShadowColorArgb(darkOnSurface)

        assertNotEquals(android.graphics.Color.BLACK, result)
    }

    @Test
    fun `light and dark themes derive different shadow tones`() {
        val lightResult = carouselShadowColorArgb(BrandedLightColorScheme.onSurface)
        val darkResult = carouselShadowColorArgb(BrandedDarkColorScheme.onSurface)

        assertNotEquals(lightResult, darkResult)
    }
}
