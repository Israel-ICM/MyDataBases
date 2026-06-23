package com.sphynxs.mydatabases.ui.screens.databases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphynxs.mydatabases.core.database.models.CharacterSet
import com.sphynxs.mydatabases.core.database.models.Collation
import com.sphynxs.mydatabases.core.database.models.DatabaseError
import com.sphynxs.mydatabases.domain.usecases.CreateDatabaseUseCase
import com.sphynxs.mydatabases.domain.usecases.GetCharacterSetsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para el formulario de agregar base de datos.
 *
 * Maneja:
 * - Carga de character sets desde el servidor (con cache)
 * - Carga de collations filtradas por charset seleccionado (con cache)
 * - Validación de formulario
 *
 * @author israel-icm
 * @date 2026-06-19
 */
@HiltViewModel
class AddDatabaseViewModel @Inject constructor(
    private val getCharacterSetsUseCase: GetCharacterSetsUseCase,
    private val createDatabaseUseCase: CreateDatabaseUseCase
) : ViewModel() {
    
    // UI State para character sets
    private val _charsetState = MutableStateFlow<CharsetLoadState>(CharsetLoadState.Loading)
    val charsetState: StateFlow<CharsetLoadState> = _charsetState.asStateFlow()
    
    // UI State para collations
    private val _collationState = MutableStateFlow<CollationLoadState>(CollationLoadState.Idle)
    val collationState: StateFlow<CollationLoadState> = _collationState.asStateFlow()
    
    // UI State para submit (database creation)
    private val _submitState = MutableStateFlow<CreateDatabaseState>(CreateDatabaseState.Idle)
    val submitState: StateFlow<CreateDatabaseState> = _submitState.asStateFlow()
    
    // Cache de collations por charset para evitar queries repetidas
    private val collationCache = mutableMapOf<String, List<Collation>>()
    
    init {
        loadCharacterSets()
    }
    
    /**
     * Carga todos los character sets disponibles desde el servidor.
     *
     * Se ejecuta automáticamente al crear el ViewModel.
     * Resultados se cachean en memoria durante la vida del ViewModel.
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
        // Si ya están en cache, usarlas
        collationCache[charset]?.let { cached ->
            _collationState.value = CollationLoadState.Success(cached)
            return
        }
        
        // Si no, cargar desde servidor
        viewModelScope.launch {
            _collationState.value = CollationLoadState.Loading
            
            getCharacterSetsUseCase.getCollations(charset)
                .onSuccess { collations ->
                    // Guardar en cache
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
     * Creates a new database with the specified name and optional charset/collation.
     *
     * Transitions submitState through: Idle → Submitting → (Success | Error)
     * Error mapping handles:
     * - "database exists" → specific user-friendly message
     * - "Access denied" / "command denied" → permission message
     * - ConnectionFailed → connection-lost message
     * - InvalidConfiguration → invalid-name message
     * - Generic → generic failure message
     *
     * @param name Database name
     * @param charset Character set (optional)
     * @param collation Collation (optional)
     */
    fun createDatabase(name: String, charset: String?, collation: String?) {
        viewModelScope.launch {
            _submitState.value = CreateDatabaseState.Submitting
            
            createDatabaseUseCase(name, charset, collation)
                .onSuccess {
                    _submitState.value = CreateDatabaseState.Success
                }
                .onFailure { error ->
                    val message = mapErrorToMessage(error)
                    _submitState.value = CreateDatabaseState.Error(message)
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
                    error.reason.contains("database exists", ignoreCase = true) ||
                    error.reason.contains("1007") -> "error_database_exists"
                    
                    error.reason.contains("Access denied", ignoreCase = true) ||
                    error.reason.contains("command denied", ignoreCase = true) -> "error_permission_denied"
                    
                    else -> "error_create_database_failed"
                }
            }
            is DatabaseError.ConnectionFailed -> "error_connection_lost"
            is DatabaseError.InvalidConfiguration -> "error_invalid_database_name"
            else -> "error_create_database_failed"
        }
    }
    
    /**
     * Resets submitState back to Idle (for retries or after success side effects complete).
     */
    fun resetSubmitState() {
        _submitState.value = CreateDatabaseState.Idle
    }
}

/**
 * Estados de carga para character sets.
 */
sealed class CharsetLoadState {
    data object Loading : CharsetLoadState()
    data class Success(val charsets: List<CharacterSet>) : CharsetLoadState()
    data class Error(val message: String) : CharsetLoadState()
}

/**
 * Estados de carga para collations.
 */
sealed class CollationLoadState {
    data object Idle : CollationLoadState()
    data object Loading : CollationLoadState()
    data class Success(val collations: List<Collation>) : CollationLoadState()
    data class Error(val message: String) : CollationLoadState()
}

/**
 * Estados de submit para database creation.
 */
sealed class CreateDatabaseState {
    data object Idle : CreateDatabaseState()
    data object Submitting : CreateDatabaseState()
    data object Success : CreateDatabaseState()
    data class Error(val message: String) : CreateDatabaseState()
}
