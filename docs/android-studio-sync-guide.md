# Cómo Sincronizar y Compilar en Android Studio

## 1️⃣ Sync Project with Gradle Files

### Opción A: Menú File
```
1. Click en "File" (esquina superior izquierda)
2. Buscá "Sync Project with Gradle Files"
   (generalmente está en la mitad del menú)
```

### Opción B: Toolbar (más rápido)
```
1. Mirá la barra de herramientas superior
2. Buscá el ícono del elefante (Gradle) con flechas circulares 🔄
3. Tooltip dice: "Sync Project with Gradle Files"
4. Click en ese ícono
```

### Opción C: Atajo de Teclado
```
Ctrl + Shift + A  (abre el buscador de acciones)
Escribí: "sync"
Seleccioná: "Sync Project with Gradle Files"
```

### Opción D: Banner Automático
```
Si acabás de abrir el proyecto, Android Studio generalmente muestra
un banner amarillo arriba que dice:

"Gradle files have changed since last project sync..."
[Sync Now]  <-- Click acá
```

---

## 2️⃣ Make Project (Compilar)

### Opción A: Menú Build
```
1. Click en "Build" (en la barra de menú superior)
2. Seleccioná "Make Project"
```

### Opción B: Atajo de Teclado (RECOMENDADO)
```
Ctrl + F9
```

### Opción C: Toolbar
```
Buscá el ícono del martillo 🔨 en la toolbar superior
Click en el martillo
```

---

## 3️⃣ Ejecutar Tests

### Encontrar los Tests

```
Panel izquierdo → Vista "Project" o "Android"

Si está en "Android":
  app
    └── java
        └── com.sphynxs.mydatabases (test)  <-- versión gris/verde
            └── core
                └── database
                    └── ssh
                        ├── SSHKeyReaderTest
                        ├── SSHTunnelManagerTest
                        └── ...

Si está en "Project":
  app
    └── src
        └── test
            └── java
                └── com
                    └── sphynxs
                        └── mydatabases
                            └── core
                                └── database
                                    └── ssh
                                        ├── SSHKeyReaderTest.kt
                                        ├── SSHTunnelManagerTest.kt
                                        └── ...
```

### Ejecutar Tests

```
Opción A - Carpeta completa:
  1. Click DERECHO en la carpeta "ssh"
  2. Menú contextual → "Run 'Tests in 'ssh''"
  
Opción B - Archivo individual:
  1. Abrí SSHKeyReaderTest.kt
  2. Mirá al lado del número de línea donde dice "class SSHKeyReaderTest"
  3. Hay un ícono verde ▶ (play) al lado
  4. Click en ese ícono → "Run 'SSHKeyReaderTest'"

Opción C - Método individual:
  1. Dentro del archivo, cada @Test tiene su propio ícono ▶
  2. Click en el ícono al lado de cualquier función @Test
  3. Ejecuta solo ese test
```

---

## 4️⃣ Ver Resultados

### Build Output
```
Panel inferior → pestaña "Build"

✅ Éxito:
BUILD SUCCESSFUL in Xs

❌ Error:
BUILD FAILED in Xs
e: /path/to/file.kt: (línea, columna): Error message
```

### Test Results
```
Panel inferior → pestaña "Run"

✅ Tests passed:
╔══════════════════════════════════╗
║ SSHKeyReaderTest         ✓ 16/16 ║
║ SSHTunnelManagerTest     ✓ 13/13 ║
║ ...                               ║
╚══════════════════════════════════╝

❌ Tests failed:
Muestra en rojo cuál falló y el stack trace
```

---

## 🆘 Si No Encuentras las Opciones

### Verificar que el Proyecto Está Abierto Correctamente

```
1. File → Open
2. Navegá a: D:\AndroidStudioProjects\MyDataBases
3. Seleccioná la CARPETA (no un archivo)
4. Click "OK"
```

### Verificar que la Vista es Correcta

```
Panel izquierdo, arriba, hay un dropdown que puede decir:
- "Android" (vista simplificada) ← RECOMENDADO para desarrollo
- "Project" (vista completa del filesystem)
- "Packages"
- etc.

Cambiá a "Android" si no lo está.
```

### Reconstruir Índices (si todo falla)

```
File → Invalidate Caches...
[Invalidate and Restart]

Esto reinicia Android Studio y reconstruye todo.
ADVERTENCIA: Tarda unos minutos.
```

---

## ✅ Checklist Rápido

- [ ] Android Studio abierto con el proyecto MyDataBases
- [ ] Panel izquierdo muestra la estructura del proyecto
- [ ] Menú superior tiene: File, Edit, View, Navigate, Code, Analyze, Refactor, Build, Run, Tools
- [ ] Toolbar superior tiene íconos: Sync (elefante 🔄), Build (martillo 🔨), Run (play ▶)

Si ves todo eso → estás en el lugar correcto ✅
