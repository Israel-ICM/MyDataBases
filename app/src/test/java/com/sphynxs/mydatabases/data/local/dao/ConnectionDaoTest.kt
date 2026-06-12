package com.sphynxs.mydatabases.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.data.local.AppDatabase
import com.sphynxs.mydatabases.data.local.entities.ConnectionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests para [ConnectionDao] — ciclo RED → GREEN → TRIANGULATE.
 *
 * Usa una base de datos Room en memoria para testear operaciones CRUD
 * sin tocar la DB real.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@RunWith(RobolectricTestRunner::class)
class ConnectionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ConnectionDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        dao = database.connectionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * RED: Este test va a fallar porque ConnectionDao y AppDatabase aún no existen.
     *
     * Escenario: Insertar una conexión debe permitir recuperarla por ID.
     */
    @Test
    fun `insert() debe guardar conexión y permitir recuperarla por ID`() = runTest {
        // Given
        val connection = ConnectionEntity(
            id = "test-id",
            name = "Test Connection",
            type = DatabaseType.MYSQL,
            host = "localhost",
            port = 3306,
            database = "testdb",
            username = "root",
            encryptedPassword = "encrypted-key-123",
            useSSL = true,
            sshTunnelConfig = null,
            connectionTimeout = 10000L,
            readTimeout = 30000L,
            maxPoolSize = 10,
            createdAt = System.currentTimeMillis(),
            lastUsedAt = null
        )

        // When
        dao.insert(connection)
        val retrieved = dao.getById("test-id")

        // Then
        assertNotNull("La conexión debe existir después de insertarla", retrieved)
        assertEquals("El ID debe coincidir", connection.id, retrieved?.id)
        assertEquals("El nombre debe coincidir", connection.name, retrieved?.name)
        assertEquals("El password encriptado debe coincidir", connection.encryptedPassword, retrieved?.encryptedPassword)
    }

    /**
     * TRIANGULATE: Segundo caso con delete.
     *
     * Escenario: Después de eliminar una conexión, no debe poder recuperarse.
     */
    @Test
    fun `deleteById() debe eliminar la conexión`() = runTest {
        // Given
        val connection = ConnectionEntity(
            id = "delete-test",
            name = "To Delete",
            type = DatabaseType.MARIADB,
            host = "192.168.1.1",
            port = 3306,
            database = "mariadb",
            username = "admin",
            encryptedPassword = "encrypted-key-456",
            useSSL = false,
            sshTunnelConfig = null,
            connectionTimeout = 5000L,
            readTimeout = 15000L,
            maxPoolSize = 5,
            createdAt = System.currentTimeMillis(),
            lastUsedAt = null
        )
        dao.insert(connection)

        // When
        dao.deleteById("delete-test")
        val retrieved = dao.getById("delete-test")

        // Then
        assertNull("La conexión debe ser null después de eliminarla", retrieved)
    }

    /**
     * TRIANGULATE: Tercer caso con getAll().
     *
     * Escenario: Obtener todas las conexiones debe retornar lista ordenada por lastUsedAt DESC.
     */
    @Test
    fun `getAll() debe retornar todas las conexiones ordenadas por último uso`() = runTest {
        // Given
        val connection1 = ConnectionEntity(
            id = "conn1",
            name = "Old Connection",
            type = DatabaseType.MYSQL,
            host = "old.server",
            port = 3306,
            database = "olddb",
            username = "user1",
            encryptedPassword = "key1",
            useSSL = true,
            sshTunnelConfig = null,
            connectionTimeout = 10000L,
            readTimeout = 30000L,
            maxPoolSize = 10,
            createdAt = 1000L,
            lastUsedAt = 2000L
        )
        val connection2 = ConnectionEntity(
            id = "conn2",
            name = "New Connection",
            type = DatabaseType.MARIADB,
            host = "new.server",
            port = 3306,
            database = "newdb",
            username = "user2",
            encryptedPassword = "key2",
            useSSL = true,
            sshTunnelConfig = null,
            connectionTimeout = 10000L,
            readTimeout = 30000L,
            maxPoolSize = 10,
            createdAt = 3000L,
            lastUsedAt = 5000L
        )

        // When
        dao.insert(connection1)
        dao.insert(connection2)
        val all = dao.getAll().first()

        // Then
        assertEquals("Debe haber 2 conexiones", 2, all.size)
        assertEquals("La primera debe ser la más reciente", "conn2", all[0].id)
        assertEquals("La segunda debe ser la más vieja", "conn1", all[1].id)
    }

    /**
     * TRIANGULATE: Cuarto caso con updateLastUsed().
     *
     * Escenario: Actualizar lastUsedAt debe cambiar el timestamp sin tocar otros campos.
     */
    @Test
    fun `updateLastUsed() debe actualizar solo el timestamp`() = runTest {
        // Given
        val connection = ConnectionEntity(
            id = "update-test",
            name = "Update Test",
            type = DatabaseType.MYSQL,
            host = "localhost",
            port = 3306,
            database = "testdb",
            username = "root",
            encryptedPassword = "key",
            useSSL = true,
            sshTunnelConfig = null,
            connectionTimeout = 10000L,
            readTimeout = 30000L,
            maxPoolSize = 10,
            createdAt = 1000L,
            lastUsedAt = null
        )
        dao.insert(connection)

        // When
        val newTimestamp = 9999L
        dao.updateLastUsed("update-test", newTimestamp)
        val updated = dao.getById("update-test")

        // Then
        assertNotNull("La conexión debe existir", updated)
        assertEquals("El lastUsedAt debe actualizarse", newTimestamp, updated?.lastUsedAt)
        assertEquals("El nombre NO debe cambiar", "Update Test", updated?.name)
    }
}
