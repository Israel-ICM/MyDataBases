package com.sphynxs.mydatabases.ui.screens.databases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sphynxs.mydatabases.R
import kotlinx.coroutines.launch

/**
 * Contenido del formulario para agregar una nueva base de datos.
 * 
 * Diseñado para usarse dentro de un ModalBottomSheet (igual que ConnectionFormScreen).
 * Form UI con tres campos: name (required), charset (dropdown), collation (dropdown).
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
 * @date 2026-06-19 (updated para dropdowns dinámicos)
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedCharset by remember { mutableStateOf<String?>(null) }
    var selectedCollation by remember { mutableStateOf<String?>(null) }
    
    // Dropdown expansion states
    var charsetExpanded by remember { mutableStateOf(false) }
    var collationExpanded by remember { mutableStateOf(false) }

    // Cuando se selecciona un charset, cargar sus collations
    LaunchedEffect(selectedCharset) {
        if (selectedCharset != null) {
            viewModel.loadCollations(selectedCharset!!)
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
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = stringResource(R.string.add_database_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Name field (required)
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.add_database_field_name)) },
            placeholder = { Text(stringResource(R.string.add_database_field_name_hint)) },
            isError = !nameValidation.isValid && name.isNotEmpty(),
            supportingText = {
                if (!nameValidation.isValid && name.isNotEmpty()) {
                    Text(nameValidation.errorMessage ?: "")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Database name field"
                },
            singleLine = true
        )

        // Charset dropdown (optional)
        ExposedDropdownMenuBox(
            expanded = charsetExpanded,
            onExpandedChange = { charsetExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = selectedCharset ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.add_database_field_charset)) },
                placeholder = { Text(stringResource(R.string.add_database_field_charset_hint)) },
                trailingIcon = {
                    if (charsetState is CharsetLoadState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.padding(12.dp))
                    } else {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = charsetExpanded)
                    }
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Character set dropdown"
                    }
            )
            
            ExposedDropdownMenu(
                expanded = charsetExpanded,
                onDismissRequest = { charsetExpanded = false }
            ) {
                when (charsetState) {
                    is CharsetLoadState.Success -> {
                        val charsets = (charsetState as CharsetLoadState.Success).charsets
                        charsets.forEach { charset ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(charset.name)
                                        Text(
                                            text = charset.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedCharset = charset.name
                                    charsetExpanded = false
                                }
                            )
                        }
                    }
                    is CharsetLoadState.Error -> {
                        DropdownMenuItem(
                            text = { Text((charsetState as CharsetLoadState.Error).message) },
                            onClick = { charsetExpanded = false },
                            enabled = false
                        )
                    }
                    else -> {}
                }
            }
        }

        // Collation dropdown (optional, habilitado solo si hay charset seleccionado)
        ExposedDropdownMenuBox(
            expanded = collationExpanded,
            onExpandedChange = { 
                if (selectedCharset != null) {
                    collationExpanded = it
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = selectedCollation ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = selectedCharset != null,
                label = { Text(stringResource(R.string.add_database_field_collation)) },
                placeholder = { Text(stringResource(R.string.add_database_field_collation_hint)) },
                trailingIcon = {
                    if (collationState is CollationLoadState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.padding(12.dp))
                    } else if (selectedCharset != null) {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = collationExpanded)
                    }
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Collation dropdown"
                    }
            )
            
            ExposedDropdownMenu(
                expanded = collationExpanded,
                onDismissRequest = { collationExpanded = false }
            ) {
                when (collationState) {
                    is CollationLoadState.Success -> {
                        val collations = (collationState as CollationLoadState.Success).collations
                        collations.forEach { collation ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = collation.name,
                                        fontWeight = if (collation.isDefault) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedCollation = collation.name
                                    collationExpanded = false
                                },
                                trailingIcon = {
                                    if (collation.isDefault) {
                                        Text(
                                            text = "Default",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                    is CollationLoadState.Error -> {
                        DropdownMenuItem(
                            text = { Text((collationState as CollationLoadState.Error).message) },
                            onClick = { collationExpanded = false },
                            enabled = false
                        )
                    }
                    else -> {}
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Create button
        Button(
            onClick = {
                // Spec: "Coming soon" snackbar, no SQL execution
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Coming soon - database creation will be wired in a follow-up"
                    )
                }
            },
            enabled = isFormValid,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = if (isFormValid) {
                        "Create database button, enabled"
                    } else {
                        "Create database button, disabled"
                    }
                }
        ) {
            Text(stringResource(R.string.add_database_button_create))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
