package com.sphynxs.mydatabases.ui.screens.connections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.ui.components.DatabaseTypeSelector
import com.sphynxs.mydatabases.ui.components.SectionCard
import kotlinx.coroutines.launch

/**
 * Pantalla de formulario de conexión (crear/editar).
 *
 * Permite ingresar todos los datos de la conexión, probarla,
 * y guardarla.
 *
 * @param connectionId El ID de la conexión a editar (null para nueva)
 * @param onNavigateBack Callback para volver atrás después de guardar
 * @param viewModel El ViewModel con la lógica de estado
 * @param modifier Modificador opcional
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionFormScreen(
    connectionId: String?,
    onNavigateBack: () -> Unit,
    viewModel: ConnectionFormViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val formState by viewModel.formState.collectAsState()
    val testState by viewModel.testState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Form fields state
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(DatabaseType.MYSQL) }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("3306") }
    var database by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Load existing connection if editing
    LaunchedEffect(connectionId) {
        connectionId?.let { id ->
            viewModel.loadConnection(id)?.let { config ->
                name = config.name
                selectedType = config.type
                host = config.host
                port = config.port.toString()
                database = config.database
                username = config.username
                password = config.password
            }
        }
    }

    val saveSuccessMsg = stringResource(R.string.connection_save_success)
    val testSuccessMsg = stringResource(R.string.connection_test_success)

    // Handle form state changes
    LaunchedEffect(formState) {
        when (formState) {
            is ConnectionFormUiState.Saved -> {
                snackbarHostState.showSnackbar(saveSuccessMsg)
                onNavigateBack()
            }
            is ConnectionFormUiState.Error -> {
                val message = (formState as ConnectionFormUiState.Error).message
                snackbarHostState.showSnackbar("Save failed: $message")
            }
            else -> {}
        }
    }

    // Handle test state changes
    LaunchedEffect(testState) {
        when (testState) {
            is ConnectionTestUiState.Success -> {
                snackbarHostState.showSnackbar(testSuccessMsg)
            }
            is ConnectionTestUiState.Error -> {
                val message = (testState as ConnectionTestUiState.Error).message
                snackbarHostState.showSnackbar("Test failed: $message")
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (connectionId == null) R.string.connection_form_title_new
                            else R.string.connection_form_title_edit
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Sección 1: Identidad
            SectionCard(title = "Identidad") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.connection_field_name)) },
                    placeholder = { Text(stringResource(R.string.connection_field_name_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                DatabaseTypeSelector(
                    selected = selectedType,
                    onSelect = { selectedType = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección 2: Conexión
            SectionCard(title = "Conexión") {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(stringResource(R.string.connection_field_host)) },
                    placeholder = { Text(stringResource(R.string.connection_field_host_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text(stringResource(R.string.connection_field_port)) },
                        placeholder = { Text(stringResource(R.string.connection_field_port_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = database,
                    onValueChange = { database = it },
                    label = { Text(stringResource(R.string.connection_field_database)) },
                    placeholder = { Text(stringResource(R.string.connection_field_database_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección 3: Autenticación
            SectionCard(title = "Autenticación") {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.connection_field_username)) },
                    placeholder = { Text(stringResource(R.string.connection_field_username_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.connection_field_password)) },
                    placeholder = { Text(stringResource(R.string.connection_field_password_hint)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility
                                else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Ocultar contraseña"
                                else "Mostrar contraseña"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        val config = createConnectionConfig(
                            id = connectionId,
                            name = name,
                            type = selectedType,
                            host = host,
                            port = port.toIntOrNull() ?: 3306,
                            database = database,
                            username = username,
                            password = password
                        )
                        viewModel.testConnection(config)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = testState != ConnectionTestUiState.Testing
                ) {
                    Text(stringResource(R.string.action_test))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        val config = createConnectionConfig(
                            id = connectionId,
                            name = name,
                            type = selectedType,
                            host = host,
                            port = port.toIntOrNull() ?: 3306,
                            database = database,
                            username = username,
                            password = password
                        )
                        viewModel.saveConnection(config)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = formState != ConnectionFormUiState.Saving
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

/**
 * Helper para crear el ConnectionConfig desde los campos del formulario.
 */
private fun createConnectionConfig(
    id: String?,
    name: String,
    type: DatabaseType,
    host: String,
    port: Int,
    database: String,
    username: String,
    password: String
): ConnectionConfig {
    return ConnectionConfig(
        id = id ?: java.util.UUID.randomUUID().toString(),
        name = name,
        type = type,
        host = host,
        port = port,
        database = database,
        username = username,
        password = password
    )
}
