package com.sphynxs.mydatabases.domain.usecases.folders

import com.sphynxs.mydatabases.data.local.dao.ConnectionDao
import javax.inject.Inject

/**
 * Use case para mover una conexión a un folder (o a root).
 *
 * @property connectionDao DAO de conexiones (acceso directo para update)
 * @author israel-icm
 * @date 2026-06-30
 */
class MoveConnectionToFolderUseCase @Inject constructor(
    private val connectionDao: ConnectionDao
) {
    /**
     * Mueve una conexión a un folder específico.
     *
     * @param connectionId ID de la conexión a mover
     * @param folderId ID del folder destino (null = mover a root)
     * @return Result con Unit si exitoso, error si falla
     */
    suspend operator fun invoke(
        connectionId: String,
        folderId: String?
    ): Result<Unit> {
        return try {
            connectionDao.moveToFolder(connectionId, folderId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
