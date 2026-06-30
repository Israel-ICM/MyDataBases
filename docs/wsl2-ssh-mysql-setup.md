# Setup SSH + MySQL en WSL2 para Probar SSH Tunneling

## Requisitos
- WSL2 con Ubuntu instalado
- Windows 11

---

## 1. Configurar SSH Server en WSL2

Abrí una terminal WSL (Ubuntu) y ejecutá:

```bash
# Instalar SSH server
sudo apt update
sudo apt install openssh-server -y

# Iniciar SSH server
sudo service ssh start

# Verificar que está corriendo
sudo service ssh status
# Debería decir: "sshd is running"

# Crear usuario de prueba para SSH
sudo adduser sshtest
# Password: test123 (o el que quieras)

# Obtener IP de WSL2
ip addr show eth0 | grep "inet\s" | awk '{print $2}' | cut -d/ -f1
# Ejemplo: 172.25.160.1
# GUARDÁ ESTA IP - la vas a usar en la app
```

---

## 2. Configurar MySQL en WSL2

```bash
# Instalar MySQL
sudo apt install mysql-server -y

# Iniciar MySQL
sudo service mysql start

# Configurar MySQL (crear usuario y base de datos de prueba)
sudo mysql -u root << EOF
CREATE DATABASE testdb;
CREATE USER 'dbuser'@'localhost' IDENTIFIED BY 'dbpass123';
GRANT ALL PRIVILEGES ON testdb.* TO 'dbuser'@'localhost';
FLUSH PRIVILEGES;
EOF

# Verificar que funciona
mysql -u dbuser -pdbpass123 -e "SHOW DATABASES;"
# Debería mostrar: testdb
```

---

## 3. Configurar Windows Firewall (si es necesario)

WSL2 normalmente es accesible desde Windows sin configuración extra, pero si tenés problemas:

```powershell
# En PowerShell (como Administrador)
New-NetFirewallRule -DisplayName "WSL2 SSH" -Direction Inbound -LocalPort 22 -Protocol TCP -Action Allow
```

---

## 4. Probar Conexión SSH desde Windows

Abrí PowerShell en Windows y probá:

```powershell
# Reemplazá 172.25.160.1 con la IP que obtuviste antes
ssh sshtest@172.25.160.1
# Password: test123

# Si conecta correctamente, salí con:
exit
```

---

## 5. Datos para Configurar en la App Android

### Conexión MySQL Normal (sin SSH)
- **Host**: 172.25.160.1 (IP de WSL2)
- **Port**: 3306
- **Database**: testdb
- **Username**: dbuser
- **Password**: dbpass123

### Configuración SSH Tunnel
- **SSH Host**: 172.25.160.1 (IP de WSL2)
- **SSH Port**: 22
- **SSH Username**: sshtest
- **SSH Password**: test123
- **Authentication**: Password

### Conexión MySQL a través de SSH Tunnel
- **Host**: localhost (o 127.0.0.1) ← IMPORTANTE: después del túnel, MySQL está en "localhost" del túnel
- **Port**: 3306
- **Database**: testdb
- **Username**: dbuser
- **Password**: dbpass123
- **SSH Tunnel**: ✅ Enabled (con los datos de arriba)

---

## 6. En el Emulador de Android

**IMPORTANTE**: Desde el emulador Android, para acceder a la máquina Windows (y por ende a WSL2), usá:

```
10.0.2.2
```

Entonces los datos quedan:

### SSH Tunnel Config en el Emulador:
- **SSH Host**: 10.0.2.2 (apunta a la máquina Windows/WSL2)
- **SSH Port**: 22
- **SSH Username**: sshtest
- **SSH Password**: test123

### MySQL Config (post-tunnel):
- **Host**: localhost
- **Port**: 3306
- **Database**: testdb
- **Username**: dbuser
- **Password**: dbpass123

---

## 7. Troubleshooting

### SSH no conecta desde el emulador
```bash
# En WSL2, verificar que SSH está escuchando en todas las interfaces
sudo nano /etc/ssh/sshd_config

# Buscar la línea:
ListenAddress 0.0.0.0

# Si no existe, agregarla. Luego reiniciar SSH:
sudo service ssh restart
```

### MySQL no conecta a través del túnel
```bash
# Verificar que MySQL está corriendo
sudo service mysql status

# Verificar que MySQL escucha en localhost
sudo netstat -tlnp | grep 3306
# Debería mostrar: 127.0.0.1:3306
```

### Olvidé la IP de WSL2
```bash
# En WSL2
ip addr show eth0 | grep "inet\s" | awk '{print $2}' | cut -d/ -f1
```

### WSL2 se reinició y cambió la IP
Cada vez que reiniciás Windows, WSL2 puede cambiar de IP. Ejecutá el comando de arriba para obtener la nueva IP.

---

## 8. Verificación Final

### Desde Windows (PowerShell):

```powershell
# Test SSH
ssh sshtest@<IP_WSL2>

# Test SSH tunnel manual (para verificar que funciona)
ssh -L 13306:localhost:3306 sshtest@<IP_WSL2>
# Password: test123
# Dejá esta ventana abierta

# En otra ventana PowerShell, probá conectar a MySQL a través del túnel:
mysql -h 127.0.0.1 -P 13306 -u dbuser -pdbpass123
# Debería conectar exitosamente
```

---

## Resumen de Comandos Rápidos

```bash
# Iniciar servicios en WSL2 (cada vez que reiniciás Windows)
sudo service ssh start
sudo service mysql start

# Ver IP de WSL2
hostname -I | awk '{print $1}'

# Ver logs de SSH (si algo falla)
sudo tail -f /var/log/auth.log
```

---

## Siguiente Paso

Una vez que tengas SSH y MySQL corriendo en WSL2:

1. Ejecutá la app MyDataBases en el emulador
2. Creá una nueva conexión con los datos de arriba
3. Habilitá SSH Tunnel
4. Tap "Test Connection"
5. Debería conectar exitosamente ✅
