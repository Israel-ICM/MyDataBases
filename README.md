# MyDataBases

Cliente profesional de administración de bases de datos para Android.

## 📱 Acerca del Proyecto

MyDataBases es un cliente SQL profesional para Android inspirado en Navicat, construido 100% con Jetpack Compose y siguiendo Clean Architecture.

### Stack Tecnológico

- **Kotlin** 100%
- **Jetpack Compose** (UI declarativa)
- **Material Design 3** (Dynamic Color)
- **Hilt** (Dependency Injection)
- **Room** (Persistencia local)
- **Coroutines + Flow** (Programación asíncrona)
- **Navigation Compose** (Navegación)
- **MVVM + Clean Architecture**

### Motores Soportados

#### v1.0 (Actual)
- MySQL
- MariaDB

#### Próximas Versiones
- PostgreSQL (v1.1)
- SQLite (v1.1)
- Amazon RDS/Aurora (v1.4)
- SQL Server, Oracle, MongoDB (v3.1+)

## 🏗️ Arquitectura

El proyecto sigue **Clean Architecture** con módulos separados por features:

```
app/                          # Punto de entrada
feature-auth/                 # Autenticación
feature-home/                 # Home y provider grid
feature-connections/          # Gestión de conexiones
feature-explorer/             # Explorador de DB
feature-editor/               # Editor SQL
core-database/                # Motores de DB extensibles
core-ui/                      # Componentes compartidos
core-designsystem/            # Tema Material 3
```

## 🚀 Comenzar

### Requisitos

- Android Studio Hedgehog | 2023.1.1 o superior
- JDK 17
- Android SDK 36
- Gradle 8.2+

### Configuración

1. Clonar el repositorio:
```bash
git clone <repository-url>
cd MyDataBases
```

2. Abrir en Android Studio

3. Sincronizar Gradle

4. Ejecutar en emulador o dispositivo (API 29+)

## 📖 Documentación

Toda la documentación del proyecto está en `.atl/`:

- **Agentes IA**: `.atl/agents/` - Arquitectura, Kotlin, UX, Security, Database
- **Producto**: `.atl/product/` - Visión, roadmap, features
- **Arquitectura**: `.atl/architecture/` - Overview, módulos, ADRs
- **Estándares**: `.atl/standards/` - Código, testing, accesibilidad, etc.

## 🧪 Testing

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

## 📋 Desarrollo

Este proyecto usa **SDD (Spec-Driven Development)**. Ver `.atl/standards/sdd-workflow.md` para el workflow completo.

### Comandos SDD

```bash
/sdd-init-jos              # Inicializar SDD
/sdd-new <feature>         # Nueva feature
/sdd-apply                 # Implementar
/sdd-verify                # Verificar
```

## 🌐 Multilenguaje

Soportado desde v1.0:
- 🇪🇸 Español
- 🇺🇸 Inglés
- 🇨🇳 简体中文 (Chino Simplificado)
- 🇮🇳 हिन्दी (Hindi)
- 🇫🇷 Français (Francés)
- 🇩🇪 Deutsch (Alemán)
- 🇧🇷 Português (Portugués)
- 🇯🇵 日本語 (Japonés)
- 🇸🇦 العربية (Árabe)
- 🇷🇺 Русский (Ruso)

## 📱 Soporte de Dispositivos

- ✅ Teléfonos (Compact)
- ✅ Tablets (Medium/Expanded)
- ✅ Foldables
- ✅ Orientación portrait y landscape

## 📄 Licencia

Ver archivo [LICENSE](LICENSE)

## 👥 Contribuir

Este proyecto sigue estándares estrictos de calidad. Consulta `.atl/standards/` antes de contribuir.

---

**Empresa**: Sphynxs  
**Versión**: 1.0.0  
**Min SDK**: 29 (Android 10)  
**Target SDK**: 36 (Android 16)
