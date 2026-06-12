package com.sphynxs.mydatabases.core.database.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Configuración de túnel SSH para conexiones remotas.
 *
 * Permite conectarse a bases de datos a través de un servidor SSH intermediario.
 * Esta funcionalidad será implementada en un change separado.
 *
 * @property host Host del servidor SSH
 * @property port Puerto del servidor SSH (default: 22)
 * @property username Usuario SSH
 * @property password Contraseña SSH (opcional si se usa clave)
 * @property privateKeyPath Ruta a la clave privada SSH (opcional)
 * @author israel-icm
 * @date 2026-06-11
 */
@Parcelize
data class SSHTunnelConfig(
    val host: String,
    val port: Int = 22,
    val username: String,
    val password: String? = null,
    val privateKeyPath: String? = null
) : Parcelable
