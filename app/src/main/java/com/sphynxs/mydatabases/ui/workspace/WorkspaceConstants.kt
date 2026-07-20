package com.sphynxs.mydatabases.ui.workspace

import androidx.compose.ui.unit.dp

/**
 * Constantes compartidas del workspace.
 */
object WorkspaceConstants {
    /**
     * Espacio reservado en la parte inferior del topsheet para la toolbar flotante.
     * Este valor se resta de la altura del topsheet (tanto base como frame) para crear
     * espacio visual entre el topsheet y la toolbar.
     */
    val TOOLBAR_SPACING = 16.dp

    /**
     * Altura visible del "peek" (tira colapsada) del TopSheet/TopSheetFrame.
     *
     * Reducido de 60dp a 40dp para que la tira colapsada ocupe menos espacio de
     * pantalla (feedback del usuario: "no ocupe tanto espacio").
     */
    val PEEK_HEIGHT = 40.dp

    /**
     * Buffer "muerto" entre la barra de estado y donde arranca la zona interactiva
     * (drag) del peek colapsado.
     *
     * Sin este buffer, el gesto de "arrastrar para expandir" arranca prácticamente
     * pegado al borde superior de la pantalla — la misma franja que Android reserva
     * para el swipe-down que revela la bandeja de notificaciones, generando
     * confusión entre ambos gestos (feedback del usuario). Empuja TODO el peek
     * (fondo + íconos) hacia abajo esa distancia extra, sin afectar el estado
     * expandido (offset 0 sin cambios).
     */
    val TOP_GESTURE_BUFFER = 12.dp
}
