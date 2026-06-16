# PlayStation Workspace Design - MyDataBases

## Resumen

Rediseño completo del TableViewer inspirado en PlayStation App con sistema de cards apiladas draggables tipo bottom sheet + carousel.

## Capturas de referencia

Ver capturas compartidas de PlayStation App:
1. Vista principal con juegos (equivalente a nuestra lista de tablas)
2. Vista de chat con bottom sheet (referencia de interacción drag)
3. Vista de chat expandido (referencia de transiciones)

---

## Lista de tablas (equivalente a juegos en PlayStation)

- **Lista compacta** sin ocupar mucho espacio
- **Cards pequeñas** con:
  - Ícono de tabla
  - Nombre
  - Metadata mínima (row count, tamaño, etc.)
- **Al hacer clic en una tabla** → se crea una card draggable arriba

---

## Sistema de cards apiladas (parte superior) - CORRECCIÓN IMPORTANTE

### Comportamiento principal
- Cuando abro una **tabla/query/vista** → se agrega una **card en la parte superior en estado MINIMIZADO**
- Las cards viven **ARRIBA** como pestañas apiladas (NO abajo como bottom sheet tradicional)
- **Drag DOWN** en una card minimizada → la card **BAJA y se EXPANDE** (cubre ~70% pantalla)
- **Drag UP** en una card expandida → la card **SUBE y se MINIMIZA** (vuelve arriba como pestaña)
- **Máximo 3 cards visibles** apiladas arriba
- **4ta card** muestra ícono **"+"** indicando que hay más cards escondidas

### Estados de card
- **Minimizada (arriba)**: altura ~80-100dp, muestra título + ícono, posicionada arriba
- **Expandida (abajo)**: altura ~70% pantalla, muestra contenido completo, posicionada más abajo
- **Transición**: animación suave de arriba ↔ abajo con drag gesture

### Panel de cards expandido
- Al tocar el **"+"** → abre **panel con todas las cards en carousel**
- Puedo **arrastrar/swipear** entre cards del carousel
- Cada card puede mostrar:
  - Estructura de tabla (schema + datos)
  - Query editor
  - Vista
  - Backup/export
  - Funciones/stored procedures

---

## Navegación inferior (equivalente a navegación PlayStation)

Bottom navigation con opciones:
- **Tablas**
- **Queries**
- **Vistas**
- **Backups**
- (otras opciones de DB según contexto)

---

## Interacciones clave

| Gesto | Acción |
|-------|--------|
| **Drag vertical** | Minimizar/maximizar card activa (como bottom sheet) |
| **Swipe horizontal** | Cambiar entre cards del carousel |
| **Tap en card minimizada** | Expandir esa card |
| **Tap en "+"** | Abrir panel de todas las cards |
| **Tap en navegación inferior** | Cambiar contexto (tablas/queries/vistas/backups) |

---

## Elementos visuales PlayStation

- **Cards con elevación/shadow pronunciada** (similar a PlayStation cards)
- **Transiciones fluidas** con animaciones suaves
- **Fondos con gradientes sutiles**
- **Tipografía clara** y espaciado generoso
- **Sin borders** → énfasis en sombras y elevación
- **Mucho aire** entre elementos

---

## Componentes a crear

### 1. `WorkspaceCardStack` (nuevo)
- Maneja las 3 cards visibles + indicador "+"
- Controla el drag vertical para minimizar/expandir
- Animaciones de apilado

### 2. `WorkspaceCarousel` (nuevo)
- Panel expandido con todas las cards
- Swipe horizontal entre cards
- Indicadores de posición (dots o similar)

### 3. `TableCard` (refactor)
- Card individual draggable
- Estados: expandido, minimizado, carousel
- Contenido: tabla data grid + schema info

### 4. `DatabaseBottomNav` (nuevo)
- Navegación inferior con íconos
- Tabs: Tablas, Queries, Vistas, Backups
- Estilo PlayStation con íconos grandes

---

## Flujo de usuario

1. Usuario conecta a DB → ve lista de tablas compacta
2. Usuario toca tabla "users" → se crea card arriba con los datos
3. Usuario puede hacer **drag down** en la card para minimizarla (queda apilada arriba)
4. Usuario toca otra tabla "orders" → nueva card se apila (ahora hay 2)
5. Usuario toca otra tabla "products" → nueva card (ahora hay 3 visibles)
6. Usuario toca otra tabla "categories" → aparece ícono **"+"** (indica 4+ cards)
7. Usuario toca **"+"** → se abre panel carousel con las 4 cards
8. Usuario **swipea horizontal** para navegar entre cards
9. Usuario toca navegación inferior **"Queries"** → cambia contexto a editor SQL

---

## Implementación sugerida

### Fase 1: WorkspaceCardStack básico
- Card única expandible/minimizable con drag vertical
- Animación de transición smooth

### Fase 2: Sistema de apilado
- Hasta 3 cards visibles apiladas
- Indicador "+" cuando hay más de 3

### Fase 3: Carousel completo
- Panel expandido con todas las cards
- Swipe horizontal funcional

### Fase 4: Bottom navigation
- Tabs funcionales: Tablas, Queries, Vistas, Backups
- Integración con contexto de DB

---

## Notas técnicas

- **Gesture handling**: usar `Modifier.pointerInput` para drag custom
- **Animaciones**: `animateDpAsState`, `AnimatedVisibility` para transiciones
- **Estado**: `WorkspaceViewModel` para manejar cards activas/minimizadas
- **Performance**: lazy loading de contenido de cards no visibles

---

## 🎨 Animaciones (CRÍTICO)

**TODAS las animaciones deben ser suaves, fluidas y agradables - es parte esencial del diseño PlayStation.**

### Specs de animaciones

| Elemento | Tipo de animación | Duración | Easing |
|----------|------------------|----------|--------|
| **Card expandir** | Spring animation | ~400ms | damping 0.8f, stiffness Medium |
| **Card minimizar** | Scale + TranslationY + Alpha | 350ms | FastOutSlowInEasing |
| **Swipe horizontal** | TranslationX animada | 300ms | FastOutSlowInEasing |
| **Aparecer card** | Fade + Scale (0.9f → 1f) | 400ms | EaseOutCubic |
| **Desaparecer card** | Fade + Scale (1f → 0.9f) | 300ms | EaseInCubic |
| **Apilado** | Offset Y con desfase 8dp | 350ms | FastOutSlowInEasing |
| **Indicador "+"** | Pulse scale (1f → 1.1f → 1f) | 600ms loop | EaseInOutCubic |

### Implementación

```kotlin
// Spring para drag
val offsetY by animateFloatAsState(
    targetValue = targetOffset,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
)

// Fade + Scale combinados
AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn(animationSpec = tween(400)) + scaleIn(
        initialScale = 0.9f,
        animationSpec = tween(400, easing = EaseOutCubic)
    ),
    exit = fadeOut(animationSpec = tween(300)) + scaleOut(
        targetScale = 0.9f,
        animationSpec = tween(300, easing = EaseInCubic)
    )
)

// Parallax sutil en cards apiladas
val parallaxOffset = (cardIndex * 4.dp) * scrollProgress
```

### Principios
- ✅ **Nunca usar animaciones lineales** - siempre usar easing curves
- ✅ **Spring animations para gestures** - se siente más natural
- ✅ **Combinar transformaciones** - fade + scale + translate juntos
- ✅ **Anticipación suave** - elementos no aparecen/desaparecen bruscamente
- ✅ **Probar en dispositivo real** - las animaciones se sienten diferente que en emulador
