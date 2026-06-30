# SSH Tunneling Technical Design

**Change**: ssh-tunneling  
**Version**: 1.0  
**Status**: PLANNING  
**Author**: israel-icm  
**Created**: 2026-06-30  

## Architecture Overview

### Component Diagram

```
┌─────────────────────────────────────────────────┐
│           Presentation Layer                     │
│  ┌────────────────────────────────────────────┐ │
│  │ ConnectionFormScreen                       │ │
│  │  - SSH tunnel toggle                       │ │
│  │  - SSH config fields                       │ │
│  │  - Private key picker                      │ │
│  └────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│              Domain Layer                        │
│  ┌────────────────────────────────────────────┐ │
│  │ SSHTunnelManager                           │ │
│  │  + connect()                               │ │
│  │  + disconnect()                            │ │
│  │  + isActive()                              │ │
│  │  + getLocalPort()                          │ │
│  └────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────┐ │
│  │ SSHKeyReader                               │ │
│  │  + readPrivateKey()                        │ │
│  │  + isEncrypted()                           │ │
│  │  + parseKeyFormat()                        │ │
│  └────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│               Data Layer                         │
│  ┌────────────────────────────────────────────┐ │
│  │ MySQLConnectionPool                        │ │
│  │  - sshTunnelManager: SSHTunnelManager?     │ │
│  │  + getConnection() (with SSH tunnel)       │ │
│  │  + close() (cleanup tunnel)                │ │
│  └────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────┐ │
│  │ ConnectionConfig                           │ │
│  │  + sshTunnelConfig: SSHTunnelConfig?       │ │
│  └────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│          External Dependencies                   │
│  ┌────────────────────────────────────────────┐ │
│  │ JSch Library (com.github.mwiede:jsch)      │ │
│  │  - Session                                 │ │
│  │  - ChannelDirectTCPIP (port forward)       │ │
│  └────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

### Connection Flow Sequence

```
User -> ConnectionFormScreen: Tap "Connect"
ConnectionFormScreen -> ViewModel: connect(config)
ViewModel -> DatabaseRepository: connect(config)
DatabaseRepository -> DatabaseEngineFactory: create(type, context)
DatabaseEngineFactory -> MySQLEngine: new(context)
DatabaseRepository -> MySQLEngine: connect(config)

alt SSH Tunnel Enabled
    MySQLEngine -> MySQLConnectionPool: new(config, context)
    MySQLConnectionPool -> SSHTunnelManager: new(sshConfig, context)
    MySQLConnectionPool -> SSHTunnelManager: connect()
    SSHTunnelManager -> JSch: createSession(host, port, username)
    
    alt Password Auth
        SSHTunnelManager -> JSch Session: setPassword(password)
    else Private Key Auth
        SSHTunnelManager -> SSHKeyReader: readPrivateKey(uri)
        SSHKeyReader -> ContentResolver: openInputStream(uri)
        SSHKeyReader --> SSHTunnelManager: privateKeyBytes
        SSHTunnelManager -> JSch: addIdentity(privateKeyBytes)
    end
    
    SSHTunnelManager -> JSch Session: connect()
    SSHTunnelManager -> JSch Session: setPortForwardingL(localPort, dbHost, dbPort)
    SSHTunnelManager --> MySQLConnectionPool: localPort
    
    MySQLConnectionPool -> MySQLConnectionPool: updateJdbcUrl(localhost, localPort)
end

MySQLConnectionPool -> MySQLSSLConfigBuilder: applyToProperties(props)
MySQLConnectionPool -> DriverManager: getConnection(jdbcUrl, props)
DriverManager --> MySQLConnectionPool: Connection
MySQLConnectionPool --> MySQLEngine: Connection
MySQLEngine --> DatabaseRepository: Result.success(Connection)
DatabaseRepository --> ViewModel: Result.success(Connection)
ViewModel -> UI: Update state (Connected)
```

## Data Structures

### SSHTunnelConfig

```kotlin
package com.sphynxs.mydatabases.core.database.models

/**
 * SSH tunnel configuration for database connections.
 *
 * @property enabled Whether SSH tunnel is enabled
 * @property host SSH server hostname or IP
 * @property port SSH server port (default: 22)
 * @property username SSH username
 * @property authMethod Authentication method (password or private key)
 * @property password SSH password (required if authMethod = PASSWORD)
 * @property privateKeyUri URI to private key file (required if authMethod = PRIVATE_KEY)
 */
data class SSHTunnelConfig(
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int = 22,
    val username: String = "",
    val authMethod: SSHAuthMethod = SSHAuthMethod.PASSWORD,
    val password: String = "",
    val privateKeyUri: String? = null
)

enum class SSHAuthMethod {
    PASSWORD,
    PRIVATE_KEY
}
```

### ConnectionConfig Update

```kotlin
data class ConnectionConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: DatabaseType,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val database: String = "",
    val useSSL: Boolean = false,
    val connectionTimeout: Int = 10000,
    val readTimeout: Int = 30000,
    
    // Advanced connection options (v1.0)
    val sslConfig: SSLConfig? = null,
    val sshTunnelConfig: SSHTunnelConfig? = null,  // NEW
    val connectionString: String? = null
)
```

## Class Implementations

### SSHTunnelManager

**File**: `app/src/main/java/com/sphynxs/mydatabases/core/database/ssh/SSHTunnelManager.kt`

```kotlin
package com.sphynxs.mydatabases.core.database.ssh

import android.content.Context
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.sphynxs.mydatabases.core.database.models.SSHAuthMethod
import com.sphynxs.mydatabases.core.database.models.SSHTunnelConfig
import java.util.Properties

/**
 * Manages SSH tunnel lifecycle for database connections.
 *
 * Uses JSch library to establish SSH sessions and local port forwarding.
 * Thread-safe for concurrent connections.
 *
 * @param config SSH tunnel configuration
 * @param context Android context for reading private keys
 */
class SSHTunnelManager(
    private val config: SSHTunnelConfig,
    private val context: Context
) {
    private var session: Session? = null
    private var localPort: Int? = null
    
    /**
     * Establishes SSH tunnel and returns local port for JDBC connection.
     *
     * @param remoteHost Database server hostname (for port forwarding)
     * @param remotePort Database server port (for port forwarding)
     * @return Local port number for JDBC connection
     * @throws SSHTunnelException if connection fails
     */
    fun connect(remoteHost: String, remotePort: Int): Int {
        // Implementation details in tasks
    }
    
    /**
     * Closes SSH tunnel and releases resources.
     */
    fun disconnect() {
        // Implementation details in tasks
    }
    
    /**
     * Checks if SSH tunnel is active.
     */
    fun isActive(): Boolean {
        return session?.isConnected == true
    }
    
    /**
     * Gets the local port for JDBC connection.
     */
    fun getLocalPort(): Int? = localPort
}
```

### SSHKeyReader

**File**: `app/src/main/java/com/sphynxs/mydatabases/core/database/ssh/SSHKeyReader.kt`

```kotlin
package com.sphynxs.mydatabases.core.database.ssh

import android.content.Context
import android.net.Uri

/**
 * Reads and parses SSH private keys from Android URIs.
 *
 * Supports:
 * - PEM format (-----BEGIN RSA PRIVATE KEY-----)
 * - OpenSSH format (-----BEGIN OPENSSH PRIVATE KEY-----)
 *
 * @author israel-icm
 * @date 2026-06-30
 */
object SSHKeyReader {
    
    /**
     * Reads private key from URI.
     *
     * @param context Android context
     * @param uri URI to private key file
     * @return Private key bytes
     * @throws IllegalArgumentException if key format is invalid
     */
    fun readPrivateKey(context: Context, uri: Uri): ByteArray {
        // Implementation details in tasks
    }
    
    /**
     * Checks if private key is encrypted (has passphrase).
     *
     * @param keyContent Private key content as String
     * @return true if key is encrypted
     */
    fun isEncrypted(keyContent: String): Boolean {
        return keyContent.contains("ENCRYPTED") ||
               keyContent.contains("Proc-Type: 4,ENCRYPTED")
    }
    
    /**
     * Validates private key format.
     *
     * @param keyContent Private key content as String
     * @return true if format is valid PEM or OpenSSH
     */
    fun isValidPrivateKey(keyContent: String): Boolean {
        return (keyContent.contains("-----BEGIN RSA PRIVATE KEY-----") ||
                keyContent.contains("-----BEGIN OPENSSH PRIVATE KEY-----") ||
                keyContent.contains("-----BEGIN PRIVATE KEY-----"))
    }
}
```

### MySQLConnectionPool Updates

**Changes**:

1. Add SSH tunnel manager as optional dependency
2. Establish tunnel before JDBC connection if SSH enabled
3. Update JDBC URL to use localhost:localPort when tunnel active
4. Cleanup tunnel in close()

```kotlin
class MySQLConnectionPool(
    private val config: ConnectionConfig,
    private val context: Context
) {
    private var activeConnection: Connection? = null
    private var sslConfigBuilder: MySQLSSLConfigBuilder? = null
    private var sshTunnelManager: SSHTunnelManager? = null  // NEW
    
    suspend fun getConnection(): Connection = withContext(Dispatchers.IO) {
        // ... existing code ...
        
        // Establish SSH tunnel if configured
        val (effectiveHost, effectivePort) = if (shouldUseSSHTunnel()) {
            establishSSHTunnel()
        } else {
            effectiveConfig.host to effectiveConfig.port
        }
        
        // Update JDBC URL with effective host/port
        val jdbcUrl = "jdbc:mysql://${effectiveHost}:${effectivePort}/$databaseSegment"
        
        // ... rest of existing code ...
    }
    
    private fun shouldUseSSHTunnel(): Boolean {
        return config.sshTunnelConfig?.enabled == true &&
               config.connectionString.isNullOrBlank()  // Don't override connection string
    }
    
    private fun establishSSHTunnel(): Pair<String, Int> {
        val sshConfig = config.sshTunnelConfig!!
        sshTunnelManager = SSHTunnelManager(sshConfig, context)
        val localPort = sshTunnelManager!!.connect(
            remoteHost = config.host,
            remotePort = config.port
        )
        return "127.0.0.1" to localPort
    }
    
    fun close() {
        // ... existing connection cleanup ...
        
        // Cleanup SSH tunnel
        sshTunnelManager?.disconnect()
        sshTunnelManager = null
    }
}
```

## UI Components

### SSH Tunnel Section (ConnectionFormScreen)

**State Management**:

```kotlin
// In ConnectionFormViewModel
data class ConnectionFormState(
    // ... existing fields ...
    val sshTunnelEnabled: Boolean = false,
    val sshHost: String = "",
    val sshPort: String = "22",
    val sshUsername: String = "",
    val sshAuthMethod: SSHAuthMethod = SSHAuthMethod.PASSWORD,
    val sshPassword: String = "",
    val sshPrivateKeyUri: String? = null,
    val sshPrivateKeyName: String? = null,
    val showSSHSecurityWarning: Boolean = false
)
```

**UI Composable**:

```kotlin
@Composable
private fun SSHTunnelSection(
    state: ConnectionFormState,
    onToggleSSH: (Boolean) -> Unit,
    onSSHHostChange: (String) -> Unit,
    onSSHPortChange: (String) -> Unit,
    onSSHUsernameChange: (String) -> Unit,
    onSSHAuthMethodChange: (SSHAuthMethod) -> Unit,
    onSSHPasswordChange: (String) -> Unit,
    onSelectPrivateKey: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExpandableSection(
        title = stringResource(R.string.connection_ssh_tunnel_title),
        expanded = state.sshTunnelEnabled || state.sshTunnelConfig != null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Toggle
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
                    checked = state.sshTunnelEnabled,
                    onCheckedChange = onToggleSSH
                )
            }
            
            if (state.sshTunnelEnabled) {
                // SSH Host field
                OutlinedTextField(
                    value = state.sshHost,
                    onValueChange = onSSHHostChange,
                    label = { Text(stringResource(R.string.connection_ssh_host)) },
                    isError = state.sshHost.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // SSH Port field
                OutlinedTextField(
                    value = state.sshPort,
                    onValueChange = onSSHPortChange,
                    label = { Text(stringResource(R.string.connection_ssh_port)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // SSH Username field
                OutlinedTextField(
                    value = state.sshUsername,
                    onValueChange = onSSHUsernameChange,
                    label = { Text(stringResource(R.string.connection_ssh_username)) },
                    isError = state.sshUsername.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Auth Method radio group
                Text(
                    text = stringResource(R.string.connection_ssh_auth_method),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RadioButton(
                        selected = state.sshAuthMethod == SSHAuthMethod.PASSWORD,
                        onClick = { onSSHAuthMethodChange(SSHAuthMethod.PASSWORD) }
                    )
                    Text(stringResource(R.string.connection_ssh_auth_password))
                    
                    RadioButton(
                        selected = state.sshAuthMethod == SSHAuthMethod.PRIVATE_KEY,
                        onClick = { onSSHAuthMethodChange(SSHAuthMethod.PRIVATE_KEY) }
                    )
                    Text(stringResource(R.string.connection_ssh_auth_private_key))
                }
                
                // Conditional fields based on auth method
                when (state.sshAuthMethod) {
                    SSHAuthMethod.PASSWORD -> {
                        PasswordField(
                            value = state.sshPassword,
                            onValueChange = onSSHPasswordChange,
                            label = stringResource(R.string.connection_ssh_password),
                            isError = state.sshPassword.isBlank(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    SSHAuthMethod.PRIVATE_KEY -> {
                        FilePicker(
                            label = stringResource(R.string.connection_ssh_private_key),
                            selectedFileName = state.sshPrivateKeyName,
                            onSelectFile = onSelectPrivateKey,
                            mimeTypes = arrayOf("*/*"),  // PEM files often have no MIME type
                            isError = state.sshPrivateKeyUri == null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
```

## Security Considerations

### 1. Known Hosts Verification

**Problem**: JSch by default requires known_hosts file, which doesn't exist on Android.

**Solution**: Disable strict host key checking with user warning.

```kotlin
val sessionConfig = Properties().apply {
    put("StrictHostKeyChecking", "no")
}
session.setConfig(sessionConfig)
```

**Mitigation**: Show security warning on first connection:
- Alert dialog explaining risk
- "I understand, continue" / "Cancel" options
- Store acceptance per connection (don't ask again)

### 2. Password Encryption

SSH passwords MUST be encrypted using DataStore encryption (same as database passwords).

### 3. Private Key Security

- Private key URIs stored (not file content)
- Android SAF permissions handle access control
- Keys read on-demand, not cached in memory

### 4. Local Port Binding

```kotlin
// CORRECT: Bind to localhost only
session.setPortForwardingL(
    "127.0.0.1",  // bind address - localhost only
    localPort,    // local port
    remoteHost,   // remote host (database server)
    remotePort    // remote port (database port)
)

// WRONG: Bind to all interfaces (security risk)
session.setPortForwardingL(localPort, remoteHost, remotePort)
```

## Testing Strategy

### Unit Tests

**SSHTunnelManagerTest**:
- Mock JSch Session for connection lifecycle
- Test password authentication flow
- Test private key authentication flow
- Test local port selection and retry logic
- Test cleanup on disconnect
- Test error handling (auth fail, timeout, network error)

**SSHKeyReaderTest**:
- Test reading PEM private keys from URIs
- Test reading OpenSSH private keys from URIs
- Test encrypted key detection
- Test invalid key format detection

### Integration Tests

**MySQLConnectionPoolTest** (with SSH):
- Test JDBC connection through SSH tunnel
- Test SSH tunnel + SSL connection (layered)
- Test tunnel cleanup on disconnect
- Test tunnel reconnect on network interruption

### Manual Testing Checklist

- [ ] SSH tunnel with password auth connects successfully
- [ ] SSH tunnel with private key auth connects successfully
- [ ] Invalid SSH credentials show clear error
- [ ] Encrypted private key shows error message
- [ ] SSH tunnel + SSL connection works (layered security)
- [ ] SSH tunnel cleaned up on disconnect
- [ ] Security warning shown on first SSH connection
- [ ] SSH config persisted and restored correctly

## Dependencies

Add to `app/build.gradle.kts`:

```kotlin
dependencies {
    // ... existing dependencies ...
    
    // SSH tunneling
    implementation("com.github.mwiede:jsch:0.2.16")
}
```

**Why this fork?**:
- Original JSch abandoned (last release 2018)
- `com.github.mwiede:jsch` is the maintained community fork
- Active development, security patches, Android compatibility

## Performance Considerations

### Connection Overhead

| Phase | Expected Time | Notes |
|-------|---------------|-------|
| SSH session establishment | 500ms-2s | Network dependent |
| SSH authentication | 100ms-500ms | Key exchange |
| Port forwarding setup | <100ms | Local operation |
| **Total SSH overhead** | **600ms-2.6s** | First connection only |
| Per-query overhead | <10ms | Tunnel already established |

### Memory Impact

- JSch Session: ~500KB per connection
- Port forwarding channel: ~100KB
- **Total**: ~600KB overhead per SSH connection

### Optimization Strategies

1. **Connection pooling**: Reuse SSH session for multiple JDBC connections (future)
2. **Lazy cleanup**: Keep tunnel alive for X seconds after JDBC disconnect (future)
3. **Health check caching**: Check tunnel health max once per second

## Migration Path

### Backward Compatibility

- Existing connections without `sshTunnelConfig`: continue working (null-safe)
- ConnectionConfig serialization: `sshTunnelConfig` defaults to null
- UI: SSH section collapsed by default (opt-in)

### Data Migration

No migration needed - new field defaults to null in existing saved connections.

## Rollback Plan

1. **Immediate**: Set `sshTunnelConfig.enabled = false` in UI
2. **Code**: Remove SSH establishment block from `MySQLConnectionPool`
3. **Dependency**: Remove JSch from build.gradle if issues arise
4. **User impact**: Users lose SSH tunnel feature but existing connections continue working
