package com.sphynxs.mydatabases.domain.editor

import androidx.compose.ui.text.TextRange
import org.junit.Assert.*
import org.junit.Test

class EditorSnapshotTest {
    
    @Test
    fun `data class equality works correctly`() {
        val snapshot1 = EditorSnapshot(
            text = "SELECT * FROM users",
            selection = TextRange(0, 6),
            cursorPositions = listOf(10, 20),
            timestamp = 1000L
        )
        
        val snapshot2 = EditorSnapshot(
            text = "SELECT * FROM users",
            selection = TextRange(0, 6),
            cursorPositions = listOf(10, 20),
            timestamp = 1000L
        )
        
        assertEquals(snapshot1, snapshot2)
        assertEquals(snapshot1.hashCode(), snapshot2.hashCode())
    }
    
    @Test
    fun `copy creates independent instance`() {
        val original = EditorSnapshot(
            text = "SELECT",
            selection = TextRange(6),
            cursorPositions = listOf(0)
        )
        
        val copy = original.copy(text = "INSERT")
        
        assertEquals("INSERT", copy.text)
        assertEquals("SELECT", original.text) // Original unchanged
    }
    
    @Test
    fun `timestamp defaults to current time`() {
        val before = System.currentTimeMillis()
        val snapshot = EditorSnapshot(
            text = "test",
            selection = TextRange(0),
            cursorPositions = emptyList()
        )
        val after = System.currentTimeMillis()
        
        assertTrue(snapshot.timestamp in before..after)
    }
}
