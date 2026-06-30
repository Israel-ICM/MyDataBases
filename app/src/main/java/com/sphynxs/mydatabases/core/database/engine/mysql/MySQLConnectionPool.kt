package com.sphynxs.mydatabases.core.database.engine.mysql

import android.content.Context
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
     * @return Conexión JDBC válida
     * @throws SQLException si no se puede obtener conexión
     */
    suspend fun getConnection(): Connection = withContext(Dispatchers.IO) {
        // Cargar driver explícitamente (necesario en Android)
        Class.forName("com.mysql.jdbc.Driver")
        
        val connectionProps = Properties().apply {
            put("user", config.username)
            put("password", config.password)
            
            // Configuración SSL avanzada
            if (config.useSSL && config.sslConfig != null) {
                // Usar configuración SSL detallada con certificados
                sslConfigBuilder = MySQLSSLConfigBuilder(context, config.sslConfig)
                sslConfigBuilder?.applyToProperties(this)
            } else if (config.useSSL) {
                // SSL básico sin certificados (backward compatibility)
                put("useSSL", "true")
                put("requireSSL", "true")
                put("verifyServerCertificate", "false")
            } else {
                // Sin SSL
                put("useSSL", "false")
            }
            
            // Timeout básico (milisegundos)
            put("connectTimeout", config.connectionTimeout.toString())
            put("socketTimeout", config.readTimeout.toString())
            
            // Tolerancia con fechas/datos inválidos (igual que Navicat)
            put("zeroDateTimeBehavior", "convertToNull")
            put("jdbcCompliantTruncation", "false")
        }
        
        val databaseSegment = config.database.takeIf { it.isNotBlank() } ?: ""
        val jdbcUrl = "jdbc:mysql://${config.host}:${config.port}/$databaseSegment"
        
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
