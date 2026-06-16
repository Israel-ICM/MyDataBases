# Estándares de Código

## Seguí Estos

1. **Principios SOLID**
2. **DRY** (Don't Repeat Yourself)
3. **KISS** (Keep It Simple, Stupid)
4. **Clean Code** por Robert C. Martin

## Estilo Kotlin

- Seguí la [guía de estilo oficial de Kotlin](https://kotlinlang.org/docs/coding-conventions.html)
- Usá `ktlint` para formateo
- Usá `detekt` para análisis estático

## Convenciones de Nombres

- **Clases**: PascalCase (`UserRepository`)
- **Funciones**: camelCase (`fetchUserData`)
- **Constantes**: UPPER_SNAKE_CASE (`MAX_RETRY_COUNT`)
- **Paquetes**: lowercase (`com.sphynxs.mydatabases.feature.auth`)

## Organización de Archivos

- Una clase por archivo
- El nombre del archivo coincide con el nombre de la clase
- Agrupar clases relacionadas en el mismo paquete
- Evitar paquetes profundamente anidados

## Calidad de Código

- **Sin números mágicos**: Usar constantes con nombre
- **Sin funciones largas**: Máximo 20-30 líneas
- **Sin clases dios**: Principio de Responsabilidad Única
- **Sin código comentado**: Eliminarlo (git tiene historial)
- **Nombres significativos**: `getUserById()` no `get()`

## Comentarios

- **Por qué, no qué**: Explicar intención, no implementación
- **Evitar comentarios obvios**: `// Set name` es inútil
- **KDoc para APIs públicas**: Documentar funciones y clases públicas

```kotlin
/**
 * Obtiene datos del usuario desde la API remota.
 *
 * @param userId El identificador único del usuario
 * @return Result conteniendo datos del usuario o error
 */
suspend fun fetchUser(userId: String): Result<User>
```

## Manejo de Errores

- Usar `Result<T>` para fallos esperados
- Usar excepciones para fallos inesperados
- Nunca tragar excepciones silenciosamente
- Loguear errores con contexto

---

*Patrones detallados de Kotlin en `.atl/agents/kotlin-expert.md`.*
