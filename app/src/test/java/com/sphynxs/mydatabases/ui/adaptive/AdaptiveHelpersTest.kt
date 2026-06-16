package com.sphynxs.mydatabases.ui.adaptive

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.unit.DensityImpl
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests para helpers adaptativos — padding, columns, iconSize según WindowSizeClass.
 *
 * @author israel-icm
 * @date 2026-06-15
 */
class AdaptiveHelpersTest {
    
    private val density = DensityImpl(1f, 1f)
    
    @Test
    fun `adaptivePadding Compact devuelve 16dp`() {
        // GIVEN
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            size = androidx.compose.ui.unit.DpSize(400.dp, 800.dp),
            density = density
        )
        
        // WHEN
        val padding = adaptivePadding(windowSizeClass)
        
        // THEN
        assertEquals(PaddingValues(16.dp), padding)
    }
    
    @Test
    fun `adaptivePadding Medium devuelve 24dp`() {
        // GIVEN
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            size = androidx.compose.ui.unit.DpSize(700.dp, 1000.dp),
            density = density
        )
        
        // WHEN
        val padding = adaptivePadding(windowSizeClass)
        
        // THEN
        assertEquals(PaddingValues(24.dp), padding)
    }
    
    @Test
    fun `adaptivePadding Expanded devuelve 32dp`() {
        // GIVEN
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            size = androidx.compose.ui.unit.DpSize(1200.dp, 1000.dp),
            density = density
        )
        
        // WHEN
        val padding = adaptivePadding(windowSizeClass)
        
        // THEN
        assertEquals(PaddingValues(32.dp), padding)
    }
    
    @Test
    fun `adaptiveGridColumns Compact devuelve 1`() {
        // GIVEN
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            size = androidx.compose.ui.unit.DpSize(400.dp, 800.dp),
            density = density
        )
        
        // WHEN
        val columns = adaptiveGridColumns(windowSizeClass)
        
        // THEN
        assertEquals(1, columns)
    }
    
    @Test
    fun `adaptiveGridColumns Medium devuelve 2`() {
        // GIVEN
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            size = androidx.compose.ui.unit.DpSize(700.dp, 1000.dp),
            density = density
        )
        
        // WHEN
        val columns = adaptiveGridColumns(windowSizeClass)
        
        // THEN
        assertEquals(2, columns)
    }
    
    @Test
    fun `adaptiveGridColumns Expanded devuelve 3`() {
        // GIVEN
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            size = androidx.compose.ui.unit.DpSize(1200.dp, 1000.dp),
            density = density
        )
        
        // WHEN
        val columns = adaptiveGridColumns(windowSizeClass)
        
        // THEN
        assertEquals(3, columns)
    }
    
    @Test
    fun `adaptiveIconSize Compact devuelve 24dp`() {
        // GIVEN
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            size = androidx.compose.ui.unit.DpSize(400.dp, 800.dp),
            density = density
        )
        
        // WHEN
        val iconSize = adaptiveIconSize(windowSizeClass)
        
        // THEN
        assertEquals(24.dp, iconSize)
    }
    
    @Test
    fun `adaptiveIconSize Medium devuelve 28dp`() {
        // GIVEN
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            size = androidx.compose.ui.unit.DpSize(700.dp, 1000.dp),
            density = density
        )
        
        // WHEN
        val iconSize = adaptiveIconSize(windowSizeClass)
        
        // THEN
        assertEquals(28.dp, iconSize)
    }
    
    @Test
    fun `adaptiveIconSize Expanded devuelve 32dp`() {
        // GIVEN
        val windowSizeClass = WindowSizeClass.calculateFromSize(
            size = androidx.compose.ui.unit.DpSize(1200.dp, 1000.dp),
            density = density
        )
        
        // WHEN
        val iconSize = adaptiveIconSize(windowSizeClass)
        
        // THEN
        assertEquals(32.dp, iconSize)
    }
}
