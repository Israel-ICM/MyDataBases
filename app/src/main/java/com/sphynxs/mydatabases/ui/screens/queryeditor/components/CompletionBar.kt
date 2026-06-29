package com.sphynxs.mydatabases.ui.screens.queryeditor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.domain.completion.CompletionKind
import com.sphynxs.mydatabases.domain.completion.CompletionSuggestion

/**
 * Completion bar for mobile mode (soft keyboard visible).
 *
 * Renders completion suggestions as horizontal chips above the keyboard:
 * - Fully rounded chips (RoundedCornerShape(50%))
 * - Horizontal scroll (LazyRow)
 * - Tap-to-accept
 * - Selected chip highlighted
 *
 * Design: Mobile-first completion UX - anchored bottom, no keyboard nav needed
 * Spec: openspec/changes/editor-completion-and-format/spec.md (mobile variant)
 *
 * @param suggestions List of suggestions to render (top 20 from provider)
 * @param selectedIndex Currently selected suggestion (0-based, for highlighting)
 * @param onSuggestionClick Invoked when user taps a suggestion chip
 * @param modifier Modifier for the container
 *
 * @author israel-icm
 * @date 2026-06-29
 */
@Composable
fun CompletionBar(
    suggestions: List<CompletionSuggestion>,
    selectedIndex: Int,
    onSuggestionClick: (CompletionSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Code completion bar" },
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        if (suggestions.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.completion_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            itemsIndexed(suggestions) { index, suggestion ->
                val isSelected = index == selectedIndex
                
                // Format display text: "column : TYPE" for columns, plain text otherwise
                val displayText = if (suggestion.kind == CompletionKind.COLUMN && suggestion.detail != null) {
                    "${suggestion.text} : ${suggestion.detail}"
                } else {
                    suggestion.text
                }
                
                Surface(
                    modifier = Modifier
                        .clickable { onSuggestionClick(suggestion) }
                        .semantics { contentDescription = "Suggestion: $displayText" },
                    shape = RoundedCornerShape(50), // Fully rounded
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = displayText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        }
    }
}
