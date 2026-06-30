# Connection Lifecycle Management

**Status**: Planned  
**Priority**: Medium  
**Complexity**: Medium  
**Estimated effort**: 3-4 hours

---

## Problem Statement

Currently, database connections remain open indefinitely once established:

- ❌ No automatic disconnect when user navigates away
- ❌ No cleanup when switching between connections
- ❌ Memory leaks and resource waste (JDBC connections + SSH tunnels stay active)
- ❌ No visibility of connection state for the user
- ❌ Will block future background services (sync, backups, long migrations)

**Real-world impact:**
- SSH tunnels remain active even after user closes the app
- JDBC connections consume server resources unnecessarily
- No way to know if a connection is still alive or stale

---

## Goals

### Primary Goals
1. **Explicit disconnect control**: User can manually close connections
2. **Auto-disconnect on switch**: Opening a new connection closes the previous one
3. **Resource cleanup**: Properly close JDBC + SSH tunnels + SSL temp files

### Secondary Goals (Future - Phase 2)
4. **Smart lifecycle states**: IDLE / ACTIVE / BACKGROUND_TASK
5. **Idle timeout**: Auto-disconnect after N minutes of inactivity
6. **Background task protection**: Don't disconnect if a service/worker is using the connection

---

## Non-Goals

- ❌ Connection pooling across multiple databases simultaneously
- ❌ Connection persistence across app restarts
- ❌ Automatic reconnection (manual reconnect only)

---

## Proposed Solution

### Phase 1: Manual + Switch Disconnect (MVP)

#### 1. Add Disconnect Button to UI

**Location**: Workspace screen (when a connection is active)

```kotlin
// Top bar action
IconButton(onClick = { viewModel.disconnect() }) {
    Icon(Icons.Default.PowerOff, "Disconnect")
}
```

**Behavior**:
- Visible only when a connection is active
- Shows confirmation dialog: "Disconnect from {name}?"
- On confirm: Close JDBC → Close SSH tunnel → Clear state → Navigate to connections list

#### 2. Auto-Disconnect on Connection Switch

**Trigger**: User opens a different connection from the list

**Flow**:
```
1. User taps connection B (connection A is active)
2. Disconnect connection A:
   - Close activeConnection (JDBC)
   - Call connectionPool.close() → closes SSH tunnel
   - Clear currentEngine
3. Connect to connection B
```

**Implementation**:
```kotlin
// In DatabaseRepositoryImpl.kt
override suspend fun connect(config: ConnectionConfig): Result<Connection> {
    // Auto-disconnect previous connection
    currentEngine?.disconnect()
    currentEngine = null
    
    // Connect to new one
    val engine = engineFactory.create(config.type, context)
    val result = engine.connect(config)
    
    if (result.isSuccess) {
        currentEngine = engine
    }
    
    return result
}
```

#### 3. Cleanup Order (Critical for SSH)

**Correct order** (reverse of establishment):
```
1. Close JDBC connection
2. Cleanup SSL temp certificates
3. Disconnect SSH tunnel
```

**Already implemented** in `MySQLConnectionPool.close()` ✅

---

### Phase 2: Smart Lifecycle States (Future)

**Connection States**:

```kotlin
enum class ConnectionState {
    DISCONNECTED,    // No active connection
    CONNECTING,      // Establishing connection (SSH → JDBC)
    IDLE,            // Connected but no active queries
    ACTIVE,          // Executing queries
    BACKGROUND_TASK, // Service/Worker using connection
    ERROR            // Connection failed or lost
}
```

**State Transitions**:

```
DISCONNECTED → CONNECTING → IDLE
IDLE → ACTIVE (query starts)
ACTIVE → IDLE (query completes)
IDLE → BACKGROUND_TASK (service starts)
BACKGROUND_TASK → IDLE (service completes)
IDLE → DISCONNECTED (manual disconnect or timeout)
* → ERROR (connection lost)
```

**Timeout Rules**:

| State | Timeout | Action |
|-------|---------|--------|
| IDLE | 5 minutes | Auto-disconnect + notify user |
| ACTIVE | None | Reset timeout when query completes |
| BACKGROUND_TASK | None | Never auto-disconnect |

**Benefits**:
- ✅ Automatic resource cleanup for forgotten connections
- ✅ Background services protected from interruption
- ✅ User gets notified: "Connection auto-closed after 5 min of inactivity"

---

## Implementation Plan

### Phase 1: MVP (Immediate)

**Step 1: Add DisconnectUseCase**

```kotlin
// File: domain/usecases/DisconnectFromDatabaseUseCase.kt
class DisconnectFromDatabaseUseCase @Inject constructor(
    private val repository: DatabaseRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.disconnect()
    }
}
```

**Step 2: Update DatabaseRepositoryImpl**

Add auto-disconnect logic in `connect()` method (already shown above).

**Step 3: Update UI - Add Disconnect Button**

```kotlin
// In WorkspaceScreen or DatabasesListScreen
if (connectionState is Connected) {
    IconButton(onClick = { 
        showDisconnectDialog = true 
    }) {
        Icon(PhosphorIcons.Power, "Disconnect")
    }
}

if (showDisconnectDialog) {
    AlertDialog(
        title = "Disconnect",
        text = "Close connection to ${currentConnection.name}?",
        confirmButton = {
            TextButton(onClick = {
                viewModel.disconnect()
                showDisconnectDialog = false
            }) {
                Text("Disconnect")
            }
        },
        dismissButton = {
            TextButton(onClick = { showDisconnectDialog = false }) {
                Text("Cancel")
            }
        }
    )
}
```

**Step 4: Add Connection State to UI**

```kotlin
// Show connection status indicator
Row {
    Icon(
        PhosphorIcons.Circle,
        tint = if (isConnected) Color.Green else Color.Gray,
        modifier = Modifier.size(8.dp)
    )
    Text("Connected to: ${currentConnection.name}")
}
```

---

### Phase 2: Smart States (Future)

**Step 1: Add ConnectionStateManager**

```kotlin
class ConnectionStateManager {
    private val _state = MutableStateFlow<ConnectionState>(DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()
    
    private var idleJob: Job? = null
    
    fun markActive() {
        _state.value = ACTIVE
        idleJob?.cancel()
    }
    
    fun markIdle() {
        _state.value = IDLE
        startIdleTimeout()
    }
    
    fun markBackgroundTask() {
        _state.value = BACKGROUND_TASK
        idleJob?.cancel()
    }
    
    private fun startIdleTimeout() {
        idleJob = scope.launch {
            delay(5.minutes)
            if (_state.value == IDLE) {
                disconnect()
            }
        }
    }
}
```

**Step 2: Integrate with Repository**

```kotlin
// In DatabaseRepositoryImpl
private val stateManager = ConnectionStateManager()

override suspend fun executeQuery(...): Result<QueryResult> {
    stateManager.markActive()
    val result = currentEngine?.executeQuery(...)
    stateManager.markIdle()
    return result
}
```

**Step 3: Background Task Protection**

```kotlin
// In BackupWorker / SyncWorker
class BackupWorker : CoroutineWorker() {
    override suspend fun doWork(): Result {
        stateManager.markBackgroundTask()
        
        try {
            // Run backup...
        } finally {
            stateManager.markIdle()
        }
        
        return Result.success()
    }
}
```

---

## Testing Strategy

### Manual Testing

**Phase 1**:
- [ ] Open connection A → verify state = Connected
- [ ] Click Disconnect → verify state = Disconnected
- [ ] Verify SSH tunnel closed (check Logcat for "SSH session disconnected")
- [ ] Open connection A → open connection B → verify A auto-disconnected
- [ ] Check no memory leaks (Android Profiler)

**Phase 2**:
- [ ] Connect → wait 5 min idle → verify auto-disconnect + notification
- [ ] Start query → idle timer should reset
- [ ] Start background service → verify no auto-disconnect during task

### Unit Tests

```kotlin
@Test
fun `disconnect closes engine and clears state`() = runTest {
    repository.connect(mockConfig)
    repository.disconnect()
    
    verify(mockEngine).disconnect()
    assertNull(repository.currentEngine)
}

@Test
fun `connecting to new database disconnects previous`() = runTest {
    repository.connect(configA)
    repository.connect(configB)
    
    verify(engineA).disconnect()
    verify(engineB).connect(configB)
}
```

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| User loses work if auto-disconnected mid-query | High | Phase 2 only; mark ACTIVE during queries |
| Background service interrupted by timeout | Critical | Phase 2: BACKGROUND_TASK state prevents disconnect |
| SSH tunnel not properly closed | Medium | Already handled in MySQLConnectionPool.close() |
| User forgets to disconnect (resource leak) | Low | Phase 2: idle timeout cleanup |

---

## Success Metrics

**Phase 1**:
- ✅ Manual disconnect works 100% of the time
- ✅ Auto-disconnect on switch works 100% of the time
- ✅ No SSH tunnels remain after disconnect (verify with `netstat`)
- ✅ No memory leaks detected in 30-min session

**Phase 2**:
- ✅ Idle timeout triggers after 5 min ± 10s
- ✅ Background tasks never interrupted
- ✅ User receives notification on auto-disconnect
- ✅ Average memory usage reduced by 30% in multi-connection scenarios

---

## Dependencies

**Required**:
- `DatabaseRepository.disconnect()` (already exists ✅)
- `MySQLConnectionPool.close()` (already exists ✅)
- `SSHTunnelManager.disconnect()` (already exists ✅)

**New**:
- `DisconnectFromDatabaseUseCase` (Phase 1)
- `ConnectionStateManager` (Phase 2)
- Idle timeout coroutine (Phase 2)

---

## Open Questions

1. ❓ Should we show a "Reconnect" button after auto-disconnect?
2. ❓ What should happen to open query editor tabs when disconnected?
3. ❓ Should we persist "last connected" state across app restarts?

---

## References

- Navicat behavior: Manual disconnect + auto-close on exit
- DBeaver behavior: Connection pooling + idle timeout (5 min default)
- MySQL Workbench: Manual disconnect only
