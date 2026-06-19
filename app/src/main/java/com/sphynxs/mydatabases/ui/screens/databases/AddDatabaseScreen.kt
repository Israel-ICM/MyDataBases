package com.sphynxs.mydatabases.ui.screens.databases

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    viewModel: AddDatabaseViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    // ViewModel states
    val charsetState by viewModel.charsetState.collectAsState()
    val collationState by viewModel.collationState.collectAsState()

    // Form state
    var name by remember { mutableStateOf("") }
    var selectedCharset by remember { mutableStateOf<com.sphynxs.mydatabases.core.database.models.CharacterSet?>(null) }
    var selectedCollation by remember { mutableStateOf<com.sphynxs.mydatabases.core.database.models.Collation?>(null) }

    // Cuando se selecciona un charset, cargar sus collations
    LaunchedEffect(selectedCharset) {
        if (selectedCharset != null) {
            viewModel.loadCollations(selectedCharset!!.name)
        } else {
            viewModel.clearCollations()
            selectedCollation = null
        }
    }

    // Validation results
    val nameValidation = validateDatabaseName(name)

    // Form is valid when name is valid
    val isFormValid = nameValidation.isValid

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
            .padding(vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Title (iOS style)
        Text(
            text = stringResource(R.string.add_database_title),
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Card: Database Info
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "DATABASE",
                fontSize = 13.sp,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(start = 16.dp)
            )
            
            IOSGroupedCard {
                // Name field (required)
                IOSTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = stringResource(R.string.add_database_field_name_hint),
                    showDivider = true
                )
                
                // Charset dropdown (optional)
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
                            isLoading = false
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
                
                // Collation dropdown (optional, enabled only if charset selected)
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
                            itemTrailing = { collation ->
                                if (collation.isDefault) {
                                    Text(
                                        text = "Default",
                                        fontSize = 13.sp,
                                        color = Color(0xFF007AFF),
                                        fontWeight = FontWeight.Medium
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
                    fontSize = 13.sp,
                    color = Color(0xFFFF3B30),
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }

        // Create button (iOS style)
        IOSButton(
            text = stringResource(R.string.add_database_button_create),
            onClick = {
                // Spec: "Coming soon" snackbar, no SQL execution
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Coming soon - database creation will be wired in a follow-up"
                    )
                }
            },
            enabled = isFormValid,
            style = IOSButtonStyle.PRIMARY,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
