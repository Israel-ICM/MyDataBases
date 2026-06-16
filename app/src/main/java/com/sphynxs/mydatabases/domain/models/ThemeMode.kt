package com.sphynxs.mydatabases.domain.models

/**
 * Modos de tema disponibles para la aplicación.
 *
 * Permite al usuario elegir entre tema claro, oscuro o seguir el sistema.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
enum class ThemeMode {
    /**
     * Tema claro siempre, sin importar la configuración del sistema.
     */
    LIGHT,
    
    /**
     * Tema oscuro siempre, sin importar la configuración del sistema.
     */
    DARK,
    
    /**
     * Sigue la configuración de tema del sistema operativo.
     */
    SYSTEM
}
