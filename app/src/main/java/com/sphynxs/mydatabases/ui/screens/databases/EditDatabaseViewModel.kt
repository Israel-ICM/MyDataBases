package com.sphynxs.mydatabases.ui.screens.databases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphynxs.mydatabases.core.database.models.Collation
import com.sphynxs.mydatabases.core.database.models.DatabaseError
import com.sphynxs.mydatabases.domain.usecases.GetCharacterSetsUseCase
import com.sphynxs.mydatabases.domain.usecases.UpdateDatabaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para el formulario de editar base de datos (charset/collation).
 *
 * Reutiliza `GetCharacterSetsUseCase` y los mismos [CharsetLoadState]/[CollationLoadState]
 * definidos en [AddDatabaseViewModel] (mismo paquete) para cargar las listas de charset y
 * collation con cache local. El nombre de la base de datos NO es editable — MySQL/MariaDB
 * no soportan `RENAME DATABASE` vía ALTER, solo charset y collation.
 *
 * @property getCharacterSetsUseCase Use case para cargar charsets/collations disponibles
 * @property updateDatabaseUseCase Use case que compone y ejecuta el ALTER DATABASE
 *
 * @author gentle-ai
 * @date 2026-07-21
 */
@HiltViewModel
class EditDatabaseViewModel @Inject constructor(
    private val getCharacterSetsUseCase: GetCharacterSetsUseCase,
    private val updateDatabaseUseCase: UpdateDatabaseUseCase
) : ViewModel() {

    // UI State para character sets
    private val _charsetState = MutableStateFlow<CharsetLoadState>(CharsetLoadState.Loading)
    val charsetState: StateFlow<CharsetLoadState> = _charsetState.asStateFlow()

    // UI State para collations
    private val _collationState = MutableStateFlow<CollationLoadState>(CollationLoadState.Idle)
    val collationState: StateFlow<CollationLoadState> = _collationState.asStateFlow()

    // UI State para submit (database update)
    private val _submitState = MutableStateFlow<UpdateDatabaseState>(UpdateDatabaseState.Idle)
    val submitState: StateFlow<UpdateDatabaseState> = _submitState.asStateFlow()

    // Cache de collations por charset para evitar queries repetidas
    private val collationCache = mutableMapOf<String, List<Collation>>()

    init {
        loadCharacterSets()
    }

    /**
     * Carga todos los character sets disponibles desde el servidor.
     *
     * Se ejecuta automáticamente al crear el ViewModel.
     */
    private fun loadCharacterSets() {
        viewModelScope.launch {
            _charsetState.value = CharsetLoadState.Loading

            getCharacterSetsUseCase.getCharacterSets()
                .onSuccess { charsets ->
                    _charsetState.value = CharsetLoadState.Success(charsets)
                }
                .onFailure { error ->
                    _charsetState.value = CharsetLoadState.Error(
                        error.message ?: "Error loading character sets"
                    )
                }
        }
    }

    /**
     * Carga las collations disponibles para un character set específico.
     *
     * Usa cache si ya fueron cargadas previamente.
     *
     * @param charset Nombre del character set (ej: utf8mb4)
     */
    fun loadCollations(charset: String) {
        collationCache[charset]?.let { cached ->
            _collationState.value = CollationLoadState.Success(cached)
            return
        }

        viewModelScope.launch {
            _collationState.value = CollationLoadState.Loading

            getCharacterSetsUseCase.getCollations(charset)
                .onSuccess { collations ->
                    collationCache[charset] = collations
                    _collationState.value = CollationLoadState.Success(collations)
                }
                .onFailure { error ->
                    _collationState.value = CollationLoadState.Error(
                        error.message ?: "Error loading collations"
                    )
                }
        }
    }

    /**
     * Resetea el estado de collations a Idle cuando se deselecciona un charset.
     */
    fun clearCollations() {
        _collationState.value = CollationLoadState.Idle
    }

    /**
     * Updates the database's charset and/or collation.
     *
     * Transitions submitState through: Idle → Submitting → (Success | Error)
     *
     * @param name Database name (identifies which database to alter, not renamable)
     * @param charset New character set (optional)
     * @param collation New collation (optional)
     */
    fun updateDatabase(name: String, charset: String?, collation: String?) {
        viewModelScope.launch {
            _submitState.value = UpdateDatabaseState.Submitting

            updateDatabaseUseCase(name, charset, collation)
                .onSuccess {
                    _submitState.value = UpdateDatabaseState.Success
                }
                .onFailure { error ->
                    val message = mapErrorToMessage(error)
                    _submitState.value = UpdateDatabaseState.Error(message)
                }
        }
    }

    /**
     * Maps DatabaseError to user-friendly message keys.
     *
     * Returns string resource keys that the UI layer will resolve to localized strings.
     */
    private fun mapErrorToMessage(error: Throwable): String {
        return when (error) {
            is DatabaseError.QueryExecutionFailed -> {
                when {
                    error.reason.contains("Access denied", ignoreCase = true) ||
                    error.reason.contains("command denied", ignoreCase = true) -> "error_update_permission_denied"

                    else -> "error_update_database_failed"
                }
            }
            is DatabaseError.ConnectionFailed -> "error_connection_lost"
            else -> "error_update_database_failed"
        }
    }

    /**
     * Resets submitState back to Idle (for retries or after success side effects complete).
     */
    fun resetSubmitState() {
        _submitState.value = UpdateDatabaseState.Idle
    }
}

/**
 * Estados de submit para database update.
 */
sealed class UpdateDatabaseState {
    data object Idle : UpdateDatabaseState()
    data object Submitting : UpdateDatabaseState()
    data object Success : UpdateDatabaseState()
    data class Error(val message: String) : UpdateDatabaseState()
}
