package com.sphynxs.mydatabases.core.database.engine.mysql

import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

/**
 * Connection manager para MySQL usando DriverManager directo.
 * 
 * Esta implementación copia el enfoque probado de MyDataBasesDeprecated:
 * - Sin HikariCP (evita incompatibilidades JVM/server en Android)
 * - DriverManager.getConnection() directo con Properties
 * - Driver viejo mysql-connector-java:5.1.46 (único compatible con Android)
 * 
 * Características:
 * - Soporte SSL/TLS opcional
 * - Timeouts básicos de socket
 * - Sin pooling (una conexión por request)
 * 
 * @param config Configuración de conexión con credenciales
 * @author israel-icm
 * @date 2026-06-15
 */
class MySQLConnectionPool(private val config: ConnectionConfig) {
    
    private var activeConnection: Connection? = null
    
    /**
     * Obtiene una conexión directa usando DriverManager.
     * Copia la receta probada de MyDataBasesDeprecated.
     * 
     * @return Conexión JDBC válida
     * @throws SQLException si no se puede obtener conexión
     */
    suspend fun getConnection(): Connection = withContext(Dispatchers.IO) {
        // Cargar driver explícitamente (necesario en Android)
        Class.forName("com.mysql.jdbc.Driver")
        
        val connectionProps = Properties().apply {
            put("user", config.username)
            put("password", config.password)
            
            // SSL settings
            if (config.useSSL) {
                put("useSSL", "true")
                put("requireSSL", "true")
            } else {
                put("useSSL", "false")
            }
            
            // Timeout básico (milisegundos)
            put("connectTimeout", config.connectionTimeout.toString())
        }
        
        val databaseSegment = config.database.takeIf { it.isNotBlank() } ?: ""
        val jdbcUrl = "jdbc:mysql://${config.host}:${config.port}/$databaseSegment"
        
        // DriverManager directo - la receta probada
        val connection = DriverManager.getConnection(jdbcUrl, connectionProps)
        activeConnection = connection
        connection
    }
    
    /**
     * Cierra la conexión activa si existe.
     * Debe llamarse al desconectar para evitar leaks.
     */
    fun close() {
        activeConnection?.let {
            if (!it.isClosed) {
                it.close()
            }
        }
        activeConnection = null
    }
}
