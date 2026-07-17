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
) {
    data object Int : SqlColumnType("INT", supportsLength = false, supportsDecimals = false)
    data object TinyInt : SqlColumnType("TINYINT", supportsLength = false, supportsDecimals = false)
    data object SmallInt : SqlColumnType("SMALLINT", supportsLength = false, supportsDecimals = false)
    data object BigInt : SqlColumnType("BIGINT", supportsLength = false, supportsDecimals = false)
    data object VarChar : SqlColumnType("VARCHAR", supportsLength = true, supportsDecimals = false)
    data object Char : SqlColumnType("CHAR", supportsLength = true, supportsDecimals = false)
    data object Decimal : SqlColumnType("DECIMAL", supportsLength = true, supportsDecimals = true)
    data object Numeric : SqlColumnType("NUMERIC", supportsLength = true, supportsDecimals = true)
    data object Float : SqlColumnType("FLOAT", supportsLength = false, supportsDecimals = true)
    data object Double : SqlColumnType("DOUBLE", supportsLength = false, supportsDecimals = true)
    data object Text : SqlColumnType("TEXT", supportsLength = false, supportsDecimals = false)
    data object LongText : SqlColumnType("LONGTEXT", supportsLength = false, supportsDecimals = false)
    data object Boolean : SqlColumnType("BOOLEAN", supportsLength = false, supportsDecimals = false)
    data object Date : SqlColumnType("DATE", supportsLength = false, supportsDecimals = false)
    data object DateTime : SqlColumnType("DATETIME", supportsLength = false, supportsDecimals = false)
    data object Timestamp : SqlColumnType("TIMESTAMP", supportsLength = false, supportsDecimals = false)
    data object Time : SqlColumnType("TIME", supportsLength = false, supportsDecimals = false)
}

/**
 * Funciones puras de validación cruzada de campos para [ColumnDefinition] (change `create-table`).
 *
 * Cubre las reglas definidas en `specs/create-table/spec.md`:
 * - Aplicabilidad de Longitud/Decimales según [SqlColumnType]
 * - Llave (Key) fuerza Nulo=false para columnas no-generadas
 * - Control Nulo oculto/deshabilitado cuando Virtual=true
 * - Expresión requerida y no-blank solo cuando Virtual=true (sin parseo/validación semántica)
 * - Nombre: identificador SQL válido (`^[A-Za-z0-9_]{1,64}$`)
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
}
