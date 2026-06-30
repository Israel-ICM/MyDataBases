package com.sphynxs.mydatabases.core.database

import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import java.net.URI

/**
 * Parser de connection strings JDBC para diferentes motores de bases de datos.
 *
 * Soporta formatos estándar:
 * - MySQL: jdbc:mysql://host:port/database?params
 * - MariaDB: jdbc:mariadb://host:port/database?params
 * - PostgreSQL: jdbc:postgresql://host:port/database?params
 *
 * También soporta variantes no-JDBC:
 * - mysql://user:pass@host:port/database
 * - postgresql://user:pass@host:port/database
 *
 * @author israel-icm
 * @date 2026-06-30
 */
object ConnectionStringParser {
    
    /**
     * Resultado del parsing de un connection string.
     *
     * @property host Hostname o IP del servidor
     * @property port Puerto del servidor (opcional, usa defaults por tipo)
     * @property database Nombre de la base de datos (opcional)
     * @property username Usuario de autenticación (extraído de user:pass@host o parámetros)
     * @property password Contraseña de autenticación
     * @property parameters Parámetros adicionales del query string
     * @property type Tipo de motor detectado del prefijo
     */
    data class ParsedConnectionString(
        val host: String,
        val port: Int?,
        val database: String?,
        val username: String?,
        val password: String?,
        val parameters: Map<String, String> = emptyMap(),
        val type: DatabaseType?
    )
    
    /**
     * Parsea un connection string a componentes estructurados.
     *
     * @param connectionString String de conexión en cualquier formato soportado
     * @return ParsedConnectionString con los componentes extraídos
     * @throws IllegalArgumentException si el formato no es reconocible
     */
    fun parse(connectionString: String): ParsedConnectionString {
        val trimmed = connectionString.trim()
        
        return when {
            trimmed.startsWith("jdbc:mysql://") -> parseJdbcUri(trimmed, DatabaseType.MYSQL)
            trimmed.startsWith("jdbc:mariadb://") -> parseJdbcUri(trimmed, DatabaseType.MARIADB)
            trimmed.startsWith("jdbc:postgresql://") -> parseJdbcUri(trimmed, DatabaseType.POSTGRESQL)
            trimmed.startsWith("mysql://") -> parseGenericUri(trimmed, DatabaseType.MYSQL)
            trimmed.startsWith("mariadb://") -> parseGenericUri(trimmed, DatabaseType.MARIADB)
            trimmed.startsWith("postgresql://") -> parseGenericUri(trimmed, DatabaseType.POSTGRESQL)
            else -> throw IllegalArgumentException("Connection string no reconocido: debe empezar con jdbc:mysql://, mysql://, etc.")
        }
    }
    
    /**
     * Parsea un JDBC URI estándar: jdbc:mysql://host:port/database?params
     */
    private fun parseJdbcUri(jdbcUri: String, type: DatabaseType): ParsedConnectionString {
        // Remover prefijo jdbc:TYPE://
        val withoutPrefix = jdbcUri.substringAfter("://")
        
        // Separar host:port/database de parámetros
        val parts = withoutPrefix.split("?", limit = 2)
        val hostAndPath = parts[0]
        val queryString = parts.getOrNull(1)
        
        // Parsear parámetros del query string
        val parameters = parseQueryString(queryString)
        
        // Extraer user/pass de parámetros si existen
        val username = parameters["user"]
        val password = parameters["password"]
        
        // Separar host:port de /database
        val pathParts = hostAndPath.split("/", limit = 2)
        val hostAndPort = pathParts[0]
        val database = pathParts.getOrNull(1)?.takeIf { it.isNotBlank() }
        
        // Parsear host:port
        val (host, port) = parseHostAndPort(hostAndPort)
        
        return ParsedConnectionString(
            host = host,
            port = port,
            database = database,
            username = username,
            password = password,
            parameters = parameters,
            type = type
        )
    }
    
    /**
     * Parsea un URI genérico con credenciales: mysql://user:pass@host:port/database
     */
    private fun parseGenericUri(uriString: String, type: DatabaseType): ParsedConnectionString {
        try {
            val uri = URI(uriString)
            
            val userInfo = uri.userInfo
            val (username, password) = if (userInfo != null) {
                val credParts = userInfo.split(":", limit = 2)
                credParts[0] to credParts.getOrNull(1)
            } else {
                null to null
            }
            
            val host = uri.host ?: throw IllegalArgumentException("Host no encontrado en URI")
            val port = if (uri.port > 0) uri.port else null
            
            // Extraer database del path (remover / inicial)
            val database = uri.path
                ?.removePrefix("/")
                ?.takeIf { it.isNotBlank() }
            
            // Parsear query string si existe
            val parameters = parseQueryString(uri.query)
            
            return ParsedConnectionString(
                host = host,
                port = port,
                database = database,
                username = username,
                password = password,
                parameters = parameters,
                type = type
            )
        } catch (e: Exception) {
            throw IllegalArgumentException("Error parseando URI: ${e.message}", e)
        }
    }
    
    /**
     * Parsea host:port o solo host.
     *
     * @return Pair(host, port) donde port puede ser null si no está especificado
     */
    private fun parseHostAndPort(hostAndPort: String): Pair<String, Int?> {
        val parts = hostAndPort.split(":", limit = 2)
        val host = parts[0]
        val port = parts.getOrNull(1)?.toIntOrNull()
        return host to port
    }
    
    /**
     * Parsea query string en mapa de parámetros.
     *
     * Ejemplo: "useSSL=true&user=root" -> {"useSSL": "true", "user": "root"}
     */
    private fun parseQueryString(queryString: String?): Map<String, String> {
        if (queryString.isNullOrBlank()) return emptyMap()
        
        return queryString
            .split("&")
            .mapNotNull { param ->
                val parts = param.split("=", limit = 2)
                if (parts.size == 2) {
                    parts[0] to parts[1]
                } else {
                    null
                }
            }
            .toMap()
    }
    
    /**
     * Obtiene el puerto por defecto según el tipo de motor.
     */
    fun getDefaultPort(type: DatabaseType): Int {
        return when (type) {
            DatabaseType.MYSQL -> 3306
            DatabaseType.MARIADB -> 3306
            DatabaseType.POSTGRESQL -> 5432
            DatabaseType.SQLITE -> throw IllegalArgumentException("SQLite no usa puerto de red")
        }
    }
}
