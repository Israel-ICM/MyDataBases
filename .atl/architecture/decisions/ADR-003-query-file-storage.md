# ADR-003: Almacenamiento de query files particionado por motor

## Estado

✅ **Aceptado**

Fecha: 2026-08-05

## Contexto

El cambio `query-files-storage` agrega una pantalla que lista los archivos `.sql`
creados/guardados por el usuario, sin una tabla de tracking en base de datos —
el contenido real de una carpeta es la fuente de verdad. Esto requirió decidir:

1. Dónde vive esa carpeta (privada de la app vs. elegida por el usuario).
2. Cómo particionarla, dado que la app soporta múltiples motores (MySQL,
   MariaDB, PostgreSQL, SQLite) con conexiones potencialmente ilimitadas.
3. Cómo unificar el acceso a ambas fuentes (archivo privado vs. árbol SAF) sin
   duplicar lógica de listado/lectura/escritura.
4. Cómo manejar la pérdida de permiso SAF (SD card removida, grant revocado)
   sin crashear ni bloquear al usuario.

**Opciones evaluadas para el particionado**:

1. **Una sola carpeta global** para toda la app, sin distinción de motor ni
   conexión.
2. **Una carpeta por conexión individual** (ej. por `connectionId`).
3. **Una carpeta por tipo de motor** (`DatabaseType`), compartida entre todas
   las conexiones de ese mismo motor.

## Decisión

**Opción 3**: `MyDataBase/{engineType}/queries/`, donde `{engineType}` es
`DatabaseType.name.lowercase()` (`mysql`, `mariadb`, `postgresql`, `sqlite`).

**Razón**:

- Los dialectos SQL no son portables entre motores — una query de MySQL
  típicamente no corre igual en PostgreSQL. Separar por motor evita mezclar
  archivos que no tienen sentido compartir.
- Una carpeta por conexión individual sería excesivamente granular: si el
  usuario tiene 5 conexiones MySQL distintas, sus queries son en general
  intercambiables entre ellas (mismo dialecto), así que compartir carpeta
  reduce fragmentación sin perder la separación que sí importa (motor).
- Anticipa el requerimiento explícito del usuario de que vendrán más carpetas
  gestionadas por la app en el futuro (backups, exports, etc.) — el mismo
  patrón `{engineType}/{proposito}/` se reutiliza sin rediseño.

**Alternativa descartada**: carpeta global única — se descartó porque mezclar
dialectos SQL sin ninguna separación hace que la lista sea menos útil a medida
que crece (no hay forma de saber a qué motor pertenece cada archivo sin
abrirlo).

## Decisiones relacionadas

### `QueryFileStore` — implementación única sobre raíz resuelta

En vez de dos implementaciones (`PrivateQueryFileStore`/`SafQueryFileStore`),
se usa **una sola** `QueryFileStoreImpl` que opera sobre lo que devuelve
`QueryStorageResolver.resolveRoot()` — un `DocumentFile` que puede venir de
`DocumentFile.fromFile()` (privado) o `DocumentFile.fromTreeUri()` (SAF).
`DocumentFile` unifica ambas fuentes detrás de un mismo `Uri`, compatible con
los consumidores existentes de `ContentResolver.openInputStream(...)`.
Evita duplicar la lógica de listado/particionado por motor en dos lugares.

### `RootResolution` — contrato sellado con fallback explícito

`sealed class RootResolution { Resolved(root); Fallback(root, reason) }` deja
explícito en el tipo cuándo se está usando la carpeta privada como
*fallback* (permiso SAF perdido) en vez de como *default* real. El aviso de
fallback se muestra **en cada resolución** donde se detecta la condición —
nunca se suprime después de la primera vez — porque no hay ningún estado de
"ya lo mostré" en ningún lugar del código (decisión confirmada explícitamente
por el usuario: más simple que persistir/cachear un flag de supresión).

### `AppFolder` — particionado como parámetro desde el día uno

Todas las operaciones de `QueryFileStore` (`list`, `write`) reciben
`DatabaseType` explícito, sin default. `AppFolder.resolve(engine, folder)`
también recibe el motor como parámetro, aunque hoy solo existe `AppFolder.Queries`
— así, cuando se agregue una segunda variante (ej. `AppFolder.Backups`), no
hace falta romper la firma de ningún método existente.

## Consecuencias

- ✅ Lista de archivos siempre acotada a un dialecto SQL coherente.
- ✅ Un único código de lectura/escritura/listado, sin ramificación por fuente.
- ✅ Extensible a futuras carpetas gestionadas sin romper la API existente.
- ⚠️ La carpeta privada por defecto se borra al desinstalar la app — mitigado
  con la opción de redirigir a una carpeta SAF (potencialmente en SD
  removible) desde Preferencias.
- ⚠️ El guardado del editor ahora converge a esta carpeta por defecto — el
  guardado legacy (`MediaStore`/`Environment.getExternalStorageDirectory()`)
  fue eliminado; "Guardar como..." (exportación explícita a cualquier
  ubicación) queda intacto.
