package com.sphynxs.mydatabases.ui.screens.queryfiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphynxs.mydatabases.data.storage.QueryStorageResolver
import com.sphynxs.mydatabases.domain.models.QueryFileInfo
import com.sphynxs.mydatabases.domain.models.RootResolution
import com.sphynxs.mydatabases.domain.repositories.ConnectionRepository
import com.sphynxs.mydatabases.domain.usecases.queryfiles.ListQueryFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Sealed UI state for the Query Files list screen (change `query-files-storage`). */
sealed class QueryFilesUiState {
    data object Idle : QueryFilesUiState()
    data object Loading : QueryFilesUiState()
    data class Success(val files: List<QueryFileInfo>) : QueryFilesUiState()
    data object Empty : QueryFilesUiState()
    data class Error(val message: String) : QueryFilesUiState()
}

/**
 * Drives the Query Files list screen: resolves the active connection's [com.sphynxs.mydatabases.core.database.engine.DatabaseType]
 * and lists that engine's managed `.sql` files, most-recent-first (change `query-files-storage`).
 *
 * No folder-watching — [load]/[refresh] are the only refresh points, called on initial
 * composition and on screen resume respectively (same call, different trigger).
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
@HiltViewModel
class QueryFilesViewModel @Inject constructor(
    private val listQueryFilesUseCase: ListQueryFilesUseCase,
    private val connectionRepository: ConnectionRepository,
    private val queryStorageResolver: QueryStorageResolver
) : ViewModel() {

    private val _state = MutableStateFlow<QueryFilesUiState>(QueryFilesUiState.Idle)
    val state: StateFlow<QueryFilesUiState> = _state.asStateFlow()

    /**
     * True when the active storage root is a SAF-permission-loss fallback (change
     * `query-files-storage`, Phase 9 gap closure) — surfaced on EVERY [load]/[refresh], never
     * suppressed, per the confirmed decision.
     */
    private val _showFallbackNotice = MutableStateFlow(false)
    val showFallbackNotice: StateFlow<Boolean> = _showFallbackNotice.asStateFlow()

    fun load(connectionId: String) {
        viewModelScope.launch {
            _state.value = QueryFilesUiState.Loading
            _showFallbackNotice.value = queryStorageResolver.resolveRoot() is RootResolution.Fallback

            val connection = connectionRepository.getById(connectionId)
            if (connection == null) {
                _state.value = QueryFilesUiState.Error("Connection not found")
                return@launch
            }

            listQueryFilesUseCase(connection.type).fold(
                onSuccess = { files ->
                    val sorted = files
                        .filter { it.name.endsWith(".sql", ignoreCase = true) }
                        .sortedWith(compareByDescending<QueryFileInfo> { it.lastModified }.thenBy { it.name })
                    _state.value = if (sorted.isEmpty()) QueryFilesUiState.Empty else QueryFilesUiState.Success(sorted)
                },
                onFailure = { error ->
                    _state.value = QueryFilesUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    /** Called on screen resume — same as [load], exposed under its own name for call-site clarity. */
    fun refresh(connectionId: String) = load(connectionId)
}
