package com.sphynxs.mydatabases.core.database.engine.mysql

import android.content.Context
import com.sphynxs.mydatabases.core.database.ConnectionStringParser
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.core.database.ssh.SSHTunnelManager
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
 * - SSH tunneling para conexiones a través de bastion hosts
 * - Timeouts básicos de socket
 * - Sin pooling (una conexión por request)
 * 
 * @param config Configuración de conexión con credenciales
 * @param context Contexto de Android para leer certificados y claves SSH
 * @author israel-icm
 * @date 2026-06-15 (updated 2026-06-30 for SSL certificates and SSH tunneling)
 */
class MySQLConnectionPool(
    private val config: ConnectionConfig,
    private val context: Context
) {
    
    private var activeConnection: Connection? = null
    private var sslConfigBuilder: MySQLSSLConfigBuilder? = null
    private var sshTunnelManager: SSHTunnelManager? = null
    
    /**
     * Obtiene una conexión directa usando DriverManager.
     * Copia la receta probada de MyDataBasesDeprecated con soporte SSL mejorado y SSH tunneling.
     * 
     * Flujo de conexión:
     * 1. Si SSH tunnel habilitado → establecer túnel primero
     * 2. Connection string (si está presente, sobreescribe host/port/user/pass)
     * 3. Aplicar configuración SSL (certificados)
     * 4. Establecer conexión JDBC
     * 
     * Prioridad de host/port:
     * 1. SSH tunnel (localhost:localPort) si está activo
     * 2. Connection string si está presente
     * 3. Valores individuales de config
     * 
     * @return Conexión JDBC válida
     * @throws SQLException si no se puede obtener conexión
     * @throws SSHTunnelException si SSH tunnel falla
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
        
        // Establecer SSH tunnel si está configurado (ANTES de JDBC connection)
        val (jdbcHost, jdbcPort) = if (shouldUseSSHTunnel()) {
            establishSSHTunnel(effectiveConfig.host, effectiveConfig.port)
        } else {
            effectiveConfig.host to effectiveConfig.port
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
        val jdbcUrl = "jdbc:mysql://${jdbcHost}:${jdbcPort}/$databaseSegment"
        
        try {
            // DriverManager directo - la receta probada
            val connection = DriverManager.getConnection(jdbcUrl, connectionProps)
            activeConnection = connection
            connection
        } catch (e: Exception) {
            // Limpiar recursos si falló la conexión
            sslConfigBuilder?.cleanup()
            sshTunnelManager?.disconnect()
            throw e
        }
    }
    
    /**
     * Determina si se debe usar SSH tunnel para esta conexión.
     * 
     * SSH tunnel NO se usa si:
     * - No está habilitado en config
     * - Se está usando connection string (asumimos que el string ya maneja la conexión)
     */
    private fun shouldUseSSHTunnel(): Boolean {
        return config.sshTunnelConfig?.enabled == true &&
               config.connectionString.isNullOrBlank()
    }
    
    /**
     * Establece SSH tunnel y retorna el host/port efectivo para JDBC.
     * 
     * @param databaseHost Host del servidor de base de datos (destino del túnel)
     * @param databasePort Puerto del servidor de base de datos
     * @return Pair(host, port) para usar en JDBC URL (localhost:localPort)
     * @throws SSHTunnelException si el túnel no se puede establecer
     */
    private fun establishSSHTunnel(databaseHost: String, databasePort: Int): Pair<String, Int> {
        val sshConfig = config.sshTunnelConfig
            ?: throw IllegalStateException("SSH tunnel config is null but shouldUseSSHTunnel returned true")
        
        try {
            sshTunnelManager = SSHTunnelManager(sshConfig, context)
            val localPort = sshTunnelManager!!.connect(
                remoteHost = databaseHost,
                remotePort = databasePort
            )
            
            // Retornar localhost:localPort para JDBC connection
            return "127.0.0.1" to localPort
            
        } catch (e: Exception) {
            // Cleanup tunnel manager on failure
            sshTunnelManager?.disconnect()
            sshTunnelManager = null
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
     * 
     * Limpia recursos en orden:
     * 1. Conexión JDBC
     * 2. Certificados SSL temporales
     * 3. Túnel SSH
     */
    fun close() {
        activeConnection?.let {
            if (!it.isClosed) {
                it.close()
            }
        }
        activeConnection = null
        
        // Limpiar certificados temporales SSL
        sslConfigBuilder?.cleanup()
        sslConfigBuilder = null
        
        // Limpiar túnel SSH
        sshTunnelManager?.disconnect()
        sshTunnelManager = null
    }
}
