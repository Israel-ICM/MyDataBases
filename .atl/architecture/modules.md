# Estructura de Módulos

## Módulo App

```
app/
├── di/                  # Configuración Hilt de aplicación
├── navigation/          # Grafos de navegación
└── MyDataBasesApp.kt    # Clase Application
```

## Módulos de Features

Cada feature es un módulo auto-contenido:

```
feature-xxx/
├── presentation/
│   ├── screens/         # Pantallas Composable
│   ├── components/      # Componentes específicos del feature
│   └── viewmodels/      # ViewModels
├── domain/
│   ├── usecases/        # Lógica de negocio
│   ├── models/          # Entidades de dominio
│   └── repositories/    # Interfaces de repositorio
└── data/
    ├── repositories/    # Implementaciones de repositorio
    ├── sources/         # Fuentes de datos Remote/Local
    └── mappers/         # Mappers DTO ↔ Entity
```

### Lista de Features

- `feature-auth` — Autenticación y onboarding
- `feature-home` — Pantalla principal y grid de proveedores
- `feature-connections` — Gestión de conexiones
- `feature-explorer` — Navegación de árbol de base de datos
- `feature-editor` — Editor SQL
- `feature-designer` — Diseñador de esquemas (v2.0+)
- `feature-settings` — Configuración de la app
- `feature-profile` — Perfil de usuario

## Módulos Core

Compartidos entre features:

- `core-ui` — Componentes Compose reutilizables (100% Jetpack Compose)
- `core-network` — Clientes HTTP, interfaces API
- `core-security` — Encriptación, keystore, secretos
- `core-database` — Room, drivers DB, parsers SQL, **motor extensible**
- `core-designsystem` — Tema, colores, tipografía Material 3
- `core-common` — Extensions, utils, constantes

### Módulo `core-database` (Extensible)

Estructura interna para soportar múltiples motores:

```
core-database/
├── engine/
│   ├── DatabaseEngine.kt           # Interface común
│   ├── DatabaseEngineFactory.kt    # Factory para crear motores
│   ├── mysql/
│   │   └── MySQLEngine.kt
│   ├── postgresql/
│   │   └── PostgreSQLEngine.kt
│   ├── sqlite/
│   │   └── SQLiteEngine.kt
│   ├── mariadb/
│   │   └── MariaDBEngine.kt
│   ├── rds/
│   │   └── RDSEngine.kt
│   └── aurora/
│       └── AuroraEngine.kt
│   # Futuro:
│   # ├── sqlserver/
│   # ├── oracle/
│   # └── mongodb/
├── models/
│   ├── Connection.kt
│   ├── QueryResult.kt
│   └── Table.kt
└── parser/
    └── SQLParser.kt
```

**Agregar un nuevo motor**:

1. Crear carpeta `core-database/engine/{nuevo-motor}/`
2. Implementar `DatabaseEngine` interface
3. Registrar en `DatabaseEngineFactory`
4. Listo — el resto de la app funciona sin cambios

## Dependencias de Módulos

```
app
 ├─> feature-*
 ├─> core-*

feature-*
 ├─> core-ui
 ├─> core-designsystem
 ├─> core-common
 └─> core-* (según necesidad)

core-ui
 └─> core-designsystem
```

**Reglas**:

- Features NUNCA dependen de otros features
- Features solo dependen de módulos core
- Módulos core pueden depender de otros módulos core (cuidadosamente)

---

*La creación de módulos ocurre incrementalmente a medida que se construyen features.*
