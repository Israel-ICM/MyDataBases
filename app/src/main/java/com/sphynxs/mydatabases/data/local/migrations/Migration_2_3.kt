package com.sphynxs.mydatabases.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migración de base de datos Room de versión 2 a 3.
 *
 * Cambios:
 * 1. Agregar columnas `folder_id` y `order` a tabla `connections`
 * 2. Crear tabla `connection_folders` para organizar conexiones
 * 3. Crear índice en `folder_id` para lookups rápidos
 *
 * @author israel-icm
 * @date 2026-06-30
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Agregar columna `folder_id` a connections (nullable)
        database.execSQL("ALTER TABLE connections ADD COLUMN folder_id TEXT")
        
        // 2. Agregar columna `order` a connections (NOT NULL default 0)
        database.execSQL("ALTER TABLE connections ADD COLUMN `order` INTEGER NOT NULL DEFAULT 0")
        
        // 3. Crear tabla connection_folders
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS connection_folders (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                icon TEXT,
                color TEXT,
                is_expanded INTEGER NOT NULL DEFAULT 1,
                `order` INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
        """.trimIndent())
    }
}
