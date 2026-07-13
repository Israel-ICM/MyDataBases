package com.sphynxs.mydatabases.ui.screens.connections

import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.sphynxs.mydatabases.ui.components.FilePicker
import com.sphynxs.mydatabases.ui.components.getFileName
import com.sphynxs.mydatabases.ui.components.ios.IOSButton
import com.sphynxs.mydatabases.ui.components.ios.IOSButtonStyle
import com.sphynxs.mydatabases.ui.components.ios.IOSGroupedCard
import com.sphynxs.mydatabases.ui.components.ios.IOSTextField
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens

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
    
    // SSL Certificate state
    var sslMode by remember { mutableStateOf("REQUIRED") }
    var caCertificateUri by remember { mutableStateOf<Uri?>(null) }
    var caCertificateName by remember { mutableStateOf<String?>(null) }
    var clientCertificateUri by remember { mutableStateOf<Uri?>(null) }
    var clientCertificateName by remember { mutableStateOf<String?>(null) }
    var clientKeyUri by remember { mutableStateOf<Uri?>(null) }
    var clientKeyName by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    
    // SSH Tunnel state
    var sshTunnelEnabled by remember { mutableStateOf(false) }
    var sshHost by remember { mutableStateOf("") }
    var sshPort by remember { mutableStateOf("22") }
    var sshUsername by remember { mutableStateOf("") }
    var sshAuthMethod by remember { mutableStateOf(com.sphynxs.mydatabases.core.database.models.SSHAuthMethod.PASSWORD) }
    var sshPassword by remember { mutableStateOf("") }
    var sshPrivateKeyUri by remember { mutableStateOf<Uri?>(null) }
    var sshPrivateKeyName by remember { mutableStateOf<String?>(null) }
    var showSSHSecurityWarning by remember { mutableStateOf(false) }
    
    // Connection String state
    var connectionString by remember { mutableStateOf("") }

    // Load existing connection if editing
    LaunchedEffect(connectionId) {
        connectionId?.let { id ->
            try {
                val loadedConfig = viewModel.loadConnection(id)
                loadedConfig?.let { config ->
                name = config.name.trim()
                selectedType = config.type
                host = config.host.trim()
                port = config.port.toString()
                username = config.username.trim()
                password = config.password.trim()
                useSSL = config.useSSL
                
                // Load advanced connection settings
                if (config.hasAdvancedConfig) {
                    showAdvancedConnection = true
                }
                
                // Load SSL config
                config.sslConfig?.let { ssl ->
                    sslMode = when (ssl.mode) {
                        com.sphynxs.mydatabases.core.database.models.SSLMode.VERIFY_CA -> "VERIFY_CA"
                        com.sphynxs.mydatabases.core.database.models.SSLMode.VERIFY_IDENTITY -> "VERIFY_IDENTITY"
                        else -> "REQUIRED"
                    }
                    ssl.caCertificateUri?.let { uri ->
                        caCertificateUri = android.net.Uri.parse(uri)
                        caCertificateName = caCertificateUri?.getFileName(context)
                    }
                    ssl.clientCertificateUri?.let { uri ->
                        clientCertificateUri = android.net.Uri.parse(uri)
                        clientCertificateName = clientCertificateUri?.getFileName(context)
                    }
                    ssl.clientKeyUri?.let { uri ->
                        clientKeyUri = android.net.Uri.parse(uri)
                        clientKeyName = clientKeyUri?.getFileName(context)
                    }
                }
                
                // Load SSH tunnel config
                config.sshTunnelConfig?.let { ssh ->
                    sshTunnelEnabled = ssh.enabled
                    sshHost = ssh.host
                    sshPort = ssh.port.toString()
                    sshUsername = ssh.username
                    sshAuthMethod = ssh.authMethod
                    sshPassword = ssh.password
                    ssh.privateKeyUri?.let { uri ->
                        sshPrivateKeyUri = android.net.Uri.parse(uri)
                        sshPrivateKeyName = sshPrivateKeyUri?.getFileName(context)
                    }
                }
                
                // Load connection string
                config.connectionString?.let { cs ->
                    connectionString = cs
                }
            }
            } catch (e: Exception) {
                android.util.Log.e("ConnectionFormScreen", "Error loading connection $id", e)
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
        modifier = modifier.background(LocalDesignTokens.current.backgroundPrimary),
        topBar = {
            // iOS-style header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LocalDesignTokens.current.backgroundPrimary)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancelar",
                        tint = LocalDesignTokens.current.accentPrimary
                    )
                }
                Text(
                    stringResource(
                        if (connectionId == null) R.string.connection_form_title_new
                        else R.string.connection_form_title_edit
                    ),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalDesignTokens.current.textPrimary
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
                            useSSL = useSSL,
                            sslMode = if (showAdvancedConnection) sslMode else null,
                            caCertificateUri = caCertificateUri?.toString(),
                            clientCertificateUri = clientCertificateUri?.toString(),
                            clientKeyUri = clientKeyUri?.toString(),
                            sshTunnelEnabled = sshTunnelEnabled,
                            sshHost = if (showAdvancedConnection && sshTunnelEnabled) sshHost else null,
                            sshPort = if (showAdvancedConnection && sshTunnelEnabled) sshPort.toIntOrNull() else null,
                            sshUsername = if (showAdvancedConnection && sshTunnelEnabled) sshUsername else null,
                            sshAuthMethod = sshAuthMethod,
                            sshPassword = if (showAdvancedConnection && sshTunnelEnabled) sshPassword else null,
                            sshPrivateKeyUri = sshPrivateKeyUri?.toString(),
                            connectionString = if (showAdvancedConnection) connectionString else null
                        )
                        viewModel.saveConnection(config)
                    },
                    enabled = formState != ConnectionFormUiState.Saving
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Guardar",
                        tint = if (formState != ConnectionFormUiState.Saving) 
                            LocalDesignTokens.current.accentPrimary else LocalDesignTokens.current.accentPrimary.copy(alpha = 0.3f)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalDesignTokens.current.backgroundPrimary)
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
                    color = LocalDesignTokens.current.textSecondary,
                    modifier = Modifier.padding(start = 16.dp)
                )
                IOSGroupedCard {
                    IOSTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = stringResource(R.string.connection_field_name_hint),
                        showDivider = false
                    )
                }
            }

            // Card 2: Conexión
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "CONEXIÓN",
                    fontSize = 13.sp,
                    color = LocalDesignTokens.current.textSecondary,
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
                    color = LocalDesignTokens.current.textSecondary,
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
                            .background(LocalDesignTokens.current.surfacePrimary)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Conexión avanzada",
                            fontSize = 17.sp,
                            color = LocalDesignTokens.current.textPrimary
                        )
                        Switch(
                            checked = showAdvancedConnection,
                            onCheckedChange = { showAdvancedConnection = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = LocalDesignTokens.current.accentPrimary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = LocalDesignTokens.current.separator
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
                        color = LocalDesignTokens.current.textSecondary,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    IOSGroupedCard {
                        // SSL Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LocalDesignTokens.current.surfacePrimary)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Usar SSL/TLS",
                                fontSize = 17.sp,
                                color = LocalDesignTokens.current.textPrimary
                            )
                            Switch(
                                checked = useSSL,
                                onCheckedChange = { useSSL = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = LocalDesignTokens.current.accentSuccess,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = LocalDesignTokens.current.separator
                                )
                            )
                        }
                        
                        // SSL Mode Dropdown (only visible when SSL is enabled)
                        if (useSSL) {
                            // SSL Mode selector
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LocalDesignTokens.current.surfacePrimary)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    "Modo SSL",
                                    fontSize = 13.sp,
                                    color = LocalDesignTokens.current.textSecondary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("REQUIRED", "VERIFY_CA", "VERIFY_IDENTITY").forEach { mode ->
                                        androidx.compose.material3.FilterChip(
                                            selected = sslMode == mode,
                                            onClick = { sslMode = mode },
                                            label = { 
                                                Text(
                                                    text = mode.replace("_", " "),
                                                    fontSize = 13.sp
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                            
                            // CA Certificate
                            FilePicker(
                                onFileSelected = { uri ->
                                    caCertificateUri = uri
                                    caCertificateName = uri.getFileName(context)
                                },
                                mimeTypes = arrayOf("application/x-pem-file", "application/x-x509-ca-cert", "*/*")
                            ) { launchPicker ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(LocalDesignTokens.current.surfacePrimary)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "CA Certificate",
                                            fontSize = 17.sp,
                                            color = LocalDesignTokens.current.textPrimary
                                        )
                                        if (caCertificateName != null) {
                                            Text(
                                                text = caCertificateName ?: "",
                                                fontSize = 13.sp,
                                                color = LocalDesignTokens.current.textSecondary
                                            )
                                        }
                                    }
                                    TextButton(
                                        onClick = { launchPicker() }
                                    ) {
                                        Text(
                                            if (caCertificateName == null) "Seleccionar" else "Cambiar",
                                            color = LocalDesignTokens.current.accentPrimary
                                        )
                                    }
                                }
                            }
                            
                            // Client Certificate (optional)
                            FilePicker(
                                onFileSelected = { uri ->
                                    clientCertificateUri = uri
                                    clientCertificateName = uri.getFileName(context)
                                },
                                mimeTypes = arrayOf("application/x-pem-file", "application/x-x509-ca-cert", "*/*")
                            ) { launchPicker ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(LocalDesignTokens.current.surfacePrimary)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Client Certificate (opcional)",
                                            fontSize = 17.sp,
                                            color = LocalDesignTokens.current.textPrimary
                                        )
                                        if (clientCertificateName != null) {
                                            Text(
                                                text = clientCertificateName ?: "",
                                                fontSize = 13.sp,
                                                color = LocalDesignTokens.current.textSecondary
                                            )
                                        }
                                    }
                                    TextButton(
                                        onClick = { launchPicker() }
                                    ) {
                                        Text(
                                            if (clientCertificateName == null) "Seleccionar" else "Cambiar",
                                            color = LocalDesignTokens.current.accentPrimary
                                        )
                                    }
                                }
                            }
                            
                            // Client Key (optional)
                            FilePicker(
                                onFileSelected = { uri ->
                                    clientKeyUri = uri
                                    clientKeyName = uri.getFileName(context)
                                },
                                mimeTypes = arrayOf("application/x-pem-file", "application/pkcs8", "*/*")
                            ) { launchPicker ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(LocalDesignTokens.current.surfacePrimary)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Client Key (opcional)",
                                            fontSize = 17.sp,
                                            color = LocalDesignTokens.current.textPrimary
                                        )
                                        if (clientKeyName != null) {
                                            Text(
                                                text = clientKeyName ?: "",
                                                fontSize = 13.sp,
                                                color = LocalDesignTokens.current.textSecondary
                                            )
                                        }
                                    }
                                    TextButton(
                                        onClick = { launchPicker() }
                                    ) {
                                        Text(
                                            if (clientKeyName == null) "Seleccionar" else "Cambiar",
                                            color = LocalDesignTokens.current.accentPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // SSH Tunnel Configuration
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "TÚNEL SSH",
                        fontSize = 13.sp,
                        color = LocalDesignTokens.current.textSecondary,
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
                
                // SSH Tunnel Configuration
                FilePicker(
                    onFileSelected = { uri ->
                        sshPrivateKeyUri = uri
                        sshPrivateKeyName = uri.getFileName(context)
                    },
                    mimeTypes = arrayOf("*/*")  // PEM files often have no specific MIME type
                ) { launchSshKeyPicker ->
                    SSHTunnelSection(
                        enabled = sshTunnelEnabled,
                        host = sshHost,
                        port = sshPort,
                        username = sshUsername,
                        authMethod = sshAuthMethod,
                        password = sshPassword,
                        privateKeyUri = sshPrivateKeyUri,
                        privateKeyName = sshPrivateKeyName,
                        onToggle = { enabled ->
                            sshTunnelEnabled = enabled
                            if (enabled) {
                                showSSHSecurityWarning = true
                            }
                        },
                        onHostChange = { sshHost = it },
                        onPortChange = { sshPort = it },
                        onUsernameChange = { sshUsername = it },
                        onAuthMethodChange = { sshAuthMethod = it },
                        onPasswordChange = { sshPassword = it },
                        onSelectPrivateKey = { launchSshKeyPicker() }
                    )
                }
                
                // Connection String Configuration
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "CONNECTION STRING",
                        fontSize = 13.sp,
                        color = LocalDesignTokens.current.textSecondary,
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
                        color = LocalDesignTokens.current.textSecondary,
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
                        useSSL = useSSL,
                        sslMode = if (showAdvancedConnection) sslMode else null,
                        caCertificateUri = caCertificateUri?.toString(),
                        clientCertificateUri = clientCertificateUri?.toString(),
                        clientKeyUri = clientKeyUri?.toString(),
                        sshTunnelEnabled = sshTunnelEnabled,
                        sshHost = if (showAdvancedConnection && sshTunnelEnabled) sshHost else null,
                        sshPort = if (showAdvancedConnection && sshTunnelEnabled) sshPort.toIntOrNull() else null,
                        sshUsername = if (showAdvancedConnection && sshTunnelEnabled) sshUsername else null,
                        sshAuthMethod = sshAuthMethod,
                        sshPassword = if (showAdvancedConnection && sshTunnelEnabled) sshPassword else null,
                        sshPrivateKeyUri = sshPrivateKeyUri?.toString(),
                        connectionString = if (showAdvancedConnection) connectionString else null
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
    
    // SSH Security Warning Dialog
    if (showSSHSecurityWarning) {
        SSHSecurityWarningDialog(
            onDismiss = {
                showSSHSecurityWarning = false
                sshTunnelEnabled = false  // Disable SSH if user cancels
            },
            onAccept = {
                showSSHSecurityWarning = false
                // Keep SSH enabled
            }
        )
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
    useSSL: Boolean,
    sslMode: String? = null,
    caCertificateUri: String? = null,
    clientCertificateUri: String? = null,
    clientKeyUri: String? = null,
    sshTunnelEnabled: Boolean = false,
    sshHost: String? = null,
    sshPort: Int? = null,
    sshUsername: String? = null,
    sshAuthMethod: com.sphynxs.mydatabases.core.database.models.SSHAuthMethod = com.sphynxs.mydatabases.core.database.models.SSHAuthMethod.PASSWORD,
    sshPassword: String? = null,
    sshPrivateKeyUri: String? = null,
    connectionString: String? = null
): ConnectionConfig {
    // Crear SSL config si SSL está habilitado y hay configuración avanzada
    val sslConfig = if (useSSL && (sslMode != null || caCertificateUri != null)) {
        com.sphynxs.mydatabases.core.database.models.SSLConfig(
            mode = when (sslMode) {
                "VERIFY_CA" -> com.sphynxs.mydatabases.core.database.models.SSLMode.VERIFY_CA
                "VERIFY_IDENTITY" -> com.sphynxs.mydatabases.core.database.models.SSLMode.VERIFY_IDENTITY
                else -> com.sphynxs.mydatabases.core.database.models.SSLMode.REQUIRED
            },
            caCertificateUri = caCertificateUri,
            clientCertificateUri = clientCertificateUri,
            clientKeyUri = clientKeyUri
        )
    } else null
    
    // Crear SSH tunnel config si está habilitado
    val sshTunnelConfig = if (sshTunnelEnabled && !sshHost.isNullOrBlank()) {
        com.sphynxs.mydatabases.core.database.models.SSHTunnelConfig(
            enabled = true,
            host = sshHost,
            port = sshPort ?: 22,
            username = sshUsername ?: "",
            authMethod = sshAuthMethod,
            password = sshPassword ?: "",
            privateKeyUri = sshPrivateKeyUri
        )
    } else null
    
    return ConnectionConfig(
        id = id ?: java.util.UUID.randomUUID().toString(),
        name = name,
        type = type,
        host = host,
        port = port,
        database = "",  // Se selecciona después de conectar
        username = username,
        password = password,
        useSSL = useSSL,
        sslConfig = sslConfig,
        sshTunnelConfig = sshTunnelConfig,
        connectionString = if (!connectionString.isNullOrBlank()) connectionString else null
    )
}
