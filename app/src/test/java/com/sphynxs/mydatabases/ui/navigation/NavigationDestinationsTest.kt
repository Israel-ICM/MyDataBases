package com.sphynxs.mydatabases.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests para NavigationDestinations — lógica de destinos contextuales.
 *
 * Verifica que `destinationsForContext` devuelve los destinos correctos
 * según el contexto (OutsideConnection vs InsideConnection).
 *
 * @author israel-icm
 * @date 2026-06-15
 */
class NavigationDestinationsTest {
    
    @Test
    fun `destinationsForContext OutsideConnection devuelve 2 destinos`() {
        // GIVEN
        val context = NavigationContext.OutsideConnection
        
        // WHEN
        val destinations = destinationsForContext(context)
        
        // THEN
        assertEquals(2, destinations.size)
    }
    
    @Test
    fun `destinationsForContext OutsideConnection contiene Connections y Settings`() {
        // GIVEN
        val context = NavigationContext.OutsideConnection
        
        // WHEN
        val destinations = destinationsForContext(context)
        
        // THEN
        val ids = destinations.map { it.id }
        assertTrue("Debe contener 'connections'", ids.contains("connections"))
        assertTrue("Debe contener 'settings'", ids.contains("settings"))
    }
    
    @Test
    fun `destinationsForContext InsideConnection devuelve 5 destinos`() {
        // GIVEN
        val context = NavigationContext.InsideConnection("test-id")
        
        // WHEN
        val destinations = destinationsForContext(context)
        
        // THEN
        assertEquals(5, destinations.size)
    }
    
    @Test
    fun `destinationsForContext InsideConnection contiene los 5 destinos esperados`() {
        // GIVEN
        val context = NavigationContext.InsideConnection("test-id")
        
        // WHEN
        val destinations = destinationsForContext(context)
        
        // THEN
        val ids = destinations.map { it.id }
        assertTrue("Debe contener 'tables'", ids.contains("tables"))
        assertTrue("Debe contener 'views'", ids.contains("views"))
        assertTrue("Debe contener 'editor'", ids.contains("editor"))
        assertTrue("Debe contener 'functions'", ids.contains("functions"))
        assertTrue("Debe contener 'backup'", ids.contains("backup"))
    }
    
    @Test
    fun `destinationsForContext InsideConnection interpola connectionId en route`() {
        // GIVEN
        val connectionId = "abc-123"
        val context = NavigationContext.InsideConnection(connectionId)
        
        // WHEN
        val destinations = destinationsForContext(context)
        val tablesDestination = destinations.find { it.id == "tables" }
        
        // THEN
        val expectedRoute = "connection/$connectionId/tables"
        assertEquals(expectedRoute, tablesDestination?.route)
    }
    
    @Test
    fun `destinationsForContext OutsideConnection routes no contienen connectionId`() {
        // GIVEN
        val context = NavigationContext.OutsideConnection
        
        // WHEN
        val destinations = destinationsForContext(context)
        
        // THEN
        val connectionsDestination = destinations.find { it.id == "connections" }
        assertEquals("connections", connectionsDestination?.route)
        
        val settingsDestination = destinations.find { it.id == "settings" }
        assertEquals("settings", settingsDestination?.route)
    }
}
