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
import com.sphynxs.mydatabases.core.database.models.CharacterSet
import com.sphynxs.mydatabases.core.database.models.Collation
import com.sphynxs.mydatabases.core.database.models.ColumnDefinition
import com.sphynxs.mydatabases.core.database.models.ColumnDefinitionValidation
import com.sphynxs.mydatabases.core.database.models.SqlColumnType
import com.sphynxs.mydatabases.ui.components.ios.IOSButton
import com.sphynxs.mydatabases.ui.components.ios.IOSButtonStyle
import com.sphynxs.mydatabases.ui.components.ios.IOSDropdownField
import com.sphynxs.mydatabases.ui.components.ios.IOSGroupedCard
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
    SqlColumnType.MediumInt,
    SqlColumnType.BigInt,
    SqlColumnType.Bit,
    SqlColumnType.VarChar,
    SqlColumnType.Char,
    SqlColumnType.Decimal,
    SqlColumnType.Numeric,
    SqlColumnType.Float,
    SqlColumnType.Double,
    SqlColumnType.TinyText,
    SqlColumnType.Text,
    SqlColumnType.MediumText,
    SqlColumnType.LongText,
    SqlColumnType.Binary,
    SqlColumnType.VarBinary,
    SqlColumnType.TinyBlob,
    SqlColumnType.Blob,
    SqlColumnType.MediumBlob,
    SqlColumnType.LongBlob,
    SqlColumnType.Json,
    SqlColumnType.Enum,
    SqlColumnType.Set,
    SqlColumnType.Boolean,
    SqlColumnType.Date,
    SqlColumnType.DateTime,
    SqlColumnType.Timestamp,
    SqlColumnType.Time,
    SqlColumnType.Year,
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
 * - Valores se muestra en el mismo lugar (ENUM/SET, change `create-table` ENUM/SET support)
 *   solo cuando [SqlColumnType.supportsValues] aplica al tipo seleccionado — mutuamente
 *   excluyente con Longitud/Decimales, ya que ENUM/SET no soportan ninguno de los dos.
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
 * @param charsets Character sets disponibles para los dropdowns Conjunto de caracteres/Collation
 *   (cargados en vivo por `CreateTableViewModel`, change `create-table` extended field
 *   attributes addendum)
 * @param charsetsLoading Si la lista de charsets está cargando
 * @param collations Collations disponibles para el charset actualmente seleccionado
 * @param collationsLoading Si la lista de collations está cargando
 * @param onCharsetSelected Invocado con el nombre del charset seleccionado, para que el
 *   caller dispare la recarga de collations (mirrors `AddDatabaseViewModel.loadCollations`)
 *
 * @author sdd-apply
 * @date 2026-07-17
 */
@Composable
fun FieldDefinitionDialog(
    onDismiss: () -> Unit,
    onFieldConfirmed: (ColumnDefinition) -> Unit,
    modifier: Modifier = Modifier,
    charsets: List<CharacterSet> = emptyList(),
    charsetsLoading: Boolean = false,
    collations: List<Collation> = emptyList(),
    collationsLoading: Boolean = false,
    onCharsetSelected: (String) -> Unit = {},
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
    var valuesText by remember { mutableStateOf("") }
    var defaultValue by remember { mutableStateOf("") }
    var autoIncrement by remember { mutableStateOf(false) }
    var zeroFill by remember { mutableStateOf(false) }
    var selectedCharset by remember { mutableStateOf<CharacterSet?>(null) }
    var selectedCollation by remember { mutableStateOf<Collation?>(null) }
    var autoUpdateTimestamp by remember { mutableStateOf(false) }

    // Errores de validación inline, uno por campo con reglas propias
    var nameError by remember { mutableStateOf<String?>(null) }
    var typeError by remember { mutableStateOf<String?>(null) }
    var expressionError by remember { mutableStateOf<String?>(null) }
    var valuesError by remember { mutableStateOf<String?>(null) }

    val lengthApplicable = type?.let { ColumnDefinitionValidation.isLengthApplicable(it) } ?: false
    val decimalsApplicable = type?.let { ColumnDefinitionValidation.isDecimalsApplicable(it) } ?: false
    val valuesApplicable = type?.let { ColumnDefinitionValidation.isValuesApplicable(it) } ?: false
    val nuloEditable = ColumnDefinitionValidation.isNuloEditable(isVirtual, isPrimaryKey)
    val defaultApplicable = ColumnDefinitionValidation.isDefaultApplicable(isVirtual, autoIncrement)
    val autoIncrementApplicable = type?.let { ColumnDefinitionValidation.isAutoIncrementApplicable(it, isVirtual) } ?: false
    val zeroFillApplicable = type?.let { ColumnDefinitionValidation.isZeroFillApplicable(it) } ?: false
    val charsetApplicable = type?.let { ColumnDefinitionValidation.isCharsetApplicable(it) } ?: false
    val autoUpdateTimestampApplicable = type?.let { ColumnDefinitionValidation.isAutoUpdateTimestampApplicable(it) } ?: false

    // Strings localizados (labels, hints, mensajes de error)
    val errorNameRequired = stringResource(R.string.field_def_error_name_required)
    val errorNameInvalid = stringResource(R.string.field_def_error_name_invalid)
    val errorTypeRequired = stringResource(R.string.field_def_error_type_required)
    val errorExpressionRequired = stringResource(R.string.field_def_error_expression_required)
    val errorValuesRequired = stringResource(R.string.field_def_error_values_required)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 640.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp))
                // NOTA: fondo backgroundPrimary (no surfacePrimary) a propósito — los campos
                // de adentro (IOSTextField/IOSDropdownField/FieldSwitchRow, agrupados en
                // IOSGroupedCard) usan surfacePrimary para su propia fila; si el contenedor
                // usara el mismo token, los campos quedan invisibles (mismo color = sin
                // contraste, se ven como texto plano). Mismo patrón que CreateTableFormContent.
                .background(tokens.backgroundPrimary, RoundedCornerShape(20.dp))
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
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Card "Identidad": Nombre, Tipo (change `create-table`, visual polish —
                // agrupado en IOSGroupedCard para que los campos tengan contraste real contra
                // el fondo del diálogo; antes ambos compartían el mismo token surfacePrimary
                // que el contenedor y se veían como texto plano, sin apariencia de input).
                IOSGroupedCard {
                    // (1) Nombre
                    Column {
                        IOSTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                nameError = null
                            },
                            placeholder = stringResource(R.string.field_def_name_hint),
                            showDivider = true,
                        )
                        nameError?.let { FieldErrorText(it, Modifier.padding(horizontal = 16.dp)) }
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
                                if (!ColumnDefinitionValidation.isValuesApplicable(selected)) valuesText = ""
                                if (!ColumnDefinitionValidation.isZeroFillApplicable(selected)) zeroFill = false
                                if (!ColumnDefinitionValidation.isCharsetApplicable(selected)) {
                                    selectedCharset = null
                                    selectedCollation = null
                                }
                                if (!ColumnDefinitionValidation.isAutoIncrementApplicable(selected, isVirtual)) {
                                    autoIncrement = false
                                }
                                if (!ColumnDefinitionValidation.isAutoUpdateTimestampApplicable(selected)) {
                                    autoUpdateTimestamp = false
                                }
                            },
                            placeholder = stringResource(R.string.field_def_type_hint),
                            items = ALL_SQL_COLUMN_TYPES,
                            itemLabel = { it.sqlName },
                            showDivider = false,
                            showFilter = true,
                            filterPlaceholder = stringResource(R.string.field_def_type_filter_hint),
                        )
                        typeError?.let { FieldErrorText(it, Modifier.padding(horizontal = 16.dp)) }
                    }
                }

                // Card "Atributos de tipo": Longitud/Decimales/ZeroFill/Valores/Charset/
                // Collation — todos condicionados al tipo seleccionado. Solo se renderiza si
                // al menos uno aplica, para no dejar una card vacía.
                if (lengthApplicable || decimalsApplicable || zeroFillApplicable || valuesApplicable || charsetApplicable) {
                    IOSGroupedCard {
                        // (3) Longitud — solo visible para tipos con supportsLength
                        if (lengthApplicable) {
                            IOSTextField(
                                value = length,
                                onValueChange = { length = it.filter { ch -> ch.isDigit() } },
                                placeholder = stringResource(R.string.field_def_length_hint),
                                showDivider = true,
                                keyboardType = KeyboardType.Number,
                            )
                        }

                        // (4) Decimales — solo visible para tipos con supportsDecimals
                        if (decimalsApplicable) {
                            IOSTextField(
                                value = decimals,
                                onValueChange = { decimals = it.filter { ch -> ch.isDigit() } },
                                placeholder = stringResource(R.string.field_def_decimals_hint),
                                showDivider = true,
                                keyboardType = KeyboardType.Number,
                            )
                        }

                        // (ZeroFill) — solo visible para tipos numéricos con supportsZeroFill
                        // (los 5 tipos enteros + DECIMAL/NUMERIC/FLOAT/DOUBLE). Sin forzado
                        // cruzado: solo afecta el DDL emitido (`UNSIGNED ZEROFILL`, change
                        // `create-table` extended field attributes).
                        if (zeroFillApplicable) {
                            FieldSwitchRow(
                                label = stringResource(R.string.field_def_zerofill_label),
                                checked = zeroFill,
                                onCheckedChange = { zeroFill = it },
                                showDivider = true,
                            )
                        }

                        // (Valores) — solo visible para tipos con supportsValues (ENUM/SET);
                        // ocupa el mismo lugar que Longitud/Decimales ya que estos no aplican
                        // para estos tipos. Texto libre separado por comas.
                        if (valuesApplicable) {
                            Column {
                                IOSTextField(
                                    value = valuesText,
                                    onValueChange = {
                                        valuesText = it
                                        valuesError = null
                                    },
                                    placeholder = stringResource(R.string.field_def_values_hint),
                                    showDivider = true,
                                )
                                valuesError?.let { FieldErrorText(it, Modifier.padding(horizontal = 16.dp)) }
                            }
                        }

                        // (Charset/Collation) — solo visible para tipos con supportsCharset
                        // (CHAR, VARCHAR, TEXT/TINYTEXT/MEDIUMTEXT/LONGTEXT, ENUM, SET).
                        // Cargados en vivo desde el servidor vía CreateTableViewModel
                        // .loadCollations (mirrors AddDatabaseViewModel's charset/collation
                        // live-loading pattern, change `create-table` extended field
                        // attributes addendum). Seleccionar un charset limpia la Collation
                        // seleccionada y dispara [onCharsetSelected] para recargar collations
                        // filtradas por ese charset.
                        if (charsetApplicable) {
                            IOSDropdownField(
                                value = selectedCharset,
                                onValueChange = { selected ->
                                    selectedCharset = selected
                                    selectedCollation = null
                                    onCharsetSelected(selected.name)
                                },
                                placeholder = stringResource(R.string.field_def_charset_hint),
                                items = charsets,
                                itemLabel = { it.name },
                                itemSubtitle = { it.description },
                                showDivider = true,
                                isLoading = charsetsLoading,
                            )
                            IOSDropdownField(
                                value = selectedCollation,
                                onValueChange = { selectedCollation = it },
                                placeholder = stringResource(R.string.field_def_collation_hint),
                                items = collations,
                                itemLabel = { it.name },
                                showDivider = false,
                                isLoading = collationsLoading,
                                enabled = selectedCharset != null,
                            )
                        }
                    }
                }

                // Card "Comportamiento": Valor predeterminado, Nulo, Actualización automática,
                // Virtual, Expresión, Llave, Autoincrement.
                IOSGroupedCard {
                    // (Valor predeterminado) — visible cuando !Virtual && !Autoincrement
                    // (regla nueva, no gateada por tipo). Texto libre OPAQUO: el cliente NUNCA
                    // cita/parsea el valor (el usuario escribe 'texto' o CURRENT_TIMESTAMP/0/
                    // etc. según corresponda), mismo principio que Expresión (change
                    // `create-table` extended field attributes addendum).
                    if (defaultApplicable) {
                        IOSTextField(
                            value = defaultValue,
                            onValueChange = { defaultValue = it },
                            placeholder = stringResource(R.string.field_def_default_value_hint),
                            showDivider = true,
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
                            showDivider = true,
                        )
                    }

                    // (Actualización automática de fecha/hora) — solo visible para TIMESTAMP/
                    // DATETIME (`ON UPDATE CURRENT_TIMESTAMP`, change `create-table` extended
                    // field attributes addendum).
                    if (autoUpdateTimestampApplicable) {
                        FieldSwitchRow(
                            label = stringResource(R.string.field_def_auto_update_timestamp_label),
                            checked = autoUpdateTimestamp,
                            onCheckedChange = { autoUpdateTimestamp = it },
                            showDivider = true,
                        )
                    }

                    // (6) Virtual
                    FieldSwitchRow(
                        label = stringResource(R.string.field_def_virtual_label),
                        checked = isVirtual,
                        onCheckedChange = { checked ->
                            isVirtual = checked
                            nullable = ColumnDefinitionValidation.resolveNullable(nullable, isPrimaryKey, checked)
                            if (!checked) {
                                expressionError = null
                            } else {
                                // Autoincrement es mutuamente excluyente con columnas generadas
                                // (change `create-table` extended field attributes addendum)
                                autoIncrement = false
                            }
                        },
                        showDivider = true,
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
                                showDivider = true,
                            )
                            expressionError?.let { FieldErrorText(it, Modifier.padding(horizontal = 16.dp)) }
                        }
                    }

                    // (8) Llave — fuerza Nulo=false cuando Virtual=false (design.md: el modo
                    // de almacenamiento STORED/VIRTUAL se deriva automáticamente, sin control
                    // propio)
                    FieldSwitchRow(
                        label = stringResource(R.string.field_def_primary_key_label),
                        checked = isPrimaryKey,
                        onCheckedChange = { checked ->
                            isPrimaryKey = checked
                            nullable = ColumnDefinitionValidation.resolveNullable(nullable, checked, isVirtual)
                        },
                        showDivider = autoIncrementApplicable,
                    )

                    // (Autoincrement) — solo visible para los 5 tipos enteros base con
                    // Virtual=false (mutuamente excluyente con columnas generadas). Al
                    // activarse fuerza Llave=true (resolvePrimaryKeyForAutoIncrement), que a
                    // su vez ya fuerza Nulo=false vía la regla Llave→Nulo existente (change
                    // `create-table` extended field attributes addendum).
                    if (autoIncrementApplicable) {
                        FieldSwitchRow(
                            label = stringResource(R.string.field_def_autoincrement_label),
                            checked = autoIncrement,
                            onCheckedChange = { checked ->
                                autoIncrement = checked
                                val resolvedPrimaryKey = ColumnDefinitionValidation.resolvePrimaryKeyForAutoIncrement(
                                    isPrimaryKey,
                                    checked,
                                )
                                isPrimaryKey = resolvedPrimaryKey
                                nullable = ColumnDefinitionValidation.resolveNullable(
                                    nullable,
                                    resolvedPrimaryKey,
                                    isVirtual,
                                )
                            },
                            showDivider = false,
                        )
                    }
                }

                // Card "Comentario" — opcional, siempre visible
                IOSGroupedCard {
                    IOSTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        placeholder = stringResource(R.string.field_def_comment_hint),
                        showDivider = false,
                    )
                }
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

                        val parsedValues = valuesText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        val valuesRequired = selectedType != null &&
                            ColumnDefinitionValidation.isValuesApplicable(selectedType)
                        valuesError = if (
                            valuesRequired &&
                            !ColumnDefinitionValidation.isValuesValid(parsedValues, selectedType!!)
                        ) {
                            hasError = true
                            errorValuesRequired
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
                                    values = if (ColumnDefinitionValidation.isValuesApplicable(selectedType)) {
                                        parsedValues
                                    } else {
                                        emptyList()
                                    },
                                    defaultValue = if (
                                        ColumnDefinitionValidation.isDefaultApplicable(isVirtual, autoIncrement)
                                    ) {
                                        defaultValue.trim().ifBlank { null }
                                    } else {
                                        null
                                    },
                                    autoIncrement = if (
                                        ColumnDefinitionValidation.isAutoIncrementApplicable(selectedType, isVirtual)
                                    ) {
                                        autoIncrement
                                    } else {
                                        false
                                    },
                                    zeroFill = if (ColumnDefinitionValidation.isZeroFillApplicable(selectedType)) {
                                        zeroFill
                                    } else {
                                        false
                                    },
                                    characterSet = if (ColumnDefinitionValidation.isCharsetApplicable(selectedType)) {
                                        selectedCharset?.name
                                    } else {
                                        null
                                    },
                                    collation = if (ColumnDefinitionValidation.isCharsetApplicable(selectedType)) {
                                        selectedCollation?.name
                                    } else {
                                        null
                                    },
                                    autoUpdateTimestamp = if (
                                        ColumnDefinitionValidation.isAutoUpdateTimestampApplicable(selectedType)
                                    ) {
                                        autoUpdateTimestamp
                                    } else {
                                        false
                                    },
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
    showDivider: Boolean = true,
) {
    val tokens = LocalDesignTokens.current

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(tokens.surfacePrimary)
                .padding(horizontal = 16.dp, vertical = 12.dp),
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

        if (showDivider) {
            androidx.compose.material3.HorizontalDivider(
                color = tokens.separator,
                thickness = 0.5.dp,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
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
