package com.sphynxs.mydatabases.core.database.ssl

import android.content.Context
import android.net.Uri
import com.sphynxs.mydatabases.core.database.models.SSLConfig
import com.sphynxs.mydatabases.core.database.models.SSLMode
import java.util.Properties

/**
 * Constructor de propiedades SSL para conexiones MySQL/MariaDB.
 *
 * Convierte la configuración SSL de alto nivel a propiedades JDBC específicas
 * del driver MySQL Connector/J 5.1.46 (el único compatible con Android).
 *
 * @author israel-icm
 * @date 2026-06-30
 */
class MySQLSSLConfigBuilder(
    private val context: Context,
    private val sslConfig: SSLConfig?
) {
    
    /**
     * Aplica la configuración SSL a las propiedades JDBC.
     *
     * @param props Properties donde se agregarán las configuraciones SSL
     */
    fun applyToProperties(props: Properties) {
        if (sslConfig == null) {
            props["useSSL"] = "false"
            return
        }
        
        when (sslConfig.mode) {
            SSLMode.DISABLED -> {
                props["useSSL"] = "false"
            }
            
            SSLMode.PREFERRED -> {
                props["useSSL"] = "true"
                props["requireSSL"] = "false"
                // Intenta SSL pero permite fallback
            }
            
            SSLMode.REQUIRED -> {
                props["useSSL"] = "true"
                props["requireSSL"] = "true"
                props["verifyServerCertificate"] = "false"
                // SSL obligatorio pero sin verificar certificado
            }
            
            SSLMode.VERIFY_CA -> {
                props["useSSL"] = "true"
                props["requireSSL"] = "true"
                props["verifyServerCertificate"] = "true"
                
                // Configurar truststore con CA certificate
                configureTrustStore(props)
            }
            
            SSLMode.VERIFY_IDENTITY -> {
                props["useSSL"] = "true"
                props["requireSSL"] = "true"
                props["verifyServerCertificate"] = "true"
                
                // Verificar CA + hostname
                configureTrustStore(props)
            }
        }
        
        // Configurar cliente mTLS si hay certificados de cliente
        if (sslConfig.isMutualTLS) {
            configureClientCertificate(props)
        }
    }
    
    /**
     * Configura el truststore con el certificado CA.
     */
    private fun configureTrustStore(props: Properties) {
        sslConfig?.caCertificateUri?.let { uriString ->
            try {
                val uri = Uri.parse(uriString)
                val caCertFile = CertificateReader.readCertificateToTempFile(
                    context = context,
                    uri = uri,
                    fileName = "ca-cert.pem"
                )
                
                // Validar que sea un certificado PEM válido
                val content = caCertFile.readText()
                if (!CertificateReader.isValidPemCertificate(content)) {
                    throw IllegalArgumentException("El archivo CA no es un certificado PEM válido")
                }
                
                // MySQL Connector/J 5.1.46 usa estas propiedades para SSL
                props["trustCertificateKeyStoreUrl"] = "file:${caCertFile.absolutePath}"
                props["trustCertificateKeyStoreType"] = "PEM"
                
            } catch (e: Exception) {
                throw Exception("Error al cargar certificado CA: ${e.message}", e)
            }
        }
    }
    
    /**
     * Configura el certificado y clave privada del cliente para mTLS.
     */
    private fun configureClientCertificate(props: Properties) {
        val clientCertUri = sslConfig?.clientCertificateUri
        val clientKeyUri = sslConfig?.clientKeyUri
        
        if (clientCertUri != null && clientKeyUri != null) {
            try {
                // Leer certificado de cliente
                val certFile = CertificateReader.readCertificateToTempFile(
                    context = context,
                    uri = Uri.parse(clientCertUri),
                    fileName = "client-cert.pem"
                )
                
                // Leer clave privada de cliente
                val keyFile = CertificateReader.readCertificateToTempFile(
                    context = context,
                    uri = Uri.parse(clientKeyUri),
                    fileName = "client-key.pem"
                )
                
                // Validar archivos
                val certContent = certFile.readText()
                val keyContent = keyFile.readText()
                
                if (!CertificateReader.isValidPemCertificate(certContent)) {
                    throw IllegalArgumentException("El certificado de cliente no es válido")
                }
                
                if (!CertificateReader.isValidPemPrivateKey(keyContent)) {
                    throw IllegalArgumentException("La clave privada de cliente no es válida")
                }
                
                // Configurar keystore para cliente
                props["clientCertificateKeyStoreUrl"] = "file:${certFile.absolutePath}"
                props["clientCertificateKeyStoreType"] = "PEM"
                props["clientCertificateKeyStorePassword"] = "" // PEM files sin password
                
            } catch (e: Exception) {
                throw Exception("Error al cargar certificados de cliente: ${e.message}", e)
            }
        }
    }
    
    /**
     * Limpia archivos temporales de certificados después de establecer la conexión.
     */
    fun cleanup() {
        CertificateReader.cleanupTempCertificates(context)
    }
}
