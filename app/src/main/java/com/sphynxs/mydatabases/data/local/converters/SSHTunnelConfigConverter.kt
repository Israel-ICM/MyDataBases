package com.sphynxs.mydatabases.data.local.converters

import androidx.room.TypeConverter
import com.sphynxs.mydatabases.core.database.models.SSHAuthMethod
import com.sphynxs.mydatabases.core.database.models.SSHTunnelConfig
import org.json.JSONObject

/**
 * Converter para persistir [SSHTunnelConfig] en Room como JSON.
 *
 * Como SSHTunnelConfig es un objeto complejo, lo serializamos a JSON
 * para guardarlo en una columna de tipo String usando JSONObject nativo de Android.
 *
 * **Security**: SSH passwords are NOT encrypted in this converter - they're already
 * encrypted before being passed here (handled by repository layer).
 *
 * @author israel-icm
 * @date 2026-06-12 (updated 2026-06-30 for full SSH support)
 */
class SSHTunnelConfigConverter {

    /**
     * Convierte un [SSHTunnelConfig] a JSON string.
     *
     * @param config El objeto a convertir (null si no hay túnel SSH)
     * @return El JSON como string o null
     */
    @TypeConverter
    fun fromSSHTunnelConfig(config: SSHTunnelConfig?): String? {
        if (config == null) return null
        
        return JSONObject().apply {
            put("enabled", config.enabled)
            put("host", config.host)
            put("port", config.port)
            put("username", config.username)
            put("authMethod", config.authMethod.name)
            put("password", config.password)  // Already encrypted by repository
            config.privateKeyUri?.let { put("privateKeyUri", it) }
        }.toString()
    }

    /**
     * Convierte un JSON string de vuelta a [SSHTunnelConfig].
     *
     * @param value El JSON guardado en la DB
     * @return El objeto deserializado o null
     */
    @TypeConverter
    fun toSSHTunnelConfig(value: String?): SSHTunnelConfig? {
        if (value == null) return null
        
        return try {
            val json = JSONObject(value)
            SSHTunnelConfig(
                enabled = json.optBoolean("enabled", false),
                host = json.getString("host"),
                port = json.getInt("port"),
                username = json.getString("username"),
                authMethod = SSHAuthMethod.valueOf(
                    json.optString("authMethod", SSHAuthMethod.PASSWORD.name)
                ),
                password = json.getString("password"),  // Will be decrypted by repository
                privateKeyUri = if (json.has("privateKeyUri")) json.getString("privateKeyUri") else null
            )
        } catch (e: Exception) {
            null
        }
    }
}
