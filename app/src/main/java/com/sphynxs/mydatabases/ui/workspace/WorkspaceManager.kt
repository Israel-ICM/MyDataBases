package com.sphynxs.mydatabases.ui.workspace

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Workspace Manager — singleton que gestiona el estado del workspace multi-tab.
 *
 * El workspace permite abrir múltiples "cards" (tablas, queries, editores, etc.)
 * y navegar entre ellas con un HorizontalPager estilo IDE.
 *
 * Responsabilidades:
 * - Mantener la lista de cards abiertas
 * - Gestionar el índice de la card activa (selected tab)
 * - Controlar el estado visual del workspace (Collapsed/Peek/Expanded)
 * - Prevenir duplicados (si se abre una card que ya existe, se navega a ella)
 *
 * Flows expuestos:
 * - cards: Lista de cards abiertas (inmutable StateFlow)
 * - activeIndex: Índice de la card actualmente visible
 * - state: Estado visual del workspace (Collapsed/Peek/Expanded)
 *
 * Uso:
 * ```kotlin
 * @Inject lateinit var workspaceManager: WorkspaceManager
 *
 * // Abrir una tabla
 * workspaceManager.openCard(
 *     WorkspaceCard.Table(
 *         id = "table:conn1:mydb:users",
 *         title = "users",
 *         connectionId = "conn1",
 *         databaseName = "mydb",
 *         tableName = "users"
 *     )
 * )
 *
 * // Cerrar la card activa
 * workspaceManager.closeCard(workspaceManager.activeIndex.value)
 *
 * // Expandir el workspace
 * workspaceManager.expand()
 * ```
 *
 * @author israel-icm
 * @date 2026-06-15
 */
@Singleton
class WorkspaceManager @Inject constructor() {
    private val _cards = MutableStateFlow<List<WorkspaceCard>>(emptyList())
    val cards: StateFlow<List<WorkspaceCard>> = _cards.asStateFlow()

    private val _activeIndex = MutableStateFlow(0)
    val activeIndex: StateFlow<Int> = _activeIndex.asStateFlow()

    private val _state = MutableStateFlow(WorkspaceState.Collapsed)
    val state: StateFlow<WorkspaceState> = _state.asStateFlow()

    /**
     * Abre una card en el workspace.
     *
     * Si la card ya existe (mismo id), navega a ella en lugar de crear duplicado.
     * Si es nueva, la agrega al final y la selecciona.
     * Automáticamente expande el workspace a Expanded.
     *
     * @param card La card a abrir
     */
    fun openCard(card: WorkspaceCard) {
        val existing = _cards.value.indexOfFirst { it.id == card.id }
        if (existing >= 0) {
            // Card ya existe — navegar a ella
            _activeIndex.value = existing
        } else {
            // Card nueva — agregar al final
            _cards.value = _cards.value + card
            _activeIndex.value = _cards.value.size - 1
        }
        _state.value = WorkspaceState.Expanded
    }

    /**
     * Cierra una card por índice.
     *
     * Si se cierra la última card, colapsa el workspace.
     * Si el índice activo queda fuera de rango, lo ajusta al último válido.
     *
     * @param index Índice de la card a cerrar
     */
    fun closeCard(index: Int) {
        _cards.value = _cards.value.filterIndexed { i, _ -> i != index }
        if (_cards.value.isEmpty()) {
            _state.value = WorkspaceState.Collapsed
        } else if (_activeIndex.value >= _cards.value.size) {
            _activeIndex.value = _cards.value.size - 1
        }
    }

    /**
     * Cambia la card activa (tab selected).
     *
     * @param index Índice de la nueva card activa
     */
    fun setActiveIndex(index: Int) {
        _activeIndex.value = index
    }

    /**
     * Expande el workspace a full sheet (HorizontalPager).
     */
    fun expand() {
        _state.value = WorkspaceState.Expanded
    }

    /**
     * Cambia el workspace a peek mode (preview de 3 cards + FAB).
     */
    fun peek() {
        _state.value = WorkspaceState.Peek
    }

    /**
     * Colapsa el workspace (no visible).
     */
    fun collapse() {
        _state.value = WorkspaceState.Collapsed
    }
}
