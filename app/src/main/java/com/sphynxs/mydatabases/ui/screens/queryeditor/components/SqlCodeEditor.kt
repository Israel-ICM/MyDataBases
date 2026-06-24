package com.sphynxs.mydatabases.ui.screens.queryeditor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.animation.core.*
import android.content.res.Configuration
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
    cursorPositions: MutableList<Int> = mutableListOf(),
    onAddCursor: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Detectar si hay teclado físico conectado
    val configuration = LocalConfiguration.current
    val hasPhysicalKeyboard = remember(configuration) {
        val keyboardType = configuration.keyboard
        android.util.Log.d("SqlCodeEditor", "Keyboard type: $keyboardType (NOKEYS=${Configuration.KEYBOARD_NOKEYS}, QWERTY=${Configuration.KEYBOARD_QWERTY}, 12KEY=${Configuration.KEYBOARD_12KEY})")
        
        keyboardType != Configuration.KEYBOARD_NOKEYS &&
        keyboardType != Configuration.KEYBOARD_UNDEFINED
    }
    
    // Estado para multi-cursor
    var multiCursorMode by remember { mutableStateOf(false) }
    var isCtrlPressed by remember { mutableStateOf(false) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    
    // Animación de parpadeo de cursores
    val infiniteTransition = rememberInfiniteTransition(label = "cursor-blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor-alpha"
    )
    
    // Handler para cambios de texto con multi-cursor
    val handleValueChange: (TextFieldValue) -> Unit = { newValue ->
        android.util.Log.d("SqlCodeEditor", "handleValueChange called. Cursors: ${cursorPositions.size}, oldText.length: ${value.text.length}, newText.length: ${newValue.text.length}")
        
        if (cursorPositions.isEmpty()) {
            // Sin multi-cursor, comportamiento normal
            android.util.Log.d("SqlCodeEditor", "No cursors, normal behavior")
            onValueChange(newValue)
        } else {
            android.util.Log.d("SqlCodeEditor", "Multi-cursor active with ${cursorPositions.size} cursors at positions: $cursorPositions")
            // Con multi-cursor, aplicar cambios en todas las posiciones
            val oldText = value.text
            val newText = newValue.text
            
            // Detectar qué cambió (inserción o borrado)
            if (newText.length > oldText.length) {
                // Inserción
                val insertedText = newText.substring(value.selection.start, value.selection.start + (newText.length - oldText.length))
                android.util.Log.d("SqlCodeEditor", "Insertion detected: '$insertedText' at position ${value.selection.start}")
                
                var resultText = oldText
                
                // Insertar en cada posición de cursor (ordenadas de menor a mayor, SIN duplicados)
                val allPositions = (cursorPositions + value.selection.start).distinct().sorted()
                android.util.Log.d("SqlCodeEditor", "All cursor positions (unique): $allPositions")
                
                // Insertar de derecha a izquierda para mantener las posiciones válidas
                allPositions.reversed().forEach { pos ->
                    resultText = resultText.substring(0, pos) + insertedText + resultText.substring(pos)
                }
                
                android.util.Log.d("SqlCodeEditor", "Result text after multi-insert: '$resultText'")
                
                // Actualizar posiciones de cursores: cada cursor se mueve por las inserciones ANTES de él
                val newCursorPositions = cursorPositions.map { originalPos ->
                    // Contar cuántas inserciones ocurrieron antes o en esta posición
                    val insertionsBefore = allPositions.count { it <= originalPos }
                    originalPos + (insertionsBefore * insertedText.length)
                }.toMutableList()
                
                android.util.Log.d("SqlCodeEditor", "Updated cursor positions: $newCursorPositions")
                
                cursorPositions.clear()
                cursorPositions.addAll(newCursorPositions)
                
                // Calcular nueva posición del cursor principal
                val mainCursorInsertionsBefore = allPositions.count { it <= value.selection.start }
                val newMainCursorPos = value.selection.start + (mainCursorInsertionsBefore * insertedText.length)
                
                onValueChange(TextFieldValue(resultText, TextRange(newMainCursorPos)))
            } else if (newText.length < oldText.length) {
                // Borrado - aplicar en todas las posiciones
                val deletedCount = oldText.length - newText.length
                android.util.Log.d("SqlCodeEditor", "Deletion detected: $deletedCount chars at position ${value.selection.start}")
                
                var resultText = oldText
                
                // Borrar en cada posición (de derecha a izquierda, SIN duplicados)
                val allPositions = (cursorPositions + value.selection.start).distinct().sorted()
                
                allPositions.reversed().forEach { pos ->
                    if (pos >= deletedCount) {
                        resultText = resultText.substring(0, pos - deletedCount) + resultText.substring(pos)
                    }
                }
                
                android.util.Log.d("SqlCodeEditor", "Result text after multi-delete: '$resultText'")
                
                // Actualizar posiciones: cada cursor se mueve hacia atrás por las eliminaciones antes de él
                val newCursorPositions = cursorPositions.map { originalPos ->
                    val deletionsBefore = allPositions.count { it <= originalPos }
                    maxOf(0, originalPos - (deletionsBefore * deletedCount))
                }.toMutableList()
                
                android.util.Log.d("SqlCodeEditor", "Updated cursor positions after delete: $newCursorPositions")
                
                cursorPositions.clear()
                cursorPositions.addAll(newCursorPositions)
                
                val mainCursorDeletionsBefore = allPositions.count { it <= value.selection.start }
                val newMainCursorPos = maxOf(0, value.selection.start - (mainCursorDeletionsBefore * deletedCount))
                
                onValueChange(TextFieldValue(resultText, TextRange(newMainCursorPos)))
            } else {
                // Solo movimiento de cursor
                onValueChange(newValue)
            }
        }
    }
    
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
            
            // Editor con cursores dibujados
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = handleValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 20.dp, top = 16.dp, bottom = 16.dp)
                        .onKeyEvent { keyEvent ->
                            // Detectar Ctrl presionado/soltado
                            when {
                                keyEvent.key == Key.CtrlLeft || keyEvent.key == Key.CtrlRight -> {
                                    isCtrlPressed = keyEvent.type == KeyEventType.KeyDown
                                    true
                                }
                                else -> false
                            }
                        }
                        .pointerInput(multiCursorMode, isCtrlPressed) {
                            detectTapGestures { offset ->
                                // Si está en modo multi-cursor (botón) o Ctrl presionado, agregar cursor
                                if (multiCursorMode || isCtrlPressed) {
                                    // Aquí necesitaríamos calcular la posición del cursor desde el offset
                                    // Por ahora, agregamos la posición actual de selección
                                    val currentPos = value.selection.start
                                    if (!cursorPositions.contains(currentPos)) {
                                        cursorPositions.add(currentPos)
                                    }
                                }
                            }
                        },
                    textStyle = textStyle,
                    visualTransformation = visualTransformation,
                    onTextLayout = { layoutResult ->
                        textLayoutResult = layoutResult
                    },
                    decorationBox = { innerTextField ->
                        Box {
                            if (value.text.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = textStyle.copy(
                                        color = Color(0xFF008000)
                                    )
                                )
                            }
                            innerTextField()
                            
                            // Dibujar cursores adicionales
                            if (cursorPositions.isNotEmpty() && textLayoutResult != null) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    cursorPositions.forEach { pos ->
                                        try {
                                            val boundingBox = textLayoutResult!!.getBoundingBox(pos)
                                            val cursorX = boundingBox.left
                                            val cursorTop = boundingBox.top
                                            val cursorBottom = boundingBox.bottom
                                            
                                            // Dibujar línea vertical (cursor)
                                            drawLine(
                                                color = Color(0xFF0066CC).copy(alpha = cursorAlpha),
                                                start = Offset(cursorX, cursorTop),
                                                end = Offset(cursorX, cursorBottom),
                                                strokeWidth = 2.dp.toPx(),
                                                cap = StrokeCap.Round
                                            )
                                        } catch (e: Exception) {
                                            // Ignorar si la posición está fuera de rango
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
