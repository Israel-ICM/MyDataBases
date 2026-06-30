# SSH Tunneling Specification

**Change**: ssh-tunneling  
**Version**: 1.0  
**Status**: PLANNING  
**Author**: israel-icm  
**Created**: 2026-06-30  

## Overview

This specification defines the requirements for SSH tunneling support in MyDataBases, enabling connections to remote databases through bastion/jump hosts.

## Functional Requirements

### FR-1: SSH Configuration UI

**Priority**: MUST  

**Description**: The connection form MUST provide SSH tunnel configuration options.

**Given** a user is creating or editing a database connection  
**When** they expand the "Advanced Connection" section  
**Then** they MUST see an "SSH Tunnel" subsection with:
- Toggle: "Enable SSH Tunnel" (default: OFF)
- Text field: "SSH Host" (required when enabled)
- Number field: "SSH Port" (default: 22)
- Text field: "SSH Username" (required when enabled)
- Radio group: "Authentication Method" (Password / Private Key)
- IF Password selected: Password field "SSH Password"
- IF Private Key selected: File picker "Private Key File"

**Localization**:
- **es**: "Túnel SSH", "Habilitar túnel SSH", "Host SSH", "Puerto SSH", "Usuario SSH", "Método de autenticación", "Contraseña SSH", "Archivo de clave privada"
- **en**: "SSH Tunnel", "Enable SSH tunnel", "SSH Host", "SSH Port", "SSH Username", "Authentication method", "SSH Password", "Private Key File"

### FR-2: SSH Tunnel Establishment

**Priority**: MUST  

**Description**: The system MUST establish an SSH tunnel before JDBC connection when SSH is enabled.

**Given** a connection config with SSH tunnel enabled  
**When** the user initiates a connection  
**Then** the system MUST:
1. Validate SSH configuration (host, username, auth credentials)
2. Establish SSH session to SSH host:port
3. Create local port forward (localhost:randomPort → dbHost:dbPort)
4. Update JDBC connection to use localhost:randomPort
5. Proceed with JDBC connection (with SSL if configured)

**Given** SSH tunnel establishment fails  
**When** an error occurs during SSH connection  
**Then** the system MUST:
1. NOT attempt JDBC connection
2. Show error message with SSH failure reason
3. Clean up any partial SSH session

### FR-3: SSH Authentication Methods

**Priority**: MUST  

**Description**: The system MUST support password and private key authentication.

**Scenario 1: Password Authentication**

**Given** SSH auth method is "Password"  
**When** establishing SSH tunnel  
**Then** the system MUST use username + password for authentication

**Scenario 2: Private Key Authentication**

**Given** SSH auth method is "Private Key"  
**And** user has selected a valid private key file  
**When** establishing SSH tunnel  
**Then** the system MUST:
1. Read private key from selected URI
2. Parse key format (PEM/OpenSSH)
3. Use key-based authentication with SSH server

**Given** private key is encrypted (has passphrase)  
**When** system detects encrypted key  
**Then** system MUST show error: "Encrypted SSH keys not supported in v1.0. Please use an unencrypted key or password authentication."

### FR-4: Local Port Selection

**Priority**: MUST  

**Description**: The system MUST select an available local port for SSH forwarding.

**Given** SSH tunnel is being established  
**When** selecting local port  
**Then** the system MUST:
1. Choose random port in ephemeral range (49152-65535)
2. Bind to localhost (127.0.0.1) only
3. Retry with different port if bind fails (max 3 attempts)
4. Throw error if all attempts fail

### FR-5: Tunnel Lifecycle Management

**Priority**: MUST  

**Description**: The system MUST manage SSH tunnel lifecycle correctly.

**Given** an active SSH tunnel  
**When** user disconnects from database  
**Then** the system MUST:
1. Close JDBC connection
2. Close SSH port forward
3. Disconnect SSH session
4. Release local port

**Given** an active SSH tunnel  
**When** app is destroyed or ViewModel cleared  
**Then** the system MUST clean up tunnel resources (leak prevention)

### FR-6: Tunnel Health Check

**Priority**: SHOULD  

**Description**: The system SHOULD verify tunnel health before queries.

**Given** an established SSH tunnel  
**When** about to execute a database query  
**Then** the system SHOULD:
1. Check if SSH session is connected
2. Attempt reconnect if session dropped
3. Proceed with query if tunnel is healthy
4. Fail with clear error if tunnel cannot be restored

### FR-7: Security Warnings

**Priority**: MUST  

**Description**: The system MUST warn users about security implications.

**Given** user enables SSH tunnel  
**When** first establishing connection  
**Then** the system MUST show a one-time warning:
- **es**: "ADVERTENCIA DE SEGURIDAD: Se aceptarán todos los hosts SSH sin verificación. Solo conecte a hosts de confianza."
- **en**: "SECURITY WARNING: All SSH hosts will be accepted without verification. Only connect to trusted hosts."

**Acceptance options**:
- "I understand, continue" / "Entiendo, continuar"
- "Cancel" / "Cancelar"

### FR-8: Configuration Persistence

**Priority**: MUST  

**Description**: SSH configuration MUST be persisted securely.

**Given** user saves a connection with SSH tunnel config  
**When** storing configuration  
**Then** the system MUST:
1. Encrypt SSH password using DataStore encryption
2. Store SSH private key URI (not file content)
3. Store other SSH settings (host, port, username, auth method)

**Given** user edits an existing connection with SSH config  
**When** loading configuration  
**Then** the system MUST:
1. Decrypt SSH password from storage
2. Restore SSH settings to form
3. Pre-expand "SSH Tunnel" section

## Non-Functional Requirements

### NFR-1: Performance

**Priority**: SHOULD  

- SSH tunnel establishment SHOULD complete within 3 seconds on typical networks
- SSH tunnel overhead per query SHOULD be < 10ms
- Local port selection SHOULD complete within 100ms

### NFR-2: Reliability

**Priority**: MUST  

- SSH tunnel cleanup MUST occur even on app crash (leak detector)
- SSH session MUST handle network interruptions gracefully
- Failed SSH connection MUST NOT leave zombie processes

### NFR-3: Security

**Priority**: MUST  

- SSH passwords MUST be stored encrypted
- Local port forwarding MUST bind to localhost only (not 0.0.0.0)
- Private keys MUST be read from URIs with proper permissions
- SSH host key verification MUST be configurable (warn when disabled)

### NFR-4: Compatibility

**Priority**: MUST  

- SSH tunneling MUST work on Android 8.0+ (API 26+)
- JSch library MUST be compatible with Android runtime
- SSH tunnel MUST work with SSL/TLS connections (layered security)

## Data Model

### SSHTunnelConfig

```kotlin
data class SSHTunnelConfig(
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int = 22,
    val username: String = "",
    val authMethod: SSHAuthMethod = SSHAuthMethod.PASSWORD,
    val password: String = "",
    val privateKeyUri: String? = null
)
```

### SSHAuthMethod

```kotlin
enum class SSHAuthMethod {
    PASSWORD,
    PRIVATE_KEY
}
```

### ConnectionConfig Update

```kotlin
data class ConnectionConfig(
    // ... existing fields ...
    val sshTunnelConfig: SSHTunnelConfig? = null
)
```

## UI Specification

### SSH Tunnel Section (ConnectionFormScreen)

**Layout** (Compact WindowSizeClass):
```
[Advanced Connection ▼]
  
  [SSH Tunnel ▼]
  
  ⚡ Enable SSH Tunnel     [Toggle OFF]
  
  SSH Host *              [___________]
  SSH Port               [22________]
  SSH Username *          [___________]
  
  Authentication Method
  ○ Password
  ○ Private Key
  
  [IF Password]
  SSH Password *          [___________] 👁
  
  [IF Private Key]
  Private Key File *      [Choose file...]
                          📄 client-key.pem
```

**Validation**:
- SSH Host: Required when enabled, non-empty
- SSH Port: 1-65535
- SSH Username: Required when enabled, non-empty
- SSH Password: Required when method=PASSWORD
- Private Key File: Required when method=PRIVATE_KEY

**Auto-expand behavior**:
- IF editing connection with SSH enabled → auto-expand "SSH Tunnel"
- ELSE → collapsed by default

## Error Handling

| Error Condition | User Message (en) | User Message (es) |
|-----------------|-------------------|-------------------|
| SSH connection timeout | "SSH connection timed out. Check SSH host and port." | "Tiempo de espera agotado para SSH. Verifique host y puerto SSH." |
| SSH auth failed (password) | "SSH authentication failed. Check username and password." | "Autenticación SSH fallida. Verifique usuario y contraseña." |
| SSH auth failed (key) | "SSH authentication failed. Check private key file." | "Autenticación SSH fallida. Verifique archivo de clave privada." |
| Private key invalid | "Invalid private key format. Expected PEM or OpenSSH format." | "Formato de clave privada inválido. Se esperaba formato PEM u OpenSSH." |
| Private key encrypted | "Encrypted SSH keys not supported in v1.0. Use unencrypted key or password auth." | "Claves SSH cifradas no soportadas en v1.0. Use clave sin cifrar o autenticación por contraseña." |
| Local port unavailable | "Cannot allocate local port for SSH tunnel. Close other connections." | "No se puede asignar puerto local para túnel SSH. Cierre otras conexiones." |
| SSH tunnel dropped | "SSH tunnel connection lost. Reconnecting..." | "Conexión de túnel SSH perdida. Reconectando..." |

## Testing Requirements

### Unit Tests

- [ ] SSHTunnelManager: session lifecycle (connect, disconnect, cleanup)
- [ ] SSHTunnelManager: password authentication
- [ ] SSHTunnelManager: private key authentication
- [ ] SSHTunnelManager: local port selection (retry on conflict)
- [ ] SSHTunnelManager: error handling (auth fail, timeout, invalid key)
- [ ] SSHKeyReader: parse PEM private keys
- [ ] SSHKeyReader: parse OpenSSH private keys
- [ ] SSHKeyReader: detect encrypted keys

### Integration Tests

- [ ] MySQLConnectionPool: establish JDBC connection through SSH tunnel
- [ ] MySQLConnectionPool: SSH tunnel + SSL connection (layered)
- [ ] ConnectionConfig: persist and restore SSH config with encryption

### UI Tests

- [ ] ConnectionFormScreen: SSH tunnel section appears when advanced expanded
- [ ] ConnectionFormScreen: toggle enables/disables SSH fields
- [ ] ConnectionFormScreen: auth method switches between password/key fields
- [ ] ConnectionFormScreen: private key file picker works
- [ ] ConnectionFormScreen: validation errors show for required SSH fields

## Acceptance Criteria

- [ ] User can enable SSH tunnel in connection form
- [ ] User can configure SSH with password authentication
- [ ] User can configure SSH with private key authentication
- [ ] System establishes SSH tunnel before JDBC connection
- [ ] System shows security warning on first SSH connection
- [ ] System validates SSH configuration before attempting connection
- [ ] System shows clear errors for SSH failures
- [ ] SSH tunnel works with SSL/TLS connections (layered security)
- [ ] SSH tunnel is cleaned up on disconnect
- [ ] SSH configuration is persisted securely (encrypted password)
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] All UI tests pass
