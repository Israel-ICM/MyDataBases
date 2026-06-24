package com.sphynxs.mydatabases.ui.screens.queryeditor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.sphynxs.mydatabases.domain.completion.CompletionKind
import com.sphynxs.mydatabases.domain.completion.CompletionSuggestion

/**
 * Completion popup anchored at cursor position.
 *
 * Renders completion suggestions in a dropdown with:
 * - Max 8 visible rows (scrollable for more)
 * - Selected row highlighted
 * - Tap-to-accept
 * - Keyboard navigation support (handled by parent)
 *
 * Design: ADR 4 — Popup component, Compose anchoring
 * Spec: openspec/changes/editor-completion-and-format/spec.md (scenarios 13-28)
 *
 * @param suggestions List of suggestions to render (top 20 from provider)
 * @param selectedIndex Currently selected suggestion (0-based, for keyboard navigation)
 * @param anchorOffset Cursor position from TextLayoutResult.getBoundingBox()
 * @param onSuggestionClick Invoked when user taps a suggestion row
 * @param onDismiss Invoked when user dismisses popup (Esc, outside click)
 *
 * @author israel-icm
 * @date 2026-06-24
 */
@Composable
fun CompletionPopup(
    suggestions: List<CompletionSuggestion>,
    selectedIndex: Int,
    anchorOffset: IntOffset,
    onSuggestionClick: (CompletionSuggestion) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) {
        return
    }

    Popup(
        offset = anchorOffset,
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = modifier
                .width(280.dp)
                .heightIn(max = 320.dp) // ~8 rows × 40dp
                .shadow(8.dp, RoundedCornerShape(8.dp))
                .semantics { contentDescription = "Code completion popup" },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            val listState = rememberLazyListState()

            // Auto-scroll to selected item
            LaunchedEffect(selectedIndex) {
                if (selectedIndex in suggestions.indices) {
                    listState.animateScrollToItem(selectedIndex)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.padding(4.dp)
            ) {
                itemsIndexed(suggestions) { index, suggestion ->
                    CompletionRow(
                        suggestion = suggestion,
                        isSelected = index == selectedIndex,
                        onClick = { onSuggestionClick(suggestion) }
                    )
                }
            }
        }
    }
}

/**
 * Single completion row showing suggestion text, kind badge, and optional detail.
 */
@Composable
private fun CompletionRow(
    suggestion: CompletionSuggestion,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics { contentDescription = "${suggestion.text} - ${suggestion.kind.name.lowercase()}" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Kind badge (K/T/C)
        Text(
            text = when (suggestion.kind) {
                CompletionKind.KEYWORD -> "K"
                CompletionKind.TABLE -> "T"
                CompletionKind.COLUMN -> "C"
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = when (suggestion.kind) {
                CompletionKind.KEYWORD -> MaterialTheme.colorScheme.primary
                CompletionKind.TABLE -> MaterialTheme.colorScheme.tertiary
                CompletionKind.COLUMN -> MaterialTheme.colorScheme.secondary
            },
            modifier = Modifier
                .padding(end = 8.dp)
                .width(16.dp)
        )

        // Suggestion text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.text,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            // Optional detail (column type)
            if (suggestion.detail != null) {
                Text(
                    text = suggestion.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}
