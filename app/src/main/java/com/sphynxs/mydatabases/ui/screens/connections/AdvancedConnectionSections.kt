package com.sphynxs.mydatabases.ui.screens.connections

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.core.database.models.SSHAuthMethod
import com.sphynxs.mydatabases.ui.components.FilePicker
import com.sphynxs.mydatabases.ui.components.ios.IOSGroupedCard
import com.sphynxs.mydatabases.ui.components.ios.IOSTextField

/**
 * SSH Tunnel configuration section for advanced connection options.
 *
 * Provides UI for:
 * - Enable/disable SSH tunnel toggle
 * - SSH host, port, username
 * - Authentication method selection (password / private key)
 * - Conditional password field or private key file picker
 *
 * @param enabled Whether SSH tunnel is enabled
 * @param host SSH server hostname
 * @param port SSH server port
 * @param username SSH username
 * @param authMethod Authentication method (PASSWORD or PRIVATE_KEY)
 * @param password SSH password (for password auth)
 * @param privateKeyUri URI to private key file (for key auth)
 * @param privateKeyName Display name of selected private key file
 * @param onToggle Callback when SSH tunnel toggle changes
 * @param onHostChange Callback when host changes
 * @param onPortChange Callback when port changes
 * @param onUsernameChange Callback when username changes
 * @param onAuthMethodChange Callback when auth method changes
 * @param onPasswordChange Callback when password changes
 * @param onSelectPrivateKey Callback to trigger private key file picker
 *
 * @author israel-icm
 * @date 2026-06-30
 */
@Composable
fun SSHTunnelSection(
    enabled: Boolean,
    host: String,
    port: String,
    username: String,
    authMethod: SSHAuthMethod,
    password: String,
    privateKeyUri: Uri?,
    privateKeyName: String?,
    onToggle: (Boolean) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onAuthMethodChange: (SSHAuthMethod) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSelectPrivateKey: () -> Unit,
    modifier: Modifier = Modifier
) {
    IOSGroupedCard(
        title = stringResource(R.string.connection_ssh_tunnel_title),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Toggle to enable/disable SSH tunnel
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.connection_ssh_tunnel_enable),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle
                )
            }
            
            if (enabled) {
                Spacer(modifier = Modifier.height(4.dp))
                
                // SSH Host
                IOSTextField(
                    value = host,
                    onValueChange = onHostChange,
                    label = stringResource(R.string.connection_ssh_host),
                    placeholder = stringResource(R.string.connection_ssh_host_hint),
                    isError = host.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // SSH Port
                IOSTextField(
                    value = port,
                    onValueChange = onPortChange,
                    label = stringResource(R.string.connection_ssh_port),
                    placeholder = stringResource(R.string.connection_ssh_port_hint),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // SSH Username
                IOSTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = stringResource(R.string.connection_ssh_username),
                    placeholder = stringResource(R.string.connection_ssh_username_hint),
                    isError = username.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Authentication Method label
                Text(
                    text = stringResource(R.string.connection_ssh_auth_method),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Radio buttons for auth method
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Password auth
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = authMethod == SSHAuthMethod.PASSWORD,
                            onClick = { onAuthMethodChange(SSHAuthMethod.PASSWORD) }
                        )
                        Text(
                            text = stringResource(R.string.connection_ssh_auth_password),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    // Private key auth
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = authMethod == SSHAuthMethod.PRIVATE_KEY,
                            onClick = { onAuthMethodChange(SSHAuthMethod.PRIVATE_KEY) }
                        )
                        Text(
                            text = stringResource(R.string.connection_ssh_auth_private_key),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Conditional fields based on auth method
                when (authMethod) {
                    SSHAuthMethod.PASSWORD -> {
                        IOSTextField(
                            value = password,
                            onValueChange = onPasswordChange,
                            label = stringResource(R.string.connection_ssh_password),
                            placeholder = stringResource(R.string.connection_ssh_password_hint),
                            isPassword = true,
                            isError = password.isBlank(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    SSHAuthMethod.PRIVATE_KEY -> {
                        FilePicker(
                            label = stringResource(R.string.connection_ssh_private_key),
                            selectedFileName = privateKeyName,
                            onSelectFile = onSelectPrivateKey,
                            mimeTypes = arrayOf("*/*"),  // PEM files often have no specific MIME type
                            isError = privateKeyUri == null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * SSH Security Warning dialog.
 *
 * Displays a warning about disabled host key verification on Android.
 * User must acknowledge the security implications before continuing.
 *
 * @param onDismiss Callback when user cancels
 * @param onAccept Callback when user accepts and continues
 *
 * @author israel-icm
 * @date 2026-06-30
 */
@Composable
fun SSHSecurityWarningDialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.connection_ssh_security_warning_title),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text = stringResource(R.string.connection_ssh_security_warning_message),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text(stringResource(R.string.connection_ssh_security_warning_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
