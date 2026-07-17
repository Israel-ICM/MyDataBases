package com.sphynxs.mydatabases.core.database.models

/**
 * Representa la definición de una columna al construir una nueva tabla (change `create-table`).
 *
 * A diferencia de [Column] (que refleja metadata de una columna YA existente leída del motor),
 * `ColumnDefinition` es la entrada del usuario usada por `CreateTableUseCase` para construir el
 * DDL `CREATE TABLE`.
 *
 * @property name Nombre de la columna, validado contra `^[A-Za-z0-9_]{1,64}$`
 * @property type Tipo de columna MySQL/MariaDB (ver [SqlColumnType])
 * @property length Longitud, solo aplicable cuando `type.supportsLength` es true
 * @property decimals Decimales, solo aplicable cuando `type.supportsDecimals` es true
 * @property nullable Si la columna permite NULL (ignorado para columnas generadas)
 * @property isVirtual Si es una columna generada (`GENERATED ALWAYS AS (...)`)
 * @property expression Expresión SQL para columnas generadas; requerida y no-blank solo
 *   cuando [isVirtual] es true. El cliente NUNCA parsea ni valida semánticamente esta expresión.
 * @property isPrimaryKey Si la columna forma parte de la PRIMARY KEY de la tabla
 * @property comment Comentario opcional de columna (`COMMENT '...'`)
 * @property values Lista de valores literales permitidos, solo aplicable cuando
 *   `type.supportsValues` es true (ENUM/SET); ignorado para el resto de los tipos
 * @property defaultValue Valor por defecto OPAQUO (`DEFAULT <valor>`), aplicable cuando
 *   `!isVirtual && !autoIncrement`; el cliente NUNCA cita/parsea este valor (change
 *   `create-table`, extended field attributes addendum)
 * @property autoIncrement Si la columna es `AUTO_INCREMENT`, solo aplicable para tipos
 *   enteros base (`type.supportsAutoIncrement`) con `isVirtual = false`
 * @property zeroFill Si la columna emite `UNSIGNED ZEROFILL`, solo aplicable para tipos
 *   numéricos (`type.supportsZeroFill`)
 * @property characterSet Character set (`CHARACTER SET <valor>`), solo aplicable para
 *   tipos de cadena (`type.supportsCharset`); cargado en vivo desde el servidor
 * @property collation Collation (`COLLATE <valor>`), solo aplicable junto a [characterSet]
 *   para tipos de cadena (`type.supportsCharset`); cargado en vivo desde el servidor
 * @property autoUpdateTimestamp Si la columna emite `ON UPDATE CURRENT_TIMESTAMP`, solo
 *   aplicable para `TIMESTAMP`/`DATETIME` (`type.supportsAutoUpdateTimestamp`)
 * @author sdd-apply (Strict TDD)
 * @date 2026-07-15
 */
data class ColumnDefinition(
    val name: String,
    val type: SqlColumnType,
    val length: Int? = null,
    val decimals: Int? = null,
    val nullable: Boolean = true,
    val isVirtual: Boolean = false,
    val expression: String? = null,
    val isPrimaryKey: Boolean = false,
    val comment: String? = null,
    val values: List<String> = emptyList(),
    val defaultValue: String? = null,
    val autoIncrement: Boolean = false,
    val zeroFill: Boolean = false,
    val characterSet: String? = null,
    val collation: String? = null,
    val autoUpdateTimestamp: Boolean = false,
) {
    /**
     * Modo de almacenamiento resuelto para columnas generadas; `null` para columnas normales.
     *
     * MySQL/MariaDB exige `STORED` cuando una columna generada participa de la PRIMARY KEY;
     * en cualquier otro caso una columna generada usa `VIRTUAL` por defecto. Este valor se
     * deriva automáticamente y NUNCA requiere un control de usuario separado.
     */
    val generatedStorageMode: GeneratedStorageMode?
        get() = when {
            !isVirtual -> null
            isPrimaryKey -> GeneratedStorageMode.STORED
            else -> GeneratedStorageMode.VIRTUAL
        }
}

/**
 * Modo de almacenamiento de una columna generada (`GENERATED ALWAYS AS (...) [VIRTUAL|STORED]`).
 *
 * @property sqlKeyword Palabra clave SQL a emitir en el DDL
 * @author sdd-apply (Strict TDD)
 * @date 2026-07-15
 */
enum class GeneratedStorageMode(val sqlKeyword: String) {
    VIRTUAL("VIRTUAL"),
    STORED("STORED"),
}

/**
 * Tipos de columna MySQL/MariaDB soportados por el formulario de creación de tabla.
 *
 * `supportsLength` y `supportsDecimals` gobiernan la aplicabilidad de los controles
 * Longitud/Decimales del formulario (ver `ColumnDefinitionValidation`):
 * - Longitud: solo VARCHAR, CHAR, DECIMAL, NUMERIC
 * - Decimales: solo DECIMAL, NUMERIC, FLOAT, DOUBLE
 *
 * @property sqlName Nombre del tipo tal como se emite en el DDL
 * @property supportsLength Si el tipo acepta una longitud (`(N)`)
 * @property supportsDecimals Si el tipo acepta decimales (`(N,D)`)
 * @property supportsValues Si el tipo acepta una lista de valores literales entre paréntesis
 *   (`(v1,v2,...)`), ej. ENUM/SET (change `create-table`, ENUM/SET support)
 * @property supportsAutoIncrement Si el tipo acepta `AUTO_INCREMENT`; solo los 5 tipos
 *   enteros base (change `create-table`, extended field attributes addendum)
 * @property supportsZeroFill Si el tipo acepta `UNSIGNED ZEROFILL`; los 5 tipos enteros base
 *   más DECIMAL/NUMERIC/FLOAT/DOUBLE (change `create-table`, extended field attributes addendum)
 * @property supportsCharset Si el tipo acepta `CHARACTER SET`/`COLLATE`; tipos de cadena
 *   (CHAR, VARCHAR, TEXT/TINYTEXT/MEDIUMTEXT/LONGTEXT, ENUM, SET) (change `create-table`,
 *   extended field attributes addendum)
 * @property supportsAutoUpdateTimestamp Si el tipo acepta `ON UPDATE CURRENT_TIMESTAMP`;
 *   solo TIMESTAMP/DATETIME, per MySQL/MariaDB docs (change `create-table`, extended field
 *   attributes addendum)
 * @author sdd-apply (Strict TDD)
 * @date 2026-07-15
 */
sealed class SqlColumnType(
    val sqlName: String,
    // NOTA: calificado como `kotlin.Boolean` a propósito. Sin esto, el compilador
    // resuelve `Boolean` al `data object Boolean` anidado más abajo (el tipo SQL
    // BOOLEAN) en vez del tipo primitivo, por scoping de miembros de la clase.
    val supportsLength: kotlin.Boolean,
    val supportsDecimals: kotlin.Boolean,
    val supportsValues: kotlin.Boolean = false,
    val supportsAutoIncrement: kotlin.Boolean = false,
    val supportsZeroFill: kotlin.Boolean = false,
    val supportsCharset: kotlin.Boolean = false,
    val supportsAutoUpdateTimestamp: kotlin.Boolean = false,
) {
    data object Int : SqlColumnType(
        "INT",
        supportsLength = false,
        supportsDecimals = false,
        supportsAutoIncrement = true,
        supportsZeroFill = true,
    )
    data object TinyInt : SqlColumnType(
        "TINYINT",
        supportsLength = false,
        supportsDecimals = false,
        supportsAutoIncrement = true,
        supportsZeroFill = true,
    )
    data object SmallInt : SqlColumnType(
        "SMALLINT",
        supportsLength = false,
        supportsDecimals = false,
        supportsAutoIncrement = true,
        supportsZeroFill = true,
    )
    data object MediumInt : SqlColumnType(
        "MEDIUMINT",
        supportsLength = false,
        supportsDecimals = false,
        supportsAutoIncrement = true,
        supportsZeroFill = true,
    )
    data object BigInt : SqlColumnType(
        "BIGINT",
        supportsLength = false,
        supportsDecimals = false,
        supportsAutoIncrement = true,
        supportsZeroFill = true,
    )
    data object Bit : SqlColumnType("BIT", supportsLength = true, supportsDecimals = false)
    data object VarChar : SqlColumnType("VARCHAR", supportsLength = true, supportsDecimals = false, supportsCharset = true)
    data object Char : SqlColumnType("CHAR", supportsLength = true, supportsDecimals = false, supportsCharset = true)
    data object Decimal : SqlColumnType("DECIMAL", supportsLength = true, supportsDecimals = true, supportsZeroFill = true)
    data object Numeric : SqlColumnType("NUMERIC", supportsLength = true, supportsDecimals = true, supportsZeroFill = true)
    data object Float : SqlColumnType("FLOAT", supportsLength = false, supportsDecimals = true, supportsZeroFill = true)
    data object Double : SqlColumnType("DOUBLE", supportsLength = false, supportsDecimals = true, supportsZeroFill = true)
    data object TinyText : SqlColumnType("TINYTEXT", supportsLength = false, supportsDecimals = false, supportsCharset = true)
    data object Text : SqlColumnType("TEXT", supportsLength = false, supportsDecimals = false, supportsCharset = true)
    data object MediumText : SqlColumnType(
        "MEDIUMTEXT",
        supportsLength = false,
        supportsDecimals = false,
        supportsCharset = true,
    )
    data object LongText : SqlColumnType("LONGTEXT", supportsLength = false, supportsDecimals = false, supportsCharset = true)
    data object Binary : SqlColumnType("BINARY", supportsLength = true, supportsDecimals = false)
    data object VarBinary : SqlColumnType("VARBINARY", supportsLength = true, supportsDecimals = false)
    data object TinyBlob : SqlColumnType("TINYBLOB", supportsLength = false, supportsDecimals = false)
    data object Blob : SqlColumnType("BLOB", supportsLength = false, supportsDecimals = false)
    data object MediumBlob : SqlColumnType("MEDIUMBLOB", supportsLength = false, supportsDecimals = false)
    data object LongBlob : SqlColumnType("LONGBLOB", supportsLength = false, supportsDecimals = false)
    data object Json : SqlColumnType("JSON", supportsLength = false, supportsDecimals = false)
    data object Enum : SqlColumnType(
        "ENUM",
        supportsLength = false,
        supportsDecimals = false,
        supportsValues = true,
        supportsCharset = true,
    )
    data object Set : SqlColumnType(
        "SET",
        supportsLength = false,
        supportsDecimals = false,
        supportsValues = true,
        supportsCharset = true,
    )
    data object Boolean : SqlColumnType("BOOLEAN", supportsLength = false, supportsDecimals = false)
    data object Date : SqlColumnType("DATE", supportsLength = false, supportsDecimals = false)
    data object DateTime : SqlColumnType(
        "DATETIME",
        supportsLength = false,
        supportsDecimals = false,
        supportsAutoUpdateTimestamp = true,
    )
    data object Timestamp : SqlColumnType(
        "TIMESTAMP",
        supportsLength = false,
        supportsDecimals = false,
        supportsAutoUpdateTimestamp = true,
    )
    data object Time : SqlColumnType("TIME", supportsLength = false, supportsDecimals = false)
    data object Year : SqlColumnType("YEAR", supportsLength = false, supportsDecimals = false)
}

/**
 * Funciones puras de validación cruzada de campos para [ColumnDefinition] (change `create-table`).
 *
 * Cubre las reglas definidas en `specs/create-table/spec.md`:
 * - Aplicabilidad de Longitud/Decimales según [SqlColumnType]
 * - Aplicabilidad y validez de Valores según [SqlColumnType] (ENUM/SET, change `create-table`
 *   ENUM/SET support)
 * - Llave (Key) fuerza Nulo=false para columnas no-generadas
 * - Control Nulo oculto/deshabilitado cuando Virtual=true
 * - Expresión requerida y no-blank solo cuando Virtual=true (sin parseo/validación semántica)
 * - Nombre: identificador SQL válido (`^[A-Za-z0-9_]{1,64}$`)
 * - Aplicabilidad de Valor predeterminado/Autoincrement/ZeroFill/Charset-Collation/
 *   Actualización automática de fecha/hora según [SqlColumnType] y estado cruzado (change
 *   `create-table`, extended field attributes addendum)
 * - Autoincrement fuerza Llave=true (que a su vez ya fuerza Nulo=false vía la regla existente)
 *
 * @author sdd-apply (Strict TDD)
 * @date 2026-07-15
 */
object ColumnDefinitionValidation {

    private val IDENTIFIER_REGEX = Regex("^[A-Za-z0-9_]{1,64}$")

    /** Longitud MUST be enabled only for length-bearing types (VARCHAR, CHAR, DECIMAL, NUMERIC). */
    fun isLengthApplicable(type: SqlColumnType): Boolean = type.supportsLength

    /** Decimales MUST be enabled only for numeric/decimal types (DECIMAL, NUMERIC, FLOAT, DOUBLE). */
    fun isDecimalsApplicable(type: SqlColumnType): Boolean = type.supportsDecimals

    /** Valores MUST be enabled only for value-list types (ENUM, SET). */
    fun isValuesApplicable(type: SqlColumnType): Boolean = type.supportsValues

    /**
     * Valida la lista de Valores permitidos para tipos ENUM/SET.
     *
     * Cuando el tipo NO soporta Valores, la validación siempre pasa (`true`) y el campo
     * se ignora. Cuando aplica (ENUM/SET): se requiere al menos un valor, sin duplicados
     * (comparación case-sensitive, igual que la semántica de MySQL ENUM/SET), y cada valor
     * individual debe quedar no-blank tras aplicar `trim()`.
     */
    fun isValuesValid(values: List<String>, type: SqlColumnType): Boolean {
        if (!type.supportsValues) return true
        if (values.isEmpty()) return false
        val trimmedValues = values.map { it.trim() }
        if (trimmedValues.any { it.isBlank() }) return false
        return trimmedValues.size == trimmedValues.toSet().size
    }

    /** Nombre MUST be required, non-blank, and match `^[A-Za-z0-9_]{1,64}$`. */
    fun isValidName(name: String): Boolean = IDENTIFIER_REGEX.matches(name)

    /**
     * Resuelve el valor final de Nulo aplicando la regla Llave→Nulo=false.
     *
     * Cuando la columna NO es virtual y es Llave (PRIMARY KEY), Nulo se fuerza a false.
     * Para columnas virtuales la regla no aplica: la nulabilidad se deriva de la expresión
     * y el control Nulo ya está oculto/deshabilitado en la UI.
     */
    fun resolveNullable(requestedNullable: Boolean, isPrimaryKey: Boolean, isVirtual: Boolean): Boolean =
        if (!isVirtual && isPrimaryKey) false else requestedNullable

    /**
     * Determina si el control Nulo debe estar habilitado.
     *
     * Oculto/deshabilitado cuando Virtual=true (nulabilidad derivada de la expresión).
     * Deshabilitado cuando Virtual=false y Llave=true (regla Llave→Nulo=false).
     */
    fun isNuloEditable(isVirtual: Boolean, isPrimaryKey: Boolean = false): Boolean =
        !isVirtual && !isPrimaryKey

    /** Expresión MUST be shown and required only when Virtual=true. */
    fun isExpressionRequired(isVirtual: Boolean): Boolean = isVirtual

    /**
     * Valida que Expresión sea no-blank cuando es requerida (Virtual=true).
     *
     * Cuando Virtual=false, Expresión está oculta/ignorada y siempre es válida.
     * El contenido de la expresión NUNCA se parsea ni se valida semánticamente
     * (los errores de SQL inválido surgen solo al ejecutar el DDL).
     */
    fun isExpressionValid(expression: String?, isVirtual: Boolean): Boolean =
        if (!isVirtual) true else !expression.isNullOrBlank()

    // --- Extended field attributes (change `create-table`, extended field attributes addendum) ---

    /** Valor predeterminado MUST be applicable only when the column is not generated and not auto-increment. */
    fun isDefaultApplicable(isVirtual: Boolean, autoIncrement: Boolean): Boolean = !isVirtual && !autoIncrement

    /** Autoincrement MUST be enabled only for base integer types, and only when the column is not generated. */
    fun isAutoIncrementApplicable(type: SqlColumnType, isVirtual: Boolean): Boolean =
        type.supportsAutoIncrement && !isVirtual

    /** Rellenar con ceros (ZeroFill) MUST be enabled only for numeric types (`type.supportsZeroFill`). */
    fun isZeroFillApplicable(type: SqlColumnType): Boolean = type.supportsZeroFill

    /** Conjunto de caracteres (Character Set) / Collation MUST be enabled only for string types. */
    fun isCharsetApplicable(type: SqlColumnType): Boolean = type.supportsCharset

    /** Actualización automática de fecha/hora MUST be enabled only for TIMESTAMP/DATETIME. */
    fun isAutoUpdateTimestampApplicable(type: SqlColumnType): Boolean = type.supportsAutoUpdateTimestamp

    /**
     * Resuelve Llave cuando se activa Autoincrement: MySQL/MariaDB requiere que una columna
     * AUTO_INCREMENT forme parte de una llave (PRIMARY KEY en este formulario). Al activar
     * Autoincrement, Llave se fuerza a true; al desactivarlo, Llave conserva su valor actual
     * (mismo patrón que [resolveNullable] con Llave→Nulo).
     */
    fun resolvePrimaryKeyForAutoIncrement(currentIsPrimaryKey: Boolean, autoIncrement: Boolean): Boolean =
        if (autoIncrement) true else currentIsPrimaryKey
}
