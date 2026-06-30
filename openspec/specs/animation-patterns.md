# Animation Patterns

Patrones de animación reutilizables definidos en el proyecto.

## Spring Slide-Up Animation

**Descripción**: Animación suave de deslizamiento desde abajo con efecto de rebote natural.

**Uso**: Elementos que aparecen desde fuera de pantalla (bottom) sincronizados con otra animación pero con movimiento independiente.

**Implementación**:
```kotlin
val animatedOffsetY by animateFloatAsState(
    targetValue = targetOffsetY,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    ),
    label = "springSlideUp"
)

// Aplicar con graphicsLayer para performance
Modifier.graphicsLayer { 
    translationY = animatedOffsetY
}
```

**Parámetros clave**:
- `dampingRatio`: `Spring.DampingRatioMediumBouncy` - rebote medio, da sensación natural
- `stiffness`: `Spring.StiffnessLow` - rigidez baja, movimiento suave y fluido
- `translationY`: offset positivo = abajo, 0 = posición final

**Características**:
- Suaviza movimientos rápidos/bruscos del target
- Independiente del drag del usuario
- Performante con `graphicsLayer` (no causa recomposición)
- Efecto "bouncy" natural que se siente premium

**Casos de uso**:
- Toolbar flotante en workspace (implementado en `WorkspaceOverlay.kt`)
- Bottom sheets que aparecen desde abajo
- FABs que entran desde fuera de pantalla
- Cualquier elemento que necesite entrada suave desde bottom

**Ejemplo real**:
```kotlin
// Toolbar que sube desde abajo sincronizada con topsheet
val targetOffsetY = with(density) { 
    (1f - expansionProgress) * 100.dp.toPx()
}

val animatedOffsetY by animateFloatAsState(
    targetValue = targetOffsetY,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    ),
    label = "toolbarOffset"
)

QueryEditorToolbarRow(
    modifier = Modifier.graphicsLayer { 
        translationY = animatedOffsetY
    }
)
```

**Notas**:
- La animación spring maneja cambios bruscos mejor que tween
- El rebote medio (`MediumBouncy`) es más sutil que `HighBouncy`
- `StiffnessLow` hace que la animación sea más lenta/suave vs `StiffnessMedium`
- Usar siempre `graphicsLayer` en lugar de `offset` para mejor performance

---

**Fecha creación**: 2026-06-30  
**Implementado en**: `WorkspaceOverlay.kt` - floating toolbar  
**Autor**: israel-icm
