package com.sphynxs.mydatabases.domain.usecases.folders

import com.sphynxs.mydatabases.data.local.dao.ConnectionDao
import com.sphynxs.mydatabases.domain.repositories.FolderRepository
import javax.inject.Inject

/**
 * Use case para eliminar un folder de conexiones.
 *
 * Por seguridad, mueve todas las conexiones del folder a root antes de eliminarlo.
 * Esto evita que queden conexiones huérfanas.
 *
 * @property folderRepository Repositorio de folders
 * @property connectionDao DAO de conexiones (acceso directo para batch update)
 * @author israel-icm
 * @date 2026-06-30
 */
class DeleteFolderUseCase @Inject constructor(
    private val folderRepository: FolderRepository,
    private val connectionDao: ConnectionDao
) {
    /**
     * Elimina un folder.
     *
     * IMPORTANTE: Las conexiones del folder se mueven automáticamente a root.
     * Si quieres eliminarlas también, debes hacerlo manualmente antes.
     *
     * @param folderId ID del folder a eliminar
     * @param moveToRoot Si true (default), mueve conexiones a root. Si false, asume que ya están movidas o eliminadas.
     * @return Result con Unit si exitoso, error si falla
     */
    suspend operator fun invoke(
        folderId: String,
        moveToRoot: Boolean = true
    ): Result<Unit> {
        return try {
            // Primero mover conexiones a root (si se solicita)
            if (moveToRoot) {
                connectionDao.moveConnectionsToRoot(folderId)
            }
            
            // Luego eliminar el folder
            folderRepository.delete(folderId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
