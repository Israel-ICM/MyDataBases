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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.ui.components.ios.IOSButton
import com.sphynxs.mydatabases.ui.components.ios.IOSButtonStyle
import com.sphynxs.mydatabases.ui.components.ios.IOSDropdownField
import com.sphynxs.mydatabases.ui.components.ios.IOSGroupedCard
import com.sphynxs.mydatabases.ui.components.ios.IOSTextField
import kotlinx.coroutines.launch

/**
 * Contenido del formulario para agregar una nueva base de datos.
 * 
 * Diseñado para usarse dentro de un ModalBottomSheet (igual que ConnectionFormScreen).
 * Form UI estilo iOS con tres campos: name (required), charset (dropdown), collation (dropdown).
 * Charsets y collations se cargan dinámicamente desde el servidor MySQL/MariaDB.
 * Submit muestra "Coming soon" — no SQL execution en este change.
 *
 * Spec: add-database-form
 * Phase: 5 (AddDatabaseScreen Implementation)
 *
 * @param connectionId ID de la conexión activa
 * @param onDismiss Callback para cerrar el bottom sheet
 * @param snackbarHostState Estado del snackbar (provisto por el sheet parent)
 * @param viewModel ViewModel que maneja la carga de charsets y collations
 * @param modifier Modificador opcional
 *
 * @author sdd-apply
 * @date 2026-06-19 (updated para estilo iOS)
 */
@Composable
fun AddDatabaseFormContent(
    connectionId: String,
    onDismiss: () -> Unit = {},
    onDatabaseCreated: (databaseName: String) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    viewModel: AddDatabaseViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    // Estados del ViewModel
    val charsetState by viewModel.charsetState.collectAsState()
    val collationState by viewModel.collationState.collectAsState()
    val submitState by viewModel.submitState.collectAsState()

    // Estado del formulario
    var name by remember { mutableStateOf("") }
    var selectedCharset by remember { mutableStateOf<com.sphynxs.mydatabases.core.database.models.CharacterSet?>(null) }
    var selectedCollation by remember { mutableStateOf<com.sphynxs.mydatabases.core.database.models.Collation?>(null) }

    // Strings localizados (extraídos para usar en LaunchedEffect)
    val successMessage = stringResource(R.string.create_database_success)
    val errorDatabaseExists = stringResource(R.string.error_database_exists)
    val errorPermissionDenied = stringResource(R.string.error_permission_denied)
    val errorConnectionLost = stringResource(R.string.error_connection_lost)
    val errorInvalidName = stringResource(R.string.error_invalid_database_name)
    val errorGeneric = stringResource(R.string.error_create_database_failed)
    
    // Cuando se selecciona un charset, cargar sus collations
    LaunchedEffect(selectedCharset) {
        if (selectedCharset != null) {
            viewModel.loadCollations(selectedCharset!!.name)
        } else {
            viewModel.clearCollations()
            selectedCollation = null
        }
    }
    
    // Manejar cambios de estado del submit
    LaunchedEffect(submitState) {
        android.util.Log.d("AddDatabase", "submitState changed: $submitState")
        when (submitState) {
            is CreateDatabaseState.Success -> {
                android.util.Log.d("AddDatabase", "Success! Closing sheet and navigating...")
                val dbName = name.trim()
                android.util.Log.d("AddDatabase", "Database name: $dbName")
                
                // Cerrar el sheet primero
                onDismiss()
                
                // Navegar a las tablas de la nueva database (antes del snackbar)
                android.util.Log.d("AddDatabase", "Calling onDatabaseCreated with: $dbName")
                onDatabaseCreated(dbName)
                
                // Mostrar snackbar de éxito (no bloqueante, en coroutine separada)
                scope.launch {
                    snackbarHostState.showSnackbar(successMessage)
                }
                
                // Resetear estado para la próxima vez
                viewModel.resetSubmitState()
            }
            is CreateDatabaseState.Error -> {
                // Mapear clave de error a string localizado
                val errorKey = (submitState as CreateDatabaseState.Error).message
                val errorMessage = when (errorKey) {
                    "error_database_exists" -> errorDatabaseExists
                    "error_permission_denied" -> errorPermissionDenied
                    "error_connection_lost" -> errorConnectionLost
                    "error_invalid_database_name" -> errorInvalidName
                    else -> errorGeneric
                }
                
                // Mostrar snackbar de error
                snackbarHostState.showSnackbar(errorMessage)
                
                // Resetear estado para que el usuario pueda reintentar
                viewModel.resetSubmitState()
            }
            else -> {
                // Idle o Submitting - no hacer nada
            }
        }
    }

    // Resultados de validación
    val nameValidation = validateDatabaseName(name)

    // El formulario es válido cuando el nombre es válido
    val isFormValid = nameValidation.isValid

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(com.sphynxs.mydatabases.ui.theme.DesignTokens.BackgroundPrimary)
                .padding(vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
        // Título (estilo iOS)
        Text(
            text = stringResource(R.string.add_database_title),
            fontSize = com.sphynxs.mydatabases.ui.theme.DesignTokens.LargeTitleSize,
            fontWeight = com.sphynxs.mydatabases.ui.theme.DesignTokens.LargeTitleWeight,
            color = com.sphynxs.mydatabases.ui.theme.DesignTokens.LargeTitleColor,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Card: Información de la Database
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.database_header),
                fontSize = com.sphynxs.mydatabases.ui.theme.DesignTokens.LabelSize,
                color = com.sphynxs.mydatabases.ui.theme.DesignTokens.TextSecondary,
                modifier = Modifier.padding(start = 16.dp)
            )
            
            IOSGroupedCard {
                // Campo de nombre (requerido)
                IOSTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = stringResource(R.string.add_database_field_name_hint),
                    showDivider = true
                )
                
                // Dropdown de charset (opcional)
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
                
                // Dropdown de collation (opcional, habilitado solo si charset está seleccionado)
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
                                        fontSize = com.sphynxs.mydatabases.ui.theme.DesignTokens.LabelSize,
                                        color = com.sphynxs.mydatabases.ui.theme.DesignTokens.AccentPrimary,
                                        fontWeight = com.sphynxs.mydatabases.ui.theme.DesignTokens.LabelWeight
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
            
            // Error message for name validation
            if (!nameValidation.isValid && name.isNotEmpty()) {
                Text(
                    text = nameValidation.errorMessage ?: "",
                    fontSize = com.sphynxs.mydatabases.ui.theme.DesignTokens.LabelSize,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }

        // Create button (iOS style)
        IOSButton(
            text = if (submitState is CreateDatabaseState.Submitting) {
                stringResource(R.string.loading)
            } else {
                stringResource(R.string.add_database_button_create)
            },
            onClick = {
                android.util.Log.d("AddDatabase", "Create button clicked! submitState before: $submitState")
                // Iniciar creación de database (el sheet se mantiene abierto con loading overlay)
                viewModel.createDatabase(
                    name = name,
                    charset = selectedCharset?.name,
                    collation = selectedCollation?.name
                )
                android.util.Log.d("AddDatabase", "createDatabase called")
            },
            enabled = isFormValid && submitState !is CreateDatabaseState.Submitting,
            style = IOSButtonStyle.Primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Overlay de loading durante el submit
        if (submitState is CreateDatabaseState.Submitting) {
            android.util.Log.d("AddDatabase", "Rendering loading overlay")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = com.sphynxs.mydatabases.ui.theme.DesignTokens.AccentPrimary
                    )
                    Text(
                        text = stringResource(R.string.add_database_creating),
                        fontSize = 16.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
