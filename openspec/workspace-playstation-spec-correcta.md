# Workspace PlayStation - Especificación CORRECTA

**Fecha**: 2026-06-15
**Estado**: Especificación recuperada después de pérdida de contexto matutino

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

### Fase 1: Card única con drag correcto (PENDIENTE RE-HACER)
- Card posicionada ARRIBA en estado minimizado
- Drag DOWN → expande y baja
- Drag UP → colapsa y sube
- Animaciones suaves spring

### Fase 2: Sistema de apilado
- Hasta 3 cards visibles apiladas arriba
- Indicador "+" cuando hay más de 3
- Offset entre cards apiladas (~8dp)

### Fase 3: Carousel expandido
- Panel con todas las cards
- Swipe horizontal para navegar
- Indicadores de posición

### Fase 4: Contenido real + Bottom navigation
- Integrar TableCardContent en cards expandidas
- Bottom nav: Tablas/Queries/Vistas/Backups
