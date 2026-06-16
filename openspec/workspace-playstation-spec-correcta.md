# Workspace PlayStation - Especificación CORRECTA

**Fecha**: 2026-06-15
**Estado**: Fase 1 COMPLETADA ✅
**Última actualización**: 2026-06-15

---

## Comportamiento CORRECTO según capturas PlayStation

### Posición y estado inicial
- Las cards viven **ARRIBA** en estado minimizado (peek)
- Se ven como **pestañas apiladas** una sobre otra en la parte superior
- Muestran solo un preview pequeño (como tabs horizontales)
- Pueden haber múltiples cards apiladas simultáneamente

### Animación de expansión (drag DOWN)
- Usuario hace **drag hacia ABAJO** en una card minimizada
- La card activa **baja** expandiéndose (como bottom sheet)
- Cubre la mayor parte de la pantalla pero deja las otras cards arriba visibles
- Las otras cards quedan arriba en estado minimizado/apiladas

### Animación de colapso (drag UP)
- Usuario hace **drag hacia ARRIBA** en la card expandida
- La card **sube** y vuelve a su posición minimizada arriba
- Vuelve al estado de pestaña apilada

### Visualización
- **Minimizada (arriba)**: altura pequeña ~80-100dp, muestra título + ícono
- **Expandida (baja)**: ocupa ~70% de pantalla, muestra contenido completo
- **Apilado**: máximo 3 cards visibles, 4ta muestra "+" indicador

---

## Lo que estaba MAL en Fase 1 (implementación incorrecta)

- ❌ Card subía desde abajo (bottom sheet tradicional)
- ❌ Posición inicial abajo en lugar de arriba
- ❌ Drag down minimizaba en lugar de expandir
- ❌ No había sistema de apilado visible arriba

## Lo CORRECTO

- ✅ Cards viven arriba minimizadas (pestañas)
- ✅ Drag DOWN expande (card baja)
- ✅ Drag UP colapsa (card sube)
- ✅ Múltiples cards apiladas arriba simultáneamente

---

## Fases de implementación CORREGIDAS

### Fase 1: Card única con drag correcto ✅ COMPLETADA
**Componentes implementados:**
- `TopSheet.kt`: Bottom sheet invertido que baja desde arriba
- `WorkspaceOverlay.kt`: Capa que integra TopSheet sobre contenido de fondo
- `WorkspaceManager.kt`: Singleton para gestión de estado de cards
- `WorkspaceCard.kt`: Sealed class para tipos de cards (Table, Query, View, etc.)
- `TableCardContent.kt`: Contenido específico para cards de tipo Table

**Funcionalidad completada:**
- ✅ Card posicionada ARRIBA en estado minimizado (peek 60dp)
- ✅ Drag DOWN → expande y baja (hasta 92% altura de pantalla)
- ✅ Drag UP → colapsa y sube (vuelve a peek)
- ✅ Animaciones fluidas: 0ms durante drag (sigue el dedo), 300ms al soltar
- ✅ Backdrop progresivo: alpha 0.0→0.5 según expansión
- ✅ Backdrop cubre toda la pantalla (TopAppBar + contenido + bottom nav)
- ✅ Auto-expansión al seleccionar tabla
- ✅ Click en backdrop cierra el panel (colapsa)
- ✅ Handle visible de 48x5dp en parte inferior del panel
- ✅ Threshold de 100dp para cambio de estado (evita cambios accidentales)
- ✅ Integrado con Hilt para inyección de WorkspaceManager

**Detalles técnicos:**
- Altura panel: 92% de screenHeight (calculado con LocalConfiguration)
- Offset minimizado: `-sheetHeightPx + peekHeightPx` (negativo, oculto arriba)
- Offset expandido: `0f` (visible desde el top)
- Drag state: `isDragging` controla animationSpec (0ms vs 300ms)
- LaunchedEffect sincroniza `rawOffset` cuando `isExpanded` cambia externamente
- Backdrop alpha = `expansionProgress × 0.5f`

### Fase 2: Sistema de apilado
- Hasta 3 cards visibles apiladas arriba
- Indicador "+" cuando hay más de 3
- Offset entre cards apiladas (~8dp)

### Fase 3: Carousel expandido
- Panel con todas las cards
- Swipe horizontal para navegar
- Indicadores de posición

### Fase 4: Contenido real + Bottom navigation
- ✅ **Integrar TableCardContent en cards expandidas** — COMPLETADO (`WorkspaceOverlay.kt:157-166`):
  - Se reemplazaron los textos metadata "Database: / Table:" por el composable `TableCardContent`
  - Se agregó `LaunchedEffect` en `TableCardContent` para que cargue datos cuando se usa standalone (cada `hiltViewModel()` es instancia separada)
  - El grid muestra columnas + filas con scroll horizontal en el panel expandido
- 🔲 Bottom nav: Tablas/Queries/Vistas/Backups

---

## Historial de correcciones - Fase 1

### Commit f6f580a - Backdrop fullscreen
**Problema**: Backdrop no cubría TopAppBar ni bottom navigation
**Solución**: Agregar `Modifier.fillMaxSize()` explícito a TopSheet en WorkspaceOverlay
**Resultado**: Backdrop cubre toda la pantalla incluyendo UI del sistema

### Commit 79d9407 - Auto-expansión
**Problema**: Panel quedaba minimizado al seleccionar tabla
**Solución**: LaunchedEffect que detecta cambios en `activeCards.size` y expande automáticamente
**Resultado**: Panel se abre directo cuando usuario toca una tabla

### Commit b689af8 - Sincronización backdrop click
**Problema**: Click en backdrop cambiaba estado pero panel no se animaba hacia arriba
**Solución**: LaunchedEffect que actualiza `rawOffset` cuando `isExpanded` cambia externamente
**Resultado**: Panel sube suavemente al hacer click en backdrop

### Commit caa81ce - Ajuste altura
**Problema**: Panel ocupaba 97% de pantalla (muy alto)
**Solución**: Cambiar de 97% a 92% de screenHeight
**Resultado**: Panel deja más espacio visible arriba cuando está expandido

### Commit 4ff7281 - Implementación inicial
**Funcionalidad**: TopSheet component completo con drag fluido, backdrop progresivo, integración con WorkspaceManager
**Archivos creados**: TopSheet.kt, WorkspaceOverlay.kt, DraggableCard.kt
**Docs**: PLAYSTATION_WORKSPACE_DESIGN.md, workspace-playstation-spec-correcta.md

---

## Próximos pasos (Fase 2)

1. Sistema de apilado de cards
   - Mostrar hasta 3 cards apiladas arriba
   - Offset visual de ~8dp entre cards
   - Indicador "+" cuando hay 4 o más cards
   
2. Selección de card activa
   - Tap en card minimizada la trae al frente
   - Card activa tiene mayor elevación
   
3. Cierre individual
   - Botón X en cada card minimizada
   - Animación de salida suave
