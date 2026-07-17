package com.sphynxs.mydatabases.ui.components.tables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.core.database.models.ColumnDefinition
import com.sphynxs.mydatabases.core.database.models.ColumnDefinitionValidation
import com.sphynxs.mydatabases.core.database.models.SqlColumnType
import com.sphynxs.mydatabases.ui.components.ios.IOSButton
import com.sphynxs.mydatabases.ui.components.ios.IOSButtonStyle
import com.sphynxs.mydatabases.ui.components.ios.IOSDropdownField
import com.sphynxs.mydatabases.ui.components.ios.IOSTextField
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens

/**
 * Todos los tipos de columna MySQL/MariaDB soportados, en el orden mostrado en el
 * dropdown Tipo (change `create-table`, PR-3).
 */
private val ALL_SQL_COLUMN_TYPES: List<SqlColumnType> = listOf(
    SqlColumnType.Int,
    SqlColumnType.TinyInt,
    SqlColumnType.SmallInt,
    SqlColumnType.BigInt,
    SqlColumnType.VarChar,
    SqlColumnType.Char,
    SqlColumnType.Decimal,
    SqlColumnType.Numeric,
    SqlColumnType.Float,
    SqlColumnType.Double,
    SqlColumnType.Text,
    SqlColumnType.LongText,
    SqlColumnType.Boolean,
    SqlColumnType.Date,
    SqlColumnType.DateTime,
    SqlColumnType.Timestamp,
    SqlColumnType.Time,
)

/**
 * Diálogo anidado "Agregar campo" (change `create-table`, PR-3).
 *
 * Sheet-styled `Dialog` (NO un segundo `ModalBottomSheet` anidado — ver design.md,
 * decisión "Nested field form"), con estado de formulario local. Reutiliza las
 * funciones puras de [ColumnDefinitionValidation] (PR-1) para toda la validación
 * cruzada; nunca reimplementa esas reglas.
 *
 * Orden de campos por spec (Requirement: Open Field Definition Dialog): Nombre, Tipo,
 * Longitud, Decimales, Nulo, Virtual, Expresión, Llave, Comentario.
 *
 * - Longitud/Decimales se muestran solo cuando [SqlColumnType.supportsLength]/
 *   [SqlColumnType.supportsDecimals] aplican al tipo seleccionado.
 * - Nulo se oculta por completo cuando Virtual=true (nulabilidad derivada de la
 *   expresión) y se deshabilita — sin ocultarse — cuando Llave=true con Virtual=false.
 * - Expresión se muestra y es requerida solo cuando Virtual=true.
 * - El modo de almacenamiento (`VIRTUAL`/`STORED`) es 100% derivado vía
 *   [ColumnDefinition.generatedStorageMode] — no expone ningún control propio.
 *
 * El OK del diálogo NUNCA ejecuta SQL: solo valida y, si es válido, invoca
 * [onFieldConfirmed] con el [ColumnDefinition] construido (Requirement: Field Dialog OK
 * Appends Without SQL). Cancelar/descartar el diálogo invoca [onDismiss] sin llamar a
 * [onFieldConfirmed] — el estado local del formulario se descarta naturalmente al salir
 * de composición (mismo patrón que el `name` local de `CreateTableFormContent`).
 *
 * @param onDismiss Cierra el diálogo sin agregar campo (Cancel o dismiss)
 * @param onFieldConfirmed Invocado con el [ColumnDefinition] validado cuando el usuario
 *   confirma con OK
 * @param modifier Modificador opcional para el contenedor raíz del diálogo
 *
 * @author sdd-apply
 * @date 2026-07-17
 */
@Composable
fun FieldDefinitionDialog(
    onDismiss: () -> Unit,
    onFieldConfirmed: (ColumnDefinition) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalDesignTokens.current

    // Estado local del formulario — descartado al salir de composición (Cancel/dismiss)
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf<SqlColumnType?>(null) }
    var length by remember { mutableStateOf("") }
    var decimals by remember { mutableStateOf("") }
    var nullable by remember { mutableStateOf(true) }
    var isVirtual by remember { mutableStateOf(false) }
    var expression by remember { mutableStateOf("") }
    var isPrimaryKey by remember { mutableStateOf(false) }
    var comment by remember { mutableStateOf("") }

    // Errores de validación inline, uno por campo con reglas propias
    var nameError by remember { mutableStateOf<String?>(null) }
    var typeError by remember { mutableStateOf<String?>(null) }
    var expressionError by remember { mutableStateOf<String?>(null) }

    val lengthApplicable = type?.let { ColumnDefinitionValidation.isLengthApplicable(it) } ?: false
    val decimalsApplicable = type?.let { ColumnDefinitionValidation.isDecimalsApplicable(it) } ?: false
    val nuloEditable = ColumnDefinitionValidation.isNuloEditable(isVirtual, isPrimaryKey)

    // Strings localizados (labels, hints, mensajes de error)
    val errorNameRequired = stringResource(R.string.field_def_error_name_required)
    val errorNameInvalid = stringResource(R.string.field_def_error_name_invalid)
    val errorTypeRequired = stringResource(R.string.field_def_error_type_required)
    val errorExpressionRequired = stringResource(R.string.field_def_error_expression_required)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 640.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp))
                .background(tokens.surfacePrimary, RoundedCornerShape(20.dp))
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.field_def_dialog_title),
                fontSize = tokens.sectionTitleSize,
                fontWeight = tokens.sectionTitleWeight,
                color = tokens.sectionTitleColor,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // (1) Nombre
                Column {
                    IOSTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = null
                        },
                        placeholder = stringResource(R.string.field_def_name_hint),
                        showDivider = false,
                    )
                    nameError?.let { FieldErrorText(it) }
                }

                // (2) Tipo
                Column {
                    IOSDropdownField(
                        value = type,
                        onValueChange = { selected ->
                            type = selected
                            typeError = null
                            if (!ColumnDefinitionValidation.isLengthApplicable(selected)) length = ""
                            if (!ColumnDefinitionValidation.isDecimalsApplicable(selected)) decimals = ""
                        },
                        placeholder = stringResource(R.string.field_def_type_hint),
                        items = ALL_SQL_COLUMN_TYPES,
                        itemLabel = { it.sqlName },
                        showDivider = false,
                    )
                    typeError?.let { FieldErrorText(it) }
                }

                // (3) Longitud — solo visible para tipos con supportsLength
                if (lengthApplicable) {
                    IOSTextField(
                        value = length,
                        onValueChange = { length = it.filter { ch -> ch.isDigit() } },
                        placeholder = stringResource(R.string.field_def_length_hint),
                        showDivider = false,
                        keyboardType = KeyboardType.Number,
                    )
                }

                // (4) Decimales — solo visible para tipos con supportsDecimals
                if (decimalsApplicable) {
                    IOSTextField(
                        value = decimals,
                        onValueChange = { decimals = it.filter { ch -> ch.isDigit() } },
                        placeholder = stringResource(R.string.field_def_decimals_hint),
                        showDivider = false,
                        keyboardType = KeyboardType.Number,
                    )
                }

                // (5) Nulo — oculto por completo cuando Virtual=true; deshabilitado
                // (no oculto) cuando Llave=true con Virtual=false
                if (!isVirtual) {
                    FieldSwitchRow(
                        label = stringResource(R.string.field_def_nullable_label),
                        checked = nullable,
                        onCheckedChange = { nullable = it },
                        enabled = nuloEditable,
                    )
                }

                // (6) Virtual
                FieldSwitchRow(
                    label = stringResource(R.string.field_def_virtual_label),
                    checked = isVirtual,
                    onCheckedChange = { checked ->
                        isVirtual = checked
                        nullable = ColumnDefinitionValidation.resolveNullable(nullable, isPrimaryKey, checked)
                        if (!checked) expressionError = null
                    },
                )

                // (7) Expresión — solo visible y requerida cuando Virtual=true
                if (isVirtual) {
                    Column {
                        IOSTextField(
                            value = expression,
                            onValueChange = {
                                expression = it
                                expressionError = null
                            },
                            placeholder = stringResource(R.string.field_def_expression_hint),
                            showDivider = false,
                        )
                        expressionError?.let { FieldErrorText(it) }
                    }
                }

                // (8) Llave — fuerza Nulo=false cuando Virtual=false (design.md: el modo
                // de almacenamiento STORED/VIRTUAL se deriva automáticamente, sin control propio)
                FieldSwitchRow(
                    label = stringResource(R.string.field_def_primary_key_label),
                    checked = isPrimaryKey,
                    onCheckedChange = { checked ->
                        isPrimaryKey = checked
                        nullable = ColumnDefinitionValidation.resolveNullable(nullable, checked, isVirtual)
                    },
                )

                // (9) Comentario — opcional
                IOSTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = stringResource(R.string.field_def_comment_hint),
                    showDivider = false,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IOSButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                    style = IOSButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )

                IOSButton(
                    text = stringResource(R.string.field_def_button_ok),
                    onClick = {
                        val trimmedName = name.trim()
                        val selectedType = type
                        var hasError = false

                        nameError = when {
                            trimmedName.isBlank() -> {
                                hasError = true
                                errorNameRequired
                            }
                            !ColumnDefinitionValidation.isValidName(trimmedName) -> {
                                hasError = true
                                errorNameInvalid
                            }
                            else -> null
                        }

                        typeError = if (selectedType == null) {
                            hasError = true
                            errorTypeRequired
                        } else {
                            null
                        }

                        val expressionRequired = ColumnDefinitionValidation.isExpressionRequired(isVirtual)
                        expressionError = if (
                            expressionRequired &&
                            !ColumnDefinitionValidation.isExpressionValid(expression, isVirtual)
                        ) {
                            hasError = true
                            errorExpressionRequired
                        } else {
                            null
                        }

                        if (!hasError && selectedType != null) {
                            val resolvedNullable = ColumnDefinitionValidation.resolveNullable(
                                nullable,
                                isPrimaryKey,
                                isVirtual,
                            )

                            onFieldConfirmed(
                                ColumnDefinition(
                                    name = trimmedName,
                                    type = selectedType,
                                    length = if (ColumnDefinitionValidation.isLengthApplicable(selectedType)) {
                                        length.toIntOrNull()
                                    } else {
                                        null
                                    },
                                    decimals = if (ColumnDefinitionValidation.isDecimalsApplicable(selectedType)) {
                                        decimals.toIntOrNull()
                                    } else {
                                        null
                                    },
                                    nullable = resolvedNullable,
                                    isVirtual = isVirtual,
                                    expression = if (isVirtual) expression.trim() else null,
                                    isPrimaryKey = isPrimaryKey,
                                    comment = comment.trim().ifBlank { null },
                                ),
                            )
                        }
                    },
                    style = IOSButtonStyle.Primary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Fila etiqueta + `Switch` para los controles booleanos (Nulo/Virtual/Llave), mirroring
 * el patrón de toggle de `ConnectionFormScreen` (SSL/Advanced connection).
 */
@Composable
private fun FieldSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val tokens = LocalDesignTokens.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 17.sp,
            color = tokens.textPrimary,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = tokens.accentPrimary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = tokens.separator,
            ),
        )
    }
}

/** Texto de error inline mostrado bajo un campo cuando su validación falla. */
@Composable
private fun FieldErrorText(message: String, modifier: Modifier = Modifier) {
    val tokens = LocalDesignTokens.current
    Text(
        text = message,
        fontSize = tokens.captionSize,
        color = tokens.destructiveAction,
        modifier = modifier.padding(top = 4.dp),
    )
}
