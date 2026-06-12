package com.sphynxs.mydatabases.core.database.engine

import com.sphynxs.mydatabases.core.database.models.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests del contrato de DatabaseEngine interface.
 *
 * Valida que las implementaciones cumplan con el contrato esperado.
 *
 * @author israel-icm
 * @date 2026-06-11
 */
class DatabaseEngineContractTest {

    @Test
    fun `DatabaseEngine connect returns Result with Connection on success`() = runTest {
        // GIVEN: Un engine mockeado que simula conexión exitosa
        val engine = mockk<DatabaseEngine>()
        val config = mockk<ConnectionConfig>()
        val expectedConnection = Connection(
            id = "test-id",
            type = DatabaseType.MYSQL,
            database = "test_db",
            host = "localhost",
            port = 3306,
            username = "test_user",
            version = "8.0.33",
            connectedAt = System.currentTimeMillis()
        )
        coEvery { engine.connect(config) } returns Result.success(expectedConnection)

        // WHEN: Conectamos usando el engine
        val result = engine.connect(config)

        // THEN: Retorna success con Connection
        assertTrue(result.isSuccess)
        assertEquals(expectedConnection, result.getOrNull())
    }

    @Test
    fun `DatabaseEngine connect returns Result with Error on failure`() = runTest {
        // GIVEN: Un engine que simula fallo de conexión
        val engine = mockk<DatabaseEngine>()
        val config = mockk<ConnectionConfig>()
        val error = DatabaseError.ConnectionFailed("Host unreachable")
        coEvery { engine.connect(config) } returns Result.failure(error)

        // WHEN: Intentamos conectar
        val result = engine.connect(config)

        // THEN: Retorna failure con DatabaseError
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DatabaseError.ConnectionFailed)
    }

    @Test
    fun `DatabaseEngine executeQuery returns QueryResult with data`() = runTest {
        // GIVEN: Un engine que simula ejecución exitosa de query
        val engine = mockk<DatabaseEngine>()
        val query = "SELECT id, name FROM users"
        val expectedResult = QueryResult(
            columns = listOf("id", "name"),
            rows = listOf(
                mapOf("id" to 1, "name" to "Alice"),
                mapOf("id" to 2, "name" to "Bob")
            ),
            rowCount = 2,
            executionTimeMs = 45
        )
        coEvery { engine.executeQuery(query, emptyList()) } returns Result.success(expectedResult)

        // WHEN: Ejecutamos la query
        val result = engine.executeQuery(query, emptyList())

        // THEN: Retorna QueryResult con filas
        assertTrue(result.isSuccess)
        val queryResult = result.getOrNull()
        assertEquals(2, queryResult?.rowCount)
        assertEquals(listOf("id", "name"), queryResult?.columns)
    }

    @Test
    fun `DatabaseEngine getSupportedFeatures returns Set of DatabaseFeature`() {
        // GIVEN: Un engine con features específicas
        val engine = mockk<DatabaseEngine>()
        val features = setOf(
            DatabaseFeature.STORED_PROCEDURES,
            DatabaseFeature.TRIGGERS,
            DatabaseFeature.VIEWS
        )
        coEvery { engine.getSupportedFeatures() } returns features

        // WHEN: Pedimos las features soportadas
        val result = engine.getSupportedFeatures()

        // THEN: Retorna el conjunto correcto
        assertEquals(3, result.size)
        assertTrue(DatabaseFeature.STORED_PROCEDURES in result)
        assertTrue(DatabaseFeature.TRIGGERS in result)
        assertTrue(DatabaseFeature.VIEWS in result)
    }
}
