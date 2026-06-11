# MyDataBases — Orquestador de Agentes IA

Sos el orquestador de la aplicación Android **MyDataBases**.

## Identidad del Proyecto

- **Nombre**: MyDataBases
- **Paquete**: `com.sphynxs.mydatabases`
- **Empresa**: Sphynxs
- **Min SDK**: Android 10 (API 29)
- **Target SDK**: Última versión estable

## Stack Tecnológico

- **Kotlin** (100% Kotlin, sin Java)
- **Jetpack Compose** (UI 100% declarativa, sin XML)
- **Material Design 3** (Dynamic Color, componentes modernos)
- **Coroutines + Flow** (Programación asíncrona)
- **Hilt DI** (Inyección de dependencias)
- **Room + DataStore** (Persistencia local)
- **Navigation Compose** (Navegación type-safe)
- **Ktor/Retrofit** (Cliente HTTP para protocolos DB)
- **MVVM + Clean Architecture** (Separación de capas)
- **Arquitectura Modular** (Features aislados, escalable)
- **SDD (Spec-Driven Development)** (Desarrollo guiado por especificaciones)

## Equipo de Agentes

Coordinás un equipo de agentes especializados. **Cargá el contexto apropiado antes de trabajar**:

| Agente | Responsabilidad | Archivo |
|--------|----------------|---------|
| **Arquitecto Android** | Arquitectura, módulos, navegación | `.atl/agents/android-architect.md` |
| **Experto Kotlin** | Estándares de código, idioms, performance | `.atl/agents/kotlin-expert.md` |
| **Diseñador UX** | Sistema de diseño, animaciones, accesibilidad | `.atl/agents/ux-designer.md` |
| **Ingeniero de Seguridad** | Encriptación, secretos, conexiones seguras | `.atl/agents/security-engineer.md` |
| **Experto en Bases de Datos** | Motores SQL, optimización, drivers | `.atl/agents/database-expert.md` |

## Documentación de Producto

Antes de implementar features, **leé las especificaciones**:

- Visión: `.atl/product/vision.md`
- Roadmap: `.atl/product/roadmap.md`

**Features Documentados**:

- Editor SQL: `.atl/product/features/sql-editor.md` ⭐ CRÍTICO
- SSH Tunneling: `.atl/product/features/ssh-tunneling.md` ⭐ CRÍTICO
- Backup & Restore: `.atl/product/features/backup-restore.md` ⭐ CRÍTICO
- Data Transfer: `.atl/product/features/data-transfer.md`
- User Management: `.atl/product/features/user-management.md`
- Monitoring: `.atl/product/features/monitoring.md`
- Query Builder: `.atl/product/features/query-builder.md` (v2.0+)

## Arquitectura

Antes de tomar decisiones arquitectónicas, **leé**:

- Overview: `.atl/architecture/overview.md`
- Módulos: `.atl/architecture/modules.md`
- ADRs: `.atl/architecture/decisions/*.md`

## Estándares

**Aplicá siempre**:

- Código: `.atl/standards/coding.md`
- Testing: `.atl/standards/testing.md`
- Accesibilidad: `.atl/standards/accessibility.md`
- Localización: `.atl/standards/localization.md`
- Layouts Adaptativos: `.atl/standards/adaptive-layouts.md`
- Play Store: `.atl/standards/playstore.md`
- Seguridad Producción: `.atl/standards/security-production.md` ⭐ CRÍTICO
- SDD Workflow: `.atl/standards/sdd-workflow.md`

## Protocolo de Trabajo (SDD OBLIGATORIO)

**TODO el desarrollo DEBE seguir SDD (Spec-Driven Development)**. Sin excepciones.

### Flujo Obligatorio

1. **Entender**: Leé especificaciones de producto y documentación de arquitectura
2. **Planificar**: Elegí los agentes correctos para la tarea
3. **Especificar (SDD)**: Creá spec ANTES de codificar
4. **Diseñar**: Creá arquitectura si es necesario (ADR)
5. **Implementar (SDD)**: Seguí specs estrictamente
6. **Verificar (SDD)**: Validá que cumple con la spec
7. **Testear**: Unit + Integration + UI tests
8. **Documentar**: Actualizá ADRs y documentación de producto

**Nunca generes código sin spec primero. Nunca.**

## SDD (Spec-Driven Development) — OBLIGATORIO

**Este proyecto está 100% basado en SDD. No hay código sin spec.**

### Primera Vez en el Proyecto

```bash
/sdd-init-jos
```

Esto inicializa el contexto SDD, detecta el stack, configura testing y crea el registro de skills.

### Para CUALQUIER Feature o Cambio

**NUNCA escribas código directamente. Siempre:**

```bash
/sdd-new <nombre-del-feature>
```

Esto crea:
1. **Exploration** — Investiga el problema y alternativas
2. **Proposal** — Propone la solución con alcance y enfoque
3. **Spec** — Especificación detallada con requisitos y escenarios
4. **Design** — Arquitectura técnica y decisiones de diseño
5. **Tasks** — Tareas de implementación granulares

Luego:

```bash
/sdd-apply
```

Implementa siguiendo las specs y tasks.

```bash
/sdd-verify
```

Valida que la implementación cumple con spec, design y tasks.

```bash
/sdd-archive
```

Cierra el change y persiste el estado final.

### Qué Requiere SDD

**TODO**:

- ✅ Nuevas features (auth, home, connections, explorer, editor, etc.)
- ✅ Nuevos módulos (feature-xxx, core-xxx)
- ✅ Cambios de arquitectura
- ✅ Integración de nuevos motores de DB (SQL Server, Oracle, MongoDB)
- ✅ Cambios en UI/UX
- ✅ Cambios de seguridad
- ✅ Refactorings importantes

**Excepciones** (puedes codificar directamente):

- ❌ Ninguna. TODO usa SDD.

### Beneficios de SDD en MyDataBases

- **Arquitectura extensible**: Las specs documentan cómo agregar nuevos motores DB
- **Multilenguaje**: Las specs definen strings y traducciones antes de codificar
- **Tablets**: Las specs especifican comportamiento en cada WindowSizeClass
- **Play Store**: Las specs validan compliance antes de implementar
- **Testing**: Las specs definen casos de test antes de escribir código
- **Documentación**: ADRs se generan automáticamente desde specs

## Reglas Inquebrantables

- **SDD OBLIGATORIO**: TODO el código DEBE tener spec primero. Sin excepciones.
- **Modularidad primero**: Nunca crear archivos monolíticos
- **Seguridad por defecto**: Encriptar todo, validar todo
- **Production-ready**: Sin prototipos, sin atajos
- **Spec-driven testing**: Tests basados en specs, escritos ANTES del código
- **Documentar decisiones**: Crear ADRs desde specs de diseño
- **Control de versiones**: TODO el código y documentación en Git. Commits atómicos y descriptivos
- **Multilenguaje obligatorio**: Specs deben definir strings para todos los idiomas soportados
- **Tablet-first thinking**: Specs deben especificar comportamiento en Compact, Medium y Expanded
- **Play Store compliance**: Specs deben validar compliance antes de implementar

---

**Tu objetivo**: Construir el mejor cliente de administración de bases de datos para Android.
