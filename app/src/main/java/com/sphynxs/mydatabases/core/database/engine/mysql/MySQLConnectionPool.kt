package com.sphynxs.mydatabases.core.database.engine.mysql

import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection

/**
 * Connection pool para MySQL usando HikariCP.
 * 
 * Características:
 * - Pool de conexiones reutilizables (máx configurable)
 * - Soporte SSL/TLS
 * - Optimizaciones de performance (prepared statements cache, server config cache)
 * - Timeouts configurables
 * 
 * @param config Configuración de conexión con credenciales y parámetros de pool
 * @author israel-icm
 * @date 2026-06-12
 */
class MySQLConnectionPool(private val config: ConnectionConfig) {
    
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = "jdbc:mysql://${config.host}:${config.port}/${config.database}"
        username = config.username
        password = config.password // TODO: Decrypt from Android Keystore
        
        // Pool settings
        maximumPoolSize = config.maxPoolSize
        minimumIdle = 2
        connectionTimeout = config.connectionTimeout
        idleTimeout = 600_000L // 10 minutos
        maxLifetime = 1_800_000L // 30 minutos
        
        // SSL settings
        if (config.useSSL) {
            addDataSourceProperty("useSSL", "true")
            addDataSourceProperty("requireSSL", "true")
        } else {
            addDataSourceProperty("useSSL", "false")
        }
        
        // Performance settings
        addDataSourceProperty("cachePrepStmts", "true")
        addDataSourceProperty("prepStmtCacheSize", "250")
        addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        addDataSourceProperty("useServerPrepStmts", "true")
        addDataSourceProperty("useLocalSessionState", "true")
        addDataSourceProperty("rewriteBatchedStatements", "true")
        addDataSourceProperty("cacheResultSetMetadata", "true")
        addDataSourceProperty("cacheServerConfiguration", "true")
        addDataSourceProperty("elideSetAutoCommits", "true")
        addDataSourceProperty("maintainTimeStats", "false")
    }
    
    private val dataSource = HikariDataSource(hikariConfig)
    
    /**
     * Obtiene una conexión del pool.
     * HikariCP maneja automáticamente el pooling y timeout.
     * 
     * @return Conexión JDBC válida
     * @throws SQLException si no se puede obtener conexión
     */
    suspend fun getConnection(): Connection = withContext(Dispatchers.IO) {
        dataSource.connection
    }
    
    /**
     * Cierra el pool y libera todas las conexiones.
     * Debe llamarse al desconectar para evitar leaks.
     */
    fun close() {
        if (!dataSource.isClosed) {
            dataSource.close()
        }
    }
}
