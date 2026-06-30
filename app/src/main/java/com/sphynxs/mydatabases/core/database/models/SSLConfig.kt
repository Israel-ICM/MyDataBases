package com.sphynxs.mydatabases.core.database.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Modo de verificación SSL.
 *
 * Define el nivel de verificación de certificados SSL/TLS.
 *
 * @author israel-icm
 * @date 2026-06-30
 */
enum class SSLMode {
    /** SSL deshabilitado - conexión sin cifrar */
    DISABLED,
    
    /** SSL preferido - intenta SSL, fallback a conexión sin cifrar */
    PREFERRED,
    
    /** SSL requerido - conexión cifrada obligatoria, pero sin verificar certificado */
    REQUIRED,
    
    /** Verificar CA - verifica que el certificado esté firmado por una CA confiable */
    VERIFY_CA,
    
    /** Verificar identidad - verifica CA + que el hostname del servidor coincida */
    VERIFY_IDENTITY
}

/**
 * Configuración SSL/TLS para conexiones a bases de datos.
 *
 * Contiene todos los parámetros necesarios para establecer conexiones seguras
 * con verificación de certificados y autenticación mutua (mTLS).
 *
 * @property mode Modo de verificación SSL
 * @property caCertificateUri URI del certificado CA (para verificar servidor)
 * @property clientCertificateUri URI del certificado de cliente (mTLS opcional)
 * @property clientKeyUri URI de la clave privada del cliente (mTLS opcional)
 * 
 * @author israel-icm
 * @date 2026-06-30
 */
@Parcelize
data class SSLConfig(
    val mode: SSLMode = SSLMode.REQUIRED,
    val caCertificateUri: String? = null,
    val clientCertificateUri: String? = null,
    val clientKeyUri: String? = null
) : Parcelable {
    
    /**
     * Indica si se debe usar autenticación mutua (mTLS).
     * 
     * Requiere certificado de cliente Y clave privada.
     */
    val isMutualTLS: Boolean
        get() = clientCertificateUri != null && clientKeyUri != null
    
    /**
     * Indica si se debe verificar el certificado del servidor.
     */
    val shouldVerifyCertificate: Boolean
        get() = mode == SSLMode.VERIFY_CA || mode == SSLMode.VERIFY_IDENTITY
}
