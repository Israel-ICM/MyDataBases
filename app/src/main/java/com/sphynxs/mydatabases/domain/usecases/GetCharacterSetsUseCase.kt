package com.sphynxs.mydatabases.domain.usecases

import com.sphynxs.mydatabases.core.database.models.CharacterSet
import com.sphynxs.mydatabases.core.database.models.Collation
import com.sphynxs.mydatabases.core.database.repository.DatabaseRepository
import javax.inject.Inject

/**
 * Use case para obtener character sets y collations disponibles desde MySQL/MariaDB.
 *
 * Ejecuta queries `SHOW CHARACTER SET` y `SHOW COLLATION` contra el servidor activo.
 * Diseñado para ser cacheado en el ViewModel para evitar queries repetidas.
 *
 * @param repository Repository para acceso al motor de base de datos conectado
 * @author israel-icm
 * @date 2026-06-19
 */
class GetCharacterSetsUseCase @Inject constructor(
    private val repository: DatabaseRepository
) {
    
    /**
     * Obtiene todos los character sets disponibles en el servidor MySQL activo.
     *
     * Usa el motor conectado desde DatabaseRepository.
     *
     * @return Result con lista de CharacterSet ordenados alfabéticamente
     * @throws DatabaseError.ConnectionFailed si no hay conexión activa
     */
    suspend fun getCharacterSets(): Result<List<CharacterSet>> {
        return repository.getCharacterSets().map { charsets ->
            charsets.sortedBy { it.name }
        }
    }
    
    /**
     * Obtiene todas las collations disponibles para un character set específico.
     *
     * Usa el motor conectado desde DatabaseRepository.
     *
     * @param charset Nombre del character set (ej: utf8mb4, latin1)
     * @return Result con lista de Collation ordenadas: default primero, luego alfabético
     * @throws DatabaseError.ConnectionFailed si no hay conexión activa
     */
    suspend fun getCollations(charset: String): Result<List<Collation>> {
        return repository.getCollations(charset).map { collations ->
            // Ordenar: default primero, luego alfabético
            collations.sortedWith(
                compareByDescending<Collation> { it.isDefault }
                    .thenBy { it.name }
            )
        }
    }
}
