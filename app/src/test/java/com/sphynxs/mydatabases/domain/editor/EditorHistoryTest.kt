package com.sphynxs.mydatabases.domain.editor

import androidx.compose.ui.text.TextRange
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EditorHistoryTest {
    
    private lateinit var history: EditorHistory
    
    @Before
    fun setup() {
        history = EditorHistory(maxSnapshots = 100, coalescingWindowMs = 500)
    }
    
    private fun createSnapshot(
        text: String,
        cursorPos: Int = 0,
        timestamp: Long = System.currentTimeMillis()
    ) = EditorSnapshot(
        text = text,
        selection = TextRange(cursorPos),
        cursorPositions = emptyList(),
        timestamp = timestamp
    )
    
    @Test
    fun `push adds snapshot to undo stack`() {
        val snapshot = createSnapshot("SELECT")
        history.push(snapshot)
        
        assertEquals(snapshot, history.current())
        assertTrue(history.canUndo())
    }
    
    @Test
    fun `undo returns previous snapshot`() {
        val snap1 = createSnapshot("SELECT", timestamp = 1000)
        val snap2 = createSnapshot("SELECT *", timestamp = 2000)
        
        history.push(snap1)
        Thread.sleep(600) // Wait past coalescing window
        history.push(snap2)
        
        val restored = history.undo()
        
        assertEquals(snap1, restored)
        assertTrue(history.canRedo())
    }
    
    @Test
    fun `redo restores undone snapshot`() {
        val snap1 = createSnapshot("SELECT", timestamp = 1000)
        val snap2 = createSnapshot("SELECT *", timestamp = 2000)
        
        history.push(snap1)
        Thread.sleep(600)
        history.push(snap2)
        
        history.undo()
        val restored = history.redo()
        
        assertEquals(snap2, restored)
    }
    
    @Test
    fun `coalescing replaces recent snapshot`() {
        val snap1 = createSnapshot("S", timestamp = 1000)
        val snap2 = createSnapshot("SE", timestamp = 1100) // Within 500ms window
        
        history.push(snap1)
        history.push(snap2)
        
        assertEquals(snap2, history.current())
        assertFalse(history.canUndo()) // Only one snapshot (coalesced)
    }
    
    @Test
    fun `no coalescing after time window`() {
        val snap1 = createSnapshot("SELECT", timestamp = 1000)
        val snap2 = createSnapshot("SELECT *", timestamp = 2000) // 1000ms later
        
        history.push(snap1)
        Thread.sleep(600)
        history.push(snap2)
        
        assertTrue(history.canUndo()) // Two separate snapshots
    }
    
    @Test
    fun `max snapshots enforced (FIFO drop)`() {
        val smallHistory = EditorHistory(maxSnapshots = 3)
        
        smallHistory.push(createSnapshot("1", timestamp = 1000))
        Thread.sleep(600)
        smallHistory.push(createSnapshot("2", timestamp = 2000))
        Thread.sleep(600)
        smallHistory.push(createSnapshot("3", timestamp = 3000))
        Thread.sleep(600)
        smallHistory.push(createSnapshot("4", timestamp = 4000)) // Should drop "1"
        
        // Undo 3 times should get to "2" (oldest available)
        smallHistory.undo() // 4 -> 3
        smallHistory.undo() // 3 -> 2
        val restored = smallHistory.undo() // 2 -> null (no more)
        
        assertNull(restored) // "1" was dropped
    }
    
    @Test
    fun `push clears redo stack`() {
        history.push(createSnapshot("SELECT", timestamp = 1000))
        Thread.sleep(600)
        history.push(createSnapshot("SELECT *", timestamp = 2000))
        
        history.undo()
        assertTrue(history.canRedo())
        
        Thread.sleep(600)
        history.push(createSnapshot("INSERT", timestamp = 3000))
        
        assertFalse(history.canRedo()) // Redo stack cleared
    }
    
    @Test
    fun `undo on empty stack returns null`() {
        val result = history.undo()
        assertNull(result)
    }
    
    @Test
    fun `redo on empty stack returns null`() {
        val result = history.redo()
        assertNull(result)
    }
    
    @Test
    fun `canUndo requires at least 2 snapshots`() {
        assertFalse(history.canUndo())
        
        history.push(createSnapshot("SELECT", timestamp = 1000))
        assertFalse(history.canUndo()) // Only 1 snapshot
        
        Thread.sleep(600)
        history.push(createSnapshot("SELECT *", timestamp = 2000))
        assertTrue(history.canUndo()) // 2 snapshots
    }
    
    @Test
    fun `clear removes all history`() {
        history.push(createSnapshot("SELECT", timestamp = 1000))
        Thread.sleep(600)
        history.push(createSnapshot("SELECT *", timestamp = 2000))
        
        history.clear()
        
        assertFalse(history.canUndo())
        assertFalse(history.canRedo())
        assertNull(history.current())
    }
    
    @Test
    fun `multi-cursor positions preserved in snapshot`() {
        val snapshot = EditorSnapshot(
            text = "SELECT * FROM users",
            selection = TextRange(0),
            cursorPositions = listOf(10, 15, 20),
            timestamp = 1000
        )
        
        history.push(snapshot)
        
        val current = history.current()
        assertEquals(listOf(10, 15, 20), current?.cursorPositions)
    }
}
