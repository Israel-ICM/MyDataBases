package com.sphynxs.mydatabases.domain.sql

import java.io.Reader

/**
 * Guard liviano que determina si un archivo SQL supera el umbral de líneas soportado
 * por el editor visual (Compose `TextFieldValue`), sin cargar el archivo completo en
 * memoria y sin llamar `readText()`.
 *
 * Cuenta saltos de línea (`\n`) con salida temprana: se detiene apenas confirma que
 * el archivo supera el umbral, sin seguir leyendo el resto del stream.
 *
 * @author sdd-apply
 * @date 2026-08-04
 */
object LineThresholdGuard {

    private const val LINE_THRESHOLD = 50_000
    private const val READ_BUFFER_SIZE = 8_192

    /**
     * True si [reader] contiene estrictamente más de [LINE_THRESHOLD] líneas.
     *
     * Umbral fijo v1 (no configurable vía Settings): protege contra una limitación de
     * renderizado de texto de Compose, no es una preferencia de usuario. La comparación
     * es estrictamente mayor-que — un archivo con exactamente [LINE_THRESHOLD] líneas
     * NO excede el umbral.
     */
    fun exceedsThreshold(reader: Reader): Boolean {
        val buffer = CharArray(READ_BUFFER_SIZE)
        var newlineCount = 0

        while (true) {
            val read = reader.read(buffer)
            if (read == -1) return false

            for (i in 0 until read) {
                if (buffer[i] == '\n') {
                    newlineCount++
                    if (newlineCount > LINE_THRESHOLD) return true
                }
            }
        }
    }
}
