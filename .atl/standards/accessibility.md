# Estándares de Accesibilidad

La accesibilidad **no es opcional**.

## Requisitos

- Soporte de TalkBack
- Soporte de fuentes grandes
- Modo de alto contraste
- Navegación por teclado (teclados externos)
- Soporte de lector de pantalla

## Semantics en Compose

Cada elemento interactivo DEBE tener semántica apropiada:

```kotlin
IconButton(
    onClick = { /* ... */ },
    modifier = Modifier.semantics {
        contentDescription = "Eliminar conexión"
        role = Role.Button
    }
) {
    Icon(Icons.Default.Delete, contentDescription = null)
}
```

**Reglas**:

- Establecer `contentDescription` para íconos
- Usar `Role` para identificar tipo de elemento
- Fusionar semántica para componentes complejos

## Escalado de Texto

Soportar configuraciones de tamaño de fuente de Android:

```kotlin
Text(
    text = "Título",
    style = MaterialTheme.typography.headlineMedium,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis
)
```

**Siempre establecer `maxLines` y `overflow`** para prevenir roturas de layout.

## Touch Targets

Tamaño mínimo de touch target: **48dp × 48dp**

```kotlin
IconButton(
    onClick = { /* ... */ },
    modifier = Modifier.size(48.dp)
) {
    Icon(Icons.Default.Add, contentDescription = "Agregar conexión")
}
```

## Contraste de Color

Seguir estándares WCAG 2.1 AA:

- Texto normal: ratio de contraste 4.5:1
- Texto grande: ratio de contraste 3:1
- Elementos interactivos: ratio de contraste 3:1

Material 3 maneja esto por defecto, pero **verificar colores personalizados**.

## Testing

Testear con:

- TalkBack habilitado
- Tamaños de fuente grandes (configuraciones de accesibilidad)
- Modo de alto contraste
- Teclado externo

---

*La accesibilidad se testea durante testing de UI, no como algo posterior.*
