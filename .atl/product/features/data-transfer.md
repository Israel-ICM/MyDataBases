# Feature: Data Transfer y Export

## Visión

Permitir exportar e importar datos en múltiples formatos para análisis, migración y integración con otras herramientas.

---

## Formatos de Export

### 1. CSV (Comma-Separated Values)

**Uso**: Excel, Google Sheets, análisis de datos, importar a otras DBs

```csv
id,name,email,created_at
1,"John Doe","john@example.com","2026-01-15 10:30:00"
2,"Jane Smith","jane@example.com","2026-01-16 14:25:00"
```

**Opciones**:

- Delimitador: `,` (coma), `;` (punto y coma), `\t` (tab)
- Text qualifier: `"` (comillas dobles), `'` (comillas simples)
- Include header row
- Encoding: UTF-8, UTF-16, ISO-8859-1
- Line endings: Unix (LF), Windows (CRLF)

### 2. JSON

**Uso**: APIs, aplicaciones web, NoSQL, análisis

```json
[
  {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "created_at": "2026-01-15T10:30:00Z"
  },
  {
    "id": 2,
    "name": "Jane Smith",
    "email": "jane@example.com",
    "created_at": "2026-01-16T14:25:00Z"
  }
]
```

**Opciones**:

- Format: Pretty (indented) vs Minified (compact)
- Include null values
- Date format: ISO-8601, Unix timestamp, custom

### 3. Excel (.xlsx)

**Uso**: Análisis de datos, reportes

**Características**:

- Múltiples sheets (una por tabla)
- Formato de columnas automático
- Headers en bold
- Auto-width de columnas
- Filtros automáticos

### 4. XML

**Uso**: Integraciones legacy, SOAP, config files

```xml
<?xml version="1.0" encoding="UTF-8"?>
<users>
  <user>
    <id>1</id>
    <name>John Doe</name>
    <email>john@example.com</email>
    <created_at>2026-01-15T10:30:00Z</created_at>
  </user>
</users>
```

### 5. SQL Insert Statements

**Uso**: Migración de datos, backups parciales

```sql
INSERT INTO users (id, name, email) VALUES
(1, 'John Doe', 'john@example.com'),
(2, 'Jane Smith', 'jane@example.com');
```

### 6. Markdown Table

**Uso**: Documentación, GitHub, Notion

```markdown
| id | name       | email              |
|----|------------|--------------------|
| 1  | John Doe   | john@example.com   |
| 2  | Jane Smith | jane@example.com   |
```

### 7. HTML Table

**Uso**: Reportes web, emails

```html
<table>
  <thead>
    <tr><th>id</th><th>name</th><th>email</th></tr>
  </thead>
  <tbody>
    <tr><td>1</td><td>John Doe</td><td>john@example.com</td></tr>
  </tbody>
</table>
```

---

## UI/UX de Export

### Opciones de Export

```
┌─────────────────────────────────────┐
│ Export Query Results                │
├─────────────────────────────────────┤
│ Rows: 1,523 (showing 100)           │
│                                     │
│ Format:                             │
│  ● CSV                              │
│  ○ JSON                             │
│  ○ Excel                            │
│  ○ SQL                              │
│  ○ XML                              │
│  ○ Markdown                         │
│                                     │
│ Export:                             │
│  ● All rows (1,523)                 │
│  ○ Current page (100)               │
│  ○ Selected rows (15)               │
│                                     │
│ CSV Options:                        │
│  Delimiter: [, (comma)▼]            │
│  ☑ Include headers                  │
│  Encoding: [UTF-8▼]                 │
│                                     │
│ Filename: users_export.csv          │
│                                     │
│ [Export] [Share]                    │
└─────────────────────────────────────┘
```

### Progreso de Export

```
┌─────────────────────────────────────┐
│ Exporting...                        │
├─────────────────────────────────────┤
│ Format: CSV                         │
│ Rows: 845 / 1,523                   │
│                                     │
│ ██████████░░░░░░░░░░ 55%            │
│                                     │
│ [Cancel]                            │
└─────────────────────────────────────┘
```

---

## Import de Datos

### Formatos Soportados

- CSV
- JSON (array of objects)
- Excel (.xlsx)
- SQL (INSERT statements)

### Asistente de Import

```
┌─────────────────────────────────────┐
│ Import Data - Step 1 of 3           │
├─────────────────────────────────────┤
│ Select File:                        │
│  📄 users_import.csv (245 KB)       │
│                                     │
│ Detected format: CSV                │
│ Delimiter: , (comma)                │
│ Encoding: UTF-8                     │
│ Rows detected: 1,234                │
│                                     │
│ [Next]                              │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│ Import Data - Step 2 of 3           │
├─────────────────────────────────────┤
│ Target Table: [users▼]              │
│  ○ Existing table                   │
│  ● Create new table                 │
│                                     │
│ Column Mapping:                     │
│  File Column → DB Column    Type    │
│  id          → id           INT     │
│  name        → name         VARCHAR │
│  email       → email        VARCHAR │
│  created     → created_at   DATE    │
│                                     │
│ [Back] [Next]                       │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│ Import Data - Step 3 of 3           │
├─────────────────────────────────────┤
│ Import Mode:                        │
│  ● Insert (add new rows)            │
│  ○ Replace (truncate then insert)   │
│  ○ Update (update existing rows)    │
│  ○ Upsert (insert or update)        │
│                                     │
│ Options:                            │
│  ☑ Skip first row (header)          │
│  ☑ Stop on error                    │
│  ☑ Use transaction                  │
│                                     │
│ Summary:                            │
│  • File: users_import.csv           │
│  • Rows: 1,234                      │
│  • Target: users (new table)        │
│  • Mode: Insert                     │
│                                     │
│ [Start Import]                      │
└─────────────────────────────────────┘
```

---

## Strings

**Inglés**:
```xml
<string name="export_title">Export Data</string>
<string name="export_format">Export Format</string>
<string name="export_csv">CSV</string>
<string name="export_json">JSON</string>
<string name="export_excel">Excel</string>
<string name="import_title">Import Data</string>
```

**Español**:
```xml
<string name="export_title">Exportar Datos</string>
<string name="export_format">Formato de Exportación</string>
<string name="export_csv">CSV</string>
<string name="export_json">JSON</string>
<string name="export_excel">Excel</string>
<string name="import_title">Importar Datos</string>
```

---

**Roadmap**: v1.0 (CSV, JSON, SQL), v1.1 (Excel, XML), v1.2 (Import wizard)
