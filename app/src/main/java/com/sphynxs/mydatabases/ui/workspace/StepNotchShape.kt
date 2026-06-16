package com.sphynxs.mydatabases.ui.workspace

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Shape custom con escalón/notch en la parte inferior izquierda.
 * 
 * Forma:
 * ```
 * |________________|
 * |                |
 * |      __________|
 * |_____|
 * ```
 * 
 * @param stepWidthFraction Fracción del ancho donde empieza el escalón (0.0 - 1.0)
 * @param stepHeightDp Altura del escalón en dp
 * @param cornerRadiusDp Radio de las esquinas redondeadas en dp
 */
class StepNotchShape(
    private val stepWidthFraction: Float = 0.25f,
    private val stepHeightDp: Float = 60f,
    private val cornerRadiusDp: Float = 24f
) : Shape {
    
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        
        with(density) {
            val cornerRadius = cornerRadiusDp.dp.toPx()
            val stepHeight = stepHeightDp.dp.toPx()
            val stepX = size.width * stepWidthFraction
            
            val width = size.width
            val height = size.height
            
            // Empezar desde top-left (después del corner radius)
            path.moveTo(0f, cornerRadius)
            
            // Top-left corner (redondeada)
            path.quadraticTo(
                0f, 0f,
                cornerRadius, 0f
            )
            
            // Top edge
            path.lineTo(width - cornerRadius, 0f)
            
            // Top-right corner (redondeada)
            path.quadraticTo(
                width, 0f,
                width, cornerRadius
            )
            
            // Right edge hasta antes del escalón
            path.lineTo(width, height - stepHeight - cornerRadius)
            
            // Bottom-right corner del nivel superior (redondeada)
            path.quadraticTo(
                width, height - stepHeight,
                width - cornerRadius, height - stepHeight
            )
            
            // Línea horizontal hasta el escalón
            path.lineTo(stepX + cornerRadius, height - stepHeight)
            
            // Curva descendente del escalón (suave)
            path.quadraticTo(
                stepX, height - stepHeight,
                stepX, height - stepHeight + cornerRadius
            )
            
            // Bajada vertical del escalón
            path.lineTo(stepX, height - cornerRadius)
            
            // Bottom-left corner del nivel inferior (redondeada)
            path.quadraticTo(
                stepX, height,
                stepX - cornerRadius, height
            )
            
            // Bottom edge izquierdo
            path.lineTo(cornerRadius, height)
            
            // Bottom-left corner principal (redondeada)
            path.quadraticTo(
                0f, height,
                0f, height - cornerRadius
            )
            
            // Left edge de vuelta al inicio
            path.lineTo(0f, cornerRadius)
            
            path.close()
        }
        
        return Outline.Generic(path)
    }
}
