package com.sphynxs.mydatabases.domain.repositories

import android.net.Uri
import com.sphynxs.mydatabases.domain.models.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de configuración de la aplicación.
 *
 * Gestiona preferencias del usuario usando DataStore como backend de persistencia.
 * Actualmente soporta:
 * - Branded palette toggle (usar colores branded vs dynamic)
 * - Theme mode (light/dark/system)
 *
 * Future: language, etc. (cambio #6 del roadmap)
 *
 * @author israel-icm
 * @date 2026-06-15
 */
interface SettingsRepository {
    /**
     * Observa el estado de la preferencia de paleta branded.
     *
     * @return Flow<Boolean> — true si el usuario prefiere branded, false si dynamic
     */
    fun observeBrandedPaletteEnabled(): Flow<Boolean>
    
    /**
     * Persiste la preferencia de paleta branded.
     *
     * @param enabled true para activar branded, false para dynamic
     */
    suspend fun setBrandedPaletteEnabled(enabled: Boolean)

    /**
     * Observa el modo de tema persistido.
     *
     * @return Flow<ThemeMode> — SYSTEM cuando no hay preferencia guardada (default)
     */
    fun observeThemeMode(): Flow<ThemeMode>

    /**
     * Persiste el modo de tema elegido por el usuario.
     *
     * @param mode Modo de tema a persistir (LIGHT, DARK o SYSTEM)
     */
    suspend fun setThemeMode(mode: ThemeMode)

    /**
     * Observa la Uri de árbol SAF (`OpenDocumentTree`) elegida por el usuario para el
     * almacenamiento de query files (change `query-files-storage`).
     *
     * @return Flow<Uri?> — null cuando no hay preferencia guardada (usa la carpeta privada
     *   de la app por defecto)
     */
    fun observeQueryStorageTreeUri(): Flow<Uri?>

    /**
     * Persiste (o limpia, si [uri] es null) la Uri de árbol SAF elegida por el usuario.
     *
     * @param uri Uri de árbol SAF persistente, o null para volver al default privado
     */
    suspend fun setQueryStorageTreeUri(uri: Uri?)
}
