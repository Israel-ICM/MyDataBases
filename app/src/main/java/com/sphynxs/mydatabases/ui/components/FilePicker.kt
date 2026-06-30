package com.sphynxs.mydatabases.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * File picker composable helper.
 *
 * Permite seleccionar archivos del dispositivo usando el sistema de archivos de Android.
 *
 * @param onFileSelected Callback con la URI del archivo seleccionado
 * @param mimeTypes Tipos MIME aceptados (default: todos los archivos)
 * @param content Composable que recibe la función para lanzar el picker
 *
 * @author israel-icm
 * @date 2026-06-30
 */
@Composable
fun FilePicker(
    onFileSelected: (Uri) -> Unit,
    mimeTypes: Array<String> = arrayOf("*/*"),
    content: @Composable (launchPicker: () -> Unit) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onFileSelected(it) }
    }
    
    content {
        launcher.launch(mimeTypes)
    }
}

/**
 * Extension para obtener el nombre del archivo desde una URI.
 */
fun Uri.getFileName(context: android.content.Context): String? {
    val cursor = context.contentResolver.query(this, null, null, null, null)
    return cursor?.use {
        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && nameIndex != -1) {
            it.getString(nameIndex)
        } else {
            this.lastPathSegment
        }
    }
}
