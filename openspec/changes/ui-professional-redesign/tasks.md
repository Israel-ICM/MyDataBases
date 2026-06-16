# Tasks: UI Professional Redesign

## Fase 1: Foundation (15min)

- [ ] 1.1 Crear `ui/theme/DbAccents.kt` con mapping `DbType → Color` (MYSQL=#00758F, POSTGRES=#336791, MARIADB=#C49A6C, SQLITE=#003B57)
- [ ] 1.2 Crear helper `ui/components/PressAnimation.kt` con `Modifier.pressAnimation(scale=0.97f, elevation=8.dp)`

**Verificación**: Build SUCCESS. DbAccents accesible desde ConnectionCard. Helper animado compilable.

---

## Fase 2: ConnectionCard Hero Redesign (30min)

- [ ] 2.1 En `ConnectionCard.kt`: reemplazar layout Column por Row con Icon hero 56.dp + Column de contenido
- [ ] 2.2 Aplicar icono DB desde `AppIcons.Db` según `connection.type` (MySql/Postgres/Sqlite/MariaDb)
- [ ] 2.3 Agregar gradient background `Brush.linearGradient` con `DbAccents.colorFor(type).copy(alpha=0.08f) → surface`
- [ ] 2.4 Cambiar `Text(connection.name)` de `titleMedium` a `titleLarge`
- [ ] 2.5 Agregar status dot visual (Box 8.dp con color success/error) a la derecha del título
- [ ] 2.6 Aplicar `Modifier.pressAnimation()` al Card onClick

**Verificación manual**: 
- Abrir app → ConnectionsList
- Hero icon 56dp visible por cada card
- Gradient sutil de fondo por tipo DB (MySQL teal, Postgres blue, etc.)
- Título más grande y prominente
- Dot verde/rojo visible arriba-derecha del título
- Press animación escala 0.97 al tocar card

---

## Fase 3: ConnectionsListScreen Header (30min)

- [ ] 3.1 En `ConnectionsListScreen.kt`: reemplazar `TopAppBar` por `LargeTopAppBar`
- [ ] 3.2 Agregar stats row debajo del título dentro del TopAppBar (total conexiones + quick action indicator)
- [ ] 3.3 Aplicar `AppSpacing.lg` consistente para padding del stats row

**Verificación manual**:
- Abrir app → ConnectionsList
- Header más alto con título grande
- Stats visibles (ej: "3 conexiones")
- Collapse suave al scroll

---

## Fase 4: DatabaseCard + TableCard Redesign (1h)

- [ ] 4.1 En `DatabaseCard.kt`: agregar icono de DB (24.dp) a la izquierda del nombre
- [ ] 4.2 En `DatabaseCard.kt`: convertir charset/collation de Text a SuggestionChip con ícono info
- [ ] 4.3 En `TableCard.kt`: agregar Badge visual para rowCount en vez de texto plano (badge con número)
- [ ] 4.4 En `TableCard.kt`: agregar icono de tabla (24.dp) a la izquierda del nombre según `table.type` (TABLE/VIEW)
- [ ] 4.5 Aplicar `Modifier.pressAnimation()` a DatabaseCard y TableCard

**Verificación manual**:
- Conectar a DB → DatabasesList
- Icono DB visible en cada DatabaseCard
- Charset/collation como chip visual
- Conectar a DB → TablesList
- Icono de tabla visible
- Badge numérico para rowCount (ej: badge "1523" en vez de "• 1523 filas")
- Press animación en ambos cards

---

## Fase 5: TableViewerScreen Tabs (30min)

- [ ] 5.1 En `TableViewerScreen.kt`: agregar icono a Tab "Rows" (ic_nav_tables)
- [ ] 5.2 En `TableViewerScreen.kt`: agregar icono a Tab "Schema" (ic_nav_editor)
- [ ] 5.3 Agregar Badge numérico a Tab "Rows" con rowCount si disponible
- [ ] 5.4 Agregar Badge numérico a Tab "Schema" con columnas.size

**Verificación manual**:
- Abrir tabla → TableViewerScreen
- Tabs "Rows" y "Schema" tienen iconos
- Badge visible en "Rows" con número de filas
- Badge visible en "Schema" con número de columnas

---

## Fase 6: Polish y Cleanup (15min)

- [ ] 6.1 Revisar imports en todos los archivos modificados — remover imports no usados
- [ ] 6.2 Verificar que NO se introdujeron hardcoded colors — todo usa `DbAccents` o `MaterialTheme.colorScheme`
- [ ] 6.3 Verificar que NO se introdujeron hardcoded spacing — todo usa `LocalAppSpacing.current`
- [ ] 6.4 Correr `./gradlew spotlessApply` (si aplica) o manual format

**Verificación manual**:
- Build SUCCESS sin warnings
- No hardcoded values (8.dp → spacing.sm, Color(0xFF...) → DbAccents)
- Preview de cada Card renderiza correctamente
