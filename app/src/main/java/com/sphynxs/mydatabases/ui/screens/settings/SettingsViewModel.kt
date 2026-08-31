package com.sphynxs.mydatabases.ui.screens.settings

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.data.storage.QueryStorageResolver
import com.sphynxs.mydatabases.domain.models.AppFolder
import com.sphynxs.mydatabases.domain.models.ThemeMode
import com.sphynxs.mydatabases.domain.repositories.QueryFileStore
import com.sphynxs.mydatabases.domain.repositories.SettingsRepository
import com.sphynxs.mydatabases.domain.usecases.queryfiles.MigrateQueryFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State of the query-storage-location migration prompt (change `query-files-storage`).
 *
 * [Shown.pendingUri] is `null` when the pending change is "reset to app-private default",
 * non-null when it's a specific SAF tree the user just picked.
 */
sealed class MigrationPromptState {
    data object Hidden : MigrationPromptState()
    data class Shown(val pendingUri: Uri?) : MigrationPromptState()
}

/**
 * ViewModel para la pantalla de Settings.
 *
 * Expone preferencias del usuario como StateFlows y métodos para modificarlas.
 * Actualmente gestiona:
 * - Branded palette toggle
 * - Theme mode (light/dark/system)
 *
 * Future: language, etc. (cambio #6 del roadmap)
 *
 * @author israel-icm (TDD GREEN)
 * @date 2026-06-15
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val queryStorageResolver: QueryStorageResolver,
    private val queryFileStore: QueryFileStore,
    private val migrateQueryFilesUseCase: MigrateQueryFilesUseCase
) : ViewModel() {
    
    /**
     * Estado de la preferencia de paleta branded.
     *
     * - true: usuario prefiere branded colors
     * - false: usuario prefiere dynamic color (o no disponible → branded fallback)
     */
    val brandedPaletteEnabled: StateFlow<Boolean> =
        settingsRepository.observeBrandedPaletteEnabled()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )
    
    /**
     * Persiste la preferencia de paleta branded.
     *
     * @param enabled true para activar branded, false para dynamic
     */
    fun setBrandedPaletteEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBrandedPaletteEnabled(enabled)
        }
    }

    /**
     * Estado del modo de tema (SYSTEM, LIGHT o DARK).
     *
     * Default `SYSTEM` mientras no llega el primer valor persistido.
     */
    val themeMode: StateFlow<ThemeMode> =
        settingsRepository.observeThemeMode()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ThemeMode.SYSTEM
            )

    /**
     * Persiste el modo de tema elegido por el usuario.
     *
     * @param mode Modo de tema a persistir (LIGHT, DARK o SYSTEM)
     */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    // --- Query storage location (change `query-files-storage`) ---

    /**
     * Currently persisted SAF tree Uri for query storage — `null` means the app-private default.
     */
    val queryStorageTreeUri: StateFlow<Uri?> =
        settingsRepository.observeQueryStorageTreeUri()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    private val _migrationPrompt = MutableStateFlow<MigrationPromptState>(MigrationPromptState.Hidden)
    val migrationPrompt: StateFlow<MigrationPromptState> = _migrationPrompt.asStateFlow()

    /** Count of files that failed to copy on the last [confirmMigration] run, or `null` if none/none-yet. */
    private val _lastMigrationFailureCount = MutableStateFlow<Int?>(null)
    val lastMigrationFailureCount: StateFlow<Int?> = _lastMigrationFailureCount.asStateFlow()

    /** Clears the last failure-count notice once the UI has shown it (e.g. after a snackbar dismisses). */
    fun consumeLastMigrationFailureCount() {
        _lastMigrationFailureCount.value = null
    }

    /**
     * Called with the SAF tree the user just picked via `OpenDocumentTree` (already granted
     * `takePersistableUriPermission` by the caller — see `SettingsScreen`'s picker handler), or
     * with `null` for "reset to app-private default".
     *
     * If the CURRENT storage location already has `.sql` files, shows the migration prompt
     * instead of persisting immediately — [confirmMigration]/[declineMigration] both persist,
     * only [confirmMigration] also copies.
     */
    fun onStorageTreeSelected(uri: Uri?) {
        viewModelScope.launch {
            val currentRoot = queryStorageResolver.resolveRoot().root
            if (rootHasQueryFiles(currentRoot)) {
                _migrationPrompt.value = MigrationPromptState.Shown(uri)
            } else {
                settingsRepository.setQueryStorageTreeUri(uri)
            }
        }
    }

    /** User confirmed the copy-prompt: persist the new location, then copy existing files into it. */
    fun confirmMigration() {
        val pending = _migrationPrompt.value as? MigrationPromptState.Shown ?: return
        viewModelScope.launch {
            val oldRoot = queryStorageResolver.resolveRoot().root
            settingsRepository.setQueryStorageTreeUri(pending.pendingUri)
            val result = migrateQueryFilesUseCase(
                oldRoot = oldRoot,
                readContent = { uri -> queryFileStore.read(uri).getOrNull() },
                writeContent = { engine, name, content -> queryFileStore.write(engine, name, content).isSuccess }
            )
            if (result.filesFailed > 0) {
                _lastMigrationFailureCount.value = result.filesFailed
            }
            _migrationPrompt.value = MigrationPromptState.Hidden
        }
    }

    /** User declined the copy-prompt: persist the new location, leave old files untouched. */
    fun declineMigration() {
        val pending = _migrationPrompt.value as? MigrationPromptState.Shown ?: return
        viewModelScope.launch {
            settingsRepository.setQueryStorageTreeUri(pending.pendingUri)
            _migrationPrompt.value = MigrationPromptState.Hidden
        }
    }

    /** Cheap structural scan — no content read — across all 4 engine subfolders. */
    private fun rootHasQueryFiles(root: DocumentFile): Boolean {
        return DatabaseType.entries.any { engine ->
            val segment = AppFolder.segmentFor(engine)
            val files = root.findFile(segment)?.findFile("queries")?.listFiles().orEmpty()
            files.any { it.isFile && it.name.orEmpty().endsWith(".sql", ignoreCase = true) }
        }
    }
}
