# Connection Folders & Grouping

**Status**: Planned  
**Priority**: Medium  
**Complexity**: Medium-High  
**Estimated effort**: 4-5 hours

---

## Problem Statement

Currently, all connections appear in a flat list:

- ❌ No way to organize connections by project, environment, or client
- ❌ Long lists become hard to scan when you have 10+ connections
- ❌ No visual grouping for related databases (e.g., prod/staging/dev)
- ❌ Users have to scroll through everything to find a specific connection

**Real-world scenarios:**
- Freelancer with 5 clients, each with 3 environments (15 connections total)
- Developer working on 3 projects, each with MySQL + PostgreSQL
- DBA managing production, staging, QA, and development servers

---

## Goals

### Primary Goals
1. **Create folders**: User can create named folders/groups
2. **Organize connections**: Drag connections into folders
3. **Root-level connections**: Connections without folder stay in main list
4. **Expand/collapse**: Folders can be expanded or collapsed
5. **Reorder**: Folders and connections can be reordered (both in root and inside folders)

### Secondary Goals (Future - Phase 2)
6. **Folder icons**: Custom icons or colors for folders
7. **Nested folders**: Folders inside folders (max 2 levels)
8. **Smart folders**: Auto-group by database type, host, or tags

---

## Non-Goals

- ❌ Folder-based permissions or access control
- ❌ Sharing folders between devices/users
- ❌ Folder sync with cloud

---

## Proposed Data Model

### Database Schema

```kotlin
// New entity: ConnectionFolder
@Entity(tableName = "connection_folders")
data class ConnectionFolder(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String? = null,          // Optional: icon identifier
    val color: String? = null,         // Optional: hex color
    val isExpanded: Boolean = true,    // UI state: folder open/closed
    val order: Int = 0,                // Position in the list
    val createdAt: Long = System.currentTimeMillis()
)

// Update existing entity: ConnectionConfig
@Entity(tableName = "connections")
data class ConnectionConfig(
    // ... existing fields ...
    val folderId: String? = null,      // NULL = root level
    val order: Int = 0                 // Position within folder or root
)
```

**Migration**:
- Add `folderId` column (nullable) to `connections` table
- Add `order` column (default 0) to `connections` table
- Create `connection_folders` table

---

## UI Design

### List Structure

```
┌─────────────────────────────────────────┐
│ Connections                    [Edit]   │  ← Header with reorder button
├─────────────────────────────────────────┤
│                                         │
│  Personal Projects          [▼]  [⋮]   │  ← Folder (expanded)
│  ├─ 🟢 MyApp Dev                   [⋮] │  ← Connection inside folder
│  └─ MyApp Staging                  [⋮] │
│                                         │
│  🟢 Production Main                [⋮] │  ← Root-level connection
│                                         │
│  Client ABC                 [▶]  [⋮]   │  ← Folder (collapsed)
│                                         │
│  Testing                          [⋮] │  ← Root-level connection
│                                         │
└─────────────────────────────────────────┘
```

**Visual hierarchy**:
- Folders: Bold text, expand/collapse icon (▼ / ▶), folder menu (⋮)
- Connections inside folder: Indented 16.dp from left
- Root connections: No indentation

### Folder Card Design

```kotlin
// New component: FolderCard
@Composable
fun FolderCard(
    folder: ConnectionFolder,
    connectionCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isReorderMode: Boolean = false
)
```

**Appearance**:
- Same iOS card style as ConnectionCard
- Left: Folder icon (📁) with accent color
- Center: Folder name (bold) + connection count "(3 connections)"
- Right: Expand/collapse icon + More menu

### Folder Menu Actions

**Normal mode** (More button):
- Edit folder
- Delete folder (with warning if has connections)

**Reorder mode**:
- Drag handle (≡) to reorder folders

---

## User Flows

### Create Folder

**Entry points**:
1. Long-press on connection → "Move to folder..." → "New folder"
2. Reorder mode → "New folder" button at bottom
3. Empty state: "Organize with folders" CTA

**Flow**:
```
1. Tap "New folder"
2. Bottom sheet appears: "New Folder"
   - Name field (required)
   - [Optional] Icon picker
   - [Optional] Color picker
3. Tap "Create"
4. Folder appears in the list (expanded by default)
```

### Move Connection to Folder

**Option A: Drag & Drop** (in reorder mode)
```
1. Enable reorder mode (Edit button)
2. Drag connection handle (≡)
3. Drop on folder or between items
4. Folder auto-expands on hover (500ms delay)
5. Drop inside folder → connection moves
```

**Option B: Context Menu** (normal mode)
```
1. Tap More (⋮) on connection
2. "Move to folder..."
3. Bottom sheet with folder list + "New folder" option
4. Select folder
5. Connection moves
```

### Delete Folder

**Safety checks**:
```
IF folder has connections:
  Show dialog: "Move 3 connections to root or delete them?"
  Options:
    - Move to root (keep connections)
    - Delete all (destructive, red button)
    - Cancel

IF folder is empty:
  Delete immediately (no confirmation)
```

---

## Implementation Plan

### Phase 1: Data Layer

**Step 1: Database Migration**

```kotlin
// Migration 3 → 4
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add order column to connections
        database.execSQL("ALTER TABLE connections ADD COLUMN `order` INTEGER NOT NULL DEFAULT 0")
        
        // Add folderId column to connections
        database.execSQL("ALTER TABLE connections ADD COLUMN folder_id TEXT DEFAULT NULL")
        
        // Create folders table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS connection_folders (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                icon TEXT,
                color TEXT,
                is_expanded INTEGER NOT NULL DEFAULT 1,
                `order` INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
        """)
        
        // Create index for faster folder lookups
        database.execSQL("CREATE INDEX index_connections_folder_id ON connections(folder_id)")
    }
}
```

**Step 2: Repository & DAO**

```kotlin
// New: FolderDao
@Dao
interface FolderDao {
    @Query("SELECT * FROM connection_folders ORDER BY `order` ASC")
    fun getAllFolders(): Flow<List<ConnectionFolder>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: ConnectionFolder)
    
    @Delete
    suspend fun deleteFolder(folder: ConnectionFolder)
    
    @Query("UPDATE connection_folders SET is_expanded = :isExpanded WHERE id = :folderId")
    suspend fun updateFolderExpandState(folderId: String, isExpanded: Boolean)
    
    @Query("UPDATE connection_folders SET `order` = :order WHERE id = :folderId")
    suspend fun updateFolderOrder(folderId: String, order: Int)
}

// Update: ConnectionDao
@Dao
interface ConnectionDao {
    // Add new queries
    @Query("SELECT * FROM connections WHERE folder_id IS NULL ORDER BY `order` ASC")
    fun getRootConnections(): Flow<List<ConnectionConfig>>
    
    @Query("SELECT * FROM connections WHERE folder_id = :folderId ORDER BY `order` ASC")
    fun getConnectionsInFolder(folderId: String): Flow<List<ConnectionConfig>>
    
    @Query("UPDATE connections SET folder_id = :folderId WHERE id = :connectionId")
    suspend fun moveToFolder(connectionId: String, folderId: String?)
    
    @Query("UPDATE connections SET `order` = :order WHERE id = :connectionId")
    suspend fun updateConnectionOrder(connectionId: String, order: Int)
    
    @Query("SELECT COUNT(*) FROM connections WHERE folder_id = :folderId")
    suspend fun getConnectionCountInFolder(folderId: String): Int
}
```

**Step 3: Domain Models**

```kotlin
// New: Grouped list item for UI
sealed class ConnectionListItem {
    data class FolderItem(
        val folder: ConnectionFolder,
        val connectionCount: Int,
        val connections: List<ConnectionConfig> = emptyList()
    ) : ConnectionListItem()
    
    data class ConnectionItem(
        val connection: ConnectionConfig,
        val isInFolder: Boolean = false
    ) : ConnectionListItem()
}
```

---

### Phase 2: UI Layer

**Step 1: New Components**

```kotlin
// FolderCard.kt
@Composable
fun FolderCard(
    folder: ConnectionFolder,
    connectionCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isReorderMode: Boolean = false,
    modifier: Modifier = Modifier
)

// FolderFormSheet.kt
@Composable
fun FolderFormSheet(
    folder: ConnectionFolder? = null,  // null = new folder
    onSave: (name: String) -> Unit,
    onDismiss: () -> Unit
)

// MoveFolderSheet.kt
@Composable
fun MoveToFolderSheet(
    folders: List<ConnectionFolder>,
    currentFolderId: String?,
    onSelectFolder: (String?) -> Unit,  // null = move to root
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit
)
```

**Step 2: Update ConnectionsListScreen**

```kotlin
// Transform flat list to grouped structure
val groupedItems by remember(connections, folders) {
    derivedStateOf {
        buildList {
            // Add root-level connections
            rootConnections.forEach { conn ->
                add(ConnectionListItem.ConnectionItem(conn, isInFolder = false))
            }
            
            // Add folders with their connections
            folders.forEach { folder ->
                val folderConnections = connectionsInFolder[folder.id] ?: emptyList()
                add(ConnectionListItem.FolderItem(folder, folderConnections.size, folderConnections))
            }
        }.sortedBy { /* order field */ }
    }
}

// Render in LazyColumn
LazyColumn {
    items(groupedItems, key = { /* stable key */ }) { item ->
        when (item) {
            is ConnectionListItem.FolderItem -> {
                FolderCard(
                    folder = item.folder,
                    connectionCount = item.connectionCount,
                    isExpanded = item.folder.isExpanded,
                    onToggleExpand = { viewModel.toggleFolder(item.folder.id) },
                    // ...
                )
                
                // Show connections if expanded
                if (item.folder.isExpanded) {
                    item.connections.forEach { connection ->
                        ConnectionCard(
                            connection = connection,
                            // ... with 16.dp start padding for indent
                        )
                    }
                }
            }
            
            is ConnectionListItem.ConnectionItem -> {
                ConnectionCard(connection = item.connection, /* ... */)
            }
        }
    }
}
```

---

### Phase 3: Drag & Drop Integration

**Reorder scenarios**:

1. **Reorder root items** (folders + connections at root)
2. **Reorder inside folder** (connections within the same folder)
3. **Move connection from root → folder**
4. **Move connection from folder → root**
5. **Move connection from folder A → folder B**

**Implementation strategy**:

Use `LazyListState` + `Modifier.dragAndDrop` (Compose 1.7+) or custom gesture detection.

```kotlin
// Simplified drag logic
var draggedItem by remember { mutableStateOf<ConnectionListItem?>(null) }
var dropTarget by remember { mutableStateOf<ConnectionListItem?>(null) }

// On drop:
when {
    draggedItem is ConnectionItem && dropTarget is FolderItem -> {
        // Move connection into folder
        viewModel.moveConnectionToFolder(draggedItem.connection.id, dropTarget.folder.id)
    }
    
    draggedItem is ConnectionItem && dropTarget == null -> {
        // Move connection to root
        viewModel.moveConnectionToFolder(draggedItem.connection.id, null)
    }
    
    else -> {
        // Reorder within same level
        viewModel.reorderItems(draggedItem, dropTarget)
    }
}
```

---

## Edge Cases & Validation

| Scenario | Behavior |
|----------|----------|
| Delete folder with connections | Show dialog: move to root or delete all |
| Drag folder onto connection | Not allowed (swap order instead) |
| Drag connection onto itself | No-op |
| Folder name empty | Validation error, cannot save |
| Duplicate folder name | Allowed (folders identified by UUID) |
| Delete last folder | Allowed, all connections move to root |
| Expand folder during drag | Auto-expand after 500ms hover |

---

## Testing Strategy

### Manual Testing

- [ ] Create folder → appears in list
- [ ] Rename folder → name updates
- [ ] Delete empty folder → disappears
- [ ] Delete folder with connections → dialog shows → move to root works
- [ ] Delete folder with connections → delete all works
- [ ] Expand/collapse folder → state persists across navigation
- [ ] Move connection to folder → appears inside folder
- [ ] Move connection to root → appears in root list
- [ ] Reorder folders → order persists
- [ ] Reorder connections inside folder → order persists
- [ ] Reorder root connections → order persists

### Unit Tests

```kotlin
@Test
fun `moveConnectionToFolder updates folderId`() = runTest {
    val connection = testConnection.copy(folderId = null)
    dao.insert(connection)
    
    dao.moveToFolder(connection.id, "folder-123")
    
    val updated = dao.getConnectionById(connection.id)
    assertEquals("folder-123", updated?.folderId)
}

@Test
fun `deleteFolder with connections moves them to root`() = runTest {
    val folder = testFolder
    val connection = testConnection.copy(folderId = folder.id)
    
    folderDao.insert(folder)
    connectionDao.insert(connection)
    
    // Business logic: before deleting folder, move connections to root
    connectionDao.moveConnectionsToRoot(folder.id)
    folderDao.delete(folder)
    
    val updated = connectionDao.getConnectionById(connection.id)
    assertNull(updated?.folderId)
}
```

---

## Success Metrics

**Phase 1**:
- ✅ Users can create folders
- ✅ Connections can be moved to folders via menu
- ✅ Folders can be expanded/collapsed
- ✅ Delete folder works (with safety dialog)

**Phase 2**:
- ✅ Drag & drop works for reordering
- ✅ Drag connection into folder works
- ✅ Order persists across app restarts
- ✅ No performance issues with 50+ connections in 10 folders

---

## Dependencies

**Required**:
- Room migration 3 → 4
- New DAOs and repositories
- FolderCard component
- MoveToFolderSheet component

**Optional (Phase 2)**:
- Drag & drop library or custom gesture detection
- Icon picker component
- Color picker component

---

## Open Questions

1. ❓ Should folders remember expanded/collapsed state per device or be global?
2. ❓ Max folder name length? (Suggest 50 characters)
3. ❓ Allow nested folders (folders inside folders)? Not in MVP
4. ❓ Show connection count badge on folder? Yes, like "(3)"
5. ❓ Auto-create "Favorites" or "Recent" smart folders? Future feature

---

## References

- iOS Files app: Folder UI pattern
- Navicat: Connection grouping (tree structure)
- DBeaver: Folder-based organization
- Material Design: Expandable lists
