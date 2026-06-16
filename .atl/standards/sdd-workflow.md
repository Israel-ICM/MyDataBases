# Workflow SDD para MyDataBases

**Spec-Driven Development (SDD) es OBLIGATORIO para TODO el desarrollo de MyDataBases.**

## ¿Por Qué SDD?

MyDataBases es una aplicación compleja que requiere:

- ✅ Arquitectura extensible (agregar motores DB sin romper nada)
- ✅ Soporte multi-dispositivo (teléfonos, tablets, foldables)
- ✅ Multilenguaje (español, inglés, más idiomas en el futuro)
- ✅ Seguridad crítica (credenciales de DB, conexiones encriptadas)
- ✅ Play Store compliance (políticas estrictas de Google)
- ✅ Testing riguroso (80%+ cobertura)

**SDD garantiza que TODO esto se planifica ANTES de codificar.**

## Inicialización (Una Sola Vez)

Antes de empezar CUALQUIER desarrollo:

```bash
/sdd-init-jos
```

Esto:

1. Detecta que usamos Kotlin + Jetpack Compose
2. Detecta testing capabilities (JUnit 5, Compose UI Testing, Mockk)
3. Configura Strict TDD Mode si hay soporte de testing
4. Crea el skill registry
5. Configura el backend de artifacts (Engram o OpenSpec)

**Salida esperada**:

```
✅ SDD inicializado
✅ Stack: Kotlin, Jetpack Compose, Material 3
✅ Testing: JUnit 5, Compose Testing, Mockk
✅ Strict TDD: ACTIVADO
✅ Artifact store: Engram
```

## Workflow Completo para una Feature

### Ejemplo: Implementar "Pantalla de Conexiones"

#### 1. Crear el Change (Exploration + Proposal)

```bash
/sdd-new pantalla-conexiones
```

**Qué hace**:

- Explora el problema: ¿Qué necesita la pantalla de conexiones?
- Analiza alternativas: ¿List-Detail? ¿Cards? ¿Table?
- Propone solución: Arquitectura, componentes, navegación
- Define alcance: Qué está dentro y qué está fuera

**Salida**:

- `openspec/pantalla-conexiones/exploration.md`
- `openspec/pantalla-conexiones/proposal.md`

O en Engram:

- `sdd/pantalla-conexiones/explore`
- `sdd/pantalla-conexiones/proposal`

#### 2. Revisar Proposal

**Lee el proposal y verifica**:

- ✅ ¿Define comportamiento en Compact, Medium y Expanded?
- ✅ ¿Especifica strings en español e inglés?
- ✅ ¿Considera seguridad (encriptación de credenciales)?
- ✅ ¿Cumple con arquitectura modular?
- ✅ ¿Menciona accesibilidad (TalkBack, content descriptions)?

**Si falta algo**, ajustá el proposal manualmente o pedí regenerar con `/sdd-new` otra vez.

#### 3. Generar Specs y Design

```bash
/sdd-ff pantalla-conexiones
```

**Fast-forward** genera automáticamente:

- **Spec**: Requisitos detallados, escenarios de usuario, criterios de aceptación
- **Design**: Arquitectura técnica, decisiones de diseño, estructura de componentes
- **Tasks**: Tareas granulares de implementación

**Salida**:

- `openspec/pantalla-conexiones/spec.md`
- `openspec/pantalla-conexiones/design.md`
- `openspec/pantalla-conexiones/tasks.md`

#### 4. Revisar Specs y Design

**Spec debe incluir**:

- Requisitos funcionales (crear, editar, eliminar conexión)
- Requisitos no funcionales (performance, seguridad)
- Escenarios de usuario (flujos completos)
- Criterios de aceptación (cómo validar que funciona)
- **Strings localizados** (español + inglés)
- **Comportamiento adaptativo** (Compact vs Medium vs Expanded)

**Design debe incluir**:

- Estructura de módulos (`feature-connections/`)
- Componentes Compose (`ConnectionScreen`, `ConnectionCard`, etc.)
- ViewModels y estado UI
- Navegación (rutas, parámetros)
- Seguridad (encriptación de credenciales)
- Testing strategy (unit + integration + UI)

**Si falta algo**, ajustá manualmente o regenerá.

#### 5. Implementar (Apply)

```bash
/sdd-apply pantalla-conexiones
```

**Qué hace**:

- Lee spec + design + tasks
- Implementa EXACTAMENTE lo que dicen las specs
- Crea módulos, ViewModels, Composables, repositorios
- Escribe tests ANTES o JUNTO con el código (Strict TDD)
- Usa strings localizados (`stringResource(R.string.connection_title)`)
- Implementa layouts adaptativos (`WindowSizeClass`)
- Sigue Clean Architecture (presentation/domain/data)

**Salida**:

- Código en `feature-connections/`
- Tests en `feature-connections/src/test/` y `src/androidTest/`
- Strings en `res/values/strings.xml` y `res/values-es/strings.xml`
- Progress guardado en `openspec/pantalla-conexiones/apply-progress.md`

#### 6. Verificar (Verify)

```bash
/sdd-verify pantalla-conexiones
```

**Qué hace**:

- Ejecuta TODOS los tests (unit + integration + UI)
- Valida que el código cumple con spec
- Valida que el código cumple con design
- Valida que todas las tasks están completadas
- Genera reporte de verificación

**Salida**:

- Reporte de tests (pasaron/fallaron)
- Comparación spec vs implementación
- Issues encontrados (CRITICAL / WARNING / SUGGESTION)
- `openspec/pantalla-conexiones/verify-report.md`

**Si hay CRITICAL issues**, arreglá y verificá nuevamente.

#### 7. Archivar (Archive)

```bash
/sdd-archive pantalla-conexiones
```

**Qué hace**:

- Sincroniza specs con estado final
- Cierra el change
- Guarda reporte de archivo
- Marca como completado

**Salida**:

- `openspec/pantalla-conexiones/archive-report.md`
- Change cerrado

#### 8. Commit y Push

```bash
git add .
git commit -m "feat(connections): implement connections screen

- Add ConnectionScreen with adaptive layout
- Support Compact/Medium/Expanded WindowSizeClass
- Add ConnectionViewModel with MVVM pattern
- Implement connection CRUD operations
- Add localized strings (es/en)
- Add unit, integration and UI tests

Closes #123"

git push origin feature/pantalla-conexiones
```

#### 9. Crear PR

```bash
gh pr create --title "feat(connections): Pantalla de Conexiones" \
  --body "Implementa pantalla de conexiones según spec.

## Spec
- openspec/pantalla-conexiones/spec.md

## Design
- openspec/pantalla-conexiones/design.md

## Verification
- ✅ Todos los tests pasan
- ✅ Comportamiento adaptativo Compact/Medium/Expanded
- ✅ Strings localizados es/en
- ✅ 85% code coverage

## Screenshots
- [x] Phone (Compact)
- [x] Tablet (Medium)
- [x] Tablet Landscape (Expanded)"
```

## Casos Especiales

### Agregar Nuevo Motor de DB (ej: SQL Server)

```bash
/sdd-new soporte-sql-server
```

**Spec debe incluir**:

- Protocolo de conexión (TDS)
- Driver a usar (JDBC, custom)
- Autenticación (SQL Auth, Windows Auth, Azure AD)
- Diferencias de sintaxis SQL vs MySQL/PostgreSQL
- Testing (mock SQL Server, Docker container)

**Design debe incluir**:

- Implementación de `DatabaseEngine` interface
- `SQLServerEngine` en `core-database/engine/sqlserver/`
- Registro en `DatabaseEngineFactory`
- Tests de integración

### Cambio de Arquitectura (ej: Migrar a MVI)

```bash
/sdd-new arquitectura-mvi
```

**Spec debe incluir**:

- Razón del cambio (por qué MVI vs MVVM actual)
- Impacto en módulos existentes
- Plan de migración (¿gradual? ¿big bang?)
- Estrategia de testing

**Design debe incluir**:

- Estructura MVI (Intent, State, Reducer)
- ADR documentando la decisión
- Patrón de migración módulo por módulo

## Checklist SDD

Antes de cada implementación:

- [ ] `/sdd-init-jos` ejecutado (primera vez)
- [ ] `/sdd-new <feature>` creado
- [ ] Proposal revisado y aprobado
- [ ] Spec completo (requisitos + escenarios + strings + adaptativo)
- [ ] Design completo (arquitectura + componentes + tests)
- [ ] Tasks generados
- [ ] `/sdd-apply` ejecutado
- [ ] Código sigue spec EXACTAMENTE
- [ ] Tests escritos y pasando
- [ ] `/sdd-verify` ejecutado sin CRITICAL issues
- [ ] `/sdd-archive` ejecutado
- [ ] Commit y PR creados

## Beneficios en MyDataBases

### Extensibilidad

**Sin SDD**:

- "Voy a agregar SQL Server"
- *codifica directamente*
- *rompe MySQL, PostgreSQL*
- *debugging por 3 días*

**Con SDD**:

- `/sdd-new soporte-sql-server`
- Spec define interface `DatabaseEngine`
- Design especifica `SQLServerEngine` aislado
- Apply implementa sin tocar código existente
- Verify confirma que MySQL/PostgreSQL siguen funcionando

### Multilenguaje

**Sin SDD**:

- Codifica con strings hardcodeados
- "Después traduzco"
- Olvida strings
- Play Store rechaza por strings sin traducir

**Con SDD**:

- Spec define strings en español e inglés desde el principio
- Apply usa `stringResource()` obligatoriamente
- Verify valida que todos los strings están traducidos

### Tablets

**Sin SDD**:

- Codifica para teléfono
- "Después adapto para tablet"
- UI se ve horrible en tablet
- Users 1-star reviews

**Con SDD**:

- Spec define comportamiento en Compact/Medium/Expanded
- Design especifica layouts adaptativos
- Apply implementa `WindowSizeClass` desde el inicio
- Verify valida en emuladores de teléfono y tablet

---

**Recordá**: SDD no es burocracia. Es la única forma de construir MyDataBases correctamente desde el principio.
