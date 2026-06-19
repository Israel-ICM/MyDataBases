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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.R
import kotlinx.coroutines.launch

/**
 * Pantalla para agregar una nueva base de datos.
 * 
 * Form UI con tres campos: name (required), charset (optional), collation (optional).
 * Submit muestra "Coming soon" — no SQL execution en este change.
 *
 * Spec: add-database-form
 * Phase: 5 (AddDatabaseScreen Implementation)
 *
 * @param connectionId ID de la conexión activa
 * @param onNavigateBack Callback para navegar de vuelta
 * @param modifier Modificador opcional
 *
 * @author sdd-apply
 * @date 2026-06-19
 */
@Composable
fun AddDatabaseScreen(
    connectionId: String,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Form state
    var name by remember { mutableStateOf("") }
    var charset by remember { mutableStateOf("") }
    var collation by remember { mutableStateOf("") }

    // Validation results
    val nameValidation = validateDatabaseName(name)
    val charsetValidation = validateOptionalField(charset)
    val collationValidation = validateOptionalField(collation)

    // Form is valid when all fields are valid
    val isFormValid = nameValidation.isValid && charsetValidation.isValid && collationValidation.isValid

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
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

                // Charset field (optional)
                TextField(
                    value = charset,
                    onValueChange = { charset = it },
                    label = { Text(stringResource(R.string.add_database_field_charset)) },
                    placeholder = { Text(stringResource(R.string.add_database_field_charset_hint)) },
                    isError = !charsetValidation.isValid && charset.isNotEmpty(),
                    supportingText = {
                        if (!charsetValidation.isValid && charset.isNotEmpty()) {
                            Text(charsetValidation.errorMessage ?: "")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Character set field"
                        },
                    singleLine = true
                )

                // Collation field (optional)
                TextField(
                    value = collation,
                    onValueChange = { collation = it },
                    label = { Text(stringResource(R.string.add_database_field_collation)) },
                    placeholder = { Text(stringResource(R.string.add_database_field_collation_hint)) },
                    isError = !collationValidation.isValid && collation.isNotEmpty(),
                    supportingText = {
                        if (!collationValidation.isValid && collation.isNotEmpty()) {
                            Text(collationValidation.errorMessage ?: "")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Collation field"
                        },
                    singleLine = true
                )

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
            }
        }
    }
}
