package com.sphynxs.mydatabases.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests para la jerarquía de rutas de navegación.
 *
 * Verifica que las rutas sealed class generen los paths correctos para navegación.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
class RouteTest {
    
    /**
     * Verifica que la ruta Connections genere el path correcto.
     */
    @Test
    fun `route Connections debe tener path connections`() {
        assertEquals("connections", Routes.Connections.route)
    }
    
    /**
     * Verifica que la ruta DatabaseList genere el path correcto.
     */
    @Test
    fun `route DatabaseList debe tener path database_list`() {
        assertEquals("database_list", Routes.DatabaseList.route)
    }
    
    /**
     * Verifica que la ruta TableList genere el path correcto con argumento.
     */
    @Test
    fun `route TableList debe tener path table_list con argumento databaseName`() {
        assertEquals("table_list/{databaseName}", Routes.TableList.route)
    }
    
    /**
     * Verifica que TableList.createRoute genere la ruta con el argumento reemplazado.
     */
    @Test
    fun `route TableList createRoute debe reemplazar argumento databaseName`() {
        assertEquals("table_list/test_db", Routes.TableList.createRoute("test_db"))
    }
    
    /**
     * Verifica que la ruta TableViewer genere el path correcto con argumentos.
     */
    @Test
    fun `route TableViewer debe tener path table_viewer con argumentos`() {
        assertEquals("table_viewer/{databaseName}/{tableName}", Routes.TableViewer.route)
    }
    
    /**
     * Verifica que TableViewer.createRoute genere la ruta con argumentos reemplazados.
     */
    @Test
    fun `route TableViewer createRoute debe reemplazar argumentos`() {
        assertEquals("table_viewer/test_db/users", Routes.TableViewer.createRoute("test_db", "users"))
    }
    
    /**
     * Verifica que la ruta QueryEditor genere el path correcto.
     */
    @Test
    fun `route QueryEditor debe tener path query_editor`() {
        assertEquals("query_editor", Routes.QueryEditor.route)
    }
    
    /**
     * Verifica que la ruta Settings genere el path correcto.
     */
    @Test
    fun `route Settings debe tener path settings`() {
        assertEquals("settings", Routes.Settings.route)
    }
}
