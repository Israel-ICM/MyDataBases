# Feature: SSH Tunneling

## Visión

**SSH Tunneling es CRÍTICO para conectarse a bases de datos en producción.**

La mayoría de bases de datos en producción NO están expuestas directamente a internet. Se accede vía:

- SSH Tunnel (puerto forwarding)
- Bastion host / Jump server
- VPN + SSH

**Sin SSH tunneling, MyDataBases solo sirve para bases de datos locales o de desarrollo.**

---

## ¿Qué es SSH Tunneling?

### Escenario Típico

```
Dispositivo Android (MyDataBases)
    ↓
    ↓ SSH Connection (puerto 22)
    ↓
SSH Server (Bastion Host)
    ↓
    ↓ MySQL Connection (puerto 3306 - solo accesible internamente)
    ↓
MySQL Server (Producción)
```

**Sin SSH Tunnel**: No puedes conectarte (MySQL solo acepta conexiones internas)

**Con SSH Tunnel**: MyDataBases se conecta vía SSH, y el SSH server hace forward al MySQL

---

## Requisitos Funcionales

### 1. Configuración de SSH Tunnel

**Formulario de conexión DEBE incluir**:

```
┌─────────────────────────────────────┐
│ Connection Type:                    │
│  ○ Direct Connection                │
│  ● SSH Tunnel                       │ ← Seleccionado
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│ SSH Configuration                   │
├─────────────────────────────────────┤
│ SSH Host: bastion.example.com       │
│ SSH Port: 22                        │
│ SSH User: ubuntu                    │
│                                     │
│ Authentication Method:              │
│  ● Password                         │
│  ○ Private Key                      │
│  ○ Private Key + Passphrase         │
│                                     │
│ SSH Password: ••••••••              │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│ Database Configuration              │
├─────────────────────────────────────┤
│ Host: localhost (o IP interna)      │
│ Port: 3306                          │
│ Database: production_db             │
│ Username: db_user                   │
│ Password: ••••••••                  │
└─────────────────────────────────────┘
```

### 2. Métodos de Autenticación SSH

#### Opción 1: Password (más simple)

```kotlin
data class SSHPasswordAuth(
    val username: String,
    val password: String
)
```

#### Opción 2: Private Key (más seguro)

```kotlin
data class SSHKeyAuth(
    val username: String,
    val privateKeyPath: String,
    val passphrase: String? = null  // Opcional si la key está encriptada
)
```

**Importar Private Key**:

- Desde archivo (`.pem`, `.ppk`, `id_rsa`)
- Desde clipboard (pegar contenido)
- Generar nueva keypair en la app (futuro)

**Almacenamiento**:

- Private keys DEBEN estar en Android Keystore (encriptadas)
- NUNCA en texto plano

#### Opción 3: SSH Agent (futuro)

- Usar SSH agent del sistema (si está disponible)
- Integración con Termux SSH agent

### 3. Port Forwarding

**Local Port Forwarding** (el más común):

```
MyDataBases establece:
  SSH: android-device:random-port → ssh-server:22
  Forward: localhost:3306 → mysql-server:3306
```

**Configuración**:

```kotlin
data class SSHTunnel(
    val sshHost: String,
    val sshPort: Int = 22,
    val sshAuth: SSHAuth,
    val localPort: Int,        // Puerto local en Android (random)
    val remoteHost: String,    // Host de la DB (ej: localhost, 10.0.1.5)
    val remotePort: Int        // Puerto de la DB (ej: 3306)
)
```

### 4. Conexión con SSH Tunnel

**Flujo**:

1. Usuario configura SSH tunnel en formulario de conexión
2. App guarda configuración (encriptada)
3. Al conectar:
   - Establecer conexión SSH primero
   - Crear port forwarding
   - Conectar a `localhost:{localPort}` (que hace forward a DB remota)
4. Al desconectar:
   - Cerrar conexión de DB
   - Cerrar SSH tunnel

**Estados de conexión**:

```kotlin
sealed class SSHTunnelState {
    object Idle : SSHTunnelState()
    object ConnectingSSH : SSHTunnelState()
    object SSHConnected : SSHTunnelState()
    object ConnectingDB : SSHTunnelState()
    data class Connected(val tunnel: SSHTunnel) : SSHTunnelState()
    data class Error(val message: String, val stage: ErrorStage) : SSHTunnelState()
}

enum class ErrorStage {
    SSH_AUTH_FAILED,
    SSH_HOST_UNREACHABLE,
    SSH_KEY_INVALID,
    PORT_FORWARD_FAILED,
    DB_CONNECTION_FAILED
}
```

### 5. Validación y Testing

**Test SSH Connection** (antes de guardar):

```
┌─────────────────────────────────────┐
│ Testing SSH Connection...           │
├─────────────────────────────────────┤
│ ✅ SSH authentication successful    │
│ ✅ Port forwarding established      │
│ ⏳ Testing database connection...   │
└─────────────────────────────────────┘
```

**Mostrar errores específicos**:

- ❌ SSH authentication failed: Invalid password
- ❌ SSH host unreachable: Check host/port
- ❌ Private key invalid: Check key format
- ❌ Port forwarding failed: Remote port not accessible
- ❌ Database connection failed: Check DB credentials

### 6. Keep-Alive

**SSH connections timeout si no hay actividad.**

**Solución**:

- Enviar keep-alive packets cada 30-60 segundos
- Detectar cuando SSH connection se cae
- Auto-reconectar si es posible
- Notificar al usuario si reconexión falla

```kotlin
class SSHTunnelManager {
    private val keepAliveInterval = 30.seconds
    
    fun startKeepAlive() {
        scope.launch {
            while (isConnected) {
                delay(keepAliveInterval)
                sendKeepAlive()
            }
        }
    }
}
```

---

## Requisitos de UI/UX

### Configuración Visual Clara

**Indicador de conexión**:

```
┌─────────────────────────────────────┐
│ Connection: Production MySQL        │
├─────────────────────────────────────┤
│ 🔒 Via SSH: bastion.example.com     │
│ 🗄️  Database: 10.0.1.5:3306         │
│ ✅ Connected                         │
└─────────────────────────────────────┘
```

**Diferenciación visual**:

- Conexiones directas: 🔓 ícono normal
- Conexiones SSH: 🔒 ícono de candado/túnel
- Color distintivo para conexiones SSH (más seguras)

### Onboarding / Tutorial

**Primera vez que configura SSH**:

```
┌─────────────────────────────────────┐
│ 💡 What is SSH Tunneling?           │
├─────────────────────────────────────┤
│ SSH Tunneling lets you securely     │
│ connect to databases that are not   │
│ directly accessible from internet.  │
│                                     │
│ You'll need:                        │
│ • SSH server credentials            │
│ • Database credentials              │
│                                     │
│ [Learn More] [Got It]               │
└─────────────────────────────────────┘
```

### Importar Private Key (UX)

**Métodos**:

1. **Desde archivo**:
   - Usar SAF (Storage Access Framework)
   - Permitir seleccionar `.pem`, `.ppk`, `id_rsa`

2. **Desde clipboard**:
   - Detectar formato de key en clipboard
   - Botón "Paste from Clipboard"

3. **Escanear QR** (futuro):
   - Generar QR con private key en desktop
   - Escanear desde Android

**Preview de key**:

```
┌─────────────────────────────────────┐
│ Private Key Preview                 │
├─────────────────────────────────────┤
│ Type: RSA 4096-bit                  │
│ Fingerprint: SHA256:abc123...       │
│ Encrypted: Yes (passphrase required)│
│                                     │
│ -----BEGIN RSA PRIVATE KEY-----     │
│ MIIEpAIBAAKCAQEA...                 │
│ (truncated for security)            │
│                                     │
│ [Import] [Cancel]                   │
└─────────────────────────────────────┘
```

---

## Requisitos Técnicos

### Librería SSH

**Opción 1: JSch** (más usado en Android)

```gradle
dependencies {
    implementation "com.jcraft:jsch:0.1.55"
}
```

**Pros**:

- Madura, probada
- Soporte de password y key auth
- Port forwarding built-in

**Cons**:

- Código Java (no Kotlin-first)
- API un poco antigua

**Opción 2: SSHJ**

```gradle
dependencies {
    implementation "com.hierynomus:sshj:0.35.0"
}
```

**Pros**:

- API más moderna
- Mejor soporte de algoritmos
- Kotlin-friendly

**Cons**:

- Menos usado en Android

### Implementación Básica

```kotlin
class SSHTunnelService @Inject constructor(
    private val securityManager: SecurityManager
) {
    suspend fun connect(config: SSHTunnelConfig): Result<SSHTunnel> = withContext(Dispatchers.IO) {
        try {
            val jsch = JSch()
            
            // Configurar autenticación
            when (val auth = config.auth) {
                is SSHPasswordAuth -> {
                    session = jsch.getSession(auth.username, config.host, config.port)
                    session.setPassword(auth.password)
                }
                is SSHKeyAuth -> {
                    val privateKey = securityManager.decryptPrivateKey(auth.privateKeyPath)
                    jsch.addIdentity(auth.username, privateKey, null, auth.passphrase?.toByteArray())
                    session = jsch.getSession(auth.username, config.host, config.port)
                }
            }
            
            // Configuración
            session.setConfig("StrictHostKeyChecking", "no") // O "yes" con known_hosts
            
            // Conectar SSH
            session.connect(timeout = 30000)
            
            // Port forwarding
            val localPort = session.setPortForwardingL(
                0,  // Random local port
                config.remoteHost,
                config.remotePort
            )
            
            Result.success(
                SSHTunnel(
                    session = session,
                    localPort = localPort,
                    remoteHost = config.remoteHost,
                    remotePort = config.remotePort
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun disconnect(tunnel: SSHTunnel) {
        tunnel.session.disconnect()
    }
}
```

### Integración con DatabaseEngine

```kotlin
class MySQLEngine : DatabaseEngine {
    override suspend fun connect(config: ConnectionConfig): Result<Connection> {
        return when (config) {
            is DirectConnection -> connectDirect(config)
            is SSHTunnelConnection -> connectViaSSH(config)
        }
    }
    
    private suspend fun connectViaSSH(config: SSHTunnelConnection): Result<Connection> {
        // 1. Establecer SSH tunnel
        val tunnelResult = sshTunnelService.connect(config.sshConfig)
        if (tunnelResult.isFailure) return Result.failure(tunnelResult.exceptionOrNull()!!)
        
        val tunnel = tunnelResult.getOrThrow()
        
        // 2. Conectar a localhost:{localPort}
        val dbConnection = connectDirect(
            DirectConnection(
                host = "localhost",
                port = tunnel.localPort,
                database = config.database,
                username = config.username,
                password = config.password
            )
        )
        
        return dbConnection
    }
}
```

### Seguridad

**CRÍTICO**:

- ✅ SSH passwords encriptados con Android Keystore
- ✅ Private keys encriptadas con Android Keystore
- ✅ Passphrases encriptadas
- ✅ No loguear passwords/keys NUNCA
- ✅ Limpiar memoria después de usar keys
- ✅ Validar formato de private keys antes de importar
- ✅ Host key verification (conocer fingerprint del servidor)

**Host Key Verification**:

```kotlin
// Primera conexión
┌─────────────────────────────────────┐
│ ⚠️ Unknown SSH Host                 │
├─────────────────────────────────────┤
│ The authenticity of host            │
│ 'bastion.example.com' can't be      │
│ established.                        │
│                                     │
│ RSA key fingerprint:                │
│ SHA256:abc123def456...              │
│                                     │
│ Do you want to continue?            │
│                                     │
│ [Trust Once] [Trust Always] [Cancel]│
└─────────────────────────────────────┘
```

---

## Testing

### Unit Tests

```kotlin
@Test
fun `SSH tunnel establishes connection`() = runTest {
    val config = SSHTunnelConfig(
        host = "test-ssh-server",
        port = 22,
        auth = SSHPasswordAuth("user", "pass"),
        remoteHost = "localhost",
        remotePort = 3306
    )
    
    val result = sshTunnelService.connect(config)
    
    assertTrue(result.isSuccess)
    val tunnel = result.getOrThrow()
    assertNotNull(tunnel.localPort)
}
```

### Integration Tests

```kotlin
@Test
fun `MySQL connection via SSH tunnel works`() = runTest {
    // Requiere Docker con SSH server + MySQL
    val sshConfig = SSHTunnelConfig(...)
    val dbConfig = SSHTunnelConnection(sshConfig, dbCredentials)
    
    val engine = MySQLEngine()
    val connection = engine.connect(dbConfig)
    
    assertTrue(connection.isSuccess)
    
    val result = engine.executeQuery("SELECT 1")
    assertTrue(result.isSuccess)
}
```

---

## Strings Localizados

**Inglés** (`values/strings.xml`):

```xml
<!-- SSH Tunneling -->
<string name="ssh_tunnel_title">SSH Tunnel</string>
<string name="ssh_tunnel_enable">Use SSH Tunnel</string>
<string name="ssh_host">SSH Host</string>
<string name="ssh_port">SSH Port</string>
<string name="ssh_username">SSH Username</string>
<string name="ssh_password">SSH Password</string>
<string name="ssh_private_key">Private Key</string>
<string name="ssh_passphrase">Key Passphrase</string>
<string name="ssh_auth_method">Authentication Method</string>
<string name="ssh_auth_password">Password</string>
<string name="ssh_auth_key">Private Key</string>
<string name="ssh_import_key">Import Private Key</string>
<string name="ssh_test_connection">Test SSH Connection</string>
<string name="ssh_connected">SSH tunnel established</string>
<string name="ssh_error_auth">SSH authentication failed</string>
<string name="ssh_error_host">SSH host unreachable</string>
<string name="ssh_error_key">Invalid private key</string>
```

**Español** (`values-es/strings.xml`):

```xml
<!-- SSH Tunneling -->
<string name="ssh_tunnel_title">Túnel SSH</string>
<string name="ssh_tunnel_enable">Usar Túnel SSH</string>
<string name="ssh_host">Host SSH</string>
<string name="ssh_port">Puerto SSH</string>
<string name="ssh_username">Usuario SSH</string>
<string name="ssh_password">Contraseña SSH</string>
<string name="ssh_private_key">Clave Privada</string>
<string name="ssh_passphrase">Frase de Paso</string>
<string name="ssh_auth_method">Método de Autenticación</string>
<string name="ssh_auth_password">Contraseña</string>
<string name="ssh_auth_key">Clave Privada</string>
<string name="ssh_import_key">Importar Clave Privada</string>
<string name="ssh_test_connection">Probar Conexión SSH</string>
<string name="ssh_connected">Túnel SSH establecido</string>
<string name="ssh_error_auth">Autenticación SSH fallida</string>
<string name="ssh_error_host">Host SSH inalcanzable</string>
<string name="ssh_error_key">Clave privada inválida</string>
```

---

## Roadmap

### v1.0

- ✅ SSH tunnel con password authentication
- ✅ Test SSH connection
- ✅ Indicador visual de conexión SSH
- ✅ Encriptación de credenciales SSH

### v1.2

- ✅ Private key authentication
- ✅ Importar private keys (.pem, .ppk)
- ✅ Passphrase support
- ✅ Host key verification

### v1.3

- ✅ Jump host/Bastion multi-hop (SSH → SSH → DB)
- ✅ Generar keypair en la app
- ✅ Exportar public key

### v2.0

- ✅ SSH agent integration
- ✅ SOCKS proxy support
- ✅ VPN detection y auto-config

---

**CRÍTICO**: Sin SSH tunneling, MyDataBases NO sirve para bases de datos de producción. Este feature es OBLIGATORIO para v1.0 o v1.1 como máximo.
