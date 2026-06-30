# Verificar SSH y MySQL en WSL2

## 1. Verificar que SSH y MySQL están corriendo

Abrí una terminal WSL (Ubuntu) y ejecutá:

```bash
# Verificar SSH
sudo service ssh status
# Debería decir: "sshd is running"

# Si no está corriendo:
sudo service ssh start

# Verificar MySQL
sudo service mysql status
# Debería decir: "mysql is running"

# Si no está corriendo:
sudo service mysql start
```

---

## 2. Verificar que podés conectarte por SSH desde Windows

Abrí PowerShell en Windows:

```powershell
# Probar SSH (reemplazá la IP si es necesario)
ssh sshtest@10.0.2.2
# O probá con la IP de WSL2:
# hostname -I   (en WSL2 para ver la IP)
```

Si te pide password, poné: `test123`

Si conecta exitosamente, **el SSH está bien configurado** ✅

Salí con: `exit`

---

## 3. Verificar MySQL desde dentro del SSH

Desde la conexión SSH (o directamente en WSL2):

```bash
# Conectarte a MySQL
mysql -u dbuser -pdbpass123 -h localhost testdb

# Si conecta, ejecutá:
SHOW TABLES;

# Salir:
exit
```

Si conecta exitosamente, **MySQL está bien configurado** ✅

---

## 4. Probar túnel SSH manualmente desde Windows

Abrí PowerShell y probá un túnel manual:

```powershell
# Establecer túnel SSH manual
# Formato: ssh -L [puerto_local]:localhost:[puerto_mysql] usuario@ssh_host
ssh -L 13306:localhost:3306 sshtest@10.0.2.2
# Password: test123
```

**Dejá esta ventana abierta** (el túnel está activo mientras está abierta).

En **otra ventana PowerShell**, probá conectarte a MySQL a través del túnel:

```powershell
# Si tenés MySQL client instalado:
mysql -h 127.0.0.1 -P 13306 -u dbuser -pdbpass123

# O probá con telnet para verificar que el puerto responde:
Test-NetConnection -ComputerName 127.0.0.1 -Port 13306
```

Si el túnel funciona, deberías poder conectarte ✅

---

## 5. Ver logs en la app Android

Cuando la app intente conectarse, abrí **Logcat** en Android Studio y filtrá por:

```
SSHTunnelManager
```

Deberías ver logs como:

```
D/SSHTunnelManager: Establishing SSH tunnel to 10.0.2.2:22 for forwarding to localhost:3306
D/SSHTunnelManager: SSH session connected successfully
D/SSHTunnelManager: Port forwarding established on localhost:XXXXX
D/SSHTunnelManager: SSH tunnel established: localhost:XXXXX → localhost:3306
```

Si ves algún error, copialo completo.

---

## Errores Comunes

### "Connection refused" en SSH

**Causa**: SSH server no está corriendo o el puerto 22 está bloqueado.

**Solución**:
```bash
# En WSL2
sudo service ssh start
sudo service ssh status
```

### "Connection refused" en MySQL después del túnel

**Causa**: MySQL no está corriendo o no escucha en localhost.

**Solución**:
```bash
# En WSL2
sudo service mysql start
sudo service mysql status

# Verificar que MySQL escucha en localhost:3306
sudo netstat -tlnp | grep 3306
# Debería mostrar: 127.0.0.1:3306
```

### "Authentication failed"

**Causa**: Usuario o password incorrectos.

**Solución**:
- SSH: verificar que `sshtest` con password `test123` existe
- MySQL: verificar que `dbuser` con password `dbpass123` existe

```bash
# Verificar usuario SSH
cat /etc/passwd | grep sshtest

# Verificar usuario MySQL
mysql -u root -e "SELECT user, host FROM mysql.user WHERE user='dbuser';"
```

---

## Siguiente Paso

Ejecutá estos pasos y decime:
1. ¿SSH funciona desde PowerShell?
2. ¿MySQL funciona desde WSL2?
3. ¿El túnel manual funciona desde PowerShell?
4. ¿Qué dice el Logcat cuando intentás conectar desde la app?
