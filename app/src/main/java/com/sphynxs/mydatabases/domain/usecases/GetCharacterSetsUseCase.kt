package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.engine.mysql.MySQLConnectionPool
import com.sphynxs.mydatabases.core.database.engine.mysql.MySQLEngine
import com.sphynxs.mydatabases.core.database.models.CharacterSet
import com.sphynxs.mydatabases.core.database.models.Collation
import javax.inject.Inject

/**
 * Use case para obtener character sets y collations disponibles desde MySQL/MariaDB.
 *
 * Ejecuta queries `SHOW CHARACTER SET` y `SHOW COLLATION` contra el servidor activo.
 * Diseñado para ser cacheado en el ViewModel para evitar queries repetidas.
 *
 * @author israel-icm
 * @date 2026-06-19
 */
class GetCharacterSetsUseCase @Inject constructor() {
    
    /**
     * Obtiene todos los character sets disponibles en el servidor MySQL activo.
     *
     * Usa la conexión activa del singleton MySQLConnectionPool.
     *
     * @return Result con lista de CharacterSet ordenados alfabéticamente
     * @throws DatabaseError.ConnectionFailed si no hay conexión activa
     */
    suspend fun getCharacterSets(): Result<List<CharacterSet>> {
        val engine = MySQLEngine()
        // Nota: el engine internamente usa MySQLConnectionPool singleton para obtener la conexión
        return engine.getCharacterSets().map { charsets ->
            charsets.sortedBy { it.name }
        }
    }
    
    /**
     * Obtiene todas las collations disponibles para un character set específico.
     *
     * Usa la conexión activa del singleton MySQLConnectionPool.
     *
     * @param charset Nombre del character set (ej: utf8mb4, latin1)
     * @return Result con lista de Collation ordenadas: default primero, luego alfabético
     * @throws DatabaseError.ConnectionFailed si no hay conexión activa
     */
    suspend fun getCollations(charset: String): Result<List<Collation>> {
        val engine = MySQLEngine()
        return engine.getCollations(charset).map { collations ->
            // Ordenar: default primero, luego alfabético
            collations.sortedWith(
                compareByDescending<Collation> { it.isDefault }
                    .thenBy { it.name }
            )
        }
    }
}
