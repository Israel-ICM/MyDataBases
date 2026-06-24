package com.sphynxs.mydatabases.ui.screens.queryeditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sphynxs.mydatabases.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import com.sphynxs.mydatabases.domain.editor.EditorSnapshot
import com.sphynxs.mydatabases.ui.components.ResultGrid
import com.sphynxs.mydatabases.ui.screens.queryeditor.components.SqlCodeEditor
import kotlinx.coroutines.launch

/**
 * Pantalla del editor de queries SQL.
 *
 * Features:
 * - Editor con syntax highlighting (SqlCodeEditor)
 * - Toolbar con botones Execute/Cancel/Clear
 * - Result pane con ResultGrid (SELECT) o UpdateSummary (INSERT/UPDATE/DELETE)
 * - Error display
 * - Loading state
 *
 * Spec: openspec/changes/sql-editor/specs/query-editor/spec.md
 *
 * @param connectionId ID de la conexión activa
 * @param initialSql SQL inicial (null = editor vacío)
 * @param viewModel ViewModel (inyectado por Hilt)
 *
 * @author israel-icm
 * @date 2026-06-23
 */
@Composable
fun QueryEditorScreen(
    connectionId: String,
    databaseName: String? = null,
    initialSql: String? = null,
    viewModel: QueryEditorViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var sqlText by remember { mutableStateOf(TextFieldValue(initialSql ?: "")) }
    val uiState by viewModel.uiState.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val cursorPositions = remember { mutableStateListOf<Int>() }
    
    // Load schema snapshot when databaseName changes
    LaunchedEffect(databaseName) {
        viewModel.loadSchema(databaseName)
    }
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showSaveDialog by remember { mutableStateOf(false) }
    var savedFileName by remember { mutableStateOf("") }
    
    // Completion state
    var showCompletionPopup by remember { mutableStateOf(false) }
    var completionSuggestions by remember { mutableStateOf<List<com.sphynxs.mydatabases.domain.completion.CompletionSuggestion>>(emptyList()) }
    var selectedSuggestionIndex by remember { mutableStateOf(0) }
    
    // Launcher para abrir archivo SQL
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val fileContent = reader.use { it.readText() }
                sqlText = TextFieldValue(fileContent)
                cursorPositions.clear() // Limpiar cursores al cargar archivo
                android.util.Log.d("QueryEditorScreen", "File loaded: ${fileContent.length} characters")
            } catch (e: Exception) {
                android.util.Log.e("QueryEditorScreen", "Error loading file", e)
                // TODO: Mostrar error al usuario
            }
        }
    }
    
    // Launcher para guardar archivo SQL
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            try {
                val outputStream = context.contentResolver.openOutputStream(it)
                outputStream?.use { stream ->
                    stream.write(sqlText.text.toByteArray())
                }
                android.util.Log.d("QueryEditorScreen", "File saved: ${sqlText.text.length} characters to $uri")
                // TODO: Mostrar mensaje de éxito al usuario
            } catch (e: Exception) {
                android.util.Log.e("QueryEditorScreen", "Error saving file", e)
                // TODO: Mostrar error al usuario
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
            // SQL Editor (estilo VS Code - modo claro) + Completion Popup
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    SqlCodeEditor(
                    value = sqlText,
                    onValueChange = { newValue ->
                        sqlText = newValue
                        // Push to history
                        viewModel.pushHistory(
                            text = newValue.text,
                            selection = newValue.selection,
                            cursorPositions = cursorPositions.toList()
                        )
                    },
                    placeholder = "-- Enter SQL query...",
                    cursorPositions = cursorPositions,
                    scrollState = scrollState,
                    onShortcut = { shortcut ->
                        when (shortcut) {
                            com.sphynxs.mydatabases.domain.editor.ShortcutAction.Run -> {
                                if (sqlText.text.isNotBlank()) {
                                    viewModel.executeStatements(sql = sqlText.text)
                                }
                            }
                            com.sphynxs.mydatabases.domain.editor.ShortcutAction.Save -> {
                                if (sqlText.text.isNotBlank()) {
                                    // Trigger save (same as Save button click)
                                    try {
                                        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                                        val fileName = "query_$timestamp.sql"
                                        
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            val resolver = context.contentResolver
                                            val contentValues = ContentValues().apply {
                                                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                                                put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/MyDatabase/query")
                                            }
                                            
                                            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                                            uri?.let {
                                                resolver.openOutputStream(it)?.use { outputStream ->
                                                    outputStream.write(sqlText.text.toByteArray())
                                                }
                                                savedFileName = fileName
                                                showSaveDialog = true
                                            }
                                        } else {
                                            val storageDir = android.os.Environment.getExternalStorageDirectory()
                                            val myDatabaseDir = File(storageDir, "MyDatabase")
                                            val queryDir = File(myDatabaseDir, "query")
                                            
                                            if (!queryDir.exists()) {
                                                queryDir.mkdirs()
                                            }
                                            
                                            val file = File(queryDir, fileName)
                                            FileOutputStream(file).use { outputStream ->
                                                outputStream.write(sqlText.text.toByteArray())
                                            }
                                            
                                            savedFileName = fileName
                                            showSaveDialog = true
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("QueryEditorScreen", "❌ Error saving file", e)
                                    }
                                }
                            }
                            com.sphynxs.mydatabases.domain.editor.ShortcutAction.Undo -> {
                                viewModel.undo()?.let { snapshot ->
                                    sqlText = TextFieldValue(
                                        text = snapshot.text,
                                        selection = snapshot.selection
                                    )
                                    cursorPositions.clear()
                                    cursorPositions.addAll(snapshot.cursorPositions)
                                }
                            }
                            com.sphynxs.mydatabases.domain.editor.ShortcutAction.Redo -> {
                                viewModel.redo()?.let { snapshot ->
                                    sqlText = TextFieldValue(
                                        text = snapshot.text,
                                        selection = snapshot.selection
                                    )
                                    cursorPositions.clear()
                                    cursorPositions.addAll(snapshot.cursorPositions)
                                }
                            }
                            com.sphynxs.mydatabases.ui.screens.queryeditor.domain.ShortcutAction.Format -> {
                                if (sqlText.text.isNotBlank()) {
                                    // Push current state to history before formatting
                                    viewModel.pushHistory(
                                        text = sqlText.text,
                                        selection = sqlText.selection,
                                        cursorPositions = cursorPositions.toList()
                                    )
                                    
                                    // Format SQL
                                    kotlinx.coroutines.MainScope().launch {
                                        val formatted = viewModel.formatSql(sqlText.text)
                                        sqlText = TextFieldValue(
                                            text = formatted,
                                            selection = TextRange(0)
                                        )
                                        cursorPositions.clear()
                                        
                                        // Push formatted state to history
                                        viewModel.pushHistory(
                                            text = formatted,
                                            selection = TextRange(0),
                                            cursorPositions = emptyList()
                                        )
                                    }
                                }
                            }
                            com.sphynxs.mydatabases.ui.screens.queryeditor.domain.ShortcutAction.TriggerCompletion -> {
                                // Disable if multi-cursor active
                                if (cursorPositions.isNotEmpty()) {
                                    return@SqlCodeEditor
                                }
                                
                                // Extract prefix (word before cursor)
                                val cursorPos = sqlText.selection.start
                                val textBeforeCursor = sqlText.text.substring(0, cursorPos)
                                val lastWord = textBeforeCursor.split(Regex("\\s+")).lastOrNull() ?: ""
                                
                                if (lastWord.isNotBlank()) {
                                    completionSuggestions = viewModel.getSuggestions(
                                        prefix = lastWord,
                                        context = textBeforeCursor
                                    )
                                    selectedSuggestionIndex = 0
                                    showCompletionPopup = completionSuggestions.isNotEmpty()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                )
            }
            
            // Completion popup
            if (showCompletionPopup && completionSuggestions.isNotEmpty()) {
                com.sphynxs.mydatabases.ui.screens.queryeditor.components.CompletionPopup(
                    suggestions = completionSuggestions,
                    selectedIndex = selectedSuggestionIndex,
                    anchorOffset = androidx.compose.ui.unit.IntOffset(100, 100), // Simple fixed offset for now
                    onSuggestionClick = { suggestion ->
                        // Insert suggestion at cursor
                        val cursorPos = sqlText.selection.start
                        val textBeforeCursor = sqlText.text.substring(0, cursorPos)
                        val lastWord = textBeforeCursor.split(Regex("\\s+")).lastOrNull() ?: ""
                        val textBeforeWord = textBeforeCursor.dropLast(lastWord.length)
                        val textAfterCursor = sqlText.text.substring(cursorPos)
                        
                        val newText = textBeforeWord + suggestion.text + textAfterCursor
                        val newCursorPos = (textBeforeWord + suggestion.text).length
                        
                        sqlText = TextFieldValue(
                            text = newText,
                            selection = TextRange(newCursorPos)
                        )
                        
                        showCompletionPopup = false
                    },
                    onDismiss = {
                        showCompletionPopup = false
                    }
                )
            }
        }

            Spacer(modifier = Modifier.height(16.dp))

            // Toolbar (dos grupos: izquierda y derecha)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Grupo izquierdo: Open, Save, Clear, Add Cursor (pill shape)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        // Open file button
                        IconButton(
                            onClick = { 
                                openFileLauncher.launch("*/*")
                            }
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = "Open SQL file",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Save button
                        IconButton(
                            onClick = { 
                                try {
                                    val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                                    val fileName = "query_$timestamp.sql"
                                    
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        val resolver = context.contentResolver
                                        val contentValues = ContentValues().apply {
                                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                                            put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/MyDatabase/query")
                                        }
                                        
                                        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                                        uri?.let {
                                            resolver.openOutputStream(it)?.use { outputStream ->
                                                outputStream.write(sqlText.text.toByteArray())
                                            }
                                            android.util.Log.d("QueryEditorScreen", "✅ File saved (MediaStore): Documents/MyDatabase/query/$fileName")
                                            savedFileName = fileName
                                            showSaveDialog = true
                                        }
                                    } else {
                                        val storageDir = android.os.Environment.getExternalStorageDirectory()
                                        val myDatabaseDir = File(storageDir, "MyDatabase")
                                        val queryDir = File(myDatabaseDir, "query")
                                        
                                        if (!queryDir.exists()) {
                                            queryDir.mkdirs()
                                        }
                                        
                                        val file = File(queryDir, fileName)
                                        FileOutputStream(file).use { outputStream ->
                                            outputStream.write(sqlText.text.toByteArray())
                                        }
                                        
                                        android.util.Log.d("QueryEditorScreen", "✅ File saved: ${file.absolutePath}")
                                        savedFileName = fileName
                                        showSaveDialog = true
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("QueryEditorScreen", "❌ Error saving file", e)
                                }
                            },
                            enabled = sqlText.text.isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = "Save query",
                                tint = if (sqlText.text.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                        
                        // Clear button (adaptive)
                        IconButton(
                            onClick = { 
                                if (cursorPositions.isNotEmpty()) {
                                    cursorPositions.clear()
                                    android.util.Log.d("QueryEditorScreen", "Cursors cleared")
                                } else {
                                    sqlText = TextFieldValue("")
                                    android.util.Log.d("QueryEditorScreen", "Text cleared")
                                }
                            }
                        ) {
                            if (cursorPositions.size >= 1) {
                                Text(
                                    text = "|×",
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.clear_button),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        // Undo button
                        IconButton(
                            onClick = {
                                viewModel.undo()?.let { snapshot ->
                                    sqlText = TextFieldValue(
                                        text = snapshot.text,
                                        selection = snapshot.selection
                                    )
                                    cursorPositions.clear()
                                    cursorPositions.addAll(snapshot.cursorPositions)
                                }
                            },
                            enabled = canUndo
                        ) {
                            Icon(
                                Icons.Default.Undo,
                                contentDescription = stringResource(R.string.undo_button),
                                tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                        
                        // Redo button
                        IconButton(
                            onClick = {
                                viewModel.redo()?.let { snapshot ->
                                    sqlText = TextFieldValue(
                                        text = snapshot.text,
                                        selection = snapshot.selection
                                    )
                                    cursorPositions.clear()
                                    cursorPositions.addAll(snapshot.cursorPositions)
                                }
                            },
                            enabled = canRedo
                        ) {
                            Icon(
                                Icons.Default.Redo,
                                contentDescription = stringResource(R.string.redo_button),
                                tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                        
                        // Format button
                        IconButton(
                            onClick = {
                                // Push current state to history before formatting
                                viewModel.pushHistory(
                                    text = sqlText.text,
                                    selection = sqlText.selection,
                                    cursorPositions = cursorPositions.toList()
                                )
                                
                                // Format SQL
                                kotlinx.coroutines.MainScope().launch {
                                    val formatted = viewModel.formatSql(sqlText.text)
                                    sqlText = TextFieldValue(
                                        text = formatted,
                                        selection = TextRange(0) // Reset cursor to start
                                    )
                                    cursorPositions.clear() // Clear multi-cursors when formatting
                                    
                                    // Push formatted state to history (enables undo)
                                    viewModel.pushHistory(
                                        text = formatted,
                                        selection = TextRange(0),
                                        cursorPositions = emptyList()
                                    )
                                }
                            },
                            enabled = sqlText.text.isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.FormatAlignLeft,
                                contentDescription = stringResource(R.string.format_button),
                                tint = if (sqlText.text.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                        
                        // Add cursor button with badge
                        BadgedBox(
                            badge = {
                                if (cursorPositions.isNotEmpty()) {
                                    Badge {
                                        Text(
                                            text = "${cursorPositions.size}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        ) {
                            IconButton(
                                onClick = {
                                    val currentPos = sqlText.selection.start
                                    android.util.Log.d("QueryEditorScreen", "Click |+ at position $currentPos")
                                    if (!cursorPositions.contains(currentPos)) {
                                        cursorPositions.add(currentPos)
                                        android.util.Log.d("QueryEditorScreen", "✅ Cursor added at $currentPos")
                                    } else {
                                        cursorPositions.remove(currentPos)
                                        android.util.Log.d("QueryEditorScreen", "❌ Cursor removed from $currentPos")
                                    }
                                }
                            ) {
                                Text(
                                    text = "|+",
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                
                // Grupo derecho: Run/Stop (pill shape)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        // Execute/Cancel button (toggle based on running state)
                        if (uiState is QueryEditorUiState.Running) {
                            // Cancel button (red stop icon)
                            IconButton(
                                onClick = { viewModel.cancel() }
                            ) {
                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = stringResource(R.string.cancel_button),
                                    tint = Color(0xFFF44336) // Rojo
                                )
                            }
                        } else {
                            // Execute button (green play icon)
                            IconButton(
                                onClick = {
                                    viewModel.executeStatements(sql = sqlText.text)
                                },
                                enabled = sqlText.text.isNotBlank()
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.execute_button),
                                    tint = if (sqlText.text.isNotBlank()) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        }
                    }
                }
            }

            // Result pane (solo visible cuando NO está Idle)
            if (uiState !is QueryEditorUiState.Idle) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (val state = uiState) {
                            is QueryEditorUiState.Running -> {
                                CircularProgressIndicator()
                            }

                            is QueryEditorUiState.SelectResult -> {
                                ResultGrid(
                                    columns = state.result.columns,
                                    rows = state.result.rows,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            is QueryEditorUiState.UpdateSummary -> {
                                UpdateSummaryTable(results = state.results)
                            }

                            is QueryEditorUiState.Success -> {
                                SuccessDisplay(
                                    message = state.message,
                                    executionTimeMs = state.executionTimeMs
                                )
                            }

                            is QueryEditorUiState.Error -> {
                                ErrorDisplay(
                                    message = state.message,
                                    failedStatement = state.failedStatement
                                )
                            }

                            is QueryEditorUiState.Idle -> {
                                // No mostrar nada (ya está filtrado arriba)
                            }
                        }
                    }
                }
            }
        }
    
    // Diálogo de confirmación después de guardar
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(stringResource(R.string.save_dialog_title))
            },
            text = {
                Text(stringResource(R.string.save_dialog_message, savedFileName))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        sqlText = TextFieldValue("")
                        cursorPositions.clear()
                        android.util.Log.d("QueryEditorScreen", "Editor closed after save")
                    }
                ) {
                    Text(stringResource(R.string.save_dialog_close))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        android.util.Log.d("QueryEditorScreen", "Continue editing after save")
                    }
                ) {
                    Text(stringResource(R.string.save_dialog_continue))
                }
            }
        )
    }
}

/**
 * Tabla de resumen para statements UPDATE/INSERT/DELETE.
 *
 * Muestra: statement SQL, rows affected, execution time.
 */
@Composable
private fun UpdateSummaryTable(results: List<com.sphynxs.mydatabases.domain.models.StatementResult>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.update_summary),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        results.forEach { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = result.sql,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.rows_affected_label, result.affectedRows ?: 0),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = stringResource(R.string.execution_time_label, result.executionTimeMs),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * Display de éxito con mensaje verde.
 */
@Composable
private fun SuccessDisplay(
    message: String,
    executionTimeMs: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.success_prefix),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.execution_time_full, executionTimeMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Display de error con mensaje y statement que falló.
 */
@Composable
private fun ErrorDisplay(
    message: String,
    failedStatement: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.query_editor_error_prefix),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            failedStatement?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.failed_statement),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
