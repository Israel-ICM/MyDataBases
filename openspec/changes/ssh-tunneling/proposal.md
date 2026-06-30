# SSH Tunneling for Remote Database Connections

**Status**: PLANNING  
**Type**: Feature  
**Priority**: HIGH  
**Author**: israel-icm  
**Created**: 2026-06-30  

## Intent

Implement SSH tunneling (port forwarding) to allow secure connections to remote MySQL/MariaDB databases through a bastion/jump host. This complements SSL/TLS support by securing the network transport layer.

## Problem

Users need to connect to database servers that are:
- Behind firewalls (only SSH access exposed)
- In private networks accessible only through bastion hosts
- Requiring multi-layer security (SSH tunnel + SSL connection)

Common use case in production:
```
Android App → SSH Tunnel → Bastion Host → SSL Connection → MySQL Server
```

Without SSH tunneling, users cannot reach these databases from the mobile app.

## Scope

### In Scope
- SSH tunnel configuration UI (host, port, username, auth method)
- SSH authentication methods: password, private key
- Private key selection via Android SAF (PEM/OpenSSH format)
- SSH tunnel establishment before JDBC connection
- Auto-reconnect SSH tunnel on connection loss
- Cleanup tunnel on disconnect

### Out of Scope
- SSH agent forwarding (v1.1)
- Jump host chaining (ProxyJump, v1.1)
- Known hosts verification (accept all in v1.0, security warning shown)
- SSH key passphrase support (only unencrypted keys in v1.0)
- Custom SSH port forwarding rules (only database port forwarding)

## Approach

### Architecture Decision

Use **JSch library** (Java Secure Channel) for SSH tunneling:
- Mature, battle-tested library
- Works on Android without native dependencies
- Supports password and key-based auth
- Thread-safe session management

Alternative considered: Apache MINA SSHD (rejected: heavyweight for mobile)

### Data Model

Extend `ConnectionConfig` with SSH configuration:

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

enum class SSHAuthMethod {
    PASSWORD,
    PRIVATE_KEY
}
```

### Implementation Flow

1. **UI Layer**: Add SSH tunnel configuration section in `ConnectionFormScreen`
2. **Domain Layer**: SSH tunnel manager with lifecycle (connect, disconnect, isActive)
3. **Data Layer**: Persist SSH config in `ConnectionConfig`
4. **Integration**: Update `MySQLConnectionPool` to establish tunnel before JDBC connection

### Connection Sequence

```
1. Parse ConnectionConfig
2. IF sshTunnelConfig.enabled:
   a. Establish SSH tunnel (local random port → remote DB port)
   b. Update JDBC connection to use localhost:localPort
3. ELSE:
   a. Use direct connection (existing flow)
4. Apply SSL config (if enabled)
5. Establish JDBC connection
```

### Local Port Selection

- Use ephemeral port range (49152-65535)
- Bind to localhost only (security)
- Retry with different port on bind failure (max 3 attempts)

## Rollback Plan

- SSH config defaults to `enabled = false` (backward compatible)
- Existing connections without SSH config continue working
- If SSH tunnel fails, show clear error message and do not attempt JDBC connection
- Users can disable SSH tunnel via UI to revert to direct connection

## Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| JSch library incompatibility with Android | HIGH | Test on Android 8.0+ before commit |
| SSH tunnel not cleaned up on app crash | MEDIUM | Use `onCleared()` in ViewModel + connection leak detector |
| User selects encrypted private key | LOW | Validate key format, show error if passphrase required |
| Known hosts verification disabled | MEDIUM | Show security warning in UI ("Accept SSH host without verification?") |
| SSH tunnel breaks during long-running query | MEDIUM | Implement tunnel health check before each query |

## Impact

### UI Impact
- ConnectionFormScreen: New expandable "SSH Tunnel" section
- Locales: New strings for SSH configuration (es/en)
- WindowSizeClass: Compact (single column), Medium/Expanded (grouped layout)

### Security Impact
- Credentials (SSH password, private key) stored encrypted via DataStore
- SSH tunnel uses localhost bind (not exposed to network)
- Security warning shown when known_hosts verification is disabled

### Performance Impact
- SSH tunnel establishment: ~500ms-2s overhead on first connection
- Minimal overhead on established tunnel (<10ms per query)

## Dependencies

- **JSch library**: `com.github.mwiede:jsch:0.2.16` (maintained fork)
- File picker for private key selection (already implemented)
- CertificateReader pattern (reuse for SSH keys)

## Success Criteria

- [ ] User can configure SSH tunnel with password auth
- [ ] User can configure SSH tunnel with private key auth
- [ ] SSH tunnel establishes before JDBC connection
- [ ] Connection works through tunnel (MySQL + SSL)
- [ ] Tunnel auto-reconnects on connection loss
- [ ] Tunnel cleaned up on disconnect
- [ ] Security warning shown for known_hosts acceptance
- [ ] All tests pass (unit + integration)
