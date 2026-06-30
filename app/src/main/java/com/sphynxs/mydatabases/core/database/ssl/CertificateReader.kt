package com.sphynxs.mydatabases.core.database.ssl

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Utilidad para leer y procesar certificados SSL desde URIs de Android.
 *
 * Los certificados seleccionados mediante el file picker están en content:// URIs.
 * Para usarlos con JDBC, necesitamos copiarlos a archivos temporales accesibles.
 *
 * @author israel-icm
 * @date 2026-06-30
 */
object CertificateReader {
    
    /**
     * Lee un certificado desde una URI y lo copia a un archivo temporal.
     *
     * @param context Contexto de Android para acceder al ContentResolver
     * @param uri URI del certificado (content://, file://, etc.)
     * @param fileName Nombre del archivo temporal (ej: "ca-cert.pem")
     * @return File temporal con el contenido del certificado
     * @throws Exception si no se puede leer o copiar el archivo
     */
    fun readCertificateToTempFile(
        context: Context,
        uri: Uri,
        fileName: String
    ): File {
        val tempDir = File(context.cacheDir, "ssl-certs")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        
        val tempFile = File(tempDir, fileName)
        
        // Leer desde URI y copiar a archivo temporal
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw Exception("No se pudo abrir el certificado desde URI: $uri")
        
        return tempFile
    }
    
    /**
     * Lee el contenido de un certificado como String.
     *
     * @param context Contexto de Android
     * @param uri URI del certificado
     * @return Contenido del certificado como String
     */
    fun readCertificateContent(
        context: Context,
        uri: Uri
    ): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        } ?: throw Exception("No se pudo leer el certificado desde URI: $uri")
    }
    
    /**
     * Valida que un archivo sea un certificado PEM válido.
     *
     * @param content Contenido del archivo
     * @return true si parece ser un certificado PEM válido
     */
    fun isValidPemCertificate(content: String): Boolean {
        return content.contains("-----BEGIN CERTIFICATE-----") &&
               content.contains("-----END CERTIFICATE-----")
    }
    
    /**
     * Valida que un archivo sea una clave privada PEM válida.
     *
     * @param content Contenido del archivo
     * @return true si parece ser una clave privada PEM válida
     */
    fun isValidPemPrivateKey(content: String): Boolean {
        return (content.contains("-----BEGIN PRIVATE KEY-----") &&
                content.contains("-----END PRIVATE KEY-----")) ||
               (content.contains("-----BEGIN RSA PRIVATE KEY-----") &&
                content.contains("-----END RSA PRIVATE KEY-----"))
    }
    
    /**
     * Limpia archivos temporales de certificados antiguos.
     *
     * @param context Contexto de Android
     */
    fun cleanupTempCertificates(context: Context) {
        val tempDir = File(context.cacheDir, "ssl-certs")
        if (tempDir.exists()) {
            tempDir.listFiles()?.forEach { file ->
                // Eliminar archivos más antiguos que 1 hora
                if (System.currentTimeMillis() - file.lastModified() > 3600_000) {
                    file.delete()
                }
            }
        }
    }
}
