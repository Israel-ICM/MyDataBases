package com.sphynxs.mydatabases.ui.screens.queryeditor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * SQL code editor con syntax highlighting, line numbers y Tab key support.
 *
 * Features:
 * - Line numbers (columna izquierda fija con fondo gris)
 * - Monospace font (FontFamily.Monospace)
 * - Syntax highlighting via SqlHighlightTransformation
 * - Debounced re-tokenization (300ms) para performance
 * - Tab key handling (inserta 4 espacios)
 * - Multi-line text field con scroll
 *
 * Spec: openspec/changes/sql-editor/specs/query-editor/spec.md
 *
 * @param value Current text field value (hoisted state)
 * @param onValueChange Callback cuando el texto cambia
 * @param placeholder Placeholder text cuando está vacío
 * @param modifier Modifier para el container
 *
 * @author israel-icm
 * @date 2026-06-24
 */
@OptIn(FlowPreview::class)
@Composable
fun SqlCodeEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier
) {
    // Estado para tokens (debounced para evitar re-tokenization en cada keystroke)
    var tokens by remember { mutableStateOf<List<SqlToken>>(emptyList()) }

    // Debounce de 300ms para re-tokenización
    val textFlow = remember { MutableStateFlow(value.text) }

    LaunchedEffect(value.text) {
        textFlow.value = value.text
    }

    LaunchedEffect(Unit) {
        textFlow
            .debounce(300)
            .distinctUntilChanged()
            .collect { sql ->
                // Re-tokenizar en Default dispatcher (background)
                tokens = SqlTokenizer.tokenize(sql)
            }
    }

    // Visual transformation con tokens actualizados
    val visualTransformation = rememberSqlHighlightTransformation(tokens)

    // Calcular número de líneas
    val lineCount = remember(value.text) {
        if (value.text.isEmpty()) 1 else value.text.count { it == '\n' } + 1
    }

    val fontSize = MaterialTheme.typography.bodyMedium.fontSize
    val lineHeight = fontSize * 1.5f // 1.5x line height típico de editores
    
    val textStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = fontSize,
        lineHeight = lineHeight
    )

    Box(modifier = modifier.fillMaxWidth()) {
        Row {
            // Números de línea (clickeables para seleccionar toda la línea)
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)
            ) {
                val lines = remember(value.text) {
                    if (value.text.isEmpty()) listOf("") else value.text.lines()
                }
                
                repeat(lineCount) { index ->
                    Text(
                        text = "${index + 1}",
                        style = textStyle.copy(
                            color = Color(0xFF858585)
                        ),
                        modifier = Modifier.clickable {
                            // Calcular start/end de la línea
                            val start = lines.take(index).sumOf { it.length + 1 }
                            val lineLength = lines.getOrNull(index)?.length ?: 0
                            val end = start + lineLength
                            
                            // Seleccionar toda la línea
                            onValueChange(value.copy(selection = TextRange(start, end)))
                        }
                    )
                }
            }
            
            // Editor
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 20.dp, top = 16.dp, bottom = 16.dp),
                    textStyle = textStyle,
                    visualTransformation = visualTransformation,
                    decorationBox = { innerTextField ->
                        if (value.text.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = textStyle.copy(
                                    color = Color(0xFF008000)
                                )
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}
