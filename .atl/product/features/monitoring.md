# Feature: Monitoreo y Performance

## Visión

Monitorear el estado de la base de datos, queries activas y performance.

---

## Funcionalidades

### 1. Queries Activas (Process List)

**MySQL**: `SHOW PROCESSLIST`

**PostgreSQL**: `SELECT * FROM pg_stat_activity`

**Mostrar**:
- ID del proceso
- Usuario
- Host
- Database
- Query ejecutándose
- Tiempo de ejecución
- Estado (Running, Sleeping, Locked)

**Acciones**:
- Ver query completa
- Matar query (`KILL [process_id]`)

### 2. Estadísticas de la DB

**Información**:
- Tamaño total de la DB
- Tamaño por tabla
- Número de tablas/views/procedures
- Queries lentas (slow query log)
- Conexiones activas
- Uptime del servidor

### 3. Server Status

**MySQL**: `SHOW STATUS`

**Variables importantes**:
- Connections
- Questions (queries ejecutadas)
- Uptime
- Threads_connected
- Slow_queries

### 4. Storage / Espacio

**Por tabla**:
- Data size
- Index size
- Total size
- Rows count

**Gráfico de distribución de espacio**

### 5. Query Performance

**Execution Plans**:
- `EXPLAIN` query
- Índices usados
- Costo estimado
- Sugerencias de optimización

---

## UI

```
┌─────────────────────────────────────┐
│ Monitoring                          │
├─────────────────────────────────────┤
│ 📊 Database: production_db          │
│                                     │
│ Server Status:                      │
│  • Uptime: 15 days 6 hours          │
│  • Connections: 45 / 100            │
│  • Queries/sec: 234                 │
│  • Slow queries: 12                 │
│                                     │
│ Storage:                            │
│  • Total size: 2.3 GB               │
│  • Largest table: orders (1.5 GB)   │
│  • Free space: 45 GB                │
│                                     │
│ Active Queries: 5                   │
│  [View Process List]                │
│                                     │
│ Performance:                        │
│  [View Slow Queries]                │
│  [Analyze Query Performance]        │
└─────────────────────────────────────┘
```

---

## Roadmap

- v1.4: Process list, matar queries
- v1.4: Estadísticas básicas
- v2.0: Gráficos de performance en tiempo real
- v2.0: Alertas (queries lentas, conexiones altas)
