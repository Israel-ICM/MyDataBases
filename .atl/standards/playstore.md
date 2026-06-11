# Requisitos de Play Store

La aplicación DEBE cumplir con todas las políticas de Google Play Store para publicación.

## Políticas Obligatorias

### 1. Privacidad y Datos de Usuario

**Política de Privacidad**:

- Crear y publicar política de privacidad
- URL de la política debe estar en Play Console
- Explicar qué datos se recopilan y por qué
- Explicar cómo se usan, comparten y protegen los datos

**Datos de Usuario**:

- Solo solicitar permisos necesarios
- Explicar por qué se necesita cada permiso
- Nunca vender datos de usuario
- Permitir exportar/eliminar datos de usuario

**Permisos Requeridos**:

```xml
<!-- Conexiones de red a bases de datos -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Exportar resultados de queries -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />

<!-- Biometric unlock (opcional) -->
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
```

**Justificaciones Requeridas**:

- `INTERNET`: Conexión a servidores de bases de datos remotas
- `ACCESS_NETWORK_STATE`: Verificar conectividad antes de queries
- `WRITE_EXTERNAL_STORAGE`: Exportar resultados de queries (solo Android 9-)
- `USE_BIOMETRIC`: Desbloqueo biométrico opcional

### 2. Seguridad de Datos

**Formulario de Seguridad de Datos** (Play Console):

- Declarar qué datos se recopilan
- Declarar si se comparten con terceros
- Declarar medidas de seguridad (encriptación)

**Para MyDataBases**:

- ✅ Credenciales de conexión (encriptadas localmente)
- ✅ Historial de queries (almacenado localmente)
- ✅ NO se envían datos a servidores de Sphynxs (hasta v1.2)
- ✅ Encriptación con Android Keystore

### 3. Contenido y Metadatos

**Título de la App**:

- Máximo 30 caracteres
- `MyDataBases` (12 caracteres ✅)

**Descripción Corta**:

- Máximo 80 caracteres
- Ejemplo: "Cliente profesional de bases de datos MySQL, PostgreSQL, SQLite y más"

**Descripción Larga**:

- Máximo 4000 caracteres
- Explicar funcionalidades principales
- Listar bases de datos soportadas
- Mencionar casos de uso

**Categoría**:

- `Tools` o `Productivity`

**Clasificación de Contenido**:

- Rellenar cuestionario de clasificación
- Probablemente: PEGI 3, ESRB Everyone

**Screenshots Requeridos**:

- Mínimo 2 screenshots
- Teléfono: 16:9 o 9:16
- Tablet (opcional pero recomendado): 16:9 o 9:16
- Mostrar funcionalidades principales

**Ícono de Aplicación**:

- 512x512 PNG
- Sin transparencias
- Seguir guías de Material Design

### 4. Firma y Seguridad de la App

**App Signing**:

- Usar Play App Signing (Google gestiona la clave)
- Guardar upload key de forma segura
- Nunca compartir keystores

**ProGuard/R8**:

- Ofuscación obligatoria en release
- Mantener reglas de ProGuard actualizadas
- Testear builds ofuscados antes de publicar

**Archivo de Mapeo**:

- Subir mapping.txt a Play Console
- Permite desofuscar stack traces de crashes

### 5. Target API Level

**Requisito de Google**:

- Target SDK: API 34 (Android 14) o superior
- Google requiere targetar última API o API-1

**Para MyDataBases**:

```gradle
android {
    compileSdk = 34
    defaultConfig {
        minSdk = 29        // Android 10
        targetSdk = 34     // Android 14
    }
}
```

**Actualizar Target SDK anualmente** cuando Google lo requiera.

### 6. Pruebas Pre-lanzamiento

**Pre-launch Report**:

- Google ejecuta tests automáticos
- Revisar crashes antes de publicar
- Corregir errores críticos

**Pruebas Internas**:

- Crear track de prueba interna
- Testear con usuarios reales antes de producción
- Usar closed testing o open testing

### 7. Política de Familias (NO aplica para MyDataBases)

MyDataBases NO es una app para niños → NO participar en programa Diseñado para Familias.

### 8. Declaración de Anuncios

**Para MyDataBases**:

- ❌ NO mostrar anuncios (al menos en v1.0)
- Si en el futuro hay ads, declarar en Play Console
- Cumplir con políticas de ads de Google

### 9. Facturación In-App (Futuro)

Si hay features premium (v1.2+):

- Usar Google Play Billing Library
- Declarar compras in-app en Play Console
- Permitir cancelación de suscripciones
- Política de reembolsos clara

### 10. Actualizaciones y Soporte

**Ciclo de Actualizaciones**:

- Corregir bugs críticos en <7 días
- Nuevas features según roadmap
- Mantener app actualizada con última Target API

**Soporte al Usuario**:

- Email de contacto en Play Console
- Responder reviews (especialmente negativos)
- Política de soporte clara

## Checklist Pre-Publicación

Antes de subir a Play Store:

- [ ] Política de privacidad escrita y publicada
- [ ] Formulario de seguridad de datos completado
- [ ] Screenshots de calidad (teléfono + tablet)
- [ ] Ícono 512x512 listo
- [ ] Título y descripciones escritas (todos los idiomas soportados)
- [ ] ProGuard/R8 habilitado en release
- [ ] Firma con Play App Signing configurada
- [ ] Target SDK = última versión requerida por Google
- [ ] Tests pasando (unit + integration + UI)
- [ ] App testeada en múltiples dispositivos
- [ ] Clasificación de contenido completada
- [ ] Permisos justificados en descripción
- [ ] Sin crashes en Pre-launch Report

## Monitoreo Post-Publicación

Después de publicar:

- Monitorear crashes en Play Console
- Revisar reviews y responder
- Analizar métricas de uso
- Corregir bugs reportados
- Actualizar según feedback

---

**Recursos**:

- [Políticas de Google Play](https://play.google.com/about/developer-content-policy/)
- [Requisitos de Target API](https://support.google.com/googleplay/android-developer/answer/11926878)
- [Seguridad de Datos](https://support.google.com/googleplay/android-developer/answer/10787469)
