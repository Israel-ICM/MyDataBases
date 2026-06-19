package com.sphynxs.mydatabases.ui.components.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.sphynxs.mydatabases.ui.theme.DesignTokens

/**
 * Dropdown field estilo iOS para usar dentro de IOSGroupedCard.
 *
 * Mantiene el mismo estilo visual que IOSTextField pero abre un menú dropdown
 * al tocarlo.
 *
 * @param value Valor seleccionado actual (puede ser null)
 * @param onValueChange Callback cuando se selecciona un item
 * @param placeholder Texto placeholder cuando no hay selección
 * @param items Lista de items para mostrar en el dropdown
 * @param itemLabel Lambda para obtener el label de cada item
 * @param showDivider Si mostrar divider inferior (default true)
 * @param isLoading Si está cargando datos (muestra spinner)
 * @param enabled Si el campo está habilitado (default true)
 * @param modifier Modificador opcional
 *
 * @author israel-icm
 * @date 2026-06-19
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> IOSDropdownField(
    value: T?,
    onValueChange: (T) -> Unit,
    placeholder: String,
    items: List<T>,
    itemLabel: (T) -> String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    itemSubtitle: ((T) -> String)? = null,
    itemTrailing: (@Composable (T) -> Unit)? = null,
    showFilter: Boolean = false,
    filterPlaceholder: String = "Search..."
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    
    // Reset search when menu closes, request focus when opens
    LaunchedEffect(expanded) {
        if (!expanded) {
            searchQuery = ""
        } else if (showFilter) {
            // Small delay to ensure popup is fully rendered
            kotlinx.coroutines.delay(100)
            focusRequester.requestFocus()
        }
    }
    
    // Filter items based on search query
    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isEmpty()) {
            items
        } else {
            items.filter { item ->
                itemLabel(item).contains(searchQuery, ignoreCase = true) ||
                itemSubtitle?.invoke(item)?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }
    
    Column(modifier = modifier) {
        // Campo principal (trigger del dropdown)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (enabled) DesignTokens.SurfacePrimary else DesignTokens.BackgroundPrimary)
                .clickable(enabled = enabled && !isLoading) { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Texto seleccionado o placeholder
            Text(
                text = value?.let { itemLabel(it) } ?: placeholder,
                color = if (value != null) DesignTokens.TextPrimary else DesignTokens.TextTertiary,
                fontSize = DesignTokens.CardTitleSize,
                fontWeight = if (value != null) DesignTokens.CardTitleWeight else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            
            // Indicador de carga o flecha
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = DesignTokens.IconNormal
                )
            } else if (enabled) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = DesignTokens.IconNormal,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // Dropdown menu moderno estilo iOS
        MaterialTheme(
            shapes = MaterialTheme.shapes.copy(
                extraSmall = RoundedCornerShape(20.dp)
            )
        ) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(x = 0.dp, y = 8.dp),
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White,
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
                properties = PopupProperties(clippingEnabled = false),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = 400.dp)
            ) {
                // Search filter (optional)
                if (showFilter) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = filterPlaceholder,
                                    fontSize = 15.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = DesignTokens.IconNormal,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                color = DesignTokens.TextPrimary
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DesignTokens.AccentPrimary,
                                unfocusedBorderColor = DesignTokens.Separator,
                                cursorColor = DesignTokens.AccentPrimary,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { /* Keep dropdown open */ }
                            )
                        )
                        HorizontalDivider(
                            color = DesignTokens.Separator,
                            thickness = 0.5.dp
                        )
                    }
                }
                
                // Items list
                filteredItems.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = itemLabel(item),
                                    fontSize = DesignTokens.CardTitleSize,
                                    color = DesignTokens.TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                itemSubtitle?.invoke(item)?.let { subtitle ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = subtitle,
                                        fontSize = DesignTokens.LabelSize,
                                        color = DesignTokens.TextSecondary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        },
                        onClick = {
                            onValueChange(item)
                            expanded = false
                        },
                        trailingIcon = itemTrailing?.let { trailing ->
                            { trailing(item) }
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
        
        // Divider
        if (showDivider) {
            HorizontalDivider(
                color = DesignTokens.Separator,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
