# Feature: Editor SQL Profesional

## Visión

El editor SQL debe ser una herramienta profesional de nivel desktop, optimizada para dispositivos móviles y tablets.

**Inspiración**: DataGrip, DBeaver, Navicat — pero nativo en Android con Jetpack Compose.

---

## Requisitos Funcionales

### 1. Editor de Código Profesional

**Características obligatorias**:

- ✅ **Syntax Highlighting** (resaltado de sintaxis SQL)
- ✅ **Autocompletado inteligente** (tablas, columnas, keywords SQL)
- ✅ **Indentación automática**
- ✅ **Formateo de queries** (prettify SQL)
- ✅ **Detección de errores** (syntax errors en tiempo real)
- ✅ **Line numbers** (números de línea)
- ✅ **Multi-cursor** (edición simultánea en múltiples líneas)
- ✅ **Bracket matching** (resaltar paréntesis/llaves coincidentes)
- ✅ **Code folding** (colapsar/expandir bloques)

### 2. Autocompletado Inteligente (CRÍTICO)

**Contexto-aware autocomplete**:

```sql
SELECT 
  us|  ← Autocomplete sugiere: users (tabla), user_id (columna)
FROM users
WHERE user_id = |  ← Autocomplete sugiere: valores, funciones, comparadores
```

**Tipos de sugerencias**:

1. **Keywords SQL**: `SELECT`, `FROM`, `WHERE`, `JOIN`, `GROUP BY`, `ORDER BY`, etc.
2. **Nombres de tablas**: Del esquema actual de la conexión activa
3. **Nombres de columnas**: De la tabla en contexto
4. **Funciones SQL**: `COUNT()`, `SUM()`, `AVG()`, `CONCAT()`, etc.
5. **Tipos de datos**: `VARCHAR`, `INT`, `DATETIME`, etc.
6. **Snippets**: Templates comunes (`SELECT * FROM`, `INSERT INTO`, etc.)

**Prioridad de sugerencias**:

- Contexto actual > Keywords > Tablas > Funciones
- Usar frecuencia de uso (sugerir lo más usado primero)
- Filtrar por motor de DB activo (MySQL vs PostgreSQL vs SQLite)

**Trigger de autocomplete**:

- Al escribir (después de 2 caracteres)
- Con `Ctrl+Space` (teclados externos)
- Con gesture de swipe down (móvil)

### 3. Multi-Tab (Pestañas)

**Comportamiento**:

- Múltiples tabs de queries simultáneos
- Cada tab es independiente (conexión, historial, estado)
- Tabs persistentes (se restauran al reabrir la app)
- Límite recomendado: 10 tabs abiertos simultáneamente

**Acciones por tab**:

- Renombrar tab
- Cerrar tab
- Cerrar otros tabs
- Cerrar todos los tabs
- Duplicar tab
- Mover tab (reordenar)

### 4. Ejecución de Queries

**Métodos de ejecución**:

- **Ejecutar todo** (botón principal)
- **Ejecutar selección** (ejecutar solo texto seleccionado)
- **Ejecutar hasta cursor** (ejecutar desde inicio hasta posición del cursor)
- **Ejecutar statement actual** (detectar statement donde está el cursor)

**Shortcuts**:

- `Ctrl+Enter` o `Cmd+Enter`: Ejecutar query
- `F5`: Ejecutar todo
- `Ctrl+Shift+Enter`: Ejecutar selección

**Resultados**:

- Mostrar en panel inferior (tablet) o pantalla separada (teléfono)
- Paginación de resultados (100 rows por página)
- Tiempo de ejecución
- Rows affected
- Errores con línea exacta del error

### 5. Formateo de Queries

**Auto-format** (prettify):

```sql
-- Antes
select u.id,u.name,u.email from users u where u.active=1 and u.role='admin'

-- Después
SELECT 
  u.id,
  u.name,
  u.email
FROM users u
WHERE 
  u.active = 1
  AND u.role = 'admin'
```

**Opciones de formato**:

- Uppercase keywords (`SELECT` vs `select`)
- Indentación (2 espacios, 4 espacios, tabs)
- Comma-first vs comma-last
- Líneas en blanco entre secciones

### 6. Historial de Queries

**Persistencia**:

- Guardar TODAS las queries ejecutadas
- Asociar a conexión específica
- Timestamp de ejecución
- Estado (exitosa/fallida)

**Búsqueda**:

- Filtrar por texto
- Filtrar por fecha
- Filtrar por conexión
- Filtrar por estado

**Acciones**:

- Re-ejecutar query del historial
- Copiar al editor
- Guardar como snippet
- Eliminar del historial

### 7. Snippets Guardados

**Crear snippet**:

- Desde query actual
- Desde selección
- Desde historial

**Organización**:

- Carpetas/categorías
- Tags
- Búsqueda por nombre/contenido

**Snippets pre-instalados**:

```sql
-- MySQL
SELECT * FROM ${table} WHERE ${condition}
INSERT INTO ${table} (${columns}) VALUES (${values})
UPDATE ${table} SET ${column} = ${value} WHERE ${condition}

-- PostgreSQL
CREATE TABLE ${table} (
  id SERIAL PRIMARY KEY,
  ${column} ${type}
)

-- SQLite
PRAGMA table_info(${table})
```

### 8. Diagnóstico de Errores

**Detección en tiempo real**:

- Syntax errors (subrayado rojo)
- Warnings (subrayado amarillo)
- Sugerencias (subrayado azul)

**Tipos de errores**:

- Sintaxis SQL incorrecta
- Tablas que no existen
- Columnas que no existen
- Tipos incompatibles
- Permisos insuficientes

**Mostrar**:

- Línea y columna del error
- Mensaje descriptivo
- Sugerencia de corrección (cuando sea posible)

### 9. Execution Plan (v1.1+)

**Mostrar plan de ejecución**:

- `EXPLAIN` query (MySQL/MariaDB)
- `EXPLAIN ANALYZE` (PostgreSQL)
- `EXPLAIN QUERY PLAN` (SQLite)

**Visualización**:

- Árbol de operaciones
- Costo estimado
- Índices usados
- Sugerencias de optimización

---

## Requisitos de UI/UX

### Layout Adaptativo

**Teléfono (Compact)**:

```
┌─────────────────────┐
│ [Tab1] [Tab2] [+]   │ ← Tabs horizontales
├─────────────────────┤
│                     │
│   Editor SQL        │
│   (código)          │
│                     │
├─────────────────────┤
│ [▶ Run] [Format]    │ ← Toolbar
└─────────────────────┘

↓ Al ejecutar, navega a pantalla de resultados
```

**Tablet (Medium/Expanded)**:

```
┌────────────────────────────────────┐
│ [Tab1] [Tab2] [Tab3] [+]           │
├────────────────────────────────────┤
│                                    │
│   Editor SQL (60% altura)          │
│   (código)                         │
│                                    │
├────────────────────────────────────┤
│ [▶ Run] [Format] [History] [...]   │
├────────────────────────────────────┤
│                                    │
│   Resultados (40% altura)          │
│   (tabla con resultados)           │
│                                    │
└────────────────────────────────────┘
```

**Tablet Landscape (Expanded)**:

```
┌──────────────────┬─────────────────┐
│                  │                 │
│   Editor SQL     │   Resultados    │
│   (código)       │   (tabla)       │
│   50%            │   50%           │
│                  │                 │
└──────────────────┴─────────────────┘
```

### Toolbar

**Acciones principales**:

- ▶️ **Run Query** (ejecutar)
- ⏸️ **Stop Query** (cancelar ejecución)
- 🎨 **Format** (formatear código)
- 💾 **Save as Snippet** (guardar como snippet)
- 📋 **Copy** (copiar query)
- 🗑️ **Clear** (limpiar editor)
- ⏱️ **History** (abrir historial)
- ⚙️ **Settings** (configuración del editor)

### Teclado Virtual Extendido

**Para dispositivos móviles sin teclado físico**:

```
┌─────────────────────────────────────┐
│ Teclado Android Normal              │
├─────────────────────────────────────┤
│ [SELECT][FROM][WHERE][JOIN][AND][OR]│ ← Barra extra con keywords
└─────────────────────────────────────┘
```

**Keywords rápidos**:

- `SELECT`, `FROM`, `WHERE`, `JOIN`, `AND`, `OR`
- `(`, `)`, `;`, `=`, `<`, `>`
- `LIMIT`, `ORDER BY`, `GROUP BY`

### Temas

**Syntax highlighting themes**:

- 🌙 **Dracula** (dark, por defecto)
- 🌃 **Monokai** (dark)
- ☀️ **GitHub Light** (light)
- 🎨 **Material** (dynamic color)

**Configuración**:

- Tamaño de fuente (12sp - 18sp)
- Font family (JetBrains Mono, Fira Code, monospace)
- Line height
- Show/hide line numbers
- Show/hide minimap (tablet only)

---

## Requisitos Técnicos

### Librerías Recomendadas

**Opción 1: CodeMirror + WebView** (más rápido de implementar)

- CodeMirror en WebView
- Bridge Kotlin ↔ JavaScript
- Pros: Todas las features out-of-the-box
- Cons: WebView overhead, no 100% Compose

**Opción 2: Custom Compose Editor** (más nativo)

- `TextField` customizado con `VisualTransformation`
- Syntax highlighting manual
- Autocomplete con Compose Popup
- Pros: 100% Compose, mejor performance
- Cons: Más trabajo de implementación

**Opción 3: Usar librería existente**

- Buscar librerías Compose para code editing
- Ejemplos: `compose-code-editor`, `jetpack-compose-code-editor`

### Autocompletado - Implementación

**1. Parser SQL**:

```kotlin
interface SQLParser {
    fun parse(query: String): ParsedQuery
    fun getContextAtCursor(query: String, cursorPosition: Int): SQLContext
}

data class SQLContext(
    val type: ContextType,  // TABLE, COLUMN, KEYWORD, FUNCTION
    val prefix: String,     // Texto antes del cursor
    val availableTables: List<String>,
    val availableColumns: List<String>
)
```

**2. Autocomplete Provider**:

```kotlin
interface AutocompleteProvider {
    suspend fun getSuggestions(
        context: SQLContext,
        engine: DatabaseEngine
    ): List<Suggestion>
}

data class Suggestion(
    val text: String,
    val type: SuggestionType,
    val description: String?,
    val icon: ImageVector?
)

enum class SuggestionType {
    KEYWORD, TABLE, COLUMN, FUNCTION, SNIPPET
}
```

**3. Integración con DatabaseEngine**:

```kotlin
// Obtener metadatos para autocomplete
suspend fun DatabaseEngine.getTableNames(database: String): List<String>
suspend fun DatabaseEngine.getColumns(table: String): List<Column>
suspend fun DatabaseEngine.getFunctions(): List<String>
```

### Performance

**Optimizaciones**:

- Syntax highlighting incremental (solo líneas visibles)
- Autocomplete con debounce (300ms)
- Cache de metadatos de DB (tablas, columnas)
- Virtualización de resultados (solo renderizar rows visibles)
- Background parsing (worker thread)

### Testing

**Unit Tests**:

- SQLParser correctamente identifica contexto
- AutocompleteProvider retorna sugerencias correctas
- Formateo de queries produce output esperado

**Integration Tests**:

- Autocompletado funciona con MySQL, PostgreSQL, SQLite
- Ejecución de queries retorna resultados correctos
- Historial persiste queries correctamente

**UI Tests**:

- Usuario puede escribir query y ejecutar
- Autocomplete aparece al escribir
- Tabs se pueden crear/cerrar/renombrar
- Resultados se muestran correctamente

---

## Roadmap del Editor

### v1.0 — Editor Básico

- ✅ Syntax highlighting básico
- ✅ Ejecución de queries
- ✅ Resultados paginados
- ✅ Single tab

### v1.1 — Editor Profesional

- ✅ **Autocompletado inteligente** (CRÍTICO)
- ✅ Multi-tab
- ✅ Formateo de queries
- ✅ Historial persistente
- ✅ Snippets guardados
- ✅ Diagnóstico de errores
- ✅ Execution plans

### v1.2 — Features Avanzadas

- ✅ Code folding
- ✅ Multi-cursor
- ✅ Find & Replace
- ✅ Minimap (tablet)
- ✅ Bracket matching animado
- ✅ SQL refactoring (rename table, extract query)

### v2.0 — Visual Query Builder (Futuro)

- Diseñador visual de queries
- Drag & drop tablas
- Generación automática de SQL
- Conversión visual ↔ código

---

## Ejemplo de Uso

### Caso 1: Query Simple con Autocomplete

```
Usuario: Escribe "SEL"
Editor: Muestra autocomplete
  ┌────────────────────┐
  │ SELECT            │ ← Sugerido
  │ SELF              │
  └────────────────────┘

Usuario: Selecciona "SELECT"
Editor: Inserta "SELECT " y posiciona cursor

Usuario: Escribe "* FR"
Editor: Muestra autocomplete
  ┌────────────────────┐
  │ FROM              │ ← Sugerido
  └────────────────────┘

Usuario: Selecciona "FROM"
Editor: Inserta "FROM " 

Usuario: Espacio
Editor: Muestra tablas disponibles
  ┌────────────────────┐
  │ users             │
  │ products          │
  │ orders            │
  └────────────────────┘

Usuario: Selecciona "users"
Editor: Query completa: "SELECT * FROM users"

Usuario: Presiona ▶️ Run
Editor: Ejecuta y muestra resultados en panel inferior
```

### Caso 2: Snippet Reutilizable

```
Usuario: Abre menú Snippets
Editor: Muestra snippets guardados
  ┌────────────────────────────────┐
  │ 📁 MySQL                       │
  │   ├─ Users activos             │
  │   ├─ Productos por categoría   │
  │ 📁 PostgreSQL                  │
  │   ├─ Ventas del mes            │
  └────────────────────────────────┘

Usuario: Selecciona "Users activos"
Editor: Inserta snippet
  SELECT 
    id,
    name,
    email
  FROM users
  WHERE active = 1
  ORDER BY created_at DESC

Usuario: Modifica y ejecuta
```

---

## Strings Localizados

**Inglés** (`values/strings.xml`):

```xml
<!-- SQL Editor -->
<string name="editor_title">SQL Editor</string>
<string name="editor_run">Run Query</string>
<string name="editor_format">Format</string>
<string name="editor_history">History</string>
<string name="editor_snippets">Snippets</string>
<string name="editor_autocomplete_title">Suggestions</string>
<string name="editor_execution_time">Execution time: %1$s ms</string>
<string name="editor_rows_affected">%1$d rows affected</string>
```

**Español** (`values-es/strings.xml`):

```xml
<!-- SQL Editor -->
<string name="editor_title">Editor SQL</string>
<string name="editor_run">Ejecutar Query</string>
<string name="editor_format">Formatear</string>
<string name="editor_history">Historial</string>
<string name="editor_snippets">Snippets</string>
<string name="editor_autocomplete_title">Sugerencias</string>
<string name="editor_execution_time">Tiempo de ejecución: %1$s ms</string>
<string name="editor_rows_affected">%1$d filas afectadas</string>
```

---

**Recordá**: El editor SQL es EL diferenciador de MyDataBases. Debe sentirse profesional, rápido y poderoso.
