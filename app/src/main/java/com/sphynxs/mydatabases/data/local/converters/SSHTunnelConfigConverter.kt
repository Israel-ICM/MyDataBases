package com.sphynxs.mydatabases.data.local.converters

import androidx.room.TypeConverter
import com.sphynxs.mydatabases.core.database.models.SSHTunnelConfig
import org.json.JSONObject

/**
 * Converter para persistir [SSHTunnelConfig] en Room como JSON.
 *
 * Como SSHTunnelConfig es un objeto complejo, lo serializamos a JSON
 * para guardarlo en una columna de tipo String usando JSONObject nativo de Android.
 *
 * @author israel-icm
 * @date 2026-06-12
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
            put("host", config.host)
            put("port", config.port)
            put("username", config.username)
            config.password?.let { put("password", it) }
            config.privateKeyPath?.let { put("privateKeyPath", it) }
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
                host = json.getString("host"),
                port = json.getInt("port"),
                username = json.getString("username"),
                password = if (json.has("password")) json.getString("password") else null,
                privateKeyPath = if (json.has("privateKeyPath")) json.getString("privateKeyPath") else null
            )
        } catch (e: Exception) {
            null
        }
    }
}
