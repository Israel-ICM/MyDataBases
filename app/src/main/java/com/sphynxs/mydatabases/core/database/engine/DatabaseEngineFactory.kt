package com.sphynxs.mydatabases.core.database.engine

import com.sphynxs.mydatabases.core.database.engine.mariadb.MariaDBEngine
import com.sphynxs.mydatabases.core.database.engine.mysql.MySQLEngine

/**
 * Factory para crear instancias de DatabaseEngine según el tipo de motor.
 * 
 * Patrón: Factory Method
 * Propósito: Centralizar la creación de engines y facilitar testing (mockeable).
 * 
 * Implementaciones disponibles en v1.0:
 * - MySQL 5.7+, 8.0+
 * - MariaDB 10.5+
 * 
 * Pendientes para v1.1:
 * - PostgreSQL
 * - SQLite
 * 
 * @author israel-icm
 * @date 2026-06-12
 */
object DatabaseEngineFactory {
    
    /**
     * Crea una instancia de DatabaseEngine según el tipo especificado.
     * 
     * @param type Tipo de motor (MYSQL, MARIADB, POSTGRESQL, SQLITE)
     * @return Instancia concreta de DatabaseEngine
     * @throws NotImplementedError si el tipo no está implementado todavía
     * 
     * @see DatabaseType
     * @see DatabaseEngine
     */
    fun create(type: DatabaseType): DatabaseEngine {
        return when (type) {
            DatabaseType.MYSQL -> MySQLEngine()
            DatabaseType.MARIADB -> MariaDBEngine()
            DatabaseType.POSTGRESQL -> throw NotImplementedError("PostgreSQL será implementado en v1.1")
            DatabaseType.SQLITE -> throw NotImplementedError("SQLite será implementado en v1.1")
        }
    }
}
