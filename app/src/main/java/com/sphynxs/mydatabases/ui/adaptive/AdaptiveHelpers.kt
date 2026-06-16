package com.sphynxs.mydatabases.ui.adaptive

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Helpers adaptativos para UI responsive según WindowSizeClass.
 *
 * Proporcionan valores de padding, grid columns, y tamaño de íconos
 * optimizados para Compact, Medium y Expanded.
 *
 * @author israel-icm
 * @date 2026-06-15
 */

/**
 * Devuelve el padding adaptativo según el ancho de pantalla.
 *
 * ## Valores por WindowWidthSizeClass
 *
 * - **Compact** (< 600dp): 16.dp — pantallas pequeñas, maximizar espacio
 * - **Medium** (600-840dp): 24.dp — tablets portrait, más aire
 * - **Expanded** (> 840dp): 32.dp — tablets landscape/desktops, padding generoso
 *
 * @param windowSizeClass WindowSizeClass actual del dispositivo
 * @return PaddingValues adaptativo
 *
 * ## Ejemplo
 *
 * ```kotlin
 * Box(modifier = Modifier.padding(adaptivePadding(windowSizeClass))) {
 *     // contenido
 * }
 * ```
 */
fun adaptivePadding(windowSizeClass: WindowSizeClass): PaddingValues {
    return when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> PaddingValues(16.dp)
        WindowWidthSizeClass.Medium -> PaddingValues(24.dp)
        WindowWidthSizeClass.Expanded -> PaddingValues(32.dp)
        else -> PaddingValues(16.dp) // Fallback seguro
    }
}

/**
 * Devuelve el número de columnas de grid adaptativo según el ancho de pantalla.
 *
 * ## Valores por WindowWidthSizeClass
 *
 * - **Compact** (< 600dp): 1 columna — lista vertical simple
 * - **Medium** (600-840dp): 2 columnas — grid 2×N
 * - **Expanded** (> 840dp): 3 columnas — grid 3×N
 *
 * @param windowSizeClass WindowSizeClass actual del dispositivo
 * @return Número de columnas para LazyVerticalGrid
 *
 * ## Ejemplo
 *
 * ```kotlin
 * LazyVerticalGrid(
 *     columns = GridCells.Fixed(adaptiveGridColumns(windowSizeClass))
 * ) {
 *     items(connections) { connection ->
 *         ConnectionCard(connection)
 *     }
 * }
 * ```
 */
fun adaptiveGridColumns(windowSizeClass: WindowSizeClass): Int {
    return when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 1
        WindowWidthSizeClass.Medium -> 2
        WindowWidthSizeClass.Expanded -> 3
        else -> 1 // Fallback seguro
    }
}

/**
 * Devuelve el tamaño de ícono adaptativo según el ancho de pantalla.
 *
 * ## Valores por WindowWidthSizeClass
 *
 * - **Compact** (< 600dp): 24.dp — íconos estándar Material
 * - **Medium** (600-840dp): 28.dp — íconos ligeramente más grandes
 * - **Expanded** (> 840dp): 32.dp — íconos grandes para touch targets amplios
 *
 * @param windowSizeClass WindowSizeClass actual del dispositivo
 * @return Tamaño de ícono en Dp
 *
 * ## Ejemplo
 *
 * ```kotlin
 * Icon(
 *     painter = painterResource(R.drawable.ic_nav_tables),
 *     contentDescription = "Tables",
 *     modifier = Modifier.size(adaptiveIconSize(windowSizeClass))
 * )
 * ```
 */
fun adaptiveIconSize(windowSizeClass: WindowSizeClass): Dp {
    return when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 24.dp
        WindowWidthSizeClass.Medium -> 28.dp
        WindowWidthSizeClass.Expanded -> 32.dp
        else -> 24.dp // Fallback seguro
    }
}
