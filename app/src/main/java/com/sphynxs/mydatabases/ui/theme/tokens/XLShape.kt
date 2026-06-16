package com.sphynxs.mydatabases.ui.theme.tokens

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Extra Large shape token — corner radius 24dp.
 *
 * Usado para componentes hero destacados (HeroConnectionCard, grandes containers).
 * Más redondeado que `shapes.large` (20dp), transmite diseño premium.
 *
 * @author israel-icm
 * @date 2026-06-15
 */
object XLShape {
    /**
     * Corner radius para extra large shape (24dp).
     */
    val cornerRadius = 24.dp

    /**
     * Shape con corner radius 24dp en todas las esquinas.
     */
    val shape: CornerBasedShape = RoundedCornerShape(cornerRadius)
}
