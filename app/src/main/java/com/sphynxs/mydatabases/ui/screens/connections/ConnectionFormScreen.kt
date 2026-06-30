package com.sphynxs.mydatabases.ui.screens.connections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.ui.components.DatabaseTypeSelector
import com.sphynxs.mydatabases.ui.components.ios.IOSButton
import com.sphynxs.mydatabases.ui.components.ios.IOSButtonStyle
import com.sphynxs.mydatabases.ui.components.ios.IOSGroupedCard
import com.sphynxs.mydatabases.ui.components.ios.IOSTextField

/**
 * Pantalla de formulario de conexión (crear/editar).
 *
 * Permite ingresar todos los datos de la conexión, probarla,
 * y guardarla.
 *
 * @param connectionId El ID de la conexión a editar (null para nueva)
 * @param preselectedType Tipo de base de datos preseleccionado (null para default MySQL)
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
    preselectedType: DatabaseType? = null,
    onNavigateBack: () -> Unit,
    viewModel: ConnectionFormViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val formState by viewModel.formState.collectAsState()
    val testState by viewModel.testState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Form fields state
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(preselectedType ?: DatabaseType.MYSQL) }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf((preselectedType?.defaultPort ?: 3306).toString()) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var useSSL by remember { mutableStateOf(false) }  // Default false - mayoría de DBs locales no usan SSL
    
    // Advanced connection state
    var showAdvancedConnection by remember { mutableStateOf(false) }
    
    // SSH Tunnel state
    var sshHost by remember { mutableStateOf("") }
    var sshPort by remember { mutableStateOf("22") }
    var sshUsername by remember { mutableStateOf("") }
    var sshPassword by remember { mutableStateOf("") }
    
    // Connection String state
    var connectionString by remember { mutableStateOf("") }

    // Load existing connection if editing
    LaunchedEffect(connectionId) {
        connectionId?.let { id ->
            val loadedConfig = viewModel.loadConnection(id)
            loadedConfig?.let { config ->
                name = config.name.trim()
                selectedType = config.type
                host = config.host.trim()
                port = config.port.toString()
                username = config.username.trim()
                password = config.password.trim()
                useSSL = config.useSSL
            }
        }
    }

    val saveSuccessMsg = stringResource(R.string.connection_save_success)
    val testSuccessMsg = stringResource(R.string.connection_test_success)

    // Handle form state changes
    LaunchedEffect(formState) {
        when (formState) {
            is ConnectionFormUiState.Saved -> {
                onNavigateBack()
                snackbarHostState.showSnackbar(saveSuccessMsg)
            }
            is ConnectionFormUiState.Error -> {
                val message = (formState as ConnectionFormUiState.Error).message
                snackbarHostState.showSnackbar(
                    message = "Save failed: $message",
                    duration = SnackbarDuration.Long
                )
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
                snackbarHostState.showSnackbar(
                    message = "Test failed: $message",
                    duration = SnackbarDuration.Long
                )
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = modifier.background(Color(0xFFF2F2F7)),
        topBar = {
            // iOS-style header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F2F7))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancelar",
                        tint = Color(0xFF007AFF)
                    )
                }
                Text(
                    stringResource(
                        if (connectionId == null) R.string.connection_form_title_new
                        else R.string.connection_form_title_edit
                    ),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                IconButton(
                    onClick = {
                        val config = createConnectionConfig(
                            id = connectionId,
                            name = name,
                            type = selectedType,
                            host = host,
                            port = port.toIntOrNull() ?: selectedType.defaultPort,
                            username = username,
                            password = password,
                            useSSL = useSSL
                        )
                        viewModel.saveConnection(config)
                    },
                    enabled = formState != ConnectionFormUiState.Saving
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Guardar",
                        tint = if (formState != ConnectionFormUiState.Saving) 
                            Color(0xFF007AFF) else Color(0xFF007AFF).copy(alpha = 0.3f)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F7))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Card 1: Identidad
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "IDENTIDAD",
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(start = 16.dp)
                )
                IOSGroupedCard {
                    IOSTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = stringResource(R.string.connection_field_name_hint),
                        showDivider = preselectedType == null
                    )
                    
                    // Database type selector solo si NO viene preseleccionado
                    if (preselectedType == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            DatabaseTypeSelector(
                                selected = selectedType,
                                onSelect = { selectedType = it }
                            )
                        }
                    }
                }
            }

            // Card 2: Conexión
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "CONEXIÓN",
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(start = 16.dp)
                )
                IOSGroupedCard {
                    IOSTextField(
                        value = host,
                        onValueChange = { host = it },
                        placeholder = stringResource(R.string.connection_field_host_hint)
                    )
                    IOSTextField(
                        value = port,
                        onValueChange = { port = it },
                        placeholder = stringResource(R.string.connection_field_port_hint),
                        keyboardType = KeyboardType.Number,
                        showDivider = false
                    )
                }
            }

            // Card 3: Autenticación
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "AUTENTICACIÓN",
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(start = 16.dp)
                )
                IOSGroupedCard {
                    IOSTextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = stringResource(R.string.connection_field_username_hint)
                    )
                    IOSTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = stringResource(R.string.connection_field_password_hint),
                        isPassword = true,
                        showDivider = false
                    )
                }
            }

            // Card 4: Conexión avanzada (toggle)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IOSGroupedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Conexión avanzada",
                            fontSize = 17.sp,
                            color = Color.Black
                        )
                        Switch(
                            checked = showAdvancedConnection,
                            onCheckedChange = { showAdvancedConnection = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF007AFF),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFE5E5EA)
                            )
                        )
                    }
                }
            }
            
            // Advanced connection sections (visible when toggle is ON)
            if (showAdvancedConnection) {
                // SSL Configuration
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "SSL/TLS",
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    IOSGroupedCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Usar SSL/TLS",
                                fontSize = 17.sp,
                                color = Color.Black
                            )
                            Switch(
                                checked = useSSL,
                                onCheckedChange = { useSSL = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF34C759),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFE5E5EA)
                                )
                            )
                        }
                    }
                }
                
                // SSH Tunnel Configuration
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "TÚNEL SSH",
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    IOSGroupedCard {
                        IOSTextField(
                            value = sshHost,
                            onValueChange = { sshHost = it },
                            placeholder = "SSH Host (ej: bastion.ejemplo.com)"
                        )
                        IOSTextField(
                            value = sshPort,
                            onValueChange = { sshPort = it },
                            placeholder = "SSH Port",
                            keyboardType = KeyboardType.Number
                        )
                        IOSTextField(
                            value = sshUsername,
                            onValueChange = { sshUsername = it },
                            placeholder = "SSH Usuario"
                        )
                        IOSTextField(
                            value = sshPassword,
                            onValueChange = { sshPassword = it },
                            placeholder = "SSH Contraseña",
                            isPassword = true,
                            showDivider = false
                        )
                    }
                }
                
                // Connection String Configuration
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "CONNECTION STRING",
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    IOSGroupedCard {
                        IOSTextField(
                            value = connectionString,
                            onValueChange = { connectionString = it },
                            placeholder = when (selectedType) {
                                DatabaseType.MYSQL -> "mysql://user:pass@host:3306/database"
                                DatabaseType.POSTGRESQL -> "postgresql://user:pass@host:5432/database"
                                DatabaseType.MARIADB -> "mariadb://user:pass@host:3306/database"
                                DatabaseType.SQLITE -> "sqlite:///path/to/database.db"
                            },
                            showDivider = false
                        )
                    }
                    Text(
                        "Si se proporciona, sobreescribe host, port, user y password",
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }

            // Botón de test
            IOSButton(
                text = stringResource(R.string.action_test),
                onClick = {
                    val config = createConnectionConfig(
                        id = connectionId,
                        name = name,
                        type = selectedType,
                        host = host,
                        port = port.toIntOrNull() ?: selectedType.defaultPort,
                        username = username,
                        password = password,
                        useSSL = useSSL
                    )
                    viewModel.testConnection(config)
                },
                style = IOSButtonStyle.Secondary,
                enabled = testState != ConnectionTestUiState.Testing
            )
            
            // Spacer para scroll bottom
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Helper para crear el ConnectionConfig desde los campos del formulario.
 * Database se deja vacío - se seleccionará después de conectar.
 */
private fun createConnectionConfig(
    id: String?,
    name: String,
    type: DatabaseType,
    host: String,
    port: Int,
    username: String,
    password: String,
    useSSL: Boolean
): ConnectionConfig {
    return ConnectionConfig(
        id = id ?: java.util.UUID.randomUUID().toString(),
        name = name,
        type = type,
        host = host,
        port = port,
        database = "",  // Se selecciona después de conectar
        username = username,
        password = password,
        useSSL = useSSL
    )
}
