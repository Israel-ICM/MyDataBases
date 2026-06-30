# Connection Folders & Reordering - Apply Progress

**Change**: connection-folders  
**Status**: IN PROGRESS (70% complete)  
**Started**: 2026-06-30  
**Target**: Full folder organization + drag & drop reordering

---

## Progress Summary

### ✅ Phase 1: Data Layer (100% complete)

**Completed**:
- [x] ConnectionFolder entity created
- [x] ConnectionEntity updated with folderId and order fields
- [x] ConnectionConfig updated with folderId and order fields
- [x] ConnectionListItem sealed class (FolderItem | ConnectionItem)
- [x] FolderDao with full CRUD operations
- [x] ConnectionDao updated with folder queries
- [x] Room Migration 2→3 created and tested
- [x] AppDatabase updated to version 3

**Commits**:
- `3f9eddd` - feat(data): agregar data layer para folders y reordering
- `b19d790` - fix(migration): remover DEFAULT values e indices de migration 2→3

**Files**:
- `core/database/models/ConnectionFolder.kt`
- `core/database/models/ConnectionConfig.kt` (updated)
- `data/local/entities/ConnectionEntity.kt` (updated)
- `domain/models/ConnectionListItem.kt`
- `data/local/dao/FolderDao.kt`
- `data/local/dao/ConnectionDao.kt` (updated)
- `data/local/migrations/Migration_2_3.kt`
- `data/local/AppDatabase.kt` (updated to v3)

---

### ✅ Phase 2: Repository Layer (100% complete)

**Completed**:
- [x] FolderRepository interface
- [x] FolderRepositoryImpl implementation
- [x] ConnectionRepositoryImpl mappers updated
- [x] DatabaseModule updated with FolderDao provider
- [x] RepositoryModule updated with FolderRepository binding

**Commits**:
- `3d9c23d` - feat(data): migration 2→3 y repository layer para folders

**Files**:
- `domain/repositories/FolderRepository.kt`
- `data/repositories/FolderRepositoryImpl.kt`
- `data/repositories/ConnectionRepositoryImpl.kt` (updated)
- `core/di/DatabaseModule.kt` (updated)
- `core/di/RepositoryModule.kt` (updated)

---

### ✅ Phase 3: Domain Layer (100% complete)

**Completed**:
- [x] CreateFolderUseCase (with name validation)
- [x] DeleteFolderUseCase (with moveToRoot safety)
- [x] MoveConnectionToFolderUseCase
- [x] GetGroupedConnectionsUseCase (Flow combine)

**Commits**:
- `cda1519` - feat(domain): use cases para folders

**Files**:
- `domain/usecases/folders/CreateFolderUseCase.kt`
- `domain/usecases/folders/DeleteFolderUseCase.kt`
- `domain/usecases/folders/MoveConnectionToFolderUseCase.kt`
- `domain/usecases/folders/GetGroupedConnectionsUseCase.kt`

---

### 🔄 Phase 4: Presentation Layer (30% complete)

**Completed**:
- [x] Reorder mode UI foundation (Edit button toggle)
- [x] ConnectionCard updated with isReorderMode param
- [x] Drag handle icon (GripVertical) when in reorder mode
- [x] PhosphorAppIcons.Action.dragHandle added

**Pending**:
- [ ] FoldersViewModel or extend ConnectionsListViewModel
- [ ] FolderCard component (expandible with counter)
- [ ] FolderFormSheet (create/edit folder bottom sheet)
- [ ] MoveToFolderSheet (select destination folder)
- [ ] ConnectionsListScreen integration with grouped list
- [ ] Expand/collapse folder functionality
- [ ] Indentation for connections inside folders (16.dp)

**Commits (partial)**:
- `a955238` - feat(ui): agregar modo de reordenamiento de conexiones

**Files (partial)**:
- `ui/screens/connections/ConnectionsListScreen.kt` (reorder mode only)
- `ui/components/ConnectionCard.kt` (drag handle only)
- `ui/components/PhosphorAppIcons.kt` (dragHandle icon)

---

### ❌ Phase 5: Drag & Drop (0% complete)

**Pending**:
- [ ] LazyColumn item reordering logic
- [ ] Drag gesture detection
- [ ] Drop zones for folders
- [ ] Auto-expand folder on hover (500ms delay)
- [ ] Update order fields on drop
- [ ] Reorder scenarios:
  - [ ] Reorder root items (folders + connections)
  - [ ] Reorder inside folder
  - [ ] Move connection root → folder
  - [ ] Move connection folder → root
  - [ ] Move connection folder A → folder B

**Note**: This is Phase 2 (optional for MVP), can be deferred.

---

## Next Steps (Priority Order)

### 1. Create FolderCard Component (HIGH PRIORITY)
**File**: `ui/components/FolderCard.kt`

```kotlin
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
```

**Design**:
- Same IOSCard style as ConnectionCard
- Left: Folder icon (📁) with accent color
- Center: Folder name (bold) + connection count "(3)"
- Right: Expand/collapse chevron (▼/▶) + More menu (⋮)
- Reorder mode: Show drag handle instead of More menu

**Estimated**: 1 hour

---

### 2. Update ConnectionsListViewModel (HIGH PRIORITY)
**File**: `ui/screens/connections/ConnectionsListViewModel.kt`

**Add**:
```kotlin
// Folders state
val groupedConnections: StateFlow<List<ConnectionListItem>> = 
    getGroupedConnectionsUseCase().stateIn(...)

// Folder actions
fun createFolder(name: String)
fun deleteFolder(folderId: String, moveToRoot: Boolean = true)
fun toggleFolderExpand(folderId: String)
fun moveConnectionToFolder(connectionId: String, folderId: String?)
```

**Estimated**: 1 hour

---

### 3. Create FolderFormSheet (MEDIUM PRIORITY)
**File**: `ui/components/folders/FolderFormSheet.kt`

```kotlin
@Composable
fun FolderFormSheet(
    folder: ConnectionFolder? = null,  // null = new folder
    onSave: (name: String) -> Unit,
    onDismiss: () -> Unit
)
```

**Fields**:
- Name (required, TextField)
- [Future] Icon picker (optional)
- [Future] Color picker (optional)

**Validation**:
- Name not empty
- Name max 50 chars

**Estimated**: 1.5 hours

---

### 4. Create MoveToFolderSheet (MEDIUM PRIORITY)
**File**: `ui/components/folders/MoveToFolderSheet.kt`

```kotlin
@Composable
fun MoveToFolderSheet(
    folders: List<ConnectionFolder>,
    currentFolderId: String?,
    onSelectFolder: (String?) -> Unit,  // null = move to root
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit
)
```

**Design**:
- List of folders (selectable)
- "Root level" option (no folder)
- "New folder" option at bottom
- Currently selected folder highlighted

**Estimated**: 1.5 hours

---

### 5. Update ConnectionsListScreen (HIGH PRIORITY)
**File**: `ui/screens/connections/ConnectionsListScreen.kt`

**Changes needed**:

```kotlin
// Replace current flat list with grouped list
val groupedItems by viewModel.groupedConnections.collectAsState()

LazyColumn {
    items(groupedItems, key = { it.id }) { item ->
        when (item) {
            is ConnectionListItem.FolderItem -> {
                FolderCard(
                    folder = item.folder,
                    connectionCount = item.connectionCount,
                    isExpanded = item.folder.isExpanded,
                    onToggleExpand = { viewModel.toggleFolderExpand(item.folder.id) },
                    onEditClick = { /* show edit sheet */ },
                    onDeleteClick = { /* show delete dialog */ },
                    isReorderMode = isReorderMode
                )
                
                // Show connections if expanded
                if (item.folder.isExpanded) {
                    item.connections.forEach { connection ->
                        ConnectionCard(
                            connection = connection,
                            isReorderMode = isReorderMode,
                            modifier = Modifier.padding(start = 16.dp)  // Indent
                            // ... other params
                        )
                    }
                }
            }
            
            is ConnectionListItem.ConnectionItem -> {
                ConnectionCard(
                    connection = item.connection,
                    isReorderMode = isReorderMode,
                    // ... other params
                )
            }
        }
    }
}
```

**Add**:
- Folder creation flow (FAB menu or dedicated button)
- Delete folder confirmation dialog
- Move to folder bottom sheet integration

**Estimated**: 2 hours

---

### 6. Fix GetGroupedConnectionsUseCase (MEDIUM PRIORITY)
**File**: `domain/usecases/folders/GetGroupedConnectionsUseCase.kt`

**Current issue**:
- `connectionCount` is hardcoded to 0 (placeholder)
- Needs proper Flow combination with folder connection counts

**Fix**:
```kotlin
operator fun invoke(): Flow<List<ConnectionListItem>> {
    return combine(
        folderRepository.getAllFolders(),
        connectionDao.getRootConnections()
    ) { folders, rootConnections ->
        buildList {
            // Root connections
            rootConnections.forEach { /* ... */ }
            
            // Folders with counts
            folders.forEach { folder ->
                // This needs to be reactive - combine with connections flow
                val folderConnections = connectionDao
                    .getConnectionsInFolder(folder.id)
                    .first()  // BAD: blocking call
                    
                add(FolderItem(
                    folder = folder,
                    connectionCount = folderConnections.size,
                    connections = if (folder.isExpanded) folderConnections else emptyList()
                ))
            }
        }
    }
}
```

**Better approach**: Create a Flow that combines ALL folder connections flows.

**Estimated**: 1 hour

---

### 7. Add String Resources (LOW PRIORITY)
**Files**: `app/src/main/res/values*/strings.xml`

**New strings needed**:
```xml
<!-- Folders -->
<string name="folder_create">New folder</string>
<string name="folder_edit">Edit folder</string>
<string name="folder_delete">Delete folder</string>
<string name="folder_name">Folder name</string>
<string name="folder_name_hint">My Project</string>
<string name="folder_delete_confirm_title">Delete folder?</string>
<string name="folder_delete_confirm_message">Move %1$d connections to root or delete them?</string>
<string name="folder_delete_move_to_root">Move to root</string>
<string name="folder_delete_all">Delete all</string>
<string name="folder_connections_count">%1$d connections</string>
<string name="folder_empty">No connections in this folder</string>
<string name="move_to_folder">Move to folder</string>
<string name="move_to_root">Root level</string>
```

**Translate to 10 languages**: en, es, fr, de, pt-rBR, ru, zh-rCN, ja, hi, ar

**Estimated**: 1 hour

---

## Testing Checklist

### Manual Testing (when UI is complete)

**Folder CRUD**:
- [ ] Create folder → appears in list
- [ ] Rename folder → name updates
- [ ] Delete empty folder → disappears
- [ ] Delete folder with connections → dialog shows
  - [ ] Move to root → connections appear in root
  - [ ] Delete all → connections disappear
- [ ] Expand/collapse folder → state persists

**Move Connections**:
- [ ] Move connection to folder → appears inside folder
- [ ] Move connection to root → appears in root list
- [ ] Move connection between folders → updates correctly

**Reorder** (when drag & drop is implemented):
- [ ] Reorder folders → order persists
- [ ] Reorder connections inside folder → order persists
- [ ] Reorder root connections → order persists

**Edge Cases**:
- [ ] Empty folder shows "No connections" or collapses
- [ ] Folder with 1 connection shows "(1 connection)" not "(1 connections)"
- [ ] Long folder names truncate with ellipsis
- [ ] Delete last folder → all connections move to root
- [ ] Create folder with empty name → validation error

---

## Known Issues

### 1. Migration validation failed (FIXED)
**Issue**: Room expected `defaultValue='undefined'` but migration had explicit DEFAULT values.

**Fix**: 
- Removed `DEFAULT NULL` from folder_id
- Removed indices from migration
- Commit: `b19d790`

**Status**: ✅ RESOLVED

---

### 2. GetGroupedConnectionsUseCase needs optimization
**Issue**: Connection count per folder is hardcoded to 0.

**Impact**: Folder cards will show "(0 connections)" even when they have connections.

**Fix needed**: Combine flows properly to get real counts.

**Status**: ⚠️ PENDING

---

### 3. Password decryption in lists
**Issue**: `toDomainWithoutDecryption` helper creates ConnectionConfig with empty password.

**Impact**: Low - passwords only needed when connecting, not for displaying lists.

**Future optimization**: Create a separate `ConnectionListItemModel` without password field.

**Status**: ⚠️ TECH DEBT (not blocking)

---

## Estimates

### Remaining Work
- FolderCard component: 1 hour
- ConnectionsListViewModel updates: 1 hour
- FolderFormSheet: 1.5 hours
- MoveToFolderSheet: 1.5 hours
- ConnectionsListScreen integration: 2 hours
- Fix GetGroupedConnectionsUseCase: 1 hour
- String resources (10 languages): 1 hour
- Manual testing: 30 min

**Total**: ~9.5 hours to complete MVP (without drag & drop)

### Drag & Drop (Phase 2 - Optional)
- LazyColumn reordering: 3 hours
- Gesture detection: 2 hours
- Drop zones + auto-expand: 2 hours
- Testing: 1 hour

**Total Phase 2**: ~8 hours

---

## References

- Proposal: `openspec/changes/connection-folders/proposal.md`
- Connection Lifecycle: `openspec/changes/connection-lifecycle/proposal.md`
- Room Migrations Guide: https://developer.android.com/training/data-storage/room/migrating-db-versions
- LazyColumn Reordering: https://developer.android.com/jetpack/compose/lists#item-animations

---

**Last updated**: 2026-06-30  
**Next review**: After Phase 4 completion
