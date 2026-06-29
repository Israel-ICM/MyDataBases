package com.sphynxs.mydatabases.ui.screens.queryeditor.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.ui.screens.queryeditor.FindReplaceMode

/**
 * Find & Replace inline bar (FR-1, FR-9).
 *
 * Phase 5.3 — Composable UI for search/replace.
 *
 * Displays above the editor with:
 * - Find input with match counter (FR-2)
 * - Replace input (visible in REPLACE mode only)
 * - Toggle buttons: Match case, Whole word, Regex (FR-6, FR-7, FR-8)
 * - Navigation buttons: Previous, Next (FR-4)
 * - Replace buttons: Replace one, Replace all (FR-10, FR-11)
 * - Close button (FR-5: Esc handled by parent)
 *
 * @param mode Current mode (FIND or REPLACE)
 * @param findQuery Current find query
 * @param replaceText Current replace text
 * @param currentMatchIndex 0-based index of current match
 * @param totalMatches Total number of matches
 * @param matchCase Match case toggle state
 * @param wholeWord Whole word toggle state
 * @param useRegex Regex mode toggle state
 * @param onFindQueryChange Callback when find query changes
 * @param onReplaceTextChange Callback when replace text changes
 * @param onToggleMatchCase Toggle match case
 * @param onToggleWholeWord Toggle whole word
 * @param onToggleUseRegex Toggle regex mode
 * @param onNavigateNext Navigate to next match
 * @param onNavigatePrevious Navigate to previous match
 * @param onReplaceOne Replace current match
 * @param onReplaceAll Replace all matches
 * @param onClose Close the find bar
 *
 * @author israel-icm (SDD apply phase)
 * @date 2026-06-29
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindReplaceBar(
    mode: FindReplaceMode,
    findQuery: String,
    replaceText: String,
    currentMatchIndex: Int,
    totalMatches: Int,
    matchCase: Boolean,
    wholeWord: Boolean,
    useRegex: Boolean,
    onFindQueryChange: (String) -> Unit,
    onReplaceTextChange: (String) -> Unit,
    onToggleMatchCase: () -> Unit,
    onToggleWholeWord: () -> Unit,
    onToggleUseRegex: () -> Unit,
    onNavigateNext: () -> Unit,
    onNavigatePrevious: () -> Unit,
    onReplaceOne: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // First row: Find input + match counter + toggles + navigation + close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Find input
                OutlinedTextField(
                    value = findQuery,
                    onValueChange = onFindQueryChange,
                    label = { Text(stringResource(R.string.find_label)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                // Match counter (FR-2)
                if (totalMatches > 0) {
                    Text(
                        text = "${currentMatchIndex + 1} / $totalMatches",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (findQuery.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.find_no_matches),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Navigation buttons (FR-4)
                IconButton(
                    onClick = onNavigatePrevious,
                    enabled = totalMatches > 0
                ) {
                    Text("↑")
                }

                IconButton(
                    onClick = onNavigateNext,
                    enabled = totalMatches > 0
                ) {
                    Text("↓")
                }

                // Close button
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.find_close))
                }
            }

            // Second row: Toggles (FR-6, FR-7, FR-8)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = matchCase,
                    onClick = onToggleMatchCase,
                    label = { Text(stringResource(R.string.find_match_case)) }
                )

                FilterChip(
                    selected = wholeWord,
                    onClick = onToggleWholeWord,
                    label = { Text(stringResource(R.string.find_whole_word)) }
                )

                FilterChip(
                    selected = useRegex,
                    onClick = onToggleUseRegex,
                    label = { Text(stringResource(R.string.find_regex)) }
                )
            }

            // Third row: Replace input + buttons (only in REPLACE mode)
            if (mode == FindReplaceMode.REPLACE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Replace input
                    OutlinedTextField(
                        value = replaceText,
                        onValueChange = onReplaceTextChange,
                        label = { Text(stringResource(R.string.replace_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    // Replace one button (FR-10)
                    Button(
                        onClick = onReplaceOne,
                        enabled = currentMatchIndex >= 0
                    ) {
                        Text(stringResource(R.string.replace_one))
                    }

                    // Replace all button (FR-11)
                    Button(
                        onClick = onReplaceAll,
                        enabled = totalMatches > 0
                    ) {
                        Text(stringResource(R.string.replace_all))
                    }
                }
            }
        }
    }
}
