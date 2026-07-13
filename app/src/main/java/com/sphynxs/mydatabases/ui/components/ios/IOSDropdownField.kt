package com.sphynxs.mydatabases.ui.components.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens

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
    
    val keyboardController = LocalSoftwareKeyboardController.current

    // Reset search when menu closes, request focus when opens
    LaunchedEffect(expanded) {
        if (!expanded) {
            searchQuery = ""
        } else if (showFilter) {
            // Delay para que el Dialog se renderice antes de pedir foco
            kotlinx.coroutines.delay(300)
            focusRequester.requestFocus()
            kotlinx.coroutines.delay(100)
            keyboardController?.show()
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
                .background(if (enabled) LocalDesignTokens.current.surfacePrimary else LocalDesignTokens.current.backgroundPrimary)
                .clickable(enabled = enabled && !isLoading) { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Texto seleccionado o placeholder
            Text(
                text = value?.let { itemLabel(it) } ?: placeholder,
                color = if (value != null) LocalDesignTokens.current.textPrimary else LocalDesignTokens.current.textTertiary,
                fontSize = LocalDesignTokens.current.cardTitleSize,
                fontWeight = if (value != null) LocalDesignTokens.current.cardTitleWeight else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            
            // Indicador de carga o flecha
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = LocalDesignTokens.current.iconNormal
                )
            } else if (enabled) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = LocalDesignTokens.current.iconNormal,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // Divider — lo movemos antes del Dialog para mantener el layout del trigger
        if (showDivider) {
            HorizontalDivider(
                color = LocalDesignTokens.current.separator,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }

    // Dialog modal en vez de DropdownMenu para evitar problemas de foco/IME
    // DropdownMenu usa un Popup internamente y no enruta bien el teclado virtual
    if (expanded) {
        Dialog(
            onDismissRequest = { expanded = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = 400.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .background(LocalDesignTokens.current.surfacePrimary, RoundedCornerShape(20.dp))
                    .padding(top = 8.dp)
            ) {
                // Search filter (optional)
                if (showFilter) {
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
                                tint = LocalDesignTokens.current.iconNormal,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = LocalDesignTokens.current.textPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LocalDesignTokens.current.accentPrimary,
                            unfocusedBorderColor = LocalDesignTokens.current.separator,
                            cursorColor = LocalDesignTokens.current.accentPrimary,
                            focusedContainerColor = LocalDesignTokens.current.surfacePrimary,
                            unfocusedContainerColor = LocalDesignTokens.current.surfacePrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { /* Keep dialog open */ }
                        )
                    )
                    HorizontalDivider(
                        color = LocalDesignTokens.current.separator,
                        thickness = 0.5.dp
                    )
                }

                // Items list (scrollable)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (filteredItems.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_results),
                            fontSize = LocalDesignTokens.current.labelSize,
                            color = LocalDesignTokens.current.textSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        filteredItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onValueChange(item)
                                        expanded = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = itemLabel(item),
                                        fontSize = LocalDesignTokens.current.cardTitleSize,
                                        color = LocalDesignTokens.current.textPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    itemSubtitle?.invoke(item)?.let { subtitle ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = subtitle,
                                            fontSize = LocalDesignTokens.current.labelSize,
                                            color = LocalDesignTokens.current.textSecondary,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                                itemTrailing?.invoke(item)
                            }
                            HorizontalDivider(
                                color = LocalDesignTokens.current.separator.copy(alpha = 0.5f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
