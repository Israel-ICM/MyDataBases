# Resumen de Arquitectura

## Estilo Arquitectónico

**Clean Architecture** con **Diseño Modular**.

## Principios Core

1. **Independencia**: Lógica de negocio independiente de UI, frameworks y bases de datos
2. **Testabilidad**: Cada capa puede ser testeada en aislamiento
3. **Flexibilidad**: Fácil intercambiar implementaciones
4. **Escalabilidad**: Agregar features sin tocar código existente

## Diagrama de Capas

```
┌─────────────────────────────────────────┐
│          Capa Presentation              │
│   (UI, ViewModels, Pantallas Compose)   │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│            Capa Domain                  │
│  (Casos de Uso, Entidades, Repositorios)│
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│             Capa Data                   │
│  (Impls Repositorio, Fuentes, DTOs)     │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│          Sistemas Externos              │
│   (Bases de Datos, APIs, File System)   │
└─────────────────────────────────────────┘
```

## Regla de Dependencias

**Las capas internas nunca dependen de las externas.**

- `domain` → sin dependencias
- `data` → depende de `domain`
- `presentation` → depende de `domain`

## Organización de Módulos

Ver `.atl/architecture/modules.md` para estructura detallada de módulos.

---

*Decisiones arquitectónicas detalladas están documentadas en `.atl/architecture/decisions/`.*
