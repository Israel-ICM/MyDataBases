package com.sphynxs.mydatabases.ui.screens.tables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.ui.components.ios.IOSButton
import com.sphynxs.mydatabases.ui.components.ios.IOSButtonStyle
import com.sphynxs.mydatabases.ui.components.ios.IOSGroupedCard
import com.sphynxs.mydatabases.ui.components.ios.IOSTextField
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens
import kotlinx.coroutines.launch

/**
 * Contenido del sheet "Crear tabla" (change `create-table`, PR-2).
 *
 * Diseñado para usarse dentro de un `ModalBottomSheet` (mirrors `AddDatabaseFormContent`).
 * Orden vertical exacto por spec (Requirement: Sheet Layout Order): (1) campo nombre de
 * tabla, (2) lista de campos/columnas, (3) botón "+ Agregar campo", (4) acciones OK/Cancel.
 *
 * El botón "+ Agregar campo" queda sin wireado en este PR — abrir `FieldDefinitionDialog`
 * es responsabilidad de PR-3 (ver tasks.md Phase 3).
 *
 * Spec: create-table
 * Phase: 2 (Nav Wiring & Parent Sheet)
 *
 * @param connectionId ID de la conexión activa
 * @param onDismiss Callback para cerrar el bottom sheet
 * @param onTableCreated Callback invocado tras crear la tabla exitosamente (para refrescar la lista)
 * @param snackbarHostState Estado del snackbar (provisto por el sheet parent)
 * @param viewModel ViewModel que maneja la lista de campos y la creación de la tabla
 * @param modifier Modificador opcional
 *
 * @author sdd-apply
 * @date 2026-07-15
 */
@Composable
fun CreateTableFormContent(
    connectionId: String,
    onDismiss: () -> Unit = {},
    onTableCreated: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    viewModel: CreateTableViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // Estados del ViewModel
    val fields by viewModel.fields.collectAsState()
    val createTableState by viewModel.createTableState.collectAsState()

    // Estado local del nombre de tabla (mirrors AddDatabaseFormContent's `name`)
    var name by remember { mutableStateOf("") }

    // Strings localizados (extraídos para usar en LaunchedEffect)
    val successMessage = stringResource(R.string.create_table_success)
    val errorGeneric = stringResource(R.string.create_table_error_generic)

    // Manejar cambios de estado de la creación de tabla
    LaunchedEffect(createTableState) {
        when (createTableState) {
            is CreateTableState.Success -> {
                // Cerrar el sheet y resetear el ViewModel (spec: "Sheet opens fresh after prior dismiss")
                onDismiss()
                onTableCreated()

                scope.launch {
                    snackbarHostState.showSnackbar(successMessage)
                }

                viewModel.reset()
            }
            is CreateTableState.Error -> {
                // Mostrar error, mantener el sheet abierto con los datos ingresados intactos
                // (Requirement: Parent OK Executes DDL — "DDL failure keeps data")
                scope.launch {
                    snackbarHostState.showSnackbar(errorGeneric)
                }
            }
            else -> {
                // Idle o Submitting - no hacer nada
            }
        }
    }

    // El OK del sheet padre se habilita solo con nombre no-blank Y al menos un campo
    // (Requirement: Parent OK Enablement)
    val isFormValid = name.isNotBlank() && fields.isNotEmpty()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalDesignTokens.current.backgroundPrimary)
                .padding(vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Título (estilo iOS)
            Text(
                text = stringResource(R.string.create_table_title),
                fontSize = LocalDesignTokens.current.largeTitleSize,
                fontWeight = LocalDesignTokens.current.largeTitleWeight,
                color = LocalDesignTokens.current.largeTitleColor,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // (1) Campo de nombre de tabla
            IOSGroupedCard {
                IOSTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = stringResource(R.string.create_table_field_name_hint),
                    showDivider = false
                )
            }

            // (2) Lista de campos/columnas — solo lectura en este PR (sin edición/borrado)
            if (fields.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fields.forEach { field ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    LocalDesignTokens.current.surfacePrimary,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = field.name,
                                color = LocalDesignTokens.current.textPrimary,
                                fontSize = 17.sp
                            )
                            Text(
                                text = stringResource(R.string.column_type, field.type.sqlName),
                                color = LocalDesignTokens.current.textSecondary,
                                fontSize = LocalDesignTokens.current.labelSize
                            )
                        }
                    }
                }
            }

            // (3) Botón "+ Agregar campo" — sin wireado en PR-2 (PR-3 abre FieldDefinitionDialog)
            IOSButton(
                text = stringResource(R.string.create_table_add_field),
                onClick = {
                    // TODO(PR-3): abrir FieldDefinitionDialog y llamar viewModel.addField(...)
                    // en onFieldConfirmed. Sin wireado en PR-2 por diseño (ver tasks.md Phase 3).
                },
                style = IOSButtonStyle.Secondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // (4) Acciones OK/Cancel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IOSButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = {
                        // Requirement: Parent Cancel Dismisses — descarta el borrador
                        viewModel.reset()
                        onDismiss()
                    },
                    style = IOSButtonStyle.Secondary,
                    modifier = Modifier.weight(1f)
                )

                IOSButton(
                    text = if (createTableState is CreateTableState.Submitting) {
                        stringResource(R.string.loading)
                    } else {
                        stringResource(R.string.create_table_button_create)
                    },
                    onClick = {
                        viewModel.createTable(connectionId, name, fields)
                    },
                    enabled = isFormValid && createTableState !is CreateTableState.Submitting,
                    style = IOSButtonStyle.Primary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Overlay de loading durante el submit (mirrors AddDatabaseFormContent)
        if (createTableState is CreateTableState.Submitting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LocalDesignTokens.current.backdropScrim),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = LocalDesignTokens.current.accentPrimary
                    )
                    Text(
                        text = stringResource(R.string.loading),
                        fontSize = 16.sp,
                        color = LocalDesignTokens.current.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
