package com.sphynxs.mydatabases.core.database.engine.mysql

import com.sphynxs.mydatabases.core.database.models.*
import java.sql.Connection
import java.sql.ResultSet

/**
 * Helper para leer metadata de MySQL usando information_schema.
 * Separado del engine para Single Responsibility Principle.
 * 
 * Métodos puros que transforman ResultSet → domain models.
 * 
 * @author israel-icm
 * @date 2026-06-12
 */
class MySQLMetadataReader {
    
    /**
     * Lee todas las bases de datos del servidor ejecutando la query provista.
     * 
     * @param connection Conexión JDBC activa
     * @param query SQL query que retorna columnas: name, charset, collation
     * @return Lista de Database ordenada alfabéticamente
     */
    fun readDatabases(connection: Connection, query: String): List<Database> {
        val databases = mutableListOf<Database>()
        
        connection.createStatement().use { statement ->
            statement.executeQuery(query).use { resultSet ->
                while (resultSet.next()) {
                    databases.add(
                        Database(
                            name = resultSet.getString("name"),
                            charset = resultSet.getString("charset"),
                            collation = resultSet.getString("collation")
                        )
                    )
                }
            }
        }
        
        return databases
    }
    
    /**
     * Lee todas las tablas de una base de datos específica.
     * 
     * @param connection Conexión JDBC activa
     * @param query SQL query con placeholder `?` para database name
     * @param database Nombre de la base de datos
     * @return Lista de Table con metadata completa
     */
    fun readTables(connection: Connection, query: String, database: String): List<Table> {
        val tables = mutableListOf<Table>()
        
        connection.prepareStatement(query).use { statement ->
            statement.setString(1, database)
            
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    tables.add(
                        Table(
                            name = resultSet.getString("name"),
                            database = database,
                            type = parseTableType(resultSet.getString("type")),
                            engine = resultSet.getString("engine"),
                            rowCount = resultSet.getLong("rowCount"),
                            dataLength = resultSet.getLong("dataLength"),
                            createdAt = resultSet.getLongOrNull("createdAt"),
                            comment = resultSet.getString("comment")
                        )
                    )
                }
            }
        }
        
        return tables
    }
    
    /**
     * Lee todas las columnas de una tabla con metadata completa.
     * 
     * @param connection Conexión JDBC activa
     * @param query SQL query con placeholder `?` para table name
     * @param table Nombre de la tabla
     * @return Lista de Column ordenada por posición
     */
    fun readColumns(connection: Connection, query: String, table: String): List<Column> {
        val columns = mutableListOf<Column>()
        
        connection.prepareStatement(query).use { statement ->
            statement.setString(1, table)
            
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    columns.add(
                        Column(
                            name = resultSet.getString("name"),
                            type = resultSet.getString("type"),
                            nullable = resultSet.getString("nullable") == "YES",
                            key = parseColumnKey(resultSet.getString("key")),
                            default = resultSet.getString("default_value"),
                            extra = resultSet.getString("extra"),
                            comment = resultSet.getString("comment")
                        )
                    )
                }
            }
        }
        
        return columns
    }
    
    /**
     * Lee todos los índices de una tabla.
     * Agrupa columnas de índices compuestos.
     * 
     * @param connection Conexión JDBC activa
     * @param query SQL query con placeholder `?` para table name
     * @param table Nombre de la tabla
     * @return Lista de Index (índices compuestos agrupados)
     */
    fun readIndexes(connection: Connection, query: String, table: String): List<Index> {
        val indexMap = mutableMapOf<String, MutableList<String>>()
        val indexMetadata = mutableMapOf<String, Pair<Boolean, String>>()
        
        connection.prepareStatement(query).use { statement ->
            statement.setString(1, table)
            
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    val indexName = resultSet.getString("name")
                    val columnName = resultSet.getString("column")
                    val nonUnique = resultSet.getInt("nonUnique") == 1
                    val indexType = resultSet.getString("type")
                    
                    indexMap.getOrPut(indexName) { mutableListOf() }.add(columnName)
                    indexMetadata[indexName] = Pair(!nonUnique, indexType)
                }
            }
        }
        
        return indexMap.map { (name, columns) ->
            val (unique, type) = indexMetadata[name]!!
            Index(
                name = name,
                columns = columns,
                unique = unique,
                type = parseIndexType(type)
            )
        }
    }
    
    /**
     * Lee todas las foreign keys de una tabla.
     * 
     * @param connection Conexión JDBC activa
     * @param query SQL query con placeholder `?` para table name
     * @param table Nombre de la tabla
     * @return Lista de ForeignKey con acciones referenciadas
     */
    fun readForeignKeys(connection: Connection, query: String, table: String): List<ForeignKey> {
        val foreignKeys = mutableListOf<ForeignKey>()
        
        connection.prepareStatement(query).use { statement ->
            statement.setString(1, table)
            
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    foreignKeys.add(
                        ForeignKey(
                            name = resultSet.getString("name"),
                            column = resultSet.getString("column"),
                            referencedTable = resultSet.getString("referencedTable"),
                            referencedColumn = resultSet.getString("referencedColumn"),
                            onDelete = parseReferentialAction(resultSet.getString("onDelete")),
                            onUpdate = parseReferentialAction(resultSet.getString("onUpdate"))
                        )
                    )
                }
            }
        }
        
        return foreignKeys
    }
    
    // ========== Funciones de parseo puras ==========
    
    /**
     * Convierte string de tipo de tabla a enum TableType.
     * 
     * @param type Valor de TABLE_TYPE de information_schema
     * @return TableType correspondiente
     */
    private fun parseTableType(type: String): TableType {
        return when (type.uppercase()) {
            "BASE TABLE" -> TableType.TABLE
            "VIEW" -> TableType.VIEW
            "SYSTEM VIEW" -> TableType.SYSTEM_TABLE
            else -> TableType.TABLE
        }
    }
    
    /**
     * Convierte string de column key a enum ColumnKey.
     * 
     * @param key Valor de COLUMN_KEY de information_schema (PRI, UNI, MUL, null)
     * @return ColumnKey correspondiente
     */
    private fun parseColumnKey(key: String?): ColumnKey {
        return when (key?.uppercase()) {
            "PRI" -> ColumnKey.PRIMARY
            "UNI" -> ColumnKey.UNIQUE
            "MUL" -> ColumnKey.MULTIPLE
            else -> ColumnKey.NONE
        }
    }
    
    /**
     * Convierte string de tipo de índice a enum IndexType.
     * 
     * @param type Valor de INDEX_TYPE de information_schema
     * @return IndexType correspondiente
     */
    private fun parseIndexType(type: String): IndexType {
        return when (type.uppercase()) {
            "BTREE" -> IndexType.BTREE
            "HASH" -> IndexType.HASH
            "FULLTEXT" -> IndexType.FULLTEXT
            "SPATIAL" -> IndexType.SPATIAL
            else -> IndexType.BTREE
        }
    }
    
    /**
     * Convierte string de acción referencial a enum ReferentialAction.
     * 
     * @param action Valor de DELETE_RULE o UPDATE_RULE de information_schema
     * @return ReferentialAction correspondiente
     */
    private fun parseReferentialAction(action: String): ReferentialAction {
        return when (action.uppercase()) {
            "CASCADE" -> ReferentialAction.CASCADE
            "SET NULL" -> ReferentialAction.SET_NULL
            "RESTRICT" -> ReferentialAction.RESTRICT
            "NO ACTION" -> ReferentialAction.NO_ACTION
            else -> ReferentialAction.NO_ACTION
        }
    }
    
    /**
     * Extension function para manejar NULL values en Long.
     * 
     * @param columnLabel Nombre de la columna
     * @return Long o null si el valor es SQL NULL
     */
    private fun ResultSet.getLongOrNull(columnLabel: String): Long? {
        return try {
            val value = getLong(columnLabel)
            if (wasNull()) null else value
        } catch (e: Exception) {
            null
        }
    }
}
