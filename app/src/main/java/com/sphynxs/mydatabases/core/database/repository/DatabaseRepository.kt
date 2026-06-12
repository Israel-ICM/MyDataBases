package com.sphynxs.mydatabases.core.database.repository

import com.sphynxs.mydatabases.core.database.engine.DatabaseFeature
import com.sphynxs.mydatabases.core.database.models.*

/**
 * Repository que abstrae el acceso a DatabaseEngine.
 * 
 * Permite cambiar la implementación del motor de base de datos sin afectar los UseCases.
 * 
 * @author israel-icm
 * @date 2026-06-12
 */
interface DatabaseRepository {
    
    /**
     * Conecta a una base de datos usando la configuración provista.
     * 
     * @param config Configuración de conexión (tipo, host, port, credenciales)
     * @return Result con Connection si exitoso, DatabaseError si falla
     */
    suspend fun connect(config: ConnectionConfig): Result<Connection>
    
    /**
     * Desconecta de la base de datos actual y libera recursos.
     * 
     * @return Result con Unit si exitoso, DatabaseError si falla
     */
    suspend fun disconnect(): Result<Unit>
    
    /**
     * Ejecuta una query SELECT y retorna los resultados.
     * 
     * @param query SQL query con placeholders (?)
     * @param params Parámetros para prepared statement
     * @return Result con QueryResult conteniendo columnas y rows
     * @throws DatabaseError.ConnectionFailed si no hay motor conectado
     */
    suspend fun executeQuery(query: String, params: List<Any> = emptyList()): Result<QueryResult>
    
    /**
     * Ejecuta una query INSERT/UPDATE/DELETE.
     * 
     * @param query SQL query con placeholders (?)
     * @param params Parámetros para prepared statement
     * @return Result con número de filas afectadas
     * @throws DatabaseError.ConnectionFailed si no hay motor conectado
     */
    suspend fun executeUpdate(query: String, params: List<Any> = emptyList()): Result<Int>
    
    /**
     * Lista todas las bases de datos disponibles en el servidor.
     * 
     * @return Result con lista de Database ordenada alfabéticamente
     * @throws DatabaseError.ConnectionFailed si no hay motor conectado
     */
    suspend fun getDatabases(): Result<List<Database>>
    
    /**
     * Lista todas las tablas de una base de datos específica.
     * 
     * @param database Nombre de la base de datos
     * @return Result con lista de Table ordenada por nombre
     * @throws DatabaseError.ConnectionFailed si no hay motor conectado
     */
    suspend fun getTables(database: String): Result<List<Table>>
    
    /**
     * Lista todas las columnas de una tabla con metadata completa.
     * 
     * @param table Nombre de la tabla
     * @return Result con lista de Column ordenada por posición
     * @throws DatabaseError.ConnectionFailed si no hay motor conectado
     */
    suspend fun getColumns(table: String): Result<List<Column>>
    
    /**
     * Lista todos los índices de una tabla.
     * 
     * @param table Nombre de la tabla
     * @return Result con lista de Index
     * @throws DatabaseError.ConnectionFailed si no hay motor conectado
     */
    suspend fun getIndexes(table: String): Result<List<Index>>
    
    /**
     * Lista todas las foreign keys de una tabla.
     * 
     * @param table Nombre de la tabla
     * @return Result con lista de ForeignKey
     * @throws DatabaseError.ConnectionFailed si no hay motor conectado
     */
    suspend fun getForeignKeys(table: String): Result<List<ForeignKey>>
    
    /**
     * Inicia una transacción.
     * 
     * @return Result con Transaction para hacer commit/rollback
     * @throws DatabaseError.ConnectionFailed si no hay motor conectado
     */
    suspend fun beginTransaction(): Result<Transaction>
    
    /**
     * Obtiene la versión del motor de base de datos conectado.
     * 
     * @return Result con string de versión
     * @throws DatabaseError.ConnectionFailed si no hay motor conectado
     */
    suspend fun getVersion(): Result<String>
    
    /**
     * Retorna el conjunto de features soportadas por el motor actual.
     * 
     * @return Set de DatabaseFeature (vacío si no hay motor conectado)
     */
    fun getSupportedFeatures(): Set<DatabaseFeature>
}
