package com.sphynxs.mydatabases.ui.screens.tables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphynxs.mydatabases.core.database.models.CharacterSet
import com.sphynxs.mydatabases.core.database.models.Collation
import com.sphynxs.mydatabases.core.database.models.ColumnDefinition
import com.sphynxs.mydatabases.domain.usecases.CreateTableUseCase
import com.sphynxs.mydatabases.domain.usecases.GetCharacterSetsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para el sheet "Crear tabla" (change `create-table`, PR-2).
 *
 * Es dueño de la lista de campos en construcción ([fields]) para que sobreviva a la
 * apertura/cierre del [FieldDefinitionDialog] anidado (PR-3) — ver design.md, decisión
 * "Field-list ownership". El nombre de la tabla vive como estado local en
 * `CreateTableFormContent`, igual que `AddDatabaseFormContent`.
 *
 * También es dueño de la carga en vivo de character sets/collations para los dropdowns
 * Conjunto de caracteres/Collation de [FieldDefinitionDialog] (change `create-table`,
 * extended field attributes addendum), mirroring `AddDatabaseViewModel`'s
 * `CharsetLoadState`/`CollationLoadState` pattern. [FieldCharsetLoadState]/
 * [FieldCollationLoadState] son duplicados locales (no reutilizan los de
 * `AddDatabaseViewModel`) para evitar acoplamiento cross-feature entre
 * `ui/screens/databases` y `ui/screens/tables`.
 *
 * @property createTableUseCase Use case que valida y ejecuta el DDL `CREATE TABLE`
 * @property getCharacterSetsUseCase Use case para cargar character sets/collations en vivo
 * @author sdd-apply
 * @date 2026-07-15
 */
@HiltViewModel
class CreateTableViewModel @Inject constructor(
    private val createTableUseCase: CreateTableUseCase,
    private val getCharacterSetsUseCase: GetCharacterSetsUseCase,
) : ViewModel() {

    private val _fields = MutableStateFlow<List<ColumnDefinition>>(emptyList())
    val fields: StateFlow<List<ColumnDefinition>> = _fields.asStateFlow()

    private val _createTableState = MutableStateFlow<CreateTableState>(CreateTableState.Idle)
    val createTableState: StateFlow<CreateTableState> = _createTableState.asStateFlow()

    // UI state para character sets/collations del campo Charset/Collation del Field
    // Definition Dialog (change `create-table`, extended field attributes addendum)
    private val _fieldCharsetState = MutableStateFlow<FieldCharsetLoadState>(FieldCharsetLoadState.Loading)
    val fieldCharsetState: StateFlow<FieldCharsetLoadState> = _fieldCharsetState.asStateFlow()

    private val _fieldCollationState = MutableStateFlow<FieldCollationLoadState>(FieldCollationLoadState.Idle)
    val fieldCollationState: StateFlow<FieldCollationLoadState> = _fieldCollationState.asStateFlow()

    // Cache de collations por charset para evitar queries repetidas (mirrors AddDatabaseViewModel)
    private val fieldCollationCache = mutableMapOf<String, List<Collation>>()

    init {
        loadCharacterSets()
    }

    /**
     * Carga todos los character sets disponibles desde el servidor para el dropdown Conjunto
     * de caracteres del Field Definition Dialog. Se ejecuta automáticamente al crear el
     * ViewModel (mirrors `AddDatabaseViewModel.loadCharacterSets`).
     */
    private fun loadCharacterSets() {
        viewModelScope.launch {
            _fieldCharsetState.value = FieldCharsetLoadState.Loading

            getCharacterSetsUseCase.getCharacterSets()
                .onSuccess { charsets ->
                    _fieldCharsetState.value = FieldCharsetLoadState.Success(charsets)
                }
                .onFailure { error ->
                    _fieldCharsetState.value = FieldCharsetLoadState.Error(
                        error.message ?: "Error loading character sets"
                    )
                }
        }
    }

    /**
     * Carga las collations disponibles para un character set específico, usando cache si ya
     * fueron cargadas previamente (mirrors `AddDatabaseViewModel.loadCollations`). Invocado
     * desde [FieldDefinitionDialog]'s `onCharsetSelected` cuando el usuario selecciona un
     * charset.
     *
     * @param charset Nombre del character set (ej: utf8mb4)
     */
    fun loadCollations(charset: String) {
        fieldCollationCache[charset]?.let { cached ->
            _fieldCollationState.value = FieldCollationLoadState.Success(cached)
            return
        }

        viewModelScope.launch {
            _fieldCollationState.value = FieldCollationLoadState.Loading

            getCharacterSetsUseCase.getCollations(charset)
                .onSuccess { collations ->
                    fieldCollationCache[charset] = collations
                    _fieldCollationState.value = FieldCollationLoadState.Success(collations)
                }
                .onFailure { error ->
                    _fieldCollationState.value = FieldCollationLoadState.Error(
                        error.message ?: "Error loading collations"
                    )
                }
        }
    }

    /** Resetea el estado de collations a Idle (ej. al cerrar/resetear el Field Definition Dialog). */
    fun clearCollations() {
        _fieldCollationState.value = FieldCollationLoadState.Idle
    }

    /**
     * Agrega un campo al final de la lista en construcción.
     *
     * Invocado desde `CreateTableFormContent`'s `FieldDefinitionDialog.onFieldConfirmed`
     * (PR-3).
     */
    fun addField(field: ColumnDefinition) {
        _fields.value = _fields.value + field
    }

    /**
     * Crea la tabla ejecutando el DDL vía [CreateTableUseCase].
     *
     * @param connectionId ID de la conexión activa. NOTA: al igual que `CreateDatabaseUseCase`,
     *   `CreateTableUseCase` opera contra la conexión activa a través del `DatabaseRepository`
     *   inyectado y no recibe `connectionId` explícitamente; el parámetro se mantiene aquí
     *   para coincidir con el contrato de design.md y por si una futura conexión multi-sesión
     *   lo requiere.
     * @param name Nombre de la tabla
     * @param fields Lista ordenada de definiciones de columna
     */
    fun createTable(connectionId: String, name: String, fields: List<ColumnDefinition>) {
        viewModelScope.launch {
            _createTableState.value = CreateTableState.Submitting

            createTableUseCase(name, fields)
                .onSuccess {
                    _createTableState.value = CreateTableState.Success
                }
                .onFailure { error ->
                    _createTableState.value = CreateTableState.Error(
                        error.message ?: "error_create_table_failed"
                    )
                }
        }
    }

    /**
     * Resetea el estado a fresco (campos vacíos, estado Idle).
     *
     * Invocado al cerrar/cancelar el sheet y tras un éxito, para satisfacer el escenario
     * de spec "Sheet opens fresh after prior dismiss".
     */
    fun reset() {
        _fields.value = emptyList()
        _createTableState.value = CreateTableState.Idle
    }
}

/**
 * Estados del submit de creación de tabla.
 */
sealed class CreateTableState {
    data object Idle : CreateTableState()
    data object Submitting : CreateTableState()
    data object Success : CreateTableState()
    data class Error(val message: String) : CreateTableState()
}

/**
 * Estados de carga para character sets del Field Definition Dialog (change `create-table`,
 * extended field attributes addendum). Duplicado local de `AddDatabaseViewModel`'s
 * `CharsetLoadState` para evitar acoplamiento cross-feature.
 */
sealed class FieldCharsetLoadState {
    data object Loading : FieldCharsetLoadState()
    data class Success(val charsets: List<CharacterSet>) : FieldCharsetLoadState()
    data class Error(val message: String) : FieldCharsetLoadState()
}

/**
 * Estados de carga para collations del Field Definition Dialog (change `create-table`,
 * extended field attributes addendum). Duplicado local de `AddDatabaseViewModel`'s
 * `CollationLoadState` para evitar acoplamiento cross-feature.
 */
sealed class FieldCollationLoadState {
    data object Idle : FieldCollationLoadState()
    data object Loading : FieldCollationLoadState()
    data class Success(val collations: List<Collation>) : FieldCollationLoadState()
    data class Error(val message: String) : FieldCollationLoadState()
}
