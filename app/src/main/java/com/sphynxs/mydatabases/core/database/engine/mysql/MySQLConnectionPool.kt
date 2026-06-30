package com.sphynxs.mydatabases.core.database.engine.mysql

import android.content.Context
import com.sphynxs.mydatabases.core.database.ConnectionStringParser
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.core.database.ssl.MySQLSSLConfigBuilder
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
 * - Soporte SSL/TLS con certificados personalizados
 * - Autenticación mutua (mTLS) opcional
 * - Timeouts básicos de socket
 * - Sin pooling (una conexión por request)
 * 
 * @param config Configuración de conexión con credenciales
 * @param context Contexto de Android para leer certificados
 * @author israel-icm
 * @date 2026-06-15 (updated 2026-06-30 for SSL certificates)
 */
class MySQLConnectionPool(
    private val config: ConnectionConfig,
    private val context: Context
) {
    
    private var activeConnection: Connection? = null
    private var sslConfigBuilder: MySQLSSLConfigBuilder? = null
    
    /**
     * Obtiene una conexión directa usando DriverManager.
     * Copia la receta probada de MyDataBasesDeprecated con soporte SSL mejorado.
     * 
     * Prioridad de configuración:
     * 1. Connection string (si está presente, sobreescribe host/port/user/pass)
     * 2. Valores individuales de config (host, port, username, password)
     * 
     * @return Conexión JDBC válida
     * @throws SQLException si no se puede obtener conexión
     */
    suspend fun getConnection(): Connection = withContext(Dispatchers.IO) {
        // Cargar driver explícitamente (necesario en Android)
        Class.forName("com.mysql.jdbc.Driver")
        
        // Si hay connection string, parsearla y sobreescribir config
        val effectiveConfig = if (!config.connectionString.isNullOrBlank()) {
            parseConnectionString(config.connectionString)
        } else {
            // Usar config directa
            ConnectionData(
                host = config.host,
                port = config.port,
                database = config.database,
                username = config.username,
                password = config.password,
                parameters = emptyMap()
            )
        }
        
        val connectionProps = Properties().apply {
            put("user", effectiveConfig.username)
            put("password", effectiveConfig.password)
            
            // Aplicar parámetros del connection string si existen
            effectiveConfig.parameters.forEach { (key, value) ->
                put(key, value)
            }
            
            // Configuración SSL avanzada (solo si no viene del connection string)
            if (config.useSSL && config.sslConfig != null) {
                // Usar configuración SSL detallada con certificados
                sslConfigBuilder = MySQLSSLConfigBuilder(context, config.sslConfig)
                sslConfigBuilder?.applyToProperties(this)
            } else if (config.useSSL) {
                // SSL básico sin certificados (backward compatibility)
                put("useSSL", "true")
                put("requireSSL", "true")
                put("verifyServerCertificate", "false")
            } else if (!containsKey("useSSL")) {
                // Sin SSL (solo si no viene en connection string)
                put("useSSL", "false")
            }
            
            // Timeout básico (milisegundos)
            if (!containsKey("connectTimeout")) {
                put("connectTimeout", config.connectionTimeout.toString())
            }
            if (!containsKey("socketTimeout")) {
                put("socketTimeout", config.readTimeout.toString())
            }
            
            // Tolerancia con fechas/datos inválidos (igual que Navicat)
            if (!containsKey("zeroDateTimeBehavior")) {
                put("zeroDateTimeBehavior", "convertToNull")
            }
            if (!containsKey("jdbcCompliantTruncation")) {
                put("jdbcCompliantTruncation", "false")
            }
        }
        
        val databaseSegment = effectiveConfig.database.takeIf { it.isNotBlank() } ?: ""
        val jdbcUrl = "jdbc:mysql://${effectiveConfig.host}:${effectiveConfig.port}/$databaseSegment"
        
        try {
            // DriverManager directo - la receta probada
            val connection = DriverManager.getConnection(jdbcUrl, connectionProps)
            activeConnection = connection
            connection
        } catch (e: Exception) {
            // Limpiar certificados temporales si falló la conexión
            sslConfigBuilder?.cleanup()
            throw e
        }
    }
    
    /**
     * Parsea un connection string y retorna los componentes.
     */
    private fun parseConnectionString(connectionString: String): ConnectionData {
        val parsed = ConnectionStringParser.parse(connectionString)
        return ConnectionData(
            host = parsed.host,
            port = parsed.port ?: config.port,
            database = parsed.database ?: config.database,
            username = parsed.username ?: config.username,
            password = parsed.password ?: config.password,
            parameters = parsed.parameters
        )
    }
    
    /**
     * Data class interna para almacenar datos efectivos de conexión.
     */
    private data class ConnectionData(
        val host: String,
        val port: Int,
        val database: String,
        val username: String,
        val password: String,
        val parameters: Map<String, String>
    )
    
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
        
        // Limpiar certificados temporales
        sslConfigBuilder?.cleanup()
        sslConfigBuilder = null
    }
}
