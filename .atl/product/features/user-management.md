# Feature: Gestión de Usuarios y Permisos

## Visión

Administrar usuarios, roles y permisos de la base de datos desde la app.

---

## Funcionalidades

### Ver Usuarios

Lista de usuarios con:
- Username
- Host
- Grants/Permisos
- Estado (activo/bloqueado)

### Crear Usuario

```sql
CREATE USER 'newuser'@'%' IDENTIFIED BY 'password';
GRANT SELECT, INSERT ON database.* TO 'newuser'@'%';
```

UI:
- Username
- Host (%, localhost, IP específica)
- Password
- Permisos (checkboxes): SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, etc.

### Editar Permisos

```sql
GRANT ALL PRIVILEGES ON *.* TO 'admin'@'%';
REVOKE DELETE ON database.* FROM 'readonly'@'%';
```

### Eliminar Usuario

```sql
DROP USER 'olduser'@'%';
```

---

## Soporte por Motor

- **MySQL/MariaDB**: GRANT, REVOKE, CREATE USER
- **PostgreSQL**: CREATE ROLE, GRANT, REVOKE
- **SQLite**: No soporta usuarios (es archivo local)

---

## Roadmap

- v1.4: Ver usuarios
- v1.4: Crear/editar/eliminar usuarios
- v2.0: Gestión avanzada de roles
