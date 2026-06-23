package com.sphynxs.mydatabases.core.database.engine

import com.sphynxs.mydatabases.core.database.models.*

/**
 * Interface que define las operaciones comunes para todos los motores de bases de datos.
 *
 * Implementaciones concretas: MySQLEngine, MariaDBEngine, PostgreSQLEngine, SQLiteEngine.
 *
 * @author israel-icm
 * @date 2026-06-11
 */
interface DatabaseEngine {

    /**
     * Conecta al servidor de base de datos usando la configuración provista.
     *
     * @param config Configuración de conexión (host, port, credentials, etc.)
     * @return Result con Connection si exitoso, DatabaseError si falla
     * @throws DatabaseError.ConnectionFailed si el host no es alcanzable
     * @throws DatabaseError.AuthenticationFailed si las credenciales son inválidas
     * @throws DatabaseError.TimeoutError si excede el timeout configurado
     */
    suspend fun connect(config: ConnectionConfig): Result<Connection>

    /**
     * Desconecta del servidor y libera todos los recursos (conexiones pool).
     *
     * @return Result con Unit si exitoso, DatabaseError si falla
     */
    suspend fun disconnect(): Result<Unit>

    /**
     * Ejecuta una query SELECT y retorna los resultados.
     *
     * @param query SQL query con placeholders (?) para prepared statements
     * @param params Parámetros para reemplazar los placeholders
     * @return Result con QueryResult conteniendo columnas y rows
     * @throws DatabaseError.QueryExecutionFailed si la query tiene errores de sintaxis
     * @throws DatabaseError.TimeoutError si excede el read timeout
     */
    suspend fun executeQuery(
        query: String,
        params: List<Any> = emptyList()
    ): Result<QueryResult>

    /**
     * Ejecuta una query INSERT/UPDATE/DELETE y retorna el número de filas afectadas.
     *
     * @param query SQL query con placeholders (?)
     * @param params Parámetros para prepared statement
     * @return Result con número de filas afectadas
     */
    suspend fun executeUpdate(
        query: String,
        params: List<Any> = emptyList()
    ): Result<Int>

    /**
     * Ejecuta múltiples statements SQL en la MISMA conexión.
     *
     * Permite que statements como USE DATABASE afecten a los siguientes.
     * Cada statement se ejecuta en secuencia usando la misma conexión del pool.
     *
     * @param statements Lista de SQL statements a ejecutar
     * @return Result con lista de BatchStatementResult
     */
    suspend fun executeBatch(
        statements: List<String>
    ): Result<List<com.sphynxs.mydatabases.domain.usecases.BatchStatementResult>>

    /**
     * Lista todas las bases de datos disponibles en el servidor.
     *
     * Excluye system databases (information_schema, mysql, performance_schema, sys).
     *
     * @return Result con lista de Database ordenada alfabéticamente
     */
    suspend fun getDatabases(): Result<List<Database>>

    /**
     * Lista todas las tablas y vistas de una base de datos específica.
     *
     * @param database Nombre de la base de datos
     * @return Result con lista de Table ordenada por nombre
     */
    suspend fun getTables(database: String): Result<List<Table>>

    /**
     * Lista todas las columnas de una tabla con metadata completa.
     *
     * @param table Nombre de la tabla (ej: "users" o "mydb.users")
     * @return Result con lista de Column ordenada por posición
     */
    suspend fun getColumns(table: String): Result<List<Column>>

    /**
     * Lista todos los índices de una tabla.
     *
     * @param table Nombre de la tabla
     * @return Result con lista de Index
     */
    suspend fun getIndexes(table: String): Result<List<Index>>

    /**
     * Lista todas las foreign keys de una tabla.
     *
     * @param table Nombre de la tabla
     * @return Result con lista de ForeignKey
     */
    suspend fun getForeignKeys(table: String): Result<List<ForeignKey>>

    /**
     * Inicia una transacción (deshabilita auto-commit).
     *
     * @return Result con Transaction para hacer commit/rollback
     * @throws DatabaseError.UnsupportedFeature si el motor no soporta transacciones
     */
    suspend fun beginTransaction(): Result<Transaction>

    /**
     * Retorna el conjunto de features soportadas por este motor.
     *
     * Útil para habilitar/deshabilitar funcionalidad en la UI.
     *
     * @return Set de DatabaseFeature
     */
    fun getSupportedFeatures(): Set<DatabaseFeature>

    /**
     * Obtiene la versión del motor de base de datos.
     *
     * @return Result con string de versión (ej: "8.0.33", "10.11.2-MariaDB")
     */
    suspend fun getVersion(): Result<String>
}
