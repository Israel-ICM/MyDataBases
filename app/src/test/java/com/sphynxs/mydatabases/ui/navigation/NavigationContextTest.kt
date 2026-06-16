package com.sphynxs.mydatabases.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests para NavigationContext — derivación pura desde route activo.
 *
 * Verifica que el parsing de rutas funciona correctamente para OutsideConnection
 * e InsideConnection con extracción de connectionId.
 *
 * @author israel-icm
 * @date 2026-06-15
 */
class NavigationContextTest {
    
    @Test
    fun `from connections route devuelve OutsideConnection`() {
        // GIVEN
        val route = "connections"
        
        // WHEN
        val context = NavigationContext.from(route)
        
        // THEN
        assertEquals(NavigationContext.OutsideConnection, context)
    }
    
    @Test
    fun `from settings route devuelve OutsideConnection`() {
        // GIVEN
        val route = "settings"
        
        // WHEN
        val context = NavigationContext.from(route)
        
        // THEN
        assertEquals(NavigationContext.OutsideConnection, context)
    }
    
    @Test
    fun `from null route devuelve OutsideConnection`() {
        // GIVEN
        val route: String? = null
        
        // WHEN
        val context = NavigationContext.from(route)
        
        // THEN
        assertEquals(NavigationContext.OutsideConnection, context)
    }
    
    @Test
    fun `from connection tables route devuelve InsideConnection con connectionId`() {
        // GIVEN
        val route = "connection/abc-123/tables"
        
        // WHEN
        val context = NavigationContext.from(route)
        
        // THEN
        assertEquals(NavigationContext.InsideConnection("abc-123"), context)
    }
    
    @Test
    fun `from connection views route devuelve InsideConnection con connectionId`() {
        // GIVEN
        val route = "connection/xyz-789/views"
        
        // WHEN
        val context = NavigationContext.from(route)
        
        // THEN
        assertEquals(NavigationContext.InsideConnection("xyz-789"), context)
    }
    
    @Test
    fun `from connection editor route devuelve InsideConnection con connectionId`() {
        // GIVEN
        val route = "connection/test-id/editor"
        
        // WHEN
        val context = NavigationContext.from(route)
        
        // THEN
        assertEquals(NavigationContext.InsideConnection("test-id"), context)
    }
    
    @Test
    fun `from connection functions route devuelve InsideConnection con connectionId`() {
        // GIVEN
        val route = "connection/my-conn/functions"
        
        // WHEN
        val context = NavigationContext.from(route)
        
        // THEN
        assertEquals(NavigationContext.InsideConnection("my-conn"), context)
    }
    
    @Test
    fun `from connection backup route devuelve InsideConnection con connectionId`() {
        // GIVEN
        val route = "connection/backup-123/backup"
        
        // WHEN
        val context = NavigationContext.from(route)
        
        // THEN
        assertEquals(NavigationContext.InsideConnection("backup-123"), context)
    }
    
    @Test
    fun `from database list route devuelve InsideConnection con connectionId genérico`() {
        // GIVEN
        val route = "database_list"
        
        // WHEN
        val context = NavigationContext.from(route)
        
        // THEN
        // database_list NO tiene connectionId en la ruta actual — decision: tratar como OutsideConnection
        // hasta que las rutas se refactoren para incluir connection/{id}/databases
        assertEquals(NavigationContext.OutsideConnection, context)
    }
}
