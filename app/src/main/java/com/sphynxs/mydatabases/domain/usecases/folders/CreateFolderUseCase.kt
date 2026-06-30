package com.sphynxs.mydatabases.domain.usecases.folders

import com.sphynxs.mydatabases.core.database.models.ConnectionFolder
import com.sphynxs.mydatabases.domain.repositories.FolderRepository
import javax.inject.Inject

/**
 * Use case para crear un nuevo folder de conexiones.
 *
 * @property repository Repositorio de folders
 * @author israel-icm
 * @date 2026-06-30
 */
class CreateFolderUseCase @Inject constructor(
    private val repository: FolderRepository
) {
    /**
     * Crea un nuevo folder.
     *
     * @param name Nombre del folder (requerido, no vacío)
     * @param order Posición en la lista (default: 0)
     * @return Result con Unit si exitoso, error si falla
     */
    suspend operator fun invoke(
        name: String,
        order: Int = 0
    ): Result<Unit> {
        return try {
            require(name.isNotBlank()) { "Folder name cannot be empty" }
            
            val folder = ConnectionFolder(
                name = name.trim(),
                order = order,
                isExpanded = true  // Nuevos folders expandidos por defecto
            )
            
            repository.save(folder)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
