package com.sphynxs.mydatabases.ui.screens.databases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphynxs.mydatabases.core.database.models.CharacterSet
import com.sphynxs.mydatabases.core.database.models.Collation
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
    private val getCharacterSetsUseCase: GetCharacterSetsUseCase
) : ViewModel() {
    
    // UI State para character sets
    private val _charsetState = MutableStateFlow<CharsetLoadState>(CharsetLoadState.Loading)
    val charsetState: StateFlow<CharsetLoadState> = _charsetState.asStateFlow()
    
    // UI State para collations
    private val _collationState = MutableStateFlow<CollationLoadState>(CollationLoadState.Idle)
    val collationState: StateFlow<CollationLoadState> = _collationState.asStateFlow()
    
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
