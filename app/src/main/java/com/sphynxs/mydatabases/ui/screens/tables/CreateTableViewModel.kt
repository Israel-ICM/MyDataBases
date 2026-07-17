package com.sphynxs.mydatabases.ui.screens.tables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphynxs.mydatabases.core.database.models.ColumnDefinition
import com.sphynxs.mydatabases.domain.usecases.CreateTableUseCase
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
 * @property createTableUseCase Use case que valida y ejecuta el DDL `CREATE TABLE`
 * @author sdd-apply
 * @date 2026-07-15
 */
@HiltViewModel
class CreateTableViewModel @Inject constructor(
    private val createTableUseCase: CreateTableUseCase
) : ViewModel() {

    private val _fields = MutableStateFlow<List<ColumnDefinition>>(emptyList())
    val fields: StateFlow<List<ColumnDefinition>> = _fields.asStateFlow()

    private val _createTableState = MutableStateFlow<CreateTableState>(CreateTableState.Idle)
    val createTableState: StateFlow<CreateTableState> = _createTableState.asStateFlow()

    /**
     * Agrega un campo al final de la lista en construcción.
     *
     * Invocado desde `FieldDefinitionDialog.onFieldConfirmed` (PR-3). No usado todavía
     * en PR-2, ya que el diálogo anidado aún no está wireado.
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
