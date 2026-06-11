# Feature: Query Builder Visual

## Visión

Construir queries SQL de forma visual para usuarios no-técnicos.

---

## Funcionalidad

### Modo Visual

**Drag & Drop**:
1. Seleccionar tabla
2. Arrastrar columnas a "SELECT"
3. Añadir condiciones WHERE
4. Añadir ORDER BY
5. Generar SQL automáticamente

### Ejemplo

```
Visual:
┌─────────────────────────────────────┐
│ Tables: [users ▼]                   │
│                                     │
│ SELECT:                             │
│  ☑ id                               │
│  ☑ name                             │
│  ☑ email                            │
│                                     │
│ WHERE:                              │
│  active = 1                         │
│  AND created_at > '2026-01-01'      │
│                                     │
│ ORDER BY:                           │
│  created_at DESC                    │
│                                     │
│ [Generate SQL]                      │
└─────────────────────────────────────┘

Generated SQL:
SELECT id, name, email
FROM users
WHERE active = 1
  AND created_at > '2026-01-01'
ORDER BY created_at DESC
```

### Joins Visuales

**Drag relación entre tablas**:

```
[users] ─── user_id ───> [orders]
```

Genera:
```sql
SELECT u.name, o.total
FROM users u
JOIN orders o ON u.id = o.user_id
```

---

## Beneficios

- ✅ Para usuarios no-técnicos
- ✅ Aprender SQL visualmente
- ✅ Evitar errores de sintaxis
- ✅ Más rápido para queries simples

---

## Roadmap

- v2.0: Query builder básico (SELECT, WHERE)
- v2.5: Joins visuales
- v3.0: Agregaciones (GROUP BY, HAVING)
