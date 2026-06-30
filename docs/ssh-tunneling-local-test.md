# SSH Tunneling - Guía de Verificación Local

## Opción 1: Verificación Sin Servidor (Rápida)

### 1. Compilar el Proyecto

En Android Studio:
```
1. File → Sync Project with Gradle Files
2. Build → Make Project (Ctrl+F9)
3. Verificar que no hay errores de compilación
```

### 2. Ejecutar Unit Tests

En Android Studio:
```
1. Click derecho en: app/src/test/java/com/.../ssh/
2. Run 'Tests in ssh'
3. Verificar que los 48 tests pasan
```

O desde terminal:
```bash
./gradlew test
```

---

## Opción 2: Verificación con Servidor SSH Real

### Setup con Docker (Recomendado)

**1. Crear servidor SSH de prueba:**

Crear archivo `docker-compose-ssh-test.yml`:

```yaml
version: '3.8'

services:
  # Servidor SSH (bastion)
  ssh-server:
    image: linuxserver/openssh-server:latest
    container_name: ssh-bastion
    environment:
      - PUID=1000
      - PGID=1000
      - TZ=America/Argentina/Buenos_Aires
      - PASSWORD_ACCESS=true
      - USER_PASSWORD=testpass123
      - USER_NAME=sshuser
    ports:
      - "2222:2222"
    volumes:
      - ./ssh-keys:/config/.ssh
    networks:
      - test-network

  # MySQL en red interna (solo accesible via SSH tunnel)
  mysql-internal:
    image: mysql:8.0
    container_name: mysql-internal
    environment:
      MYSQL_ROOT_PASSWORD: rootpass123
      MYSQL_DATABASE: testdb
      MYSQL_USER: dbuser
      MYSQL_PASSWORD: dbpass123
    ports:
      - "3307:3306"  # Expuesto para debugging, pero simula red interna
    networks:
      - test-network

networks:
  test-network:
    driver: bridge
```

**2. Levantar el entorno:**

```bash
# En D:\AndroidStudioProjects\MyDataBases\
docker-compose -f docker-compose-ssh-test.yml up -d
```

**3. Verificar que funciona:**

```bash
# Probar SSH
ssh -p 2222 sshuser@localhost
# Password: testpass123

# Probar MySQL directo (para comparar)
mysql -h localhost -P 3307 -u dbuser -p
# Password: dbpass123
```

---

## Opción 3: Verificación en la App (End-to-End)

### 1. Ejecutar la App en Emulador

```
1. Abrí el proyecto en Android Studio
2. Run → Run 'app' (Shift+F10)
3. Esperá que cargue el emulador
```

### 2. Crear Conexión con SSH Tunnel

**En la app:**
```
1. Tap en "New Connection"
2. Completar datos básicos:
   - Name: "Test SSH Tunnel"
   - Type: MySQL
   - Host: 10.0.2.2 (host machine desde emulador)
   - Port: 3307
   - Username: dbuser
   - Password: dbpass123

3. Expandir "Advanced Connection"

4. Expandir "SSH Tunnel"
   - Toggle: Enable SSH tunnel ✅
   - SSH Host: 10.0.2.2
   - SSH Port: 2222
   - SSH Username: sshuser
   - Authentication Method: Password
   - SSH Password: testpass123

5. Aceptar el security warning

6. Tap "Test" → debería conectar exitosamente ✅

7. Tap "Save" → debería guardar la config ✅
```

### 3. Verificar que el Tunnel se Establece

**Logs en Logcat (Android Studio):**
```
Filtrar por "SSHTunnelManager"

✅ Ver: "SSH session connected successfully"
✅ Ver: "Port forwarding established on localhost:XXXXX"
✅ Ver: "SSH tunnel established: localhost:XXXXX → 10.0.2.2:3307"
```

---

## Opción 4: Verificación Manual sin Docker

### Si tenés un servidor SSH real accesible:

1. **Servidor SSH existente** (puede ser tu PC con OpenSSH)
2. **MySQL en red interna** (o simular con MySQL local)

**Configurar SSH en tu PC (Windows):**

```powershell
# Instalar OpenSSH Server (Windows 10/11)
Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0

# Iniciar servicio
Start-Service sshd

# Configurar para inicio automático
Set-Service -Name sshd -StartupType 'Automatic'

# Crear usuario SSH de prueba
net user sshuser testpass123 /add
```

**En la app usar:**
- SSH Host: 10.0.2.2 (tu PC desde emulador)
- SSH Port: 22
- SSH Username: sshuser
- SSH Password: testpass123

---

## Verificaciones Específicas

### ✅ Verificar que SSH Password se Encripta

**En Logcat buscar:**
```
ConnectionRepositoryImpl
```

**NO deberías ver** el password en plaintext. Deberías ver algo como:
```
Saved connection with encrypted password: enc_abc123...
```

### ✅ Verificar Cleanup del Tunnel

**Al desconectar, en Logcat buscar:**
```
SSHTunnelManager

✅ Ver: "Port forwarding removed for localhost:XXXXX"
✅ Ver: "SSH session disconnected"
```

### ✅ Verificar Security Warning

**Primera vez que habilites SSH tunnel:**
- Debe aparecer dialog: "SECURITY WARNING: All SSH hosts will be accepted..."
- Botones: "I understand, continue" / "Cancel"
- Si cancelas → toggle SSH debe volver a OFF

---

## Troubleshooting

### Error: "SSH connection timed out"
- Verificar que el puerto SSH esté accesible desde el emulador
- Desde emulador: usar `10.0.2.2` en vez de `localhost`
- Verificar firewall de Windows

### Error: "SSH authentication failed"
- Verificar username/password correctos
- Verificar que el usuario existe en el servidor SSH

### Error: "Cannot allocate local port"
- Cerrar otras conexiones activas
- Reiniciar la app

### Emulador no puede acceder a localhost
- Usar `10.0.2.2` para referirse a la máquina host
- Ver: https://developer.android.com/studio/run/emulator-networking

---

## Resumen de Verificación Completa

- [x] 1. Compilar proyecto sin errores
- [x] 2. Ejecutar 48 unit tests (todos pasan)
- [ ] 3. Levantar entorno Docker (opcional)
- [ ] 4. Ejecutar app en emulador
- [ ] 5. Crear conexión con SSH tunnel
- [ ] 6. Verificar logs (tunnel established)
- [ ] 7. Test connection exitoso
- [ ] 8. Guardar y reload → config persiste
- [ ] 9. Verificar cleanup al desconectar

---

## Archivos Importantes para Debugging

- `SSHTunnelManager.kt` → Log tag: `SSHTunnelManager`
- `MySQLConnectionPool.kt` → Log tag: `MySQLConnectionPool`
- `ConnectionRepositoryImpl.kt` → Log tag: `ConnectionRepositoryImpl`

**Filtro Logcat recomendado:**
```
tag:SSHTunnelManager | tag:MySQLConnectionPool
```
