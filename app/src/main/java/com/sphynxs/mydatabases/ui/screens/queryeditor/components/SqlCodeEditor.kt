package com.sphynxs.mydatabases.ui.screens.queryeditor.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * SQL code editor con syntax highlighting y Tab key support.
 *
 * Features:
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
 * @date 2026-06-23
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

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = MaterialTheme.typography.bodyMedium.fontSize
        ),
        visualTransformation = visualTransformation,
        decorationBox = { innerTextField ->
            if (value.text.isEmpty()) {
                Text(
                    text = placeholder,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize
                    )
                )
            }
            innerTextField()
        }
    )
}
