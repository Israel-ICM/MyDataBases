package com.sphynxs.mydatabases.core.database.engine

import android.content.Context
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
 * @date 2026-06-12 (updated 2026-06-30 for Context injection)
 */
object DatabaseEngineFactory {
    
    /**
     * Crea una instancia de DatabaseEngine según el tipo especificado.
     * 
     * @param type Tipo de motor (MYSQL, MARIADB, POSTGRESQL, SQLITE)
     * @param context Contexto de Android necesario para leer certificados SSL
     * @return Instancia concreta de DatabaseEngine
     * @throws NotImplementedError si el tipo no está implementado todavía
     * 
     * @see DatabaseType
     * @see DatabaseEngine
     */
    fun create(type: DatabaseType, context: Context): DatabaseEngine {
        return when (type) {
            DatabaseType.MYSQL -> MySQLEngine(context)
            DatabaseType.MARIADB -> MariaDBEngine(context)
            DatabaseType.POSTGRESQL -> throw NotImplementedError("PostgreSQL será implementado en v1.1")
            DatabaseType.SQLITE -> throw NotImplementedError("SQLite será implementado en v1.1")
        }
    }
}
