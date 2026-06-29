package com.sphynxs.mydatabases.ui.screens.queryeditor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.focus.focusProperties
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
import androidx.compose.foundation.gestures.animateScrollBy
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
    scrollState: androidx.compose.foundation.ScrollState? = null,
    onShortcut: ((com.sphynxs.mydatabases.domain.editor.ShortcutAction) -> Unit)? = null,
    showCompletionPopup: Boolean = false,
    onCompletionNavigate: ((Int) -> Unit)? = null,
    onCompletionAccept: (() -> Unit)? = null,
    onCompletionDismiss: (() -> Unit)? = null,
    findMatches: List<androidx.compose.ui.text.TextRange> = emptyList(),
    currentMatchIndex: Int = -1,
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
    
    // Auto-scroll cuando el cursor cambia de posición
    LaunchedEffect(value.selection.start, textLayoutResult) {
        scrollState?.let { scroll ->
            textLayoutResult?.let { layout ->
                try {
                    val cursorLine = layout.getLineForOffset(value.selection.start)
                    val cursorTop = layout.getLineTop(cursorLine)
                    val cursorBottom = layout.getLineBottom(cursorLine)
                    
                    // Scroll para mantener el cursor visible
                    val targetScroll = (cursorBottom - 200).coerceAtLeast(0f)
                    scroll.animateScrollTo(targetScroll.toInt())
                } catch (e: Exception) {
                    // Ignorar si el offset está fuera de rango
                }
            }
        }
    }
    
    // Handler para cambios de texto con multi-cursor
    val handleValueChange: (TextFieldValue) -> Unit = { newValue ->
        android.util.Log.d("SqlCodeEditor", "handleValueChange called. Cursors: ${cursorPositions.size}, oldText.length: ${value.text.length}, newText.length: ${newValue.text.length}")
        
        // Task 4.4.2 — TDD GREEN: Auto-close brackets (BR-3, BR-4, BR-5)
        val valueAfterAutoClose = if (cursorPositions.isEmpty()) {
            // For BR-5 check, we need full tokenization to detect if cursor is inside string/comment
            // Performance: only tokenize on single-char insert (auto-close candidates)
            val fullTokens = if (newValue.text.length == value.text.length + 1) {
                SqlTokenizer.tokenize(newValue.text)
            } else {
                emptyList()
            }
            
            applyAutoCloseBrackets(value, newValue, fullTokens)
        } else {
            newValue // Skip auto-close when multi-cursor active
        }
        
        if (cursorPositions.isEmpty()) {
            // Sin multi-cursor, comportamiento normal
            android.util.Log.d("SqlCodeEditor", "No cursors, normal behavior")
            onValueChange(valueAfterAutoClose)
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

    // Visual transformation con tokens actualizados + match highlighting (FR-14)
    val syntaxHighlight = rememberSqlHighlightTransformation(tokens)
    val matchHighlight = remember(findMatches, currentMatchIndex) {
        if (findMatches.isNotEmpty()) {
            MatchHighlightTransformation(findMatches, currentMatchIndex)
        } else {
            null
        }
    }
    
    val visualTransformation = remember(syntaxHighlight, matchHighlight) {
        val layers = mutableListOf(syntaxHighlight)
        if (matchHighlight != null) {
            layers.add(matchHighlight)
        }
        CompositeVisualTransformation(layers)
    }

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
            // Números de línea con overlay clickeable
            Box(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)
            ) {
                // Números (un solo Text para alineación perfecta)
                Text(
                    text = (1..lineCount).joinToString("\n") { it.toString() },
                    style = textStyle.copy(
                        color = Color(0xFF858585)
                    ),
                    onTextLayout = { layoutResult ->
                        textLayoutResult = layoutResult
                    }
                )
                
                // Overlay clickeable invisible
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(value.text) {
                            detectTapGestures { offset ->
                                // Calcular en qué línea se hizo click
                                textLayoutResult?.let { layout ->
                                    val lineIndex = layout.getLineForVerticalPosition(offset.y)
                                    if (lineIndex >= 0 && lineIndex < lineCount) {
                                        // Calcular start/end de esa línea en el texto
                                        val lines = value.text.lines()
                                        val start = lines.take(lineIndex).sumOf { it.length + 1 }
                                        val lineLength = lines.getOrNull(lineIndex)?.length ?: 0
                                        val end = start + lineLength
                                        
                                        // Seleccionar toda la línea
                                        onValueChange(value.copy(selection = TextRange(start, end)))
                                        android.util.Log.d("SqlCodeEditor", "Line $lineIndex selected: $start-$end")
                                    }
                                }
                            }
                        }
                ) {
                    // Canvas invisible - solo para capturar clicks
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
                        .focusProperties {
                            // Deshabilitar navegación de foco con flechas
                            // Las flechas solo deben mover el cursor dentro del editor
                            canFocus = true
                        }
                        .onPreviewKeyEvent { keyEvent ->
                            // Completion navigation (highest priority)
                            if (showCompletionPopup && keyEvent.type == KeyEventType.KeyDown) {
                                when (keyEvent.key) {
                                    Key.DirectionDown -> {
                                        onCompletionNavigate?.invoke(1)
                                        return@onPreviewKeyEvent true
                                    }
                                    Key.DirectionUp -> {
                                        onCompletionNavigate?.invoke(-1)
                                        return@onPreviewKeyEvent true
                                    }
                                    Key.Enter, Key.Tab -> {
                                        onCompletionAccept?.invoke()
                                        return@onPreviewKeyEvent true
                                    }
                                    Key.Escape -> {
                                        onCompletionDismiss?.invoke()
                                        return@onPreviewKeyEvent true
                                    }
                                    else -> { /* continue to shortcuts */ }
                                }
                            }
                            
                            // Interceptar shortcuts ANTES del input
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                val shortcut = com.sphynxs.mydatabases.domain.editor.EditorShortcuts.mapKeyEvent(keyEvent)
                                if (shortcut != null) {
                                    onShortcut?.invoke(shortcut)
                                    return@onPreviewKeyEvent true // Consumir evento
                                }
                            }
                            
                            // Detectar Ctrl presionado/soltado
                            when {
                                keyEvent.key == Key.CtrlLeft || keyEvent.key == Key.CtrlRight -> {
                                    isCtrlPressed = keyEvent.type == KeyEventType.KeyDown
                                    true
                                }
                                else -> false
                            }
                        }
                        .onKeyEvent { keyEvent ->
                            // IMPORTANTE: Consumir TODOS los eventos de teclado después de procesarlos
                            // para que NO se propaguen al sistema de navegación de foco
                            // Esto previene que las flechas cambien el foco entre componentes
                            
                            // Bloquear Enter/Tab si popup está visible
                            if (showCompletionPopup && (keyEvent.key == Key.Enter || keyEvent.key == Key.Tab)) {
                                return@onKeyEvent true
                            }
                            
                            // Consumir TODOS los eventos para evitar navegación de foco
                            true
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

/**
 * Auto-close brackets helper function (BR-3, BR-4, BR-5).
 *
 * Task 4.4.3 — TDD GREEN implementation.
 *
 * Handles six pairs: (), [], {}, '', "", ``
 * Does NOT trigger inside STRING or COMMENT tokens (BR-5).
 *
 * @param oldValue Previous TextFieldValue
 * @param newValue New TextFieldValue after user input
 * @param tokens Current tokenization result (full text tokens)
 * @return Modified TextFieldValue with closing bracket inserted, or unchanged if no auto-close
 */
internal fun applyAutoCloseBrackets(
    oldValue: TextFieldValue,
    newValue: TextFieldValue,
    tokens: List<SqlToken>
): TextFieldValue {
    // Only auto-close on single-character insertion
    if (newValue.text.length != oldValue.text.length + 1) {
        return newValue
    }
    
    // Check if inserted character is an opening bracket
    val insertedChar = newValue.text[newValue.selection.start - 1]
    val closingChar = when (insertedChar) {
        '(' -> ')'
        '[' -> ']'
        '{' -> '}'
        '\'' -> '\''
        '"' -> '"'
        '`' -> '`'
        else -> return newValue // Not a bracket
    }
    
    // BR-5: Check if cursor is inside STRING or COMMENT token
    val cursorOffset = newValue.selection.start
    val tokenAtCursor = tokens.find { cursorOffset - 1 in it.range }
    
    if (tokenAtCursor != null && 
        (tokenAtCursor.kind == TokenKind.STRING || tokenAtCursor.kind == TokenKind.COMMENT)) {
        // Inside string/comment, do NOT auto-close
        return newValue
    }
    
    // Insert closing bracket
    val textWithClose = newValue.text.substring(0, cursorOffset) + 
                       closingChar + 
                       newValue.text.substring(cursorOffset)
    
    // Keep cursor between the pair
    return TextFieldValue(
        text = textWithClose,
        selection = TextRange(cursorOffset)
    )
}
