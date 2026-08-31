package com.sphynxs.mydatabases.ui.screens.queryfiles

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.LocalWindowSizeClass
import com.sphynxs.mydatabases.domain.models.QueryFileInfo
import com.sphynxs.mydatabases.ui.adaptive.adaptivePadding
import com.sphynxs.mydatabases.ui.components.PhosphorAppIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Query Files list screen (change `query-files-storage`) — shows `.sql` files from the active
 * connection's engine-scoped managed folder. The FAB opens the existing `NewQueryOptionsSheet`
 * (an overlay owned by `MyDataBasesNavHost`, not rendered here) via [onOpenNewQueryOptions].
 *
 * Both existing "New Query" entry points (bottom-nav modal action, `DatabaseActionMenuScreen`'s
 * "Consultas" tile) now navigate here first, replacing the old direct-to-sheet behavior.
 *
 * @param connectionId Active connection identifier — resolves the [com.sphynxs.mydatabases.core.database.engine.DatabaseType] to list
 * @param onOpenNewQueryOptions FAB callback — flips `showNewQueryOptionsSheet` at the NavHost level
 * @param onNavigateBack Back navigation callback
 * @param viewModel Hilt-provided [QueryFilesViewModel]
 * @author sdd-apply (Strict TDD)
 * @date 2026-08-05
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueryFilesScreen(
    connectionId: String,
    onOpenNewQueryOptions: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: QueryFilesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val showFallbackNotice by viewModel.showFallbackNotice.collectAsState()
    val windowSizeClass = LocalWindowSizeClass.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(connectionId) {
        viewModel.load(connectionId)
    }

    // Refresh on resume (no folder-watching) so a query saved from the editor shows up without
    // requiring an app restart.
    DisposableEffect(lifecycleOwner, connectionId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh(connectionId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.query_files_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = PhosphorAppIcons.Action.back, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenNewQueryOptions) {
                Icon(
                    imageVector = PhosphorAppIcons.Action.add,
                    contentDescription = stringResource(R.string.query_files_fab_description)
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (showFallbackNotice) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.query_storage_saf_fallback_notice),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            val padding = windowSizeClass?.let { adaptivePadding(it) } ?: androidx.compose.foundation.layout.PaddingValues(16.dp)

            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (val current = state) {
                    is QueryFilesUiState.Idle, is QueryFilesUiState.Loading ->
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                    is QueryFilesUiState.Success -> QueryFilesList(current.files)

                    is QueryFilesUiState.Empty ->
                        Text(
                            text = stringResource(R.string.query_files_empty_state),
                            modifier = Modifier.align(Alignment.Center)
                        )

                    is QueryFilesUiState.Error ->
                        Text(
                            text = current.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                }
            }
        }
    }
}

@Composable
private fun QueryFilesList(files: List<QueryFileInfo>) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(files, key = { it.uri.toString() }) { file ->
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(text = file.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = dateFormat.format(Date(file.lastModified)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

