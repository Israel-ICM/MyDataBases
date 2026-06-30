package com.sphynxs.mydatabases.data.repositories

import com.sphynxs.mydatabases.core.database.models.ConnectionFolder
import com.sphynxs.mydatabases.data.local.dao.FolderDao
import com.sphynxs.mydatabases.domain.repositories.FolderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Implementación del repositorio de folders usando Room.
 *
 * Delega directamente al DAO ya que folders no requieren encriptación.
 *
 * @property folderDao DAO de Room para acceso a la tabla connection_folders
 * @author israel-icm
 * @date 2026-06-30
 */
class FolderRepositoryImpl @Inject constructor(
    private val folderDao: FolderDao
) : FolderRepository {
    
    override fun getAllFolders(): Flow<List<ConnectionFolder>> {
        return folderDao.getAllFolders()
    }
    
    override suspend fun getById(id: String): ConnectionFolder? {
        return folderDao.getById(id)
    }
    
    override suspend fun save(folder: ConnectionFolder) {
        folderDao.insert(folder)
    }
    
    override suspend fun delete(id: String) {
        folderDao.deleteById(id)
    }
    
    override suspend fun toggleExpand(folderId: String, isExpanded: Boolean) {
        folderDao.updateExpandState(folderId, isExpanded)
    }
    
    override suspend fun updateName(folderId: String, name: String) {
        folderDao.updateName(folderId, name)
    }
    
    override suspend fun updateOrder(folderId: String, order: Int) {
        folderDao.updateOrder(folderId, order)
    }
}
