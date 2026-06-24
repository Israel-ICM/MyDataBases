package com.sphynxs.mydatabases.ui.screens.queryeditor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.sphynxs.mydatabases.ui.components.ResultGrid
import com.sphynxs.mydatabases.ui.screens.queryeditor.components.SqlCodeEditor

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
    initialSql: String? = null,
    viewModel: QueryEditorViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    var sqlText by remember { mutableStateOf(TextFieldValue(initialSql ?: "")) }
    val uiState by viewModel.uiState.collectAsState()
    val cursorPositions = remember { mutableStateListOf<Int>() }
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showSaveDialog by remember { mutableStateOf(false) }
    var savedFileName by remember { mutableStateOf("") }
    
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
            // SQL Editor (estilo VS Code - modo claro)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                SqlCodeEditor(
                    value = sqlText,
                    onValueChange = { sqlText = it },
                    placeholder = "-- Enter SQL query...",
                    cursorPositions = cursorPositions,
                    scrollState = scrollState,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Execute/Cancel button (toggle based on running state)
                if (uiState is QueryEditorUiState.Running) {
                    // Cancel button (red, stop icon, circular)
                    Button(
                        onClick = { viewModel.cancel() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336), // Rojo
                            contentColor = Color.White
                        ),
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = stringResource(R.string.cancel_button)
                        )
                    }
                } else {
                    // Execute button (green, play icon, circular)
                    Button(
                        onClick = {
                            viewModel.executeStatements(sql = sqlText.text)
                        },
                        enabled = sqlText.text.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50), // Verde
                            contentColor = Color.White
                        ),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics {
                                contentDescription = "Execute query"
                            },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.execute_button)
                        )
                    }
                }

                // Open file button (icon only, circular)
                OutlinedButton(
                    onClick = { 
                        openFileLauncher.launch("*/*") // Acepta todos los archivos
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = "Open SQL file"
                    )
                }
                
                // Save button (icon only, circular)
                OutlinedButton(
                    onClick = { 
                        try {
                            // Generar nombre de archivo con timestamp
                            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                            val fileName = "query_$timestamp.sql"
                            
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                // Android 10+ (API 29+): Usar MediaStore
                                // Ruta: /storage/emulated/0/Documents/MyDatabase/query/
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
                                // Android 9 y anteriores: Acceso directo
                                // Ruta: /storage/emulated/0/MyDatabase/query/
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
                            // TODO: Mostrar error al usuario
                        }
                    },
                    enabled = sqlText.text.isNotBlank(),
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "Save query"
                    )
                }
                
                // Clear button (adaptive: clear cursors or clear text, circular)
                OutlinedButton(
                    onClick = { 
                        if (cursorPositions.isNotEmpty()) {
                            // Si hay cursores (badge >= 1), limpiar solo cursores
                            cursorPositions.clear()
                            android.util.Log.d("QueryEditorScreen", "Cursors cleared")
                        } else {
                            // Si no hay cursores (badge vacío), limpiar texto
                            sqlText = TextFieldValue("")
                            android.util.Log.d("QueryEditorScreen", "Text cleared")
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    if (cursorPositions.size >= 1) {
                        // Mostrar "|×" cuando hay 1+ cursores (badge visible)
                        Text(
                            text = "|×",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    } else {
                        // Mostrar ícono X cuando badge está vacío
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.clear_button)
                        )
                    }
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
                    OutlinedButton(
                        onClick = {
                            val currentPos = sqlText.selection.start
                            android.util.Log.d("QueryEditorScreen", "Click |+ at position $currentPos, cursorPositions: $cursorPositions, contains: ${cursorPositions.contains(currentPos)}")
                            if (!cursorPositions.contains(currentPos)) {
                                cursorPositions.add(currentPos)
                                android.util.Log.d("QueryEditorScreen", "✅ Cursor added at $currentPos")
                            } else {
                                cursorPositions.remove(currentPos)
                                android.util.Log.d("QueryEditorScreen", "❌ Cursor removed from $currentPos")
                            }
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "|+",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Result pane
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
                        is QueryEditorUiState.Idle -> {
                            Text(
                                text = stringResource(R.string.query_editor_empty_state),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

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
                    }
                }
            }
        }
    
    // Diálogo de confirmación después de guardar
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text("Archivo guardado")
            },
            text = {
                Column {
                    Text("El archivo '$savedFileName' se guardó correctamente.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("¿Desea cerrar el editor o continuar modificando?")
                }
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
                    Text("Cerrar editor")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSaveDialog = false
                        android.util.Log.d("QueryEditorScreen", "Continue editing after save")
                    }
                ) {
                    Text("Continuar editando")
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
            text = "Update Summary",
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
                            text = "Rows affected: ${result.affectedRows ?: 0}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Time: ${result.executionTimeMs}ms",
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
                text = "✓ Success",
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
                text = "Execution time: ${executionTimeMs}ms",
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
                    text = "Failed statement:",
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
