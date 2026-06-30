# SSH Tunneling Implementation Tasks

**Change**: ssh-tunneling  
**Version**: 1.0  
**Status**: PLANNING  
**Author**: israel-icm  
**Created**: 2026-06-30  

## Task Breakdown

### Phase 1: Foundation (Data Model + Dependencies)

#### Task 1.1: Add JSch dependency
**Priority**: MUST  
**Estimated effort**: 5 minutes  

**Steps**:
1. Add `implementation("com.github.mwiede:jsch:0.2.16")` to `app/build.gradle.kts`
2. Sync Gradle
3. Verify JSch classes are accessible (import test)

**Verification**: `./gradlew build` succeeds

---

#### Task 1.2: Create SSHTunnelConfig data model
**Priority**: MUST  
**Estimated effort**: 10 minutes  

**Steps**:
1. Create `app/src/main/java/com/sphynxs/mydatabases/core/database/models/SSHTunnelConfig.kt`
2. Define `SSHTunnelConfig` data class with all fields
3. Define `SSHAuthMethod` enum (PASSWORD, PRIVATE_KEY)
4. Add KDoc documentation

**Files**:
- `app/src/main/java/com/sphynxs/mydatabases/core/database/models/SSHTunnelConfig.kt`

**Verification**: Code compiles, model accessible from ConnectionConfig

---

#### Task 1.3: Update ConnectionConfig with SSH tunnel field
**Priority**: MUST  
**Estimated effort**: 10 minutes  

**Steps**:
1. Open `app/src/main/java/com/sphynxs/mydatabases/core/database/models/ConnectionConfig.kt`
2. Add `val sshTunnelConfig: SSHTunnelConfig? = null` field
3. Update KDoc to mention SSH tunnel support
4. Verify backward compatibility (null-safe)

**Files**:
- `app/src/main/java/com/sphynxs/mydatabases/core/database/models/ConnectionConfig.kt`

**Verification**: Existing tests pass (backward compatible)

---

### Phase 2: SSH Key Reader

#### Task 2.1: Create SSHKeyReader utility
**Priority**: MUST  
**Estimated effort**: 30 minutes  

**Steps**:
1. Create `app/src/main/java/com/sphynxs/mydatabases/core/database/ssh/SSHKeyReader.kt`
2. Implement `readPrivateKey(context, uri): ByteArray`
   - Read from URI using ContentResolver
   - Return raw bytes for JSch
3. Implement `isEncrypted(keyContent): Boolean`
   - Check for "ENCRYPTED" or "Proc-Type: 4,ENCRYPTED" markers
4. Implement `isValidPrivateKey(keyContent): Boolean`
   - Check for PEM/OpenSSH headers
5. Add comprehensive KDoc

**Files**:
- `app/src/main/java/com/sphynxs/mydatabases/core/database/ssh/SSHKeyReader.kt`

**Verification**: Manual test with sample PEM key

---

#### Task 2.2: Write SSHKeyReader unit tests
**Priority**: MUST  
**Estimated effort**: 30 minutes  

**Tests**:
1. `readPrivateKey_validPemKey_returnsBytes()`
2. `readPrivateKey_validOpenSSHKey_returnsBytes()`
3. `readPrivateKey_invalidUri_throwsException()`
4. `isEncrypted_encryptedKey_returnsTrue()`
5. `isEncrypted_unencryptedKey_returnsFalse()`
6. `isValidPrivateKey_validPem_returnsTrue()`
7. `isValidPrivateKey_invalidFormat_returnsFalse()`

**Files**:
- `app/src/test/java/com/sphynxs/mydatabases/core/database/ssh/SSHKeyReaderTest.kt`

**Verification**: `./gradlew test` passes all SSHKeyReader tests

---

### Phase 3: SSH Tunnel Manager

#### Task 3.1: Create SSHTunnelManager class
**Priority**: MUST  
**Estimated effort**: 60 minutes  

**Steps**:
1. Create `app/src/main/java/com/sphynxs/mydatabases/core/database/ssh/SSHTunnelManager.kt`
2. Implement constructor with SSHTunnelConfig and Context
3. Implement `connect(remoteHost: String, remotePort: Int): Int`
   - Create JSch instance
   - Create Session with SSH host/port/username
   - Configure session (disable StrictHostKeyChecking)
   - Authenticate (password or private key)
   - Connect session
   - Set up local port forwarding
   - Return local port
4. Implement `disconnect()`
   - Close port forwarding
   - Disconnect session
   - Clean up resources
5. Implement `isActive(): Boolean`
6. Implement `getLocalPort(): Int?`
7. Add comprehensive error handling with custom exceptions

**Files**:
- `app/src/main/java/com/sphynxs/mydatabases/core/database/ssh/SSHTunnelManager.kt`
- `app/src/main/java/com/sphynxs/mydatabases/core/database/ssh/SSHTunnelException.kt`

**Verification**: Manual test with SSH server

---

#### Task 3.2: Implement local port selection with retry
**Priority**: MUST  
**Estimated effort**: 20 minutes  

**Steps**:
1. In `SSHTunnelManager.connect()`, add port selection logic:
   - Generate random port in range 49152-65535
   - Attempt port forwarding with localhost bind
   - Retry with different port on bind failure (max 3 attempts)
   - Throw exception if all attempts fail
2. Add logging for port selection attempts

**Files**:
- `app/src/main/java/com/sphynxs/mydatabases/core/database/ssh/SSHTunnelManager.kt`

**Verification**: Manual test with intentional port conflicts

---

#### Task 3.3: Write SSHTunnelManager unit tests
**Priority**: MUST  
**Estimated effort**: 60 minutes  

**Tests**:
1. `connect_passwordAuth_establishesTunnel()`
2. `connect_privateKeyAuth_establishesTunnel()`
3. `connect_invalidCredentials_throwsException()`
4. `connect_portConflict_retriesWithDifferentPort()`
5. `connect_timeout_throwsException()`
6. `disconnect_activeTunnel_cleansUpResources()`
7. `isActive_connected_returnsTrue()`
8. `isActive_disconnected_returnsFalse()`
9. `getLocalPort_established_returnsPort()`
10. `getLocalPort_notEstablished_returnsNull()`

**Files**:
- `app/src/test/java/com/sphynxs/mydatabases/core/database/ssh/SSHTunnelManagerTest.kt`

**Mock strategy**: Mock JSch Session and Channel for isolated tests

**Verification**: `./gradlew test` passes all SSHTunnelManager tests

---

### Phase 4: Integration with MySQLConnectionPool

#### Task 4.1: Add SSH tunnel support to MySQLConnectionPool
**Priority**: MUST  
**Estimated effort**: 45 minutes  

**Steps**:
1. Open `app/src/main/java/com/sphynxs/mydatabases/core/database/engine/mysql/MySQLConnectionPool.kt`
2. Add `private var sshTunnelManager: SSHTunnelManager? = null` field
3. Add `private fun shouldUseSSHTunnel(): Boolean` helper
4. Add `private fun establishSSHTunnel(): Pair<String, Int>` helper
5. Update `getConnection()`:
   - Check if SSH tunnel enabled
   - Establish tunnel before JDBC connection
   - Update JDBC URL with localhost:localPort
   - Handle SSH errors (wrap in DatabaseError)
6. Update `close()`:
   - Disconnect SSH tunnel
   - Clean up tunnel manager
7. Add comprehensive logging

**Files**:
- `app/src/main/java/com/sphynxs/mydatabases/core/database/engine/mysql/MySQLConnectionPool.kt`

**Verification**: Manual test with SSH-enabled connection

---

#### Task 4.2: Write MySQLConnectionPool SSH integration tests
**Priority**: MUST  
**Estimated effort**: 45 minutes  

**Tests**:
1. `getConnection_sshEnabled_establishesTunnelBeforeJdbc()`
2. `getConnection_sshDisabled_skipsTunnel()`
3. `getConnection_sshAndSsl_layeredSecurity()`
4. `close_sshTunnelActive_cleansUpTunnel()`
5. `getConnection_sshFails_throwsError()`

**Mock strategy**: Mock SSHTunnelManager for isolated tests

**Files**:
- `app/src/test/java/com/sphynxs/mydatabases/core/database/engine/mysql/MySQLConnectionPoolTest.kt`

**Verification**: `./gradlew test` passes all integration tests

---

### Phase 5: UI (Connection Form)

#### Task 5.1: Add SSH strings to resources
**Priority**: MUST  
**Estimated effort**: 15 minutes  

**Steps**:
1. Open `app/src/main/res/values/strings.xml`
2. Add SSH-related strings (English):
   - connection_ssh_tunnel_title
   - connection_ssh_tunnel_enable
   - connection_ssh_host
   - connection_ssh_port
   - connection_ssh_username
   - connection_ssh_auth_method
   - connection_ssh_auth_password
   - connection_ssh_auth_private_key
   - connection_ssh_password
   - connection_ssh_private_key
   - connection_ssh_security_warning_title
   - connection_ssh_security_warning_message
   - connection_ssh_security_warning_accept
3. Open `app/src/main/res/values-es/strings.xml`
4. Add Spanish translations

**Files**:
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-es/strings.xml`

**Verification**: Strings accessible in Composables

---

#### Task 5.2: Add SSH state to ConnectionFormViewModel
**Priority**: MUST  
**Estimated effort**: 20 minutes  

**Steps**:
1. Open `app/src/main/java/com/sphynxs/mydatabases/ui/screens/connections/ConnectionFormViewModel.kt`
2. Add SSH fields to `ConnectionFormState`:
   - sshTunnelEnabled: Boolean
   - sshHost: String
   - sshPort: String
   - sshUsername: String
   - sshAuthMethod: SSHAuthMethod
   - sshPassword: String
   - sshPrivateKeyUri: String?
   - sshPrivateKeyName: String?
   - showSSHSecurityWarning: Boolean
3. Add SSH update functions:
   - `onToggleSSH(enabled: Boolean)`
   - `onSSHHostChange(host: String)`
   - `onSSHPortChange(port: String)`
   - `onSSHUsernameChange(username: String)`
   - `onSSHAuthMethodChange(method: SSHAuthMethod)`
   - `onSSHPasswordChange(password: String)`
   - `onSelectSSHPrivateKey(uri: Uri, name: String)`
   - `onDismissSSHSecurityWarning()`
4. Update `buildConnectionConfig()` to include SSH config
5. Update `loadConnectionForEdit()` to restore SSH config

**Files**:
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/connections/ConnectionFormViewModel.kt`

**Verification**: Code compiles, state updates work

---

#### Task 5.3: Create SSHTunnelSection composable
**Priority**: MUST  
**Estimated effort**: 60 minutes  

**Steps**:
1. Open `app/src/main/java/com/sphynxs/mydatabases/ui/screens/connections/ConnectionFormScreen.kt`
2. Create `@Composable private fun SSHTunnelSection()` following design spec
3. Add toggle for enabling SSH tunnel
4. Add text fields for host, port, username
5. Add radio group for auth method selection
6. Add conditional password field (if PASSWORD selected)
7. Add conditional file picker (if PRIVATE_KEY selected)
8. Add validation error indicators
9. Add auto-expand logic (when editing connection with SSH)
10. Follow existing UI patterns (grouped cards, iOS style)

**Files**:
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/connections/ConnectionFormScreen.kt`

**Verification**: Manual UI test in Android Studio preview

---

#### Task 5.4: Create SSH security warning dialog
**Priority**: MUST  
**Estimated effort**: 20 minutes  

**Steps**:
1. In `ConnectionFormScreen.kt`, create `SSHSecurityWarningDialog` composable
2. Show warning message about disabled host key verification
3. Add "I understand, continue" and "Cancel" buttons
4. Store acceptance in ViewModel state
5. Show dialog only on first SSH connection (per connection)

**Files**:
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/connections/ConnectionFormScreen.kt`

**Verification**: Dialog shows when enabling SSH for first time

---

#### Task 5.5: Wire up SSH private key file picker
**Priority**: MUST  
**Estimated effort**: 15 minutes  

**Steps**:
1. In `ConnectionFormScreen`, add `ActivityResultLauncher` for private key picker
2. Configure launcher with MIME type `*/*` (PEM files have no specific type)
3. Update ViewModel state with selected URI and filename
4. Display selected filename in UI

**Files**:
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/connections/ConnectionFormScreen.kt`
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/connections/ConnectionFormViewModel.kt`

**Verification**: File picker opens, selected file shows in UI

---

### Phase 6: Persistence

#### Task 6.1: Update ConnectionConfigSerializer for SSH
**Priority**: MUST  
**Estimated effort**: 30 minutes  

**Steps**:
1. Open `app/src/main/java/com/sphynxs/mydatabases/data/local/ConnectionConfigSerializer.kt`
2. Add SSH fields to serialization schema
3. Encrypt SSH password using same method as database password
4. Store SSH private key URI (not file content)
5. Add backward compatibility (handle null SSH config)
6. Add migration if needed (version bump)

**Files**:
- `app/src/main/java/com/sphynxs/mydatabases/data/local/ConnectionConfigSerializer.kt`

**Verification**: Save/load connection with SSH config works

---

#### Task 6.2: Write persistence tests for SSH config
**Priority**: MUST  
**Estimated effort**: 20 minutes  

**Tests**:
1. `serialize_sshConfigEnabled_storesAllFields()`
2. `deserialize_sshConfigEnabled_restoresAllFields()`
3. `serialize_sshPasswordEncrypted_storesEncrypted()`
4. `deserialize_sshPasswordEncrypted_decryptsCorrectly()`
5. `serialize_sshConfigNull_backwardCompatible()`

**Files**:
- `app/src/test/java/com/sphynxs/mydatabases/data/local/ConnectionConfigSerializerTest.kt`

**Verification**: `./gradlew test` passes persistence tests

---

### Phase 7: Error Handling

#### Task 7.1: Create SSHTunnelException hierarchy
**Priority**: MUST  
**Estimated effort**: 15 minutes  

**Steps**:
1. Create `app/src/main/java/com/sphynxs/mydatabases/core/database/ssh/SSHTunnelException.kt`
2. Define exception types:
   - `SSHTunnelException` (base)
   - `SSHConnectionTimeoutException`
   - `SSHAuthenticationFailedException`
   - `SSHInvalidKeyException`
   - `SSHPortAllocationException`
3. Add localized error messages

**Files**:
- `app/src/main/java/com/sphynxs/mydatabases/core/database/ssh/SSHTunnelException.kt`

**Verification**: Exceptions throwable and catchable

---

#### Task 7.2: Map SSH exceptions to user-friendly errors
**Priority**: MUST  
**Estimated effort**: 20 minutes  

**Steps**:
1. In `MySQLConnectionPool.getConnection()`, add try-catch for SSH exceptions
2. Map each exception type to localized error message
3. Show error in UI via ViewModel state
4. Add logging for debugging

**Files**:
- `app/src/main/java/com/sphynxs/mydatabases/core/database/engine/mysql/MySQLConnectionPool.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-es/strings.xml`

**Verification**: SSH errors show clear messages in UI

---

### Phase 8: Testing & Validation

#### Task 8.1: Manual testing checklist
**Priority**: MUST  
**Estimated effort**: 60 minutes  

**Test scenarios**:
1. Create connection with SSH password auth
2. Create connection with SSH private key auth
3. Test invalid SSH credentials (show error)
4. Test encrypted private key (show error)
5. Test SSH + SSL layered security
6. Test SSH tunnel cleanup on disconnect
7. Test security warning shows on first connection
8. Test SSH config saves and restores correctly
9. Test connection form validation (required fields)
10. Test auto-expand SSH section when editing

**Verification**: All scenarios pass, no crashes

---

#### Task 8.2: Write UI tests for SSH form
**Priority**: SHOULD  
**Estimated effort**: 45 minutes  

**Tests**:
1. `sshSection_toggleEnabled_showsFields()`
2. `sshSection_toggleDisabled_hidesFields()`
3. `sshSection_authMethodPassword_showsPasswordField()`
4. `sshSection_authMethodPrivateKey_showsFilePicker()`
5. `sshSection_requiredFieldsEmpty_showsErrors()`
6. `sshSection_validationPasses_allowsSave()`

**Files**:
- `app/src/androidTest/java/com/sphynxs/mydatabases/ui/screens/connections/ConnectionFormScreenTest.kt`

**Verification**: `./gradlew connectedAndroidTest` passes UI tests

---

### Phase 9: Documentation

#### Task 9.1: Update .atl/architecture/decisions/ with SSH ADR
**Priority**: SHOULD  
**Estimated effort**: 15 minutes  

**Steps**:
1. Create `.atl/architecture/decisions/005-ssh-tunneling-jsch.md`
2. Document decision to use JSch library
3. Document security tradeoffs (disabled host key verification)
4. Document alternatives considered

**Files**:
- `.atl/architecture/decisions/005-ssh-tunneling-jsch.md`

**Verification**: ADR readable and complete

---

#### Task 9.2: Update feature documentation
**Priority**: SHOULD  
**Estimated effort**: 10 minutes  

**Steps**:
1. Update `.atl/product/features/database-connections.md` with SSH tunnel section
2. Document supported SSH auth methods
3. Document security considerations
4. Add usage examples

**Files**:
- `.atl/product/features/database-connections.md`

**Verification**: Documentation reflects implementation

---

## Task Summary

| Phase | Tasks | Estimated Time |
|-------|-------|----------------|
| 1. Foundation | 3 | 25 minutes |
| 2. SSH Key Reader | 2 | 60 minutes |
| 3. SSH Tunnel Manager | 3 | 140 minutes |
| 4. Integration | 2 | 90 minutes |
| 5. UI | 5 | 130 minutes |
| 6. Persistence | 2 | 50 minutes |
| 7. Error Handling | 2 | 35 minutes |
| 8. Testing & Validation | 2 | 105 minutes |
| 9. Documentation | 2 | 25 minutes |
| **TOTAL** | **23** | **~660 minutes (~11 hours)** |

## Recommended Implementation Order

1. **Phase 1** (Foundation) → Quick wins, enables rest of work
2. **Phase 2** (SSH Key Reader) → Isolated utility, testable independently
3. **Phase 3** (SSH Tunnel Manager) → Core logic, thorough testing needed
4. **Phase 4** (Integration) → Connect tunnel to JDBC flow
5. **Phase 6** (Persistence) → Enable saving/loading before UI work
6. **Phase 5** (UI) → User-facing work after backend solid
7. **Phase 7** (Error Handling) → Polish error experience
8. **Phase 8** (Testing) → Validation before release
9. **Phase 9** (Documentation) → Record decisions

## Dependencies Between Tasks

- **1.2** must complete before **1.3**
- **1.3** must complete before **6.1**
- **2.1** must complete before **3.1**
- **3.1** must complete before **3.2**, **3.3**, **4.1**
- **4.1** must complete before **4.2**
- **5.1** must complete before **5.2**, **5.3**
- **5.2** must complete before **5.3**, **5.4**, **5.5**
- **6.1** must complete before **6.2**
- **7.1** must complete before **7.2**

## Success Criteria

- [ ] All unit tests pass (`./gradlew test`)
- [ ] All integration tests pass
- [ ] All UI tests pass (`./gradlew connectedAndroidTest`)
- [ ] Manual testing checklist complete
- [ ] SSH password auth works end-to-end
- [ ] SSH private key auth works end-to-end
- [ ] SSH + SSL layered security works
- [ ] Security warning shows on first use
- [ ] Configuration persists correctly
- [ ] Error handling is user-friendly
- [ ] Documentation updated
- [ ] No memory leaks (tunnel cleanup verified)
