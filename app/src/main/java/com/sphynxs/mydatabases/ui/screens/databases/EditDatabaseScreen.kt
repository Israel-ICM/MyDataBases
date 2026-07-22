package com.sphynxs.mydatabases.ui.screens.databases

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import com.sphynxs.mydatabases.core.database.models.CharacterSet
import com.sphynxs.mydatabases.core.database.models.Collation
import com.sphynxs.mydatabases.ui.components.ios.IOSButton
import com.sphynxs.mydatabases.ui.components.ios.IOSButtonStyle
import com.sphynxs.mydatabases.ui.components.ios.IOSDropdownField
import com.sphynxs.mydatabases.ui.components.ios.IOSGroupedCard
import com.sphynxs.mydatabases.ui.components.ios.IOSTextField
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens
import kotlinx.coroutines.launch

/**
 * Contenido del formulario para editar el charset/collation de una base de datos existente.
 *
 * Diseñado para usarse dentro de un ModalBottomSheet (igual que [AddDatabaseFormContent]).
 * A diferencia del formulario de creación, el nombre de la base de datos NO es editable:
 * MySQL/MariaDB no soportan renombrar una base de datos vía `ALTER DATABASE`, solo charset
 * y collation. Se muestra como [IOSTextField] con `enabled = false` (input real deshabilitado,
 * no un `Text` suelto) para mantener la apariencia consistente con el resto del formulario.
 * Al abrir, preselecciona el charset/collation actuales de la base de datos.
 *
 * @param databaseName Nombre de la base de datos a editar (no editable — input deshabilitado)
 * @param currentCharset Charset actual de la base de datos (preselección inicial)
 * @param currentCollation Collation actual de la base de datos (preselección inicial)
 * @param onDismiss Callback para cerrar el bottom sheet
 * @param onDatabaseUpdated Callback cuando la actualización fue exitosa (para refrescar la lista)
 * @param snackbarHostState Estado del snackbar (provisto por el sheet parent)
 * @param viewModel ViewModel que maneja la carga de charsets/collations y el submit
 * @param modifier Modificador opcional
 *
 * @author gentle-ai
 * @date 2026-07-21
 */
@Composable
fun EditDatabaseFormContent(
    databaseName: String,
    currentCharset: String,
    currentCollation: String,
    onDismiss: () -> Unit = {},
    onDatabaseUpdated: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    viewModel: EditDatabaseViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // Estados del ViewModel
    val charsetState by viewModel.charsetState.collectAsState()
    val collationState by viewModel.collationState.collectAsState()
    val submitState by viewModel.submitState.collectAsState()

    // Estado del formulario
    var selectedCharset by remember { mutableStateOf<CharacterSet?>(null) }
    var selectedCollation by remember { mutableStateOf<Collation?>(null) }

    // Guards para preseleccionar los valores actuales una sola vez (no pisar la elección
    // del usuario en recomposiciones posteriores)
    var hasPreselectedCharset by remember { mutableStateOf(false) }
    var hasPreselectedCollation by remember { mutableStateOf(false) }

    // Strings localizados (extraídos para usar en LaunchedEffect)
    val successMessage = stringResource(R.string.update_database_success)
    val errorPermissionDenied = stringResource(R.string.error_update_permission_denied)
    val errorConnectionLost = stringResource(R.string.error_connection_lost)
    val errorGeneric = stringResource(R.string.error_update_database_failed)

    // Preseleccionar el charset actual de la base de datos apenas se cargan los charsets
    LaunchedEffect(charsetState) {
        val state = charsetState
        if (!hasPreselectedCharset && state is CharsetLoadState.Success) {
            selectedCharset = state.charsets.firstOrNull { it.name == currentCharset }
            hasPreselectedCharset = true
        }
    }

    // Cuando se selecciona un charset, cargar sus collations
    LaunchedEffect(selectedCharset) {
        if (selectedCharset != null) {
            viewModel.loadCollations(selectedCharset!!.name)
        } else {
            viewModel.clearCollations()
            selectedCollation = null
        }
    }

    // Preseleccionar la collation actual de la base de datos apenas se cargan las collations
    LaunchedEffect(collationState) {
        val state = collationState
        if (!hasPreselectedCollation && state is CollationLoadState.Success) {
            selectedCollation = state.collations.firstOrNull { it.name == currentCollation }
            hasPreselectedCollation = true
        }
    }

    // Manejar cambios de estado del submit
    LaunchedEffect(submitState) {
        when (submitState) {
            is UpdateDatabaseState.Success -> {
                // Cerrar el sheet primero
                onDismiss()

                // Refrescar la lista de databases
                onDatabaseUpdated()

                // Mostrar snackbar de éxito (no bloqueante, en coroutine separada)
                scope.launch {
                    snackbarHostState.showSnackbar(successMessage)
                }

                // Resetear estado para la próxima vez
                viewModel.resetSubmitState()
            }
            is UpdateDatabaseState.Error -> {
                val errorKey = (submitState as UpdateDatabaseState.Error).message
                val errorMessage = when (errorKey) {
                    "error_update_permission_denied" -> errorPermissionDenied
                    "error_connection_lost" -> errorConnectionLost
                    else -> errorGeneric
                }

                snackbarHostState.showSnackbar(errorMessage)
                viewModel.resetSubmitState()
            }
            else -> {
                // Idle o Submitting - no hacer nada
            }
        }
    }

    // El formulario es válido si hay al menos un charset o collation seleccionado
    // (ALTER DATABASE requiere al menos una cláusula)
    val isFormValid = selectedCharset != null || selectedCollation != null

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
                text = stringResource(R.string.edit_database_title),
                fontSize = LocalDesignTokens.current.largeTitleSize,
                fontWeight = LocalDesignTokens.current.largeTitleWeight,
                color = LocalDesignTokens.current.largeTitleColor,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Card: Información de la Database
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.database_header),
                    fontSize = LocalDesignTokens.current.labelSize,
                    color = LocalDesignTokens.current.textSecondary,
                    modifier = Modifier.padding(start = 16.dp)
                )

                IOSGroupedCard {
                    // Nombre — input deshabilitado (solo lectura): no renombrable vía
                    // ALTER DATABASE, pero se muestra como input real (no un Text suelto)
                    // para que se vea consistente con el resto del formulario.
                    IOSTextField(
                        value = databaseName,
                        onValueChange = {},
                        placeholder = databaseName,
                        showDivider = true,
                        enabled = false
                    )

                    // Dropdown de charset
                    when (val state = charsetState) {
                        is CharsetLoadState.Success -> {
                            IOSDropdownField(
                                value = selectedCharset,
                                onValueChange = { selectedCharset = it },
                                placeholder = stringResource(R.string.add_database_field_charset_hint),
                                items = state.charsets,
                                itemLabel = { it.name },
                                itemSubtitle = { it.description },
                                showDivider = true,
                                isLoading = false,
                                showFilter = true,
                                filterPlaceholder = "Search charset..."
                            )
                        }
                        is CharsetLoadState.Loading -> {
                            IOSDropdownField(
                                value = selectedCharset,
                                onValueChange = {},
                                placeholder = "Loading charsets...",
                                items = emptyList(),
                                itemLabel = { it.name },
                                showDivider = true,
                                isLoading = true,
                                enabled = false
                            )
                        }
                        is CharsetLoadState.Error -> {
                            IOSDropdownField(
                                value = selectedCharset,
                                onValueChange = {},
                                placeholder = "Error loading charsets",
                                items = emptyList(),
                                itemLabel = { it.name },
                                showDivider = true,
                                enabled = false
                            )
                        }
                    }

                    // Dropdown de collation (habilitado solo si charset está seleccionado)
                    when (val state = collationState) {
                        is CollationLoadState.Success -> {
                            IOSDropdownField(
                                value = selectedCollation,
                                onValueChange = { selectedCollation = it },
                                placeholder = stringResource(R.string.add_database_field_collation_hint),
                                items = state.collations,
                                itemLabel = { it.name },
                                showDivider = false,
                                isLoading = false,
                                showFilter = true,
                                filterPlaceholder = "Search collation...",
                                itemTrailing = { collation ->
                                    if (collation.isDefault) {
                                        Text(
                                            text = stringResource(R.string.default_option),
                                            fontSize = LocalDesignTokens.current.labelSize,
                                            color = LocalDesignTokens.current.accentPrimary,
                                            fontWeight = LocalDesignTokens.current.labelWeight
                                        )
                                    }
                                }
                            )
                        }
                        is CollationLoadState.Loading -> {
                            IOSDropdownField(
                                value = selectedCollation,
                                onValueChange = {},
                                placeholder = "Loading collations...",
                                items = emptyList(),
                                itemLabel = { it.name },
                                showDivider = false,
                                isLoading = true,
                                enabled = false
                            )
                        }
                        is CollationLoadState.Idle, is CollationLoadState.Error -> {
                            IOSDropdownField(
                                value = selectedCollation,
                                onValueChange = {},
                                placeholder = stringResource(R.string.add_database_field_collation_hint),
                                items = emptyList(),
                                itemLabel = { it.name },
                                showDivider = false,
                                enabled = selectedCharset != null
                            )
                        }
                    }
                }
            }

            // Save button (iOS style)
            IOSButton(
                text = if (submitState is UpdateDatabaseState.Submitting) {
                    stringResource(R.string.loading)
                } else {
                    stringResource(R.string.edit_database_button_save)
                },
                onClick = {
                    viewModel.updateDatabase(
                        name = databaseName,
                        charset = selectedCharset?.name,
                        collation = selectedCollation?.name
                    )
                },
                enabled = isFormValid && submitState !is UpdateDatabaseState.Submitting,
                style = IOSButtonStyle.Primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Overlay de loading durante el submit
        if (submitState is UpdateDatabaseState.Submitting) {
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
                    CircularProgressIndicator(
                        color = LocalDesignTokens.current.accentPrimary
                    )
                    Text(
                        text = stringResource(R.string.edit_database_updating),
                        fontSize = 16.sp,
                        color = LocalDesignTokens.current.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
