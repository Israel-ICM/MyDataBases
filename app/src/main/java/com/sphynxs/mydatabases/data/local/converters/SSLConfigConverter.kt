package com.sphynxs.mydatabases.data.local.converters

import androidx.room.TypeConverter
import com.sphynxs.mydatabases.core.database.models.SSLConfig
import com.sphynxs.mydatabases.core.database.models.SSLMode
import org.json.JSONObject

/**
 * Converter para persistir [SSLConfig] en Room como JSON.
 *
 * Como SSLConfig es un objeto complejo con certificados URIs, lo serializamos a JSON
 * para guardarlo en una columna de tipo String.
 *
 * **Storage**: Certificate URIs are stored as-is (content:// URIs from Android SAF).
 * The actual certificate files are managed by Android's permission system.
 *
 * @author israel-icm
 * @date 2026-06-30
 */
class SSLConfigConverter {

    /**
     * Convierte un [SSLConfig] a JSON string.
     *
     * @param config El objeto a convertir (null si no hay config SSL)
     * @return El JSON como string o null
     */
    @TypeConverter
    fun fromSSLConfig(config: SSLConfig?): String? {
        if (config == null) return null
        
        return JSONObject().apply {
            put("mode", config.mode.name)
            config.caCertificateUri?.let { put("caCertificateUri", it) }
            config.clientCertificateUri?.let { put("clientCertificateUri", it) }
            config.clientKeyUri?.let { put("clientKeyUri", it) }
        }.toString()
    }

    /**
     * Convierte un JSON string de vuelta a [SSLConfig].
     *
     * @param value El JSON guardado en la DB
     * @return El objeto deserializado o null
     */
    @TypeConverter
    fun toSSLConfig(value: String?): SSLConfig? {
        if (value == null) return null
        
        return try {
            val json = JSONObject(value)
            SSLConfig(
                mode = SSLMode.valueOf(json.getString("mode")),
                caCertificateUri = if (json.has("caCertificateUri")) json.getString("caCertificateUri") else null,
                clientCertificateUri = if (json.has("clientCertificateUri")) json.getString("clientCertificateUri") else null,
                clientKeyUri = if (json.has("clientKeyUri")) json.getString("clientKeyUri") else null
            )
        } catch (e: Exception) {
            null
        }
    }
}
