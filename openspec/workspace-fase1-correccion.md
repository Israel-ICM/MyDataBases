# Workspace Fase 1 - Corrección de drag invertido

**Fecha**: 2026-06-15
**Tipo**: Bugfix

---

## Problema

La implementación inicial de `DraggableCard` estaba **completamente invertida**:
- ❌ Card subía desde abajo (bottom sheet tradicional)
- ❌ Drag down minimizaba
- ❌ Drag up expandía
- ❌ Posición inicial abajo

## Solución

Invertida toda la lógica para coincidir con PlayStation App:
- ✅ Card vive arriba en estado minimizado
- ✅ Drag DOWN expande (card baja)
- ✅ Drag UP minimiza (card sube)
- ✅ Posición inicial arriba

---

## Cambios en código

### DraggableCard.kt

```kotlin
// ANTES (INCORRECTO)
val targetOffsetY = if (isExpanded) 0.dp else screenHeight - 200.dp
// Card expandida arriba (0dp), minimizada abajo (screenHeight - 200dp)

// AHORA (CORRECTO)
val targetOffsetY = if (isExpanded) (screenHeight * 0.3f) else 0.dp
// Card minimizada arriba (0dp), expandida abajo (30% offset)
```

```kotlin
// ANTES (INCORRECTO)
if (isExpanded && dragOffsetY > threshold) {
    onDragStateChange(false) // Drag down → minimizar
}

// AHORA (CORRECTO)
if (!isExpanded && dragOffsetY > threshold) {
    onDragStateChange(true) // Drag down → EXPANDIR
} else if (isExpanded && dragOffsetY < -threshold) {
    onDragStateChange(false) // Drag up → MINIMIZAR
}
```

```kotlin
// ANTES (INCORRECTO)
shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
// Esquinas arriba (para bottom sheet)

// AHORA (CORRECTO)
shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
// Esquinas abajo (card vive arriba)
```

### WorkspaceOverlay.kt

```kotlin
// ANTES (INCORRECTO)
var isCardExpanded by remember { mutableStateOf(true) }
.align(Alignment.BottomCenter)

// AHORA (CORRECTO)
var isCardExpanded by remember { mutableStateOf(false) }
.align(Alignment.TopCenter)
```

---

## Resultado

**Estado minimizado (peek)**:
- Posición: arriba (offset 0dp)
- Altura: 100dp
- Muestra: título + ícono
- Esquinas redondeadas: abajo

**Estado expandido**:
- Posición: baja ~30% (offset screenHeight * 0.3f)
- Altura: 600dp
- Muestra: contenido completo
- Mismas esquinas (consistente)

**Interacción**:
- Tap en tabla → card aparece arriba minimizada
- Drag DOWN → card baja expandiéndose
- Drag UP → card sube minimizándose
