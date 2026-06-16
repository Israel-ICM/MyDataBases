# Especificación: ui-premium-components

## Propósito

Elevar la sensación visual de listas, estados de carga, estados vacíos y errores al estándar premium (Navicat + PlayStation App), reemplazando `CircularProgressIndicator` genérico por skeletons por-pantalla, agregando elevación + ripple branded a cards y proveyendo `EmptyState` ilustrado reusable.

## Requirements

### Requirement: Cards refactorizadas con elevación y ripple branded

El sistema MUST refactorizar `ConnectionCard`, `DatabaseCard` y `TableCard` para que:

- Apliquen `Modifier.shadow(elevation = AppElevation.cardResting, shape = AppShapes.medium)`.
- Usen `AppShapes.medium` (12 dp) como corner radius.
- Sean `clickable` con ripple cuyo color sea `MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)`.
- Soporten `Modifier.animateContentSize()` desde día 1 (no-op si reduced motion).
- Tengan padding interno `AppSpacing.md` (12 dp).
- En su contenido principal usen `MaterialTheme.colorScheme.onSurface` (texto principal) y `outline` (metadata secundaria).

#### Scenario: ConnectionCard se ve elevada

- GIVEN `ConnectionListScreen` con 3 conexiones
- WHEN se renderiza
- THEN cada `ConnectionCard` muestra una sombra sutil (1 dp)
- AND el ripple al tap es de tinte `primary` (no gris default)

#### Scenario: Card en pressed

- GIVEN usuario haciendo press sostenido sobre una `DatabaseCard`
- WHEN se mantiene el press
- THEN aparece el ripple branded animado (o instantáneo si reduced motion)
- AND la card NO cambia de elevación (la animación de elevation se difiere a un cambio futuro)

### Requirement: Skeletons por-pantalla

El sistema MUST proveer skeletons específicos que reproduzcan la silueta del contenido real:

| Skeleton                  | Reproduce silueta de              | Cantidad de items placeholder |
|---------------------------|------------------------------------|-------------------------------|
| `ConnectionListSkeleton`  | Lista de `ConnectionCard`         | 5                             |
| `DatabaseListSkeleton`    | Lista de `DatabaseCard`           | 6                             |
| `TableListSkeleton`       | Lista de `TableCard`              | 8                             |
| `TableViewerSkeleton`     | Grid de filas/columnas de tabla   | 10 filas × 4 columnas         |

Los skeletons MUST componerse desde primitivas:

- `SkeletonBox(width, height, shape)`: rectángulo gris.
- `SkeletonText(width, lines)`: simula líneas de texto.
- `SkeletonCircle(size)`: círculo gris (para íconos/avatares).

Color del placeholder: `MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)`.

Los skeletons MUST NO incluir animación shimmer en esta capability (se difiere al cambio de motion); son gris estático.

#### Scenario: ConnectionList muestra skeleton durante carga

- GIVEN `ConnectionListScreen` en estado `Loading`
- WHEN se renderiza
- THEN se muestra `ConnectionListSkeleton` con 5 cards placeholder
- AND NO se muestra `CircularProgressIndicator`
- AND el layout coincide visualmente con la lista real (mismas alturas y paddings)

#### Scenario: TableViewer muestra skeleton de grid

- GIVEN `TableViewerScreen` cargando datos
- WHEN se renderiza
- THEN se muestra `TableViewerSkeleton` con 10 filas × 4 columnas
- AND los anchos de columna aproximan los reales

### Requirement: EmptyState ilustrado reusable

El sistema MUST exponer:

```kotlin
@Composable
fun EmptyState(
    icon: Painter,
    title: String,
    description: String? = null,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
)
```

Anatomía:

1. Ícono central (tamaño 96 dp), color `outline`.
2. Spacer `AppSpacing.lg` (16 dp).
3. Título (`MaterialTheme.typography.titleMedium`, color `onSurface`).
4. Spacer `AppSpacing.sm` (8 dp).
5. Descripción opcional (`bodyMedium`, color `outline`, max 2 líneas, centrada).
6. Spacer `AppSpacing.xl` (24 dp).
7. Acción opcional (`Button`, color `primary`).

Todo centrado vertical y horizontalmente con padding `AppSpacing.xl`.

#### Scenario: Empty state sin conexiones

- GIVEN `ConnectionListScreen` con 0 conexiones
- WHEN se renderiza
- THEN aparece `EmptyState` con:
  - ícono `AppIcons.State.EmptyConnections`
  - título: `R.string.empty_connections_title` (es: "Sin conexiones", en: "No connections")
  - descripción: `R.string.empty_connections_description`
  - acción: botón "Nueva conexión" (es) / "New connection" (en)

#### Scenario: Empty state sin tablas

- GIVEN `TableListScreen` dentro de una conexión con 0 tablas
- WHEN se renderiza
- THEN aparece `EmptyState` con título "Sin tablas" / "No tables"
- AND NO botón de acción (las tablas se crean desde Editor)

### Requirement: ErrorCard mejorada

El sistema MUST refactorizar `ErrorCard` existente para:

- Mostrar ícono `AppIcons.State.Error` (32 dp) a la izquierda.
- Título de error (`titleMedium`, color `error`).
- Descripción del error (`bodyMedium`, color `onSurface`).
- Botón "Reintentar" / "Retry" (`OutlinedButton`, color `error`).
- Background `errorContainer`, corner `AppShapes.medium`, padding `AppSpacing.lg`.

El callback `onRetry: () -> Unit` MUST ser opcional; si es `null`, el botón no se renderiza.

#### Scenario: Error al cargar conexiones

- GIVEN `ConnectionListScreen` con estado `Error("No se pudo conectar")`
- WHEN se renderiza
- THEN aparece `ErrorCard` con ícono de error, mensaje, y botón "Reintentar"

#### Scenario: Error sin retry

- GIVEN un error no recuperable (ej.: corrupción de DB local)
- WHEN se construye `ErrorCard(onRetry = null)`
- THEN el botón "Reintentar" NO aparece

## Non-Functional Requirements

- **Performance**: Cada skeleton MUST renderizar a 60 fps en dispositivos API 29+ de gama media.
- **Accessibility**:
  - Todos los `Icon` decorativos MUST usar `contentDescription = null`.
  - Skeletons MUST tener `Modifier.semantics { contentDescription = stringResource(R.string.loading) }` en el container raíz.
  - `EmptyState` MUST anunciar título + descripción a TalkBack.
- **Localización**: Todos los strings (`R.string.empty_*`, `R.string.error_retry`, etc.) MUST estar en `strings.xml` es + en.

## Edge Cases

#### Scenario: Skeleton mientras se cambia de tema

- GIVEN `ConnectionListScreen` mostrando `ConnectionListSkeleton`
- WHEN el usuario activa branded palette en otra pantalla y vuelve
- THEN el skeleton actualiza su color de placeholder al `surfaceVariant` del nuevo tema
- AND no parpadea

#### Scenario: EmptyState en Expanded (tablet grande)

- GIVEN `TableListScreen` vacío en tablet landscape
- WHEN se renderiza
- THEN el `EmptyState` aparece centrado en el pane de detalle (no ocupa toda la pantalla)
- AND el ícono mantiene 96 dp (no se escala)

#### Scenario: ErrorCard con mensaje muy largo

- GIVEN un mensaje de error de 500 caracteres
- WHEN se renderiza `ErrorCard`
- THEN la descripción muestra hasta 4 líneas y luego "..."
- AND el botón "Reintentar" sigue siendo accesible

## Constraints

- Los componentes NUEVOS (`SkeletonLoader*`, `EmptyState`) MUST vivir en `ui/components/`.
- `ErrorCard.kt` existente MUST mantener su nombre y firma pública (parámetros) para no romper consumers; solo se actualiza la implementación visual y se agrega el ícono.
- Los componentes MUST NO acceder directamente a `ViewModel`; reciben estado por parámetros.
