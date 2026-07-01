package com.sphynxs.mydatabases.ui.components.folders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.core.database.models.ConnectionFolder

/**
 * Bottom sheet para crear o editar un folder.
 *
 * Muestra un formulario simple con campo de nombre y botones de guardar/cancelar.
 * Incluye validación de nombre (no vacío, máximo 50 caracteres).
 *
 * @param folder El folder a editar (null = crear nuevo)
 * @param onSave Callback cuando se guarda el folder con el nombre ingresado
 * @param onDismiss Callback cuando se cierra el sheet sin guardar
 *
 * @author israel-icm
 * @date 2026-07-01
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderFormSheet(
    folder: ConnectionFolder? = null,
    onSave: (name: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(folder?.name ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }
    
    // Validación de nombre
    val isValid = name.isNotBlank() && name.length <= 50
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Título
            Text(
                text = if (folder == null) {
                    stringResource(R.string.folder_create)
                } else {
                    stringResource(R.string.folder_edit)
                },
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Campo de nombre
            OutlinedTextField(
                value = name,
                onValueChange = { newValue ->
                    name = newValue
                    nameError = when {
                        newValue.isBlank() -> "Folder name cannot be empty"
                        newValue.length > 50 -> "Maximum 50 characters"
                        else -> null
                    }
                },
                label = { Text(stringResource(R.string.folder_name)) },
                placeholder = { Text(stringResource(R.string.folder_name_hint)) },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Botones
            Button(
                onClick = {
                    if (isValid) {
                        onSave(name.trim())
                        onDismiss()
                    }
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_save))
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_cancel))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
