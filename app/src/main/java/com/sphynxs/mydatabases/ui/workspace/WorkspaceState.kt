package com.sphynxs.mydatabases.ui.workspace

/**
 * Workspace state — enum que representa el estado visual del workspace overlay.
 *
 * El workspace tiene 3 estados posibles:
 * - Collapsed: Minimizado, no visible — usuario enfocado en la pantalla principal
 * - Peek: Peek mode — muestra las primeras 3 cards + FAB "+" para abrir nuevas
 * - Expanded: Full sheet — HorizontalPager con todas las cards navegables
 *
 * Transiciones:
 * - Collapsed → Peek: Usuario hace click en una tabla/query
 * - Peek → Expanded: Usuario swipe-up en el peek handle
 * - Expanded → Peek: Usuario swipe-down en el ModalBottomSheet
 * - Peek → Collapsed: Usuario cierra todas las cards
 *
 * @author israel-icm
 * @date 2026-06-15
 */
enum class WorkspaceState {
    /**
     * Workspace no visible — usuario en la pantalla principal.
     */
    Collapsed,

    /**
     * Peek mode — muestra preview de las primeras 3 cards + FAB para abrir más.
     */
    Peek,

    /**
     * Full sheet — HorizontalPager con todas las cards navegables.
     */
    Expanded
}
